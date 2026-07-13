#!/bin/bash
# ============================================
# PayU Account Service E2E Test
# Status + lookup queries through gateway -> account-service
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

assert_json() {
    local label="$1" field="$2" expected="$3"
    local actual
    actual=$(python3 -c "
import json, sys
d = json.load(open('$TMPFILE'))
parts = '$field'.split('.')
for p in parts:
    if isinstance(d, dict): d = d.get(p, {})
    elif isinstance(d, list): d = d[0].get(p, {})
print(d)
" 2>/dev/null)
    if [ "$actual" = "$expected" ]; then
        printf "  ✅ %s = %s\n" "$label" "$actual"
    else
        printf "  ❌ %s expected='%s' got='%s'\n" "$label" "$expected" "$actual"
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
    UP="?user_key=$USERKEY"
else
    BASE="http://localhost:8080"
    UP=""
fi

echo "MODE=$GATEWAY_MODE"

echo
echo "========== Account Service Status & Lookup =========="

T1=$(run_test "T1: Account service status (GET /api/v1/accounts)" \
    "$BASE/api/v1/accounts$UP" \
    -H "Authorization: Bearer $JWT")
assert_http "T1 status" "200" "$T1"
assert_json "T1 service" "data.service" "account-service"

T2=$(run_test "T2: Phone lookup (GET /api/v1/accounts/lookup?phone=...)" \
    "$BASE/api/v1/accounts/lookup?phone=081234567890&user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT")
assert_http "T2 lookup" "200" "$T2"

T3=$(run_test "T3: Account IDs by user (inter-service, GET /api/v1/accounts/users/{id}/account-ids)" \
    "$BASE/api/v1/accounts/users/15ded37b-d5a2-46c5-a68d-f11dc36d4f6a/account-ids$UP" \
    -H "Authorization: Bearer $JWT")
assert_http "T3 account-ids" "200" "$T3"

echo
echo "========== Error Flows =========="

T4=$(run_test "T4: No JWT (401)" \
    "$BASE/api/v1/accounts$UP")
assert_http "T4 no JWT" "401" "$T4"

T5=$(run_test "T5: Invalid JWT (401)" \
    "$BASE/api/v1/accounts$UP" \
    -H "Authorization: Bearer dead.invalid.token")
assert_http "T5 invalid JWT" "401" "$T5"

echo
if [ "$FAILED" -eq 0 ]; then
    echo "=== ALL 5 TESTS PASSED ==="
else
    echo "=== SOME TESTS FAILED ==="
    exit 1
fi
