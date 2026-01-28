#!/bin/bash
# PayU QA Test Runner
# Comprehensive test execution script for all backend services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Results tracking
declare -A RESULTS
TOTAL_TESTS=0
TOTAL_FAILED=0

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}PayU Digital Banking - QA Test Suite${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Services to test (Spring Boot services)
SPRING_BOOT_SERVICES=(
    "account-service"
    "auth-service"
    "transaction-service"
    "wallet-service"
    "investment-service"
    "lending-service"
    "compliance-service"
    "fx-service"
    "statement-service"
    "cms-service"
    "ab-testing-service"
)

# Quarkus services
QUARKUS_SERVICES=(
    "billing-service"
    "notification-service"
    "gateway-service"
    "support-service"
    "partner-service"
    "backoffice-service"
    "promotion-service"
    "api-portal-service"
)

# Python services
PYTHON_SERVICES=(
    "kyc-service"
    "analytics-service"
)

# Shared libraries
SHARED_LIBS=(
    "security-starter"
    "resilience-starter"
    "cache-starter"
)

# Function to test a Spring Boot service
test_spring_service() {
    local service=$1
    echo -e "${BLUE}Testing: $service${NC}"

    cd /home/ubuntu/payu/backend/$service

    # Run tests and capture results
    if mvn test -q 2>&1; then
        local output=$(mvn test 2>&1 | grep -E "Tests run:" | tail -1)
        local tests=$(echo "$output" | grep -oP 'Tests run: \K\d+' || echo "0")
        local failures=$(echo "$output" | grep -oP 'Failures: \K\d+' || echo "0")
        local errors=$(echo "$output" | grep -oP 'Errors: \K\d+' || echo "0")

        TOTAL_TESTS=$((TOTAL_TESTS + tests))
        TOTAL_FAILED=$((TOTAL_FAILED + failures + errors))

        if [ "$failures" -eq 0 ] && [ "$errors" -eq 0 ]; then
            echo -e "  ${GREEN}✓ PASSED${NC} ($tests tests)"
            RESULTS[$service]="PASS"
        else
            echo -e "  ${RED}✗ FAILED${NC} ($failures failures, $errors errors)"
            RESULTS[$service]="FAIL"
        fi
    else
        echo -e "  ${YELLOW}⚠ BUILD FAILED${NC}"
        RESULTS[$service]="BUILD_FAIL"
        TOTAL_FAILED=$((TOTAL_FAILED + 1))
    fi
    echo ""
}

# Function to test a Quarkus service
test_quarkus_service() {
    local service=$1
    echo -e "${BLUE}Testing: $service (Quarkus)${NC}"

    cd /home/ubuntu/payu/backend/$service

    # Quarkus uses ./mvnw
    if [ -f "./mvnw" ]; then
        if ./mvnw test -q 2>&1; then
            local output=$(./mvnw test 2>&1 | grep -E "Tests run:" | tail -1)
            local tests=$(echo "$output" | grep -oP 'Tests run: \K\d+' || echo "0")
            local failures=$(echo "$output" | grep -oP 'Failures: \K\d+' || echo "0")
            local errors=$(echo "$output" | grep -oP 'Errors: \K\d+' || echo "0")

            TOTAL_TESTS=$((TOTAL_TESTS + tests))
            TOTAL_FAILED=$((TOTAL_FAILED + failures + errors))

            if [ "$failures" -eq 0 ] && [ "$errors" -eq 0 ]; then
                echo -e "  ${GREEN}✓ PASSED${NC} ($tests tests)"
                RESULTS[$service]="PASS"
            else
                echo -e "  ${RED}✗ FAILED${NC} ($failures failures, $errors errors)"
                RESULTS[$service]="FAIL"
            fi
        else
            echo -e "  ${YELLOW}⚠ BUILD FAILED${NC}"
            RESULTS[$service]="BUILD_FAIL"
            TOTAL_FAILED=$((TOTAL_FAILED + 1))
        fi
    else
        # Fallback to mvn
        if mvn test -q 2>&1; then
            local output=$(mvn test 2>&1 | grep -E "Tests run:" | tail -1)
            local tests=$(echo "$output" | grep -oP 'Tests run: \K\d+' || echo "0")
            local failures=$(echo "$output" | grep -oP 'Failures: \K\d+' || echo "0")
            local errors=$(echo "$output" | grep -oP 'Errors: \K\d+' || echo "0")

            TOTAL_TESTS=$((TOTAL_TESTS + tests))
            TOTAL_FAILED=$((TOTAL_FAILED + failures + errors))

            if [ "$failures" -eq 0 ] && [ "$errors" -eq 0 ]; then
                echo -e "  ${GREEN}✓ PASSED${NC} ($tests tests)"
                RESULTS[$service]="PASS"
            else
                echo -e "  ${RED}✗ FAILED${NC} ($failures failures, $errors errors)"
                RESULTS[$service]="FAIL"
            fi
        else
            echo -e "  ${YELLOW}⚠ BUILD FAILED${NC}"
            RESULTS[$service]="BUILD_FAIL"
            TOTAL_FAILED=$((TOTAL_FAILED + 1))
        fi
    fi
    echo ""
}

# Function to test a Python service
test_python_service() {
    local service=$1
    echo -e "${BLUE}Testing: $service (Python)${NC}"

    cd /home/ubuntu/payu/backend/$service

    if [ -d ".venv" ]; then
        local output
        if output=$(source .venv/bin/activate && python -m pytest -v --tb=no 2>&1); then
            local passed=$(echo "$output" | grep -oP '\d+ passed' | grep -oP '\d+' || echo "0")
            local failed=$(echo "$output" | grep -oP '\d+ failed' | grep -oP '\d+' || echo "0")

            TOTAL_TESTS=$((TOTAL_TESTS + passed + failed))
            TOTAL_FAILED=$((TOTAL_FAILED + failed))

            if [ "$failed" -eq 0 ]; then
                echo -e "  ${GREEN}✓ PASSED${NC} ($passed tests)"
                RESULTS[$service]="PASS"
            else
                echo -e "  ${RED}✗ FAILED${NC} ($failed tests)"
                RESULTS[$service]="FAIL"
            fi
        else
            echo -e "  ${YELLOW}⚠ TEST EXECUTION FAILED${NC}"
            RESULTS[$service]="EXEC_FAIL"
            TOTAL_FAILED=$((TOTAL_FAILED + 1))
        fi
    else
        echo -e "  ${YELLOW}⚠ NO VENV FOUND${NC}"
        RESULTS[$service]="NO_VENV"
    fi
    echo ""
}

# Function to test shared library
test_shared_lib() {
    local lib=$1
    echo -e "${BLUE}Testing: shared/$lib (Spring Boot)${NC}"

    cd /home/ubuntu/payu/backend/shared/$lib

    if mvn test -q 2>&1; then
        local output=$(mvn test 2>&1 | grep -E "Tests run:" | tail -1)
        local tests=$(echo "$output" | grep -oP 'Tests run: \K\d+' || echo "0")
        local failures=$(echo "$output" | grep -oP 'Failures: \K\d+' || echo "0")
        local errors=$(echo "$output" | grep -oP 'Errors: \K\d+' || echo "0")

        TOTAL_TESTS=$((TOTAL_TESTS + tests))
        TOTAL_FAILED=$((TOTAL_FAILED + failures + errors))

        if [ "$failures" -eq 0 ] && [ "$errors" -eq 0 ]; then
            echo -e "  ${GREEN}✓ PASSED${NC} ($tests tests)"
            RESULTS["shared/$lib"]="PASS"
        else
            echo -e "  ${RED}✗ FAILED${NC} ($failures failures, $errors errors)"
            RESULTS["shared/$lib"]="FAIL"
        fi
    else
        echo -e "  ${YELLOW}⚠ BUILD FAILED${NC}"
        RESULTS["shared/$lib"]="BUILD_FAIL"
        TOTAL_FAILED=$((TOTAL_FAILED + 1))
    fi
    echo ""
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

echo -e "${YELLOW}Phase 1: Testing Spring Boot Services${NC}\n"
for service in "${SPRING_BOOT_SERVICES[@]}"; do
    if [ -d "/home/ubuntu/payu/backend/$service" ]; then
        test_spring_service "$service"
    else
        echo -e "${YELLOW}Skipping $service (not found)${NC}\n"
    fi
done

echo -e "${YELLOW}Phase 2: Testing Quarkus Services${NC}\n"
for service in "${QUARKUS_SERVICES[@]}"; do
    if [ -d "/home/ubuntu/payu/backend/$service" ]; then
        test_quarkus_service "$service"
    else
        echo -e "${YELLOW}Skipping $service (not found)${NC}\n"
    fi
done

echo -e "${YELLOW}Phase 3: Testing Python Services${NC}\n"
for service in "${PYTHON_SERVICES[@]}"; do
    if [ -d "/home/ubuntu/payu/backend/$service" ]; then
        test_python_service "$service"
    else
        echo -e "${YELLOW}Skipping $service (not found)${NC}\n"
    fi
done

echo -e "${YELLOW}Phase 4: Testing Shared Libraries${NC}\n"
for lib in "${SHARED_LIBS[@]}"; do
    if [ -d "/home/ubuntu/payu/backend/shared/$lib" ]; then
        test_shared_lib "$lib"
    else
        echo -e "${YELLOW}Skipping shared/$lib (not found)${NC}\n"
    fi
done

# ============================================================================
# SUMMARY REPORT
# ============================================================================

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}QA Test Summary Report${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

echo -e "${YELLOW}Test Results:${NC}"
echo ""

# Count results
PASS_COUNT=0
FAIL_COUNT=0
BUILD_FAIL_COUNT=0
SKIP_COUNT=0

for key in "${!RESULTS[@]}"; do
    case "${RESULTS[$key]}" in
        "PASS")
            echo -e "  ${GREEN}✓${NC} $key"
            ((PASS_COUNT++))
            ;;
        "FAIL")
            echo -e "  ${RED}✗${NC} $key"
            ((FAIL_COUNT++))
            ;;
        "BUILD_FAIL")
            echo -e "  ${YELLOW}⚠${NC} $key (BUILD FAILED)"
            ((BUILD_FAIL_COUNT++))
            ;;
        "NO_VENV")
            echo -e "  ${YELLOW}⚠${NC} $key (NO VENV)"
            ((SKIP_COUNT++))
            ;;
        *)
            echo -e "  ${GRAY}○${NC} $key (NOT TESTED)"
            ((SKIP_COUNT++))
            ;;
    esac
done

echo ""
echo -e "${YELLOW}Statistics:${NC}"
echo "  Total Tests:  $TOTAL_TESTS"
echo -e "  ${GREEN}Passed Services: $PASS_COUNT${NC}"
echo -e "  ${RED}Failed Services: $FAIL_COUNT${NC}"
echo -e "  ${YELLOW}Build Failures: $BUILD_FAIL_COUNT${NC}"
echo -e "  ${GRAY}Skipped: $SKIP_COUNT${NC}"
echo ""

# Overall status
if [ $TOTAL_FAILED -eq 0 ] && [ $BUILD_FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ ALL TESTS PASSED${NC}"
    exit 0
elif [ $TOTAL_FAILED -eq 0 ]; then
    echo -e "${YELLOW}⚠ SOME SERVICES FAILED TO BUILD (tests not run)${NC}"
    exit 1
else
    echo -e "${RED}✗ SOME TESTS FAILED${NC}"
    exit 1
fi
