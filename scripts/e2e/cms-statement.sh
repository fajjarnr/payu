#!/bin/bash
# PayU CMS + Statement E2E
set -e
TMPFILE=/tmp/r.json; FAILED=0
POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
CMS="http://cms-service.payu-dev.svc.cluster.local:8080"
STMT="http://statement-service.payu-dev.svc.cluster.local:8080"
CS=$(tr -d '[:space:]' < /tmp/client-secret.txt)
JWT=$(curl -skS -X POST "https://sso-payu-dev.apps.payu.ocp.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
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

echo "=== CMS ==="
T1=$(run_test "T1: Public" "$CMS/api/v1/public/contents"); ok "T1" "200" "$T1"
T2=$(run_test "T2: By type" "$CMS/api/v1/public/contents/type/ARTICLE"); ok "T2" "200" "$T2"
T3=$(run_test "T3: No JWT → 401" "$CMS/api/v1/contents"); ok "T3" "401" "$T3"

echo "=== Statements ==="
T4=$(run_test "T4: List" "$STMT/api/v1/statements" -H "Authorization: Bearer $JWT"); ok "T4" "200" "$T4"
T5=$(run_test "T5: Latest" "$STMT/api/v1/statements/latest" -H "Authorization: Bearer $JWT"); ok "T5" "200" "$T5"
T6=$(run_test "T6: Receipt 404" "$STMT/api/v1/statements/receipts/transaction/00000000-0000-0000-0000-000000000000" -H "Authorization: Bearer $JWT"); ok "T6" "404" "$T6"
T7=$(run_test "T7: No JWT → 401" "$STMT/api/v1/statements" -H "Authorization: Bearer dead"); ok "T7" "401" "$T7"

[ "$FAILED" -eq 0 ] && echo "ALL 7 PASSED" || { echo "FAILED"; exit 1; }
