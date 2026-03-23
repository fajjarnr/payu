#!/bin/bash
# Apply cluster-scoped infrastructure resources
# These have their own namespaces and MUST NOT go through kustomize overlay
# (which forces namespace: payu-dev)
#
# Usage: ./scripts/apply-cluster-infra.sh
# ============================================================

set -euo pipefail

BASE_DIR="infrastructure/openshift/infra/base"

echo "=== Applying cluster-scoped infrastructure resources ==="
echo "(These bypass kustomize to preserve their target namespaces)"
echo ""

# Logging (openshift-logging namespace)
echo "[1/8] LokiStack (openshift-logging)..."
oc apply -f "$BASE_DIR/loki.yaml"

echo "[2/8] ClusterLogForwarder + RBAC (openshift-logging)..."
oc apply -f "$BASE_DIR/cluster-logging.yaml"

echo "[3/8] Cluster Observability UIPlugins..."
oc apply -f "$BASE_DIR/cluster-observability.yaml"

# Network Observability (netobserv namespace)
echo "[4/8] NetObserv LokiStack (netobserv)..."
oc apply -f "$BASE_DIR/netobserv-loki.yaml"

echo "[5/8] NetObserv FlowCollector..."
oc apply -f "$BASE_DIR/netobserv.yaml"

# Security (stackrox namespace)
echo "[6/8] RHACS Central + SecuredCluster (stackrox)..."
oc apply -f "$BASE_DIR/rhacs.yaml"

# Descheduler (openshift-kube-descheduler-operator namespace)
echo "[7/8] KubeDescheduler..."
oc apply -f "$BASE_DIR/kubedescheduler.yaml"

# Service Mesh & GitOps (various operator namespaces)
echo "[8/8] Service Mesh, Istio CNI, Kiali, GitOps, Pipelines, RHDH..."
oc apply -f "$BASE_DIR/service-mesh.yaml"
oc apply -f "$BASE_DIR/istio-cni.yaml"
oc apply -f "$BASE_DIR/kiali.yaml"
oc apply -f "$BASE_DIR/gitops.yaml"
oc apply -f "$BASE_DIR/pipelines.yaml"
oc apply -f "$BASE_DIR/rhdh.yaml"

echo ""
echo "=== Done! All cluster-scoped resources applied ==="
echo ""
echo "Next steps:"
echo "  1. Apply payu-dev resources: oc apply -k infrastructure/openshift/infra/overlays/dev/"
echo "  2. Init Vault:               oc apply -f $BASE_DIR/vault-init-job.yaml"
echo "  3. Deploy services:           oc apply -k infrastructure/openshift/overlays/dev/"
