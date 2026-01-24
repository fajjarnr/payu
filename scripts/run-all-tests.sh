#!/bin/bash
set -e

# ============================================
# PayU - Run All Tests Script
# Executes full test suite: unit, integration, E2E
# ============================================

echo "=========================================="
echo "PayU - Running All Tests"
echo "=========================================="

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

# Parse arguments
SKIP_BUILD=false
SKIP_UNIT=false
SKIP_INTEGRATION=false
SKIP_E2E=false
COVERAGE_ONLY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-build) SKIP_BUILD=true ;;
        --skip-unit) SKIP_UNIT=true ;;
        --skip-integration) SKIP_INTEGRATION=true ;;
        --skip-e2e) SKIP_E2E=true ;;
        --coverage) COVERAGE_ONLY=true ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
    shift
done

FAILED=()
TOTAL_TESTS=0
PASSED_TESTS=0

# Determine which compose command to use
if docker-compose --version > /dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
else
    COMPOSE_CMD="docker compose"
fi

# ============================================
# Step 0: Install shared dependencies
# ============================================

echo ""
echo "=========================================="
echo "Step 0: Installing shared dependencies"
echo "=========================================="

if [ "$SKIP_BUILD" = false ]; then
    print_info "Building and installing shared libraries..."

    # Build cache-starter
    print_info "Building cache-starter..."
    cd backend/shared/cache-starter
    if mvn clean install -DskipTests -q; then
        print_status 0 "cache-starter installed"
    else
        print_status 1 "cache-starter failed to build"
        print_warning "Some tests may fail due to missing dependencies"
    fi
    cd ../../..

    # Build resilience-starter
    print_info "Building resilience-starter..."
    cd backend/shared/resilience-starter
    if mvn clean install -DskipTests -q; then
        print_status 0 "resilience-starter installed"
    else
        print_status 1 "resilience-starter failed to build"
        print_warning "Some tests may fail due to missing dependencies"
    fi
    cd ../../..

    # Build security-starter
    print_info "Building security-starter..."
    cd backend/shared/security-starter
    if mvn clean install -DskipTests -q; then
        print_status 0 "security-starter installed"
    else
        print_status 1 "security-starter failed to build"
        print_warning "Some tests may fail due to missing dependencies"
    fi
    cd ../../..
else
    print_info "Skipping build (--skip-build)"
fi

# ============================================
# Step 1: Start test environment
# ============================================

echo ""
echo "=========================================="
echo "Step 1: Starting test environment"
echo "=========================================="

if [ "$SKIP_BUILD" = false ]; then
    print_info "Starting Docker test environment..."
    $COMPOSE_CMD -f docker-compose.test.yml up -d postgres-test redis-test kafka-test > /dev/null 2>&1
    print_info "Waiting for services to be healthy..."
    sleep 15

    # Run health check
    if ./scripts/test-health-check.sh > /dev/null 2>&1; then
        print_status 0 "Test environment is healthy"
    else
        print_status 1 "Test environment health check failed"
        print_warning "Continuing anyway..."
    fi
else
    print_info "Skipping environment start (--skip-build)"
fi

# ============================================
# Step 2: Backend Unit Tests
# ============================================

if [ "$SKIP_UNIT" = false ] && [ "$COVERAGE_ONLY" = false ]; then
    echo ""
    echo "=========================================="
    echo "Step 2: Running Backend Unit Tests"
    echo "=========================================="

    SERVICES=(
        "account-service"
        "auth-service"
        "transaction-service"
        "wallet-service"
        "billing-service"
        "notification-service"
        "gateway-service"
    )

    for service in "${SERVICES[@]}"; do
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
        print_info "Testing $service..."

        cd "backend/$service"

        if [ -f "mvnw" ]; then
            CMD="./mvnw test"
        else
            CMD="mvn test"
        fi

        if $CMD -q > /dev/null 2>&1; then
            print_status 0 "$service unit tests passed"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            print_status 1 "$service unit tests failed"
        fi

        cd ../..
    done

    # Python services
    PYTHON_SERVICES=(
        "kyc-service"
        "analytics-service"
    )

    for service in "${PYTHON_SERVICES[@]}"; do
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
        print_info "Testing $service..."

        cd "backend/$service"

        if pytest -q > /dev/null 2>&1; then
            print_status 0 "$service unit tests passed"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            print_status 1 "$service unit tests failed"
        fi

        cd ../..
    done
else
    print_info "Skipping unit tests (--skip-unit or --coverage)"
fi

# ============================================
# Step 3: Generate Coverage Reports
# ============================================

echo ""
echo "=========================================="
echo "Step 3: Generating Coverage Reports"
echo "=========================================="

if [ "$COVERAGE_ONLY" = false ] || [ "$SKIP_UNIT" = false ]; then
    print_info "Generating Java service coverage reports..."

    JAVA_SERVICES=(
        "account-service"
        "auth-service"
        "transaction-service"
        "wallet-service"
    )

    for service in "${JAVA_SERVICES[@]}"; do
        print_info "Generating coverage for $service..."
        cd "backend/$service"

        if [ -f "mvnw" ]; then
            CMD="./mvnw jacoco:report"
        else
            CMD="mvn jacoco:report"
        fi

        if $CMD -q > /dev/null 2>&1; then
            print_status 0 "$service coverage report generated"
        else
            print_status 1 "$service coverage report failed"
        fi

        cd ../..
    done

    # Python coverage
    PYTHON_SERVICES=(
        "kyc-service"
        "analytics-service"
    )

    for service in "${PYTHON_SERVICES[@]}"; do
        print_info "Generating coverage for $service..."
        cd "backend/$service"

        if pytest --cov=. --cov-report=html --cov-report=term -q > /dev/null 2>&1; then
            print_status 0 "$service coverage report generated"
        else
            print_status 1 "$service coverage report failed"
        fi

        cd ../..
    done
else
    print_info "Skipping coverage reports (--skip-unit)"
fi

# ============================================
# Step 4: Backend Integration Tests
# ============================================

if [ "$SKIP_INTEGRATION" = false ]; then
    echo ""
    echo "=========================================="
echo "Step 4: Running Backend Integration Tests"
    echo "=========================================="

    print_info "Integration tests require full Docker environment..."
    print_warning "Integration tests not yet implemented"
    print_info "Placeholder for integration test execution"
else
    print_info "Skipping integration tests (--skip-integration)"
fi

# ============================================
# Step 5: Frontend Tests
# ============================================

if [ "$SKIP_UNIT" = false ] && [ "$COVERAGE_ONLY" = false ]; then
    echo ""
    echo "=========================================="
    echo "Step 5: Running Frontend Tests"
    echo "=========================================="

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    print_info "Running frontend unit tests..."

    cd frontend/web-app

    if npm run test -- --run --reporter=verbose > /dev/null 2>&1; then
        print_status 0 "Frontend unit tests passed"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        print_status 1 "Frontend unit tests failed"
    fi

    cd ../..
else
    print_info "Skipping frontend tests (--skip-unit)"
fi

# ============================================
# Step 6: E2E Tests
# ============================================

if [ "$SKIP_E2E" = false ]; then
    echo ""
    echo "=========================================="
    echo "Step 6: Running E2E Tests"
    echo "=========================================="

    print_info "E2E tests require full application stack..."
    print_warning "E2E tests not yet implemented"
    print_info "Placeholder for E2E test execution"
else
    print_info "Skipping E2E tests (--skip-e2e)"
fi

# ============================================
# Final Summary
# ============================================

echo ""
echo "=========================================="
echo "Test Execution Summary"
echo "=========================================="
echo ""
echo "Total test suites: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $((TOTAL_TESTS - PASSED_TESTS))"
echo ""

if [ ${#FAILED[@]} -gt 0 ]; then
    echo -e "${RED}Failed test suites:${NC}"
    for failed in "${FAILED[@]}"; do
        echo "  - $failed"
    done
    echo ""
    exit 1
else
    echo -e "${GREEN}✅ All tests passed!${NC}"
    echo ""
    exit 0
fi
