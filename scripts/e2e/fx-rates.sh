#!/bin/bash
# ============================================
# PayU FX Rates & Conversion E2E Test
# Full read/estimate/convert/reverse flow
#
# Gateway routing: gateway-service -> fx-service
# Mode: GATEWAY_MODE=internal (default, via oc exec)
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
    code=$(oc exec -n payu-dev "$GATEWAY_POD" -- \
        curl -skS -o /tmp/r.json -w "%{http_code}" "$@" 2>/dev/null)
    oc exec -n payu-dev "$GATEWAY_POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null
    local body
    body=$(head -c 400 "$TMPFILE" 2>/dev/null)
    printf "\n=== %s ===\nHTTP=%s\nBODY: %s\n" "$label" "$code" "$body" >&2
    echo "$code"
}

# ============================================
echo
echo "========== FX Status & Rate Queries =========="

T1=$(run_test "T1: Service status (GET /v1)" \
    "http://localhost:8080/api/v1/fx" \
    -H "Authorization: Bearer $JWT")
assert_http "T1 status" "200" "$T1"

T2=$(run_test "T2: Get all FX rates (GET /v1/rates)" \
    "http://localhost:8080/api/v1/fx/rates" \
    -H "Authorization: Bearer $JWT")
assert_http "T2 all rates" "200" "$T2"

T3=$(run_test "T3: Get USD→IDR rate (GET /v1/rates/USD/IDR)" \
    "http://localhost:8080/api/v1/fx/rates/USD/IDR" \
    -H "Authorization: Bearer $JWT")
assert_http "T3 USD/IDR" "200" "$T3"

T4=$(run_test "T4: Get EUR→IDR rate (GET /v1/rates/EUR/IDR)" \
    "http://localhost:8080/api/v1/fx/rates/EUR/IDR" \
    -H "Authorization: Bearer $JWT")
assert_http "T4 EUR/IDR" "200" "$T4"

echo
echo "========== Conversion Estimate (No Money Moved) =========="

T5=$(run_test "T5: Estimate USD→IDR (POST /v1/conversions/estimate)" \
    -X POST "http://localhost:8080/api/v1/fx/conversions/estimate" \
    -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    -H "X-Idempotency-Key: fx-e2e-est-1" \
    -d '{"fromCurrency":"USD","toCurrency":"IDR","amount":100}')
assert_http "T5 estimate" "200" "$T5"

T6=$(run_test "T6: Estimate EUR→IDR (POST /v1/conversions/estimate)" \
    -X POST "http://localhost:8080/api/v1/fx/conversions/estimate" \
    -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    -H "X-Idempotency-Key: fx-e2e-est-2" \
    -d '{"fromCurrency":"EUR","toCurrency":"IDR","amount":50}')
assert_http "T6 estimate EUR" "200" "$T6"

echo
echo "========== Real Conversion =========="

T7=$(run_test "T7: Convert USD 10→IDR (POST /v1/conversions)" \
    -X POST "http://localhost:8080/api/v1/fx/conversions" \
    -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    -H "X-Idempotency-Key: $(uuidgen)" \
    -d '{"fromCurrency":"USD","toCurrency":"IDR","amount":10}')
assert_http "T7 convert" "201" "$T7"

CONV_ID=$(python3 -c "
import json
d = json.load(open('$TMPFILE'))
print((d.get('data') or {}).get('id', 'NO_ID'))
" 2>/dev/null)
echo "CONV_ID=$CONV_ID" >&2
echo "$CONV_ID" > /tmp/conv_id.txt
[ "$CONV_ID" = "NO_ID" ] || [ -z "$CONV_ID" ] && { echo "FATAL: failed to extract conversion ID" >&2; exit 1; }

sleep 1
T8=$(run_test "T8: Read conversion (GET /v1/conversions/{id})" \
    "http://localhost:8080/api/v1/fx/conversions/${CONV_ID}" \
    -H "Authorization: Bearer $JWT")
assert_http "T8 read conversion" "200" "$T8"

T9=$(run_test "T9: List conversions (GET /v1/conversions)" \
    "http://localhost:8080/api/v1/fx/conversions" \
    -H "Authorization: Bearer $JWT")
assert_http "T9 list conversions" "200" "$T9"

T10=$(run_test "T10: Reverse conversion (POST /v1/conversions/{id}/reverse)" \
    -X POST "http://localhost:8080/api/v1/fx/conversions/${CONV_ID}/reverse" \
    -H "Authorization: Bearer $JWT" \
    -H "X-Idempotency-Key: $(uuidgen)")
assert_http "T10 reverse" "200" "$T10"

echo
echo "========== Error Flows =========="

T11=$(run_test "T11: Nonexistent rate pair (GET /v1/rates/XXX/YYY)" \
    "http://localhost:8080/api/v1/fx/rates/XXX/YYY" \
    -H "Authorization: Bearer $JWT")
assert_http "T11 bad pair" "404" "$T11"

T12=$(run_test "T12: No JWT (POST /v1/conversions/estimate)" \
    -X POST "http://localhost:8080/api/v1/fx/conversions/estimate" \
    -H "Content-Type: application/json" \
    -d '{"fromCurrency":"USD","toCurrency":"IDR","amount":100}')
assert_http "T12 no JWT" "401" "$T12"

T13=$(run_test "T13: Missing idempotency key (POST /v1/conversions)" \
    -X POST "http://localhost:8080/api/v1/fx/conversions" \
    -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    -d '{"fromCurrency":"USD","toCurrency":"IDR","amount":1}')
assert_http "T13 no idempotency" "400" "$T13"

echo
if [ "$FAILED" -eq 0 ]; then
    echo "=== ALL 13 TESTS PASSED ==="
else
    echo "=== SOME TESTS FAILED ==="
    exit 1
fi
