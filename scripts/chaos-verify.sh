#!/bin/bash
set -euo pipefail
# chaos-verify.sh — verify Litmus / Kraken / Cerberus per CHAOS-ENV-001 / ADR-0024
# Ponytail: checks operator + CRDs + RBAC per promoted ns + ChaosEngine dry-run + kraken/cerberus + pipeline SA
# Usage: ./scripts/chaos-verify.sh
REPORT_DIR="reports/chaos"
mkdir -p "$REPORT_DIR"
echo "[chaos-verify] CHAOS-ENV-001 Litmus + Kraken/Cerberus verification"
echo "[chaos-verify] 1) Litmus operator (ns litmus, 6 pods, CRDs)"
oc get pods -n litmus --no-headers 2>&1 | tee "$REPORT_DIR/litmus-pods.log"
oc get pods -n litmus -l app.kubernetes.io/name=chaos-operator-ce --no-headers 2>&1 | tee -a "$REPORT_DIR/litmus-pods.log" || echo "WARN: chaos-operator-ce not found in litmus"
oc get crd chaosengines.litmuschaos.io chaosexperiments.litmuschaos.io chaosresults.litmuschaos.io --no-headers 2>&1 | tee "$REPORT_DIR/litmus-crd.log"
oc explain chaosengine --api-version=litmuschaos.io/v1alpha1 2>&1 | head -20 | tee -a "$REPORT_DIR/litmus-crd.log"
echo "[chaos-verify] 2) Litmus RBAC per promoted ns (payu-sit/uat/preprod/payu)"
for NS in payu-sit payu-uat payu-preprod payu; do
  echo "--- $NS ---" | tee -a "$REPORT_DIR/litmus-rbac.log"
  oc get serviceaccount litmus-admin -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/litmus-rbac.log" || echo "WARN: litmus-admin SA missing in $NS" | tee -a "$REPORT_DIR/litmus-rbac.log"
  oc get role litmus-admin -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/litmus-rbac.log" || echo "WARN: litmus-admin Role missing in $NS" | tee -a "$REPORT_DIR/litmus-rbac.log"
  oc get rolebinding litmus-admin -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/litmus-rbac.log" || echo "WARN: litmus-admin Binding missing in $NS" | tee -a "$REPORT_DIR/litmus-rbac.log"
  oc get chaosengine -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/litmus-rbac.log" || echo "no chaosengine in $NS (dry-run will create)"
  oc get networkpolicy allow-chaos-platform-traffic -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/litmus-rbac.log" || echo "WARN: allow-chaos-platform-traffic missing in $NS" | tee -a "$REPORT_DIR/litmus-rbac.log"
done
echo "[chaos-verify] 2b) Pipeline SA cross-ns RBAC (CHAOS-RBAC-001)"
for NS in payu-sit payu-uat payu-preprod payu; do
  oc get role payu-tekton-litmus-gate -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/pipeline-rbac.log" || echo "WARN: payu-tekton-litmus-gate Role missing in $NS" | tee -a "$REPORT_DIR/pipeline-rbac.log"
  oc get rolebinding payu-tekton-litmus-gate -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/pipeline-rbac.log" || echo "WARN: pipeline RBAC missing in $NS" | tee -a "$REPORT_DIR/pipeline-rbac.log"
done
echo "[chaos-verify] 3) Kraken/Cerberus in payu-preprod + payu (infra-level chaos)"
for NS in payu-preprod payu; do
  echo "--- $NS kraken/cerberus ---" | tee -a "$REPORT_DIR/kraken.log"
  oc get pods -n "$NS" -l app=cerberus --no-headers 2>&1 | tee -a "$REPORT_DIR/kraken.log" || echo "no cerberus pods in $NS" | tee -a "$REPORT_DIR/kraken.log"
  oc get pods -n "$NS" -l app=kraken --no-headers 2>&1 | tee -a "$REPORT_DIR/kraken.log" || echo "no kraken pods in $NS (Job)" | tee -a "$REPORT_DIR/kraken.log"
  oc get deployment cerberus -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/kraken.log" || echo "WARN: cerberus deployment missing in $NS" | tee -a "$REPORT_DIR/kraken.log"
  oc get configmap kraken-config -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/kraken.log" || echo "WARN: kraken-config missing in $NS" | tee -a "$REPORT_DIR/kraken.log"
  oc get configmap cerberus-config -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/kraken.log" || echo "WARN: cerberus-config missing in $NS" | tee -a "$REPORT_DIR/kraken.log"
  oc get serviceaccount cerberus -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/kraken.log" || echo "WARN: cerberus SA missing in $NS" | tee -a "$REPORT_DIR/kraken.log"
  oc get serviceaccount kraken-chaos -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/kraken.log" || echo "WARN: kraken-chaos SA missing in $NS" | tee -a "$REPORT_DIR/kraken.log"
  oc get clusterrolebinding cerberus-cluster-reader-payu-preprod --no-headers 2>&1 | tee -a "$REPORT_DIR/kraken.log" || echo "WARN: cerberus CRB missing" | tee -a "$REPORT_DIR/kraken.log"
  oc get job kraken-run -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/kraken.log" || echo "no kraken job in $NS" | tee -a "$REPORT_DIR/kraken.log"
done
echo "[chaos-verify] 4) Dry-run ChaosEngine (no cluster mutation)"
oc apply --dry-run=client -k infrastructure/platform/security/chaos/litmus --validate=true 2>&1 | tee "$REPORT_DIR/chaosengine-dryrun.log"
# Also dry-run per-file for clarity
oc create --dry-run=client -o yaml -f infrastructure/platform/security/chaos/litmus/chaosengine.yaml 2>&1 | head -40 | tee -a "$REPORT_DIR/chaosengine-dryrun.log" || \
  oc apply --dry-run=client -f infrastructure/platform/security/chaos/litmus/chaosengine.yaml 2>&1 | tee -a "$REPORT_DIR/chaosengine-dryrun.log"
echo "[chaos-verify] 4b) Dry-run Kraken/Cerberus"
oc apply --dry-run=client -k infrastructure/platform/security/chaos/kraken --validate=true 2>&1 | tee -a "$REPORT_DIR/chaosengine-dryrun.log"
echo "[chaos-verify] 5) Pods per env (rtk oc get pods --no-headers)"
for NS in payu-sit payu-uat payu-preprod payu payu-dev; do
  echo "--- $NS ---" | tee -a "$REPORT_DIR/pods.log"
  oc get pods -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/pods-$NS.log"
  # Count 1/1 Ready if rtk available
  if command -v rtk >/dev/null 2>&1; then
    echo "[rtk] rtk oc get pods -n $NS --no-headers" | tee -a "$REPORT_DIR/pods.log"
    rtk oc get pods -n "$NS" --no-headers 2>&1 | tail -20 | tee -a "$REPORT_DIR/pods.log" || true
  fi
done
echo "[chaos-verify] 6) Logs 0 ERROR/WARN (since 60s)"
for NS in payu-dev payu-sit payu-uat payu-preprod payu; do
  echo "--- $NS logs ---" | tee -a "$REPORT_DIR/logs.log"
  oc logs --since=60s -n "$NS" -l app.kubernetes.io/part-of=payu --tail=50 2>&1 | grep -E '"level":"ERROR"|"level":"WARN"' | head -20 | tee -a "$REPORT_DIR/logs-$NS.log" || echo "0 ERROR/WARN in $NS" | tee -a "$REPORT_DIR/logs.log"
  if command -v rtk >/dev/null 2>&1; then
    echo "[rtk] rtk oc logs --since=60s -n $NS" | tee -a "$REPORT_DIR/logs.log"
    rtk oc logs --since=60s -n "$NS" -l app.kubernetes.io/part-of=payu --tail=20 2>&1 | grep -E 'ERROR|WARN' | head -5 | tee -a "$REPORT_DIR/logs.log" || echo "0 ERROR/WARN (rtk) in $NS" | tee -a "$REPORT_DIR/logs.log"
  fi
done
echo "[chaos-verify] 7) Gate skip-infra removed check"
grep -R "skipping gate\|skip.*infra\|Chaos infra unavailable" infrastructure/platform/cicd/tekton/catalog/litmus-gate-task.yaml infrastructure/platform/cicd/tekton/catalog/kraken-gate-task.yaml infrastructure/platform/cicd/tekton/tasks/litmus-gate-task.yaml infrastructure/platform/cicd/tekton/tasks/kraken-gate-task.yaml 2>&1 | tee "$REPORT_DIR/skip-check.log" && echo "WARN: skip-infra still present" | tee -a "$REPORT_DIR/skip-check.log" || echo "OK: skip-infra removed (no skip strings found)" | tee -a "$REPORT_DIR/skip-check.log"
echo "[chaos-verify] done → $REPORT_DIR"
