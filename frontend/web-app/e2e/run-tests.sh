#!/bin/bash

# E2E Test Runner with Analysis
# This script runs the E2E tests and provides detailed analysis

set -e

echo "========================================"
echo "PayU E2E Test Runner"
echo "========================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Parse arguments
TEST_FILE=""
PROJECT="chromium"
REPORTER="list"

while [[ $# -gt 0 ]]; do
    case $1 in
        --file|-f)
            TEST_FILE="$2"
            shift 2
            ;;
        --project|-p)
            PROJECT="$2"
            shift 2
            ;;
        --reporter|-r)
            REPORTER="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Build test command
if [ -n "$TEST_FILE" ]; then
    TEST_CMD="npx playwright test $TEST_FILE"
else
    TEST_CMD="npx playwright test"
fi

TEST_CMD="$TEST_CMD --project=$PROJECT --reporter=$REPORTER"

echo "Running: $TEST_CMD"
echo ""

# Run tests
cd /home/ubuntu/payu/frontend/web-app

if eval "$TEST_CMD"; then
    echo ""
    echo -e "${GREEN}✓ Tests passed successfully!${NC}"
    exit 0
else
    EXIT_CODE=$?
    echo ""
    echo -e "${RED}✗ Some tests failed with exit code: $EXIT_CODE${NC}"

    # Check for common failure patterns
    echo ""
    echo "Analyzing failures..."
    echo ""

    # Look for timeout errors
    if grep -q "Timeout.*exceeded" playwright-report/index.html 2>/dev/null; then
        echo -e "${YELLOW}⚠ Timeout errors detected - consider increasing timeouts${NC}"
    fi

    # Look for selector errors
    if grep -q "locator.*click" playwright-report/index.html 2>/dev/null; then
        echo -e "${YELLOW}⚠ Selector errors detected - check element selectors${NC}"
    fi

    # Look for a11y violations
    if grep -q "color-contrast" playwright-report/index.html 2>/dev/null; then
        echo -e "${YELLOW}⚠ Color contrast issues detected - these are tracked as design debt${NC}"
    fi

    exit $EXIT_CODE
fi
