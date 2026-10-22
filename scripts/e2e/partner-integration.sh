#!/bin/bash
# PayU Partner E2E — partners, SNAP BI, sandbox
set -e
# shellcheck source=env.sh
. "$(dirname "$0")/env.sh"
TMPFILE=/tmp/r.json; FAILED=0
POD=$(oc get pod -n "$PAYU_ENV" -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
P="http://partner-service.$PAYU_ENV.svc.cluster.local:8080"
CS=$(tr -d '[:space:]' < $E2E_SECRET_FILE)
get_jwt() {
  curl -skS -X POST "https://$SSO_HOST/realms/payu/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "client_id=payu-backend" --data-urlencode "client_secret=$CS" \
    --data-urlencode "grant_type=password" --data-urlencode "username=$1" \
    --data-urlencode "password=P@ssw0rd123" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null
}
JWT=$(get_jwt customer1)
[ ${#JWT} -lt 20 ] && { echo "FATAL: no JWT"; exit 1; }
AJWT=$(get_jwt admin)
[ ${#AJWT} -lt 20 ] && { echo "FATAL: no admin JWT"; exit 1; }

ok(){ [ "$3" = "$2" ] && echo "  ✅ $1 HTTP=$3" || { echo "  ❌ $1 exp=$2 got=$3"; FAILED=1; }; true; }
run_test() {
    local label="$1"; shift; sleep 0.3
    local code=$(oc exec -n "$PAYU_ENV" "$POD" -- curl -skS -o /tmp/r.json -w "%{http_code}" "$@" 2>/dev/null)
    oc exec -n "$PAYU_ENV" "$POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null
    printf "\n%s HTTP=%s\n" "$label" "$code" >&2; echo "$code"
}

echo "=== Partner Service ==="
# T1: GET /partners requires PARTNER_* role — customer1 gets 403 by design, admin gets 200
T1=$(run_test "T1: Partners list (admin)" "$P/partners" -H "Authorization: Bearer $AJWT"); ok "T1" "200" "$T1"
T1b=$(run_test "T1b: Partners list (customer1 → 403)" "$P/partners" -H "Authorization: Bearer $JWT"); ok "T1b" "403" "$T1b"
T2=$(run_test "T2: No JWT → 401" "$P/partners"); ok "T2" "401" "$T2"
T3=$(run_test "T3: Sandbox status" "$P/admin/sandbox/status"); [ "$T3" = "200" ] && echo "  ✅ T3 HTTP=200" || [ "$T3" = "401" ] && echo "  ✅ T3 HTTP=401 (protected)" || { echo "  ❌ T3=$T3"; FAILED=1; }
# T4: SNAP token without X-CLIENT-KEY → 400 MISSING_REQUIRED_HEADER (E2E-FULL-05 precedent)
T4=$(run_test "T4: SNAP auth (no headers → 400)" -X POST "$P/v1/partner/auth/token" -H "Content-Type: application/json" -d '{}'); ok "T4" "400" "$T4"
T5=$(run_test "T5: Partner ID" "$P/partners/1" -H "Authorization: Bearer $JWT"); [ "$T5" = "400" ] && echo "  ✅ T5 HTTP=400" || [ "$T5" = "404" ] && echo "  ✅ T5 HTTP=404" || { echo "  ❌ T5=$T5"; FAILED=1; }

[ "$FAILED" -eq 0 ] && echo "ALL 6 PASSED" || { echo "FAILED"; exit 1; }
