#!/bin/bash
# PayU Partner E2E — partners, SNAP BI, sandbox
set -e
TMPFILE=/tmp/r.json; FAILED=0
POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
P="http://partner-service.payu-dev.svc.cluster.local:8080"
CS=$(tr -d '[:space:]' < /tmp/client-secret.txt)
JWT=$(curl -skS -X POST "https://sso-payu-dev.apps.payu.ocp.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "client_id=payu-backend" --data-urlencode "client_secret=$CS" \
  --data-urlencode "grant_type=password" --data-urlencode "username=customer1" \
  --data-urlencode "password=Customer1-test" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)
[ ${#JWT} -lt 20 ] && { echo "FATAL: no JWT"; exit 1; }

ok(){ [ "$3" = "$2" ] && echo "  ✅ $1 HTTP=$3" || { echo "  ❌ $1 exp=$2 got=$3"; FAILED=1; }; true; }
run_test() {
    local label="$1"; shift; sleep 0.3
    local code=$(oc exec -n payu-dev "$POD" -- curl -skS -o /tmp/r.json -w "%{http_code}" "$@" 2>/dev/null)
    oc exec -n payu-dev "$POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null
    printf "\n%s HTTP=%s\n" "$label" "$code" >&2; echo "$code"
}

echo "=== Partner Service ==="
T1=$(run_test "T1: Partners list" "$P/partners" -H "Authorization: Bearer $JWT"); ok "T1" "200" "$T1"
T2=$(run_test "T2: No JWT → 401" "$P/partners"); ok "T2" "401" "$T2"
T3=$(run_test "T3: Sandbox status" "$P/admin/sandbox/status"); [ "$T3" = "200" ] && echo "  ✅ T3 HTTP=200" || [ "$T3" = "401" ] && echo "  ✅ T3 HTTP=401 (protected)" || { echo "  ❌ T3=$T3"; FAILED=1; }
T4=$(run_test "T4: SNAP auth" -X POST "$P/v1/partner/auth/token" -H "Content-Type: application/json" -d '{}'); ok "T4" "401" "$T4"
T5=$(run_test "T5: Partner ID" "$P/partners/1" -H "Authorization: Bearer $JWT"); [ "$T5" = "400" ] && echo "  ✅ T5 HTTP=400" || [ "$T5" = "404" ] && echo "  ✅ T5 HTTP=404" || { echo "  ❌ T5=$T5"; FAILED=1; }

[ "$FAILED" -eq 0 ] && echo "ALL 5 PASSED" || { echo "FAILED"; exit 1; }
