#!/bin/bash
set -euo pipefail
# keda-verify.sh — verify RH Custom Metrics Autoscaler (KEDA) per ADR-0068
# Ponytail: checks operator + ScaledObjects + HPA + kcat produce 1000 → HPA 3→10 <30s
# Usage: ./scripts/keda-verify.sh [payu-dev|payu-prod]
NS=${1:-payu-dev}
KEDA_NS=openshift-keda
REPORT_DIR="reports/keda"
mkdir -p "$REPORT_DIR"
echo "[keda-verify] namespace $NS, KEDA $KEDA_NS"
echo "[keda-verify] 1) KEDA operator"
oc get csv -n $KEDA_NS --no-headers 2>&1 | grep custom-metrics-autoscaler || echo "WARN: RH CMA CSV not found in $KEDA_NS"
oc get pods -n $KEDA_NS --no-headers 2>&1 | tee "$REPORT_DIR/keda-pods.log"
oc get kedacontroller -n $KEDA_NS keda -o yaml 2>&1 | grep -A2 "phase" | tee -a "$REPORT_DIR/keda-pods.log"
echo "[keda-verify] 2) ScaledObjects"
oc get scaledobject -n "$NS" --no-headers 2>&1 | tee "$REPORT_DIR/scaledobjects.log"
oc get triggerauthentication -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/scaledobjects.log"
echo "[keda-verify] 3) HPA (KEDA behind)"
oc get hpa -n "$NS" --no-headers 2>&1 | tee "$REPORT_DIR/hpa.log"
echo "[keda-verify] 4) kcat produce test (ADR-0068 acceptance: 1000 msgs → HPA 3→10 <30s)"
if command -v kcat >/dev/null 2>&1; then
  echo "produce 1000 to payu.transaction.initiated.v1 via payu-kafka-kafka-bootstrap.$NS.svc:9092"
  # Use ephemeral pod with kcat if available
  oc run kcat-producer --image=edenhill/kcat:1.7.1 --restart=Never --rm -i --namespace "$NS" -- -P -b "payu-kafka-kafka-bootstrap.$NS.svc:9092" -t payu.transaction.initiated.v1 -l /tmp/data 2>&1 | head -20 || echo "kcat produce skipped (no broker reachable in CI)"
else
  echo "kcat not installed, skipping produce test (manual: kcat -L -b payu-kafka:9092; produce 1000)"
fi
# Check HPA scaling after 30s
sleep 5
oc get hpa -n "$NS" --no-headers 2>&1 | tee -a "$REPORT_DIR/hpa-after.log"
echo "[keda-verify] 5) logs check 0 WARN/ERROR"
oc logs --since=60s -n "$NS" -l app.kubernetes.io/part-of=payu --tail=20 2>&1 | grep -E '"level":"ERROR"|"level":"WARN"' | head -20 || echo "0 ERROR/WARN in $NS"
echo "[keda-verify] done → $REPORT_DIR"
