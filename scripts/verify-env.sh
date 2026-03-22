#!/bin/bash
#
# PayU Environment Verification Script
# =====================================
# Comprehensive health check for PayU development environment.
#
# Usage:
#   ./scripts/verify-env.sh              # Full verification
#   ./scripts/verify-env.sh --quick     # Quick check (services only)
#   ./scripts/verify-env.sh --health    # Health endpoints only
#   ./scripts/verify-env.sh --deps      # Dependencies only
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Counters
PASS=0
FAIL=0
WARN=0

print_header() {
    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║  $1${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"
}

print_pass() { echo -e "${GREEN}✓ PASS${NC} - $1"; ((PASS++)); }
print_fail() { echo -e "${RED}✗ FAIL${NC} - $1"; ((FAIL++)); }
print_warn() { echo -e "${YELLOW}⚠ WARN${NC} - $1"; ((WARN++)); }
print_info() { echo -e "${BLUE}  ℹ${NC} - $1"; }

# ============================================================================
# 1. SYSTEM CHECKS
# ============================================================================

check_system() {
    print_header "1. System Environment"

    # OS
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        print_pass "OS: $PRETTY_NAME"
    else
        print_warn "OS: Unknown ($(uname -s))"
    fi

    # Disk space
    DISK_AVAIL=$(df -h "$PROJECT_ROOT" | awk 'NR==2 {print $4}')
    DISK_USED=$(df -h "$PROJECT_ROOT" | awk 'NR==2 {print $5}')
    print_info "Disk: $DISK_USED used, $DISK_AVAIL available"

    # Memory
    if [ -f /proc/meminfo ]; then
        MEM_TOTAL=$(awk '/MemTotal/ {printf "%.2f GB", $2/1024/1024}' /proc/meminfo)
        MEM_AVAIL=$(awk '/MemAvailable/ {printf "%.2f GB", $2/1024/1024}' /proc/meminfo)
        print_info "Memory: $MEM_TOTAL total, $MEM_AVAIL available"
    fi

    # CPU
    if command -v nproc >/dev/null 2>&1; then
        CPUS=$(nproc)
        print_info "CPU Cores: $CPUS"
    fi
}

# ============================================================================
# 2. DEPENDENCY CHECKS
# ============================================================================

check_dependencies() {
    print_header "2. Tool Dependencies"

    local required_tools=(
        "java:21:Java 21"
        "mvn::Maven"
        "node:20:Node.js 20+"
        "npm::npm"
        "python3:3.12:Python 3.12+"
        "podman::Podman"
        "git::Git"
    )

    for tool_spec in "${required_tools[@]}"; do
        IFS=':' read -r cmd min_ver desc <<< "$tool_spec"

        if command -v "$cmd" >/dev/null 2>&1; then
            version=$(eval "${cmd}_version" 2>&1 || ${cmd} --version 2>&1 | head -n1 || echo "unknown")

            if [ -n "$min_ver" ]; then
                if echo "$version" | grep -q "$min_ver"; then
                    print_pass "$desc: $version"
                else
                    print_warn "$desc: $version (recommended: $min_ver)"
                fi
            else
                print_pass "$desc: $version"
            fi
        else
            print_fail "$desc: Not installed"
        fi
    done

    # Optional tools
    echo ""
    print_info "Optional Tools:"

    local optional_tools=(
        "podman-compose:Podman Compose"
        "jq:jq"
        "psql:PostgreSQL Client"
        "redis-cli:Redis Client"
        "kc:kcat (Kafka CLI)"
        "trivy:Trivy (Security Scanner)"
        "k6:k6 (Performance Testing)"
        "pre-commit:pre-commit"
        "oc:OpenShift CLI"
    )

    for tool_spec in "${optional_tools[@]}"; do
        IFS=':' read -r cmd desc <<< "$tool_spec"
        if command -v "$cmd" >/dev/null 2>&1; then
            print_pass "$desc: installed"
        else
            print_warn "$desc: not installed (optional)"
        fi
    done
}

# ============================================================================
# 3. PROJECT STRUCTURE CHECKS
# ============================================================================

check_project_structure() {
    print_header "3. Project Structure"

    local required_dirs=(
        "backend"
        "backend/shared"
        "backend/account-service"
        "backend/auth-service"
        "backend/transaction-service"
        "backend/wallet-service"
        "frontend/web-app"
        "frontend/developer-docs"
        "docs"
        "scripts"
        "infrastructure"
    )

    for dir in "${required_dirs[@]}"; do
        if [ -d "$dir" ]; then
            print_pass "Directory exists: $dir"
        else
            print_fail "Directory missing: $dir"
        fi
    done

    # Required files
    echo ""
    local required_files=(
        "infrastructure/local-podman/podman-compose.yml"
        ".env.example"
        "pom.xml"
        "Makefile"
        "LICENSE"
        "README.md"
    )

    for file in "${required_files[@]}"; do
        if [ -f "$file" ]; then
            print_pass "File exists: $file"
        else
            print_fail "File missing: $file"
        fi
    done
}

# ============================================================================
# 4. CONTAINER & SERVICE CHECKS
# ============================================================================

check_containers() {
    print_header "4. Container Status"

    # Check if Podman is running
    if ! podman info >/dev/null 2>&1; then
        print_fail "Podman daemon not running"
        return
    fi

    # Get all PayU containers
    local containers=$(podman ps --filter "label=io.podman.compose.project=payu" --format "{{.Names}}")

    if [ -z "$containers" ]; then
        print_fail "No PayU containers running"
        print_info "Run: podman-compose up -d"
        return
    fi

    # Check infrastructure containers
    local infra_services=(
        "payu-postgres:PostgreSQL"
        "payu-redis:Redis"
        "payu-kafka:Kafka"
        "payu-keycloak:Keycloak"
    )

    for service in "${infra_services[@]}"; do
        IFS=':' read -r container name <<< "$service"
        if podman ps --filter "name=$container" --filter "status=running" --format "{{.Names}}" | grep -q "$container"; then
            print_pass "$name is running"
        else
            print_fail "$name is not running"
        fi
    done

    # Check backend services
    echo ""
    local backend_services=(
        "payu-account-service:Account Service"
        "payu-auth-service:Auth Service"
        "payu-transaction-service:Transaction Service"
        "payu-wallet-service:Wallet Service"
        "payu-billing-service:Billing Service"
        "payu-notification-service:Notification Service"
        "payu-kyc-service:KYC Service"
        "payu-analytics-service:Analytics Service"
        "payu-investment-service:Investment Service"
        "payu-lending-service:Lending Service"
        "payu-backoffice-service:Backoffice Service"
        "payu-partner-service:Partner Service"
        "payu-promotion-service:Promotion Service"
        "payu-support-service:Support Service"
        "payu-statement-service:Statement Service"
        "payu-api-portal-service:API Portal Service"
        "payu-gateway-service:Gateway Service"
        "payu-compliance-service:Compliance Service"
        "payu-cms-service:CMS Service"
        "payu-fx-service:FX Service"
        "payu-dispute-service:Dispute Service"
        "payu-product-catalog-service:Product Catalog Service"
        "payu-integration-service:Integration Service"
    )

    for service in "${backend_services[@]}"; do
        IFS=':' read -r container name <<< "$service"
        if podman ps --filter "name=$container" --filter "status=running" --format "{{.Names}}" | grep -q "$container"; then
            # Check health status
            health=$(podman inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "unknown")
            if [ "$health" = "healthy" ]; then
                print_pass "$name is healthy"
            elif [ "$health" = "starting" ]; then
                print_warn "$name is starting"
            else
                print_warn "$name status: $health"
            fi
        else
            print_warn "$name is not running"
        fi
    done

    # Check frontend
    echo ""
    if podman ps --filter "name=payu-web-app" --filter "status=running" --format "{{.Names}}" | grep -q "payu-web-app"; then
        print_pass "Web App is running"
    else
        print_warn "Web App is not running (may be running via npm run dev)"
    fi
}

# ============================================================================
# 5. HEALTH ENDPOINT CHECKS
# ============================================================================

check_health_endpoints() {
    print_header "5. Health Endpoints"

    local services=(
        "8001:actuator/health:Account Service"
        "8002:actuator/health:Auth Service"
        "8003:actuator/health:Transaction Service"
        "8004:actuator/health:Wallet Service"
        "8005:q/health:Billing Service"
        "8006:q/health:Notification Service"
        "8007:health:KYC Service"
        "8008:health:Analytics Service"
        "8009:actuator/health:Investment Service"
        "8010:actuator/health:Lending Service"
        "8011:q/health:Backoffice Service"
        "8012:q/health:Partner Service"
        "8013:q/health:Promotion Service"
        "8014:q/health:Support Service"
        "8015:actuator/health:Statement Service"
        "8021:q/health:API Portal Service"
        "8080:q/health:Gateway Service"
        "8087:actuator/health:Compliance Service"
        "8095:actuator/health:CMS Service"
        "8096:actuator/health:FX Service"
        "8098:actuator/health:Dispute Service"
        "8100:actuator/health:Product Catalog Service"
        "8101:actuator/health:Integration Service"
    )

    for service in "${services[@]}"; do
        IFS=':' read -r port path name <<< "$service"
        local url="http://localhost:$port/$path"

        if curl -s -f "$url" >/dev/null 2>&1; then
            status=$(curl -s "$url" | jq -r '.status' 2>/dev/null || echo "UP")
            if [ "$status" = "UP" ] || [ "$status" = "null" ]; then
                print_pass "$name: UP"
            else
                print_warn "$name: $status"
            fi
        else
            print_fail "$name: Not responding"
        fi
    done

    # Check web app
    echo ""
    if curl -s -f http://localhost:3001 >/dev/null 2>&1; then
        print_pass "Web App: Responding"
    else
        print_warn "Web App: Not responding (may need npm run dev)"
    fi

    # Check Keycloak
    if curl -s -f http://localhost:8099 >/dev/null 2>&1; then
        print_pass "Keycloak: Responding"
    else
        print_fail "Keycloak: Not responding"
    fi
}

# ============================================================================
# 6. DATABASE CHECKS
# ============================================================================

check_databases() {
    print_header "6. Database Connectivity"

    # Check PostgreSQL
    if podman exec payu-postgres pg_isready -U payu >/dev/null 2>&1; then
        print_pass "PostgreSQL: Ready"

        # Check databases
        local dbs=$(podman exec payu-postgres psql -U payu -d postgres -tAc "SELECT datname FROM pg_database WHERE datname LIKE 'payu_%' ORDER BY datname;")
        local db_count=$(echo "$dbs" | wc -l)
        print_info "PayU databases: $db_count found"
    else
        print_fail "PostgreSQL: Not ready"
    fi

    # Check Redis
    if podman exec payu-redis redis-cli ping >/dev/null 2>&1; then
        print_pass "Redis: Ready"
    else
        print_fail "Redis: Not ready"
    fi

    # Check Kafka
    if podman exec payu-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1; then
        print_pass "Kafka: Ready"
    else
        print_fail "Kafka: Not ready"
    fi
}

# ============================================================================
# 7. NETWORK CHECKS
# ============================================================================

check_networks() {
    print_header "7. Network Configuration"

    # Check PayU network
    if podman network exists payu_payu-network 2>/dev/null; then
        print_pass "Podman network: payu_payu-network exists"
    else
        print_warn "Podman network: payu_payu-network not found"
    fi

    # Check port availability
    echo ""
    local ports=(
        "3001:Web App"
        "8001:Account Service"
        "8002:Auth Service"
        "8003:Transaction Service"
        "8004:Wallet Service"
        "8005:Billing Service"
        "8006:Notification Service"
        "8007:KYC Service"
        "8008:Analytics Service"
        "8009:Investment Service"
        "8010:Lending Service"
        "8011:Backoffice Service"
        "8012:Partner Service"
        "8013:Promotion Service"
        "8014:Support Service"
        "8015:Statement Service"
        "8021:API Portal Service"
        "8080:Gateway"
        "8087:Compliance Service"
        "8095:CMS Service"
        "8096:FX Service"
        "8098:Dispute Service"
        "8100:Product Catalog Service"
        "8101:Integration Service"
        "8099:Keycloak"
        "5432:PostgreSQL"
        "6379:Redis"
        "9092:Kafka"
    )

    for port in "${ports[@]}"; do
        IFS=':' read -r p name <<< "$port"
        if curl -s "http://localhost:$p" >/dev/null 2>&1 || \
           nc -z "localhost" "$p" 2>/dev/null || \
           podman ps --filter "publish=$p" --format "{{.Names}}" | grep -q .; then
            print_pass "$name: Port $p accessible"
        else
            print_info "$name: Port $p not listening"
        fi
    done
}

# ============================================================================
# 8. CONFIGURATION CHECKS
# ============================================================================

check_configuration() {
    print_header "8. Configuration Files"

    # Check .env file
    if [ -f .env ]; then
        print_pass ".env file exists"

        # Check for critical variables
        if grep -q "POSTGRES_PASSWORD=your_secure" .env 2>/dev/null; then
            print_warn ".env: Using default passwords (change for production)"
        fi
    else
        print_warn ".env file not found (copy from .env.example)"
    fi

    # Check AI skills symlink
    if [ -L .claude/skills ]; then
        print_pass "AI skills symlink configured"
    elif [ -d .agent/skills ]; then
        print_warn "AI skills symlink not configured"
    fi
}

# ============================================================================
# SUMMARY
# ============================================================================

print_summary() {
    print_header "Verification Summary"

    echo -e "  ${GREEN}PASSED${NC}: $PASS"
    echo -e "  ${YELLOW}WARNINGS${NC}: $WARN"
    echo -e "  ${RED}FAILED${NC}: $FAIL"
    echo ""

    if [ $FAIL -eq 0 ]; then
        echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}                    ✓ ALL CHECKS PASSED                          ${NC}"
        echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
        echo ""
        echo "Your PayU development environment is ready!"
        echo ""
        echo "Quick links:"
        echo "  • Web App:        http://localhost:3001"
        echo "  • Gateway:        http://localhost:8080"
        echo "  • Keycloak Admin: http://localhost:8099"
        echo "  • API Docs:       http://localhost:8080/api-docs"
        echo ""
        return 0
    else
        echo -e "${RED}═══════════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}                    ✗ SOME CHECKS FAILED                       ${NC}"
        echo -e "${RED}═══════════════════════════════════════════════════════════════${NC}"
        echo ""
        echo "Please fix the failed checks above."
        echo ""
        echo "Common fixes:"
        echo "  • Missing tools:    Run ./scripts/setup.sh"
        echo "  • Services down:    Run ./scripts/setup-dev.sh"
        echo "  • Port conflicts:   Stop conflicting services"
        echo ""
        return 1
    fi
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    # Get project root
    PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    cd "$PROJECT_ROOT"

    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║         PayU Environment Verification Script                 ║${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"

    case "${1:-full}" in
        --quick|-q)
            check_containers
            check_health_endpoints
            ;;
        --health|-h)
            check_health_endpoints
            ;;
        --deps|-d)
            check_dependencies
            ;;
        --help|--help)
            cat << EOF
PayU Environment Verification Script

Usage: ./scripts/verify-env.sh [OPTION]

Options:
  (none)      Full verification
  --quick     Quick check (services & health only)
  --health    Health endpoints only
  --deps      Dependencies only
  --help      Show this help message

Examples:
  ./scripts/verify-env.sh           # Full verification
  ./scripts/verify-env.sh --quick   # Quick check
EOF
            exit 0
            ;;
        *)
            # Full verification
            check_system
            check_dependencies
            check_project_structure
            check_configuration
            check_containers
            check_databases
            check_networks
            check_health_endpoints
            ;;
    esac

    print_summary
}

main "$@"
