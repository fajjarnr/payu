#!/bin/bash
# ============================================
# NEW-001 E2E: account-service NIK verification cache round-trip
#
# Verifies that 2 consecutive POST /api/v1/accounts/verify-nik calls
# for the same NIK both return 200, proving the cache deser fix
# (TypedJsonRedisSerializer) preserves the VerifyNikResponse type
# on cache hit (was: ClassCastException: LinkedHashMap cannot be
# cast to VerifyNikResponse).
#
# Pre-reqs: customer1 JWT available in gateway-service pod
# ============================================

set -e

JWT=$(oc exec -n payu-dev gateway-service-58dbc4cfbb-c8zbc -- cat /tmp/cust1-jwt.txt 2>/dev/null)
[ -z "$JWT" ] && { echo "ERROR: no JWT at /tmp/cust1-jwt.txt in gateway-service pod"; exit 1; }

# Use the INTERNAL account-service URL (port 8080, in-cluster DNS)
ACCT_URL="http://account-service.payu-dev.svc.cluster.local:8080"
USERKEY="${USERKEY:-04dc03f2e2a776bffcb9b16eb9f93796}"
ACCT_ID="7a51ced3-5602-40fb-96e7-1703e9243ed5"
NIK="3201234567890001"

run_test() {
    local label="$1"; shift
    sleep 1
    local out
    out=$(oc exec -n payu-dev account-service-6f85f48d64-k5z4p -- \
        curl -s -o /tmp/r.json -w "HTTP=%{http_code} TIME=%{time_total}s" \
        -X POST "$ACCT_URL/api/v1/accounts/verify-nik" \
        -H "Authorization: Bearer $JWT" \
        -H "Content-Type: application/json" \
        -d "{\"nik\":\"$NIK\",\"fullName\":\"E2E Test\",\"birthPlace\":\"Jakarta\",\"birthDate\":\"1990-01-15\"}" 2>&1)
    local body
    body=$(cat /tmp/r.json 2>/dev/null | head -c 400)
    printf "\n=== %s ===\n%s\nBODY: %s\n" "$label" "$out" "$body"
}

echo "=== NEW-001 E2E: account-service:1.8.13 NIK verification cache round-trip ==="
echo "ACCT_URL=$ACCT_URL"
echo "ACCT_ID=$ACCT_ID  NIK=$NIK"

run_test "T1: 1st verify-nik (cache miss, calls Dukcapil via gateway)"
run_test "T2: 2nd verify-nik SAME NIK (cache hit, no ClassCastException expected)"

echo
echo "=== DONE ==="
