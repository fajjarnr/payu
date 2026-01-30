#!/bin/bash
#
# PayU Digital Banking Platform - Run All Tests Script
# =====================================================
# Executes full test suite across all services:
#   - Backend Java services (Maven)
#   - Backend Python services (pytest)
#   - Frontend web-app (npm/vitest)
#   - Mobile app (npm/jest)
#   - Integration tests
#   - E2E tests
#
# Usage: ./scripts/run-all-tests.sh [options]
#   --skip-build        Skip building shared dependencies
#   --skip-unit         Skip unit tests
#   --skip-integration  Skip integration tests
#   --skip-e2e          Skip E2E tests
#   --skip-backend      Skip all backend tests
#   --skip-frontend     Skip frontend tests
#   --skip-mobile       Skip mobile tests
#   --coverage          Generate coverage reports only
#
# Exit codes:
#   0 - All tests passed
#   1 - One or more test suites failed
#

set -e

echo "=========================================="
echo "PayU - Running All Tests"
echo "=========================================="

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        FAILED+=("$2")
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_section() {
    echo ""
    echo "=========================================="
    echo -e "${CYAN}$1${NC}"
    echo "=========================================="
}

# Parse arguments
SKIP_BUILD=false
SKIP_UNIT=false
SKIP_INTEGRATION=false
SKIP_E2E=false
SKIP_BACKEND=false
SKIP_FRONTEND=false
SKIP_MOBILE=false
COVERAGE_ONLY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-build) SKIP_BUILD=true ;;
        --skip-unit) SKIP_UNIT=true ;;
        --skip-integration) SKIP_INTEGRATION=true ;;
        --skip-e2e) SKIP_E2E=true ;;
        --skip-backend) SKIP_BACKEND=true ;;
        --skip-frontend) SKIP_FRONTEND=true ;;
        --skip-mobile) SKIP_MOBILE=true ;;
        --coverage) COVERAGE_ONLY=true ;;
        -h|--help)
            echo "Usage: ./scripts/run-all-tests.sh [options]"
            echo ""
            echo "Options:"
            echo "  --skip-build        Skip building shared dependencies"
            echo "  --skip-unit         Skip unit tests"
            echo "  --skip-integration  Skip integration tests"
            echo "  --skip-e2e          Skip E2E tests"
            echo "  --skip-backend      Skip all backend tests"
            echo "  --skip-frontend     Skip frontend tests"
            echo "  --skip-mobile       Skip mobile tests"
            echo "  --coverage          Generate coverage reports only"
            echo "  -h, --help          Show this help message"
            exit 0
            ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
    shift
done

FAILED=()
TOTAL_TESTS=0
PASSED_TESTS=0

# Track overall success
OVERALL_SUCCESS=true

# Determine which compose command to use
if docker-compose --version > /dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
else
    COMPOSE_CMD="docker compose"
fi

# Get script directory for relative paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# ============================================
# Step 0: Install shared dependencies
# ============================================

print_section "Step 0: Installing shared dependencies"

if [ "$SKIP_BUILD" = false ] && [ "$SKIP_BACKEND" = false ]; then
    print_info "Building and installing shared libraries..."

    # Build shared starters
    SHARED_STARTERS=("cache-starter" "resilience-starter" "security-starter")

    for starter in "${SHARED_STARTERS[@]}"; do
        print_info "Building $starter..."
        cd "$PROJECT_ROOT/backend/shared/$starter"
        if mvn clean install -DskipTests -q; then
            print_status 0 "$starter installed"
        else
            print_status 1 "$starter failed to build"
            print_warning "Some tests may fail due to missing dependencies"
            OVERALL_SUCCESS=false
        fi
        cd "$PROJECT_ROOT"
    done
else
    if [ "$SKIP_BACKEND" = true ]; then
        print_info "Skipping backend dependency build (--skip-backend)"
    else
        print_info "Skipping build (--skip-build)"
    fi
fi

# ============================================
# Step 1: Start test environment
# ============================================

print_section "Step 1: Starting test environment"

if [ "$SKIP_BUILD" = false ] && [ "$SKIP_INTEGRATION" = false ] || [ "$SKIP_E2E" = false ]; then
    print_info "Starting Docker test environment..."
    $COMPOSE_CMD -f "$PROJECT_ROOT/docker-compose.test.yml" up -d postgres-test redis-test kafka-test zookeeper-test > /dev/null 2>&1 || true
    print_info "Waiting for services to be healthy..."
    sleep 15

    # Run health check
    if [ -f "$SCRIPT_DIR/test-health-check.sh" ]; then
        if "$SCRIPT_DIR/test-health-check.sh" > /dev/null 2>&1; then
            print_status 0 "Test environment is healthy"
        else
            print_status 1 "Test environment health check failed"
            print_warning "Continuing anyway..."
        fi
    else
        print_warning "Health check script not found, skipping..."
    fi
else
    print_info "Skipping environment start"
fi

# ============================================
# Step 2: Backend Unit Tests
# ============================================

if [ "$SKIP_UNIT" = false ] && [ "$COVERAGE_ONLY" = false ] && [ "$SKIP_BACKEND" = false ]; then
    print_section "Step 2: Running Backend Unit Tests"

    # Java Spring Boot services
    JAVA_SERVICES=(
        "account-service"
        "auth-service"
        "transaction-service"
        "wallet-service"
        "billing-service"
        "notification-service"
        "gateway-service"
        "investment-service"
        "lending-service"
        "fx-service"
        "statement-service"
        "backoffice-service"
        "partner-service"
        "promotion-service"
        "support-service"
        "compliance-service"
        "cms-service"
        "ab-testing-service"
        "api-portal-service"
    )

    print_info "Running Java service tests..."
    for service in "${JAVA_SERVICES[@]}"; do
        if [ -d "$PROJECT_ROOT/backend/$service" ]; then
            TOTAL_TESTS=$((TOTAL_TESTS + 1))
            print_info "Testing $service..."

            cd "$PROJECT_ROOT/backend/$service"

            if [ -f "pom.xml" ]; then
                if [ -f "mvnw" ]; then
                    CMD="./mvnw"
                else
                    CMD="mvn"
                fi

                if $CMD test -q > /dev/null 2>&1; then
                    print_status 0 "$service unit tests passed"
                    PASSED_TESTS=$((PASSED_TESTS + 1))
                else
                    print_status 1 "$service unit tests failed"
                    OVERALL_SUCCESS=false
                fi
            else
                print_warning "$service has no pom.xml, skipping..."
            fi

            cd "$PROJECT_ROOT"
        else
            print_warning "$service directory not found, skipping..."
        fi
    done

    # Python services
    PYTHON_SERVICES=(
        "kyc-service"
        "analytics-service"
    )

    print_info "Running Python service tests..."
    for service in "${PYTHON_SERVICES[@]}"; do
        if [ -d "$PROJECT_ROOT/backend/$service" ]; then
            TOTAL_TESTS=$((TOTAL_TESTS + 1))
            print_info "Testing $service..."

            cd "$PROJECT_ROOT/backend/$service"

            if [ -f "pytest.ini" ] || [ -f "pyproject.toml" ] || [ -d "tests" ] || [ -d "test" ]; then
                if pytest -q > /dev/null 2>&1; then
                    print_status 0 "$service unit tests passed"
                    PASSED_TESTS=$((PASSED_TESTS + 1))
                else
                    print_status 1 "$service unit tests failed"
                    OVERALL_SUCCESS=false
                fi
            else
                print_warning "$service has no test configuration, skipping..."
            fi

            cd "$PROJECT_ROOT"
        else
            print_warning "$service directory not found, skipping..."
        fi
    done
else
    if [ "$SKIP_BACKEND" = true ]; then
        print_info "Skipping backend unit tests (--skip-backend)"
    else
        print_info "Skipping unit tests (--skip-unit or --coverage)"
    fi
fi

# ============================================
# Step 3: Generate Coverage Reports
# ============================================

if [ "$SKIP_UNIT" = false ] || [ "$COVERAGE_ONLY" = true ]; then
    print_section "Step 3: Generating Coverage Reports"

    if [ "$SKIP_BACKEND" = false ]; then
        print_info "Generating Java service coverage reports..."

        JAVA_SERVICES=(
            "account-service"
            "auth-service"
            "transaction-service"
            "wallet-service"
            "billing-service"
            "notification-service"
            "gateway-service"
        )

        for service in "${JAVA_SERVICES[@]}"; do
            if [ -d "$PROJECT_ROOT/backend/$service" ] && [ -f "$PROJECT_ROOT/backend/$service/pom.xml" ]; then
                print_info "Generating coverage for $service..."
                cd "$PROJECT_ROOT/backend/$service"

                if [ -f "mvnw" ]; then
                    CMD="./mvnw"
                else
                    CMD="mvn"
                fi

                if $CMD jacoco:report -q > /dev/null 2>&1; then
                    print_status 0 "$service coverage report generated"
                else
                    print_status 1 "$service coverage report failed"
                fi

                cd "$PROJECT_ROOT"
            fi
        done

        # Python coverage
        PYTHON_SERVICES=(
            "kyc-service"
            "analytics-service"
        )

        print_info "Generating Python service coverage reports..."
        for service in "${PYTHON_SERVICES[@]}"; do
            if [ -d "$PROJECT_ROOT/backend/$service" ]; then
                print_info "Generating coverage for $service..."
                cd "$PROJECT_ROOT/backend/$service"

                if pytest --cov=. --cov-report=html --cov-report=xml --cov-report=term -q > /dev/null 2>&1; then
                    print_status 0 "$service coverage report generated"
                else
                    print_status 1 "$service coverage report failed"
                fi

                cd "$PROJECT_ROOT"
            fi
        done
    fi

    # Frontend coverage
    if [ "$SKIP_FRONTEND" = false ] && [ -d "$PROJECT_ROOT/frontend/web-app" ]; then
        print_info "Generating frontend coverage report..."
        cd "$PROJECT_ROOT/frontend/web-app"

        if npm run test:coverage -- --run > /dev/null 2>&1; then
            print_status 0 "Frontend coverage report generated"
        else
            print_status 1 "Frontend coverage report failed"
        fi

        cd "$PROJECT_ROOT"
    fi

    # Mobile coverage
    if [ "$SKIP_MOBILE" = false ] && [ -d "$PROJECT_ROOT/frontend/mobile" ]; then
        print_info "Generating mobile coverage report..."
        cd "$PROJECT_ROOT/frontend/mobile"

        if npm run test -- --coverage > /dev/null 2>&1; then
            print_status 0 "Mobile coverage report generated"
        else
            print_status 1 "Mobile coverage report failed"
        fi

        cd "$PROJECT_ROOT"
    fi
else
    print_info "Skipping coverage reports (--skip-unit)"
fi

# ============================================
# Step 4: Backend Integration Tests
# ============================================

if [ "$SKIP_INTEGRATION" = false ] && [ "$SKIP_BACKEND" = false ]; then
    print_section "Step 4: Running Backend Integration Tests"

    print_info "Integration tests require full Docker environment..."

    # Run integration tests from tests/ directory if they exist
    if [ -d "$PROJECT_ROOT/tests/integration" ]; then
        print_info "Running integration tests from tests/integration..."
        cd "$PROJECT_ROOT/tests/integration"

        if [ -f "pytest.ini" ] || [ -f "pyproject.toml" ]; then
            if pytest -q > /dev/null 2>&1; then
                print_status 0 "Integration tests passed"
                PASSED_TESTS=$((PASSED_TESTS + 1))
            else
                print_status 1 "Integration tests failed"
                OVERALL_SUCCESS=false
            fi
            TOTAL_TESTS=$((TOTAL_TESTS + 1))
        else
            print_warning "No integration test configuration found"
        fi

        cd "$PROJECT_ROOT"
    elif [ -d "$PROJECT_ROOT/tests/regression" ]; then
        print_info "Running regression tests..."
        cd "$PROJECT_ROOT/tests/regression"

        if [ -f "pytest.ini" ] || ls *.py > /dev/null 2>&1; then
            if pytest -q > /dev/null 2>&1; then
                print_status 0 "Regression tests passed"
                PASSED_TESTS=$((PASSED_TESTS + 1))
            else
                print_status 1 "Regression tests failed"
                OVERALL_SUCCESS=false
            fi
            TOTAL_TESTS=$((TOTAL_TESTS + 1))
        else
            print_warning "No regression tests found"
        fi

        cd "$PROJECT_ROOT"
    else
        print_warning "Integration tests not yet implemented"
        print_info "Create tests/integration/ directory with pytest tests"
    fi
else
    if [ "$SKIP_BACKEND" = true ]; then
        print_info "Skipping backend integration tests (--skip-backend)"
    else
        print_info "Skipping integration tests (--skip-integration)"
    fi
fi

# ============================================
# Step 5: Frontend Tests
# ============================================

if [ "$SKIP_UNIT" = false ] && [ "$COVERAGE_ONLY" = false ] && [ "$SKIP_FRONTEND" = false ]; then
    print_section "Step 5: Running Frontend Tests"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    print_info "Running frontend unit tests..."

    if [ -d "$PROJECT_ROOT/frontend/web-app" ]; then
        cd "$PROJECT_ROOT/frontend/web-app"

        if npm run test -- --run --reporter=verbose > /dev/null 2>&1; then
            print_status 0 "Frontend unit tests passed"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            print_status 1 "Frontend unit tests failed"
            OVERALL_SUCCESS=false
        fi

        cd "$PROJECT_ROOT"
    else
        print_warning "Frontend web-app directory not found"
    fi
else
    if [ "$SKIP_FRONTEND" = true ]; then
        print_info "Skipping frontend tests (--skip-frontend)"
    else
        print_info "Skipping frontend tests (--skip-unit or --coverage)"
    fi
fi

# ============================================
# Step 6: Mobile Tests
# ============================================

if [ "$SKIP_UNIT" = false ] && [ "$COVERAGE_ONLY" = false ] && [ "$SKIP_MOBILE" = false ]; then
    print_section "Step 6: Running Mobile Tests"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    print_info "Running mobile unit tests..."

    if [ -d "$PROJECT_ROOT/frontend/mobile" ]; then
        cd "$PROJECT_ROOT/frontend/mobile"

        if npm run test > /dev/null 2>&1; then
            print_status 0 "Mobile unit tests passed"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            print_status 1 "Mobile unit tests failed"
            OVERALL_SUCCESS=false
        fi

        cd "$PROJECT_ROOT"
    else
        print_warning "Mobile directory not found"
    fi
else
    if [ "$SKIP_MOBILE" = true ]; then
        print_info "Skipping mobile tests (--skip-mobile)"
    else
        print_info "Skipping mobile tests (--skip-unit or --coverage)"
    fi
fi

# ============================================
# Step 7: E2E Tests
# ============================================

if [ "$SKIP_E2E" = false ]; then
    print_section "Step 7: Running E2E Tests"

    print_info "E2E tests require full application stack..."

    # Check for Playwright E2E tests in web-app
    if [ -d "$PROJECT_ROOT/frontend/web-app" ] && [ -d "$PROJECT_ROOT/frontend/web-app/e2e" -o -d "$PROJECT_ROOT/frontend/web-app/tests/e2e" ]; then
        print_info "Running Playwright E2E tests..."
        cd "$PROJECT_ROOT/frontend/web-app"

        if npm run test:e2e > /dev/null 2>&1; then
            print_status 0 "E2E tests passed"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            print_status 1 "E2E tests failed"
            OVERALL_SUCCESS=false
        fi
        TOTAL_TESTS=$((TOTAL_TESTS + 1))

        cd "$PROJECT_ROOT"
    else
        print_warning "E2E tests not yet implemented"
        print_info "Add E2E tests to frontend/web-app/e2e/"
    fi
else
    print_info "Skipping E2E tests (--skip-e2e)"
fi

# ============================================
# Step 8: Generate Combined Coverage Report
# ============================================

if [ "$SKIP_UNIT" = false ] || [ "$COVERAGE_ONLY" = true ]; then
    print_section "Step 8: Generating Combined Coverage Report"

    COVERAGE_DIR="$PROJECT_ROOT/coverage"
    mkdir -p "$COVERAGE_DIR"

    # Collect Java coverage reports
    if [ "$SKIP_BACKEND" = false ]; then
        print_info "Collecting Java coverage reports..."
        mkdir -p "$COVERAGE_DIR/java"

        for service in "${JAVA_SERVICES[@]}"; do
            if [ -d "$PROJECT_ROOT/backend/$service/target/site/jacoco" ]; then
                cp -r "$PROJECT_ROOT/backend/$service/target/site/jacoco" "$COVERAGE_DIR/java/$service" 2>/dev/null || true
            fi
        done
        print_status 0 "Java coverage reports collected in coverage/java/"
    fi

    # Collect Python coverage reports
    if [ "$SKIP_BACKEND" = false ]; then
        print_info "Collecting Python coverage reports..."
        mkdir -p "$COVERAGE_DIR/python"

        for service in "${PYTHON_SERVICES[@]}"; do
            if [ -d "$PROJECT_ROOT/backend/$service/htmlcov" ]; then
                cp -r "$PROJECT_ROOT/backend/$service/htmlcov" "$COVERAGE_DIR/python/$service" 2>/dev/null || true
            fi
        done
        print_status 0 "Python coverage reports collected in coverage/python/"
    fi

    # Collect Frontend coverage reports
    if [ "$SKIP_FRONTEND" = false ]; then
        print_info "Collecting frontend coverage reports..."
        mkdir -p "$COVERAGE_DIR/frontend"

        if [ -d "$PROJECT_ROOT/frontend/web-app/coverage" ]; then
            cp -r "$PROJECT_ROOT/frontend/web-app/coverage" "$COVERAGE_DIR/frontend/web-app" 2>/dev/null || true
        fi
        print_status 0 "Frontend coverage reports collected in coverage/frontend/"
    fi

    # Collect Mobile coverage reports
    if [ "$SKIP_MOBILE" = false ]; then
        print_info "Collecting mobile coverage reports..."
        mkdir -p "$COVERAGE_DIR/mobile"

        if [ -d "$PROJECT_ROOT/frontend/mobile/coverage" ]; then
            cp -r "$PROJECT_ROOT/frontend/mobile/coverage" "$COVERAGE_DIR/mobile/app" 2>/dev/null || true
        fi
        print_status 0 "Mobile coverage reports collected in coverage/mobile/"
    fi

    # Generate combined report index
    cat > "$COVERAGE_DIR/index.html" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>PayU Test Coverage Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
        h1 { color: #333; }
        .section { background: white; padding: 20px; margin: 20px 0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        a { color: #0066cc; text-decoration: none; }
        a:hover { text-decoration: underline; }
        ul { line-height: 1.8; }
    </style>
</head>
<body>
    <h1>PayU Digital Banking - Test Coverage Report</h1>
    <div class="section">
        <h2>Backend Services</h2>
        <h3>Java Services</h3>
        <ul>
            <li><a href="java/account-service/index.html">Account Service</a></li>
            <li><a href="java/auth-service/index.html">Auth Service</a></li>
            <li><a href="java/transaction-service/index.html">Transaction Service</a></li>
            <li><a href="java/wallet-service/index.html">Wallet Service</a></li>
        </ul>
        <h3>Python Services</h3>
        <ul>
            <li><a href="python/kyc-service/index.html">KYC Service</a></li>
            <li><a href="python/analytics-service/index.html">Analytics Service</a></li>
        </ul>
    </div>
    <div class="section">
        <h2>Frontend</h2>
        <ul>
            <li><a href="frontend/web-app/index.html">Web App</a></li>
        </ul>
    </div>
    <div class="section">
        <h2>Mobile</h2>
        <ul>
            <li><a href="mobile/app/index.html">Mobile App</a></li>
        </ul>
    </div>
</body>
</html>
EOF

    print_status 0 "Combined coverage report generated at coverage/index.html"
else
    print_info "Skipping combined coverage report (--skip-unit)"
fi

# ============================================
# Final Summary
# ============================================

print_section "Test Execution Summary"

echo ""
echo "Total test suites: $TOTAL_TESTS"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $((TOTAL_TESTS - PASSED_TESTS))${NC}"
echo ""

if [ ${#FAILED[@]} -gt 0 ]; then
    echo -e "${RED}Failed test suites:${NC}"
    for failed in "${FAILED[@]}"; do
        echo "  - $failed"
    done
    echo ""
    echo -e "${RED}❌ Some tests failed!${NC}"
    echo ""
    exit 1
else
    echo -e "${GREEN}✅ All tests passed!${NC}"
    echo ""
    exit 0
fi
