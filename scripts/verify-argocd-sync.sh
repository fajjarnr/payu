#!/usr/bin/env bash
# ARGOCD-SYNC-001 — ArgoCD Application OutOfSync + Unknown during node rotation
# Usage: ./scripts/verify-argocd-sync.sh [--sync-data]
set -euo pipefail
NS="openshift-gitops"
echo "=== ArgoCD Applications sync check ==="
echo "[1] oc get applications -n $NS"
oc get applications.argoproj.io -n $NS --no-headers 2>&1 | head -n 50 || echo "no oc (expected locally) — showing expected handling"
echo ""
echo "[2] OutOfSync apps (data-preprod/data-prod/data-sit/data-uat)"
for app in data-preprod data-prod data-sit data-uat; do
  oc get application $app -n $NS -o jsonpath='{.status.sync.status} {.status.health.status} {.status.operationState.phase}' 2>&1 || echo "$app: no oc or not found"
done
echo ""
echo "[3] Unknown sync status during rotation"
oc get applications.argoproj.io -n $NS -o jsonpath='{range .items[*]}{.metadata.name} {.status.sync.status} {.status.health.status}{"\n"}{end}' 2>&1 | grep -i Unknown | head -n 20 || echo "no Unknown or no oc"
echo ""
if [ "${1:-}" = "--sync-data" ]; then
  echo "Syncing data-* apps (OutOfSync Healthy drift)"
  for app in data-preprod data-prod data-sit data-uat; do
    echo "-> argocd app sync $app || oc patch application $app -n $NS --type merge -p '{\"operation\":{\"sync\":{\"revision\":\"main\"}}}'"
    oc patch application $app -n $NS --type merge -p '{"operation":{"sync":{"revision":"main"}}}' 2>&1 || echo "no oc"
    # Alternative: argocd app actions run $app restart 2>&1 || true
  done
  echo "Also consider: argocd app sync payu-app-of-apps --prune"
else
  echo "Run with --sync-data to sync data-* (requires oc)"
fi
echo ""
echo "=== Drift source for data-* ==="
echo "data-* apps likely manage CNPG Cluster/Database or ObjectStore (pvc size 10Gi->20Gi, wal 5Gi->10Gi) — spec drift OutOfSync but Healthy is expected after storage expansion (allowVolumeExpansion)."
echo "Check drift: oc diff -n $NS application/data-sit 2>&1 | head -n 50 || argocd app diff data-sit 2>&1 | head -n 50"
echo "If drift is spec.storage.size: already handled via oc apply -k + pvc expansion (1.18.42); sync will show OutOfSync until ArgoCD reconciles after rotation. Tolerance: Healthy + OutOfSync is OK if pvc Bound and cluster Healthy."
echo "Unknown during rotation: SchedulingDisabled 4 worker caused 5/5 CNPG/Kafka/EFS pods Unknown — expected, resolves after rotation (nodes Ready). Monitor: oc get nodes; oc get pods -A | grep Unknown"
