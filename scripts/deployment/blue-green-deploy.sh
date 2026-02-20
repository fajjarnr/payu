#!/bin/bash
#
# Blue-Green Deployment Script
# =============================
# Deploys a new version using blue-green strategy
#
# Usage: ./blue-green-deploy.sh <service-name> <version>
# Example: ./blue-green-deploy.sh gateway-service 1.3.0

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${NAMESPACE:-payu-dev}"
SERVICE="${1:-}"
VERSION="${2:-}"
BLUE_WEIGHT="${BLUE_WEIGHT:-0}"
GREEN_WEIGHT="${GREEN_WEIGHT:-100}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Blue-Green Deployment${NC}"
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
        echo "Usage: $0 <service-name> <version>"
        echo "Example: $0 gateway-service 1.3.0"
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

detect_current_version() {
    print_info "Detecting current deployment..."

    # Check which version is currently active
    CURRENT_SELECTOR=$(oc get route "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.spec.to.name}' 2>/dev/null || echo "")

    if [[ "$CURRENT_SELECTOR" == *"-blue"* ]]; then
        CURRENT_VERSION="blue"
        TARGET_VERSION="green"
    elif [[ "$CURRENT_SELECTOR" == *"-green"* ]]; then
        CURRENT_VERSION="green"
        TARGET_VERSION="blue"
    else
        # Default to blue-green swap
        CURRENT_VERSION="blue"
        TARGET_VERSION="green"
    fi

    print_info "Current: ${CURRENT_VERSION}, Target: ${TARGET_VERSION}"
}

pre_deployment_checks() {
    print_info "Running pre-deployment checks..."

    # Check if service exists
    if ! oc get deployment "${SERVICE}-${CURRENT_VERSION}" -n "${NAMESPACE}" &> /dev/null; then
        print_error "Service ${SERVICE}-${CURRENT_VERSION} not found"
        exit 1
    fi

    # Check database migration compatibility
    if [ -f "${SCRIPT_DIR}/verify-db-compatibility.sh" ]; then
        "${SCRIPT_DIR}/verify-db-compatibility.sh" || {
            print_error "Database compatibility check failed"
            exit 1
        }
    fi

    # Verify current version health
    READY_REPLICAS=$(oc get deployment "${SERVICE}-${CURRENT_VERSION}" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    if [ "$READY_REPLICAS" -eq 0 ]; then
        print_error "Current version has 0 ready replicas"
        exit 1
    fi

    print_success "Pre-deployment checks passed"
}

deploy_target_version() {
    print_info "Deploying ${TARGET_VERSION} version ${VERSION}..."

    # Update image tag for target deployment
    oc set image "deployment/${SERVICE}-${TARGET_VERSION}" "${SERVICE}-${TARGET_VERSION}=image-registry.openshift-image-registry.svc:5000/${NAMESPACE}/${SERVICE}:${VERSION}" -n "${NAMESPACE}"

    # Trigger rollout
    oc rollout latest "deployment/${SERVICE}-${TARGET_VERSION}" -n "${NAMESPACE}" 2>/dev/null || true

    # Wait for rollout
    print_info "Waiting for rollout to complete..."
    if ! oc rollout status "deployment/${SERVICE}-${TARGET_VERSION}" -n "${NAMESPACE}" --timeout=300s; then
        print_error "Rollout failed or timed out"
        print_info "Rolling back..."
        oc rollout undo "deployment/${SERVICE}-${TARGET_VERSION}" -n "${NAMESPACE}"
        exit 1
    fi

    print_success "Target version deployed successfully"
}

verify_target_health() {
    print_info "Verifying target version health..."

    # Run verification script if available
    if [ -f "${SCRIPT_DIR}/verify-deployment.sh" ]; then
        "${SCRIPT_DIR}/verify-deployment.sh" "${TARGET_VERSION}" || {
            print_error "Health verification failed"
            return 1
        }
    else
        # Basic health check
        READY_REPLICAS=$(oc get deployment "${SERVICE}-${TARGET_VERSION}" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
        DESIRED_REPLICAS=$(oc get deployment "${SERVICE}-${TARGET_VERSION}" -n "${NAMESPACE}" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "1")

        if [ "$READY_REPLICAS" -lt "$DESIRED_REPLICAS" ]; then
            print_error "Only ${READY_REPLICAS}/${DESIRED_REPLICAS} replicas ready"
            return 1
        fi

        # Check pod health
        UNHEALTHY_PODS=$(oc get pods -n "${NAMESPACE}" -l "app=${SERVICE}-${TARGET_VERSION}" --field-selector=status.phase!=Running 2>/dev/null | wc -l)
        if [ "$UNHEALTHY_PODS" -gt 0 ]; then
            print_error "Found unhealthy pods"
            return 1
        fi
    fi

    print_success "Target version health verified"
}

switch_traffic() {
    print_info "Switching traffic to ${TARGET_VERSION}..."

    # Update route to point to target
    oc patch route "${SERVICE}" -n "${NAMESPACE}" -p \
        "{\"spec\":{\"to\":{\"name\":\"${SERVICE}-${TARGET_VERSION}\"}}}"

    print_success "Traffic switched to ${TARGET_VERSION}"
}

monitor_deployment() {
    local duration="${1:-300}"  # Default 5 minutes
    print_info "Monitoring deployment for ${duration} seconds..."

    local end_time=$(($(date +%s) + duration))
    local errors=0

    while [ $(date +%s) -lt $end_time ]; do
        # Check error rate
        if command -v curl &> /dev/null; then
            GATEWAY_URL=$(oc get route "${SERVICE}" -n "${NAMESPACE}" -o jsonpath='{.spec.host}' 2>/dev/null || echo "")
            if [ -n "$GATEWAY_URL" ]; then
                HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "https://${GATEWAY_URL}/actuator/health" 2>/dev/null || echo "000")
                if [ "$HTTP_CODE" != "200" ]; then
                    errors=$((errors + 1))
                    print_warning "Health check failed: HTTP ${HTTP_CODE}"
                fi
            fi
        fi

        # Check pod status
        UNHEALTHY_PODS=$(oc get pods -n "${NAMESPACE}" -l "app=${SERVICE}-${TARGET_VERSION}" --field-selector=status.phase!=Running 2>/dev/null | grep -v NAME | wc -l)
        if [ "$UNHEALTHY_PODS" -gt 0 ]; then
            errors=$((errors + 1))
            print_warning "Unhealthy pods detected: ${UNHEALTHY_PODS}"
        fi

        if [ $errors -gt 5 ]; then
            print_error "Too many errors detected"
            return 1
        fi

        sleep 10
        echo -n "."
    done

    echo ""
    print_success "Monitoring complete"
}

rollback() {
    print_warning "Rolling back to ${CURRENT_VERSION}..."

    # Switch traffic back
    oc patch route "${SERVICE}" -n "${NAMESPACE}" -p \
        "{\"spec\":{\"to\":{\"name\":\"${SERVICE}-${CURRENT_VERSION}\"}}}"

    # Scale down target version
    oc scale "deployment/${SERVICE}-${TARGET_VERSION}" -n "${NAMESPACE}" --replicas=0

    print_success "Rollback complete"
}

main() {
    print_header

    validate_inputs
    detect_current_version
    pre_deployment_checks

    # Deploy
    if ! deploy_target_version; then
        print_error "Deployment failed"
        exit 1
    fi

    # Verify
    if ! verify_target_health; then
        print_error "Health verification failed"
        exit 1
    fi

    # Switch traffic
    switch_traffic

    # Monitor
    if ! monitor_deployment 300; then
        print_error "Deployment monitoring detected issues"
        read -p "Rollback? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            rollback
        fi
        exit 1
    fi

    print_success "Blue-green deployment complete!"
    print_info "Service ${SERVICE} is now running version ${VERSION} on ${TARGET_VERSION}"
    print_info "Previous version (${CURRENT_VERSION}) is on standby for rollback if needed"
}

# Handle Ctrl+C
trap 'print_error "Deployment interrupted"; exit 130' INT

main "$@"
