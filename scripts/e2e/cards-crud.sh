#!/bin/bash
# ============================================
# PayU Cards CRUD E2E Test
# Full CREATE/READ/UPDATE/DELETE + idempotency + error flows
#
# Gateway routing: wallet-service -> Postgres
#
# Modes (set GATEWAY_MODE env var):
#   internal  - via oc exec from gateway pod (default, bypasses 3scale)
#   apicast   - through 3scale APIcast external URL
#
# Pre-reqs wallet-service needs a wallet row with accountId =
# customer1 Keycloak sub (15ded37b-...)
# ============================================

set -e

GATEWAY_MODE="${GATEWAY_MODE:-internal}"
USERKEY="${USERKEY:-04dc03f2e2a776bffcb9b16eb9f93796}"
HOST="${HOST:-https://payu-product-payu-apicast-production.apps.payu.ocp.fajjjar.my.id}"
# accountId = Keycloak sub (customer1 user UUID)
ACCT="${ACCT:-15ded37b-d5a2-46c5-a68d-f11dc36d4f6a}"
TMPFILE=/tmp/r.json
FAILED=0

GATEWAY_POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')

# Generate fresh JWT via Keycloak (ran from host, not pod)
refresh_jwt() {
    local client_secret
    client_secret=$(cat /tmp/client-secret.txt 2>/dev/null || echo "")
    if [ -z "$client_secret" ]; then
        echo "ERROR: /tmp/client-secret.txt not found — generate first with:" >&2
        echo "  ADMIN_TOKEN=\$(cat /tmp/admin-master-jwt.txt)" >&2
        echo "  curl -skS .../clients/PAYU_BACKEND_ID/client-secret -H 'Authorization: Bearer \$ADMIN_TOKEN' | ..." >&2
        exit 1
    fi
    local result
    result=$(curl -skS -X POST \
        "https://sso-payu-dev.apps.payu.ocp.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=payu-backend" \
        -d "client_secret=${client_secret}" \
        -d "grant_type=password" \
        -d "username=customer1" \
        -d "password=Customer1-test" 2>/dev/null)
    local token
    token=$(echo "$result" | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)
    if [ -z "$token" ]; then
        echo "ERROR: JWT generation failed: $result" >&2
        exit 1
    fi
    echo "$token" > /tmp/cust1-jwt.txt
    echo "$token"
}

JWT=$(refresh_jwt)
echo "JWT: ${JWT:0:30}..."

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

http_code() {
    curl -skS -o "$TMPFILE" -w "%{http_code}" "$@"
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
    # Print debug info to stderr so it doesn't pollute captured output
    local body
    body=$(head -c 500 "$TMPFILE" 2>/dev/null)
    printf "\n=== %s ===\nHTTP=%s\nBODY: %s\n" "$label" "$code" "$body" >&2
    echo "$code"
}

# ============================================
echo "MODE=$GATEWAY_MODE ACCT=$ACCT"
echo
echo "========== PHASE 1: CRUD Happy Path =========="

if [ "$GATEWAY_MODE" = "apicast" ]; then
    T1=$(run_test "T1: CREATE card (POST /api/v1/cards)" \
        -X POST "${HOST}/api/v1/cards?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT" \
        -H "Content-Type: application/json" \
        -d "{\"accountId\":\"$ACCT\",\"cardHolderName\":\"E2E Test Card\",\"dailyLimit\":5000000}")
else
    T1=$(run_test "T1: CREATE card (POST /api/v1/cards)" \
        -X POST "http://localhost:8080/api/v1/cards" \
        -H "Authorization: Bearer $JWT" \
        -H "Content-Type: application/json" \
        -d "{\"accountId\":\"$ACCT\",\"cardHolderName\":\"E2E Test Card\",\"dailyLimit\":5000000}")
fi
assert_http "T1 CREATE" "201" "$T1"

CARD_ID=$(python3 -c "
import json
d = json.load(open('$TMPFILE'))
print((d.get('data') or {}).get('id', 'NO_ID'))
" 2>/dev/null)
echo "CARD_ID=$CARD_ID"
echo "$CARD_ID" > /tmp/card_id.txt
[ "$CARD_ID" = "NO_ID" ] || [ -z "$CARD_ID" ] && { echo "FATAL: failed to extract card ID"; exit 1; }

if [ "$GATEWAY_MODE" = "apicast" ]; then
    T2=$(run_test "T2: READ cards list (GET /api/v1/cards?accountId=...)" \
        "${HOST}/api/v1/cards?accountId=${ACCT}&user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT")
else
    T2=$(run_test "T2: READ cards list (GET /api/v1/cards?accountId=...)" \
        "http://localhost:8080/api/v1/cards?accountId=${ACCT}" \
        -H "Authorization: Bearer $JWT")
fi
assert_http "T2 READ list" "200" "$T2"

if [ "$GATEWAY_MODE" = "apicast" ]; then
    T3=$(run_test "T3: READ specific card (GET /api/v1/cards/{id})" \
        "${HOST}/api/v1/cards/${CARD_ID}?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT")
else
    T3=$(run_test "T3: READ specific card (GET /api/v1/cards/{id})" \
        "http://localhost:8080/api/v1/cards/${CARD_ID}" \
        -H "Authorization: Bearer $JWT")
fi
assert_http "T3 READ one" "200" "$T3"

if [ "$GATEWAY_MODE" = "apicast" ]; then
    T4=$(run_test "T4: FREEZE (POST /api/v1/cards/{id}/freeze)" \
        -X POST "${HOST}/api/v1/cards/${CARD_ID}/freeze?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT")
else
    T4=$(run_test "T4: FREEZE (POST /api/v1/cards/{id}/freeze)" \
        -X POST "http://localhost:8080/api/v1/cards/${CARD_ID}/freeze" \
        -H "Authorization: Bearer $JWT")
fi
assert_http "T4 FREEZE" "200" "$T4"

sleep 1
if [ "$GATEWAY_MODE" = "apicast" ]; then
    T5=$(run_test "T5: Verify freeze status (GET /api/v1/cards/{id})" \
        "${HOST}/api/v1/cards/${CARD_ID}?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT")
else
    T5=$(run_test "T5: Verify freeze status (GET /api/v1/cards/{id})" \
        "http://localhost:8080/api/v1/cards/${CARD_ID}" \
        -H "Authorization: Bearer $JWT")
fi
assert_http "T5 READ after freeze" "200" "$T5"
assert_json "T5 status" "data.status" "FROZEN"

# ============================================
echo
echo "========== PHASE 2: Idempotency & Edge Cases =========="

if [ "$GATEWAY_MODE" = "apicast" ]; then
    T6=$(run_test "T6: FREEZE again — idempotent (POST /api/v1/cards/{id}/freeze)" \
        -X POST "${HOST}/api/v1/cards/${CARD_ID}/freeze?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT")
else
    T6=$(run_test "T6: FREEZE again — idempotent (POST /api/v1/cards/{id}/freeze)" \
        -X POST "http://localhost:8080/api/v1/cards/${CARD_ID}/freeze" \
        -H "Authorization: Bearer $JWT")
fi
assert_http "T6 FREEZE idempotent" "200" "$T6"

if [ "$GATEWAY_MODE" = "apicast" ]; then
    T7=$(run_test "T7: UNFREEZE (POST /api/v1/cards/{id}/unfreeze)" \
        -X POST "${HOST}/api/v1/cards/${CARD_ID}/unfreeze?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT")
else
    T7=$(run_test "T7: UNFREEZE (POST /api/v1/cards/{id}/unfreeze)" \
        -X POST "http://localhost:8080/api/v1/cards/${CARD_ID}/unfreeze" \
        -H "Authorization: Bearer $JWT")
fi
assert_http "T7 UNFREEZE" "200" "$T7"

sleep 1
if [ "$GATEWAY_MODE" = "apicast" ]; then
    T8=$(run_test "T8: Verify unfreeze status" \
        "${HOST}/api/v1/cards/${CARD_ID}?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT")
else
    T8=$(run_test "T8: Verify unfreeze status" \
        "http://localhost:8080/api/v1/cards/${CARD_ID}" \
        -H "Authorization: Bearer $JWT")
fi
assert_http "T8 READ after unfreeze" "200" "$T8"
assert_json "T8 status" "data.status" "ACTIVE"

if [ "$GATEWAY_MODE" = "apicast" ]; then
    T9=$(run_test "T9: UNFREEZE again — idempotent (POST /api/v1/cards/{id}/unfreeze)" \
        -X POST "${HOST}/api/v1/cards/${CARD_ID}/unfreeze?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT")
else
    T9=$(run_test "T9: UNFREEZE again — idempotent (POST /api/v1/cards/{id}/unfreeze)" \
        -X POST "http://localhost:8080/api/v1/cards/${CARD_ID}/unfreeze" \
        -H "Authorization: Bearer $JWT")
fi
assert_http "T9 UNFREEZE idempotent" "200" "$T9"

# ============================================
echo
echo "========== PHASE 3: Error Flows =========="

if [ "$GATEWAY_MODE" = "apicast" ]; then
    T10=$(run_test "T10: GET nonexistent card (404)" \
        "${HOST}/api/v1/cards/00000000-0000-0000-0000-000000000000?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT")
else
    T10=$(run_test "T10: GET nonexistent card (404)" \
        "http://localhost:8080/api/v1/cards/00000000-0000-0000-0000-000000000000" \
        -H "Authorization: Bearer $JWT")
fi
assert_http "T10 nonexistent card" "404" "$T10"

if [ "$GATEWAY_MODE" = "apicast" ]; then
    T11=$(run_test "T11: POST without JWT (401)" \
        -X POST "${HOST}/api/v1/cards?user_key=$USERKEY" \
        -H "Content-Type: application/json" \
        -d '{"accountId":"'$ACCT'","cardHolderName":"NoAuth","dailyLimit":1000}')
else
    T11=$(run_test "T11: POST without JWT (401)" \
        -X POST "http://localhost:8080/api/v1/cards" \
        -H "Content-Type: application/json" \
        -d '{"accountId":"'$ACCT'","cardHolderName":"NoAuth","dailyLimit":1000}')
fi
assert_http "T11 no JWT" "401" "$T11"

if [ "$GATEWAY_MODE" = "apicast" ]; then
    T12=$(run_test "T12: POST with invalid JWT (401)" \
        -X POST "${HOST}/api/v1/cards?user_key=$USERKEY" \
        -H "Authorization: Bearer dead.invalid.token" \
        -H "Content-Type: application/json" \
        -d '{"accountId":"'$ACCT'","cardHolderName":"BadToken","dailyLimit":1000}')
else
    T12=$(run_test "T12: POST with invalid JWT (401)" \
        -X POST "http://localhost:8080/api/v1/cards" \
        -H "Authorization: Bearer dead.invalid.token" \
        -H "Content-Type: application/json" \
        -d '{"accountId":"'$ACCT'","cardHolderName":"BadToken","dailyLimit":1000}')
fi
assert_http "T12 invalid JWT" "401" "$T12"

# ============================================
echo
echo "========== PHASE 4: Lifecycle Cleanup =========="

if [ "$GATEWAY_MODE" = "apicast" ]; then
    T13=$(run_test "T13: FREEZE after unfreeze (POST /api/v1/cards/{id}/freeze)" \
        -X POST "${HOST}/api/v1/cards/${CARD_ID}/freeze?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT")
else
    T13=$(run_test "T13: FREEZE after unfreeze (POST /api/v1/cards/{id}/freeze)" \
        -X POST "http://localhost:8080/api/v1/cards/${CARD_ID}/freeze" \
        -H "Authorization: Bearer $JWT")
fi
assert_http "T13 FREEZE again" "200" "$T13"

sleep 1
if [ "$GATEWAY_MODE" = "apicast" ]; then
    T14=$(run_test "T14: Card still exists — status FROZEN" \
        "${HOST}/api/v1/cards/${CARD_ID}?user_key=$USERKEY" \
        -H "Authorization: Bearer $JWT")
else
    T14=$(run_test "T14: Card still exists — status FROZEN" \
        "http://localhost:8080/api/v1/cards/${CARD_ID}" \
        -H "Authorization: Bearer $JWT")
fi
assert_http "T14 verify exists" "200" "$T14"
assert_json "T14 final status" "data.status" "FROZEN"

# ============================================
echo
if [ "$FAILED" -eq 0 ]; then
    echo "=== ALL 14 TESTS PASSED ==="
else
    echo "=== SOME TESTS FAILED ==="
    exit 1
fi
