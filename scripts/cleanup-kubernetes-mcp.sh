#!/bin/bash
# Cleanup Kubernetes MCP Server ServiceAccount, RBAC, and token kubeconfig
#
# Usage: ./scripts/cleanup-kubernetes-mcp.sh [namespace] [sa-name]
# Example: ./scripts/cleanup-kubernetes-mcp.sh mcp mcp-viewer
# ==============================================================================

set -euo pipefail

NAMESPACE="${1:-mcp}"
SA_NAME="${2:-mcp-viewer}"
KUBECONFIG_PATH="${HOME}/.kube/mcp-viewer.kubeconfig"

echo "=== Cleaning up Kubernetes MCP Server Resources ==="

if command -v oc &>/dev/null; then
  oc adm policy remove-cluster-role-from-user cluster-reader "system:serviceaccount:${NAMESPACE}:${SA_NAME}" || true
  oc -n "${NAMESPACE}" delete sa "${SA_NAME}" --ignore-not-found || true
elif command -v kubectl &>/dev/null; then
  kubectl delete clusterrolebinding "${SA_NAME}-cluster-reader" --ignore-not-found || true
  kubectl -n "${NAMESPACE}" delete sa "${SA_NAME}" --ignore-not-found || true
fi

rm -f "${KUBECONFIG_PATH}"

echo "Cleaned up ServiceAccount '${SA_NAME}' in namespace '${NAMESPACE}' and deleted ${KUBECONFIG_PATH}."
