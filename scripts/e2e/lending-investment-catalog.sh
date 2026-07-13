#!/bin/bash
# PayU Lending + Investment + Product Catalog E2E
set -e
TMPFILE=/tmp/r.json; FAILED=0
POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
L="http://lending-service.payu-dev.svc.cluster.local:8080"
I="http://investment-service.payu-dev.svc.cluster.local:8080"
C="http://product-catalog-service.payu-dev.svc.cluster.local:8080"
CS=$(tr -d '[:space:]' < /tmp/client-secret.txt)
JWT=$(curl -skS -X POST "https://sso-payu-dev.apps.payu.ocp.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "client_id=payu-backend" --data-urlencode "client_secret=$CS" \
  --data-urlencode "grant_type=password" --data-urlencode "username=customer1" \
  --data-urlencode "password=Customer1-test" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)
[ ${#JWT} -lt 20 ] && { echo "FATAL: no JWT"; exit 1; }

ok(){ [ "$3" = "$2" ] && echo "  ✅ $1 HTTP=$3" || { echo "  ❌ $1 exp=$2 got=$3"; FAILED=1; }; }
run_test() {
    local label="$1"; shift; sleep 0.3
    local code=$(oc exec -n payu-dev "$POD" -- curl -skS -o /tmp/r.json -w "%{http_code}" "$@" 2>/dev/null)
    oc exec -n payu-dev "$POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null
    printf "\n%s HTTP=%s\n" "$label" "$code" >&2; echo "$code"
}

echo "=== Lending ==="
T1=$(run_test "T1: Status" "$L/api/v1/lending" -H "Authorization: Bearer $JWT"); ok "T1" "200" "$T1"
T2=$(run_test "T2: Credit score (404 — no data)" "$L/api/v1/lending/credit-score" -H "Authorization: Bearer $JWT"); ok "T2" "404" "$T2"
T3=$(run_test "T3: No JWT → 401" "$L/api/v1/lending"); ok "T3" "401" "$T3"

echo "=== Investment ==="
T4=$(run_test "T4: Status" "$I/api/v1/investments" -H "Authorization: Bearer $JWT"); ok "T4" "200" "$T4"
T5=$(run_test "T5: Gold (400 — no account)" "$I/api/v1/investments/gold/me" -H "Authorization: Bearer $JWT"); ok "T5" "400" "$T5"
T6=$(run_test "T6: No JWT → 401" "$I/api/v1/investments"); ok "T6" "401" "$T6"

echo "=== Product Catalog ==="
T7=$(run_test "T7: Public" "$C/products"); ok "T7" "200" "$T7"
T8=$(run_test "T8: By type" "$C/products?type=loan"); ok "T8" "200" "$T8"

[ "$FAILED" -eq 0 ] && echo "ALL 8 PASSED" || { echo "FAILED"; exit 1; }
