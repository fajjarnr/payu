#!/bin/bash
# PayU Simulators + Lending Rules E2E
set -e
POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')

echo "=== Simulators + Rules ==="

for svc in \
  "bi-fast:http://bi-fast-simulator.payu-dev.svc.cluster.local:8080/q/health" \
  "biller:http://biller-simulator.payu-dev.svc.cluster.local:8080/q/health" \
  "dukcapil:http://dukcapil-simulator.payu-dev.svc.cluster.local:8080/q/health" \
  "qris:http://qris-simulator.payu-dev.svc.cluster.local:8080/q/health" \
  "va:http://va-simulator.payu-dev.svc.cluster.local:8080/q/health" \
  "loan-origination:http://loan-origination-process.payu-dev.svc.cluster.local:8080/actuator/health" \
  "lending-rules:http://lending-rules.payu-dev.svc.cluster.local:8080/actuator/health" \
; do
  name="${svc%%:*}" url="${svc#*:}"
  code=$(oc exec -n payu-dev "$POD" -- timeout 5 curl -skS -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "TO")
  echo "  $name: $code"
done

echo "DONE"
