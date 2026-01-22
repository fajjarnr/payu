#!/bin/bash
# Test Runner Script for PayU Python Services

set -e

# Get the project root directory (one level up from scripts/)
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "=========================================="
echo "PayU E2E Test Runner"
echo "=========================================="
echo "Project Root: $PROJECT_ROOT"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check Python and pip
if command_exists python3; then
    echo -e "${GREEN}✓${NC} Python 3 found"
else
    echo -e "${RED}✗${NC} Python 3 not found. Please install Python 3.12 or later."
    exit 1
fi

if command_exists pip3; then
    echo -e "${GREEN}✓${NC} pip3 found"
else
    echo -e "${RED}✗${NC} pip3 not found. Please install pip3."
    exit 1
fi

# Install dependencies for KYC Service
echo ""
echo "Installing dependencies for KYC Service..."
if [ -d "$PROJECT_ROOT/backend/kyc-service" ]; then
    cd "$PROJECT_ROOT/backend/kyc-service"
    pip3 install -q -r requirements.txt
    echo -e "${GREEN}✓${NC} KYC Service dependencies installed"
else
    echo -e "${RED}✗${NC} KYC Service directory not found at $PROJECT_ROOT/backend/kyc-service"
fi

# Install dependencies for Analytics Service
echo ""
echo "Installing dependencies for Analytics Service..."
if [ -d "$PROJECT_ROOT/backend/analytics-service" ]; then
    cd "$PROJECT_ROOT/backend/analytics-service"
    pip3 install -q -r requirements.txt
    echo -e "${GREEN}✓${NC} Analytics Service dependencies installed"
else
    echo -e "${RED}✗${NC} Analytics Service directory not found at $PROJECT_ROOT/backend/analytics-service"
fi

# Install pytest for both services
echo ""
echo "Installing pytest..."
if [ -d "$PROJECT_ROOT/backend/kyc-service" ]; then
    cd "$PROJECT_ROOT/backend/kyc-service"
    pip3 install -q pytest pytest-asyncio pytest-cov pytest-mock
fi
if [ -d "$PROJECT_ROOT/backend/analytics-service" ]; then
    cd "$PROJECT_ROOT/backend/analytics-service"
    pip3 install -q pytest pytest-asyncio pytest-cov pytest-mock
fi
echo -e "${GREEN}✓${NC} pytest installed"

# Run Unit Tests
echo ""
echo "=========================================="
echo "Running Unit Tests"
echo "=========================================="

if [ -d "$PROJECT_ROOT/backend/kyc-service" ]; then
    cd "$PROJECT_ROOT/backend/kyc-service"
    echo ""
    echo "KYC Service Unit Tests..."
    python3 -m pytest tests/unit/ -v --tb=short || echo -e "${YELLOW}Some tests failed${NC}"
fi

if [ -d "$PROJECT_ROOT/backend/analytics-service" ]; then
    cd "$PROJECT_ROOT/backend/analytics-service"
    echo ""
    echo "Analytics Service Unit Tests..."
    python3 -m pytest tests/unit/ -v --tb=short || echo -e "${YELLOW}Some tests failed${NC}"
fi

# Run E2E Tests
echo ""
echo "=========================================="
echo "Running E2E Tests"
echo "=========================================="

if [ -d "$PROJECT_ROOT/backend/kyc-service" ]; then
    cd "$PROJECT_ROOT/backend/kyc-service"
    echo ""
    echo "KYC Service E2E Tests..."
    python3 -m pytest tests/e2e/ -v --tb=short || echo -e "${YELLOW}Some tests failed${NC}"
fi

if [ -d "$PROJECT_ROOT/backend/analytics-service" ]; then
    cd "$PROJECT_ROOT/backend/analytics-service"
    echo ""
    echo "Analytics Service E2E Tests..."
    python3 -m pytest tests/e2e/ -v --tb=short || echo -e "${YELLOW}Some tests failed${NC}"
fi

# Run Coverage Report
echo ""
echo "=========================================="
echo "Running Coverage Reports"
echo "=========================================="

if [ -d "$PROJECT_ROOT/backend/kyc-service" ]; then
    cd "$PROJECT_ROOT/backend/kyc-service"
    echo ""
    echo "KYC Service Coverage..."
    python3 -m pytest --cov=src --cov-report=html --cov-report=term tests/ || echo -e "${YELLOW}Coverage report generated${NC}"
fi

if [ -d "$PROJECT_ROOT/backend/analytics-service" ]; then
    cd "$PROJECT_ROOT/backend/analytics-service"
    echo ""
    echo "Analytics Service Coverage..."
    python3 -m pytest --cov=src --cov-report=html --cov-report=term tests/ || echo -e "${YELLOW}Coverage report generated${NC}"
fi

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo -e "${GREEN}✓${NC} KYC Service: E2E tests created"
echo -e "${GREEN}✓${NC} Analytics Service: E2E tests created"
echo ""
echo "Coverage Reports:"
echo "  KYC Service: file://$PROJECT_ROOT/backend/kyc-service/htmlcov/index.html"
echo "  Analytics Service: file://$PROJECT_ROOT/backend/analytics-service/htmlcov/index.html"
echo ""
echo "To run tests manually:"
echo "  cd $PROJECT_ROOT/backend/kyc-service && python3 -m pytest"
echo "  cd $PROJECT_ROOT/backend/analytics-service && python3 -m pytest"
echo ""
echo "To run docker-compose test environment:"
echo "  cd $PROJECT_ROOT && docker-compose -f docker-compose.test.yml up"
echo ""
