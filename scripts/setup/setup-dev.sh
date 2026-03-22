#!/bin/bash
#
# PayU Quick Start - Development Environment
# =========================================
# Quick setup for developers who already have tools installed.
# For full environment setup, run: ./scripts/setup.sh
#
# Usage:
#   ./scripts/setup-dev.sh           # Quick start (build + start services)
#   ./scripts/setup-dev.sh --verify  # Verify environment only
#   ./scripts/setup-dev.sh --stop    # Stop all services
#   ./scripts/setup-dev.sh --clean   # Clean and rebuild
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_section() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ $1${NC}"; }

# Get project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# ============================================================================
# VERIFY FUNCTIONS
# ============================================================================

check_command() {
    if command -v "$1" >/dev/null 2>&1; then
        print_success "$1: $(command -v $1)"
        return 0
    else
        print_error "$1: Not found"
        return 1
    fi
}

verify_prerequisites() {
    print_section "Checking Prerequisites"

    local all_good=true
    check_command java || all_good=false
    check_command mvn || all_good=false
    check_command node || all_good=false
    check_command npm || all_good=false
    check_command python3 || all_good=false
    check_command podman || all_good=false
    check_command podman-compose || all_good=false

    if [ "$all_good" = false ]; then
        echo ""
        print_error "Missing prerequisites! Run: ./scripts/setup.sh"
        exit 1
    fi

    echo ""
    print_success "All prerequisites installed"
}

verify_services() {
    print_section "Verifying Services"

    local services_up=0
    local services_down=0

    # Check core services
    for service in postgres redis kafka keycloak; do
        if podman ps --filter "name=payu-$service" --filter "status=running" --format "{{.Names}}" | grep -q "payu-$service"; then
            print_success "$service: Running"
            ((services_up++))
        else
            print_error "$service: Not running"
            ((services_down++))
        fi
    done

    # Check backend services
    for service in account-service auth-service transaction-service wallet-service; do
        if podman ps --filter "name=payu-$service" --filter "status=running" --format "{{.Names}}" | grep -q "payu-$service"; then
            print_success "$service: Running"
            ((services_up++))
        else
            print_warning "$service: Not running"
            ((services_down++))
        fi
    done

    echo ""
    echo "Services: $services_up up, $services_down down"
}

verify_health_endpoints() {
    print_section "Checking Health Endpoints"

    # Wait for services to be ready
    echo "Waiting for services to be ready..."
    sleep 5

    local services=(
        "http://localhost:8001/actuator/health|Account Service"
        "http://localhost:8002/actuator/health|Auth Service"
        "http://localhost:8003/actuator/health|Transaction Service"
        "http://localhost:8004/actuator/health|Wallet Service"
        "http://localhost:8080/actuator/health|Gateway Service"
        "http://localhost:3001|Web App"
    )

    for service in "${services[@]}"; do
        url="${service%%|*}"
        name="${service##*|}"

        if curl -s -f "$url" >/dev/null 2>&1; then
            print_success "$name: Healthy"
        else
            print_warning "$name: Not responding"
        fi
    done
}

# ============================================================================
# BUILD FUNCTIONS
# ============================================================================

build_shared_starters() {
    print_section "Building Shared Starters"

    cd "$PROJECT_ROOT/backend/shared"

    print_success "Building api-commons..."
    cd api-commons && mvn clean install -DskipTests -q && cd ..

    print_success "Building cache-starter..."
    cd cache-starter && mvn clean install -DskipTests -q && cd ..

    print_success "Building resilience-starter..."
    cd resilience-starter && mvn clean install -DskipTests -q && cd ..

    print_success "Building security-starter..."
    cd security-starter && mvn clean install -DskipTests -q && cd ..

    cd "$PROJECT_ROOT"
    print_success "Shared starters built"
}

build_backend_services() {
    print_section "Building Backend Services (Parallel)"

    local services=(
        "account-service"
        "auth-service"
        "transaction-service"
        "wallet-service"
        "investment-service"
        "lending-service"
        "fx-service"
        "statement-service"
        "billing-service"
        "notification-service"
        "backoffice-service"
        "partner-service"
        "promotion-service"
        "support-service"
        "compliance-service"
        "cms-service"
        "ab-testing-service"
    )

    for service in "${services[@]}"; do
        if [ -d "backend/$service" ]; then
            echo -n "Building $service... "
            (cd "backend/$service" && mvn clean package -DskipTests -T 1C -q) && \
                print_success "$service built" || \
                print_error "$service failed"
        fi
    done
}

build_python_services() {
    print_section "Building Python Services"

    for service in kyc-service analytics-service; do
        if [ -d "backend/$service" ]; then
            echo -n "Building $service... "
            # Python services use Containerfile, just verify requirements exist
            if [ -f "backend/$service/requirements.txt" ]; then
                print_success "$service ready"
            else
                print_warning "$service requirements.txt missing"
            fi
        fi
    done
}

build_quarkus_services() {
    print_section "Building Quarkus Services"

    local services=(
        "gateway-service"
        "api-portal-service"
    )

    for service in "${services[@]}"; do
        if [ -d "backend/$service" ]; then
            echo -n "Building $service... "
            (cd "backend/$service" && mvn clean package -DskipTests -Dquarkus.package.type=uber-jar -q) && \
                print_success "$service built" || \
                print_error "$service failed"
        fi
    done
}

build_simulators() {
    print_section "Building Simulators"

    for sim in bi-fast-simulator dukcapil-simulator qris-simulator; do
        if [ -d "backend/simulators/$sim" ]; then
            echo -n "Building $sim... "
            (cd "backend/simulators/$sim" && mvn clean package -DskipTests -q) && \
                print_success "$sim built" || \
                print_error "$sim failed"
        fi
    done
}

install_frontend_deps() {
    print_section "Installing Frontend Dependencies"

    # Web App
    if [ -d "frontend/web-app" ]; then
        echo -n "Installing web-app deps... "
        (cd frontend/web-app && npm install --legacy-peer-deps --silent) && \
            print_success "web-app ready" || \
            print_error "web-app failed"
    fi

    # Developer Docs
    if [ -d "frontend/developer-docs" ]; then
        echo -n "Installing developer-docs deps... "
        (cd frontend/developer-docs && npm install --legacy-peer-deps --silent) && \
            print_success "developer-docs ready" || \
            print_error "developer-docs failed"
    fi
}

# ============================================================================
# SERVICE MANAGEMENT
# ============================================================================

start_services() {
    print_section "Starting Services with Podman Compose"

    podman-compose up -d

    echo ""
    print_success "Services started"
    echo ""
    echo "Waiting for services to be healthy (30s)..."
    sleep 30
}

stop_services() {
    print_section "Stopping All Services"

    podman-compose down

    echo ""
    print_success "All services stopped"
}

clean_build() {
    print_section "Cleaning Build Artifacts"

    # Stop services first
    podman-compose down 2>/dev/null || true

    # Clean Maven target directories
    find backend -type d -name "target" -exec rm -rf {} + 2>/dev/null || true

    # Clean node_modules
    find frontend -type d -name "node_modules" -prune -exec rm -rf {} \; 2>/dev/null || true

    # Clean Python venv
    find backend -type d -name ".venv" -prune -exec rm -rf {} \; 2>/dev/null || true

    print_success "Build artifacts cleaned"
}

rebuild_containers() {
    print_section "Rebuilding Containers"

    podman-compose build --no-cache

    print_success "Containers rebuilt"
}

# ============================================================================
# MAIN
# ============================================================================

show_help() {
    cat << EOF
PayU Quick Start - Development Environment

Usage: ./scripts/setup-dev.sh [OPTION]

Options:
  (none)      Quick start (verify + build + start)
  --verify    Verify environment and services only
  --build     Build all services without starting
  --start     Start services without building
  --stop      Stop all services
  --clean     Clean build artifacts and containers
  --rebuild   Rebuild all containers (no cache)
  --health    Check health endpoints
  --help      Show this help message

Examples:
  ./scripts/setup-dev.sh           # Full quick start
  ./scripts/setup-dev.sh --verify  # Check environment
  ./scripts/setup-dev.sh --stop    # Stop services

For full environment setup: ./scripts/setup.sh
EOF
}

main() {
    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║              PayU Quick Start - Development Environment        ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    case "${1:-start}" in
        --verify|-v)
            verify_prerequisites
            verify_services
            verify_health_endpoints
            ;;
        --build|-b)
            verify_prerequisites
            build_shared_starters
            build_backend_services
            build_quarkus_services
            build_python_services
            build_simulators
            install_frontend_deps
            print_success "Build complete!"
            echo ""
            echo "Run: ./scripts/setup-dev.sh --start"
            ;;
        --start|-s)
            verify_prerequisites
            start_services
            verify_services
            verify_health_endpoints
            ;;
        --stop)
            stop_services
            ;;
        --clean)
            clean_build
            ;;
        --rebuild)
            verify_prerequisites
            clean_build
            build_shared_starters
            build_backend_services
            build_quarkus_services
            rebuild_containers
            ;;
        --health|-h)
            verify_health_endpoints
            ;;
        --help|--help)
            show_help
            ;;
        *)
            # Quick start
            verify_prerequisites
            build_shared_starters
            build_backend_services
            build_quarkus_services
            build_python_services
            build_simulators
            install_frontend_deps
            start_services
            verify_services
            verify_health_endpoints

            print_section "Quick Start Complete!"
            echo ""
            echo "Services are running at:"
            echo "  • Web App:        http://localhost:3001"
            echo "  • Gateway:        http://localhost:8080"
            echo "  • Keycloak Admin: http://localhost:8099"
            echo "  • API Portal:     http://localhost:8080/api-docs"
            echo ""
            echo "Credentials:"
            echo "  • Keycloak Admin: admin / P@ssw0rd123"
            echo "  • Customer:       customer1 / P@ssw0rd123"
            echo ""
            echo "Next steps:"
            echo "  1. Open http://localhost:3001"
            echo "  2. Login with customer1 / P@ssw0rd123"
            echo "  3. Or run: npm run dev (in frontend/web-app)"
            echo ""
            ;;
    esac
}

main "$@"
