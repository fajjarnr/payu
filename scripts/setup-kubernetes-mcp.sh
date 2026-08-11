#!/bin/bash
# Setup Kubernetes MCP Server ServiceAccount, RBAC, Token, and isolated Kubeconfig
# Based on Red Hat Developer guide for OpenShift 4.19+ & Kubernetes
#
# Usage: ./scripts/setup-kubernetes-mcp.sh [namespace] [sa-name] [duration]
# Example: ./scripts/setup-kubernetes-mcp.sh mcp mcp-viewer 24h
# ==============================================================================

set -euo pipefail

NAMESPACE="${1:-mcp}"
SA_NAME="${2:-mcp-viewer}"
DURATION="${3:-24h}"
KUBECONFIG_PATH="${HOME}/.kube/mcp-viewer.kubeconfig"

echo "=== Kubernetes MCP Server Setup ==="
echo "Namespace:        ${NAMESPACE}"
echo "ServiceAccount:   ${SA_NAME}"
echo "Token Duration:   ${DURATION}"
echo "Kubeconfig Path:  ${KUBECONFIG_PATH}"
echo ""

# 1. Check CLI availability
if command -v oc &>/dev/null; then
  KUBE_CLI="oc"
elif command -v kubectl &>/dev/null; then
  KUBE_CLI="kubectl"
else
  echo "Error: Neither 'oc' nor 'kubectl' binary found in PATH." >&2
  exit 1
fi

echo "Using CLI: ${KUBE_CLI}"

# Verify cluster connection
if ! ${KUBE_CLI} whoami &>/dev/null && ! ${KUBE_CLI} cluster-info &>/dev/null; then
  echo "Error: Not logged into an OpenShift/Kubernetes cluster. Please run '${KUBE_CLI} login' first." >&2
  exit 1
fi

# 2. Create namespace if not exists
echo "[1/5] Ensuring namespace '${NAMESPACE}' exists..."
if [ "${KUBE_CLI}" = "oc" ]; then
  oc get project "${NAMESPACE}" &>/dev/null || oc new-project "${NAMESPACE}" --skip-config-write=true &>/dev/null || oc create namespace "${NAMESPACE}"
else
  kubectl get namespace "${NAMESPACE}" &>/dev/null || kubectl create namespace "${NAMESPACE}"
fi

# 3. Create ServiceAccount
echo "[2/5] Creating ServiceAccount '${SA_NAME}' in namespace '${NAMESPACE}'..."
${KUBE_CLI} create sa "${SA_NAME}" -n "${NAMESPACE}" --dry-run=client -o yaml | ${KUBE_CLI} apply -f -

# 4. Grant Cluster-wide read-only RBAC
echo "[3/5] Granting 'cluster-reader' ClusterRole to ServiceAccount..."
if [ "${KUBE_CLI}" = "oc" ]; then
  oc adm policy add-cluster-role-to-user cluster-reader "system:serviceaccount:${NAMESPACE}:${SA_NAME}"
else
  kubectl create clusterrolebinding "${SA_NAME}-cluster-reader" \
    --clusterrole=cluster-reader \
    --serviceaccount="${NAMESPACE}:${SA_NAME}" \
    --dry-run=client -o yaml | kubectl apply -f -
fi

# 5. Mint token
echo "[4/5] Minting time-bound ServiceAccount token (duration: ${DURATION})..."
TOKEN=""
if [ "${KUBE_CLI}" = "oc" ]; then
  TOKEN="$(oc -n "${NAMESPACE}" create token "${SA_NAME}" --duration="${DURATION}" 2>/dev/null || true)"
fi

if [ -z "${TOKEN}" ]; then
  # Fallback using kubectl / ServiceAccount token creation
  TOKEN="$(${KUBE_CLI} -n "${NAMESPACE}" create token "${SA_NAME}" --duration="${DURATION}" 2>/dev/null || true)"
fi

if [ -z "${TOKEN}" ]; then
  echo "Error: Failed to mint token using TokenRequest API." >&2
  exit 1
fi

# 6. Generate isolated kubeconfig
echo "[5/5] Building isolated Kubeconfig file at ${KUBECONFIG_PATH}..."
mkdir -p "${HOME}/.kube"

SERVER_URL=""
if [ "${KUBE_CLI}" = "oc" ]; then
  SERVER_URL="$(oc whoami --show-server 2>/dev/null || true)"
fi

if [ -z "${SERVER_URL}" ]; then
  SERVER_URL="$(${KUBE_CLI} config view --minify -o jsonpath='{.clusters[0].cluster.server}' 2>/dev/null || true)"
fi

if [ -z "${SERVER_URL}" ]; then
  echo "Error: Could not determine cluster server URL." >&2
  exit 1
fi

if [ "${KUBE_CLI}" = "oc" ]; then
  oc login --server="${SERVER_URL}" --token="${TOKEN}" --kubeconfig="${KUBECONFIG_PATH}" --insecure-skip-tls-verify=true &>/dev/null
else
  kubectl config set-cluster mcp-cluster --server="${SERVER_URL}" --insecure-skip-tls-verify=true --kubeconfig="${KUBECONFIG_PATH}"
  kubectl config set-credentials "${SA_NAME}" --token="${TOKEN}" --kubeconfig="${KUBECONFIG_PATH}"
  kubectl config set-context mcp-context --cluster=mcp-cluster --user="${SA_NAME}" --namespace="${NAMESPACE}" --kubeconfig="${KUBECONFIG_PATH}"
  kubectl config use-context mcp-context --kubeconfig="${KUBECONFIG_PATH}"
fi

chmod 600 "${KUBECONFIG_PATH}"

echo ""
echo "=== Setup Completed Successfully ==="
echo "Kubeconfig generated at: ${KUBECONFIG_PATH}"
echo ""
echo "Verification check:"
${KUBE_CLI} --kubeconfig="${KUBECONFIG_PATH}" whoami 2>/dev/null || ${KUBE_CLI} --kubeconfig="${KUBECONFIG_PATH}" config current-context
${KUBE_CLI} auth can-i --as="system:serviceaccount:${NAMESPACE}:${SA_NAME}" list pods --all-namespaces
