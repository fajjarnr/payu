#!/bin/bash
# ============================================
# PayU Promotion Service E2E Test
# Read-only promotion queries through gateway -> promotion-service
#
# Mode: GATEWAY_MODE=internal (default) or apicast
# ============================================

set -e

GATEWAY_MODE="${GATEWAY_MODE:-internal}"
USERKEY="${USERKEY:-9a3f2bf49ca8d9c1eb3a7d1e4a4c55ed}"
HOST="${HOST:-https://payu-product-payu-apicast-production.apps.payu.ocp.fajjjar.my.id}"
ACCT="${ACCT:-7753193d-b7e7-4e1e-bcb8-f9e4612e9207}"
TMPFILE=/tmp/r.json
FAILED=0

GATEWAY_POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')

refresh_jwt() {
    local client_secret
    client_secret=$(cat /tmp/client-secret.txt 2>/dev/null || echo "")
    [ -z "$client_secret" ] && { echo "ERROR: /tmp/client-secret.txt not found" >&2; exit 1; }
    local token
    token=$(curl -skS -X POST \
        "https://sso-dev.apps.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
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
    if [ "$GATEWAY_MODE" = "apicast" ]; then
        code=$(curl -skS -o "$TMPFILE" -w "%{http_code}" "$@" 2>/dev/null)
    else
        code=$(oc exec -n payu-dev "$GATEWAY_POD" -- \
            curl -skS -o /tmp/r.json -w "%{http_code}" "$@" 2>/dev/null)
        oc exec -n payu-dev "$GATEWAY_POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null
    fi
    local body
    body=$(head -c 400 "$TMPFILE" 2>/dev/null)
    printf "\n=== %s ===\nHTTP=%s\nBODY: %s\n" "$label" "$code" "$body" >&2
    echo "$code"
}

if [ "$GATEWAY_MODE" = "apicast" ]; then
    BASE="$HOST"
else
    BASE="http://localhost:8080"
fi

echo "MODE=$GATEWAY_MODE ACCT=$ACCT"

echo
echo "========== Promotions =========="

T1=$(run_test "T1: List all promotions (GET /api/v1/promotions)" \
    "$BASE/api/v1/promotions" \
    -H "Authorization: Bearer $JWT")
assert_http "T1 list all" "200" "$T1"

T2=$(run_test "T2: Get active promotions (GET /api/v1/promotions/active)" \
    "$BASE/api/v1/promotions/active" \
    -H "Authorization: Bearer $JWT")
assert_http "T2 active" "200" "$T2"

echo
echo "========== Cashbacks & Loyalty =========="

T3=$(run_test "T3: List cashbacks (GET /api/v1/cashbacks)" \
    "$BASE/api/v1/cashbacks" \
    -H "Authorization: Bearer $JWT")
assert_http "T3 cashbacks" "200" "$T3"

T4=$(run_test "T4: Cashback summary (GET /api/v1/cashbacks/account/{id}/summary)" \
    "$BASE/api/v1/cashbacks/account/$ACCT/summary" \
    -H "Authorization: Bearer $JWT")
assert_http "T4 summary" "200" "$T4"

T5=$(run_test "T5: Loyalty points balance (GET /api/v1/loyalty-points/account/{id}/balance)" \
    "$BASE/api/v1/loyalty-points/account/$ACCT/balance" \
    -H "Authorization: Bearer $JWT")
assert_http "T5 loyalty" "200" "$T5"

echo
echo "========== Error Flows =========="

T6=$(run_test "T6: No JWT (401)" \
    "$BASE/api/v1/promotions")
assert_http "T6 no JWT" "401" "$T6"

T7=$(run_test "T7: Invalid JWT (401)" \
    "$BASE/api/v1/promotions/active" \
    -H "Authorization: Bearer dead.invalid.token")
assert_http "T7 invalid JWT" "401" "$T7"

echo
if [ "$FAILED" -eq 0 ]; then
    echo "=== ALL 7 TESTS PASSED ==="
else
    echo "=== SOME TESTS FAILED ==="
    exit 1
fi
