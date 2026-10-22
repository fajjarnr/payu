#!/bin/bash
# PayU Integration + Dispute Read-only E2E
set -e
TMPFILE=/tmp/r.json; FAILED=0
POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
INT="http://integration-service.payu-dev.svc.cluster.local:8080"
DSP="http://dispute-service.payu-dev.svc.cluster.local:8080"
CS=$(tr -d '[:space:]' < /tmp/client-secret.txt)
get_jwt() {
  curl -skS -X POST "https://sso-dev.apps.payu.ocp.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "client_id=payu-backend" --data-urlencode "client_secret=$CS" \
    --data-urlencode "grant_type=password" --data-urlencode "username=$1" \
    --data-urlencode "password=P@ssw0rd123" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null
}
JWT=$(get_jwt customer1)
[ ${#JWT} -lt 20 ] && { echo "FATAL"; exit 1; }
AJWT=$(get_jwt admin)
[ ${#AJWT} -lt 20 ] && { echo "FATAL: no admin JWT"; exit 1; }
ok(){ [ "$3" = "$2" ] && echo "  ✅ $1 HTTP=$3" || { echo "  ❌ $1 exp=$2 got=$3"; FAILED=1; }; true; }
t(){ local l="$1"; shift; sleep 0.3; local c=$(oc exec -n payu-dev "$POD" -- curl -skS -o /tmp/r.json -w "%{http_code}" "$@" 2>/dev/null); oc exec -n payu-dev "$POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null; printf "\n%s HTTP=%s\n" "$l" "$c" >&2; printf "%s" "$c"; }

echo "=== Integration ==="
T1=$(t "T1: Status" "$INT/api/v1/integration/status" -H "Authorization: Bearer $JWT"); ok "T1" "200" "$T1"
# T2: requires ?status param + ADMIN/INTEGRATION_OPERATOR/SYSTEM role
T2=$(t "T2: Messages (admin+status)" "$INT/api/v1/integration/messages?status=FAILED" -H "Authorization: Bearer $AJWT"); ok "T2" "200" "$T2"
T2b=$(t "T2b: Messages (no param → 400)" "$INT/api/v1/integration/messages" -H "Authorization: Bearer $AJWT"); ok "T2b" "400" "$T2b"
T3=$(t "T3: No JWT" "$INT/api/v1/integration/status"); ok "T3" "401" "$T3"

echo "=== Dispute ==="
# T4/T5: dispute list requires ADMIN/BACKOFFICE/DISPUTE_AGENT — customer1 gets 403 by design
T4=$(t "T4: List (admin)" "$DSP/api/v1/disputes" -H "Authorization: Bearer $AJWT"); ok "T4" "200" "$T4"
T4b=$(t "T4b: List (customer1 → 403)" "$DSP/api/v1/disputes" -H "Authorization: Bearer $JWT"); ok "T4b" "403" "$T4b"
T5=$(t "T5: By status (admin)" "$DSP/api/v1/disputes/status/OPEN" -H "Authorization: Bearer $AJWT"); ok "T5" "200" "$T5"
T6=$(t "T6: No JWT" "$DSP/api/v1/disputes"); ok "T6" "401" "$T6"

[ "$FAILED" -eq 0 ] && echo "ALL 8 PASSED" || { echo "FAILED"; exit 1; }
