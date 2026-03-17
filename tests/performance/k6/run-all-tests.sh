#!/bin/bash
#
# PayU K6 Load Testing Suite - Automated Execution Script
# ========================================================
#
# Usage:
#   chmod +x run-all-tests.sh
#   ./run-all-tests.sh              # Run all tests
#   ./run-all-tests.sh --smoke      # Run smoke test only
#   ./run-all-tests.sh --load       # Run load test only
#   ./run-all-tests.sh --stress     # Run stress test only
#   ./run-all-tests.sh --crud       # Run CRUD tests only
#   ./run-all-tests.sh --consistency # Run data consistency test
#   ./run-all-tests.sh --local      # Use local endpoints
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

print_section() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

print_subsection() {
    echo ""
    echo -e "${CYAN}───────────────────────────────────────────────────────────────${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}───────────────────────────────────────────────────────────────${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    print_section "Checking Prerequisites"

    if ! command -v k6 &> /dev/null; then
        print_error "k6 is not installed. Please install k6 first."
        echo "  Ubuntu/Debian: sudo apt-get install k6"
        echo "  Other: https://k6.io/docs/getting-started/installation/"
        exit 1
    fi

    print_success "k6 installed: $(k6 version)"

    # Check endpoint availability
    local config_file="${1:-config.js}"
    local gateway_url=$(grep -o "gateway: '[^']*'" "$config_file" 2>/dev/null | cut -d"'" -f2 || echo "unknown")

    print_section "Checking Endpoint Availability"
    echo "Gateway URL: $gateway_url"

    if curl -s --max-time 5 "$gateway_url/q/health" > /dev/null 2>&1; then
        print_success "Gateway is accessible"
    else
        print_warning "Gateway is not accessible (status: $?)"
        echo "Continuing anyway - test will validate connectivity"
    fi
}

# Run basic smoke test
run_smoke_test() {
    print_section "Running Basic Smoke Test"
    echo "Duration: ~30 seconds"
    echo "Virtual Users: 1"
    echo "Coverage: Health endpoints only"
    echo ""

    k6 run smoke-test.js

    if [ $? -eq 0 ]; then
        print_success "Smoke test completed"
        return 0
    else
        print_error "Smoke test failed"
        return 1
    fi
}

# Run CRUD smoke test
run_crud_smoke_test() {
    print_section "Running CRUD Smoke Test"
    echo "Duration: ~2 minutes"
    echo "Virtual Users: 1"
    echo "Coverage: Full CRUD operations (Account, Wallet, Transaction, Card)"
    echo ""

    # Create a minimal CRUD test for smoke testing
    cat > crud-smoke-test.js << 'EOF'
import { login, getProfile } from './lib/auth.js';
import { getWallets } from './lib/wallet.js';
import { BASE_URLS, TEST_USERS } from './config.js';

export const options = {
    stages: [{ duration: '30s', target: 1 }],
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        http_req_failed: ['rate<0.1']
    }
};

export default function () {
    const token = login(BASE_URLS.keycloak, TEST_USERS[0].username, TEST_USERS[0].password);
    if (token) {
        getProfile(BASE_URLS.gateway, token);
        getWallets(BASE_URLS.gateway, token);
    }
}
EOF

    k6 run crud-smoke-test.js
    rm crud-smoke-test.js

    if [ $? -eq 0 ]; then
        print_success "CRUD smoke test completed"
        return 0
    else
        print_error "CRUD smoke test failed"
        return 1
    fi
}

# Run load test
run_load_test() {
    print_section "Running Basic Load Test"
    echo "Duration: ~25 minutes"
    echo "Max Virtual Users: 100"
    echo "Coverage: Health endpoints"
    echo ""
    echo "Press Ctrl+C within 5 seconds to cancel..."
    sleep 5

    local timestamp=$(date +%Y%m%d_%H%M%S)
    k6 run load-test.js --out "json=results/load-test-${timestamp}.json"

    if [ $? -eq 0 ]; then
        print_success "Load test completed - results saved to results/load-test-${timestamp}.json"
        return 0
    else
        print_error "Load test failed"
        return 1
    fi
}

# Run CRUD load test
run_crud_load_test() {
    print_section "Running CRUD Load Test"
    echo "Duration: ~25 minutes"
    echo "Max Virtual Users: 100"
    echo "Coverage: Full CRUD operations"
    echo "  - Account: READ, UPDATE"
    echo "  - Wallet/Pocket: CREATE, READ, UPDATE, DELETE"
    echo "  - Transaction: CREATE, READ (transfer)"
    echo "  - Card: CREATE, READ, UPDATE"
    echo ""
    echo "Press Ctrl+C within 5 seconds to cancel..."
    sleep 5

    local timestamp=$(date +%Y%m%d_%H%M%S)
    k6 run crud-load-test.js --out "json=results/crud-load-test-${timestamp}.json"

    if [ $? -eq 0 ]; then
        print_success "CRUD load test completed - results saved to results/crud-load-test-${timestamp}.json"
        return 0
    else
        print_error "CRUD load test failed"
        return 1
    fi
}

# Run stress test
run_stress_test() {
    print_section "Running Basic Stress Test"
    echo "Duration: ~40 minutes"
    echo "Max Virtual Users: 1000"
    echo "Coverage: Health endpoints to breaking point"
    echo ""
    echo "⚠️  WARNING: This test will push the system to its limits!"
    echo "Press Ctrl+C within 5 seconds to cancel..."
    sleep 5

    local timestamp=$(date +%Y%m%d_%H%M%S)
    k6 run stress-test.js --out "json=results/stress-test-${timestamp}.json"

    if [ $? -eq 0 ]; then
        print_success "Stress test completed - results saved to results/stress-test-${timestamp}.json"
        return 0
    else
        print_error "Stress test failed (this may be expected if breaking point was found)"
        return 0
    fi
}

# Run CRUD stress test
run_crud_stress_test() {
    print_section "Running CRUD Stress Test"
    echo "Duration: ~40 minutes"
    echo "Max Virtual Users: 1000"
    echo "Coverage: Full CRUD operations to breaking point"
    echo ""
    echo "⚠️  WARNING: This test will push the database to its limits!"
    echo "Press Ctrl+C within 5 seconds to cancel..."
    sleep 5

    local timestamp=$(date +%Y%m%d_%H%M%S)
    k6 run crud-stress-test.js --out "json=results/crud-stress-test-${timestamp}.json"

    if [ $? -eq 0 ]; then
        print_success "CRUD stress test completed - results saved to results/crud-stress-test-${timestamp}.json"
        return 0
    else
        print_error "CRUD stress test failed (this may be expected if breaking point was found)"
        return 0
    fi
}

# Run data consistency test
run_consistency_test() {
    print_section "Running Data Consistency Test"
    echo "Duration: ~25 minutes"
    echo "Max Virtual Users: 50"
    echo "Coverage:"
    echo "  - Read-after-write consistency"
    echo "  - Transaction atomicity"
    echo "  - Concurrent update detection"
    echo ""
    echo "Press Ctrl+C within 5 seconds to cancel..."
    sleep 5

    local timestamp=$(date +%Y%m%d_%H%M%S)
    k6 run crud-data-consistency-test.js --out "json=results/consistency-test-${timestamp}.json"

    if [ $? -eq 0 ]; then
        print_success "Data consistency test completed - results saved to results/consistency-test-${timestamp}.json"
        return 0
    else
        print_error "Data consistency test failed"
        return 1
    fi
}

# Setup local config
use_local_config() {
    print_section "Switching to Local Config"

    if [ -f "config.js" ] && [ ! -f "config.openshift.js" ]; then
        cp config.js config.openshift.js
        print_success "Backed up config.js to config.openshift.js"
    fi

    if [ -f "config.local.js" ]; then
        cp config.local.js config.js
        print_success "Switched to local config"
    else
        print_error "config.local.js not found"
        exit 1
    fi
}

# Restore OpenShift config
restore_openshift_config() {
    if [ -f "config.openshift.js" ]; then
        cp config.openshift.js config.js
        print_success "Restored OpenShift config"
    fi
}

# Run all tests
run_all_tests() {
    print_section "Starting Full Test Suite"
    echo "Total estimated time: ~120 minutes"
    echo ""

    # Create results directory
    mkdir -p results

    # 1. Basic smoke test
    print_subsection "Phase 1: Basic Smoke Test"
    if ! run_smoke_test; then
        print_error "Basic smoke test failed - aborting full suite"
        exit 1
    fi

    # 2. CRUD smoke test
    print_subsection "Phase 2: CRUD Smoke Test"
    if ! run_crud_smoke_test; then
        print_error "CRUD smoke test failed - aborting full suite"
        exit 1
    fi

    # 3. Basic load test
    print_subsection "Phase 3: Basic Load Test (~25 min)"
    run_load_test

    # 4. CRUD load test
    print_subsection "Phase 4: CRUD Load Test (~25 min)"
    run_crud_load_test

    # 5. Data consistency test
    print_subsection "Phase 5: Data Consistency Test (~25 min)"
    run_consistency_test

    # 6. CRUD stress test
    print_subsection "Phase 6: CRUD Stress Test (~40 min)"
    run_crud_stress_test

    print_section "Full Test Suite Complete"
    echo "Results saved in: $(pwd)/results/"
    echo ""
    echo "Generated files:"
    ls -lh results/*.json 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}'
}

# Run CRUD-only tests
run_crud_tests() {
    print_section "Running CRUD Tests Only"
    echo "Total estimated time: ~90 minutes"
    echo ""

    mkdir -p results

    # 1. CRUD smoke test
    if ! run_crud_smoke_test; then
        print_error "CRUD smoke test failed - aborting"
        exit 1
    fi

    # 2. CRUD load test
    run_crud_load_test

    # 3. Data consistency test
    run_consistency_test

    # 4. CRUD stress test (optional)
    read -p "Run CRUD stress test (~40 min)? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        run_crud_stress_test
    fi

    print_section "CRUD Tests Complete"
}

# Create results directory
mkdir -p results

# Main execution
main() {
    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║     PayU K6 Load Testing Suite - Complete CRUD Edition        ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    local config_file="config.js"
    local test_mode="all"

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --smoke)
                test_mode="smoke"
                shift
                ;;
            --load)
                test_mode="load"
                shift
                ;;
            --stress)
                test_mode="stress"
                shift
                ;;
            --crud)
                test_mode="crud"
                shift
                ;;
            --consistency)
                test_mode="consistency"
                shift
                ;;
            --local)
                use_local_config
                config_file="config.local.js"
                shift
                ;;
            --help|-h)
                echo "Usage: ./run-all-tests.sh [OPTIONS]"
                echo ""
                echo "Basic Tests:"
                echo "  (none)           Run all tests (basic + CRUD)"
                echo "  --smoke          Run smoke tests (basic + CRUD)"
                echo "  --load           Run basic load test only"
                echo "  --stress         Run basic stress test only"
                echo ""
                echo "CRUD Tests:"
                echo "  --crud           Run all CRUD tests"
                echo "  --consistency    Run data consistency test only"
                echo ""
                echo "Environment:"
                echo "  --local          Use local endpoints (localhost:8080)"
                echo "  --help           Show this help message"
                echo ""
                echo "Test Coverage:"
                echo "  Basic Tests:     Health endpoints, Keycloak auth"
                echo "  CRUD Tests:      Account, Wallet, Transaction, Card operations"
                echo "  Consistency:     Read-after-write, atomicity, concurrent updates"
                echo ""
                echo "Individual Test Files:"
                echo "  k6 run smoke-test.js              # Basic smoke (30s)"
                echo "  k6 run crud-load-test.js          # CRUD load (25min)"
                echo "  k6 run crud-stress-test.js        # CRUD stress (40min)"
                echo "  k6 run crud-data-consistency-test.js  # Consistency (25min)"
                echo ""
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done

    # Check prerequisites
    check_prerequisites "$config_file"

    # Run tests based on mode
    case $test_mode in
        smoke)
            run_smoke_test && run_crud_smoke_test
            ;;
        load)
            run_load_test
            ;;
        stress)
            run_stress_test
            ;;
        crud)
            run_crud_tests
            ;;
        consistency)
            run_consistency_test
            ;;
        all)
            run_all_tests
            ;;
    esac

    # Cleanup
    restore_openshift_config

    echo ""
    echo -e "${GREEN}Done!${NC}"
    echo ""
}

# Handle Ctrl+C
trap 'echo ""; print_warning "Test interrupted by user"; restore_openshift_config; exit 130' INT

main "$@"
