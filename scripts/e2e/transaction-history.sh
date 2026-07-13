#!/bin/bash
# ============================================
# PayU Transaction E2E Test
# Read-only transaction queries + transfer attempt through
#   3scale APIcast -> gateway-service -> transaction-service
#
# Pre-reqs:
#   1. Same as cards-crud.sh (JWT, user_key, customer1)
#   2. wallet-service has wallet for accountId=7a51ced3... with
#      balance from wallet-bootstrap.sql
#   3. For T5 (transfer POST): a second test account must exist
#      with a known account number as recipient.
#      - If recipient not set, T5 expects 422 (business rule)
#      - Set RECIPIENT_ACCT env var to run a real transfer
# ============================================

set -e

USERKEY="${USERKEY:-04dc03f2e2a776bffcb9b16eb9f93796}"
HOST="${HOST:-https://payu-product-payu-apicast-production.apps.payu.ocp.fajjjar.my.id}"
ACCT="${ACCT:-7a51ced3-5602-40fb-96e7-1703e9243ed5}"
RECIPIENT_ACCT="${RECIPIENT_ACCT:-}"
TMPFILE=/tmp/r.json
FAILED=0

GATEWAY_POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
JWT=$(oc exec -n payu-dev "$GATEWAY_POD" -- cat /tmp/cust1-jwt.txt 2>/dev/null)
[ -z "$JWT" ] && { echo "ERROR: no JWT at /tmp/cust1-jwt.txt in gateway-service pod"; exit 1; }

# ---- helpers ----

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
import json
d = json.load(open('$TMPFILE'))
parts = '$field'.split('.')
for p in parts:
    if isinstance(d, dict):
        d = d.get(p, {})
    elif isinstance(d, list) and p.isdigit():
        d = d[int(p)] if int(p) < len(d) else None
    else:
        d = d[0].get(p, {}) if isinstance(d, list) and len(d) > 0 else None
try:
    print(d)
except:
    print('__PARSE_ERROR__')
" 2>/dev/null)
    if [ "$actual" = "$expected" ]; then
        printf "  ✅ %s = %s\n" "$label" "$actual"
    else
        printf "  ❌ %s expected='%s' got='%s'\n" "$label" "$expected" "$actual"
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
    if isinstance(d, dict):
        d = d.get(p)
    elif isinstance(d, list) and p.isdigit():
        d = d[int(p)] if int(p) < len(d) else None
    elif isinstance(d, list):
        d = d[0].get(p, {})
try:
    print('OK' if d is not None and d != {} else 'MISSING')
except:
    print('MISSING')
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
    code=$(curl -skS -o "$TMPFILE" -w "%{http_code}" "$@" 2>/dev/null)
    local body
    body=$(head -c 500 "$TMPFILE" 2>/dev/null)
    printf "\n=== %s ===\n" "$label"
    printf "HTTP=%s\nBODY: %s\n" "$code" "$body"
    echo "$code"
}

# ============================================
echo
echo "========== PHASE 1: Transaction History (Read-only) =========="

T1=$(run_test "T1: List transactions for account (GET /api/v1/transactions?accountId=...)" \
    "${HOST}/api/v1/transactions?accountId=${ACCT}&user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT")
assert_http "T1 list" "200" "$T1"
assert_json "T1 success" "success" "True"

T2=$(run_test "T2: List with pagination (GET /api/v1/transactions?page=0&size=5)" \
    "${HOST}/api/v1/transactions?accountId=${ACCT}&page=0&size=5&user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT")
assert_http "T2 paginated" "200" "$T2"

T3=$(run_test "T3: List with status filter (GET /api/v1/transactions?status=SUCCESS)" \
    "${HOST}/api/v1/transactions?accountId=${ACCT}&status=SUCCESS&user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT")
assert_http "T3 status filter" "200" "$T3"

T4=$(run_test "T4: List with date range (GET /api/v1/transactions?startDate=...&endDate=...)" \
    "${HOST}/api/v1/transactions?accountId=${ACCT}&startDate=2026-01-01&endDate=2026-12-31&user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT")
assert_http "T4 date range" "200" "$T4"

# ============================================
echo
echo "========== PHASE 2: Transfer Initiation =========="

if [ -n "$RECIPIENT_ACCT" ]; then
    T5=$(run_test "T5: Initiate transfer (POST /api/v1/transactions/transfer)" \
        -X POST "${HOST}/api/v1/transactions/transfer?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT" \
        -H "Content-Type: application/json" \
        -H "X-Idempotency-Key: $(uuidgen)" \
        -d "{
            \"senderAccountId\":\"$ACCT\",
            \"recipientAccountNumber\":\"$RECIPIENT_ACCT\",
            \"amount\":1000,
            \"currency\":\"IDR\",
            \"description\":\"E2E test transfer\",
            \"type\":\"INTERNAL\"
        }")
    assert_http "T5 transfer" "201" "$T5"
    assert_json "T5 success" "success" "True"

    TX_ID=$(python3 -c "
import json
d = json.load(open('$TMPFILE'))
print((d.get('data') or {}).get('id', d.get('data', {}).get('transactionId', 'NO_ID')))
" 2>/dev/null)
    echo "TX_ID=$TX_ID"
    echo "$TX_ID" > /tmp/tx_id.txt

    if [ -n "$TX_ID" ] && [ "$TX_ID" != "NO_ID" ]; then
        sleep 1
        T6=$(run_test "T6: Get transaction by ID (GET /api/v1/transactions/{id})" \
            "${HOST}/api/v1/transactions/${TX_ID}?user_key=$USERKEY" \
            -H "Authorization: Bearer $JWT")
        assert_http "T6 get tx" "200" "$T6"
    fi
else
    echo
    echo "=== SKIPPED transfer POST: set RECIPIENT_ACCT env var to run ==="
    echo "    RECIPIENT_ACCT should be a valid 10-20 digit account number from a second test user"
fi

# ============================================
echo
echo "========== PHASE 3: Error Flows =========="

T7=$(run_test "T7: Get nonexistent transaction (GET /api/v1/transactions/{uuid})" \
    "${HOST}/api/v1/transactions/00000000-0000-0000-0000-000000000000?user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT")
assert_http "T7 not found" "404" "$T7"

T8=$(run_test "T8: List transactions without JWT (401)" \
    "${HOST}/api/v1/transactions?accountId=${ACCT}&user_key=$USERKEY")
assert_http "T8 no JWT" "401" "$T8"

T9=$(run_test "T9: Invalid JWT (401)" \
    "${HOST}/api/v1/transactions?accountId=${ACCT}&user_key=$USERKEY" \
    -H "Authorization: Bearer dead.token.here")
assert_http "T9 invalid JWT" "401" "$T9"

T10=$(run_test "T10: Transfer with missing required fields (400)" \
    -X POST "${HOST}/api/v1/transactions/transfer?user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    -d '{"amount":1000}')
assert_http "T10 validation" "400" "$T10"

T11=$(run_test "T11: Transfer without idempotency key (400)" \
    -X POST "${HOST}/api/v1/transactions/transfer?user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    -d "{
        \"senderAccountId\":\"$ACCT\",
        \"recipientAccountNumber\":\"0000000000\",
        \"amount\":1000,
        \"currency\":\"IDR\",
        \"description\":\"no idempotency\",
        \"type\":\"INTERNAL\"
    }")
assert_http "T11 no idempotency" "400" "$T11"

# ============================================
echo
if [ "$FAILED" -eq 0 ]; then
    echo "=== ALL TESTS PASSED ==="
else
    echo "=== SOME TESTS FAILED ==="
    exit 1
fi
