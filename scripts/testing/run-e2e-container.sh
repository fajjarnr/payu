#!/bin/bash
# =============================================================================
# PayU E2E Blackbox Test Runner (Container Environment)
# =============================================================================
# Runs pytest-based API E2E tests against services running in Podman containers.
# Usage: ./scripts/run-e2e-container.sh [test-type] [--no-infra] [--keep-up]
#
# Test types: all, smoke, account, auth, transaction, wallet, investment,
#   lending, backoffice, partner, promotion, support, compliance, fx, cms,
#   statement, billing, notification, dispute, kyc, gateway, abtesting,
#   product_catalog, integration_svc
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_DIR="$PROJECT_ROOT/infrastructure/local-podman"
E2E_DIR="$PROJECT_ROOT/tests/e2e_blackbox"
COMPOSE_FILE="$COMPOSE_DIR/podman-compose.yml"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
GATEWAY_HEALTH="${GATEWAY_URL}/q/health"
MAX_GATEWAY_WAIT=180   # seconds
MAX_SERVICE_WAIT=120   # seconds

# Flags
START_INFRA=true
KEEP_UP=false
TEST_TYPE="${1:-all}"

# Parse optional flags
shift || true
while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-infra)  START_INFRA=false ;;
        --keep-up)   KEEP_UP=true ;;
        *)           echo "Unknown flag: $1"; exit 1 ;;
    esac
    shift
done

# ---------------------------------------------------------------------------
# Colors
# ---------------------------------------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${NC}    $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC}   $1"; }
log_step()    { echo -e "\n${CYAN}━━━ $1 ━━━${NC}"; }

# ---------------------------------------------------------------------------
# Prerequisites
# ---------------------------------------------------------------------------
check_prerequisites() {
    log_step "Step 0: Checking prerequisites"
    local missing=0

    for cmd in podman podman-compose curl python3 pip3; do
        if ! command -v "$cmd" &>/dev/null; then
            # podman-compose may be invoked as 'podman compose'
            if [[ "$cmd" == "podman-compose" ]]; then
                if ! podman compose version &>/dev/null; then
                    log_error "Missing: $cmd"
                    missing=1
                fi
            else
                log_error "Missing: $cmd"
                missing=1
            fi
        fi
    done

    if [[ $missing -eq 1 ]]; then
        log_error "Install missing prerequisites before running."
        exit 1
    fi
    log_success "All prerequisites found."
}

# ---------------------------------------------------------------------------
# Install Python test dependencies
# ---------------------------------------------------------------------------
install_test_deps() {
    log_step "Step 1: Installing test dependencies"
    if [[ -f "$E2E_DIR/requirements.txt" ]]; then
        # Use venv if available, otherwise --break-system-packages for PEP 668
        local venv_dir="$E2E_DIR/.venv"
        if [[ -d "$venv_dir" ]]; then
            source "$venv_dir/bin/activate"
            pip install -q -r "$E2E_DIR/requirements.txt" 2>&1 | tail -5
        elif python3 -m venv "$venv_dir" 2>/dev/null; then
            source "$venv_dir/bin/activate"
            pip install -q -r "$E2E_DIR/requirements.txt" 2>&1 | tail -5
        else
            pip3 install -q --break-system-packages -r "$E2E_DIR/requirements.txt" 2>&1 | tail -5
        fi
        log_success "Test dependencies installed."
    else
        log_warning "No requirements.txt found at $E2E_DIR; assuming deps are present."
    fi
}

# ---------------------------------------------------------------------------
# Start infrastructure in stages
# ---------------------------------------------------------------------------
start_infrastructure() {
    log_step "Step 2: Starting container infrastructure"
    cd "$COMPOSE_DIR"

    # Stage 1: Core infra (DB, cache, messaging) — start first, let them init
    log_info "Stage 1/3 — Core infrastructure (postgres, redis, kafka, vault, jaeger)..."
    podman compose -f "$COMPOSE_FILE" up -d \
        postgres redis kafka vault jaeger 2>&1 | tail -5
    log_info "Waiting 20s for core infra to initialise..."
    sleep 20

    # Stage 2: Identity & simulators
    log_info "Stage 2/3 — Identity & simulators (keycloak, simulators)..."
    podman compose -f "$COMPOSE_FILE" up -d \
        keycloak bi-fast-simulator qris-simulator dukcapil-simulator 2>&1 | tail -5
    sleep 10

    # Stage 3: All remaining services (let compose resolve dependency graph)
    log_info "Stage 3/3 — All services (compose handles dependency order)..."
    podman compose -f "$COMPOSE_FILE" up -d 2>&1 | tail -20
    sleep 10

    log_success "All containers started."
    cd "$PROJECT_ROOT"
}

# ---------------------------------------------------------------------------
# Wait for gateway health
# ---------------------------------------------------------------------------
wait_for_gateway() {
    log_step "Step 3: Waiting for gateway ($GATEWAY_URL)"
    local elapsed=0

    while [[ $elapsed -lt $MAX_GATEWAY_WAIT ]]; do
        if curl -sf "$GATEWAY_HEALTH" -o /dev/null 2>/dev/null; then
            log_success "Gateway is healthy! (${elapsed}s)"
            return 0
        fi
        sleep 5
        elapsed=$((elapsed + 5))
        printf "."
    done

    echo ""
    log_error "Gateway did not become healthy within ${MAX_GATEWAY_WAIT}s"
    log_info "Gateway logs (last 30 lines):"
    podman logs "$(podman ps -aqf 'name=gateway')" 2>&1 | tail -30 || true
    return 1
}

# ---------------------------------------------------------------------------
# Service health check summary
# ---------------------------------------------------------------------------
check_service_health() {
    log_step "Step 4: Service health check"
    local services=(
        "account-service:8001"
        "auth-service:8002"
        "transaction-service:8003"
        "wallet-service:8004"
        "billing-service:8005"
        "notification-service:8006"
        "kyc-service:8007"
        "analytics-service:8008"
        "investment-service:8009"
        "lending-service:8010"
        "backoffice-service:8011"
        "partner-service:8012"
        "promotion-service:8013"
        "support-service:8014"
        "statement-service:8015"
        "compliance-service:8016"
        "fx-service:8096"
        "cms-service:8095"
        "ab-testing-service:8097"
        "dispute-service:8098"
        "product-catalog-service:8100"
        "integration-service:8101"
        "api-portal-service:8021"
        "gateway-service:8080"
    )

    local healthy=0
    local unhealthy=0

    for svc in "${services[@]}"; do
        local name="${svc%%:*}"
        local port="${svc##*:}"
        local url="http://localhost:${port}/actuator/health"

        # Quarkus services use /q/health
        if [[ "$name" == "gateway-service" || "$name" == "billing-service" || \
              "$name" == "notification-service" || "$name" == "api-portal-service" ]]; then
            url="http://localhost:${port}/q/health"
        fi
        # Python services use /health
        if [[ "$name" == "kyc-service" || "$name" == "analytics-service" ]]; then
            url="http://localhost:${port}/health"
        fi

        if curl -sf "$url" -o /dev/null 2>/dev/null; then
            echo -e "  ${GREEN}✓${NC} $name (:$port)"
            healthy=$((healthy + 1))
        else
            echo -e "  ${RED}✗${NC} $name (:$port)"
            unhealthy=$((unhealthy + 1))
        fi
    done

    echo ""
    log_info "Healthy: $healthy | Unhealthy: $unhealthy"
    if [[ $unhealthy -gt 0 ]]; then
        log_warning "Some services are unhealthy — tests targeting them may fail."
    fi
}

# ---------------------------------------------------------------------------
# Run E2E tests
# ---------------------------------------------------------------------------
run_e2e_tests() {
    log_step "Step 5: Running E2E tests (type=$TEST_TYPE)"
    cd "$E2E_DIR"

    # Activate venv if it exists
    local venv_dir="$E2E_DIR/.venv"
    if [[ -d "$venv_dir/bin/activate" ]] || [[ -f "$venv_dir/bin/activate" ]]; then
        source "$venv_dir/bin/activate"
    fi

    local pytest_args=( "-v" "--tb=short" "--no-header" )

    case "$TEST_TYPE" in
        all)
            pytest_args+=( "." )
            ;;
        smoke)
            pytest_args+=( "-m" "smoke" )
            ;;
        account)       pytest_args+=( "test_account_flow.py" ) ;;
        auth)          pytest_args+=( "test_auth_flow.py" ) ;;
        transaction)   pytest_args+=( "test_transaction_flow.py" ) ;;
        wallet)        pytest_args+=( "test_wallet_flow.py" ) ;;
        investment)    pytest_args+=( "test_investment_flow.py" ) ;;
        lending)       pytest_args+=( "test_lending_flow.py" ) ;;
        backoffice)    pytest_args+=( "test_backoffice_flow.py" ) ;;
        partner)       pytest_args+=( "test_partner_flow.py" ) ;;
        promotion)     pytest_args+=( "test_promotion_flow.py" ) ;;
        support)       pytest_args+=( "test_support_flow.py" ) ;;
        compliance)    pytest_args+=( "test_compliance_flow.py" ) ;;
        fx)            pytest_args+=( "test_fx_flow.py" ) ;;
        cms)           pytest_args+=( "test_cms_flow.py" ) ;;
        statement)     pytest_args+=( "test_statement_flow.py" ) ;;
        billing)       pytest_args+=( "test_billing_flow.py" ) ;;
        notification)  pytest_args+=( "test_notification_flow.py" ) ;;
        dispute)       pytest_args+=( "test_dispute_flow.py" ) ;;
        kyc)           pytest_args+=( "test_kyc_flow.py" ) ;;
        gateway)       pytest_args+=( "test_gateway_flow.py" ) ;;
        abtesting)     pytest_args+=( "test_ab_testing_flow.py" ) ;;
        product_catalog) pytest_args+=( "test_product_catalog_flow.py" ) ;;
        integration_svc) pytest_args+=( "test_integration_flow.py" ) ;;
        *)
            log_error "Unknown test type: $TEST_TYPE"
            echo "Valid types: all, smoke, account, auth, transaction, wallet, investment,"
            echo "  lending, backoffice, partner, promotion, support, compliance, fx, cms,"
            echo "  statement, billing, notification, dispute, kyc, gateway, abtesting,"
            echo "  product_catalog, integration_svc"
            exit 1
            ;;
    esac

    log_info "Running: python3 -m pytest ${pytest_args[*]}"
    GATEWAY_URL="$GATEWAY_URL" python3 -m pytest "${pytest_args[@]}" \
        --junitxml="$PROJECT_ROOT/tests/e2e_blackbox/reports/e2e-results.xml"

    TEST_EXIT_CODE=$?
    cd "$PROJECT_ROOT"
    return "$TEST_EXIT_CODE"
}

# ---------------------------------------------------------------------------
# Print summary
# ---------------------------------------------------------------------------
print_summary() {
    local result=$1
    log_step "Summary"

    local report="$PROJECT_ROOT/tests/e2e_blackbox/reports/e2e-results.xml"
    if [[ -f "$report" ]]; then
        local tests=$(grep -oP 'tests="\K[0-9]+' "$report" 2>/dev/null || echo "?")
        local failures=$(grep -oP 'failures="\K[0-9]+' "$report" 2>/dev/null || echo "?")
        local errors=$(grep -oP 'errors="\K[0-9]+' "$report" 2>/dev/null || echo "?")
        log_info "Tests: $tests | Failures: $failures | Errors: $errors"
        log_info "Report: $report"
    fi

    if [[ $result -eq 0 ]]; then
        log_success "All E2E tests passed!"
    else
        log_error "Some E2E tests failed. Review the report above."
    fi
}

# ---------------------------------------------------------------------------
# Cleanup handler
# ---------------------------------------------------------------------------
cleanup() {
    if [[ "$KEEP_UP" == "false" && "$START_INFRA" == "true" ]]; then
        log_info "Stopping containers (use --keep-up to skip)..."
        cd "$COMPOSE_DIR"
        podman compose -f "$COMPOSE_FILE" down 2>&1 | tail -5 || true
        cd "$PROJECT_ROOT"
    fi
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
main() {
    echo "============================================"
    echo "  PayU E2E Blackbox Test Runner"
    echo "  Test type : $TEST_TYPE"
    echo "  Gateway   : $GATEWAY_URL"
    echo "  Start infra: $START_INFRA"
    echo "============================================"
    echo ""

    check_prerequisites
    install_test_deps

    if [[ "$START_INFRA" == "true" ]]; then
        start_infrastructure
        wait_for_gateway || { log_error "Aborting — gateway unreachable."; exit 1; }
    fi

    check_service_health

    # Ensure reports directory exists
    mkdir -p "$E2E_DIR/reports"

    run_e2e_tests
    TEST_RESULT=$?

    print_summary $TEST_RESULT

    # Cleanup on exit unless --keep-up
    if [[ "$KEEP_UP" == "false" && "$START_INFRA" == "true" ]]; then
        log_info "Use --keep-up to keep containers running after tests."
    fi

    exit $TEST_RESULT
}

trap cleanup EXIT
main
