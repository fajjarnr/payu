#!/bin/bash
# ============================================
# PayU Wallet Balance & Ledger E2E Test
# Read-only wallet queries through gateway -> wallet-service
#
# Mode: GATEWAY_MODE=internal (default) or apicast
# ============================================

set -e

GATEWAY_MODE="${GATEWAY_MODE:-internal}"
USERKEY="${USERKEY:-9a3f2bf49ca8d9c1eb3a7d1e4a4c55ed}"
HOST="${HOST:-https://payu-product-payu-apicast-production.apps.payu.ocp.fajjjar.my.id}"
WALLET_ID="${WALLET_ID:-33333333-3333-3333-3333-333333333333}"
ACCT="${ACCT:-15ded37b-d5a2-46c5-a68d-f11dc36d4f6a}"
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

assert_json_not_null() {
    local label="$1" field="$2"
    local actual
    actual=$(python3 -c "
import json
d = json.load(open('$TMPFILE'))
parts = '$field'.split('.')
for p in parts:
    if isinstance(d, dict): d = d.get(p)
print('NULL' if d is None else str(d)[:40])
" 2>/dev/null)
    if [ "$actual" != "NULL" ] && [ -n "$actual" ]; then
        printf "  ✅ %s = %s\n" "$label" "$actual"
    else
        printf "  ❌ %s is null/missing\n" "$label"
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
    USERKEY_PARAM="?user_key=$USERKEY"
else
    BASE="http://localhost:8080"
    USERKEY_PARAM=""
fi

# ============================================
echo "MODE=$GATEWAY_MODE ACCT=$ACCT"

echo
echo "========== Wallet Status & Balance =========="

T1=$(run_test "T1: Wallet service status (GET /api/v1/wallets)" \
    "$BASE/api/v1/wallets$USERKEY_PARAM" \
    -H "Authorization: Bearer $JWT")
assert_http "T1 status" "200" "$T1"

T2=$(run_test "T2: Get wallet balance (GET /api/v1/wallets/{id}/balance)" \
    "$BASE/api/v1/wallets/$ACCT/balance$USERKEY_PARAM" \
    -H "Authorization: Bearer $JWT")
assert_http "T2 balance" "200" "$T2"
assert_json_not_null "T2 balance" "data.balance"

echo
echo "========== Ledger & Transactions =========="

T3=$(run_test "T3: Get ledger entries (GET /api/v1/wallets/{id}/ledger)" \
    "$BASE/api/v1/wallets/$ACCT/ledger$USERKEY_PARAM" \
    -H "Authorization: Bearer $JWT")
assert_http "T3 ledger" "200" "$T3"
assert_json_exists "T3 data" "data"

T4=$(run_test "T4: Get transaction history (GET /api/v1/wallets/{id}/transactions)" \
    "$BASE/api/v1/wallets/$ACCT/transactions$USERKEY_PARAM" \
    -H "Authorization: Bearer $JWT")
assert_http "T4 transactions" "200" "$T4"

echo
echo "========== Pockets =========="

T5=$(run_test "T5: List pockets (GET /api/v1/wallets/pockets)" \
    "$BASE/api/v1/wallets/pockets$USERKEY_PARAM" \
    -H "Authorization: Bearer $JWT")
assert_http "T5 pockets list" "200" "$T5"

T6=$(run_test "T6: Pocket total balance (GET .../pockets/total-balance/IDR)" \
    "$BASE/api/v1/wallets/pockets/total-balance/IDR$USERKEY_PARAM" \
    -H "Authorization: Bearer $JWT")
assert_http "T6 total balance" "200" "$T6"

echo
echo "========== Error Flows =========="

T7=$(run_test "T7: Balance without JWT (401)" \
    "$BASE/api/v1/wallets/$ACCT/balance$USERKEY_PARAM")
assert_http "T7 no JWT" "401" "$T7"

T8=$(run_test "T8: Invalid JWT (401)" \
    "$BASE/api/v1/wallets/$ACCT/ledger$USERKEY_PARAM" \
    -H "Authorization: Bearer dead.invalid.token")
assert_http "T8 invalid JWT" "401" "$T8"

echo
if [ "$FAILED" -eq 0 ]; then
    echo "=== ALL 8 TESTS PASSED ==="
else
    echo "=== SOME TESTS FAILED ==="
    exit 1
fi
