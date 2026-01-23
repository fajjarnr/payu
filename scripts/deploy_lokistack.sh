#!/bin/bash
set -euo pipefail

# LokiStack Deployment Script for PayU on OpenShift
# This script deploys LokiStack for centralized log management

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
LOGGING_DIR="${PROJECT_ROOT}/infrastructure/openshift/base/logging"

echo "===================================="
echo "LokiStack Deployment for PayU"
echo "===================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if oc command is available
    if ! command -v oc &> /dev/null; then
        log_error "oc command not found. Please install the OpenShift CLI."
        exit 1
    fi

    # Check if user is logged in
    if ! oc whoami &> /dev/null; then
        log_error "Not logged in to OpenShift. Please run 'oc login' first."
        exit 1
    fi

    # Check if kustomize is available
    if ! command -v kustomize &> /dev/null; then
        log_warn "kustomize not found. Using 'kubectl kustomize' instead."
    fi

    log_info "Prerequisites check passed."
}

# Create namespaces
create_namespaces() {
    log_info "Creating namespaces..."

    oc apply -f "${LOGGING_DIR}/logging-namespace.yaml"

    log_info "Namespaces created successfully."
}

# Deploy logging operators
deploy_operators() {
    log_info "Deploying logging operators..."

    oc apply -f "${LOGGING_DIR}/logging-operator.yaml"

    log_info "Waiting for operators to be ready..."
    sleep 30

    # Wait for loki-operator
    log_info "Waiting for Loki operator to be ready..."
    timeout 300 oc rollout status deployment/loki-operator -n openshift-logging || {
        log_error "Loki operator failed to become ready"
        exit 1
    }

    log_info "Operators deployed successfully."
}

# Configure storage secrets
configure_storage() {
    log_info "Configuring storage secrets..."

    # Check if S3 credentials are provided
    if [ -z "${S3_ACCESS_KEY:-}" ] || [ -z "${S3_SECRET_KEY:-}" ]; then
        log_warn "S3_ACCESS_KEY and/or S3_SECRET_KEY not set. Using placeholder values."
        log_warn "Please update the secret with actual S3 credentials after deployment."
    fi

    # Create storage secret with environment variables if available
    envsubst < "${LOGGING_DIR}/lokistack-storage-secret.yaml" | oc apply -f -

    log_info "Storage secrets configured."
}

# Deploy LokiStack
deploy_lokistack() {
    log_info "Deploying LokiStack..."

    oc apply -f "${LOGGING_DIR}/lokistack.yaml"

    log_info "Waiting for LokiStack to be ready..."
    sleep 60

    # Check LokiStack status
    log_info "Checking LokiStack status..."
    timeout 600 bash -c 'until oc get lokistack loki -n openshift-logging -o jsonpath="{.status.conditions[?(@.type==\"Ready\")].status}" | grep -q "True"; do sleep 10; done' || {
        log_error "LokiStack failed to become ready"
        exit 1
    }

    log_info "LokiStack deployed successfully."
}

# Deploy RBAC
deploy_rbac() {
    log_info "Deploying RBAC resources..."

    oc apply -f "${LOGGING_DIR}/loki-rbac.yaml"

    log_info "RBAC resources deployed successfully."
}

# Deploy ClusterLogging
deploy_cluster_logging() {
    log_info "Deploying ClusterLogging..."

    oc apply -f "${LOGGING_DIR}/clusterlogging.yaml"

    log_info "Waiting for ClusterLogging to be ready..."
    sleep 30

    log_info "ClusterLogging deployed successfully."
}

# Deploy ClusterLogForwarder
deploy_log_forwarder() {
    log_info "Deploying ClusterLogForwarder..."

    # Create Loki token secret
    LOKI_TOKEN=$(oc get secret lokistack-gw-ca-tls -n openshift-logging -o jsonpath='{.data.tls\.crt}' 2>/dev/null || echo "")
    if [ -n "$LOKI_TOKEN" ]; then
        cat > "${LOGGING_DIR}/lokistack-token-secret.yaml" <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: loki-token
  namespace: openshift-logging
  labels:
    app.kubernetes.io/part-of: payu
type: Opaque
stringData:
  token: ${LOKI_TOKEN}
EOF
    fi

    oc apply -f "${LOGGING_DIR}/lokistack-token-secret.yaml"
    oc apply -f "${LOGGING_DIR}/clusterlogforwarder.yaml"

    log_info "ClusterLogForwarder deployed successfully."
}

# Deploy alert rules
deploy_alert_rules() {
    log_info "Deploying Loki alert rules..."

    oc apply -f "${LOGGING_DIR}/loki-alert-rules.yaml"

    log_info "Alert rules deployed successfully."
}

# Deploy Loki route
deploy_route() {
    log_info "Deploying Loki route..."

    oc apply -f "${LOGGING_DIR}/loki-route.yaml"

    log_info "Loki route deployed successfully."
}

# Verify deployment
verify_deployment() {
    log_info "Verifying deployment..."

    # Check LokiStack
    if ! oc get lokistack loki -n openshift-logging &> /dev/null; then
        log_error "LokiStack not found"
        return 1
    fi

    # Check ClusterLogForwarder
    if ! oc get clusterlogforwarder payu-log-forwarder -n openshift-logging &> /dev/null; then
        log_error "ClusterLogForwarder not found"
        return 1
    fi

    # Check pods
    log_info "Checking LokiStack pods..."
    oc wait --for=condition=ready pod -l app.kubernetes.io/name=loki -n openshift-logging --timeout=300s || {
        log_warn "Some Loki pods are not ready yet"
    }

    log_info "Deployment verification completed."
}

# Print access information
print_access_info() {
    log_info "===================================="
    log_info "LokiStack Access Information"
    log_info "===================================="

    # Get route URL
    LOKI_URL=$(oc get route loki-gateway -n openshift-logging -o jsonpath='{.spec.host}' 2>/dev/null || echo "")

    if [ -n "$LOKI_URL" ]; then
        log_info "Loki Gateway URL: https://${LOKI_URL}"
    else
        log_info "Loki Gateway URL: Use port-forward or create route"
    fi

    log_info ""
    log_info "To access Loki locally:"
    log_info "  oc port-forward -n openshift-logging svc/loki-gateway-http 3100:3100"
    log_info ""
    log_info "Then open http://localhost:3100 in your browser"
    log_info ""
    log_info "To query logs using jq:"
    log_info "  oc logs -n openshift-logging deployment/loki-querier | jq ."
    log_info "===================================="
}

# Main deployment function
main() {
    check_prerequisites
    create_namespaces
    deploy_operators
    configure_storage
    deploy_lokistack
    deploy_rbac
    deploy_cluster_logging
    deploy_log_forwarder
    deploy_alert_rules
    deploy_route
    verify_deployment
    print_access_info

    log_info "LokiStack deployment completed successfully!"
}

# Handle command line arguments
case "${1:-deploy}" in
    deploy)
        main
        ;;
    verify)
        verify_deployment
        ;;
    delete)
        log_info "Deleting LokiStack..."
        oc delete -f "${LOGGING_DIR}"
        log_info "LokiStack deleted successfully."
        ;;
    status)
        log_info "LokiStack Status:"
        oc get lokistack -n openshift-logging
        oc get clusterlogforwarder -n openshift-logging
        oc get pods -n openshift-logging -l app.kubernetes.io/name=loki
        ;;
    *)
        echo "Usage: $0 {deploy|verify|delete|status}"
        exit 1
        ;;
esac
