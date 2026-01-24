#!/bin/bash
set -e

# ============================================
# PayU - Test Single Service Script
# Run tests for a specific service
# ============================================

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
    fi
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check service name argument
if [ -z "$1" ]; then
    echo "Usage: $0 <service-name>"
    echo ""
    echo "Available services:"
    echo "  account-service"
    echo "  auth-service"
    echo "  transaction-service"
    echo "  wallet-service"
    echo "  billing-service"
    echo "  notification-service"
    echo "  gateway-service"
    echo "  kyc-service"
    echo "  analytics-service"
    echo "  web-app"
    echo ""
    exit 1
fi

SERVICE_NAME=$1
SERVICE_PATH=""

# Determine service path
if [[ "$SERVICE_NAME" == *"web-app"* ]] || [[ "$SERVICE_NAME" == "frontend" ]]; then
    SERVICE_PATH="frontend/web-app"
else
    SERVICE_PATH="backend/$SERVICE_NAME"
fi

# Check if service exists
if [ ! -d "$SERVICE_PATH" ]; then
    echo -e "${RED}Error:${NC} Service '$SERVICE_NAME' not found at $SERVICE_PATH"
    exit 1
fi

echo "=========================================="
echo "Testing: $SERVICE_NAME"
echo "=========================================="
echo ""

cd "$SERVICE_PATH"

# Detect service type and run appropriate tests
if [ -f "pom.xml" ]; then
    # Java/Maven service
    print_info "Detected Maven service"

    if [ -f "mvnw" ]; then
        MVN_CMD="./mvnw"
    else
        MVN_CMD="mvn"
    fi

    print_info "Running unit tests..."
    if $MVN_CMD test -q; then
        print_status 0 "Unit tests passed"
    else
        print_status 1 "Unit tests failed"
        exit 1
    fi

    print_info "Generating coverage report..."
    if $MVN_CMD jacoco:report -q > /dev/null 2>&1; then
        print_status 0 "Coverage report generated at target/site/jacoco/index.html"
    else
        print_warning "Could not generate coverage report"
    fi

elif [ -f "requirements.txt" ] || [ -f "pyproject.toml" ]; then
    # Python service
    print_info "Detected Python service"

    print_info "Running unit tests..."
    if pytest -v; then
        print_status 0 "Unit tests passed"
    else
        print_status 1 "Unit tests failed"
        exit 1
    fi

    print_info "Generating coverage report..."
    if pytest --cov=. --cov-report=html --cov-report=term; then
        print_status 0 "Coverage report generated at htmlcov/index.html"
    else
        print_warning "Could not generate coverage report"
    fi

elif [ -f "package.json" ]; then
    # Node.js service
    print_info "Detected Node.js service"

    print_info "Running unit tests..."
    if npm run test; then
        print_status 0 "Unit tests passed"
    else
        print_status 1 "Unit tests failed"
        exit 1
    fi

    print_info "Running type check..."
    if npm run type-check > /dev/null 2>&1; then
        print_status 0 "Type check passed"
    else
        print_warning "Type check failed"
    fi

    print_info "Running lint..."
    if npm run lint > /dev/null 2>&1; then
        print_status 0 "Lint passed"
    else
        print_warning "Lint failed"
    fi
else
    echo -e "${RED}Error:${NC} Unknown service type"
    exit 1
fi

cd ../..

echo ""
echo "=========================================="
echo -e "${GREEN}✅ $SERVICE_NAME tests completed!${NC}"
echo "=========================================="
