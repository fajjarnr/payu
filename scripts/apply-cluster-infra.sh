#!/bin/bash
# Apply cluster-scoped infrastructure resources
# These have their own namespaces and MUST NOT go through kustomize overlay
# (which forces namespace: payu-dev)
#
# Usage: ./scripts/apply-cluster-infra.sh

set -euo pipefail

BASE_DIR="infrastructure/platform"

echo "=== Applying cluster-scoped infrastructure resources ==="
echo "(These bypass kustomize to preserve their target namespaces)"
echo ""

# Logging (openshift-logging namespace)
echo "[1/8] LokiStack (openshift-logging)..."
oc apply -f "$BASE_DIR/observability/loki.yaml"

echo "[2/8] ClusterLogForwarder + RBAC (openshift-logging)..."
oc apply -f "$BASE_DIR/observability/cluster-logging.yaml"

echo "[3/8] Cluster Observability UIPlugins..."
oc apply -f "$BASE_DIR/observability/cluster-observability.yaml"

# Network Observability (netobserv namespace)
echo "[4/8] NetObserv LokiStack (netobserv)..."
oc apply -f "$BASE_DIR/observability/netobserv-loki.yaml"

echo "[5/8] NetObserv FlowCollector..."
oc apply -f "$BASE_DIR/observability/netobserv.yaml"

# Security (stackrox namespace)
echo "[6/8] RHACS Central + SecuredCluster (stackrox)..."
oc apply -f "$BASE_DIR/security/rhacs.yaml"

# Descheduler (openshift-kube-descheduler-operator namespace)
echo "[7/8] KubeDescheduler..."
oc apply -f "$BASE_DIR/cost/kubedescheduler.yaml"

# Service Mesh & GitOps (various operator namespaces)
echo "[8/8] Service Mesh, Istio CNI, Kiali, GitOps, Pipelines, RHDH..."
# OSSM 3.4 control plane: istio-system + istio-cni namespaces, IstioCNI CR, Istio CR
oc apply -f "$BASE_DIR/mesh/base/namespace.yaml"
oc apply -f "$BASE_DIR/mesh/base/istio-cni.yaml"
oc apply -f "$BASE_DIR/mesh/base/service-mesh.yaml"
# Kiali console CRs (Kiali + OSSMConsole + monitoring RBAC) — the CRDs come
# from the separate kiali-operator subscription, not from OSSM 3.4, so skip
# them when the operator is absent.
if oc get crd ossmconsoles.kiali.io &> /dev/null; then
    oc apply -f "$BASE_DIR/mesh/base/kiali.yaml"
else
    echo "     ossmconsoles.kiali.io CRD not found - skipping Kiali console CRs"
fi
# Dev-scoped mesh policy (PERMISSIVE mTLS in payu-dev)
if oc get namespace payu-dev &> /dev/null; then
    oc apply -k "$BASE_DIR/mesh/overlays/payu-dev"
else
    echo "     payu-dev namespace not found - skipping dev mesh overlay"
fi
oc apply -f "$BASE_DIR/cicd/argocd/gitops.yaml"
oc apply -f "$BASE_DIR/cicd/tekton/pipelines.yaml"
oc apply -f "$BASE_DIR/devportal/rhdh.yaml"

echo ""
echo "=== Done! All cluster-scoped resources applied ==="
echo ""
echo "Next steps:"
echo "  1. Apply payu-dev resources: oc apply -k infrastructure/foundation/namespaces/overlays/dev/"
echo "  2. Init Vault:               oc apply -f $BASE_DIR/security/vault-init-job.yaml"
echo "  3. Deploy services:           oc apply -k infrastructure/workloads/overlays/dev/"
