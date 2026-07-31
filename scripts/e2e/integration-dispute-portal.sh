#!/bin/bash
# PayU Integration + Dispute Read-only E2E
set -e
TMPFILE=/tmp/r.json; FAILED=0
POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
INT="http://integration-service.payu-dev.svc.cluster.local:8080"
DSP="http://dispute-service.payu-dev.svc.cluster.local:8080"
CS=$(tr -d '[:space:]' < /tmp/client-secret.txt)
JWT=$(curl -skS -X POST "https://sso-dev.apps.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "client_id=payu-backend" --data-urlencode "client_secret=$CS" \
  --data-urlencode "grant_type=password" --data-urlencode "username=customer1" \
  --data-urlencode "password=Customer1-test" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))")
[ ${#JWT} -lt 20 ] && { echo "FATAL"; exit 1; }
ok(){ [ "$3" = "$2" ] && echo "  ✅ $1 HTTP=$3" || { echo "  ❌ $1 exp=$2 got=$3"; FAILED=1; }; true; }
t(){ local l="$1"; shift; sleep 0.3; local c=$(oc exec -n payu-dev "$POD" -- curl -skS -o /tmp/r.json -w "%{http_code}" "$@" 2>/dev/null); oc exec -n payu-dev "$POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null; printf "\n%s HTTP=%s\n" "$l" "$c" >&2; printf "%s" "$c"; }

echo "=== Integration ==="
t "T1: Status" "$INT/api/v1/integration/status" -H "Authorization: Bearer $JWT"; ok "T1" "200" "$T1"
t "T2: Messages" "$INT/api/v1/integration/messages" -H "Authorization: Bearer $JWT"; ok "T2" "200" "$T2"
t "T3: No JWT" "$INT/api/v1/integration/status"; ok "T3" "401" "$T3"

echo "=== Dispute ==="
t "T4: List" "$DSP/api/v1/disputes" -H "Authorization: Bearer $JWT"; ok "T4" "200" "$T4"
t "T5: By status" "$DSP/api/v1/disputes/status/OPEN" -H "Authorization: Bearer $JWT"; ok "T5" "200" "$T5"
t "T6: No JWT" "$DSP/api/v1/disputes"; ok "T6" "401" "$T6"

[ "$FAILED" -eq 0 ] && echo "ALL 6 PASSED" || { echo "FAILED"; exit 1; }
