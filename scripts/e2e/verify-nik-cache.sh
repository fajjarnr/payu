#!/bin/bash
# NEW-001 E2E: account-service NIK verification cache round-trip
#
# Verifies that 2 consecutive POST /api/v1/accounts/verify-nik calls
# for the same NIK both return 200, proving the cache deser fix
# (TypedJsonRedisSerializer) preserves the VerifyNikResponse type
# on cache hit (was: ClassCastException: LinkedHashMap cannot be
# cast to VerifyNikResponse).
#
# Pre-reqs: customer1 JWT available in gateway-service pod

set -e

GATEWAY_POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
ACCOUNT_POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=account-service -o jsonpath='{.items[0].metadata.name}')
TMPFILE=/tmp/r.json.host
# Generate fresh JWT via Keycloak (ran from host, not pod)
refresh_jwt() {
    local client_secret
    client_secret=$(cat /tmp/client-secret.txt 2>/dev/null || echo "")
    if [ -z "$client_secret" ]; then
        echo "ERROR: /tmp/client-secret.txt not found" >&2
        exit 1
    fi
    local result
    result=$(curl -skS -X POST \
        "https://sso-dev.apps.payu.ocp.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=payu-backend" \
        -d "client_secret=${client_secret}" \
        -d "grant_type=password" \
        -d "username=customer1" \
        -d "password=P@ssw0rd123" 2>/dev/null)
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

# Use the INTERNAL account-service URL (port 8080, in-cluster DNS)
ACCT_URL="http://account-service.payu-dev.svc.cluster.local:8080"
USERKEY="${USERKEY:-04dc03f2e2a776bffcb9b16eb9f93796}"
ACCT_ID="750e8400-e29b-41d4-a716-446655440001"
NIK="3201234567890001"

FAILED=0
ok(){ [ "$3" = "$2" ] && echo "  ✅ $1 HTTP=$3" || { echo "  ❌ $1 exp=$2 got=$3"; FAILED=1; }; true; }
run_test() {
    local label="$1"; shift
    sleep 1
    local code
    code=$(oc exec -n payu-dev "$ACCOUNT_POD" -- \
        curl -s -o /tmp/r.json -w "%{http_code}" \
        -X POST "$ACCT_URL/api/v1/accounts/verify-nik" \
        -H "Authorization: Bearer $JWT" \
        -H "Content-Type: application/json" \
        -d "{\"nik\":\"$NIK\",\"fullName\":\"E2E Test\",\"birthPlace\":\"Jakarta\",\"birthDate\":\"1990-01-15\"}" 2>&1)
    oc exec -n payu-dev "$ACCOUNT_POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null
    local body
    body=$(head -c 400 "$TMPFILE" 2>/dev/null)
    printf "\n=== %s ===\nHTTP=%s\nBODY: %s\n" "$label" "$code" "$body" >&2
    printf "%s" "$code"
}

echo "=== NEW-001 E2E: account-service:1.8.13 NIK verification cache round-trip ==="
echo "ACCT_URL=$ACCT_URL"
echo "ACCT_ID=$ACCT_ID  NIK=$NIK"

# Requires realm client-scope account:verify on payu-backend (else 403).
# Dukcapil simulator has a 3% random-failure mode, so a 503-body on either
# leg is retried once with a fresh NIK (different cache key, same shape).
attempt=0
while true; do
    T1=$(run_test "T1: 1st verify-nik (cache miss, calls Dukcapil)")
    T2=$(run_test "T2: 2nd verify-nik SAME NIK (cache hit, no ClassCastException)")
    if [ "$T1" = "200" ] && [ "$T2" = "200" ]; then
        ok "T1 status" "200" "$T1"; ok "T2 status" "200" "$T2"
        break
    fi
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 2 ]; then
        ok "T1 status" "200" "$T1"; ok "T2 status" "200" "$T2"
        break
    fi
    echo "  ⚠️ simulator random failure — retrying once with fresh NIK" >&2
    NIK="3201234567890002"
done

[ "$FAILED" -eq 0 ] && echo "ALL 2 PASSED" || { echo "FAILED"; exit 1; }
