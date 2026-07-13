#!/bin/bash
# ============================================
# PayU Cards CRUD E2E Test
# 7-step CREATE/READ/UPDATE/DELETE end-to-end flow against
#   3scale APIcast -> gateway-service -> wallet-service -> Postgres
#
# Pre-reqs:
#   1. Keycloak payu realm has customer1 user with password
#      'customer1-test-pass' (set via admin API on first run)
#   2. The 'payu-product' 3scale product has an Application with
#      user_key=04dc03f2e2a776bffcb9b16eb9f93796
#   3. wallet-service has a 'wallets' row for accountId =
#      7a51ced3-5602-40fb-96e7-1703e9243ed5 (Keycloak customer1 user_id)
#      - See scripts/e2e/walletbootstrap.sql
# ============================================

set -e

USERKEY="${USERKEY:-04dc03f2e2a776bffcb9b16eb9f93796}"
HOST="${HOST:-https://payu-product-payu-apicast-production.apps.payu.ocp.fajjjar.my.id}"
ACCT="${ACCT:-7a51ced3-5602-40fb-96e7-1703e9243ed5}"

# Resolve JWT from the keycloak pod (one-shot)
GATEWAY_POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
JWT=$(oc exec -n payu-dev $GATEWAY_POD -- cat /tmp/cust1-jwt.txt 2>/dev/null)
[ -z "$JWT" ] && { echo "ERROR: no JWT at /tmp/cust1-jwt.txt in gateway-service pod"; exit 1; }

run_test() {
    local label="$1"; shift
    sleep 1
    local out=$(curl -skS -o /tmp/r.json -w "HTTP=%{http_code} TIME=%{time_total}s" "$@" 2>&1)
    local body=$(cat /tmp/r.json 2>/dev/null | head -c 400)
    printf "\n=== %s ===\n%s\nBODY: %s\n" "$label" "$out" "$body"
}

echo
run_test "T1: CREATE card (POST /api/v1/cards)" \
    -X POST "${HOST}/api/v1/cards?user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    -d "{\"accountId\":\"$ACCT\",\"cardHolderName\":\"E2E Test Card\",\"dailyLimit\":5000000}"

CARD_ID=$(cat /tmp/r.json | python3 -c "import json,sys;d=json.load(sys.stdin);print((d.get('data') or {}).get('id','NO_ID'))" 2>/dev/null)
echo "CARD_ID=$CARD_ID"
echo "$CARD_ID" > /tmp/card_id.txt

run_test "T2: READ cards list (GET /api/v1/cards?accountId=...)" \
    "${HOST}/api/v1/cards?accountId=${ACCT}&user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT"

run_test "T3: READ specific card (GET /api/v1/cards/{id})" \
    "${HOST}/api/v1/cards/${CARD_ID}?user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT"

run_test "T4: UPDATE freeze (POST /api/v1/cards/{id}/freeze)" \
    -X POST "${HOST}/api/v1/cards/${CARD_ID}/freeze?user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT"

sleep 2
run_test "T5: Verify freeze status" \
    "${HOST}/api/v1/cards/${CARD_ID}?user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT"
echo "STATUS after freeze: $(cat /tmp/r.json | python3 -c "import json,sys;d=json.load(sys.stdin);print(d.get('data',{}).get('status','N/A'))" 2>/dev/null)"

run_test "T6: UPDATE unfreeze (POST /api/v1/cards/{id}/unfreeze)" \
    -X POST "${HOST}/api/v1/cards/${CARD_ID}/unfreeze?user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT"

run_test "T7: Verify unfreeze status" \
    "${HOST}/api/v1/cards/${CARD_ID}?user_key=$USERKEY" \
    -H "Authorization: Bearer $JWT"
echo "STATUS after unfreeze: $(cat /tmp/r.json | python3 -c "import json,sys;d=json.load(sys.stdin);print(d.get('data',{}).get('status','N/A'))" 2>/dev/null)"

echo
echo "=== ALL DONE ==="
