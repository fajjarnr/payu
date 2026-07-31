#!/bin/bash
# PayU Support + Compliance + Backoffice E2E
set -e
TMPFILE=/tmp/r.json; FAILED=0
POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
S="http://support-service.payu-dev.svc.cluster.local:8080"
C="http://compliance-service.payu-dev.svc.cluster.local:8080"
B="http://backoffice-service.payu-dev.svc.cluster.local:8080"
CS=$(tr -d '[:space:]' < /tmp/client-secret.txt)
JWT=$(curl -skS -X POST "https://sso-dev.apps.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "client_id=payu-backend" --data-urlencode "client_secret=$CS" \
  --data-urlencode "grant_type=password" --data-urlencode "username=customer1" \
  --data-urlencode "password=Customer1-test" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)
[ ${#JWT} -lt 20 ] && { echo "FATAL: no JWT"; exit 1; }

ok(){ [ "$3" = "$2" ] && echo "  ✅ $1 HTTP=$3" || { echo "  ❌ $1 exp=$2 got=$3"; FAILED=1; }; true; }
run_test() {
    local label="$1"; shift; sleep 0.3
    local code=$(oc exec -n payu-dev "$POD" -- curl -skS -o /tmp/r.json -w "%{http_code}" "$@" 2>/dev/null)
    oc exec -n payu-dev "$POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null
    printf "\n%s HTTP=%s\n" "$label" "$code" >&2; echo "$code"
}

echo "=== Support ==="
T1=$(run_test "T1: Status" "$S/api/v1/support" -H "Authorization: Bearer $JWT"); ok "T1" "200" "$T1"
T2=$(run_test "T2: Training" "$S/api/v1/support/training-status" -H "Authorization: Bearer $JWT"); ok "T2" "200" "$T2"
T3=$(run_test "T3: No JWT → 401" "$S/api/v1/support"); ok "T3" "401" "$T3"

echo "=== Compliance ==="
T4=$(run_test "T4: Audit reports" "$C/api/v1/compliance/audit-report" -H "Authorization: Bearer $JWT"); ok "T4" "200" "$T4"
T5=$(run_test "T5: GDPR ops (403 — ADMIN only)" "$C/api/v1/gdpr-audit/operations/READ" -H "Authorization: Bearer $JWT"); ok "T5" "403" "$T5"
T6=$(run_test "T6: No JWT → 401" "$C/api/v1/compliance/audit-report"); ok "T6" "401" "$T6"

echo "=== Backoffice ==="
T7=$(run_test "T7: Status" "$B/api/v1/backoffice" -H "Authorization: Bearer $JWT"); ok "T7" "200" "$T7"
T8=$(run_test "T8: No JWT → 401" "$B/api/v1/backoffice"); ok "T8" "401" "$T8"

[ "$FAILED" -eq 0 ] && echo "ALL 8 PASSED" || { echo "FAILED"; exit 1; }
