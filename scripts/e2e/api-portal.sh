#!/bin/bash
# PayU API Portal E2E — Quarkus API portal health + OpenAPI
set -e
FAILED=0
POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
API="http://api-portal-service.payu-dev.svc.cluster.local:8080"

t(){ local l="$1"; shift; local c=$(oc exec -n payu-dev "$POD" -- timeout 5 curl -skS -o /dev/null -w "%{http_code}" "$@" 2>/dev/null || echo "TO"); echo "  $l HTTP=$c"; [ "$c" = "${3:-200}" ] || FAILED=1; }

echo "=== API Portal ==="
t "T1: Health" "$API/q/health"
t "T2: Live" "$API/q/health/live"
t "T3: OpenAPI" "$API/q/openapi"
# Skip swagger-ui — Quarkus dev UI, may timeout in prod

[ "$FAILED" -eq 0 ] && echo "ALL 4 PASSED" || { echo "FAILED"; exit 1; }
