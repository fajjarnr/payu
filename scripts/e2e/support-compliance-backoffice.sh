#!/bin/bash
# PayU Support + Compliance + Backoffice E2E
set -e
# shellcheck source=env.sh
. "$(dirname "$0")/env.sh"
TMPFILE=/tmp/r.json; FAILED=0
POD=$(oc get pod -n "$PAYU_ENV" -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
S="http://support-service.$PAYU_ENV.svc.cluster.local:8080"
C="http://compliance-service.$PAYU_ENV.svc.cluster.local:8080"
B="http://backoffice-service.$PAYU_ENV.svc.cluster.local:8080"
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

echo "=== Support ==="
T1=$(run_test "T1: Status" "$S/api/v1/support" -H "Authorization: Bearer $JWT"); ok "T1" "200" "$T1"
T2=$(run_test "T2: Training" "$S/api/v1/support/training-status" -H "Authorization: Bearer $JWT"); ok "T2" "200" "$T2"
T3=$(run_test "T3: No JWT → 401" "$S/api/v1/support"); ok "T3" "401" "$T3"

echo "=== Compliance ==="
# T4: audit-report needs a search param + elevated role; no-param → 400 by design
T4=$(run_test "T4: Audit reports (admin+param)" "$C/api/v1/compliance/audit-report?transactionId=00000000-0000-0000-0000-000000000000" -H "Authorization: Bearer $AJWT"); ok "T4" "200" "$T4"
T4b=$(run_test "T4b: Audit reports (customer1 → 403)" "$C/api/v1/compliance/audit-report?transactionId=00000000-0000-0000-0000-000000000000" -H "Authorization: Bearer $JWT"); ok "T4b" "403" "$T4b"
T4c=$(run_test "T4c: Audit reports (no param → 400)" "$C/api/v1/compliance/audit-report" -H "Authorization: Bearer $AJWT"); ok "T4c" "400" "$T4c"
T5=$(run_test "T5: GDPR ops (403 — ADMIN only)" "$C/api/v1/gdpr-audit/operations/READ" -H "Authorization: Bearer $JWT"); ok "T5" "403" "$T5"
T6=$(run_test "T6: No JWT → 401" "$C/api/v1/compliance/audit-report"); ok "T6" "401" "$T6"

echo "=== Backoffice ==="
T7=$(run_test "T7: Status" "$B/api/v1/backoffice" -H "Authorization: Bearer $JWT"); ok "T7" "200" "$T7"
T8=$(run_test "T8: No JWT → 401" "$B/api/v1/backoffice"); ok "T8" "401" "$T8"

[ "$FAILED" -eq 0 ] && echo "ALL 10 PASSED" || { echo "FAILED"; exit 1; }
