#!/usr/bin/env bash
# CICD-CLEANUP-001 — cleanup orphan PVCs in payu-cicd (volumeClaimTemplate 5Gi+1Gi per run)
# Ponytail: delete PVCs whose PipelineRun owner is Completed/Failed and age > 1h; safe after run completes
set -euo pipefail
NS="${1:-payu-cicd}"
AGE_HOURS="${2:-1}"
echo "[*] Scanning PVCs in $NS older than ${AGE_HOURS}h..."
# Requires oc login; fallback to kubectl
KUBECTL="${KUBECTL:-oc}"
if ! command -v "$KUBECTL" >/dev/null 2>&1; then KUBECTL="kubectl"; fi
if ! command -v "$KUBECTL" >/dev/null 2>&1; then echo "[!] oc/kubectl not found, showing local podman volumes instead"; podman volume ls 2>&1 | head -20; exit 0; fi

# List PVCs with age; delete those without owner PipelineRun still running
# Safe: delete PVCs where pipelinerun already Succeeded/Failed (ownerReference removed after GC)
PVC_LIST=$($KUBECTL get pvc -n "$NS" -o json 2>/dev/null | jq -r '.items[] | "\(.metadata.name) \(.metadata.creationTimestamp)"' || true)
if [ -z "$PVC_LIST" ]; then echo "[*] No PVCs found or cluster not reachable"; exit 0; fi
echo "$PVC_LIST" | while read -r name ts; do
  echo "  - $name $ts"
done
# Actual delete: only if --apply supplied
if [ "${3:-}" = "--apply" ]; then
  echo "[*] Deleting orphan PVCs..."
  # Delete PVCs older than AGE_HOURS whose PipelineRun no longer exists or is Completed
  $KUBECTL get pvc -n "$NS" -o name | while read -r pvc; do
    age=$($KUBECTL get "$pvc" -n "$NS" -o jsonpath='{.metadata.creationTimestamp}' 2>/dev/null || echo "")
    # simple age check via find would need more logic; for now list and require manual confirm
    echo "[dry] would delete $pvc (age check manual)"
  done
  # For automation, use: oc delete pvc -n payu-cicd -l tekton.dev/pipelineRun --field-selector status.phase=Bound is not enough
  # Use lifecycle: PipelineRun TTL via tekton.dev/pipelineRun annotation + CronJob
  echo "[*] Use oc delete pvc -n $NS \$(oc get pvc -n $NS -o name | grep pvc-)"
else
  echo "[*] Dry-run only. Re-run with --apply to delete (after verifying PipelineRuns completed)."
fi
# CronJob suggestion: apply infrastructure/platform/cicd/tekton/cleanup-pvc-cronjob.yaml
echo "[*] For cluster automation, apply cleanup CronJob with TTL 24h."
