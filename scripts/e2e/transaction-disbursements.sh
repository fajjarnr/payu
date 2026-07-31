#!/bin/bash
# ============================================
# PayU Transaction Disbursements + Split Bills + Smart Routing E2E
# Read-only queries through gateway -> transaction-service
# ============================================

set -e

GATEWAY_MODE="${GATEWAY_MODE:-internal}"
USERKEY="${USERKEY:-9a3f2bf49ca8d9c1eb3a7d1e4a4c55ed}"
HOST="${HOST:-https://payu-product-payu-apicast-production.apps.payu.ocp.fajjjar.my.id}"
ACCT="${ACCT:-15ded37b-d5a2-46c5-a68d-f11dc36d4f6a}"
TMPFILE=/tmp/r.json
FAILED=0

GATEWAY_POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
TX_URL="http://transaction-service.payu-dev.svc.cluster.local:8080"

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
    local actual=$(python3 -c "import json; d=json.load(open('$TMPFILE')); parts='$field'.split('.'); exec('for p in parts: d=d.get(p) if isinstance(d,dict) else d[0].get(p)'); print('OK' if d else 'MISSING')" 2>/dev/null || echo "MISSING")
    [ "$actual" = "OK" ] && printf "  ✅ %s exists\n" "$label" || { printf "  ❌ %s missing\n" "$label"; FAILED=1; }
}

run_test() {
    local label="$1"; shift
    sleep 0.4
    local code
    code=$(oc exec -n payu-dev "$GATEWAY_POD" -- \
        curl -skS -o /tmp/r.json -w "%{http_code}" "$@" 2>/dev/null)
    oc exec -n payu-dev "$GATEWAY_POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null
    printf "\n=== %s ===\nHTTP=%s\nBODY: %s\n" "$label" "$code" "$(head -c 300 "$TMPFILE")" >&2
    echo "$code"
}

echo "MODE=$GATEWAY_MODE ACCT=$ACCT"

echo
echo "====== Disbursements ======"

T1=$(run_test "T1: List disbursements (GET /api/v1/disbursements)" \
    "$TX_URL/api/v1/disbursements" \
    -H "Authorization: Bearer $JWT")
assert_http "T1 disbursements" "200" "$T1"

# T2: Batch disbursements needs sourceAccountId + proper role (ADMIN/PARTNER).
# customer1 may get 403 (insufficient permission). Accept both.
T2=$(run_test "T2: Batch disbursements (GET /api/v1/disbursements/batch)" \
    "$TX_URL/api/v1/disbursements/batch?sourceAccountId=00000000-0000-0000-0000-000000000000" \
    -H "Authorization: Bearer $JWT")
[ "$T2" = "200" ] || [ "$T2" = "403" ] && printf "  ✅ T2 Batch HTTP=%s (200=OK, 403=restricted role)\n" "$T2" || { printf "  ❌ T2 unexpected HTTP=%s\n" "$T2"; FAILED=1; }

# T3: Disbursement by idempotency key returns 400 (INVALID_ARGUMENT) not 404
T3=$(run_test "T3: Disbursement by idempotency key (400)" \
    "$TX_URL/api/v1/disbursements/by-idempotency-key/no-such-key" \
    -H "Authorization: Bearer $JWT")
[ "$T3" = "400" ] && printf "  ✅ T3 Idempotency not found HTTP=400 (expected)\n" || { printf "  ❌ T3 unexpected HTTP=%s\n" "$T3"; FAILED=1; }

echo
echo "====== Split Bills ======"

T4=$(run_test "T4: List split bills (GET /api/v1/split-bills)" \
    "$TX_URL/api/v1/split-bills/account/$ACCT" \
    -H "Authorization: Bearer $JWT")
assert_http "T4 split-bills" "200" "$T4"

# T5: Split bill may return 403 (permission) or 404 (not found). Either is fine.
T5=$(run_test "T5: Nonexistent split bill (404)" \
    "$TX_URL/api/v1/split-bills/00000000-0000-0000-0000-000000000000" \
    -H "Authorization: Bearer $JWT")
[ "$T5" = "404" ] || [ "$T5" = "403" ] && printf "  ✅ T5 Split bill HTTP=%s (404=not found, 403=restricted)\n" "$T5" || { printf "  ❌ T5 unexpected HTTP=%s\n" "$T5"; FAILED=1; }

echo
echo "====== Smart Routing & Scheduled Transfers ======"

T6=$(run_test "T6: Smart routes (GET /api/v1/transfers/routes)" \
    "$TX_URL/api/v1/transfers/routes?amount=10000&currency=IDR" \
    -H "Authorization: Bearer $JWT")
assert_http "T6 routes" "200" "$T6"

T7=$(run_test "T7: Scheduled transfers (GET /api/v1/scheduled-transfers/accounts/{id})" \
    "$TX_URL/api/v1/scheduled-transfers/accounts/$ACCT" \
    -H "Authorization: Bearer $JWT")
assert_http "T7 scheduled" "200" "$T7"

echo
echo "====== Error Flows ======"

T8=$(run_test "T8: No JWT (401)" \
    "$TX_URL/api/v1/disbursements")
assert_http "T8 no JWT" "401" "$T8"

T9=$(run_test "T9: Invalid JWT (401)" \
    "$TX_URL/api/v1/disbursements" \
    -H "Authorization: Bearer dead.invalid.token")
assert_http "T9 invalid JWT" "401" "$T9"

echo
[ "$FAILED" -eq 0 ] && echo "=== ALL 9 TESTS PASSED ===" || { echo "=== SOME TESTS FAILED ==="; exit 1; }
