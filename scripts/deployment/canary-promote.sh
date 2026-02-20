#!/bin/bash
#
# Canary Promotion Script
# =======================
# Promotes canary deployment to higher traffic percentage or full rollout
#
# Usage: ./canary-promote.sh <service-name> <percentage>
# Example: ./canary-promote.sh gateway-service 50

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${NAMESPACE:-payu-dev}"
SERVICE="${1:-}"
TARGET_PERCENTAGE="${2:-}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Canary Promotion${NC}"
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
    if [ -z "$SERVICE" ] || [ -z "$TARGET_PERCENTAGE" ]; then
        echo "Usage: $0 <service-name> <percentage>"
        echo "Example: $0 gateway-service 50"
        echo "         $0 gateway-service 100  (for full promotion)"
        exit 1
    fi

    if [ "$TARGET_PERCENTAGE" -lt 1 ] || [ "$TARGET_PERCENTAGE" -gt 100 ]; then
        print_error "Percentage must be between 1 and 100"
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
        print_info "Loaded canary state: version=${CANARY_VERSION}"
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
        print_error "No canary deployment found for ${SERVICE}"
        print_info "Run canary-deploy.sh first to create a canary deployment"
        exit 1
    fi

    # Get current canary percentage
    if [ "$USE_ROUTE" = true ]; then
        CURRENT_PERCENTAGE=$(oc get route "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.spec.alternateBackends[0].weight}' 2>/dev/null || echo "0")
    else
        CURRENT_PERCENTAGE=$(oc get virtualservice "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.spec.http[0].route[1].weight}' 2>/dev/null || echo "0")
    fi

    if [ "$CURRENT_PERCENTAGE" = "0" ] || [ -z "$CURRENT_PERCENTAGE" ]; then
        print_warning "Canary appears to have 0% traffic"
        CURRENT_PERCENTAGE=0
    fi

    print_info "Current canary traffic: ${CURRENT_PERCENTAGE}%"
    print_info "Target canary traffic: ${TARGET_PERCENTAGE}%"
}

scale_canary_replicas() {
    local target_percentage=$1

    # Calculate desired replicas based on percentage
    STABLE_REPLICAS=$(oc get deployment "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "1")
    DESIRED_REPLICAS=$(( (STABLE_REPLICAS * target_percentage + 99) / 100 ))

    if [ "$DESIRED_REPLICAS" -lt 1 ]; then
        DESIRED_REPLICAS=1
    fi

    CURRENT_REPLICAS=$(oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")

    if [ "$DESIRED_REPLICAS" -ne "$CURRENT_REPLICAS" ]; then
        print_info "Scaling canary replicas: ${CURRENT_REPLICAS} → ${DESIRED_REPLICAS}"
        oc scale "deployment/${SERVICE}-canary" -n "${NAMESPACE}" --replicas="${DESIRED_REPLICAS}"

        # Wait for new replicas to be ready
        print_info "Waiting for canary replicas to be ready..."
        timeout=180
        while [ $timeout -gt 0 ]; do
            READY=$(oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
            if [ "$READY" -ge "$DESIRED_REPLICAS" ]; then
                print_success "All canary replicas ready"
                return 0
            fi
            sleep 5
            timeout=$((timeout - 5))
            echo -n "."
        done
        echo ""
        print_error "Timeout waiting for canary replicas"
        return 1
    fi
}

update_traffic_split() {
    local canary_weight=$1
    local stable_weight=$((100 - canary_weight))

    print_info "Updating traffic split: ${stable_weight}% stable, ${canary_weight}% canary..."

    if [ "$USE_ROUTE" = true ]; then
        # Update OpenShift Route weights
        if [ "$canary_weight" -eq 0 ]; then
            # Remove alternate backends when promoting to 100%
            oc patch route "${SERVICE}" -n "${NAMESPACE}" --type='json' -p '[{"op": "remove", "path": "/spec/alternateBackends"}]' 2>/dev/null || true
            oc patch route "${SERVICE}" -n "${NAMESPACE}" -p '{"spec":{"to":{"weight": 100}}}'
        elif [ "$canary_weight" -eq 100 ]; then
            # Swap canary to primary for full promotion
            print_info "Promoting canary to primary..."

            # Update stable deployment to canary version
            CANARY_IMAGE=$(oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.containers[0].image}')
            oc set image "deployment/${SERVICE}" "${SERVICE}=${CANARY_IMAGE}" -n "${NAMESPACE}"
            oc rollout status "deployment/${SERVICE}" -n "${NAMESPACE}" --timeout=300s

            # Remove canary from route
            oc patch route "${SERVICE}" -n "${NAMESPACE}" --type='json' -p '[{"op": "remove", "path": "/spec/alternateBackends"}]' 2>/dev/null || true
            oc patch route "${SERVICE}" -n "${NAMESPACE}" -p '{"spec":{"to":{"weight": 100}}}'

            # Scale down and remove canary
            oc scale "deployment/${SERVICE}-canary" -n "${NAMESPACE}" --replicas=0
            print_success "Canary promoted to primary successfully"
        else
            # Update weights for partial promotion
            oc patch route "${SERVICE}" -n "${NAMESPACE}" -p "{
                \"spec\": {
                    \"to\": {
                        \"weight\": ${stable_weight}
                    },
                    \"alternateBackends\": [{
                        \"kind\": \"Service\",
                        \"name\": \"${SERVICE}-canary\",
                        \"weight\": ${canary_weight}
                    }]
                }
            }"
        fi
    else
        # Istio VirtualService update
        if [ "$canary_weight" -eq 100 ]; then
            # Full promotion - swap canary to primary
            print_info "Promoting canary to primary..."

            CANARY_IMAGE=$(oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.containers[0].image}')
            oc set image "deployment/${SERVICE}" "${SERVICE}=${CANARY_IMAGE}" -n "${NAMESPACE}"
            oc rollout status "deployment/${SERVICE}" -n "${NAMESPACE}" --timeout=300s

            # Reset VirtualService to 100% primary
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

            # Scale down canary
            oc scale "deployment/${SERVICE}-canary" -n "${NAMESPACE}" --replicas=0
            print_success "Canary promoted to primary successfully"
        else
            # Update weights
            oc patch virtualservice "${SERVICE}" -n "${NAMESPACE}" --type='json' -p "[{
                \"op\": \"replace\",
                \"path\": \"/spec/http/0/route\",
                \"value\": [
                    {
                        \"destination\": {
                            \"host\": \"${SERVICE}\"
                        },
                        \"weight\": ${stable_weight}
                    },
                    {
                        \"destination\": {
                            \"host\": \"${SERVICE}-canary\"
                        },
                        \"weight\": ${canary_weight}
                    }
                ]
            }]"
        fi
    fi

    print_success "Traffic split updated"
}

verify_promotion() {
    print_info "Verifying promotion..."

    if [ "$TARGET_PERCENTAGE" -eq 100 ]; then
        # Full promotion verification
        READY=$(oc get deployment "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
        if [ "$READY" -lt 1 ]; then
            print_error "Primary deployment not ready after promotion"
            return 1
        fi

        # Verify canary is scaled down
        CANARY_REPLICAS=$(oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")
        if [ "$CANARY_REPLICAS" -ne 0 ]; then
            print_warning "Canary still has ${CANARY_REPLICAS} replicas"
        fi
    else
        # Partial promotion verification
        READY=$(oc get deployment "${SERVICE}-canary" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
        if [ "$READY" -lt 1 ]; then
            print_error "Canary deployment not ready"
            return 1
        fi
    fi

    print_success "Promotion verified"
}

monitor_promotion() {
    local duration=300  # 5 minutes default
    print_info "Monitoring promotion for ${duration} seconds..."

    local end_time=$(($(date +%s) + duration))
    local errors=0

    while [ $(date +%s) -lt $end_time ]; do
        # Check pod health
        UNHEALTHY=$(oc get pods -n "${NAMESPACE}" -l "app=${SERVICE}-canary" --field-selector=status.phase!=Running 2>/dev/null | grep -v NAME | wc -l)
        if [ "$UNHEALTHY" -gt 0 ]; then
            errors=$((errors + 1))
            print_warning "Unhealthy canary pods: ${UNHEALTHY}"
        fi

        if [ $errors -gt 3 ]; then
            print_error "Too many errors during promotion monitoring"
            return 1
        fi

        sleep 10
        echo -n "."
    done

    echo ""
    print_success "Monitoring complete"
}

update_state() {
    local state_file="/tmp/canary-${SERVICE}-state.json"

    if [ -f "$state_file" ] && [ "$TARGET_PERCENTAGE" -eq 100 ]; then
        # Remove state file after full promotion
        rm -f "$state_file"
        print_info "Canary state cleared (full promotion complete)"
    elif [ -f "$state_file" ]; then
        # Update state file with new percentage
        jq ".percentage = ${TARGET_PERCENTAGE} | .timestamp = \"$(date -Iseconds)\"" "$state_file" > "${state_file}.tmp" && mv "${state_file}.tmp" "$state_file"
        print_info "Canary state updated: ${TARGET_PERCENTAGE}%"
    fi
}

main() {
    print_header

    validate_inputs
    load_state
    check_canary_exists

    # Scale replicas if needed
    if [ "$TARGET_PERCENTAGE" -ne 100 ]; then
        scale_canary_replicas "$TARGET_PERCENTAGE" || exit 1
    fi

    # Update traffic split
    update_traffic_split "$TARGET_PERCENTAGE"

    # Verify
    if ! verify_promotion; then
        print_error "Promotion verification failed"
        exit 1
    fi

    # Update state
    update_state

    # Monitor
    if [ "$TARGET_PERCENTAGE" -ne 100 ]; then
        monitor_promotion
    fi

    if [ "$TARGET_PERCENTAGE" -eq 100 ]; then
        print_success "Full promotion complete! ${SERVICE} is now 100% on new version"
    else
        print_success "Canary promoted to ${TARGET_PERCENTAGE}% traffic"
        print_info "To continue:"
        print_info "  - Promote further: ./canary-promote.sh ${SERVICE} <higher_percentage>"
        print_info "  - Complete promotion: ./canary-promote.sh ${SERVICE} 100"
        print_info "  - Rollback: ./canary-rollback.sh ${SERVICE}"
    fi
}

# Handle Ctrl+C
trap 'print_error "Promotion interrupted"; exit 130' INT

main "$@"
