#!/bin/bash
#
# Zero-Downtime Deployment Test Script
# ====================================
# Tests that deployments happen without service interruption
#
# Usage: ./test-zero-downtime.sh <service-name> [duration-seconds]
# Example: ./test-zero-downtime.sh gateway-service 300

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${NAMESPACE:-payu-dev}"
SERVICE="${1:-gateway-service}"
DURATION="${2:-300}"  # Default 5 minutes

# Test configuration
REQUEST_INTERVAL=1     # Seconds between requests
CONCURRENT_REQUESTS=5  # Parallel requests
ERROR_THRESHOLD=5      # Max errors allowed

# Results
REQUESTS_SENT=0
REQUESTS_SUCCESS=0
REQUESTS_FAILED=0
ERRORS_DURING_DEPLOYMENT=0
MAX_RESPONSE_TIME=0
DEPLOYMENT_START_TIME=""
DEPLOYMENT_END_TIME=""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Zero-Downtime Deployment Test${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo "Service: ${SERVICE}"
    echo "Namespace: ${NAMESPACE}"
    echo "Duration: ${DURATION} seconds"
    echo "Concurrent Requests: ${CONCURRENT_REQUESTS}"
    echo ""
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

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

get_service_url() {
    # Get route URL for service
    SERVICE_URL=$(oc get route "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.spec.host}' 2>/dev/null || echo "")

    if [ -z "$SERVICE_URL" ]; then
        print_error "No route found for service ${SERVICE}"
        exit 1
    fi

    echo "https://${SERVICE_URL}"
}

send_request() {
    local url=$1
    local start_time end_time response_time

    start_time=$(date +%s%N)

    # Send request with timeout
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}\n%{time_total}" \
        --max-time 10 \
        -H "Accept: application/json" \
        "${url}/actuator/health" 2>/dev/null || echo -e "000\n0")

    end_time=$(date +%s%N)
    response_time=$(( (end_time - start_time) / 1000000 ))  # Convert to ms

    echo "${HTTP_CODE},${response_time}"
}

test_single_request() {
    local url=$1
    local result

    result=$(send_request "$url")
    local http_code=$(echo "$result" | cut -d',' -f1)
    local response_time=$(echo "$result" | cut -d',' -f2)

    REQUESTS_SENT=$((REQUESTS_SENT + 1))

    if [ "$http_code" = "200" ] || [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        REQUESTS_SUCCESS=$((REQUESTS_SUCCESS + 1))
        if [ "${response_time%.*}" -gt "$MAX_RESPONSE_TIME" ]; then
            MAX_RESPONSE_TIME="${response_time%.*}"
        fi
        return 0
    else
        REQUESTS_FAILED=$((REQUESTS_FAILED + 1))

        # Check if during deployment
        if [ -n "$DEPLOYMENT_START_TIME" ] && [ -z "$DEPLOYMENT_END_TIME" ]; then
            ERRORS_DURING_DEPLOYMENT=$((ERRORS_DURING_DEPLOYMENT + 1))
        fi

        return 1
    fi
}

continuous_test() {
    local url=$1
    local duration=$2
    local end_time

    end_time=$(($(date +%s) + duration))

    print_info "Starting continuous load test..."
    print_info "Press Ctrl+C to stop early"
    echo ""

    while [ $(date +%s) -lt $end_time ]; do
        # Run concurrent requests
        for i in $(seq 1 $CONCURRENT_REQUESTS); do
            test_single_request "$url" &
        done

        wait

        # Progress indicator
        if [ $((REQUESTS_SENT % 50)) -eq 0 ]; then
            echo -ne "\rRequests: ${REQUESTS_SENT} | Success: ${REQUESTS_SUCCESS} | Failed: ${REQUESTS_FAILED}"
        fi

        sleep $REQUEST_INTERVAL
    done

    echo ""
}

trigger_deployment() {
    local service=$1

    print_info "Triggering deployment for ${service}..."

    # Record start time
    DEPLOYMENT_START_TIME=$(date +%s)

    # Trigger rollout (this could be replaced with actual deployment command)
    if oc get deployment "${service}" -n "${NAMESPACE}" >/dev/null 2>&1; then
        oc rollout restart "deployment/${service}" -n "${NAMESPACE}"

        # Wait for deployment to complete
        if oc rollout status "deployment/${service}" -n "${NAMESPACE}" --timeout=300s; then
            DEPLOYMENT_END_TIME=$(date +%s)
            print_success "Deployment completed"
        else
            DEPLOYMENT_END_TIME=$(date +%s)
            print_error "Deployment failed or timed out"
            return 1
        fi
    else
        print_warning "Deployment not found, simulating deployment..."
        sleep 5
        DEPLOYMENT_END_TIME=$(date +%s)
    fi
}

calculate_statistics() {
    local duration=$1

    SUCCESS_RATE=0
    if [ $REQUESTS_SENT -gt 0 ]; then
        SUCCESS_RATE=$(( (REQUESTS_SUCCESS * 100) / REQUESTS_SENT ))
    fi

    AVG_REQUESTS_PER_SEC=0
    if [ $duration -gt 0 ]; then
        AVG_REQUESTS_PER_SEC=$(( REQUESTS_SENT / duration ))
    fi

    DEPLOYMENT_DURATION=0
    if [ -n "$DEPLOYMENT_START_TIME" ] && [ -n "$DEPLOYMENT_END_TIME" ]; then
        DEPLOYMENT_DURATION=$((DEPLOYMENT_END_TIME - DEPLOYMENT_START_TIME))
    fi
}

print_results() {
    local duration=$1

    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Test Results${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo "Test Duration:        ${duration} seconds"
    echo "Total Requests:       ${REQUESTS_SENT}"
    echo "Successful:           ${REQUESTS_SUCCESS} (${SUCCESS_RATE}%)"
    echo "Failed:               ${REQUESTS_FAILED}"
    echo "Max Response Time:    ${MAX_RESPONSE_TIME}ms"
    echo "Avg Requests/sec:     ${AVG_REQUESTS_PER_SEC}"
    echo ""

    if [ $ERRORS_DURING_DEPLOYMENT -gt 0 ]; then
        echo -e "Errors During Deploy: ${RED}${ERRORS_DURING_DEPLOYMENT}${NC}"
        echo "Deployment Duration:  ${DEPLOYMENT_DURATION}s"
    else
        echo -e "Errors During Deploy: ${GREEN}0${NC}"
        if [ $DEPLOYMENT_DURATION -gt 0 ]; then
            echo "Deployment Duration:  ${DEPLOYMENT_DURATION}s"
        fi
    fi

    echo ""
    echo -e "${BLUE}========================================${NC}"

    # Determine pass/fail
    if [ $SUCCESS_RATE -ge 99 ] && [ $ERRORS_DURING_DEPLOYMENT -eq 0 ]; then
        echo -e "${GREEN}✓ ZERO-DOWNTIME TEST PASSED${NC}"
        echo "  Service maintained availability during deployment"
        return 0
    elif [ $SUCCESS_RATE -ge 95 ] && [ $ERRORS_DURING_DEPLOYMENT -le 2 ]; then
        echo -e "${YELLOW}⚠ ZERO-DOWNTIME TEST PASSED WITH WARNINGS${NC}"
        echo "  Minor interruptions detected but within acceptable limits"
        return 0
    else
        echo -e "${RED}✗ ZERO-DOWNTIME TEST FAILED${NC}"
        echo "  Service experienced downtime during deployment"
        return 1
    fi
}

run_blue_green_test() {
    local url=$1

    print_info "Testing Blue-Green deployment pattern..."
    print_info "This will switch traffic between blue and green environments"
    echo ""

    # Detect current active version
    CURRENT_SELECTOR=$(oc get route "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.spec.to.name}' 2>/dev/null || echo "")

    if [[ "$CURRENT_SELECTOR" == *"-blue"* ]]; then
        CURRENT="blue"
        TARGET="green"
    elif [[ "$CURRENT_SELECTOR" == *"-green"* ]]; then
        CURRENT="green"
        TARGET="blue"
    else
        print_warning "No blue/green deployment detected, using standard deployment"
        return 1
    fi

    print_info "Current: ${CURRENT}, Target: ${TARGET}"

    # Start load test in background
    continuous_test "$url" "$DURATION" &
    local test_pid=$!

    # Wait a moment for test to establish baseline
    sleep 5

    # Trigger traffic switch
    print_info "Switching traffic to ${TARGET}..."
    DEPLOYMENT_START_TIME=$(date +%s)

    oc patch route "${SERVICE}" -n "${NAMESPACE}" -p \
        "{\"spec\":{\"to\":{\"name\":\"${SERVICE}-${TARGET}\"}}}"

    DEPLOYMENT_END_TIME=$(date +%s)

    # Wait for test to complete
    wait $test_pid

    return 0
}

run_canary_test() {
    local url=$1

    print_info "Testing Canary deployment pattern..."
    print_info "This will progressively shift traffic to canary version"
    echo ""

    # Check if canary deployment exists
    if ! oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" >/dev/null 2>&1; then
        print_warning "No canary deployment found"
        return 1
    fi

    # Start load test in background
    continuous_test "$url" "$DURATION" &
    local test_pid=$!

    sleep 5

    # Progressive traffic shift
    local percentages=(10 25 50 75 100)

    for pct in "${percentages[@]}"; do
        print_info "Shifting traffic to ${pct}% canary..."
        DEPLOYMENT_START_TIME=$(date +%s)

        if oc get virtualservice "${SERVICE}" -n "${NAMESPACE}" >/dev/null 2>&1; then
            # Istio VirtualService
            oc patch virtualservice "${SERVICE}" -n "${NAMESPACE}" --type='json' -p "[{
                \"op\": \"replace\",
                \"path\": \"/spec/http/0/route\",
                \"value\": [
                    {\"destination\": {\"host\": \"${SERVICE}\"}, \"weight\": $((100 - pct))},
                    {\"destination\": {\"host\": \"${SERVICE}-canary\"}, \"weight\": ${pct}}
                ]
            }]"
        else
            # OpenShift Route
            oc patch route "${SERVICE}" -n "${NAMESPACE}" -p "{
                \"spec\": {
                    \"to\": {\"weight\": $((100 - pct))},
                    \"alternateBackends\": [{\"kind\": \"Service\", \"name\": \"${SERVICE}-canary\", \"weight\": ${pct}}]
                }
            }"
        fi

        DEPLOYMENT_END_TIME=$(date +%s)

        # Wait between shifts
        sleep 30
    done

    wait $test_pid
    return 0
}

main() {
    print_header

    # Validate inputs
    if ! command -v oc &> /dev/null; then
        print_error "OpenShift CLI (oc) not found"
        exit 1
    fi

    if ! oc whoami &> /dev/null; then
        print_error "Not logged into OpenShift"
        exit 1
    fi

    # Get service URL
    local url
    url=$(get_service_url)
    print_info "Target URL: ${url}"

    # Check which test to run
    if [ -n "${TEST_TYPE:-}" ]; then
        case "$TEST_TYPE" in
            blue-green)
                run_blue_green_test "$url"
                ;;
            canary)
                run_canary_test "$url"
                ;;
            *)
                print_error "Unknown test type: ${TEST_TYPE}"
                exit 1
                ;;
        esac
    else
        # Auto-detect deployment pattern
        if oc get deployment "${SERVICE}-blue" -n "${NAMESPACE}" >/dev/null 2>&1 || \
           oc get deployment "${SERVICE}-green" -n "${NAMESPACE}" >/dev/null 2>&1; then
            run_blue_green_test "$url" || {
                print_info "Falling back to standard deployment test..."
                continuous_test "$url" "$DURATION"
            }
        elif oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" >/dev/null 2>&1; then
            run_canary_test "$url" || {
                print_info "Falling back to standard deployment test..."
                continuous_test "$url" "$DURATION"
            }
        else
            print_info "Running standard deployment test..."

            # Start continuous test in background
            continuous_test "$url" "$DURATION" &
            local test_pid=$!

            # Trigger a deployment if requested
            if [ "${TRIGGER_DEPLOY:-false}" = "true" ]; then
                sleep 10
                trigger_deployment "$SERVICE"
            fi

            wait $test_pid
        fi
    fi

    # Calculate and display results
    calculate_statistics "$DURATION"
    print_results "$DURATION"
    exit $?
}

# Handle Ctrl+C
trap 'print_error "Test interrupted"; exit 130' INT

main "$@"
