# PayU E2E Test Suite

End-to-end test scripts untuk PayU Digital Banking Platform. Semua script berjalan melalui 3scale APIcast (Tier 1) atau langsung ke gateway-service (internal mode).

## Prerequisites

```bash
# 1. OpenShift CLI logged in
oc whoami

# 2. Cluster ready (optional: verify)
oc get pods -n payu-dev | grep gateway-service

# 3. Keycloak client secret — generate once per session:
#    (lihat section "JWT Bootstrap" di bawah)

# 4. Test customer1 user exists in Keycloak payu realm
#    password: Customer1-test
```

## Quick Start

```bash
# Pastikan client secret tersedia (satu kali per sesi):
# Jika belum, lihat section JWT Bootstrap.

# Run semua test melalui APIcast
GATEWAY_MODE=apicast bash scripts/e2e/cards-crud.sh

# Atau direct ke gateway (internal cluster)
bash scripts/e2e/cards-crud.sh

# Run semua script berurutan
for f in scripts/e2e/*.sh; do
  echo "=== Running $f ==="
  bash "$f" || echo "FAILED: $f"
done
```

## JWT Bootstrap (per session)

3scale APIcast dan gateway-service memvalidasi JWT dari Keycloak. Script E2E auto-refresh JWT menggunakan client secret `payu-backend`.

### Step 1: Dapatkan Keycloak admin token

```bash
ADMIN_TOKEN=$(curl -skS -X POST \
  "https://sso-payu-dev.apps.payu.ocp.fajjjar.my.id/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=payu-keycloak-admin-dev-only" | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))")

echo "$ADMIN_TOKEN" > /tmp/admin-master-jwt.txt
```

### Step 2: Dapatkan client secret payu-backend

```bash
CLIENT_SECRET=$(curl -skS \
  "https://sso-payu-dev.apps.payu.ocp.fajjjar.my.id/admin/realms/payu/clients/42e4097d-fad7-4a98-9562-6880fbc49da7/client-secret" \
  -H "Authorization: Bearer $(cat /tmp/admin-master-jwt.txt)" | python3 -c "import json,sys; print(json.load(sys.stdin)['value'])")

echo "$CLIENT_SECRET" > /tmp/client-secret.txt
```

### Step 3: Verifikasi

```bash
curl -skS -X POST \
  "https://sso-payu-dev.apps.payu.ocp.fajjjar.my.id/realms/payu/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=payu-backend" \
  -d "client_secret=$(cat /tmp/client-secret.txt)" \
  -d "grant_type=password" \
  -d "username=customer1" \
  -d "password=Customer1-test" | python3 -c "import json,sys; print('OK' if 'access_token' in json.load(sys.stdin) else 'FAIL')"
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `GATEWAY_MODE` | `internal` | `internal` (via oc exec) atau `apicast` (external URL) |
| `USERKEY` | `9a3f2bf...` | 3scale user_key (Application key di payu-product) |
| `HOST` | `https://payu-product-payu-apicast-production...` | APIcast production endpoint |
| `ACCT` | `15ded37b-...` | Account ID (Keycloak `sub` customer1) |
| `WALLET_ID` | `33333333-...` | Wallet ID (untuk wallet-balance.sh) |
| `RECIPIENT_ACCT` | (kosong) | Recipient account number untuk transfer test |

## Test Scripts

### cards-crud.sh (14 tests) — wallet-service

**Flow:** CREATE → READ list → READ by id → FREEZE → verify FROZEN → idempotent FREEZE → UNFREEZE → verify ACTIVE → idempotent UNFREEZE → 404 not found → 401 no JWT → 401 invalid JWT → lifecycle re-freeze → verify final state

```
bash scripts/e2e/cards-crud.sh
GATEWAY_MODE=apicast bash scripts/e2e/cards-crud.sh
```

### wallet-balance.sh (8 tests) — wallet-service

**Flow:** service status → balance → ledger entries → transaction history → pockets list → pocket total balance → 401 no JWT → 401 invalid JWT

```
bash scripts/e2e/wallet-balance.sh
```

### fx-rates.sh (13 tests) — fx-service

**Flow:** service status → all rates → USD/IDR rate → EUR/IDR rate → estimate USD→IDR → estimate EUR→IDR → convert USD 10→IDR → read conversion → list conversions → reverse → 404 bad pair → 401 no JWT → 400 missing idempotency

> ⚠️ Requires `QUARKUS_OIDC_TOKEN_ISSUER` external URL on fx-service. May need `oc apply -k` first.

```
bash scripts/e2e/fx-rates.sh
```

### transaction-history.sh (11 tests) — transaction-service

**Flow:** list transactions → paginated list → status filter → date range → transfer (opt-in via `RECIPIENT_ACCT`) → get by ID → 404 not found → 401 no JWT → 401 invalid JWT → 400 validation → 400 missing idempotency

```
# Read-only (no recipient needed)
bash scripts/e2e/transaction-history.sh

# With transfer
RECIPIENT_ACCT="12345678901234" bash scripts/e2e/transaction-history.sh
```

### billing-billers.sh (6 tests) — billing-service

**Flow:** biller list → biller categories → topup providers → 404 unknown biller → 401 no JWT → 401 invalid JWT

```
bash scripts/e2e/billing-billers.sh
```

### account-beneficiaries.sh (7 tests) — account-service

**Flow:** CREATE beneficiary → LIST → UPDATE nickname → DELETE → 401 no JWT → 401 invalid JWT → 400 validation

```
bash scripts/e2e/account-beneficiaries.sh
```

### account-service.sh (5 tests) — account-service

Nik verification cache round-trip (2 calls same NIK to verify cache deserialization).

```
bash scripts/e2e/verify-nik-cache.sh
```

## Architecture

```
                    ┌─────────────────────┐
GATEWAY_MODE=apicast │  curl (localhost)   │
   ──────────────►   │        │            │
                     │  3scale APIcast     │
                     │  (Tier 1 Gateway)   │
                     │        │            │
                     │  user_key validation│
                     │  rate limiting      │
                     │        │            │
                     └────────┼────────────┘
                              │
                    ┌─────────▼────────────┐
GATEWAY_MODE=internal│ oc exec             │
   ──────────────►   │ curl localhost:8080 │
                     │        │            │
                     │  gateway-service    │
                     │  (Quarkus)          │
                     │  JWT validation     │
                     │  route dispatch     │
                     │        │            │
                     └────────┼────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
    ┌─────────▼─┐  ┌─────────▼─┐  ┌─────────▼─┐
    │  wallet    │  │    fx     │  │transaction │
    │  account   │  │  billing  │  │            │
    └───────────┘  └───────────┘  └───────────┘
```

## Helper Functions

Semua script menggunakan shared helpers:

- `refresh_jwt()` — generates fresh Keycloak JWT via client_secret (requires `/tmp/client-secret.txt`)
- `assert_http(label, expected_code, actual_code)` — strict HTTP assertion
- `assert_json(label, field, expected)` — JSON field value assertion
- `assert_json_exists(label, field)` — verify JSON field exists
- `assert_json_not_null(label, field)` — verify JSON field has non-null value
- `run_test(label, curl_args...)` — execute curl + capture response

Script berhenti pada assertion failure pertama (`set -e`) dengan exit code >0.

## Troubleshooting

### 3scale APIcast returns 403 (0ms)

User key tidak valid. Verifikasi di access log APIcast:
```bash
oc logs -n payu-api-management deployment/apicast-production --tail=20 | grep user_key
```

### Gateway returns INVALID_TOKEN

1. JWT expired — script auto-refresh (jika `/tmp/client-secret.txt` ada)
2. OIDC issuer mismatch — pastikan kustomize overlay menggunakan external URL

### Wallet returns CARD_403

`accountId` di request != `sub` di JWT. Gunakan Keycloak user UUID:
```bash
# Dari JWT payload:
python3 -c "import base64,json,sys; t=sys.stdin.read().strip(); print(json.loads(base64.urlsafe_b64decode(t.split('.')[1]+'=='))['sub'])" < /tmp/cust1-jwt.txt
```

### FX/routes return 404 from gateway

Gateway route mapping belum include prefix. Cek `application.yaml` di gateway-service.

## Files

```
scripts/e2e/
├── README.md                  ← this file
├── cards-crud.sh              wallet-service   (14 tests)
├── wallet-balance.sh          wallet-service   (8 tests)
├── fx-rates.sh                fx-service       (13 tests)
├── transaction-history.sh     transaction-svc  (11 tests)
├── billing-billers.sh         billing-service  (6 tests)
├── account-beneficiaries.sh   account-service  (7 tests)
├── verify-nik-cache.sh        account-service  (2 tests)
└── wallet-bootstrap.sql       DB fixture
```
