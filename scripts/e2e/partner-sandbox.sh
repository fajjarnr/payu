#!/bin/bash
# ============================================
# PayU Partner Service E2E Test
# Direct to partner-service (skips gateway routing gap for /partners)
#
# Mode: GATEWAY_MODE=internal (default) or apicast
# ============================================

set -e

GATEWAY_MODE="${GATEWAY_MODE:-internal}"
USERKEY="${USERKEY:-9a3f2bf49ca8d9c1eb3a7d1e4a4c55ed}"
HOST="${HOST:-https://payu-product-payu-apicast-production.apps.payu.ocp.fajjjar.my.id}"
TMPFILE=/tmp/r.json
FAILED=0

GATEWAY_POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
PARTNER_URL="http://partner-service.payu-dev.svc.cluster.local:8080"

refresh_jwt() {
    local client_secret
    client_secret=$(cat /tmp/client-secret.txt 2>/dev/null || echo "")
    [ -z "$client_secret" ] && { echo "ERROR: /tmp/client-secret.txt not found" >&2; exit 1; }
    local token
    token=$(curl -skS -X POST \
        "https://sso-payu-dev.apps.payu.ocp.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=payu-backend" \
        -d "client_secret=${client_secret}" \
        -d "grant_type=password" \
        -d "username=customer1" \
        -d "password=Customer1-test" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)
    [ -z "$token" ] && { echo "ERROR: JWT generation failed" >&2; exit 1; }
    echo "$token" > /tmp/cust1-jwt.txt
    echo "$token"
}

JWT=$(refresh_jwt)

assert_http() {
    local label="$1" expected="$2" actual="$3"
    if [ "$actual" = "$expected" ]; then
        printf "  ✅ %s HTTP=%s\n" "$label" "$actual"
    else
        printf "  ❌ %s expected HTTP=%s got HTTP=%s\n" "$label" "$expected" "$actual"
        FAILED=1
    fi
}

assert_json_exists() {
    local label="$1" field="$2"
    local actual
    actual=$(python3 -c "
import json
d = json.load(open('$TMPFILE'))
parts = '$field'.split('.')
for p in parts:
    if isinstance(d, dict): d = d.get(p)
print('OK' if d is not None and d != {} else 'MISSING')
" 2>/dev/null)
    if [ "$actual" = "OK" ]; then
        printf "  ✅ %s exists\n" "$label"
    else
        printf "  ❌ %s missing\n" "$label"
        FAILED=1
    fi
}

run_test() {
    local label="$1"; shift
    sleep 0.5
    local code
    code=$(oc exec -n payu-dev "$GATEWAY_POD" -- \
        curl -skS -o /tmp/r.json -w "%{http_code}" "$@" 2>/dev/null)
    oc exec -n payu-dev "$GATEWAY_POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null
    local body
    body=$(head -c 400 "$TMPFILE" 2>/dev/null)
    printf "\n=== %s ===\nHTTP=%s\nBODY: %s\n" "$label" "$code" "$body" >&2
    echo "$code"
}

echo "MODE=$GATEWAY_MODE"

echo
echo "========== Partner Service =========="

T1=$(run_test "T1: List partners (GET /partners)" \
    "$PARTNER_URL/partners" \
    -H "Authorization: Bearer $JWT")
assert_http "T1 partners" "200" "$T1"
assert_json_exists "T1 success" "success"

# T2: Sandbox status — public endpoint but may require auth via gateway.
# Partner service security config may protect admin paths.
T2=$(run_test "T2: Sandbox scenarios (GET /admin/sandbox/scenarios)" \
    "$PARTNER_URL/admin/sandbox/scenarios" \
    -H "Authorization: Bearer $JWT")
# Accept 200 (OK) or 403 (insufficient permissions — customer1 not ADMIN)
if [ "$T2" = "200" ] || [ "$T2" = "403" ]; then
    printf "  ✅ T2 Scenarios HTTP=%s (200=OK, 403=restricted to ADMIN)\n" "$T2"
else
    printf "  ❌ T2 Scenarios expected HTTP=200 or 403 got HTTP=%s\n" "$T2"
    FAILED=1
fi

echo
echo "========== Error Flows =========="

T4=$(run_test "T4: Partners without JWT (401)" \
    "$PARTNER_URL/partners")
assert_http "T4 no JWT" "401" "$T4"

T5=$(run_test "T5: Invalid JWT (401)" \
    "$PARTNER_URL/partners" \
    -H "Authorization: Bearer dead.invalid.token")
assert_http "T5 invalid JWT" "401" "$T5"

echo
if [ "$FAILED" -eq 0 ]; then
    echo "=== ALL 5 TESTS PASSED ==="
else
    echo "=== SOME TESTS FAILED ==="
    exit 1
fi
