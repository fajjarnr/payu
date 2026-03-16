# Deep Audit Findings - 2026-03-16

> Detailed addendum for the March 16 deep audit referenced from [`TODOS.md`](./TODOS.md).
> These findings are newly identified beyond the already-listed open bug table in `TODOS.md`.

---

## Summary

| Area | New Bugs |
| :--- | -------: |
| Backend Logic | 32 |
| Frontend Logic | 38 |
| Auth / Session | 19 |
| Frontend-Backend Mismatch | 35 |
| Infrastructure / OpenShift | 34 |
| Test Coverage / Quality | 24 |
| **TOTAL** | **182** |

Reserved bug IDs:

- `BUG-BE-163` - `BUG-BE-194`
- `BUG-FE-069` - `BUG-FE-106`
- `BUG-AUTH-016` - `BUG-AUTH-034`
- `BUG-CROSS-039` - `BUG-CROSS-073`
- `BUG-INFRA-010` - `BUG-INFRA-043`
- `BUG-TEST-027` - `BUG-TEST-050`

---

## Backend Logic (`BUG-BE-163` - `BUG-BE-194`)

| Key | Severity | Service | Summary | Evidence | Impact |
| :-- | :------: | :------ | :------ | :------- | :----- |
| BUG-BE-163 | P0 | wallet-service | `reserveBalance` accepts zero/negative amounts with no `amount > 0` validation | `backend/wallet-service/src/main/java/id/payu/wallet/application/service/WalletService.java:142` | Negative reservations can corrupt wallet balances and bypass balance checks |
| BUG-BE-164 | P0 | wallet-service | `credit` accepts zero/negative amounts with no `amount > 0` validation | `backend/wallet-service/src/main/java/id/payu/wallet/application/service/WalletService.java:289` | Negative credit can function as unauthorized debit |
| BUG-BE-165 | P0 | wallet-service | `reserveBalance` reads wallet without pessimistic lock before reservation | `backend/wallet-service/src/main/java/id/payu/wallet/application/service/WalletService.java:142` | Concurrent reservations can over-reserve beyond real balance |
| BUG-BE-166 | P1 | wallet-service | `SettlementService.applyRevenueSplit` only logs revenue split and never credits partner wallets | `backend/wallet-service/src/main/java/id/payu/wallet/application/service/SettlementService.java:268` | Revenue sharing is a no-op |
| BUG-BE-167 | P1 | wallet-service | `generateRoyaltyStatement` never accumulates total royalties and always reports zero | `backend/wallet-service/src/main/java/id/payu/wallet/application/service/SettlementService.java:284` | Royalty statements are financially incorrect |
| BUG-BE-168 | P2 | wallet-service | `scheduledDailySettlement` checks stale pre-processing batch discrepancy state | `backend/wallet-service/src/main/java/id/payu/wallet/application/service/SettlementService.java:336` | Real discrepancies can be missed |
| BUG-BE-169 | P0 | wallet-service | `CardService` public operations lack ownership validation by card ID | `backend/wallet-service/src/main/java/id/payu/wallet/application/service/CardService.java` | Any user can freeze/unfreeze another user's card |
| BUG-BE-170 | P1 | wallet-service | `PocketService.creditPocket` and `debitPocket` accept zero/negative amounts | `backend/wallet-service/src/main/java/id/payu/wallet/application/service/PocketService.java` | Pocket fund movement can be inverted or corrupted |
| BUG-BE-171 | P0 | fx-service | `reverseConversion` marks transaction reversed but does not reverse wallet debit/credit | `backend/fx-service/src/main/java/id/payu/fx/application/service/FxConversionService.java:96` | FX reversals are cosmetic and leave money misallocated |
| BUG-BE-172 | P0 | investment-service | `buyMutualFund` debits wallet before persistence with no rollback on save failure | `backend/investment-service/src/main/java/id/payu/investment/application/service/InvestmentApplicationService.java:205` | Money can be lost with no investment record |
| BUG-BE-173 | P0 | investment-service | `buyGold` has same wallet-debit-without-rollback failure mode | `backend/investment-service/src/main/java/id/payu/investment/application/service/InvestmentApplicationService.java:263` | Money can be lost with no gold purchase record |
| BUG-BE-174 | P0 | investment-service | `sellInvestment` does not verify request `accountId` matches authenticated user | `backend/investment-service/src/main/java/id/payu/investment/application/service/InvestmentApplicationService.java:319` | Users can sell another user's investments |
| BUG-BE-175 | P2 | lending-service | `applyLoan` passes `command.userId()` to `calculateCreditScore` with UUID/string mismatch risk | `backend/lending-service/src/main/java/id/payu/lending/application/service/LendingApplicationService.java:59` | Credit scoring path can fail or use wrong overload |
| BUG-BE-176 | P1 | lending-service | `calculateCreditScore` always returns hardcoded score `700` | `backend/lending-service/src/main/java/id/payu/lending/application/service/LendingApplicationService.java:206` | Risk scoring is non-functional |
| BUG-BE-177 | P1 | account-service | `BeneficiaryController` comments about ownership validation but does not enforce it | `backend/account-service/src/main/java/id/payu/account/adapter/web/BeneficiaryController.java:43` | Users can list/manage beneficiaries across accounts |
| BUG-BE-178 | P1 | auth-service | `RiskEvaluationService.recordSuccessfulLogin` keeps `addKnownDevice` commented out | `backend/auth-service/src/main/java/id/payu/auth/application/service/RiskEvaluationService.java:100` | Known devices are never recorded; false-positive risk escalations |
| BUG-BE-179 | P0 | transaction-service | Scheduled transfer cancel/pause/resume flows have no ownership validation by UUID | `backend/transaction-service/src/main/java/id/payu/transaction/application/service/ScheduledTransferService.java` | Users can disrupt another user's scheduled transfers |
| BUG-BE-180 | P0 | transaction-service | `VirtualAccountService.handleBankCallback` does not validate callback amount against expected VA amount | `backend/transaction-service/src/main/java/id/payu/transaction/application/service/VirtualAccountService.java:93` | Underpayment or overpayment can be accepted silently |
| BUG-BE-181 | P0 | partner-service | `createRefund` does not validate refund amount <= original payment amount | `backend/partner-service/src/main/java/id/payu/partner/application/service/SnapBiPaymentService.java:112` | Oversized refunds create money |
| BUG-BE-182 | P1 | partner-service | `SnapBiPaymentService` stores payment data in in-memory `ConcurrentHashMap` only | `backend/partner-service/src/main/java/id/payu/partner/application/service/SnapBiPaymentService.java` | Payment records disappear on restart and money movement is not real |
| BUG-BE-183 | P1 | partner-service | `createPayment` accepts zero/negative amounts | `backend/partner-service/src/main/java/id/payu/partner/application/service/SnapBiPaymentService.java:30` | Invalid SNAP-BI payments can be created |
| BUG-BE-184 | P0 | partner-service | `settleToMerchantWallet` credits merchant wallet without debiting payer | `backend/partner-service/src/main/java/id/payu/partner/application/service/MerchantService.java:203` | Merchant settlement creates money |
| BUG-BE-185 | P0 | partner-service | `confirmQrPayment` does not validate payer identity ownership | `backend/partner-service/src/main/java/id/payu/partner/application/service/MerchantService.java:175` | Users can confirm payments against another payer account |
| BUG-BE-186 | P1 | promotion-service | `grantLoyaltyPoints` hardcodes current balance as zero | `backend/promotion-service/src/main/java/id/payu/promotion/application/service/ReferralService.java:167` | Loyalty ledger `balanceAfter` is corrupted |
| BUG-BE-187 | P0 | billing-service | `SubscriptionService.processCharge` marks charges successful without wallet debit | `backend/billing-service/src/main/java/id/payu/billing/application/service/SubscriptionService.java:273` | Recurring subscriptions renew for free |
| BUG-BE-188 | P2 | partner-service | `SnapBiTokenService` stores one token per `clientId`, silently invalidating previous token | `backend/partner-service/src/main/java/id/payu/partner/application/service/SnapBiTokenService.java:149` | Shared client usage causes spurious token invalidation |
| BUG-BE-189 | P0 | wallet-service | `SavingsGoalController` authorizes by permission only and never verifies wallet ownership | `backend/wallet-service/src/main/java/id/payu/wallet/adapter/web/SavingsGoalController.java:56` | Users can manage savings goals for another wallet |
| BUG-BE-190 | P0 | wallet-service | `WalletGrpcService.transfer` can debit source and fail credit destination after reservation commit | `backend/wallet-service/src/main/java/id/payu/wallet/adapter/grpc/WalletGrpcService.java:204` | Inter-wallet transfers can destroy money |
| BUG-BE-191 | P0 | lending-service | `activatePayLater` trusts caller-supplied `userId` request param with no ownership check | `backend/lending-service/src/main/java/id/payu/lending/adapter/web/LendingController.java:193` | Users can activate PayLater for another user |
| BUG-BE-192 | P1 | lending-service | `calculateCreditScore` endpoint trusts caller-supplied `userId` request param | `backend/lending-service/src/main/java/id/payu/lending/adapter/web/LendingController.java:300` | Users can trigger/view another user's credit score |
| BUG-BE-193 | P1 | lending-service | `checkPreApproval` trusts `userId` in request body with no ownership enforcement | `backend/lending-service/src/main/java/id/payu/lending/adapter/web/LendingController.java:328` | Users can inspect another user's pre-approval status |
| BUG-BE-194 | P1 | analytics-service | SQLAlchemy filter uses Python `is True` identity check instead of SQL equality | `backend/analytics-service/src/app/api/v1/analytics.py:381` | High-risk transaction query returns wrong results |

---

## Frontend Logic (`BUG-FE-069` - `BUG-FE-106`)

| Key | Severity | Area | Summary | Evidence | Impact |
| :-- | :------: | :--- | :------ | :------- | :----- |
| BUG-FE-069 | P1 | Auth / State | Logout does not clear wallet or notification store state | `frontend/web-app/src/hooks/useLogout.ts`, `frontend/web-app/src/stores/walletStore.ts`, `frontend/web-app/src/stores/notificationStore.ts` | Next user can inherit stale wallet and notification state |
| BUG-FE-070 | P2 | Hook / Service | `useCreditPocket` passes `currency` into description arg slot | `frontend/web-app/src/hooks/useWallet.ts`, `frontend/web-app/src/services/WalletService.ts` | Pocket credit metadata is corrupted |
| BUG-FE-071 | P2 | Auth / Cache | Logout does not clear AB testing localStorage cache | `frontend/web-app/src/services/ABTestingService.ts`, `frontend/web-app/src/hooks/useLogout.ts` | Experiment assignments leak across users |
| BUG-FE-072 | P1 | XSS / Security | Landing page renders raw translation HTML via `dangerouslySetInnerHTML` without sanitization | `frontend/web-app/src/app/[locale]/page.tsx:177` | Stored XSS risk on public landing page |
| BUG-FE-073 | P1 | Dashboard | `BalanceCard` hardcodes `percentage={45.2}` | `frontend/web-app/src/app/[locale]/dashboard/page.tsx:82`, `frontend/web-app/src/components/dashboard/BalanceCard.tsx:15` | Users see fabricated savings percentage |
| BUG-FE-074 | P1 | Dashboard | `FinancialHealthScore` hardcodes score `78` and previous score `72` | `frontend/web-app/src/app/[locale]/dashboard/page.tsx:90` | Users see fabricated financial health score |
| BUG-FE-075 | P1 | Dashboard | `BalanceCard` hardcodes income and expense totals | `frontend/web-app/src/components/dashboard/BalanceCard.tsx:127` | Financial summary is fabricated |
| BUG-FE-076 | P1 | Dashboard | `BalanceCard` fabricates net worth as `balance * 1.5` | `frontend/web-app/src/components/dashboard/BalanceCard.tsx:65` | Net worth is inflated and incorrect |
| BUG-FE-077 | P2 | Dashboard | `BalanceCard` hardcodes card number, expiry, and cardholder | `frontend/web-app/src/components/dashboard/BalanceCard.tsx:100` | All users see fake card details |
| BUG-FE-078 | P1 | Investments | Investments page falls back to fabricated portfolio balance, allocation, and gains | `frontend/web-app/src/app/[locale]/investments/page.tsx:20`, `frontend/web-app/src/app/[locale]/investments/page.tsx:88` | Users see fake portfolio performance |
| BUG-FE-079 | P1 | Lending | Lending page shows hardcoded credit score and transaction data | `frontend/web-app/src/app/[locale]/lending/page.tsx:50` | Borrowing decisions are informed by fake data |
| BUG-FE-080 | P2 | Cards | Cards page falls back to hardcoded card number, expiry, holder, and usage | `frontend/web-app/src/app/[locale]/cards/page.tsx:58`, `frontend/web-app/src/app/[locale]/cards/page.tsx:233` | Users see fake card inventory and limits |
| BUG-FE-081 | P1 | Transfer | Transfer page hardcodes recent contacts and review balance `Rp 86.353.000` | `frontend/web-app/src/app/[locale]/transfer/page.tsx:86`, `frontend/web-app/src/app/[locale]/transfer/page.tsx:283` | Contacts and balance preview are fabricated |
| BUG-FE-082 | P2 | Security | Security page hardcodes active sessions device list | `frontend/web-app/src/app/[locale]/security/page.tsx:21` | Session inventory is fake |
| BUG-FE-083 | P2 | Backoffice | Backoffice main page hardcodes customer/session stats | `frontend/web-app/src/app/[locale]/backoffice/page.tsx:7` | Admin metrics are fabricated |
| BUG-FE-084 | P2 | BFF / Security | BFF proxy forwards all `x-*` headers too broadly | `frontend/web-app/src/app/api/v1/[...path]/route.ts:186` | Internal or spoofed headers can leak downstream |
| BUG-FE-085 | P1 | i18n | Landing page contains extensive hardcoded Indonesian copy outside i18n | `frontend/web-app/src/app/[locale]/page.tsx:237`, `frontend/web-app/src/app/[locale]/page.tsx:351` | Locale switching leaves major content untranslated |
| BUG-FE-086 | P1 | Scheduled Transfers | Scheduled transfers page uses `user?.id` instead of account ID | `frontend/web-app/src/app/[locale]/scheduled-transfers/page.tsx:51` | Queries use wrong identifier and fail |
| BUG-FE-087 | P1 | Dashboard | `FinancialHealthScore` hardcodes factor breakdown values | `frontend/web-app/src/components/dashboard/FinancialHealthScore.tsx:183` | Breakdown is fabricated |
| BUG-FE-088 | P1 | Dashboard | `SpendingInsights` always renders hardcoded categories because no live `data` prop is passed | `frontend/web-app/src/components/dashboard/SpendingInsights.tsx:36`, `frontend/web-app/src/app/[locale]/dashboard/page.tsx` | All users see identical fake spending patterns |
| BUG-FE-089 | P1 | Dashboard | `BudgetTracking` always renders hardcoded default budgets | `frontend/web-app/src/components/dashboard/BudgetTracking.tsx:45`, `frontend/web-app/src/app/[locale]/dashboard/page.tsx` | Budget alerts are fabricated |
| BUG-FE-090 | P1 | Dashboard | `InvestmentPerformance` defaults to fabricated ROI and investment total | `frontend/web-app/src/components/dashboard/InvestmentPerformance.tsx:52` | All users see fake investment performance |
| BUG-FE-091 | P1 | Dashboard | `StatsCharts` is fully hardcoded for returns, spending, allocation, and totals | `frontend/web-app/src/components/dashboard/StatsCharts.tsx:34`, `frontend/web-app/src/components/dashboard/StatsCharts.tsx:150` | Charts are entirely fabricated |
| BUG-FE-092 | P2 | Backoffice | KYC page hardcodes pending count | `frontend/web-app/src/app/[locale]/backoffice/kyc/page.tsx:32` | Admin queue depth is stale |
| BUG-FE-093 | P2 | Backoffice | Fraud page hardcodes critical alert count | `frontend/web-app/src/app/[locale]/backoffice/fraud/page.tsx:33` | Admin fraud summary is stale |
| BUG-FE-094 | P2 | Backoffice | Customers page hardcodes open count | `frontend/web-app/src/app/[locale]/backoffice/customers/page.tsx:33` | Admin case count is stale |
| BUG-FE-095 | P1 | Backoffice | FX rates page uses `MOCK_FX_RATES` instead of live API | `frontend/web-app/src/app/[locale]/backoffice/fx-rates/page.tsx:32` | Admin sees fake exchange rates |
| BUG-FE-096 | P2 | Backoffice | AB testing page uses `MOCK_EXPERIMENTS` and hardcoded metrics | `frontend/web-app/src/app/[locale]/backoffice/ab-testing/page.tsx:42`, `frontend/web-app/src/app/[locale]/backoffice/ab-testing/page.tsx:127` | Admin experiment view is fabricated |
| BUG-FE-097 | P2 | Backoffice | Campaigns page uses `MOCK_CAMPAIGNS` and hardcoded stats | `frontend/web-app/src/app/[locale]/backoffice/campaigns/page.tsx:40`, `frontend/web-app/src/app/[locale]/backoffice/campaigns/page.tsx:119` | Campaign reporting is fabricated |
| BUG-FE-098 | P2 | Backoffice | CMS page uses `MOCK_CONTENT` and hardcoded totals | `frontend/web-app/src/app/[locale]/backoffice/cms/page.tsx:43`, `frontend/web-app/src/app/[locale]/backoffice/cms/page.tsx:178` | Admin CMS cannot reflect live content |
| BUG-FE-099 | P2 | Backoffice | Broadcast page uses `MOCK_BROADCASTS` and hardcoded engagement metrics | `frontend/web-app/src/app/[locale]/backoffice/broadcast/page.tsx:33`, `frontend/web-app/src/app/[locale]/backoffice/broadcast/page.tsx:109` | Broadcast metrics are fabricated |
| BUG-FE-100 | P2 | Backoffice | Compliance page hardcodes security/compliance scores as fallback | `frontend/web-app/src/app/[locale]/backoffice/compliance/page.tsx:78` | Admin compliance posture is fabricated |
| BUG-FE-101 | P2 | Backoffice | Partners page hardcodes partner counts and SNAP BI volume fallback | `frontend/web-app/src/app/[locale]/backoffice/partners/page.tsx:71` | Partner stats are fabricated |
| BUG-FE-102 | P2 | Settings | Statement downloader effect uses stale closure pattern with missing deps | `frontend/web-app/src/components/settings/statement-downloader.tsx:47` | Statement list can fail to refresh correctly |
| BUG-FE-103 | P2 | CMS | `PromoPopup` ignores `sessionKey` when reading initial session state | `frontend/web-app/src/components/cms/PromoPopup.tsx:50`, `frontend/web-app/src/components/cms/PromoPopup.tsx:115` | Multi-popup show/hide state is inconsistent |
| BUG-FE-104 | P2 | CMS / Navigation | `BannerCarousel` deep links use `window.location.href` instead of router navigation | `frontend/web-app/src/components/cms/BannerCarousel.tsx:46` | Full page reload loses SPA state |
| BUG-FE-105 | P2 | Feedback | `FeedbackWidget.getConsoleLogs()` always returns empty array | `frontend/web-app/src/components/feedback/FeedbackWidget.tsx:112` | Auto-captured console logs never attach |
| BUG-FE-106 | P2 | CMS / React | `BannerCarousel` lacks `'use client'` despite client-only handlers and refs | `frontend/web-app/src/components/cms/BannerCarousel.tsx:1` | Build/runtime errors or silent client behavior failure |

---

## Auth / Session (`BUG-AUTH-016` - `BUG-AUTH-034`)

| Key | Severity | Area | Summary | Evidence | Impact |
| :-- | :------: | :--- | :------ | :------- | :----- |
| BUG-AUTH-016 | P0 | Gateway / Identity Spoofing | `AuthorizationFilter` uses `add()` instead of replacing inbound identity headers | `backend/gateway-service/src/main/java/id/payu/gateway/adapter/filter/AuthorizationFilter.java:213` | Attacker can inject `X-User-Id` / `X-Account-Id` / roles headers |
| BUG-AUTH-017 | P0 | Gateway / Auth Bypass | IP whitelist bypass is enabled via `X-Bypass-IP-Check: true` header | `backend/gateway-service/src/main/java/id/payu/gateway/adapter/filter/IpWhitelistFilter.java:39`, `backend/gateway-service/src/main/resources/application.yaml:396` | External clients can skip IP whitelist checks |
| BUG-AUTH-018 | P0 | Gateway / Crypto | HMAC signature validation uses `String.equals()` instead of constant-time comparison | `backend/gateway-service/src/main/java/id/payu/gateway/adapter/filter/RequestSigningFilter.java:152` | Timing attack against request signatures |
| BUG-AUTH-019 | P0 | Gateway / Crypto | HMAC signature intentionally omits request body hashing | `backend/gateway-service/src/main/java/id/payu/gateway/adapter/filter/RequestSigningFilter.java:193` | Signed bodies can be tampered with |
| BUG-AUTH-020 | P0 | Gateway / JWT | Gateway has hardcoded default JWT secret fallback | `backend/gateway-service/src/main/java/id/payu/gateway/config/GatewayConfig.java:500` | Forged JWTs become possible if env is missing |
| BUG-AUTH-021 | P0 | Analytics / WebSocket | Analytics websocket accepts unauthenticated connections with bare `user_id` | `backend/analytics-service/src/app/api/v1/websocket.py:19` | Real-time analytics data can be exfiltrated anonymously |
| BUG-AUTH-022 | P0 | KYC | KYC endpoints lack authentication middleware | `backend/kyc-service/src/app/api/v1/kyc.py` | Sensitive KYC actions are publicly callable |
| BUG-AUTH-023 | P1 | Gateway / Public Paths | Public endpoint prefix matching is overly broad via `startsWith(...)` | `backend/gateway-service/src/main/java/id/payu/gateway/adapter/filter/AuthorizationFilter.java:52` | Unintended subpaths can bypass auth |
| BUG-AUTH-024 | P1 | IP Trust | `X-Forwarded-For` is trusted without trusted-proxy validation | `backend/gateway-service/src/main/java/id/payu/gateway/adapter/filter/IpWhitelistFilter.java:91`, `backend/auth-service/src/main/java/id/payu/auth/adapter/web/AuthController.java:179` | IP spoofing bypasses whitelist, rate limit, and audit correctness |
| BUG-AUTH-025 | P1 | Gateway / Rate Limit | `X-E2E-Test` bypass defeats rate limiting when test mode is enabled | `backend/gateway-service/src/main/java/id/payu/gateway/adapter/filter/RateLimitFilter.java:117` | Production misconfig can disable rate limiting |
| BUG-AUTH-026 | P1 | FX / CORS | FX service allows wildcard origins, methods, and headers | `backend/fx-service/src/main/java/id/payu/fx/config/SecurityConfig.java:45` | Cross-origin abuse against financial endpoints |
| BUG-AUTH-027 | P1 | Frontend / Cookies | Auth cookies are set with `secure: false` in login and refresh routes | `frontend/web-app/src/app/api/auth/login/route.ts:96`, `frontend/web-app/src/app/api/auth/refresh/route.ts:62` | Session tokens can travel over plaintext HTTP |
| BUG-AUTH-028 | P1 | Account Service | `/actuator/**` is publicly permitted | `backend/account-service/src/main/java/id/payu/account/config/SecurityConfig.java:85` | Environment and operational data disclosure |
| BUG-AUTH-029 | P1 | Gateway / JWT | JWKS is loaded once via immutable JWK set with no refresh path | `backend/gateway-service/src/main/java/id/payu/gateway/adapter/filter/AuthorizationFilter.java:123` | Key rotation can lock out all users until restart |
| BUG-AUTH-030 | P1 | FX / Method Security | FX security config lacks `@EnableMethodSecurity` | `backend/fx-service/src/main/java/id/payu/fx/config/SecurityConfig.java:19` | Method-level authorization is ignored |
| BUG-AUTH-031 | P1 | Auth Service / Method Security | Auth service security config lacks `@EnableMethodSecurity` | `backend/auth-service/src/main/java/id/payu/auth/config/SecurityConfig.java:30` | Method-level authorization is ignored |
| BUG-AUTH-032 | P2 | Dispute / Method Security | Dispute security config lacks `@EnableMethodSecurity` | `backend/dispute-service/src/main/java/id/payu/dispute/config/SecurityConfig.java:15` | Method-level authorization is ignored |
| BUG-AUTH-033 | P2 | Promotion / Method Security | Promotion security config lacks `@EnableMethodSecurity` | `backend/promotion-service/src/main/java/id/payu/promotion/config/SecurityConfig.java:17` | Method-level authorization is ignored |
| BUG-AUTH-034 | P2 | KYC / Crypto | KYC service uses HS256 shared-secret JWT model instead of asymmetric key validation | `backend/kyc-service/src/app/config.py:47` | Shared secret compromise enables forged KYC tokens |

---

## Frontend-Backend Mismatch (`BUG-CROSS-039` - `BUG-CROSS-073`)

| Key | Severity | FE File | BE File | Summary | Impact |
| :-- | :------: | :------ | :------ | :------ | :----- |
| BUG-CROSS-039 | P1 | `frontend/web-app/src/services/FxService.ts` | `backend/fx-service/src/main/java/id/payu/fx/adapter/web/FxController.java` | Gateway forwards FX paths to `/fx-api/v1/...` while service exposes `/v1/...` | All FX API calls 404 |
| BUG-CROSS-040 | P1 | `frontend/web-app/src/services/PartnerService.ts` | `frontend/web-app/src/app/api/v1/[...path]/route.ts` | BFF whitelist allows `/partners` but FE calls singular `/partner/payments` SNAP-BI paths | Partner payment calls are blocked at BFF |
| BUG-CROSS-041 | P1 | `frontend/web-app/src/services/WalletService.ts` | `backend/wallet-service/src/main/java/id/payu/wallet/adapter/web/WalletController.java` | FE does not unwrap `ApiResponse<T>` envelope returned by backend | Wallet data resolves to undefined fields |
| BUG-CROSS-042 | P1 | `frontend/web-app/src/services/WalletService.ts` | `backend/gateway-service/src/main/resources/application.yaml` | Gateway has no route for `/pockets` | Pocket CRUD returns 404 |
| BUG-CROSS-043 | P1 | `frontend/web-app/src/services/WalletService.ts` | `backend/wallet-service/src/main/java/id/payu/wallet/dto/CreatePocketRequest.java` | FE `CreatePocketRequest` fields drift from backend DTO (`description` missing; `target/type` extra) | Pocket creation fails validation |
| BUG-CROSS-044 | P1 | `frontend/web-app/src/services/WalletService.ts` | `backend/wallet-service/src/main/java/id/payu/wallet/dto/PocketTransactionRequest.java` | FE sends `description`; backend requires `referenceId` | Pocket credit/debit fails validation |
| BUG-CROSS-045 | P1 | `frontend/web-app/src/services/WalletService.ts` | `backend/wallet-service/src/main/java/id/payu/wallet/adapter/web/CardController.java` | Card listing omits required `accountId` query parameter | Card list request returns 400 |
| BUG-CROSS-046 | P2 | `frontend/web-app/src/services/WalletService.ts` | `backend/wallet-service/src/main/java/id/payu/wallet/adapter/web/CardController.java` | Freeze/unfreeze returns `ApiResponse<Void>` but FE expects a `VirtualCard` | FE reads card data from null response |
| BUG-CROSS-047 | P2 | `frontend/web-app/src/services/WalletService.ts` | `backend/wallet-service/src/main/java/id/payu/wallet/adapter/web/CardController.java` | Card response field names drift (`walletId` vs `accountId`, expiry format, holder name casing) | Card UI shows blank or undefined values |
| BUG-CROSS-048 | P1 | `frontend/web-app/src/services/InvestmentService.ts` | `backend/investment-service/src/main/java/id/payu/investment/adapter/web/InvestmentController.java` | FE uses `/accounts/{userId}` and `/gold/{userId}` while backend exposes `/accounts/me` and `/gold/me` | Investment account lookups 404 |
| BUG-CROSS-049 | P1 | `frontend/web-app/src/services/InvestmentService.ts` | `backend/investment-service/src/main/java/id/payu/investment/adapter/web/InvestmentController.java` | FE sends body to `createAccount()` but backend ignores request body and uses JWT subject | FE fields are silently ignored |
| BUG-CROSS-050 | P1 | `frontend/web-app/src/services/InvestmentService.ts` | `backend/investment-service/src/main/java/id/payu/investment/dto/BuyDepositRequest.java` | FE deposit purchase fields drift from backend DTO (`userId/tenureMonths` vs `accountId/tenure`) | Deposit purchase fails validation |
| BUG-CROSS-051 | P1 | `frontend/web-app/src/services/InvestmentService.ts` | `backend/investment-service/src/main/java/id/payu/investment/dto/BuyMutualFundRequest.java` | FE sends `fundId`; backend requires `fundCode` and `accountId` | Mutual fund purchase fails validation |
| BUG-CROSS-052 | P1 | `frontend/web-app/src/services/InvestmentService.ts` | `backend/investment-service/src/main/java/id/payu/investment/dto/BuyGoldRequest.java` | FE sends `weightGrams`; backend purchases by amount only | Buy-gold semantics drift silently |
| BUG-CROSS-053 | P1 | `frontend/web-app/src/services/LendingService.ts` | `backend/lending-service/src/main/java/id/payu/lending/dto/LoanApplicationCommand.java` | Loan application fields drift (`amount` vs `principalAmount`, missing `externalId`, missing `loanType`) | Loan application fails validation |
| BUG-CROSS-054 | P1 | `frontend/web-app/src/services/LendingService.ts` | `backend/lending-service/src/main/java/id/payu/lending/adapter/web/LendingController.java` | `activatePayLater` backend expects query `userId` but FE sends it in JSON body | PayLater activation fails with 400 |
| BUG-CROSS-055 | P1 | `frontend/web-app/src/services/LendingService.ts` | `backend/lending-service/src/main/java/id/payu/lending/adapter/web/LendingController.java` | `recordPurchase` backend expects request params, FE sends JSON body | PayLater purchase record fails |
| BUG-CROSS-056 | P1 | `frontend/web-app/src/services/LendingService.ts` | `backend/lending-service/src/main/java/id/payu/lending/adapter/web/LendingController.java` | `recordPayment` backend expects request param amount, FE sends JSON body | PayLater payment record fails |
| BUG-CROSS-057 | P1 | `frontend/web-app/src/services/NotificationService.ts` | `backend/notification-service/src/main/java/id/payu/notification/adapter/web/NotificationResource.java` | FE expects paged wrapper but backend returns flat list and `limit` param | Notification list renders empty |
| BUG-CROSS-058 | P1 | `frontend/web-app/src/services/NotificationService.ts` | `backend/notification-service/src/main/java/id/payu/notification/dto/SendNotificationRequest.java` | FE omits required `recipient` and sends unsupported `type` field | Notification send fails validation |
| BUG-CROSS-059 | P1 | `frontend/web-app/src/services/SupportService.ts` | `backend/support-service/src/main/java/id/payu/support/adapter/web/SupportController.java` | `updateAgentStatus` expects `{active: boolean}` but FE sends `{status: "ACTIVE"}` | Agent status update breaks or sets wrong value |
| BUG-CROSS-060 | P2 | `frontend/web-app/src/services/SupportService.ts` | `backend/support-service/src/main/java/id/payu/support/adapter/web/SupportController.java` | Training summary response shape differs from FE `TrainingStatusSummary[]` expectation | Training dashboard crashes or renders empty |
| BUG-CROSS-061 | P2 | `frontend/web-app/src/services/SupportService.ts` | `backend/support-service/src/main/java/id/payu/support/adapter/web/SupportController.java` | Per-agent training status response shape differs from FE expectation | Agent training detail renders undefined fields |
| BUG-CROSS-062 | P1 | `frontend/web-app/src/services/SupportService.ts` | `backend/support-service/src/main/java/id/payu/support/adapter/web/SupportController.java` | FE ticket/FAQ endpoints do not exist in backend | Support module routes 404 |
| BUG-CROSS-063 | P1 | `frontend/web-app/src/services/PromotionService.ts` | `backend/promotion-service/src/main/java/id/payu/promotion/adapter/web/PromoRedemptionController.java` | FE calls promotion listing/detail/claim endpoints not implemented by backend | Promotion flows 404 |
| BUG-CROSS-064 | P1 | `frontend/web-app/src/services/PromotionService.ts` | `frontend/web-app/src/app/api/v1/[...path]/route.ts` | BFF whitelist blocks loyalty, cashback, referral, gamification, and rewards prefixes | Promotion-adjacent feature calls 404 at BFF |
| BUG-CROSS-065 | P1 | `frontend/web-app/src/services/PromotionService.ts` | `backend/gateway-service/src/main/resources/application.yaml` | Gateway has no route for `/gamification` | Gamification stays unreachable even after BFF fix |
| BUG-CROSS-066 | P1 | `frontend/web-app/src/services/BillingService.ts` | `backend/gateway-service/src/main/resources/application.yaml` | Gateway has no route for `/topup` | Top-up feature is unreachable |
| BUG-CROSS-067 | P1 | `frontend/web-app/src/services/BillingService.ts` | `backend/billing-service/src/main/java/id/payu/billing/adapter/web/PaymentController.java` | FE expects `GET /payments` history endpoint that backend does not implement | Payment history view 404s |
| BUG-CROSS-068 | P1 | `frontend/web-app/src/services/BillingService.ts` | `backend/billing-service/src/main/java/id/payu/billing/dto/TopUpRequest.java` | FE sends `billerCode/customerId`; backend requires `provider/walletNumber` | Top-up request fails validation |
| BUG-CROSS-069 | P1 | `frontend/web-app/src/services/TransactionService.ts` | `backend/gateway-service/src/main/resources/application.yaml` | Gateway has no route for `/scheduled-transfers` | Scheduled transfer CRUD 404s |
| BUG-CROSS-070 | P1 | `frontend/web-app/src/services/TransactionService.ts` | `backend/gateway-service/src/main/resources/application.yaml` | Gateway has no route for `/split-bills` | Split-bill flows 404 |
| BUG-CROSS-071 | P1 | `frontend/web-app/src/services/AccountService.ts` | `backend/account-service/src/main/java/id/payu/account/dto/VerifyNikResponse.java` | NIK verification response fields drift (`birthDate` vs `dateOfBirth`, `verified` vs `isValid`) | KYC verification UI shows undefined data |
| BUG-CROSS-072 | P1 | `backend/gateway-service/src/main/resources/schemas/auth-register.json` | `backend/account-service/src/main/java/id/payu/account/dto/RegisterUserRequest.java` | Gateway register schema requires `password` and rejects backend fields like `externalId` / `nik` | Registration requests can be rejected at gateway |
| BUG-CROSS-073 | P1 | `backend/gateway-service/src/main/resources/schemas/transactions-create.json` | `backend/transaction-service/src/main/java/id/payu/transaction/adapter/web/TransactionController.java` | Gateway transaction-create schema references request shape no controller actually implements | Schema validation and runtime contract drift |

---

## Infrastructure / OpenShift (`BUG-INFRA-010` - `BUG-INFRA-043`)

| Key | Severity | Area | Summary | Evidence | Impact |
| :-- | :------: | :--- | :------ | :------- | :----- |
| BUG-INFRA-010 | P1 | Network Policies | NetworkPolicy labels target `payu-banking` while workloads use `payu` | `infrastructure/openshift/base/network-policies.yaml:50`, `infrastructure/openshift/base/account-service.yaml:9` | Allow rules never match; traffic can be blocked platform-wide |
| BUG-INFRA-011 | P1 | Secrets | auth-service base manifest hardcodes Keycloak and DB secrets in plaintext | `infrastructure/openshift/base/auth-service.yaml:62` | Credential leakage in git |
| BUG-INFRA-012 | P1 | Secrets | KYC base manifest hardcodes `SECRET_KEY` in plaintext | `infrastructure/openshift/base/kyc-service.yaml:46` | Cryptographic key exposure |
| BUG-INFRA-013 | P1 | Secrets | Dev overlay commits JWT, webhook, NextAuth, and Keycloak secrets in tracked YAML | `infrastructure/openshift/overlays/dev/secrets/dev-secrets.yaml:12` | Secret exposure pattern in VCS |
| BUG-INFRA-014 | P1 | Secrets | RHBK secret manifests include plaintext admin and DB passwords | `infrastructure/openshift/infra/base/rhbk-secrets.yaml:27` | Identity plane compromise risk |
| BUG-INFRA-015 | P1 | Secrets | Dev kustomize patch hardcodes real-looking passwords | `infrastructure/openshift/overlays/dev/kustomization.yaml:210` | Password leakage in git history |
| BUG-INFRA-016 | P1 | ArgoCD / Security | ArgoCD admin hash and placeholder OIDC secret are committed | `infrastructure/openshift/argocd/app-of-apps.yaml:194` | ArgoCD takeover risk |
| BUG-INFRA-017 | P1 | Service Mesh | JWT JWKS in mesh config is placeholder/invalid | `infrastructure/openshift/service-mesh/gateway.yaml:452` | JWT validation fails or behaves unpredictably |
| BUG-INFRA-018 | P1 | Service Mesh | TLS cert/key secret data is placeholder and invalid | `infrastructure/openshift/service-mesh/gateway.yaml:479` | HTTPS termination breaks |
| BUG-INFRA-019 | P1 | Service Mesh | `deny-all` AuthorizationPolicy with empty rule blocks all traffic | `infrastructure/openshift/service-mesh/peer-authentication.yaml:281` | Full mesh traffic outage |
| BUG-INFRA-020 | P1 | Service Mesh | Mesh-wide PeerAuthentication incorrectly scoped to ingressgateway selector only | `infrastructure/openshift/service-mesh/peer-authentication.yaml:17` | mTLS is not enforced across the mesh |
| BUG-INFRA-021 | P1 | Service Mesh | `portLevelMtls` uses invalid YAML structure | `infrastructure/openshift/service-mesh/peer-authentication.yaml:103` | Port-specific mTLS config is ignored |
| BUG-INFRA-022 | P2 | Service Mesh | Egress DestinationRule host pattern `*./*` is invalid | `infrastructure/openshift/service-mesh/destination-rules.yaml:435` | Egress traffic policy is not applied |
| BUG-INFRA-023 | P2 | Service Mesh / Kustomize | Service mesh kustomization globally forces `namespace: istio-system` | `infrastructure/openshift/service-mesh/kustomization.yaml:8` | Namespace-scoped mesh resources deploy to wrong namespace |
| BUG-INFRA-024 | P2 | Service Mesh | Two auth VirtualService prefixes route to different services and conflict | `infrastructure/openshift/service-mesh/gateway.yaml:88`, `infrastructure/openshift/service-mesh/gateway.yaml:125` | Auth traffic routes inconsistently |
| BUG-INFRA-025 | P2 | Overlay / Kustomize | Prod kustomization sets `namespace: payu` while resources hardcode `payu-prod` | `infrastructure/openshift/overlays/prod/kustomization.yaml:10` | Prod resources land in wrong namespace |
| BUG-INFRA-026 | P1 | ArgoCD | ApplicationSet git directory generator points at YAML-file directory, not service directories | `infrastructure/openshift/argocd/applicationset.yaml:16` | No applications are generated |
| BUG-INFRA-027 | P2 | ArgoCD | ApplicationSet template uses Helm values files in a Kustomize project | `infrastructure/openshift/argocd/applicationset.yaml:40` | ArgoCD render path fails |
| BUG-INFRA-028 | P2 | ArgoCD | Drift-detection CMP generate command has YAML syntax error | `infrastructure/openshift/argocd/drift-detection.yaml:22` | Drift detection plugin does not execute |
| BUG-INFRA-029 | P2 | ArgoCD | Drift detection checks `deploymentconfig` while platform uses `Deployment` | `infrastructure/openshift/argocd/drift-detection.yaml:64` | Drift always misses real resources |
| BUG-INFRA-030 | P2 | ArgoCD | Slack webhook placeholder is committed in secret manifest | `infrastructure/openshift/argocd/drift-detection.yaml:205` | Alerting is non-functional and pattern is unsafe |
| BUG-INFRA-031 | P2 | Pipelines | Deploy pipeline creates deprecated `DeploymentConfig` instead of `Deployment` | `infrastructure/pipelines/deploy-pipeline.yaml:122` | GitOps and pipeline resource types diverge |
| BUG-INFRA-032 | P2 | Pipelines | Deploy pipeline service exposes `port: 80` while ecosystem expects `8080` | `infrastructure/pipelines/deploy-pipeline.yaml:213` | Service mesh and health assumptions drift |
| BUG-INFRA-033 | P2 | Pipelines | Build pipeline uses `golang-lint` for Java/Python services | `infrastructure/pipelines/build-pipeline.yaml:71` | Lint stage is unreliable or meaningless |
| BUG-INFRA-034 | P2 | Pipelines | Build pipeline disables TLS verification for image push | `infrastructure/pipelines/build-pipeline.yaml:160` | Registry push is MITM-susceptible |
| BUG-INFRA-035 | P2 | Pipelines | Rollback pipeline hardcodes Slack webhook placeholder default | `infrastructure/pipelines/rollback-pipeline.yaml:40` | Rollback alerts go nowhere |
| BUG-INFRA-036 | P2 | Pipelines | Rollback pipeline exports all secrets from namespace into backup namespace | `infrastructure/pipelines/rollback-pipeline.yaml:74` | Broad secret exfiltration risk |
| BUG-INFRA-037 | P2 | Pipelines | Trigger binding hardcodes `service-type: spring-boot` for all repos | `infrastructure/pipelines/triggers/git-webhook-trigger.yaml:69` | Quarkus and Python builds use wrong path |
| BUG-INFRA-038 | P2 | Pipelines | Trigger binding uses monorepo repository name as service name | `infrastructure/pipelines/triggers/git-webhook-trigger.yaml:71` | Builds target non-existent `payu-platform` service |
| BUG-INFRA-039 | P2 | Base Manifests | TLS route/cert resources hardcode dev hostnames in base manifests | `infrastructure/openshift/base/tls-certificates.yaml:22` | Non-dev envs serve wrong certificates |
| BUG-INFRA-040 | P2 | Local Dev | `api-portal-service` has conflicting `SERVER_PORT` vs Quarkus `PORT` / `QUARKUS_HTTP_PORT` | `infrastructure/local-podman/podman-compose.yml:1015` | Health check targets wrong port |
| BUG-INFRA-041 | P2 | Local Dev | compliance/lending local ports diverge from cluster assumptions | `infrastructure/local-podman/podman-compose.yml:348`, `infrastructure/local-podman/podman-compose.yml:443` | Local behavior masks deployment port bugs |
| BUG-INFRA-042 | P2 | Base Manifests | Many services lack pod/container `securityContext` hardening | `infrastructure/openshift/base/kyc-service.yaml:24` | Containers can run with excessive privileges |
| BUG-INFRA-043 | P2 | Pipelines / ArgoCD | Deploy/rollback pipeline rollout commands target different resource types than task implementation | `infrastructure/pipelines/deploy-pipeline.yaml:307`, `infrastructure/pipelines/tasks/deploy-task.yaml:63` | Rollout verification targets wrong object kind |

---

## Test Coverage / Quality (`BUG-TEST-027` - `BUG-TEST-050`)

| Key | Severity | Area | Summary | Evidence | Impact |
| :-- | :------: | :--- | :------ | :------- | :----- |
| BUG-TEST-027 | P0 | Gatling | Scala string interpolation is broken in most simulations because URLs miss `s` prefix | `tests/performance/src/test/scala/id/payu/simulations/BalanceQuerySimulation.scala:46`, `tests/performance/src/test/scala/id/payu/simulations/AllServicesSimulation.scala:59` | Performance traffic hits literal `$authUrl/...` strings and results are meaningless |
| BUG-TEST-028 | P0 | Gatling | Perf DTO field names and endpoint paths are stale vs current API | `tests/performance/src/test/scala/id/payu/simulations/QRISPaymentSimulation.scala:69`, `tests/performance/src/test/scala/id/payu/simulations/TransferSimulation.scala:67` | Even fixed simulations would still hit 404/validation failures |
| BUG-TEST-029 | P1 | Gatling | `AllServicesSimulation` has Scala compilation error in assertions block | `tests/performance/src/test/scala/id/payu/simulations/AllServicesSimulation.scala:233` | The broadest simulation never runs |
| BUG-TEST-030 | P1 | k6 | Health checks use `/actuator/health` against Quarkus gateway paths | `tests/performance/k6/crud-load-test.js:74`, `tests/performance/k6/smoke-test.js:26` | Health assertions target wrong endpoint and mask failures |
| BUG-TEST-031 | P1 | k6 | Stress thresholds allow 50% HTTP failure rate | `tests/performance/k6/crud-stress-test.js:23`, `tests/performance/k6/stress-test.js:21` | Severe outage can still pass perf gate |
| BUG-TEST-032 | P2 | k6 | Keycloak discovery path still uses old `/auth/realms/` prefix | `tests/performance/k6/smoke-test.js:37`, `tests/performance/k6/load-test.js:35` | Auth health check gets false negatives |
| BUG-TEST-033 | P2 | k6 | Tests use hardcoded dev URLs with no env override | `tests/performance/k6/config.js:5` | Cannot reliably run same suite in other environments |
| BUG-TEST-034 | P1 | Blackbox E2E | Analytics blackbox suite mostly asserts 404 as success condition | `tests/e2e_blackbox/test_analytics_flow.py:25` | No real analytics-service E2E coverage |
| BUG-TEST-035 | P1 | Blackbox E2E | Compliance blackbox suite treats broken 404/429/503 responses as passing | `tests/e2e_blackbox/test_compliance_flow.py:54` | No real compliance-service E2E coverage |
| BUG-TEST-036 | P2 | Blackbox E2E | KYC blackbox test uses wrong idempotency header name and likely stale field naming | `tests/e2e_blackbox/test_kyc_flow.py:19` | KYC idempotency and request shape are not truly verified |
| BUG-TEST-037 | P2 | Blackbox E2E | Backoffice test asserts brittle message text fragments | `tests/e2e_blackbox/test_backoffice.py:35` | Minor copy changes cause false failures |
| BUG-TEST-038 | P1 | Contract | Auth contract expects nested `data.accessToken` and `email`, but live auth uses flat token fields and `username` | `tests/contract/auth-service/loginUser.groovy:14`, `backend/auth-service/src/main/java/id/payu/auth/dto/LoginResponse.java:8` | Consumers can build against the wrong auth contract |
| BUG-TEST-039 | P1 | Contract | Transfer contract still expects `sourceWalletId` / `targetWalletId` and `COMPLETED` response | `tests/contract/transaction-service/createTransfer.groovy:16`, `backend/transaction-service/src/main/java/id/payu/transaction/dto/InitiateTransferRequest.java:44` | Consumers using contract will fail at runtime |
| BUG-TEST-040 | P0 | Regression | Regression suite targets obsolete phone+PIN auth, stale endpoints, and snake_case fields | `tests/regression/test_financial_flows.py:29`, `tests/regression/test_financial_flows.py:480` | Entire regression suite exercises a non-existent API surface |
| BUG-TEST-041 | P0 | Security Tests | JWT/CORS pentest verification logic passes empty configs and flawed wildcard checks | `tests/security/test_pentest_verification.py:83`, `tests/security/test_pentest_verification.py:117` | Security tests provide false assurance |
| BUG-TEST-042 | P2 | Backend Unit | Wallet and Compliance `SecurityConfigTest` are tautological `!= null` checks | `backend/wallet-service/src/test/java/id/payu/wallet/config/SecurityConfigTest.java:13`, `backend/compliance-service/src/test/java/id/payu/compliance/config/SecurityConfigTest.java:13` | Real security config regressions go undetected |
| BUG-TEST-043 | P1 | Backend Unit | `KeycloakServiceTest` inner class is missing `@Nested`, so lockout test never runs | `backend/auth-service/src/test/java/id/payu/auth/adapter/security/KeycloakServiceTest.java:312` | Account lockout path is untested |
| BUG-TEST-044 | P2 | Backend Unit | notification-service `SimpleTest` is just `assertTrue(true)` | `backend/notification-service/src/test/java/id/payu/notification/SimpleTest.java:10` | Test count is inflated with no coverage value |
| BUG-TEST-045 | P1 | Backend Unit | Vault config tests are guarded or tautological and never validate real behavior | `backend/auth-service/src/test/java/id/payu/auth/config/VaultConfigurationTest.java:20`, `backend/account-service/src/test/java/id/payu/account/config/VaultConfigurationTest.java:100` | Vault integration regressions remain hidden |
| BUG-TEST-046 | P1 | Backend Unit | Gateway filter tests accept broad status ranges including 404/500/503 as pass | `backend/gateway-service/src/test/java/id/payu/gateway/adapter/filter/AuthorizationFilterTest.java:87`, `backend/gateway-service/src/test/java/id/payu/gateway/adapter/filter/RateLimitFilterTest.java:43` | Filter tests can pass while gateway is broken |
| BUG-TEST-047 | P1 | Backend Integration | Gateway integration tests overuse `anyOf(...)` and accept nearly any status outcome | `backend/gateway-service/src/test/java/id/payu/gateway/integration/SecurityAndValidationIntegrationTest.java`, `backend/gateway-service/src/test/java/id/payu/gateway/integration/GatewayFilterChainIntegrationTest.java` | Integration tests give near-zero regression signal |
| BUG-TEST-048 | P2 | Backend Unit | KYC unit tests accept 422 validation errors as success outcomes | `backend/kyc-service/tests/unit/test_api.py:72`, `backend/kyc-service/tests/unit/test_api.py:224` | KYC API tests pass on invalid requests |
| BUG-TEST-049 | P1 | Backend Unit | `CardControllerTest` validates raw PAN exposure via `getFullCardNumber()` | `backend/wallet-service/src/test/java/id/payu/wallet/adapter/web/CardControllerTest.java:69` | Tests normalize possible PCI-DSS-violating full card exposure |
| BUG-TEST-050 | P1 | Backend Integration | `ComplianceIntegrationTest` retrieves audit by unrelated random UUID while still asserting success | `backend/compliance-service/src/test/java/id/payu/compliance/integration/ComplianceIntegrationTest.java:426` | Audit retrieval coverage is false-positive |
