#!/bin/bash

# Service Mesh Deployment Script for PayU Digital Banking Platform
# OpenShift Service Mesh 3.4 (Sail Operator).
#
# The control plane is the sailoperator.io/v1 `Istio` + `IstioCNI` CRs in
# infrastructure/platform/mesh/base; per-environment mesh policy lives in
# infrastructure/platform/mesh/overlays/<env>.
#
# Steps:
#   1 control plane     infrastructure/platform/mesh            (cluster-wide)
#   2 mesh policy       infrastructure/platform/mesh/overlays/<env>
#   3 dev ingress       infrastructure/platform/mesh/ingress     (dev only)
#   4 mesh telemetry    infrastructure/platform/mesh/telemetry  (dev only)
#
# The ingress/ and telemetry/ packages are dev-scoped: the gateway
# namespace, the HTTPRoute and the telemetry RBAC/NetworkPolicy all target
# payu-dev, so they are skipped for the other environments.
#
# The legacy Maistra-era prod-ingress assets (base/gateway.yaml,
# base/destination-rules.yaml, base/peer-authentication.yaml) are NOT applied
# here: they target the prod namespaces and the prod ingress gateway, and
# base/peer-authentication.yaml carries a root-namespace STRICT policy that
# would bind payu-sit/payu-uat on a cluster that also hosts payu-dev.

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MESH_DIR="$REPO_ROOT/infrastructure/platform/mesh"
NAMESPACE="${NAMESPACE:-istio-system}"
ENV="${ENV:-dev}"
DRY_RUN="${DRY_RUN:-false}"

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Namespace the dev-scoped mesh packages (ingress/, telemetry/) write into.
DEV_NAMESPACE="payu-dev"

# Environment -> mesh overlay directory
overlay_dir() {
    case "$1" in
        dev) echo "payu-dev" ;;
        sit) echo "payu-sit" ;;
        uat) echo "payu-uat" ;;
        preprod) echo "payu-preprod" ;;
        prod) echo "payu" ;;
        *) echo "" ;;
    esac
}

apply() {
    local target=$1
    local flag="-f"
    [ -d "$target" ] && flag="-k"

    if [ "$DRY_RUN" = "true" ]; then
        log_warn "DRY RUN: Would apply $flag $target"
        oc apply --dry-run=client "$flag" "$target" >/dev/null
    else
        oc apply "$flag" "$target"
    fi
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v oc &> /dev/null; then
        log_error "oc command not found. Please install OpenShift CLI."
        exit 1
    fi

    if ! oc whoami &> /dev/null; then
        log_error "Not logged in to OpenShift. Please run 'oc login'."
        exit 1
    fi

    if ! oc get csv -n openshift-operators | grep -qi "servicemesh"; then
        log_warn "Sail Operator (servicemeshoperator3) not found in openshift-operators."
    fi

    log_info "Prerequisites check passed."
}

wait_for_pod() {
    local namespace=$1
    local label=$2
    local timeout=${3:-300}

    log_info "Waiting for pods with label '$label' in namespace '$namespace'..."
    oc wait --for=condition=ready pod -l "$label" -n "$namespace" --timeout="${timeout}s"
}

deploy_control_plane() {
    log_info "Deploying Service Mesh control plane (Istio + IstioCNI CRs)..."

    apply "$MESH_DIR/base"

    if [ "$DRY_RUN" = "true" ]; then
        return
    fi

    log_info "Waiting for istiod to be ready..."
    wait_for_pod "$NAMESPACE" "app=istiod" 300

    log_info "Waiting for the CNI plugin to be ready on every node..."
    oc rollout status daemonset/istio-cni-node -n istio-cni --timeout=300s
}

deploy_mesh_policy() {
    local overlay
    overlay="$(overlay_dir "$ENV")"

    if [ -z "$overlay" ]; then
        log_warn "Unknown environment '$ENV' - skipping mesh policy."
        return
    fi

    if [ ! -d "$MESH_DIR/overlays/$overlay" ]; then
        log_warn "No mesh overlay for '$overlay' yet - skipping mesh policy."
        return
    fi

    log_info "Deploying mesh policy for $overlay..."
    apply "$MESH_DIR/overlays/$overlay"
}

require_namespace() {
    local ns=$1
    if ! oc get namespace "$ns" &> /dev/null; then
        log_error "Namespace $ns not found - apply the environment namespace first."
        exit 1
    fi
}

deploy_ingress() {
    if [ "$ENV" != "dev" ]; then
        log_warn "The ingress/ package is dev-scoped - skipping for $ENV."
        return
    fi

    require_namespace "$DEV_NAMESPACE"

    log_info "Deploying the dev ingress on the shared domain..."
    apply "$MESH_DIR/ingress"

    if [ "$DRY_RUN" = "true" ]; then
        return
    fi

    log_info "Waiting for the ingress gateway to be ready..."
    oc wait --for=create pod -l gateway.networking.k8s.io/gateway-name=payu-public-gateway \
        -n payu-mesh-gateway --timeout=300s
    oc wait --for=condition=ready pod -l gateway.networking.k8s.io/gateway-name=payu-public-gateway \
        -n payu-mesh-gateway --timeout=300s
}

deploy_telemetry() {
    if [ "$ENV" != "dev" ]; then
        log_warn "The telemetry/ package is dev-scoped - skipping for $ENV."
        return
    fi

    require_namespace "$DEV_NAMESPACE"

    log_info "Deploying the mesh telemetry (ServiceMonitors + discovery RBAC)..."
    apply "$MESH_DIR/telemetry"
}

verify_deployment() {
    log_info "Verifying Service Mesh deployment..."

    log_info "Checking control plane CRs..."
    oc get istios.sailoperator.io -A
    oc get istiocnis.sailoperator.io -A

    log_info "Checking control plane pods..."
    oc get pods -n "$NAMESPACE"
    oc get pods -n istio-cni

    log_info "Checking PeerAuthentication resources..."
    oc get peerauthentication -A

    if [ "$ENV" = "dev" ]; then
        log_info "Checking the dev ingress..."
        oc get gateway -n payu-mesh-gateway
        oc get httproute -n "$DEV_NAMESPACE"

        log_info "Checking the mesh telemetry..."
        oc get servicemonitor -n payu-mesh-telemetry
    fi
}

print_next_steps() {
    log_info "Deployment complete!"
    echo ""
    echo "Next steps:"
    echo "1. Opt a workload into the mesh (pod template label):"
    echo "   sidecar.istio.io/inject: \"true\""
    echo "2. Restart the deployment so the sidecar is injected:"
    echo "   oc rollout restart deployment/account-service -n payu-dev"
    echo "3. Verify the sidecar is present (istio-proxy initContainer):"
    echo "   oc get pods -n payu-dev -o jsonpath='{range .items[*]}{.metadata.name}{'  '}{.spec.initContainers[*].name}{'\\n'}{end}'"
    echo "4. Apply the environment workloads - the sidecars and the telemetry"
    echo "   scrape targets exist only after that:"
    echo "   oc apply -k infrastructure/workloads/overlays/payu-dev"
}

print_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -e, --environment ENV   Target environment (dev, sit, uat, preprod, prod) [default: dev]"
    echo "  -n, --namespace NS      Control plane namespace [default: istio-system]"
    echo "  -d, --dry-run           Show what would be applied without applying"
    echo "  -s, --step STEP         Deploy specific step (1-4)"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Steps:"
    echo "  1 - Deploy control plane (Istio + IstioCNI CRs)"
    echo "  2 - Deploy mesh policy for the environment"
    echo "  3 - Deploy the dev ingress on the shared domain (dev only)"
    echo "  4 - Deploy the mesh telemetry (dev only)"
    echo ""
    echo "Examples:"
    echo "  $0                          # Deploy all steps"
    echo "  $0 -e dev                   # Deploy for development environment"
    echo "  $0 -d                       # Dry run (show what would be applied)"
    echo "  $0 -s 1                     # Deploy only control plane"
    echo ""
}

STEP=""
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENV="$2"
            shift 2
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -s|--step)
            STEP="$2"
            shift 2
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

main() {
    log_info "Starting Service Mesh deployment for environment: $ENV"
    log_info "Target namespace: $NAMESPACE"
    [ "$DRY_RUN" = "true" ] && log_warn "DRY RUN MODE - No changes will be applied"

    check_prerequisites

    if [ -z "$STEP" ] || [ "$STEP" = "1" ]; then
        deploy_control_plane
    fi

    if [ -z "$STEP" ] || [ "$STEP" = "2" ]; then
        deploy_mesh_policy
    fi

    if [ -z "$STEP" ] || [ "$STEP" = "3" ]; then
        deploy_ingress
    fi

    if [ -z "$STEP" ] || [ "$STEP" = "4" ]; then
        deploy_telemetry
    fi

    if [ -z "$STEP" ] && [ "$DRY_RUN" = "false" ]; then
        verify_deployment
    fi

    print_next_steps
}

main
