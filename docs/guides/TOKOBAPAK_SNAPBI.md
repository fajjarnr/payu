# TokoBapak SNAP-BI Integration Guide

> **PAYU-TB-001 + PAYU-TB-005** — SNAP-BI `POST /v1.0/access-token/b2b` → `POST /v1.0/transfer-va/payment` via PayU `partner-service` (`SnapBiController:92,171` + `SnapBiSignatureService:22,42,67`).
> Last verified: `2026-08-30` against `payu-dev` `partner-service:8080` + `wallet-service` escrow `ACC_TOKOBAPAK_ESCROW`.

## 1. Credentials (payu-dev / local podman)

| Field | Value | Source |
|---|---|---|
| `clientId` (`X-CLIENT-KEY`) | `tokobapak-mvp` | `partners.client_id` `V23__seed_tokobapak_partner.sql` + `payu-realm-export.json` `tokobapak-mvp` |
| `clientSecret` | `tokobapak-mvp-dev-secret-32chars-long!` | same seed — plaintext fallback for `EncryptedStringConverter` (`V18` dual-read `ENC(...)`), next JPA write re-encrypts |
| `partnerCode` | `TOKOBAPAK_MVP` | `partners.partner_code` UNIQUE |
| `tenant_id` | `tokobapak` | RLS `partners FORCE` policy `SYSTEM OR tenant_id = current_setting` |
| `Keycloak client` | `tokobapak-mvp` | `infrastructure/platform/identity/keycloak/payu-realm-export.json` + `keycloak-realm-import.yaml` `serviceAccountsEnabled:true` |

Local podman `payu-database-rw` + `payu-keycloak:8099` both carry the same seed via Flyway `V23` + realm import.

## 2. SNAP-BI Signature (HMAC-SHA512, `SnapBiSignatureService.java:17`)

> **Algorithm**: `HmacSHA512` (not `HmacSHA256` — `SNAP-HMAC-001` mandates SHA-512 for BI compatibility; `4012504` if SHA-256 is used). Hash of body is `hex(SHA256(rawBody))` via `hashRequestBody()` (`MessageDigest SHA-256` → `bytesToHex`).

### 2.1 Access-token B2B (`POST /v1.0/access-token/b2b`) — `generateSignatureWithClientKey`

```
stringToSign = HTTPMethod + ":" + endpoint + ":" + X-TIMESTAMP + ":" + hex(SHA256(rawBody))
signature    = Base64( HMAC-SHA512( clientSecret , stringToSign ) )
```

- `HTTPMethod` = `POST`
- `endpoint` = request URI actually hit (both `/v1.0/access-token/b2b` and legacy `/v1/partner/auth/token` are accepted — `signedEndpoint()` returns `requestUri` minus `contextPath`, matching what the caller signed `SNAP-PATH-001`)
- `X-TIMESTAMP` = `yyyy-MM-dd'T'HH:mm:ss'Z'` UTC (`SnapBiSignatureService.getCurrentTimestamp()` `ZonedDateTime.now(UTC)`)
- `rawBody` = exact request body bytes as sent (do NOT re-serialize JSON — field order/whitespace changes break the hash, `BUG-BE-139`)
- `X-CLIENT-KEY` = `tokobapak-mvp`, `X-SIGNATURE` = `signature` above

### 2.2 Transfer VA Payment (`POST /v1.0/transfer-va/payment`) — `generateSignature`

```
stringToSign = HTTPMethod + ":" + endpoint + ":" + accessToken + ":" + hex(SHA256(rawBody)) + ":" + X-TIMESTAMP
signature    = Base64( HMAC-SHA512( clientSecret , stringToSign ) )
```

- `accessToken` = `Bearer` token returned from B2B call (`SnapBiTokenService.generateAccessToken` `partner.jwt.secret`, 900s TTL `TokenResponse: Bearer 900`)
- Headers required: `Authorization: Bearer <token>`, `X-TIMESTAMP`, `X-SIGNATURE`, `X-EXTERNAL-ID` (unique per request, forwarded as `X-Idempotency-Key` for `GatewayConfig`)
- Body must include `partnerReferenceNo`, `amount{value,currency:IDR}`, `sourceAccountNo`, `beneficiaryAccountNo` — missing → `4002501` (`SnapBiPaymentService:85`)

### 2.3 Timestamp window (`SnapBiController:67 isTimestampValid`)

```java
OffsetDateTime requestTime = OffsetDateTime.parse(timestamp);
Duration diff = Duration.between(requestTime, now).abs();
return diff.toSeconds() <= 300; // ±300s (5 min)
```

Rejects stale/future timestamps with `4002508 Invalid or expired timestamp` — prevents replay.

## 3. Go Example (`payu_client.go:26` compatible)

Fixes `TokoBapak` mock-only `payu_client.go:43` (`payload+timestamp` SHA-256 mismatch + missing `sourceAccountNo/beneficiaryAccountNo`).

```go
package payu

import (
    "crypto/hmac"
    "crypto/sha256"
    "crypto/sha512"
    "encoding/base64"
    "encoding/hex"
    "net/http"
)

func hashBody(rawBody string) string {
    if rawBody == "" {
        return ""
    }
    h := sha256.Sum256([]byte(rawBody))
    return hex.EncodeToString(h[:])
}

// SignForB2B — for POST /v1.0/access-token/b2b
func SignForB2B(clientSecret, method, endpoint, timestamp, rawBody string) string {
    hashed := hashBody(rawBody)
    stringToSign := method + ":" + endpoint + ":" + timestamp + ":" + hashed
    mac := hmac.New(sha512.New, []byte(clientSecret))
    mac.Write([]byte(stringToSign))
    return base64.StdEncoding.EncodeToString(mac.Sum(nil))
}

// Sign — for POST /v1.0/transfer-va/payment (with accessToken)
func Sign(clientSecret, method, endpoint, accessToken, rawBody, timestamp string) string {
    hashed := hashBody(rawBody)
    stringToSign := method + ":" + endpoint + ":" + accessToken + ":" + hashed + ":" + timestamp
    mac := hmac.New(sha512.New, []byte(clientSecret))
    mac.Write([]byte(stringToSign))
    return base64.StdEncoding.EncodeToString(mac.Sum(nil))
}

// Usage:
// ts := time.Now().UTC().Format("2006-01-02T15:04:05Z") // must match isTimestampValid ±300s
// sig := SignForB2B("tokobapak-mvp-dev-secret-32chars-long!", "POST", "/v1.0/access-token/b2b", ts, `{"grantType":"client_credentials"}`)
```

> **Migration from old `payu_client.go:26 Sign(payload+timestamp)`**: old `hmac.SHA256(payload+timestamp) hex` → new `hmac.SHA512(method:endpoint:timestamp:hex(sha256(body))) base64`. Keep `Sign(payload,timestamp)` only for legacy fallback; new code MUST call `SignForB2B`/`Sign` above.

## 4. Error Code Mapping (`SnapBiController` + `SnapBiPaymentService:85`)

| Code | HTTP | Meaning | Trigger |
|---|---|---|---|
| `4002501` | 400 | Invalid Request Body / missing field | `partnerReferenceNo` / `sourceAccountNo` / `beneficiaryAccountNo` blank, or `objectMapper.readValue` fails (`BUG-BE-139` raw body) |
| `4002502` | 400 | Only IDR supported | `request.amount.currency != "IDR"` |
| `4002508` | 400 | Invalid or expired timestamp | `isTimestampValid` `abs(diff) > 300s` or unparsable `OffsetDateTime` |
| `4012502` | 401 | Invalid Client Key | `partnerService.findByClientId(clientKey) == null` |
| `4012503` | 401 | Partner inactive | `partner.isActive() == false` (`status != ACTIVE`) |
| `4012504` | 401 | Invalid Signature | `validateSignature*` `MessageDigest.isEqual` mismatch |
| `4012506` | 401 | Invalid or Expired Token | `tokenService.getClientIdFromToken(token) == null` (`partner.jwt.secret` mismatch or >900s) |
| `4012507` | 401 | Partner not found or inactive | token `clientId` → `findByClientId` null/inactive on payment |
| `2002500` | 200 | Successful (idempotent replay) | `findByPartnerIdAndPartnerReferenceNo` hit → returns existing `payuReferenceNo` (`uq_snap_payment_partner_ref` `V17`) |

## 5. Wallets (PayU ledger source of truth — `CONTEXT.md` Wallet/Ledger/Journal)

Seeded via `wallet-service` `V122__seed_tokobapak_wallets.sql` (`SELECT set_config('app.tenant_id','SYSTEM',false)` bypasses `V121 FORCE RLS`):

| `account_id` | `balance` | `currency` | `tenant_id` | Purpose |
|---|---|---|---|---|
| `ACC_TOKOBAPAK_ESCROW` | `100000000.0000` | IDR | `default` | Marketplace escrow — source for all `sourceAccountNo` |
| `ACC_SELLER_001` | `0.0000` | IDR | `default` | Merchant payout — `beneficiaryAccountNo` example |
| `ACC_SELLER_002` | `0.0000` | IDR | `default` | Merchant payout |
| `ACC_SELLER_003` | `0.0000` | IDR | `default` | Merchant payout |

Settlement via `WalletSettlementAdapter:48 settle()` → `POST /api/v1/wallets/transfer` `X-Idempotency-Key: snap-transfer-{payuReferenceNo}` atomic double-entry (`Journal.isBalanced()` `DEBIT==CREDIT` `DECIMAL(19,4) HALF_EVEN`, `ledger_entries` 2 rows `balance_after`, `journal_entries` 1 row). Verification after payment:

```bash
curl -s http://localhost:8080/api/v1/wallets/ACC_TOKOBAPAK_ESCROW/ledger?referenceId=PAYU-... | jq
# → 2 entries [{entryType:DEBIT,amount, balance_after}, {entryType:CREDIT,...}] isBalanced
```

## 6. End-to-End curl (local podman `payu-gateway:8080` → `partner-service:8080`, prod `payu-partner-service:8080`)

```bash
# 1) B2B token — sign with clientSecret + rawBody + timestamp
TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)
BODY='{"grantType":"client_credentials"}'
HASH=$(echo -n "$BODY" | openssl dgst -sha256 -hex | awk '{print $2}')
STRING="POST:/v1.0/access-token/b2b:$TS:$HASH"
SIG=$(echo -n "$STRING" | openssl dgst -sha512 -hmac "tokobapak-mvp-dev-secret-32chars-long!" -binary | base64)

curl -s http://localhost:8080/v1.0/access-token/b2b \
  -H "X-CLIENT-KEY: tokobapak-mvp" \
  -H "X-TIMESTAMP: $TS" \
  -H "X-SIGNATURE: $SIG" \
  -H "Content-Type: application/json" \
  -d "$BODY" | jq
# → {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":"900"}

TOKEN=$(curl -s ... | jq -r .accessToken)

# 2) Payment — sign with accessToken
PAY_BODY='{"partnerReferenceNo":"ORDER-001","amount":{"value":110000,"currency":"IDR"},"sourceAccountNo":"ACC_TOKOBAPAK_ESCROW","beneficiaryAccountNo":"ACC_SELLER_001"}'
HASH2=$(echo -n "$PAY_BODY" | openssl dgst -sha256 -hex | awk '{print $2}')
TS2=$(date -u +%Y-%m-%dT%H:%M:%SZ)
STRING2="POST:/v1.0/transfer-va/payment:$TOKEN:$HASH2:$TS2"
SIG2=$(echo -n "$STRING2" | openssl dgst -sha512 -hmac "tokobapak-mvp-dev-secret-32chars-long!" -binary | base64)

curl -s http://localhost:8080/v1.0/transfer-va/payment \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-TIMESTAMP: $TS2" \
  -H "X-SIGNATURE: $SIG2" \
  -H "X-EXTERNAL-ID: ORDER-001" \
  -H "Content-Type: application/json" \
  -d "$PAY_BODY" | jq
# → {"responseCode":"2002500","responseMessage":"Successful","partnerReferenceNo":"ORDER-001","payuReferenceNo":"PAYU-..."}
```

Local: `tokobapak/payment-service` uses `PAYU_BASE_URL=http://payu-partner-service:8080` (`podman-compose.yml:199` fixed `PAYU-TB-004`) + `gateway-service RouteRegistry` alias `/v1.0/transfer-va/payment → partner-service` so `podman exec tokobapak-payment-service wget -qO- http://payu-partner-service:8080/v1.0/access-token/b2b` returns `400/MISSING_HEADER` not `Connection refused`.

## 7. TokoBapak → PayU Flow (fixed vs before)

| Before (mock) | After (real) |
|---|---|
| `payu_client.go:43 return "payu-ref-"` without HTTP, `Sign(payload+timestamp) hex(SHA256)` mismatched `HmacSHA512 Base64` | `SignForB2B`/`Sign` above, `POST /v1.0/...` via `payu-partner-service:8080` + fallback only on network failure |
| `CreateTransaction` payload `partnerReferenceNo+amount` only (missing `source/beneficiaryAccountNo`) → `4002501` | Payload includes `sourceAccountNo=ACC_TOKOBAPAK_ESCROW`, `beneficiaryAccountNo=ACC_SELLER_*` (`PaymentRequest.java:9`) |
| `callback` without `X-SIGNATURE` | `handler.go:84` validates `X-SIGNATURE` via same `Sign` (optional, webhook HMAC) |
| `outbox_poller.go:19` never `Start()` in `order-service main.go` | `order-service/cmd/server/main.go` starts poller `SELECT FOR UPDATE SKIP LOCKED` 5s → `tokobapak.payment.completed.v1` → `notification-service` |

## 8. Reconciliation (`PAYU-TB-003/004` + `SnapBiReconciliationService`)

After payment, `GET /api/v1/wallets/ACC_SELLER_001/ledger?referenceId=PAYU-...` must show 2 balanced entries; `SnapBiReconciliationService` auto-resolves `PAYMENT`/`REFUND`/`WALLET_MOVEMENT` within 5m (`resolve()` + `caseRepository.save`).

---
> **Test vectors** (deterministic, `isTimestampValid` requires live `±300s` — regenerate `TS` at runtime):
> `rawBody={"grantType":"client_credentials"}` `TS=2026-08-30T10:00:00Z` `hash=...` `stringToSign=POST:/v1.0/access-token/b2b:2026-08-30T10:00:00Z:<hash>` → `HMAC-SHA512(tokobapak-mvp-dev-secret-32chars-long!, stringToSign)` → `Base64` as above.
