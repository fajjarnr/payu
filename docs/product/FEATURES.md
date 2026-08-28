# PayU Feature Catalog

> Daftar fitur per service sesuai pengelompokan [`ARCHITECTURE.md`](../architecture/ARCHITECTURE.md):
> Core Banking (8) · Supporting (6) · Additional (9). List fitur + endpoint utama.
> Status & temuan audit: [`../roadmap/TODOS.md`](../roadmap/TODOS.md).
> Sumber: scan `*Controller.java`/`*Resource.java`/`api/v1/*.py` (2026-08-11).

## Core Banking Services

| Service | Port | Fitur utama | Status |
|:---|:---:|:---|:---:|
| account-service | 8001 | Onboarding, eKYC, beneficiary, budget, phone lookup | 🔴 |
| auth-service | 8002 | Login, refresh, lockout, risk/MFA | 🔴 |
| wallet-service | 8004 | Ledger, reserve/commit/release, escrow, pocket, card | 🟡 |
| transaction-service | 8003 | Transfer (internal/BI-FAST/SKN/RTGS), QRIS, VA, disbursement, split-bill | 🟡 |
| investment-service | 8009 | Deposito, reksadana, emas | 🟡 |
| lending-service | 8010 | Pinjaman, PayLater, repayment, installment | 🟡 |
| fx-service | 8096 | Kurs, konversi | 🟡 |
| statement-service | 8015 | E-statement PDF, receipt | 🟡 |

## account-service (8001)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| A1 | Registrasi user + eKYC | `POST /api/v1/accounts/register` |
| A2 | Onboarding (account + profile) | `POST /api/v1/accounts` |
| A3 | Beneficiary CRUD | `/api/v1/accounts/{accountId}/beneficiaries` (+ `/{beneficiaryId}`) |
| A4 | Budget tracker | `/api/v1/accounts/{accountId}/budgets` (+ `/{budgetId}`, `/check`, `/status`) |
| A5 | Phone lookup | `/api/v1/accounts/lookup` |
| A6 | NIK verification (Dukcapil) | `POST /api/v1/accounts/verify-nik` |
| A7 | Inter-service account queries | `/api/v1/accounts/users`, `/api/v1/accounts/{userId}/account-ids` |

## auth-service (8002)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| AU1 | Login (OIDC auth-code + PKCE, LOGIN-003) | `GET /api/auth/authorize` → `GET /api/auth/callback` (BFF) · `POST /api/v1/auth/callback` |
| AU2 | Refresh token | `POST /api/v1/auth/refresh` |
| AU3 | Register | `POST /api/v1/auth/register` |
| AU4 | Validate session | `GET /api/v1/auth/validate` |
| AU5 | Logout | `POST /api/v1/auth/logout` |
| AU6 | Device binding | `POST /api/v1/auth/device` · `POST /api/v1/auth/device/token` |
| AU7 | Biometric | `POST /api/v1/biometric/register` · `POST /api/v1/biometric/authenticate` · `GET /api/v1/biometric/challenge` · `GET /api/v1/biometric/registrations/{username}` |
| AU8 | Step-up auth (internal) | `POST /internal/v1/auth/step-up` · `POST /internal/v1/auth/step-up/challenge` · `POST /internal/v1/auth/step-up/verify` |

## wallet-service (8004)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| W1 | Balance | `GET /api/v1/wallets/{accountId}/balance` |
| W2 | Reserve / commit / release | `POST /api/v1/wallets/{accountId}/reserve`, `/reservations/{reservationId}/commit`, `/reservations/{reservationId}/release` |
| W3 | Credit / transfer / reverse | `POST /api/v1/wallets/{accountId}/credit`, `/transfer/reverse` |
| W4 | Ledger history | `GET /api/v1/wallets/{accountId}/ledger`, `/ledger/transaction/{transactionId}` |
| W5 | Transaction history | `GET /api/v1/wallets/{accountId}/transactions` |
| W6 | Virtual debit card | `/api/v1/cards` (+ `/{cardId}/freeze`, `/{cardId}/unfreeze`) |
| W7 | Pocket | `/api/v1/wallets/pockets` (+ `/{pocketId}/close`, `/freeze`, `/unfreeze`, `/credit`, `/debit`, `/total-balance/{currency}`) |
| W8 | Savings goal | `/api/v1/wallets/{walletId}/savings-goals` (+ `/{goalId}/pause`, `/{goalId}/resume`) |
| W9 | Escrow | `/api/v1/escrow` (+ `/{escrowId}/refund`, `/{escrowId}/release`, `/{escrowId}/settle`, `/buyer/{buyerAccountId}`, `/seller/{sellerAccountId}`, `/partner/{partnerId}`) |
| W10 | Settlement | `/api/v1/settlements` (+ `/batches/{batchId}/process`, `/complete`, `/fail`, `/override`, `/report`, `/discrepancies/detect`) |
| W11 | Revenue split | `/api/v1/settlements/revenue-splits` (+ `/{splitId}/calculate`, `/deactivate`, `/stakeholders`, `/royalty-statement`) |
| W12 | Journal (double-entry) | `/api/v1/wallets/journals` (+ `/{journalId}`, `/trial-balance`) |
| W13 | General Ledger | `/api/v1/wallets/gl` (+ `/balance-sheet`, `/income-statement`, `/daily-settlement`) |
| W14 | Chart of Accounts | `/api/v1/wallets/chart-of-accounts` (+ `/children/{parentId}`, `/type/{type}`, `/{code}`) |
| W15 | Reconciliation | `/api/v1/reconciliation/ledger-movements` |
| W16 | Split Payment | `/api/v1/split-payments/rules` (+ `/rules/partner/{partnerId}`, `/rules/{ruleId}`, `/execute`, `/executions`, `/executions/{id}/reverse`, `/executions/payer/{payerAccountId}`) |

## transaction-service (8003)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| T1 | Transfer (internal/BI-FAST/SKN/RTGS) | `POST /api/v1/transactions/transfer` |
| T2 | Transaction history & detail | `GET /api/v1/transactions/accounts/{accountId}`, `/{transactionId}`, `/{transactionId}/tags` |
| T3 | QRIS payment | `POST /api/v1/transactions/qris/pay` |
| T4 | Virtual account | `/api/v1/payments/va` (+ `/{vaId}`, `/number/{vaNumber}`, `/callback`) |
| T5 | Disbursement | `/api/v1/disbursements` (+ `/{id}/process`, `/callback`, `/by-idempotency-key/{key}`) |
| T6 | Batch disbursement | `/api/v1/disbursements/batch` (+ `/{id}/process`, `/complete`, `/items`, `/progress`) |
| T7 | Split bill | `/api/v1/split-bills` (+ `/{id}/activate`, `/cancel`, `/settle`, `/participants/{pid}/accept`, `/decline`, `/payment`) |
| T8 | Scheduled transfer | `/api/v1/scheduled-transfers` (+ `/{id}/pause`, `/{id}/resume`, `/{id}/cancel`) |
| T9 | Smart routing | `/api/v1/transfers/routes` (+ `/fastest`, `/recommend`, `/all`) |
| T10 | Refund-details (internal) | `GET /api/v1/transactions/internal/{transactionId}/refund-details` |
| T11 | Interbank callback | `POST /api/v1/transactions/interbank/callback` |

## investment-service (8009)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| I1 | Beli deposito | `POST /api/v1/investments/deposits` |
| I2 | Beli reksadana | `POST /api/v1/investments/mutual-funds` |
| I3 | Beli emas | `POST /api/v1/investments/gold` |
| I4 | Jual (redeem) | `POST /api/v1/investments/sell` |
| I5 | Portfolio & account | `GET /api/v1/investments/accounts`, `/accounts/me`, `/gold/me` |

## lending-service (8010)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| L1 | Pengajuan pinjaman | `POST /api/v1/lending/loans` |
| L2 | Detail & schedule pinjaman | `GET /api/v1/lending/loans/{loanId}`, `/repayment-schedule` |
| L3 | Repayment | `POST /api/v1/lending/repayment-schedules/{scheduleId}/pay` |
| L4 | Credit score | `POST /api/v1/lending/credit-score/calculate`, `GET /credit-score/{userId}` |
| L5 | Pre-approval | `POST /api/v1/lending/pre-approval/check`, `GET /pre-approval/user/{userId}/active`, `/{preApprovalId}` |
| L6 | PayLater | `POST /api/v1/lending/paylater/activate`, `/paylater/{userId}/purchase`, `/payment`, `/transactions` |
| L7 | Installment checkout | `POST /api/v1/lending/installments/checkout`, `GET /tenor-options`, `/installments/{checkoutId}`, `/installments/user/{userId}` |

## fx-service (8096)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| F1 | Kurs | `GET /v1/rates` (direct `:8096`) · Via gateway `GET /api/v1/fx/rates` → `GET /v1/rates`, `/rates/{fromCurrency}/{toCurrency}` → `/v1/rates/{fromCurrency}/{toCurrency}` |
| F2 | Konversi | `POST /v1/conversions` (direct) · Via gateway `POST /api/v1/fx/conversions` → `POST /v1/conversions`, `GET /conversions/estimate` |
| F3 | Reverse konversi | `POST /v1/conversions/{conversionId}/reverse` |
## statement-service (8015)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| S1 | Generate e-statement | `POST /api/v1/statements/generate` |
| S2 | Statement terbaru | `GET /api/v1/statements/latest` |
| S3 | Statement detail & download | `GET /api/v1/statements/{id}`, `/{id}/download`, `/{id}/regenerate` |
| S4 | Receipt | `GET /api/v1/statements/receipts/transaction/{transactionId}`, `/receipts/{receiptId}`, `/{receiptId}/download`, `/receipts/generate` |

---

## Supporting Services

### billing-service (8005)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| B1 | Bill payment | `/api/v1/payments` (+ `/{id}`, `/reference/{referenceNumber}`) |
| B2 | Top-up | `/api/v1/topup` (+ `/{id}`, `/providers`, `/reference/{referenceNumber}`) |
| B3 | Biller catalog | `/api/v1/billers` (+ `/{code}`, `/categories`) |
| B4 | Subscription | `/api/v1/subscriptions` (+ `/{subscriptionId}/cancel`, `/charges`, `/account/{accountId}`, `/partner/{partnerId}`) |
| B5 | Subscription plan | `/api/v1/subscriptions/plans` (+ `/{planId}`, `/plans/partner/{partnerId}`) |

### kyc-service (8007, Python)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| K1 | Verify KTP (OCR) | `POST /api/v1/kyc/verify/ktp` |
| K2 | Verify selfie (face match) | `POST /api/v1/kyc/verify/selfie` |
| K3 | Start verification flow | `POST /api/v1/kyc/verify/start` |
| K4 | Verification status | `GET /api/v1/kyc/verify/{verificationId}` |
| K5 | User KYC data | `GET /api/v1/kyc/user/{user_id}` |
| K6 | Upload KTP image | `POST /uploads/ktp/{verification_id}.jpg` |

### notification-service (8006)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| N1 | Kirim notifikasi | `/api/v1/notifications` |
| N2 | Riwayat notifikasi user | `GET /api/v1/notifications/user/{userId}` |
| N3 | Detail & baca | `GET /api/v1/notifications/{id}`, `POST /{id}/read` |

### gateway-service (8080)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| G1 | API routing (catch-all) | `/api/v1`, `/{path: .*}` |
| G2 | Partner contract (SNAP-BI) | `/v1/partner/**` (+ `/auth/token`, `/payments`, `/refund`) |
| G3 | Rate plan & limit partner | `/api/v1/admin/rate-plans` (+ `/assignments`, `/partners/{partnerId}/limits`, `/partners/{partnerId}/rate-plan`, `/partners/{partnerId}/status`) |
| G4 | Checkout & token | `/api/v1/checkout` (+ `/tokens/{token}`, `/tokens/{token}/pay`, `/page/{token}`) |
| G5 | Simulator gateway | `/api/v1/simulator/bifast/*`, `/dukcapil/*`, `/qris/*` |
| G6 | gRPC bridge (internal) | `/api/internal/grpc/wallet/*` (balance, available-balance, reserve, commit, release, credit, debit, transfer) |
| G7 | Payment methods | `/api/v1/payments/methods` |
| G8 | Deeplink | `/api/v1/deeplinks` |
| G9 | Gateway analytics | `/gateway/analytics`, `/config`, `/top-endpoints`, `/metrics`, `/partners/{partnerId}/metrics` |
| G10 | OpenAPI aggregation | `/q/openapi`, `/q/swagger-ui`, `/v3/api-docs` |

### api-portal-service (8021)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| AP1 | Portal & service list | `/api/v1/portal/services`, `/services/{serviceId}/openapi` |
| AP2 | OpenAPI refresh | `/api/v1/portal/openapi`, `/refresh` |
| AP3 | Sandbox mock | `/api/v1/sandbox` (+ `/payments`, `/payments/{ref}/refund`, `/mock-data/examples`, `/stats`) |
| AP4 | Swagger UI | `/`, `/openapi`, `/service/{serviceId}` |

### analytics-service (8008, Python)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| AN1 | Fraud score | `/api/v1/analytics/fraud/score`, `/fraud/transaction/{transaction_id}`, `/fraud/user/{user_id}/high-risk` |
| AN2 | Spending trends & cashflow | `/api/v1/analytics/spending/trends`, `/cashflow` |
| AN3 | Rekomendasi & robo-advisory | `/api/v1/analytics/user/{user_id}/recommendations`, `/robo-advisory` |
| AN4 | User metrics | `/api/v1/analytics/user/{user_id}/metrics`, `/metrics` |
| AN5 | Dashboard realtime (WS) | `ws /api/v1/analytics/ws`, `/dashboard/{user_id}` |

---

## Additional Services

### backoffice-service (8011)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| BO1 | Customer cases | `/api/v1/backoffice/customer-cases` (+ `/{id}/assign`) |
| BO2 | Fraud cases | `/api/v1/backoffice/fraud-cases` (+ `/{id}/assign`, `/{id}/resolve`) |
| BO3 | KYC reviews | `/api/v1/backoffice/kyc-reviews` (+ `/{id}/review`) |
| BO4 | Task inbox (workflow) | `/api/v1/backoffice/tasks` (+ `/{taskId}/transition`, `/pending`, `/usertasks/instance`) |

### partner-service (8012)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| P1 | SNAP-BI payment | `/v1/partner/auth/token`, `/payments`, `/payments/{id}`, `/refund` |
| P2 | Partner CRUD | `/partners` (+ `/{id}`, `/{id}/keys/regenerate`) |
| P3 | API key lifecycle | `/partners/{partnerId}/api-keys` (+ `/{keyId}/revoke`, `/{keyId}/rotate`) |
| P4 | Webhook subscription | `/partners/{partnerId}/webhooks` (+ `/{webhookId}/deliveries`, `/secret/regenerate`) |
| P5 | Payment link | `/partners/{partnerId}/payment-links` (+ `/{linkId}`) + public `/pay/{slug}`, `/{slug}/confirm` |
| P6 | Merchant | `/merchants`, `/partners/{partnerId}/{merchantId}`, `/{merchantId}/qr`, `/qr/{referenceId}/pay` |
| P7 | Sertifikat partner | `/partners/{partnerId}/certificates` (+ `/{id}/rotate`, `/validate`, `/deactivate`, `/active`, `/expiring`, `/rotate-all`) |
| P8 | Sandbox admin | `/admin/sandbox` (+ `/scenarios`, `/seed`, `/test-accounts`, `/test-va`) |

### promotion-service (8013)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| PR1 | Promo campaign | `/api/v1/promotions` (+ `/{id}/activate`, `/code/{code}`, `/{code}/claim`) |
| PR2 | Promo redemption | `/api/v1/promotions/apply`, `/validate/{promoCode}` |
| PR3 | Reward | `/api/v1/rewards` (+ `/account/{accountId}/summary`) |
| PR4 | Cashback | `/api/v1/cashbacks` (+ `/account/{accountId}/summary`) |
| PR5 | Loyalty points | `/api/v1/loyalty-points` (+ `/account/{accountId}/balance`, `/redeem`) |
| PR6 | Referral | `/api/v1/referrals` (+ `/code/{code}`, `/complete`, `/referrer/{referrerAccountId}/summary`) |

### support-service (8014)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| SU1 | Training & modul | `/api/v1/support/modules` (+ `/{id}/status`, `/mandatory`) |
| SU2 | Training assignment | `/api/v1/support/trainings` (+ `/assign`, `/agent/{agentId}/status`, `/training-status`) |
| SU3 | Agent | `/api/v1/support/agents` (+ `/{id}/status`, `/employee/{employeeId}`) |

### compliance-service (8087)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| C1 | Audit report | `/api/v1/compliance/audit-report` (+ `/{id}`) |
| C2 | GDPR audit trail | `/api/v1/gdpr-audit` (+ `/users/{userId}`, `/users/{userId}/count`, `/users/{userId}/date-range`, `/accessed-by/{accessedBy}`, `/failed-access`, `/operations/{operationType}`, `/services/{serviceName}`, `/search`) |

### cms-service (8095)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| CM1 | Content management | `/api/v1/contents` (+ `/{id}`, `/{id}/status`, `/type/{type}`, `/status/{status}`) |
| CM2 | Public content | `/api/v1/public/contents` (+ `/type/{type}`) |

### dispute-service (8098)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| D1 | Dispute lifecycle | `/api/v1/disputes` (+ `/{disputeId}/investigate`, `/resolve`, `/reject`, `/escalate`, `/evidence`, `/customer/{customerId}`, `/merchant/{merchantId}`, `/status/{status}`, `/transaction/{transactionId}`) |
| D2 | Refund | `/api/v1/refunds` (+ `/full`, `/partial`, `/{refundId}/process`, `/complete`, `/fail`, `/cancel`, `/status/{status}`, `/transaction/{transactionId}`) |

### product-catalog-service (8100)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| PC1 | Product admin | `/admin/products` (+ `/{code}`, `/{code}/activate`, `/type/{type}`) |
| PC2 | Product publik | `/products` (+ `/{code}`, `/{code}/parameters/{key}`) |

### integration-service (8101)

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| IN1 | Message (queue) management | `/api/v1/integration/messages` (+ `/{messageId}/status`, `/retry`, `/cancel`) |
| IN2 | HTTP/Soap sender | `/api/v1/integration/http/send`, `/soap/send` |
| IN3 | Swift (ISO20022) | `/api/v1/integration/swift/process` |
| IN4 | Laporan OJK | `/api/v1/integration/ojk/generate-report` |
| IN5 | Status | `/api/v1/integration/status` |

---

## Module Pendukung (Workload Terpisah)

### lending-rules

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| LR1 | Credit scoring rules | `/api/v1/rules/credit-score` |

### loan-origination-process

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| LO1 | Loan origination workflow | `LoanOriginationController` (+ `/{id}/approve`) |

---

## External Service Simulators

> Simulator eksternal (bukan production endpoint — hanya untuk dev/test).

### bi-fast-simulator

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| SB1 | Transfer BI-FAST simulasi | `POST /api/v1/transfer` |
| SB2 | Inquiry | `POST /api/v1/inquiry` |
| SB3 | Status | `GET /api/v1/status/{referenceNumber}` |
| SB4 | Sandbox admin | `/api/v1/sandbox/*` |

### dukcapil-simulator

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| SD1 | Verify NIK | `POST /api/v1/verify` |
| SD2 | Data NIK | `GET /api/v1/nik/{nik}` |
| SD3 | Match photo | `POST /api/v1/match-photo` |

### qris-simulator

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| SQ1 | Generate QR | `POST /api/v1/qris/generate` |
| SQ2 | Pay QR | `POST /api/v1/qris/pay` |
| SQ3 | Sandbox & test merchant | `/api/v1/sandbox`, `/test-merchants`, `/scenarios` |

### biller-simulator

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| SBL1 | Biller inquiry | `POST /api/v1/biller/inquiry` |
| SBL2 | Biller pay | `POST /api/v1/biller/pay` |
| SBL3 | Biller status | `GET /api/v1/biller/status/{referenceNumber}` |

### va-simulator

| # | Fitur | Endpoint utama |
|:---:|:---|:---|
| SV1 | Register VA | `POST /api/v1/va/register` |
| SV2 | VA inquiry | `POST /api/v1/va/inquiry` |
| SV3 | VA pay | `POST /api/v1/va/pay` |
| SV4 | Detail VA | `GET /api/v1/va/{vaNumber}` |

---

*Last updated: 2026-08-28. Verifikasi automated diff path-by-path vs semua `*Controller.java`/`*Resource.java`/Python routers (148 file) — bersih setelah penambahan 6 fitur: K6 upload KTP, G9 gateway analytics, G3 assignments, P7 rotate-all, SU2 training-status, LO1 approve. Fix 2026-08-28: `fx-service F1` gateway `/api/v1/fx/*` vs direct `/v1/*`, `FLOWS.md` transfer `INTERNAL_TRANSFER` enum & `QRIS` gateway `/api/v1/qris/pay`, product-catalog cache 500 fixed via `PAYU_CACHE_ENABLED=false`.*
