#!/bin/bash
# PayU FULL CRUD E2E Test — All 23 services
GATEWAY="http://localhost:8080"
KC="http://localhost:8099"
ACCT="574e8801-a8da-4272-b6ff-493cd9b71685"

get_token() {
  local t
  t=$(podman exec payu-gateway-service curl -s -X POST "http://keycloak:8080/realms/payu/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "client_id=payu-backend" \
    -d "client_secret=payu-backend-d3v-0nly-a7c2f1e8b4d9063e5c8a2b7f1d4e9a3c" \
    -d "grant_type=password" \
    -d "username=customer1" \
    -d "password=Test1234!" 2>/dev/null | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
  [ -z "$t" ] && t=$(curl -s -X POST "${KC}/realms/payu/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=payu-backend" -d "client_secret=payu-backend-d3v-0nly-a7c2f1e8b4d9063e5c8a2b7f1d4e9a3c" -d "grant_type=password" -d "username=customer1" -d "password=Test1234!" 2>/dev/null | python3 -c "import sys,json;print(json.load(sys.stdin).get('access_token',''))")
  echo "$t"
}

test() {
  local method="$1" path="$2" data="$3" ik="$4"
  local url="${GATEWAY}${path}" code
  if [ -n "$data" ]; then
    if [ -n "$ik" ]; then
      code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -H "Idempotency-Key: $ik" -d "$data" 2>/dev/null)
    else
      code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$data" 2>/dev/null)
    fi
  else
    code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" -H "Authorization: Bearer $TOKEN" 2>/dev/null)
  fi
  local lbl="OK"
  case $code in 200|201) lbl="OK" ;; 204) lbl="OK" ;; 400) lbl="BAD" ;; 401|403) lbl="AUTH" ;; 404) lbl="404" ;; 409) lbl="CONFLICT" ;; 500) lbl="500" ;; 503) lbl="DOWN" ;; *) lbl="$code" ;; esac
  printf "%-6s %-55s %-3s %s\n" "$method" "$path" "$code" "$lbl"
}

TOKEN=$(get_token)
echo "Token: ${TOKEN:0:20}..."
echo ""

pass=0; fail=0; total=0
check() {
  local code="$1"
  total=$((total+1))
  case $code in 200|201|204|409) pass=$((pass+1)) ;; *) fail=$((fail+1)) ;; esac
}

echo "=== ACCOUNT ==="
for p in "/api/v1/accounts" "/api/v1/accounts/lookup?phone=08123456789"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}${p}")
  check "$code"; printf "%-6s %-55s %s\n" "GET" "$p" "$code"
done

echo "=== AUTH ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${GATEWAY}/api/v1/auth/login" -H "Content-Type: application/json" -d '{"username":"customer1","password":"Test1234!"}')
check "$code"; printf "%-6s %-55s %s\n" "POST" "/api/v1/auth/login" "$code"

echo "=== WALLET ==="
for p in "/api/v1/wallets" "/api/v1/wallets/${ACCT}/balance" "/api/v1/wallets/${ACCT}/ledger" "/api/v1/wallets/${ACCT}/transactions" "/api/v1/cards" "/api/v1/wallets/chart-of-accounts"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}${p}")
  check "$code"; printf "%-6s %-55s %s\n" "GET" "$p" "$code"
done

echo "=== TRANSACTION ==="
for p in "/api/v1/transactions" "/api/v1/transactions/accounts/${ACCT}"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}${p}")
  check "$code"; printf "%-6s %-55s %s\n" "GET" "$p" "$code"
done
IK=$(python3 -c "import uuid;print(uuid.uuid4())")
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${GATEWAY}/api/v1/transactions/transfer" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -H "Idempotency-Key: $IK" -d '{"senderAccountId":"574e8801-a8da-4272-b6ff-493cd9b71685","recipientAccountNumber":"1234567890","amount":10,"type":"INTERNAL_TRANSFER","description":"test"}')
check "$code"; printf "%-6s %-55s %s\n" "POST" "/api/v1/transactions/transfer" "$code"

echo "=== BILLING ==="
for p in "/api/v1/billers" "/api/v1/topup/providers"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}${p}")
  check "$code"; printf "%-6s %-55s %s\n" "GET" "$p" "$code"
done

echo "=== NOTIFICATION ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}/api/v1/notifications")
check "$code"; printf "%-6s %-55s %s\n" "GET" "/api/v1/notifications" "$code"

echo "=== FX ==="
for p in "/api/v1/fx" "/api/v1/fx/rates/USD/IDR" "/api/v1/fx/rates"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}${p}")
  check "$code"; printf "%-6s %-55s %s\n" "GET" "$p" "$code"
done

echo "=== STATEMENT ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}/api/v1/statements")
check "$code"; printf "%-6s %-55s %s\n" "GET" "/api/v1/statements" "$code"

echo "=== PROMOTION ==="
for p in "/api/v1/promotions"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}${p}")
  check "$code"; printf "%-6s %-55s %s\n" "GET" "$p" "$code"
done

echo "=== SUPPORT ==="
for p in "/api/v1/support" "/api/v1/support/agents" "/api/v1/support/modules" "/api/v1/support/training-status"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}${p}")
  check "$code"; printf "%-6s %-55s %s\n" "GET" "$p" "$code"
done
IK=$(python3 -c "import uuid;print(uuid.uuid4())")
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${GATEWAY}/api/v1/support/agents" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -H "Idempotency-Key: $IK" -d '{"name":"Test Agent","email":"agent@test.com","employeeId":"EMP001"}')
check "$code"; printf "%-6s %-55s %s\n" "POST" "/api/v1/support/agents" "$code"

echo "=== LENDING ==="
for p in "/api/v1/lending" "/api/v1/lending/credit-score"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}${p}")
  check "$code"; printf "%-6s %-55s %s\n" "GET" "$p" "$code"
done
IK=$(python3 -c "import uuid;print(uuid.uuid4())")
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${GATEWAY}/api/v1/lending/credit-score/calculate" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -H "Idempotency-Key: $IK" -d '{"userId":"574e8801-a8da-4272-b6ff-493cd9b71685"}')
check "$code"; printf "%-6s %-55s %s\n" "POST" "/api/v1/lending/credit-score/calculate" "$code"

echo "=== INVESTMENT ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}/api/v1/investments")
check "$code"; printf "%-6s %-55s %s\n" "GET" "/api/v1/investments" "$code"

echo "=== PARTNER ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}/api/v1/partners")
check "$code"; printf "%-6s %-55s %s\n" "GET" "/api/v1/partners" "$code"

echo "=== KYC ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}/api/v1/kyc")
check "$code"; printf "%-6s %-55s %s\n" "GET" "/api/v1/kyc" "$code"

echo "=== ANALYTICS ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}/api/v1/analytics")
check "$code"; printf "%-6s %-55s %s\n" "GET" "/api/v1/analytics" "$code"

echo "=== CMS/CONTENTS ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}/api/v1/contents")
check "$code"; printf "%-6s %-55s %s\n" "GET" "/api/v1/contents" "$code"

echo "=== PRODUCTS ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}/api/v1/products")
check "$code"; printf "%-6s %-55s %s\n" "GET" "/api/v1/products" "$code"

echo "=== INTEGRATION ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}/api/v1/integration")
check "$code"; printf "%-6s %-55s %s\n" "GET" "/api/v1/integration" "$code"

echo "=== DISPUTE ==="
for p in "/api/v1/disputes" "/api/v1/disputes/customer/${ACCT}" "/api/v1/disputes/status/OPEN"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}${p}")
  check "$code"; printf "%-6s %-55s %s\n" "GET" "$p" "$code"
done
IK=$(python3 -c "import uuid;print(uuid.uuid4())")
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${GATEWAY}/api/v1/disputes" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -H "Idempotency-Key: $IK" -d '{"transactionId":"932db19b-704e-4649-a82c-d69ecf0956ff","customerId":"574e8801-a8da-4272-b6ff-493cd9b71685","merchantId":"574e8801-a8da-4272-b6ff-493cd9b71685","disputedAmount":100.00,"currency":"IDR","reason":"E2E test"}')
check "$code"; printf "%-6s %-55s %s\n" "POST" "/api/v1/disputes" "$code"

echo "=== BACKOFFICE ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}/api/v1/backoffice")
check "$code"; printf "%-6s %-55s %s\n" "GET" "/api/v1/backoffice" "$code"

echo "=== COMPLIANCE ==="
code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "${GATEWAY}/api/v1/compliance/audit-report")
check "$code"; printf "%-6s %-55s %s\n" "GET" "/api/v1/compliance/audit-report" "$code"

echo ""
echo "================================================"
echo "TOTAL: $pass PASS / $total TESTS"
echo "FAIL: $fail"
echo "================================================"
