#!/bin/bash
#
# Canary Deployment Script
# ========================
# Deploys a new version using canary (progressive) strategy
#
# Usage: ./canary-deploy.sh <service-name> <version> <percentage>
# Example: ./canary-deploy.sh gateway-service 1.3.0 10

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${NAMESPACE:-payu-dev}"
SERVICE="${1:-}"
VERSION="${2:-}"
CANARY_PERCENTAGE="${3:-10}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Canary Deployment${NC}"
    echo -e "${BLUE}========================================${NC}"
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

validate_inputs() {
    if [ -z "$SERVICE" ] || [ -z "$VERSION" ]; then
        echo "Usage: $0 <service-name> <version> [percentage]"
        echo "Example: $0 gateway-service 1.3.0 10"
        exit 1
    fi

    if [ "$CANARY_PERCENTAGE" -lt 1 ] || [ "$CANARY_PERCENTAGE" -gt 100 ]; then
        print_error "Percentage must be between 1 and 100"
        exit 1
    fi

    if ! command -v oc &> /dev/null; then
        print_error "OpenShift CLI (oc) not found"
        exit 1
    fi

    if ! oc whoami &> /dev/null; then
        print_error "Not logged into OpenShift"
        exit 1
    fi
}

check_istio() {
    print_info "Checking Istio/Service Mesh availability..."

    if ! oc get virtualservice "${SERVICE}" -n "${NAMESPACE}" &> /dev/null; then
        print_warning "No VirtualService found for ${SERVICE}"
        print_info "Falling back to OpenShift Route-based canary..."
        USE_ROUTE=true
    else
        USE_ROUTE=false
        print_success "Istio VirtualService found"
    fi
}

deploy_canary_version() {
    print_info "Deploying canary version ${VERSION} (${CANARY_PERCENTAGE}%)..."

    # Check if canary deployment exists
    if ! oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" &> /dev/null; then
        print_info "Creating canary deployment from stable template..."

        # Get stable deployment as template
        oc get deployment "${SERVICE}" -n "${NAMESPACE}" -o yaml | \
            sed "s/name: ${SERVICE}/name: ${SERVICE}-canary/g" | \
            sed "s/app: ${SERVICE}/app: ${SERVICE}-canary/g" | \
            oc apply -f -
    fi

    # Update canary image
    oc set image "deployment/${SERVICE}-canary" \
        "${SERVICE}-canary=image-registry.openshift-image-registry.svc:5000/${NAMESPACE}/${SERVICE}:${VERSION}" \
        -n "${NAMESPACE}"

    # Set replicas based on percentage
    STABLE_REPLICAS=$(oc get deployment "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "1")
    CANARY_REPLICAS=$(( (STABLE_REPLICAS * CANARY_PERCENTAGE + 99) / 100 ))

    if [ "$CANARY_REPLICAS" -lt 1 ]; then
        CANARY_REPLICAS=1
    fi

    oc scale "deployment/${SERVICE}-canary" -n "${NAMESPACE}" --replicas="${CANARY_REPLICAS}"

    # Wait for rollout
    print_info "Waiting for canary rollout to complete..."
    if ! oc rollout status "deployment/${SERVICE}-canary" -n "${NAMESPACE}" --timeout=300s; then
        print_error "Canary rollout failed"
        rollback_canary
        exit 1
    fi

    print_success "Canary version deployed with ${CANARY_REPLICAS} replicas"
}

configure_traffic_split() {
    print_info "Configuring traffic split: ${CANARY_PERCENTAGE}% canary, $((100 - CANARY_PERCENTAGE))% stable..."

    if [ "$USE_ROUTE" = true ]; then
        # OpenShift Route-based traffic split using alternateBackends
        oc patch route "${SERVICE}" -n "${NAMESPACE}" --type='json' -p "[{
            \"op\": \"add\",
            \"path\": \"/spec/alternateBackends\",
            \"value\": [{
                \"kind\": \"Service\",
                \"name\": \"${SERVICE}-canary\",
                \"weight\": ${CANARY_PERCENTAGE}
            }]
        }, {
            \"op\": \"replace\",
            \"path\": \"/spec/to/weight\",
            \"value\": $((100 - CANARY_PERCENTAGE))
        }]" 2>/dev/null || \
        oc patch route "${SERVICE}" -n "${NAMESPACE}" -p "{
            \"spec\": {
                \"to\": {
                    \"weight\": $((100 - CANARY_PERCENTAGE))
                },
                \"alternateBackends\": [{
                    \"kind\": \"Service\",
                    \"name\": \"${SERVICE}-canary\",
                    \"weight\": ${CANARY_PERCENTAGE}
                }]
            }
        }"
    else
        # Istio VirtualService-based traffic split
        oc patch virtualservice "${SERVICE}" -n "${NAMESPACE}" --type='json' -p "[{
            \"op\": \"replace\",
            \"path\": \"/spec/http/0/route\",
            \"value\": [
                {
                    \"destination\": {
                        \"host\": \"${SERVICE}\"
                    },
                    \"weight\": $((100 - CANARY_PERCENTAGE))
                },
                {
                    \"destination\": {
                        \"host\": \"${SERVICE}-canary\"
                    },
                    \"weight\": ${CANARY_PERCENTAGE}
                }
            ]
        }]"
    fi

    print_success "Traffic split configured"
}

verify_canary_health() {
    print_info "Verifying canary health..."

    # Check canary pods are ready
    READY_REPLICAS=$(oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    DESIRED_REPLICAS=$(oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "1")

    if [ "$READY_REPLICAS" -lt "$DESIRED_REPLICAS" ]; then
        print_error "Only ${READY_REPLICAS}/${DESIRED_REPLICAS} canary replicas ready"
        return 1
    fi

    # Check for unhealthy pods
    UNHEALTHY_PODS=$(oc get pods -n "${NAMESPACE}" -l "app=${SERVICE}-canary" --field-selector=status.phase!=Running 2>/dev/null | grep -v NAME | wc -l)
    if [ "$UNHEALTHY_PODS" -gt 0 ]; then
        print_error "Found ${UNHEALTHY_PODS} unhealthy canary pods"
        return 1
    fi

    # Run custom verification if available
    if [ -f "${SCRIPT_DIR}/verify-deployment.sh" ]; then
        "${SCRIPT_DIR}/verify-deployment.sh" canary || {
            print_error "Custom health verification failed"
            return 1
        }
    fi

    print_success "Canary health verified"
}

monitor_canary() {
    local duration="${1:-600}"  # Default 10 minutes
    print_info "Monitoring canary for ${duration} seconds..."

    local end_time=$(($(date +%s) + duration))
    local error_count=0
    local request_count=0
    local error_threshold=5  # Max errors before alerting

    while [ $(date +%s) -lt $end_time ]; do
        # Check pod health
        UNHEALTHY_PODS=$(oc get pods -n "${NAMESPACE}" -l "app=${SERVICE}-canary" --field-selector=status.phase!=Running 2>/dev/null | grep -v NAME | wc -l)
        if [ "$UNHEALTHY_PODS" -gt 0 ]; then
            error_count=$((error_count + 1))
            print_warning "Unhealthy canary pods: ${UNHEALTHY_PODS}"
        fi

        # Check error rate via metrics if available
        if command -v curl &> /dev/null && [ -n "${PROMETHEUS_URL:-}" ]; then
            ERROR_RATE=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=rate(http_requests_total{service=\"${SERVICE}-canary\",status=~\"5..\"}[1m])" 2>/dev/null | grep -o '"value":\[[^]]*\]' | grep -o '[0-9.]*$' || echo "0")

            if [ "$(echo "$ERROR_RATE > 0.01" | bc -l 2>/dev/null || echo 0)" -eq 1 ]; then
                error_count=$((error_count + 1))
                print_warning "High error rate detected: ${ERROR_RATE}"
            fi
        fi

        # Alert if too many errors
        if [ $error_count -gt $error_threshold ]; then
            print_error "Canary showing signs of instability (${error_count} errors)"
            return 1
        fi

        sleep 10
        echo -n "."
        request_count=$((request_count + 1))
    done

    echo ""
    print_success "Canary monitoring complete (${request_count} checks)"
}

rollback_canary() {
    print_warning "Rolling back canary..."

    # Remove traffic from canary
    if [ "$USE_ROUTE" = true ]; then
        oc patch route "${SERVICE}" -n "${NAMESPACE}" --type='json' -p '[{"op": "remove", "path": "/spec/alternateBackends"}]' 2>/dev/null || true
        oc patch route "${SERVICE}" -n "${NAMESPACE}" -p '{"spec":{"to":{"weight": 100}}}'
    else
        # Reset VirtualService to 100% stable
        oc patch virtualservice "${SERVICE}" -n "${NAMESPACE}" --type='json' -p "[{
            \"op\": \"replace\",
            \"path\": \"/spec/http/0/route\",
            \"value\": [{
                \"destination\": {
                    \"host\": \"${SERVICE}\"
                },
                \"weight\": 100
            }]
        }]"
    fi

    # Scale down canary
    oc scale "deployment/${SERVICE}-canary" -n "${NAMESPACE}" --replicas=0

    print_success "Canary rollback complete"
}

save_state() {
    # Save deployment state for promote/rollback scripts
    local state_file="/tmp/canary-${SERVICE}-state.json"
    cat > "$state_file" << EOF
{
    "service": "${SERVICE}",
    "version": "${VERSION}",
    "namespace": "${NAMESPACE}",
    "percentage": ${CANARY_PERCENTAGE},
    "timestamp": "$(date -Iseconds)",
    "use_route": ${USE_ROUTE}
}
EOF
    print_info "Canary state saved to ${state_file}"
}

main() {
    print_header

    validate_inputs
    check_istio

    # Deploy canary
    if ! deploy_canary_version; then
        print_error "Canary deployment failed"
        exit 1
    fi

    # Verify health
    if ! verify_canary_health; then
        print_error "Canary health check failed"
        rollback_canary
        exit 1
    fi

    # Configure traffic split
    configure_traffic_split

    # Save state for subsequent operations
    save_state

    # Monitor
    print_info "Canary deployed at ${CANARY_PERCENTAGE}% traffic"
    print_info "Next steps:"
    print_info "  - Monitor metrics and logs for issues"
    print_info "  - Promote: ./canary-promote.sh ${SERVICE} <new_percentage>"
    print_info "  - Rollback: ./canary-rollback.sh ${SERVICE}"

    if ! monitor_canary 600; then
        print_warning "Canary monitoring detected issues"
        read -p "Rollback canary? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            rollback_canary
            exit 1
        fi
    fi

    print_success "Canary deployment at ${CANARY_PERCENTAGE}% complete!"
}

# Handle Ctrl+C
trap 'print_error "Canary deployment interrupted"; rollback_canary; exit 130' INT

main "$@"
