#!/bin/bash
# E2E Test Execution Script
# This script runs the E2E test suite with various options

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
TEST_TYPE="all"
VERBOSE=false
STOP_ON_FAIL=false
COVERAGE=false

# Help function
show_help() {
    echo "E2E Test Execution Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -t, --type TYPE       Test type to run (all, smoke, critical, investment, lending, promotion, compliance, support, partner, analytics, backoffice, journey)"
    echo "  -v, --verbose         Enable verbose output"
    echo "  -x, --stop-on-fail    Stop on first failure"
    echo "  -c, --coverage        Run with coverage report"
    echo "  -h, --help            Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                    # Run all tests"
    echo "  $0 -t smoke           # Run smoke tests only"
    echo "  $0 -t journey -v      # Run user journey tests with verbose output"
    echo "  $0 -c                 # Run all tests with coverage"
    exit 0
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            TEST_TYPE="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -x|--stop-on-fail)
            STOP_ON_FAIL=true
            shift
            ;;
        -c|--coverage)
            COVERAGE=true
            shift
            ;;
        -h|--help)
            show_help
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_help
            ;;
    esac
done

# Change to test directory
cd "$(dirname "$0")"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}PayU E2E Test Suite${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if dependencies are installed
echo -e "${YELLOW}Checking dependencies...${NC}"
if ! python -c "import pytest" 2>/dev/null; then
    echo -e "${RED}pytest not installed. Installing...${NC}"
    pip install -r requirements.txt
fi

if ! python -c "import faker" 2>/dev/null; then
    echo -e "${RED}faker not installed. Installing...${NC}"
    pip install -r requirements.txt
fi

echo -e "${GREEN}Dependencies check complete${NC}"
echo ""

# Build pytest command
PYTEST_CMD="pytest -v"

if [ "$VERBOSE" = true ]; then
    PYTEST_CMD="$PYTEST_CMD -s"
fi

if [ "$STOP_ON_FAIL" = true ]; then
    PYTEST_CMD="$PYTEST_CMD -x"
fi

if [ "$COVERAGE" = true ]; then
    PYTEST_CMD="$PYTEST_CMD --cov=. --cov-report=html --cov-report=term"
fi

# Run tests based on type
case $TEST_TYPE in
    all)
        echo -e "${BLUE}Running all E2E tests...${NC}"
        $PYTEST_CMD
        ;;
    smoke)
        echo -e "${BLUE}Running smoke tests...${NC}"
        $PYTEST_CMD -m smoke
        ;;
    critical)
        echo -e "${BLUE}Running critical tests...${NC}"
        $PYTEST_CMD -m critical
        ;;
    investment)
        echo -e "${BLUE}Running investment flow tests...${NC}"
        $PYTEST_CMD test_investment_flow.py
        ;;
    lending)
        echo -e "${BLUE}Running lending flow tests...${NC}"
        $PYTEST_CMD test_lending_flow.py
        ;;
    promotion)
        echo -e "${BLUE}Running promotion flow tests...${NC}"
        $PYTEST_CMD test_promotion_flow.py
        ;;
    compliance)
        echo -e "${BLUE}Running compliance flow tests...${NC}"
        $PYTEST_CMD test_compliance_flow.py
        ;;
    support)
        echo -e "${BLUE}Running support flow tests...${NC}"
        $PYTEST_CMD test_support_flow.py
        ;;
    partner)
        echo -e "${BLUE}Running partner flow tests...${NC}"
        $PYTEST_CMD test_partner_flow.py
        ;;
    analytics)
        echo -e "${BLUE}Running analytics flow tests...${NC}"
        $PYTEST_CMD test_analytics_flow.py
        ;;
    backoffice)
        echo -e "${BLUE}Running backoffice flow tests...${NC}"
        $PYTEST_CMD test_backoffice.py
        ;;
    journey)
        echo -e "${BLUE}Running complete user journey tests...${NC}"
        $PYTEST_CMD test_complete_user_journey.py
        ;;
    *)
        echo -e "${RED}Unknown test type: $TEST_TYPE${NC}"
        echo "Valid types: all, smoke, critical, investment, lending, promotion, compliance, support, partner, analytics, backoffice, journey"
        exit 1
        ;;
esac

# Check exit code
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}All tests passed!${NC}"
    echo -e "${GREEN}========================================${NC}"
    
    if [ "$COVERAGE" = true ]; then
        echo ""
        echo -e "${BLUE}Coverage report generated in htmlcov/index.html${NC}"
    fi
    
    exit 0
else
    echo ""
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}Some tests failed!${NC}"
    echo -e "${RED}========================================${NC}"
    exit 1
fi
