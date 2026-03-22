#!/bin/bash

# Service Mesh Deployment Script for PayU Digital Banking Platform
# This script deploys the complete OpenShift Service Mesh configuration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${NAMESPACE:-istio-system}"
ENV="${ENV:-prod}"
DRY_RUN="${DRY_RUN:-false}"

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if oc command is available
    if ! command -v oc &> /dev/null; then
        log_error "oc command not found. Please install OpenShift CLI."
        exit 1
    fi

    # Check if user is logged in
    if ! oc whoami &> /dev/null; then
        log_error "Not logged in to OpenShift. Please run 'oc login'."
        exit 1
    fi

    # Check if Service Mesh Operator is installed
    if ! oc get csv -n openshift-operators | grep -q "service-mesh"; then
        log_warn "Service Mesh Operator not found. Installing..."
        # Operator installation would go here
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
    log_info "Deploying Service Mesh Control Plane..."

    if [ "$DRY_RUN" = "true" ]; then
        log_warn "DRY RUN: Would apply control-plane.yaml"
        oc apply --dry-run=client -f "$SCRIPT_DIR/control-plane.yaml"
    else
        oc apply -f "$SCRIPT_DIR/control-plane.yaml"

        log_info "Waiting for Istiod to be ready..."
        wait_for_pod "$NAMESPACE" "app=istiod" 300

        log_info "Waiting for Ingress Gateway to be ready..."
        wait_for_pod "$NAMESPACE" "app=istio-ingressgateway" 300

        log_info "Waiting for Egress Gateway to be ready..."
        wait_for_pod "$NAMESPACE" "app=istio-egressgateway" 300
    fi
}

deploy_gateway() {
    log_info "Deploying Gateway configuration..."

    if [ "$DRY_RUN" = "true" ]; then
        log_warn "DRY RUN: Would apply gateway.yaml"
        oc apply --dry-run=client -f "$SCRIPT_DIR/gateway.yaml"
    else
        oc apply -f "$SCRIPT_DIR/gateway.yaml"
        log_info "Gateway configuration applied."
    fi
}

deploy_destination_rules() {
    log_info "Deploying Destination Rules..."

    if [ "$DRY_RUN" = "true" ]; then
        log_warn "DRY RUN: Would apply destination-rules.yaml"
        oc apply --dry-run=client -f "$SCRIPT_DIR/destination-rules.yaml"
    else
        oc apply -f "$SCRIPT_DIR/destination-rules.yaml"
        log_info "Destination rules applied."
    fi
}

deploy_peer_authentication() {
    log_info "Deploying Peer Authentication and Authorization Policies..."

    if [ "$DRY_RUN" = "true" ]; then
        log_warn "DRY RUN: Would apply peer-authentication.yaml"
        oc apply --dry-run=client -f "$SCRIPT_DIR/peer-authentication.yaml"
    else
        oc apply -f "$SCRIPT_DIR/peer-authentication.yaml"
        log_info "Peer authentication policies applied."
    fi
}

enable_sidecar_injection() {
    log_info "Enabling sidecar injection for namespaces..."

    local namespaces=("payu-prod" "payu-preprod" "payu-uat" "payu-sit" "payu-dev")

    for ns in "${namespaces[@]}"; do
        if [ "$DRY_RUN" = "true" ]; then
            log_warn "DRY RUN: Would label namespace $ns"
        else
            # Check if namespace exists
            if oc get namespace "$ns" &> /dev/null; then
                oc label namespace "$ns" "maistra.io/member-of=istio-system" --overwrite
                log_info "Enabled sidecar injection for namespace: $ns"
            else
                log_warn "Namespace $ns does not exist. Skipping."
            fi
        fi
    done
}

verify_deployment() {
    log_info "Verifying Service Mesh deployment..."

    log_info "Checking control plane pods..."
    oc get pods -n "$NAMESPACE" | grep -E "istiod|ingressgateway|egressgateway"

    log_info "Checking ServiceMeshMemberRoll..."
    oc get servicemeshmemberroll -n "$NAMESPACE"

    log_info "Checking PeerAuthentication resources..."
    oc get peerauthentication -A

    log_info "Checking DestinationRules..."
    oc get destinationrules -A

    log_info "Checking VirtualServices..."
    oc get virtualservices -A
}

print_next_steps() {
    log_info "Deployment complete!"
    echo ""
    echo "Next steps:"
    echo "1. Restart your deployments to inject sidecars:"
    echo "   oc rollout restart deployment/account-service -n payu-prod"
    echo "   oc rollout restart deployment/auth-service -n payu-prod"
    echo "   oc rollout restart deployment/transaction-service -n payu-prod"
    echo "   oc rollout restart deployment/wallet-service -n payu-prod"
    echo ""
    echo "2. Verify sidecar injection:"
    echo "   oc get pods -n payu-prod"
    echo "   # Each pod should have 2/2 containers"
    echo ""
    echo "3. Access Kiali dashboard:"
    echo "   oc port-forward -n $NAMESPACE svc/kiali 20001:20001"
    echo "   # Open http://localhost:20001"
    echo ""
    echo "4. Access Jaeger dashboard:"
    echo "   oc port-forward -n $NAMESPACE svc/jaeger-query 16686:16686"
    echo "   # Open http://localhost:16686"
    echo ""
}

print_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -e, --environment ENV   Target environment (prod, uat, sit, dev) [default: prod]"
    echo "  -n, --namespace NS      Target namespace [default: istio-system]"
    echo "  -d, --dry-run           Show what would be applied without applying"
    echo "  -s, --step STEP         Deploy specific step (1-5)"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Steps:"
    echo "  1 - Deploy control plane"
    echo "  2 - Deploy gateway"
    echo "  3 - Deploy destination rules"
    echo "  4 - Deploy peer authentication"
    echo "  5 - Enable sidecar injection"
    echo ""
    echo "Examples:"
    echo "  $0                          # Deploy all components"
    echo "  $0 -e dev                   # Deploy for development environment"
    echo "  $0 -d                       # Dry run (show what would be applied)"
    echo "  $0 -s 1                     # Deploy only control plane"
    echo ""
}

# Parse command line arguments
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

# Main deployment flow
main() {
    log_info "Starting Service Mesh deployment for environment: $ENV"
    log_info "Target namespace: $NAMESPACE"
    [ "$DRY_RUN" = "true" ] && log_warn "DRY RUN MODE - No changes will be applied"

    check_prerequisites

    case $STEP in
        1|"")
            deploy_control_plane
            ;;
    esac

    case $STEP in
        2|"")
            [ -z "$STEP" ] || { sleep 5; deploy_gateway; }
            ;;
    esac

    case $STEP in
        3|"")
            [ -z "$STEP" ] || { sleep 2; deploy_destination_rules; }
            ;;
    esac

    case $STEP in
        4|"")
            [ -z "$STEP" ] || { sleep 2; deploy_peer_authentication; }
            ;;
    esac

    case $STEP in
        5|"")
            [ -z "$STEP" ] || { sleep 2; enable_sidecar_injection; }
            ;;
    esac

    if [ -z "$STEP" ] && [ "$DRY_RUN" = "false" ]; then
        verify_deployment
    fi

    print_next_steps
}

# Run main function
main
