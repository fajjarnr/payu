#!/usr/bin/env bash
# CICD-RESULTS-001 — Tekton Results Postgres single-instance health + finalizer cleanup
# Usage: ./scripts/verify-tekton-results.sh [--fix-finalizers]
set -euo pipefail
NS="openshift-pipelines"
echo "=== Tekton Results health check ==="
echo "[1] StatefulSet tekton-results-postgres"
oc get statefulset -n $NS tekton-results-postgres -o wide 2>&1 || echo "StatefulSet not found or no oc access (expected locally)"
echo ""
echo "[2] Pod tekton-results-postgres-0"
oc get pod -n $NS -l app=tekton-results-postgres -o wide 2>&1 || oc get pod -n $NS tekton-results-postgres-0 2>&1 || echo "Pod not found (expected locally)"
echo ""
echo "[3] TektonConfig results.external_db"
oc get tektonconfig config -o jsonpath='{.spec.result.is_external_db}' 2>&1 || echo "no oc"
echo " (false = single-instance, true = CNPG external)"
echo ""
echo "[4] TektonResult retention"
oc get cm -n $NS tekton-results-config-results-retention-policy -o yaml 2>&1 | head -n 20 || echo "no cm"
echo ""
echo "[5] Recent PipelineRun TaskRun with finalizer stuck?"
if [ "${1:-}" = "--fix-finalizers" ]; then
  echo "Fixing TaskRuns with results.tekton.dev/taskrun finalizer stuck >24h"
  for ns in payu-cicd; do
    oc get taskrun -n $ns --no-headers 2>&1 | head -n 20 || true
    # Example manual fix from 2026-08-25: strip finalizer
    # oc patch taskrun <name> -n payu-cicd --type merge -p '{"metadata":{"finalizers":null}}'
  done
else
  echo "Run with --fix-finalizers to attempt cleanup (requires oc)"
  oc get taskrun -n payu-cicd --no-headers 2>&1 | grep -c "results.tekton.dev" || echo "no TaskRun with finalizer or no oc"
fi
echo ""
echo "=== Decision ==="
echo "Current: single-instance tekton-results-postgres (1 replica) — fragile during node rotation (4 worker SchedulingDisabled 2026-08-24/25 caused dial tcp :5432 connection refused, finalizer stuck)"
echo "Builds: still healthy (gateway-service/web-app 1.18.46 Completed 15/15) — history may have gaps during rotation, but pipeline execution not blocked"
echo "HA option: CNPG payu-tekton-results Database tekton_results on payu-database (3/3 Healthy, RPO=0) + external_db true — requires Vault-backed Secret tekton-results-db + TektonConfig migration (operator reverts direct TektonResult edits, must patch TektonConfig). Deferred until Vault HA + restore test (see infrastructure/platform/cicd/tekton/results-external-db.md)"
echo "Tolerance: document single-instance + monitor + manual finalizer cleanup via this script; re-evaluate HA if p95 history loss >1% or rotation happens >1x/quarter"
