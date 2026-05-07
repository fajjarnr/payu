#!/bin/bash
GATEWAY="http://localhost:8080"
KC="http://localhost:8099"
ACCT="574e8801-a8da-4272-b6ff-493cd9b71685"

get_token() {
  local t
  # Try internal first (correct issuer: keycloak:8080)
  t=$(podman exec payu-gateway-service curl -s -X POST "http://keycloak:8080/realms/payu/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "client_id=payu-backend" \
    -d "client_secret=payu-backend-d3v-0nly-a7c2f1e8b4d9063e5c8a2b7f1d4e9a3c" \
    -d "grant_type=password" \
    -d "username=customer1" \
    -d "password=Test1234!" 2>/dev/null | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
  if [ -z "$t" ]; then
    # Fallback to external
    t=$(curl -s -X POST "${KC}/realms/payu/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "client_id=payu-backend" \
      -d "client_secret=payu-backend-d3v-0nly-a7c2f1e8b4d9063e5c8a2b7f1d4e9a3c" \
      -d "grant_type=password" \
      -d "username=customer1" \
      -d "password=Test1234!" 2>/dev/null | python3 -c "import sys,json;print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null)
  fi
  echo "$t"
}

test_endpoint() {
  local method="$1" path="$2" data="$3" ik="$4"
  local url="${GATEWAY}${path}"
  local code
  
  if [ -n "$data" ]; then
    if [ -n "$ik" ]; then
      code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -H "Idempotency-Key: $ik" \
        -d "$data" 2>/dev/null)
    else
      code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "$data" 2>/dev/null)
    fi
  else
    code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" \
      -H "Authorization: Bearer $TOKEN" 2>/dev/null)
  fi
  local lbl="OK"
  case $code in
    200|201) lbl="OK" ;;
    400) lbl="BAD" ;;
    401|403) lbl="AUTH" ;;
    404) lbl="404" ;;
    500) lbl="500" ;;
    *) lbl="$code" ;;
  esac
  printf "%-6s %-45s %-3s %s\n" "$method" "$path" "$code" "$lbl"
}

TOKEN=$(get_token)
echo "Token: ${TOKEN:0:20}..."
echo ""

echo "=== AUTH ==="
RESP=$(curl -s -X POST "${GATEWAY}/api/v1/auth/login" -H "Content-Type: application/json" -d '{"username":"customer1","password":"Test1234!"}' 2>/dev/null)
SUCCESS=$(echo "$RESP" | python3 -c "import sys,json;print(json.load(sys.stdin).get('success','false'))" 2>/dev/null)
echo "POST /api/v1/auth/login -> ${SUCCESS}"

echo ""
echo "=== READ ==="
for path in \
  "/api/v1/accounts" \
  "/api/v1/wallets/${ACCT}/balance" \
  "/api/v1/transactions" \
  "/api/v1/billers" \
  "/api/v1/notifications" \
  "/api/v1/fx" \
  "/api/v1/statements" \
  "/api/v1/promotions" \
  "/api/v1/support" \
  "/api/v1/lending" \
  "/api/v1/investments" \
  "/api/v1/partners" \
  "/api/v1/kyc" \
  "/api/v1/contents" \
  "/api/v1/products" \
  "/api/v1/integration" \
  "/api/v1/disputes" \
  "/api/v1/backoffice"
do
  test_endpoint GET "$path" "" ""
done

echo ""
echo "=== CREATE ==="
IK=$(python3 -c "import uuid;print(uuid.uuid4())")
test_endpoint POST "/api/v1/transactions/transfer" '{"senderAccountId":"574e8801-a8da-4272-b6ff-493cd9b71685","recipientAccountNumber":"1234567890","amount":10,"type":"INTERNAL_TRANSFER","description":"test"}' "$IK"

test_endpoint POST "/api/v1/disputes" '{"transactionId":"932db19b-704e-4649-a82c-d69ecf0956ff","customerId":"574e8801-a8da-4272-b6ff-493cd9b71685","merchantId":"574e8801-a8da-4272-b6ff-493cd9b71685","disputedAmount":100.00,"currency":"IDR","reason":"test"}' "$(python3 -c "import uuid;print(uuid.uuid4())")"

echo ""
echo "=== MORE CREATE ==="
test_endpoint POST "/api/v1/support/agents" '{"name":"Test Agent","email":"test@test.com","employeeId":"EMP001"}' "$(python3 -c "import uuid;print(uuid.uuid4())")"
test_endpoint POST "/api/v1/lending/loans" '{"userId":"574e8801-a8da-4272-b6ff-493cd9b71685","amount":5000000,"tenureMonths":12,"interestRate":14.0}' "$(python3 -c "import uuid;print(uuid.uuid4())")"
test_endpoint POST "/api/v1/investments/deposits" '{"userId":"574e8801-a8da-4272-b6ff-493cd9b71685","amount":1000000,"tenureMonths":6}' "$(python3 -c "import uuid;print(uuid.uuid4())")"
test_endpoint POST "/api/v1/partners" '{"name":"Test Partner","email":"partner@test.com","type":"MERCHANT"}' "$(python3 -c "import uuid;print(uuid.uuid4())")"
test_endpoint POST "/api/v1/fx/conversions/estimate" '{"fromCurrency":"USD","toCurrency":"IDR","amount":100}' "" ""
