#!/bin/bash
# PayU Full Service Health Check — hits actual API not actuator
# Uses JWT for authenticated endpoints. Skips startup-sensitive /actuator.
set -e
# shellcheck source=env.sh
. "$(dirname "$0")/env.sh"
FAILED=0; POD=$(oc get pod -n "$PAYU_ENV" -l app.kubernetes.io/name=gateway-service -o jsonpath='{.items[0].metadata.name}')
CS=$(tr -d '[:space:]' < $E2E_SECRET_FILE)
JWT=$(curl -skS -X POST "https://$SSO_HOST/realms/payu/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "client_id=payu-backend" --data-urlencode "client_secret=$CS" \
  --data-urlencode "grant_type=password" --data-urlencode "username=customer1" \
  --data-urlencode "password=P@ssw0rd123" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)

check(){ local svc="$1"; shift; local c=$(oc exec -n "$PAYU_ENV" "$POD" -- timeout 5 curl -skS -o /dev/null -w "%{http_code}" "$@" 2>/dev/null || echo "TO"); [ "$c" = "200" ] && echo "  ✅ $svc" || echo "  ⚠️ $svc $c"; }

echo "=== Functional Health Check ==="
check "gateway"        "http://localhost:8080/q/health"
check "wallet"         "http://wallet-service.$PAYU_ENV:8080/api/v1/wallets" -H "Authorization: Bearer $JWT"
check "partner"        "http://partner-service.$PAYU_ENV:8080/partners" -H "Authorization: Bearer $JWT"
check "notification"   "http://notification-service.$PAYU_ENV:8080/q/health"
check "transaction"    "http://transaction-service.$PAYU_ENV:8080/api/v1/transactions?accountId=00000000-0000-0000-0000-000000000000" -H "Authorization: Bearer $JWT"
check "account"        "http://account-service.$PAYU_ENV:8080/api/v1/accounts" -H "Authorization: Bearer $JWT"
check "fx"             "http://fx-service.$PAYU_ENV:8080/v1" -H "Authorization: Bearer $JWT"
check "billing"        "http://billing-service.$PAYU_ENV:8080/api/v1/billers" -H "Authorization: Bearer $JWT"
check "promotion"      "http://promotion-service.$PAYU_ENV:8080/api/v1/promotions" -H "Authorization: Bearer $JWT"
check "lending"        "http://lending-service.$PAYU_ENV:8080/api/v1/lending" -H "Authorization: Bearer $JWT"
check "cms"            "http://cms-service.$PAYU_ENV:8080/api/v1/public/contents"
check "auth"           "http://auth-service.$PAYU_ENV:8080/api/v1/auth/validate" -H "Authorization: Bearer $JWT"
check "support"        "http://support-service.$PAYU_ENV:8080/api/v1/support" -H "Authorization: Bearer $JWT"
check "compliance"     "http://compliance-service.$PAYU_ENV:8080/api/v1/compliance/audit-report" -H "Authorization: Bearer $JWT"
check "backoffice"     "http://backoffice-service.$PAYU_ENV:8080/api/v1/backoffice" -H "Authorization: Bearer $JWT"
check "dispute"        "http://dispute-service.$PAYU_ENV:8080/api/v1/disputes" -H "Authorization: Bearer $JWT"
check "integration"    "http://integration-service.$PAYU_ENV:8080/api/v1/integration/status" -H "Authorization: Bearer $JWT"
check "investment"     "http://investment-service.$PAYU_ENV:8080/api/v1/investments" -H "Authorization: Bearer $JWT"

echo "DONE"
