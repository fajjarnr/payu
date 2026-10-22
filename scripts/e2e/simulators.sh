#!/bin/bash
# PayU Simulators + Lending Rules E2E
set -e
# shellcheck source=env.sh
. "$(dirname "$0")/env.sh"
POD=$(oc get pod -n "$PAYU_ENV" -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')

echo "=== Simulators + Rules ==="

for svc in \
  "bi-fast:http://bi-fast-simulator.$PAYU_ENV.svc.cluster.local:8080/q/health" \
  "biller:http://biller-simulator.$PAYU_ENV.svc.cluster.local:8080/q/health" \
  "dukcapil:http://dukcapil-simulator.$PAYU_ENV.svc.cluster.local:8080/q/health" \
  "qris:http://qris-simulator.$PAYU_ENV.svc.cluster.local:8080/q/health" \
  "va:http://va-simulator.$PAYU_ENV.svc.cluster.local:8080/q/health" \
  "loan-origination:http://loan-origination-process.$PAYU_ENV.svc.cluster.local:8080/actuator/health" \
  "lending:http://lending-service.$PAYU_ENV.svc.cluster.local:8080/actuator/health" \
; do
  name="${svc%%:*}" url="${svc#*:}"
  code=$(oc exec -n "$PAYU_ENV" "$POD" -- timeout 5 curl -skS -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "TO")
  echo "  $name: $code"
done

echo "DONE"
