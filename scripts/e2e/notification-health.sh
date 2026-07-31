#!/bin/bash
# PayU Notification Health E2E
set -e
TMPFILE=/tmp/r.json; FAILED=0
POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
NOTIFY="http://notification-service.payu-dev.svc.cluster.local:8080"
CS=$(tr -d '[:space:]' < /tmp/client-secret.txt)
JWT=$(curl -skS -X POST "https://sso-dev.apps.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "client_id=payu-backend" --data-urlencode "client_secret=$CS" \
  --data-urlencode "grant_type=password" --data-urlencode "username=customer1" \
  --data-urlencode "password=Customer1-test" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))")
[ ${#JWT} -lt 20 ] && { echo "FATAL"; exit 1; }
ok(){ [ "$3" = "$2" ] && echo "  ✅ $1 HTTP=$3" || { echo "  ❌ $1 exp=$2 got=$3"; FAILED=1; }; true; }
t(){ local l="$1"; shift; sleep 0.3; local c=$(oc exec -n payu-dev "$POD" -- curl -skS -o /tmp/r.json -w "%{http_code}" "$@" 2>/dev/null); oc exec -n payu-dev "$POD" -- cat /tmp/r.json > "$TMPFILE" 2>/dev/null; printf "\n%s HTTP=%s\n" "$l" "$c" >&2; printf "%s" "$c"; }

echo "=== Notification ==="
T1=$(t "T1: Health" "$NOTIFY/q/health"); ok "T1" "200" "$T1"
T2=$(t "T2: API" "$NOTIFY/api/v1/notifications" -H "Authorization: Bearer $JWT"); ok "T2" "200" "$T2"
T3=$(t "T3: No JWT" "$NOTIFY/api/v1/notifications"); ok "T3" "401" "$T3"

[ "$FAILED" -eq 0 ] && echo "ALL 3 PASSED" || { echo "FAILED"; exit 1; }
