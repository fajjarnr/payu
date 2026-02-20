#!/bin/bash
#
# Canary Rollback Script
# ======================
# Instantly rolls back canary deployment to 0% traffic
#
# Usage: ./canary-rollback.sh <service-name>
# Example: ./canary-rollback.sh gateway-service

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${NAMESPACE:-payu-dev}"
SERVICE="${1:-}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Canary Rollback${NC}"
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
    if [ -z "$SERVICE" ]; then
        echo "Usage: $0 <service-name>"
        echo "Example: $0 gateway-service"
        exit 1
    fi

    if ! command -v oc &> /dev/null; then
        print_error "OpenShift CLI (oc) not found"
        exit 1
    fi
}

load_state() {
    local state_file="/tmp/canary-${SERVICE}-state.json"

    if [ -f "$state_file" ]; then
        CANARY_VERSION=$(jq -r '.version' "$state_file" 2>/dev/null || echo "unknown")
        USE_ROUTE=$(jq -r '.use_route' "$state_file" 2>/dev/null || echo "true")
        PREVIOUS_PERCENTAGE=$(jq -r '.percentage' "$state_file" 2>/dev/null || echo "0")
        print_info "Loaded state: version=${CANARY_VERSION}, was at ${PREVIOUS_PERCENTAGE}%"
    else
        print_warning "No state file found, detecting configuration..."
        if oc get virtualservice "${SERVICE}" -n "${NAMESPACE}" &> /dev/null; then
            USE_ROUTE=false
        else
            USE_ROUTE=true
        fi
    fi
}

check_canary_exists() {
    if ! oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" &> /dev/null; then
        print_warning "No canary deployment found for ${SERVICE}"
        print_info "Nothing to rollback"
        exit 0
    fi

    # Get current canary percentage
    if [ "$USE_ROUTE" = true ]; then
        CURRENT_PERCENTAGE=$(oc get route "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.spec.alternateBackends[0].weight}' 2>/dev/null || echo "0")
    else
        CURRENT_PERCENTAGE=$(oc get virtualservice "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.spec.http[0].route[1].weight}' 2>/dev/null || echo "0")
    fi

    if [ "$CURRENT_PERCENTAGE" = "0" ] || [ -z "$CURRENT_PERCENTAGE" ]; then
        print_info "Canary already at 0% traffic"
        CURRENT_PERCENTAGE=0
    else
        print_warning "Current canary traffic: ${CURRENT_PERCENTAGE}%"
    fi
}

rollback_traffic() {
    print_info "Instantly routing 100% traffic to stable version..."

    if [ "$USE_ROUTE" = true ]; then
        # Remove canary from route and set 100% to stable
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

    print_success "All traffic routed to stable version (~10 seconds)"
}

scale_down_canary() {
    print_info "Scaling down canary deployment..."

    # Get current replicas for logging
    CURRENT_REPLICAS=$(oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")
    print_info "Current canary replicas: ${CURRENT_REPLICAS}"

    # Scale to 0
    oc scale "deployment/${SERVICE}-canary" -n "${NAMESPACE}" --replicas=0

    # Wait for pods to terminate
    print_info "Waiting for canary pods to terminate..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        RUNNING_PODS=$(oc get pods -n "${NAMESPACE}" -l "app=${SERVICE}-canary" --field-selector=status.phase=Running 2>/dev/null | grep -v NAME | wc -l)
        if [ "$RUNNING_PODS" -eq 0 ]; then
            print_success "All canary pods terminated"
            return 0
        fi
        sleep 2
        timeout=$((timeout - 2))
        echo -n "."
    done
    echo ""

    print_warning "Timeout waiting for pods to terminate, forcing deletion..."
    oc delete pods -n "${NAMESPACE}" -l "app=${SERVICE}-canary" --force --grace-period=0 2>/dev/null || true
}

verify_rollback() {
    print_info "Verifying rollback..."

    # Check stable deployment is ready
    STABLE_READY=$(oc get deployment "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    if [ "$STABLE_READY" -lt 1 ]; then
        print_error "Stable deployment has no ready replicas!"
        print_info "Attempting to restore stable deployment..."
        oc rollout retry "deployment/${SERVICE}" -n "${NAMESPACE}" 2>/dev/null || true
        sleep 10

        STABLE_READY=$(oc get deployment "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
        if [ "$STABLE_READY" -lt 1 ]; then
            print_error "CRITICAL: Stable deployment failed to recover"
            exit 1
        fi
    fi

    # Verify route is 100% stable
    if [ "$USE_ROUTE" = true ]; then
        STABLE_WEIGHT=$(oc get route "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.spec.to.weight}' 2>/dev/null || echo "100")
        if [ "$STABLE_WEIGHT" -ne 100 ]; then
            print_warning "Route weight is ${STABLE_WEIGHT}%, expected 100%"
            oc patch route "${SERVICE}" -n "${NAMESPACE}" -p '{"spec":{"to":{"weight": 100}}}'
        fi
    fi

    print_success "Rollback verified - 100% traffic on stable"
}

cleanup_canary() {
    print_info "Cleaning up canary resources..."

    # Delete canary deployment
    oc delete deployment "${SERVICE}-canary" -n "${NAMESPACE}" --ignore-not-found=true

    # Delete canary service if exists
    oc delete service "${SERVICE}-canary" -n "${NAMESPACE}" --ignore-not-found=true

    # Clean up state file
    local state_file="/tmp/canary-${SERVICE}-state.json"
    if [ -f "$state_file" ]; then
        rm -f "$state_file"
        print_info "State file removed"
    fi

    print_success "Canary resources cleaned up"
}

record_rollback() {
    # Log rollback event for metrics
    print_info "Recording rollback event..."

    cat << EOF
{
    "timestamp": "$(date -Iseconds)",
    "service": "${SERVICE}",
    "namespace": "${NAMESPACE}",
    "action": "canary_rollback",
    "previous_percentage": ${CURRENT_PERCENTAGE:-0},
    "reason": "manual_rollback"
}
EOF
}

main() {
    print_header

    validate_inputs
    load_state
    check_canary_exists

    # Confirm rollback if canary has traffic
    if [ "${CURRENT_PERCENTAGE:-0}" -gt 0 ]; then
        print_warning "This will instantly rollback ${CURRENT_PERCENTAGE}% of traffic from canary"
        read -p "Proceed with rollback? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "Rollback cancelled"
            exit 0
        fi
    fi

    # Execute rollback
    rollback_traffic
    scale_down_canary
    verify_rollback
    cleanup_canary

    # Record event
    record_rollback > "/tmp/canary-rollback-${SERVICE}-$(date +%s).json"

    print_success "Canary rollback complete!"
    print_info "Service ${SERVICE} is now 100% on stable version"
    print_info "Canary version has been removed"
}

# Handle Ctrl+C
trap 'print_error "Rollback interrupted"; exit 130' INT

main "$@"
