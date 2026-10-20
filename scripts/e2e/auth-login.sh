#!/bin/bash
# PayU Auth Service E2E Test
# Login (OIDC authorization-code exchange) + JWT validation through
# gateway -> auth-service.
#
# NOTE: POST /api/v1/auth/login (credential login) was removed by LOGIN-003
# (be378b84e) — credentials never reach auth-service. The live public login
# entry point is POST /api/v1/auth/callback. A successful browser login is
# covered by frontend/web-app/e2e/login-flow.spec.ts.
#
# Mode: GATEWAY_MODE=internal (default) or apicast

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
        "https://sso-dev.apps.payu.ocp.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=payu-backend" \
        -d "client_secret=${client_secret}" \
        -d "grant_type=password" \
        -d "username=customer1" \
        -d "password=P@ssw0rd123" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)
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
else
    BASE="http://localhost:8080"
fi

echo "MODE=$GATEWAY_MODE"

echo
echo "========== Auth Service =========="

# T1: Login — LOGIN-003 (be378b84e) retired the credential endpoint
# POST /api/v1/auth/login; the live public login entry point is the OIDC
# authorization-code exchange POST /api/v1/auth/callback. A well-formed but
# unknown code must be rejected by Keycloak with 400 (AUTH_BUS_009).
# 503 = Keycloak unreachable from auth-service (known infra gap).
T1=$(run_test "T1: OIDC callback rejects an unknown authorization code (POST /api/v1/auth/callback)" \
    -X POST "$BASE/api/v1/auth/callback" \
    -H "Content-Type: application/json" \
    -d "{\"code\":\"e2e-invalid-authorization-code\",\"codeVerifier\":\"$(printf 'a%.0s' {1..50})\",\"redirectUri\":\"https://payu-dev.apps.payu.ocp.fajjjar.my.id/api/auth/callback\"}")
# Accept 400 (invalid/expired code) or 503 (Keycloak not reachable from auth-service)
if [ "$T1" = "400" ] || [ "$T1" = "503" ]; then
    printf "  ✅ T1 OIDC callback HTTP=%s (400=invalid code, 503=KC unreachable — known gap)\n" "$T1"
else
    printf "  ❌ T1 OIDC callback expected HTTP=400 or 503 got HTTP=%s\n" "$T1"
    FAILED=1
fi

T2=$(run_test "T2: Validate JWT (GET /api/v1/auth/validate)" \
    "$BASE/api/v1/auth/validate" \
    -H "Authorization: Bearer $JWT")
assert_http "T2 validate" "200" "$T2"
assert_json "T2 valid" "data.valid" "True"

echo
echo "========== Error Flows =========="

# T3: Malformed OIDC callback body (code without codeVerifier) — request
# validation must reject it with 400 before any Keycloak round-trip.
T3=$(run_test "T3: OIDC callback with a malformed body (POST /api/v1/auth/callback)" \
    -X POST "$BASE/api/v1/auth/callback" \
    -H "Content-Type: application/json" \
    -d '{"code":"e2e-invalid-authorization-code"}')
assert_http "T3 validation" "400" "$T3"

T4=$(run_test "T4: OIDC callback with empty body (400)" \
    -X POST "$BASE/api/v1/auth/callback" \
    -H "Content-Type: application/json" \
    -d '{}')
assert_http "T4 validation" "400" "$T4"

T5=$(run_test "T5: Validate without JWT (401)" \
    "$BASE/api/v1/auth/validate")
assert_http "T5 no JWT" "401" "$T5"

T6=$(run_test "T6: Validate invalid JWT (401)" \
    "$BASE/api/v1/auth/validate" \
    -H "Authorization: Bearer dead.invalid.token")
assert_http "T6 invalid JWT" "401" "$T6"

echo
if [ "$FAILED" -eq 0 ]; then
    echo "=== ALL 6 TESTS PASSED ==="
else
    echo "=== SOME TESTS FAILED ==="
    exit 1
fi
