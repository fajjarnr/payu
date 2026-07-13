#!/bin/bash
# PayU Full Service Health Check — hits actual API not actuator
# Uses JWT for authenticated endpoints. Skips startup-sensitive /actuator.
set -e
FAILED=0; POD=$(oc get pod -n payu-dev -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
CS=$(tr -d '[:space:]' < /tmp/client-secret.txt)
JWT=$(curl -skS -X POST "https://sso-payu-dev.apps.payu.ocp.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "client_id=payu-backend" --data-urlencode "client_secret=$CS" \
  --data-urlencode "grant_type=password" --data-urlencode "username=customer1" \
  --data-urlencode "password=Customer1-test" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)

check(){ local svc="$1"; shift; local c=$(oc exec -n payu-dev "$POD" -- timeout 5 curl -skS -o /dev/null -w "%{http_code}" "$@" 2>/dev/null || echo "TO"); [ "$c" = "200" ] && echo "  ✅ $svc" || echo "  ⚠️ $svc $c"; }

echo "=== Functional Health Check ==="
check "gateway"        "http://localhost:8080/q/health"
check "wallet"         "http://wallet-service.payu-dev:8080/api/v1/wallets" -H "Authorization: Bearer $JWT"
check "partner"        "http://partner-service.payu-dev:8080/partners" -H "Authorization: Bearer $JWT"
check "notification"   "http://notification-service.payu-dev:8080/q/health"
check "transaction"    "http://transaction-service.payu-dev:8080/api/v1/transactions?accountId=00000000-0000-0000-0000-000000000000" -H "Authorization: Bearer $JWT"
check "account"        "http://account-service.payu-dev:8080/api/v1/accounts" -H "Authorization: Bearer $JWT"
check "fx"             "http://fx-service.payu-dev:8080/v1" -H "Authorization: Bearer $JWT"
check "billing"        "http://billing-service.payu-dev:8080/api/v1/billers" -H "Authorization: Bearer $JWT"
check "promotion"      "http://promotion-service.payu-dev:8080/api/v1/promotions" -H "Authorization: Bearer $JWT"
check "lending"        "http://lending-service.payu-dev:8080/api/v1/lending" -H "Authorization: Bearer $JWT"
check "cms"            "http://cms-service.payu-dev:8080/api/v1/public/contents"
check "auth"           "http://auth-service.payu-dev:8080/api/v1/auth/validate" -H "Authorization: Bearer $JWT"
check "support"        "http://support-service.payu-dev:8080/api/v1/support" -H "Authorization: Bearer $JWT"
check "compliance"     "http://compliance-service.payu-dev:8080/api/v1/compliance/audit-report" -H "Authorization: Bearer $JWT"
check "backoffice"     "http://backoffice-service.payu-dev:8080/api/v1/backoffice" -H "Authorization: Bearer $JWT"
check "dispute"        "http://dispute-service.payu-dev:8080/api/v1/disputes" -H "Authorization: Bearer $JWT"
check "integration"    "http://integration-service.payu-dev:8080/api/v1/integration/status" -H "Authorization: Bearer $JWT"
check "investment"     "http://investment-service.payu-dev:8080/api/v1/investments" -H "Authorization: Bearer $JWT"

echo "DONE"
