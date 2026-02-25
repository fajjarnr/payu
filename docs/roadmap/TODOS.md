# 🐛 PayU — Bug Backlog & Open Items

> **Dokumen ini hanya berisi item yang BELUM selesai dan perlu tindakan.**
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Bug Summary

| Kategori | P0 Critical | P1 High | P2 Medium | P3 Low | Total |
| :--- | :---: | :---: | :---: | :---: | :---: |
| Backend Logic | 36 | 64 | 42 | 5 | **147** |
| Frontend Logic | 7 | 11 | 23 | 5 | **46** |
| Frontend-Backend Mismatch | 13 | 9 | 7 | — | **29** |
| Auth / Session | 2 | 2 | 3 | 3 | **10** |
| **TOTAL** | **58** | **86** | **75** | **13** | **~232** |

> ⚠️ **Catatan**: Scorecard "Production Readiness 100/100" di PROGRESS.md mencerminkan infra/deploy coverage,
> **bukan** correctness business logic. Bug di bawah ini adalah temuan dari code review mendalam (Feb 24, 2026).

---

## 🔴 Priority Fix List (P0 — Must Fix Before Any Integration)

> Fix items ini sebelum TokoBapak / Nobar mulai integrasi!

| ID | Service | Issue | Impact |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-001~~ | ~~gateway-service~~ | ~~FIXED: JWT validation implemented dengan nimbus-jose-jwt — signature, expiration, issuer, audience validation~~ | ~~Seluruh platform tidak aman~~ |
| ✅ ~~BUG-BE-002~~ | ~~auth-service~~ | ~~FIXED: All auth state (failedAttempts, token cache) already uses Redis CacheService. MFATokenService consolidated.~~ | ~~MFA/brute-force protection gagal di scale-out~~ |
| ✅ ~~BUG-BE-035~~ | ~~partner-service~~ | ~~FIXED: Token store moved to Redis with TTL — token persistent antar pod~~ | ~~Partner integration gagal di HPA~~ |
| ✅ ~~BUG-BE-062~~ | ~~promotion-service~~ | ~~FIXED: CashbackSagaOrchestrator credits wallet via WalletServicePort before recording. Saga compensation on failure.~~ | ~~User tidak terima uang cashback~~ |
| ✅ ~~BUG-BE-060~~ | ~~promotion-service~~ | ~~FIXED: LoyaltyPointsService uses pg_advisory_xact_lock for atomic balance calculation.~~ | ~~Saldo poin salah~~ |
| ✅ ~~BUG-BE-090~~ | ~~shared/api-commons~~ | ~~FIXED: RateLimitAspect uses atomic Lua script for INCR+EXPIRE.~~ | ~~User bisa di-block selamanya~~ |
| ✅ ~~BUG-FE-021~~ | ~~All financial services~~ | ~~FIXED: X-Idempotency-Key header added to TransactionService, WalletService, BillingService via getFinancialMutationHeaders().~~ | ~~Transfer/payment duplikat~~ |
| ✅ ~~BUG-FE-027~~ | ~~providers.tsx~~ | ~~FIXED: mutations.retry set to 0 to prevent auto-retry of financial operations.~~ | ~~Double debit on network error~~ |
| **GAP-001** | *(belum ada)* | Outbound webhook ke partner (TokoBapak/Nobar) tidak ada | Partner tidak bisa tahu payment status |
| **GAP-002** | `partner-service` | Multi-tenancy tidak ada — data isolation antar partner | Data TokoBapak bisa bocor ke Nobar |
| **GAP-006** | All payment endpoints | Idempotency key tidak didukung | Double-charge on retry |
| **GAP-007** | `wallet-service` | Escrow/payment holding belum ada | TokoBapak tidak bisa implement checkout |
| **GAP-008** | *(belum ada)* | Recurring/subscription billing belum ada | Nobar tidak bisa auto-debit |

> ### 📋 Triage Notes — Remaining P0 Items (Feb 24, 2026)
>
> **GAP Items (Semua ⏭️ SKIP):** TokoBapak dan Nobar belum di-develop. GAP-001 s/d GAP-008 adalah fitur integrasi yang baru dibutuhkan saat partner app mulai development. PayU sebagai standalone banking platform sudah bisa jalan tanpa fitur ini.
>
> **Sisa P0 Bugs — Verdict:**
>
> | ID | Service | Verdict | Alasan |
> | :--- | :--- | :--- | :--- |
> | ~~BUG-BE-049~~ | ~~statement-service~~ | ✅ **Fixed** | Removed `@Transactional` from `@Async` method. |
> | ~~BUG-BE-050~~ | ~~statement-service~~ | ✅ **Fixed** | Added S3StorageAdapter for persistent PDF storage (AWS S3/MinIO). Falls back to local for dev. |
> | **BUG-BE-061** | `promotion-service` | ⏭️ **Skip** | Gamification/badge feature opsional, tidak mempengaruhi core banking flow. `getTransactionAmount()` return ZERO hanya berdampak pada badge berbasis amount. |
> | **BUG-BE-064** | `shared/cache-starter` | ⏭️ **Skip** | Stale-while-revalidate tetap return data (meski stale). Tidak ada data loss, hanya delay refresh. Bisa dioptimasi nanti. |
> | **BUG-BE-076** | `api-portal-service` | ⏭️ **Skip** | Sandbox store in-memory hanya untuk developer testing. Partner belum ada, jadi belum relevan. |
> | ~~BUG-BE-078~~ | ~~fx-service~~ | ✅ **Fixed** | Changed `/fx-api/v1` to `/api/v1/fx`. |
> | ~~BUG-BE-079~~ | ~~lending-service~~ | ✅ **Fixed** | Moved financial data from URL params to POST JSON body. |
> | **BUG-BE-080** | `lending-service` | ⏭️ **Skip** | Pre-approval endpoints belum diprioritaskan. Feature belum aktif di frontend. |
> | **BUG-BE-091** | `shared/api-commons` | ⏭️ **Skip** | Fixed-window burst hanya masalah di high traffic. Low-traffic fase awal masih aman. Bisa optimize ke sliding window nanti. |
> | **BUG-BE-092** | `shared/api-commons` | ⏭️ **Skip** | `Thread.sleep()` di webhook retry hanya masalah jika webhook dipakai intensif. Partner belum aktif. |
> | ~~XBUG-001~~ | ~~Statement FE↔BE~~ | ✅ **Fixed** | Changed `StatementStatus` from 'READY' to 'COMPLETED'. |
> | ~~XBUG-005~~ | ~~Statement FE↔BE~~ | ✅ **Fixed** | Added `customerId` to `StatementGenerationRequest`. |


---

## 🐛 Bug Backlog — Batch 1: Core Services (Feb 24, 2026)

> Services: `gateway-service`, `auth-service`, `transaction-service`, `wallet-service`, `lending-service`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-001~~ | ~~gateway-service~~ | ~~AuthorizationFilter.java L154-184~~ | ~~FIXED: JWT validation implemented dengan nimbus-jose-jwt — signature verification (RS256), expiration, issuer, audience validation~~ | ~~Implementasi JWT validation benar via Quarkus OIDC atau nimbus-jose-jwt~~ |
| **BUG-BE-002** | `auth-service` | `KeycloakService.java` L45 + `MFATokenService.java` L17-18 | **In-memory state di scaled environment** — `failedAttempts`, `tokenStore`, `otpStore`, `challengeStore` di `ConcurrentHashMap`. Multi-pod (HPA min 2): state pod A ≠ pod B. | Pindahkan semua state ke Redis via `CacheService`. |
| ✅ ~~BUG-BE-003~~ | ~~transaction-service~~ | ~~InitiateTransferCommandHandler.java L164-166~~ | ~~FIXED: All reference number generators (TXN, QRI, SPL, SCH, BILL, DEP, MF, SELL, PAY, REF) replaced with UUID-based generation across 5 services (transaction, billing, investment, api-portal)~~ | ~~Ganti ke `UUID.randomUUID()`.~~ |
| ✅ ~~BUG-BE-004~~ | ~~wallet-service~~ | ~~WalletService.java L47-54~~ | ~~FIXED: Added `wallet:id:` cache invalidation to all mutation methods (reserveBalance, commitReservation, releaseReservation, credit).~~ | ~~Tambah cache invalidation di semua mutasi.~~ |

---

### 🟠 High Severity — Batch 1

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-005~~ | ~~auth-service~~ | ~~KeycloakService.java L89~~ | ~~FIXED: Removed token plaintext logging. Username masked via `maskUsername()` helper (first 2 + last 2 chars).~~ | ~~Hapus log ini, atau log hanya status.~~ |
| ✅ ~~BUG-BE-006~~ | ~~gateway-service~~ | ~~AuthorizationFilter.java L37~~ | ~~FIXED: Narrowed `/api/v1/accounts` to `/api/v1/accounts/register` only in PUBLIC_ENDPOINTS.~~ | ~~Hapus prefix, ganti exact /register.~~ |
| ✅ ~~BUG-BE-007~~ | ~~transaction-service~~ | ~~InitiateTransferCommandHandler.java L79-81~~ | ~~FIXED: Added processing branches for INTERNAL (immediate commit) and SKN/RTGS (queue for clearing) transfers.~~ | ~~Tambahkan processing branch per transfer type.~~ |
| ✅ ~~BUG-BE-008~~ | ~~wallet-service~~ | ~~WalletService.java L162-163~~ | ~~FIXED: LedgerEntry and related entities now use `String accountId`. Prevented UUID parsing exceptions.~~ | ~~Standardisasi: pilih satu, `accountId` selalu UUID atau selalu String.~~ |
| ✅ ~~BUG-BE-009~~ | ~~lending-service~~ | ~~LoanManagementService.java L103-130~~ | ~~FIXED: Last installment amount now uses actual remaining principal + interest instead of the standard monthly rate.~~ | ~~Pada last installment: `installmentAmount = outstandingPrincipal + interestAmount`.~~ |
| ✅ ~~BUG-BE-010~~ | ~~auth-service~~ | ~~KeycloakService.java L199-215~~ | ~~FIXED: Replaced `Mono.block()` with synchronous `RestTemplate` for blocking login/refresh/validate methods to avoid Tomcat thread starvation.~~ | ~~Ganti ke synchronous `RestTemplate` atau migrasi ke WebFlux.~~ |
| ✅ ~~BUG-BE-011~~ | ~~transaction-service~~ | ~~ScheduledTransferScheduler.java L22~~ | ~~FIXED: Added Redis-based distributed lock (`stringRedisTemplate.opsForValue().setIfAbsent`) for `processDueScheduledTransfers` to handle multi-pod.~~ | ~~Tambahkan distributed lock via Redis (ShedLock atau custom).~~ |

---

### 🟡 Low Severity — Batch 1

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-012~~ | ~~promotion-service~~ | ~~FIXED: Replaced `Math.random()` with `SecureRandom` for referral code generation.~~ | ~~Gunakan `SecureRandom`.~~ |
| ✅ ~~BUG-BE-013~~ | ~~wallet-service~~ | ~~FIXED: Reused first `findByAccountId` result instead of querying DB twice.~~ | ~~Gunakan result dari cek pertama.~~ |
| ✅ ~~BUG-BE-014~~ | ~~lending-service~~ | ~~FIXED: Added `@Transactional` to `processRepayment` method.~~ | ~~Tambahkan `@Transactional`.~~ |
| ✅ ~~BUG-BE-015~~ | ~~transaction-service~~ | ~~FIXED: Added `PaginationInfo` (page, size, totalElements, totalPages) to `getAccountTransactions()` response. Also created `TransactionResponse` DTO (BUG-BE-135).~~ | ~~Implementasi `Page<Transaction>` return.~~ |
| ✅ ~~BUG-BE-016~~ | ~~auth-service~~ | ~~FIXED: Username masked in all log statements via `maskUsername()` — shows only first 2 + last 2 chars.~~ | ~~Mask or hash username in logs.~~ |
| ✅ ~~BUG-BE-017~~ | ~~gateway-service~~ | ~~FIXED: Authorization header no longer logged. Downgraded to DEBUG with only `hasAuth` boolean.~~ | ~~Remove or downgrade to DEBUG.~~ |

---

### 🟠 Medium Severity — Frontend-Backend Mismatch Batch 1

| ID | Area | Mismatch | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-CROSS-001~~ | ~~Auth~~ | ~~FIXED: BFF already maps `expires_in` → `expiresIn`. Updated refresh route to also read `expires_in` from Keycloak response. Updated `LoginResponse` type to camelCase.~~ | ~~Pastikan BFF mapping `expires_in` → `expiresIn`.~~ |
| ✅ ~~BUG-CROSS-002~~ | ~~Transaction~~ | ~~FIXED: Added `validateUUID`/`assertUUID` to `validation.ts`. TransactionService now validates accountId format before sending to backend.~~ | ~~Validasi UUID format di frontend.~~ |
| ✅ ~~BUG-CROSS-003~~ | ~~Wallet~~ | ~~FIXED: Added Axios response interceptor in `api.ts` to auto-unwrap `ApiResponse<>` wrapper (`{ success, data }` → `data`).~~ | ~~Terapkan Axios interceptor auto-unwrap atau konsisten `response.data.data`.~~ |
| ✅ ~~BUG-CROSS-004~~ | ~~Transfer~~ | ~~FIXED: Added `QRIS_PAYMENT`, `BILL_PAYMENT`, `TOP_UP` to `InitiateTransferRequest.TransactionType` enum to match `Transaction.TransactionType`.~~ | ~~Sinkronkan TransactionType enum.~~ |
| ✅ ~~BUG-CROSS-005~~ | ~~Auth~~ | ~~FIXED: Login route reads `expires_in` from Keycloak response. Refresh route also updated to read `expires_in` instead of hardcoded 900s.~~ | ~~Baca `expiresIn` dari Keycloak response, jangan hardcode.~~ |
| **BUG-CROSS-006** | Biometric | Frontend tidak punya `BiometricService.ts` padahal backend punya endpoint lengkap. | Implementasi `BiometricService.ts` atau hapus backend endpoint. |

---

## 🐛 Bug Backlog — Batch 2: Extended Services (Feb 24, 2026)

> Services: `account-service`, `investment-service`, `fx-service`, `notification-service`, `shared/security-starter`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-018~~ | ~~investment-service~~ | ~~WalletServiceAdapter.java L29-31~~ | ~~FIXED: Rewrote WalletServiceAdapter to use wallet-service's actual API: deductBalance→reserve+commit, creditBalance→/credit, hasSufficientBalance→/balance (reads 'availableBalance'). Also fixes BUG-BE-029.~~ | ~~Sesuaikan dengan flow reserve-commit yang ada di `wallet-service`.~~ |
| ✅ ~~BUG-BE-019~~ | ~~shared/security-starter~~ | ~~EncryptionService.java L263~~ | ~~FIXED: PBKDF2 salt now configurable via `payu.security.encryption.salt` property. Default fallback preserved for backward compat.~~ | ~~Jadikan configurable via env var.~~ |
| ✅ ~~BUG-BE-020~~ | ~~account-service~~ | ~~UserApplicationService.java L35-36~~ | ~~FIXED: Removed `@Async` from registerUser to ensure DB ops run synchronously within the transaction.~~ | ~~Pisahkan: sync untuk DB ops, async hanya untuk event publishing.~~ |

---

### 🟠 High Severity — Batch 2

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-021~~ | ~~investment-service~~ | ~~InvestmentApplicationService.java L115~~ | ~~FIXED: Added saga compensation — if saveDeposit fails after wallet deduction, creditBalance() rollback is triggered. Logs CRITICAL if rollback also fails for manual intervention.~~ | ~~Implementasikan saga: jika save gagal, `creditBalance()` rollback.~~ |
| ✅ ~~BUG-BE-022~~ | ~~investment-service~~ | ~~Multiple files~~ | ~~FIXED: Reference numbers (DEP, MF, SELL) replaced with UUID-based generation.~~ | ~~Ganti ke UUID-based.~~ |
| ✅ ~~BUG-BE-023~~ | ~~fx-service~~ | ~~FxRateService.java L59-61~~ | ~~FIXED: Caught exception per-currency to continue updating other rates even if one fails.~~ | ~~Catch exception per-currency, lanjutkan ke berikutnya.~~ |
| ✅ ~~BUG-BE-024~~ | ~~fx-service~~ | ~~FIXED: Added `WalletServicePort` + `WalletServiceAdapter` for wallet integration. `createConversion()` now debits source currency and credits target currency with saga compensation on failure.~~ | ~~Integrasikan dengan wallet reservation flow.~~ |
| ✅ ~~BUG-BE-025~~ | ~~notification-service~~ | ~~NotificationService.java L75~~ | ~~FIXED: Added retry scheduling logic with exponential backoff and a scheduled job to process pending retries.~~ | ~~Implementasi retry scheduler untuk FAILED notifications.~~ |
| **BUG-BE-026** | `notification-service` | `SmsSender.java` L16-29 | **SMS sender adalah mock** — OTP tidak pernah terkirim ke user. | Integrasikan Twilio/Vonage atau provider SMS lokal. |
| ✅ ~~BUG-BE-027~~ | ~~account-service~~ | ~~UserApplicationService.java L64~~ | ~~FIXED: User status now set based on KYC result — ACTIVE if approved, PENDING_VERIFICATION if rejected. Previously always ACTIVE.~~ | ~~Jika `kycStatus == REJECTED`, set `status = PENDING_VERIFICATION`.~~ |

---

### 🟠 Medium Severity — Batch 2

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-028~~ | ~~investment-service~~ | ~~FIXED: BUY fee now uses `managementFee` instead of `redemptionFee`. Redemption fee is only for SELL.~~ | ~~Gunakan `subscriptionFee` saat BUY.~~ |
| ✅ ~~BUG-BE-029~~ | ~~investment-service~~ | ~~FIXED (prior session): `hasSufficientBalance()` reads `availableBalance` from wallet response.~~ | ~~Baca `"availableBalance"` dari response.~~ |
| ✅ ~~BUG-BE-030~~ | ~~shared/security-starter~~ | ~~FIXED: Narrowed `DataMaskingAspect` pointcut from `execution(* id.payu..service..*(..))` to `@annotation(Audited)` only. Eliminates masking overhead on every service method call.~~ | ~~Batasi ke method dengan `@Audited` annotation.~~ |
| ✅ ~~BUG-BE-031~~ | ~~account-service~~ | ~~FIXED: Added `DataIntegrityViolationException` catch for race condition on concurrent registration with same email/username.~~ | ~~Tangkap `DataIntegrityViolationException` → return 409.~~ |
| ✅ ~~BUG-BE-032~~ | ~~fx-service~~ | ~~FIXED: FX conversion now calculates 0.5% fee instead of always ZERO. Frontend fee display now matches backend.~~ | ~~Implementasikan fee calculation.~~ |

---

## 🐛 Bug Backlog — Batch 3: Ops & Infra Services (Feb 24, 2026)

> Services: `billing-service`, `support-service`, `partner-service`, `backoffice-service`, `kyc-service`, `analytics-service`, `shared/outbox-starter`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-033~~ | ~~backoffice-service~~ | ~~SecurityConfig.java L46~~ | ~~FIXED: CORS origins restricted to `backoffice.payu.id`, `backoffice.payu.co.id`, `admin.payu.id`. Headers restricted. AllowCredentials enabled.~~ | ~~Ganti dengan specific origins.~~ |
| ✅ ~~BUG-BE-034~~ | ~~support-service~~ | ~~Seluruh controller~~ | ~~FIXED: Added `@PreAuthorize("hasRole('SUPPORT_MANAGER')")` to all write endpoints (createAgent, updateAgentStatus, createModule, updateModuleStatus, assignTraining).~~ | ~~Tambahkan role-based authorization.~~ |
| ✅ ~~BUG-BE-035~~ | ~~partner-service~~ | ~~SnapBiTokenService.java L31~~ | ~~FIXED: Partner token store moved to Redis with TTL. Token now shared across pods.~~ | ~~Pindahkan `tokenStore` ke Redis dengan TTL.~~ |
| ✅ ~~BUG-BE-036~~ | ~~partner-service~~ | ~~SnapBiTokenService.java L115~~ | ~~FIXED: Cleanup scheduler added with `@Scheduled(fixedRate = 60000)`.~~ | ~~Tambahkan `@Scheduled(fixedRate = 60000)`.~~ |

---

### 🟠 High Severity — Batch 3

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-037** | `billing-service` | `PaymentService.java` L69 | **Biller processing adalah mock** — selalu set `COMPLETED` tanpa panggil biller API. Balance terpotong, tagihan tidak dibayar. | Implementasi adapter per-biller (PLN, PDAM, dll.) atau set `PROCESSING` + callback. |
| ✅ ~~BUG-BE-038~~ | ~~billing-service~~ | ~~BillPayment.java L85~~ | ~~FIXED: Reference number + biller transaction IDs replaced with UUID-based generation.~~ | ~~Ganti ke UUID-based.~~ |
| ✅ ~~BUG-BE-039~~ | ~~billing-service~~ | ~~PaymentService.java L61-78~~ | ~~FIXED: Added `commitReservation()` after biller success and `releaseReservation()` in catch block. Prevents permanent fund lock.~~ | ~~Setelah biller sukses: `walletPort.commitReservation(reservationId)`. Jika gagal: `releaseReservation()`.~~ |
| ✅ **BUG-BE-040** | `backoffice-service` | `UniversalSearchService.java` L26-55 | **Search load semua hasil ke memory** — fetch ALL records, paginate di Java `subList()`. OOM risk untuk data besar. | Implementasi pagination di repository dengan `Pageable`. |
| ✅ ~~BUG-BE-041~~ | ~~partner-service~~ | ~~SnapBiSignatureService.java L19-22~~ | ~~FIXED: `generateSignature` now hashes body with SHA-256 hex before signing per SNAP-BI spec: `method + ":" + endpoint + ":" + accessToken + ":" + sha256hex(body) + ":" + timestamp`.~~ | ~~Ikuti spesifikasi SNAP-BI: `method + ":" + sha256hex(body) + ":" + timestamp`.~~ |
| ✅ ~~BUG-BE-042~~ | ~~outbox-starter~~ | ~~OutboxPublisher.java L192~~ | ~~FIXED: Replaced `whenComplete` with `handle()` that wraps errors in `CompletionException`, which is properly thrown by `future.get()`.~~ | ~~Gunakan `exceptionally()` atau flag ke outer try-catch.~~ |

---

### 🟠 Medium Severity — Batch 3

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-043~~ | ~~backoffice-service~~ | ~~FIXED: All 3 `listByStatus()` methods (Fraud, KYC, CustomerCase) now use `PageRequest.of(page, size)` for DB-level pagination.~~ | ~~Gunakan `repository.findByStatus(status, PageRequest.of(page, size))`.~~ |
| ✅ ~~BUG-BE-044~~ | ~~partner-service~~ | ~~FIXED: Replaced thread-unsafe `SimpleDateFormat` with immutable `DateTimeFormatter` in `getCurrentTimestamp()`.~~ | ~~Ganti ke `DateTimeFormatter` (thread-safe).~~ |
| ✅ ~~BUG-BE-045~~ | ~~billing-service~~ | ~~FIXED: Added `commitReservation()` + `releaseReservation()` to `WalletPort` interface and `WalletAdapter` implementation.~~ | ~~Tambahkan `commitReservation` + `releaseReservation` ke `WalletPort`.~~ |
| ✅ ~~BUG-BE-046~~ | ~~outbox-starter~~ | ~~FIXED: Replaced `new ObjectMapper().findAndRegisterModules()` with constructor-injected Spring-managed `ObjectMapper` bean.~~ | ~~Inject `ObjectMapper` sebagai bean.~~ |
| ✅ ~~BUG-BE-047~~ | ~~partner-service~~ | ~~FIXED: Added `@Scheduled(cron = "0 0 8 * * *")` trigger to auto-rotate expiring certificates daily at 8 AM.~~ | ~~Tambahkan `@Scheduled(cron = "0 0 8 * * *")`.~~ |
| ✅ ~~BUG-BE-048~~ | ~~kyc-service + analytics-service~~ | ~~FIXED: CORS origins now environment-aware. Localhost only in `ENVIRONMENT=development`, production defaults to payu.id domains only.~~ | ~~Pisahkan CORS config berdasarkan `ENVIRONMENT` env var.~~ |

---

## 🐛 Bug Backlog — Batch 4: Statement, CMS, A/B Testing (Feb 24, 2026)

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-049~~ | ~~statement-service~~ | ~~StatementService.java L69-70~~ | ~~FIXED: Removed `@Transactional` from `@Async` method. Each repository.save() runs in its own implicit transaction.~~ | ~~Pisahkan inner `@Transactional` untuk DB ops dari `@Async` outer.~~ |
| ✅ ~~BUG-BE-050~~ | ~~statement-service~~ | ~~StatementService.java L54~~ | ~~FIXED: Added S3StorageAdapter with AWS S3/MinIO support. storePdf() uses S3 in production, falls back to local /tmp for dev.~~ | ~~Ganti ke persistent volume atau upload ke object storage (S3/MinIO).~~ |

---

### 🟠 High Severity — Batch 4

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-051** | `statement-service` | `WalletServiceClient.java` L31-39 | **`getBalanceAtDate()` return saldo SAAT INI**, bukan historis. Statement opening/closing balance selalu sama (saldo terkini). | Implementasikan balance history endpoint di wallet-service. |
| ✅ ~~BUG-BE-052~~ | ~~statement-service~~ | ~~TransactionServiceClient.java L24-25~~ | ~~FIXED: Replaced `new RestTemplate()` with Spring-injected `RestTemplate` bean so timeout/resilience config applies.~~ | ~~Inject `RestTemplate` via Spring.~~ |
| ✅ ~~BUG-BE-053~~ | ~~statement-service~~ | ~~TransactionServiceClient.java L49-52~~ | ~~FIXED: Fetch exceptions now propagated as `RuntimeException` with proper error message instead of silently returning empty list.~~ | ~~Minimal log error, throw exception agar statement gagal tegas.~~ |
| ✅ ~~BUG-BE-054~~ | ~~statement-service~~ | ~~FIXED: Removed 20-transaction hard cap. PDF now renders as many transactions as fit on page, with clear overflow message instead of silent truncation.~~ | ~~Implementasi multi-page PDF.~~ |
| ✅ ~~BUG-BE-055~~ | ~~ab-testing-service~~ | ~~FIXED: Removed `@CacheEvict` from `trackConversion()`. Conversion events fire thousands/min and were causing constant experiment cache invalidation and DB re-fetches.~~ | ~~Pisahkan metrics update, jangan evict experiment cache.~~ |
| ✅ ~~BUG-BE-056~~ | ~~ab-testing-service~~ | ~~FIXED: `trackConversion()` now uses `findByIdWithLock()` with `PESSIMISTIC_WRITE` lock for safe read-modify-write on metrics. Prevents lost updates under concurrency.~~ | ~~Pessimistic lock pada metrics update.~~ |
| ✅ ~~BUG-BE-057~~ | ~~cms-service~~ | ~~FIXED: Added `DataIntegrityViolationException` handler for concurrent title conflict. DB unique constraint catches the race, returns proper conflict error.~~ | ~~`UNIQUE` constraint di DB + tangkap `DataIntegrityViolationException` → 409.~~ |

---

### 🟠 Medium Severity — Batch 4

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-058~~ | ~~cms-service~~ | ~~FIXED: Added pageable `findByContentType()` to ContentRepository for DB-level pagination.~~ | ~~Tambahkan pageable parameter atau limit.~~ |
| ✅ ~~BUG-BE-059~~ | ~~statement-service~~ | ~~FIXED: Removed `readOnly=true` from `getStatement()` since it calls `recordAccess()` + `save()` which require write access.~~ | ~~Hapus `readOnly=true` atau pisahkan `recordAccess()`.~~ |

---

## 🐛 Bug Backlog — Batch 5: Promotion & Shared (Feb 24, 2026)

> Services: `promotion-service`, `compliance-service`, `shared/saga-starter`, `shared/cache-starter`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-060~~ | ~~promotion-service~~ | ~~LoyaltyPointsService.java L124-130~~ | ~~FIXED: Uses pg_advisory_xact_lock for atomic balance calculation. Prevents lost updates from concurrent writes.~~ | ~~`SELECT FOR UPDATE` atau atomic balance column di table terpisah.~~ |
| **BUG-BE-061** | `promotion-service` | `GamificationService.java` L442-444 | **`getTransactionAmount()` selalu return `ZERO`** — badge berbasis total amount tidak bisa diraih. | Inject `TransactionServiceClient` dan query jumlah transaksi real. |
| ✅ ~~BUG-BE-062~~ | ~~promotion-service~~ | ~~CashbackService.java L56~~ | ~~FIXED: CashbackSagaOrchestrator credits wallet via WalletServicePort before recording cashback. Compensation on failure.~~ | ~~Panggil wallet-service credit sebelum set `CREDITED`. Wrap dengan saga.~~ |
| ✅ ~~BUG-BE-063~~ | ~~promotion-service~~ | ~~PromotionService.java L148-149~~ | ~~FIXED: Replaced read-check-write with atomic `atomicIncrementRedemptionCount()` using UPDATE...WHERE count < max.~~ | ~~Optimistic locking `@Version` atau atomic: `UPDATE SET count = count + 1 WHERE count < max`.~~ |
| ✅ ~~BUG-BE-064~~ | ~~shared/cache-starter~~ | ~~FIXED: Added async background refresh via `CompletableFuture.runAsync()` with dedicated `REFRESH_EXECUTOR` when cache entry is stale. Data returned immediately, refresh happens in background.~~ | ~~Inject executor + `CompletableFuture.runAsync`.~~ |

---

### 🟠 High Severity — Batch 5

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-065~~ | ~~promotion-service~~ | ~~LoyaltyPointsService.java L100-121~~ | ~~FIXED: Replaced `.count()` with `.mapToInt(LoyaltyPoints::getPoints).sum()` for totalEarned, totalRedeemed, expiredPoints. Balance was showing count of records instead of actual point values.~~ | ~~Ganti ke `.mapToInt(LoyaltyPoints::getPoints).sum()`.~~ |
| ✅ ~~BUG-BE-066~~ | ~~promotion-service~~ | ~~FIXED: Replaced O(n) in-memory scan with targeted `existsByAccountIdAndTransactionId()` JPA query for idempotency check.~~ | ~~Tambahkan `UNIQUE INDEX (account_id, transaction_id)` di DB.~~ |
| ✅ **BUG-BE-067** | `promotion-service` | `GamificationService.java` L374-427 | **N+1 query** — `badgeRepository.findById()` di-call per badge dalam loop. 50 badge = 50 queries. | Gunakan `findAllById(ids)` (1 query) + Map lookup. |
| ✅ ~~BUG-BE-068~~ | ~~shared/saga-starter~~ | ~~SagaOrchestrator.java L154-156~~ | ~~FIXED: `executeAsync()` now uses dedicated `Executors.newCachedThreadPool` (saga-worker threads) instead of `ForkJoinPool.commonPool()`.~~ | ~~Inject custom `TaskExecutor` dan gunakan di `supplyAsync(..., customExecutor)`.~~ |
| ✅ ~~BUG-BE-069~~ | ~~shared/saga-starter~~ | ~~SagaOrchestrator.java L277-283~~ | ~~FIXED: `Thread.sleep()` replaced with `ScheduledExecutorService.schedule()` for non-blocking exponential backoff. Retries run on saga-retry daemon threads, not Tomcat pool.~~ | ~~Gunakan `ScheduledExecutorService` atau Spring `TaskScheduler`.~~ |
| ✅ ~~BUG-BE-070~~ | ~~compliance-service~~ | ~~SecurityConfig.java~~ | ~~FIXED: Added `@EnableMethodSecurity` + `hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')` matcher on `/api/v1/compliance/**`. Controller also has `@PreAuthorize` on all endpoints.~~ | ~~`@PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")` pada semua endpoints.~~ |

---

### 🟠 Medium Severity — Batch 5

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-071~~ | ~~promotion-service~~ | ~~FIXED: Added `DataIntegrityViolationException` catch in `getOrCreateUserLevel()`. On race condition, retries `findByAccountId()` instead of crashing.~~ | ~~`INSERT ... ON CONFLICT DO NOTHING` atau retry on `DataIntegrityViolationException`.~~ |
| ✅ ~~BUG-BE-072~~ | ~~promotion-service~~ | ~~FIXED: `getTotalCheckins()` now uses `countByAccountId()` DB query instead of fetching all data to memory and calling `.count()`.~~ | ~~Tambahkan `countByAccountId(String accountId)` di repository.~~ |
| ✅ ~~BUG-BE-073~~ | ~~promotion-service~~ | ~~FIXED: Changed LOG.warn to LOG.error with full stack trace. Added MeterRegistry counter `promotion.kafka.publish.failure` with service/topic tags for alerting.~~ | ~~Metric counter + alert.~~ |
| ✅ ~~BUG-BE-074~~ | ~~shared/cache-starter~~ | ~~FIXED: Rewrote `DistributedCacheService` to use `ObjectMapper.convertValue()` for type-safe deserialization. Added `convertToCacheEntry()` and `convertToType()` helpers. Compatible with Redis and Red Hat Data Grid (RESP mode).~~ | ~~Tambahkan type safety check.~~ |
| ✅ ~~BUG-BE-075~~ | ~~promotion-service~~ | ~~FIXED: Replaced deprecated `BigDecimal.ROUND_HALF_UP` constant with `RoundingMode.HALF_UP` enum across 3 files (CashbackSagaOrchestrator, CashbackSagaContext, PromotionService).~~ | ~~Ganti ke `RoundingMode.HALF_UP`.~~ |

---

## 🐛 Bug Backlog — Batch 6: API Portal, Lending, Compliance, FX (Feb 24, 2026)

> Services: `api-portal-service`, `lending-service`, `compliance-service`, `fx-service`, `shared/api-commons`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-076** | `api-portal-service` | `SandboxService.java` L28-29 | **Sandbox store in-memory** — `paymentStore` + `refundStore` hilang saat pod restart. Tim dev TokoBapak/Nobar kehilangan test data. | Gunakan Redis atau database. |
| ✅ ~~BUG-BE-077~~ | ~~api-portal-service~~ | ~~SandboxService.java L34~~ | ~~FIXED: Reference numbers (PAY, REF) replaced with UUID-based generation.~~ | ~~Ganti ke `UUID.randomUUID()`.~~ |
| ✅ ~~BUG-BE-078~~ | ~~fx-service~~ | ~~FxService.ts L138~~ | ~~FIXED: Changed baseUrl from `/fx-api/v1` to `/api/v1/fx` to match standard BFF routing pattern.~~ | ~~Unify ke `/api/fx/v1`, update BFF routing.~~ |
| ✅ ~~BUG-BE-079~~ | ~~lending-service~~ | ~~LendingService.ts L130-134~~ | ~~FIXED: Moved `amount`, `merchantName` from URL query params to POST JSON body to prevent access log exposure.~~ | ~~Ubah ke POST JSON body.~~ |
| **BUG-BE-080** | `lending-service` | `LendingService.ts` L161-173 | **Pre-approval endpoints ada di frontend, tidak ada di backend** — 404. | Expose di `LendingController.java` atau hapus dari `LendingService.ts`. |
| ✅ ~~BUG-BE-090~~ | ~~shared/api-commons~~ | ~~FIXED (prior): `RateLimitAspect` already uses atomic Lua script for `INCR` + `EXPIRE`. No race condition possible.~~ | ~~Gunakan Redis Lua script untuk atomic increment+expire.~~ |
| **BUG-BE-091** | `shared/api-commons` | `RateLimitAspect.java` L69 | **Fixed-window rate limit mudah di-burst** — 59 req/menit di detik 59 + 59 req di detik 0 next = 118 req dalam 2 detik. | Gunakan sliding window atau Token Bucket. |
| ✅ ~~BUG-BE-092~~ | ~~shared/api-commons~~ | ~~FIXED: Replaced `Thread.sleep()` in `@Async` retry with `ScheduledExecutorService.schedule()` for non-blocking exponential backoff. Prevents exhaustion of async thread pool.~~ | ~~Gunakan `ScheduledExecutorService.schedule()` non-blocking.~~ |

---

### 🟠 High Severity — Batch 6

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-081~~ | ~~compliance-service~~ | ~~FIXED: Removed DELETE /gdpr-audit/{auditId} endpoint. Audit logs are immutable. Replaced with comment about soft-delete with approval workflow.~~ | ~~Hapus endpoint DELETE.~~ |
| ✅ ~~BUG-BE-082~~ | ~~api-portal-service~~ | ~~FIXED: `getPaymentStatus()` and `createRefund()` now throw `jakarta.ws.rs.NotFoundException` instead of returning null. Proper 404 response.~~ | ~~Throw NotFoundException → 404.~~ |
| ✅ ~~BUG-BE-083~~ | ~~compliance-service~~ | ~~FIXED: Aligned frontend `ComplianceService.ts` interfaces to match backend `AuditReportResponse` DTO (transactionId, merchantId, standard, checks[], overallStatus). See XBUG-083.~~ | ~~Sinkronkan DTO.~~ |
| ✅ ~~BUG-BE-084~~ | ~~fx-service~~ | ~~VERIFIED: `/conversions/estimate` endpoint already exists in FxController (POST mapping).~~ | ~~Endpoint sudah ada.~~ |
| ✅ ~~BUG-BE-085~~ | ~~lending-service~~ | ~~FIXED: `processRepayment()` now accepts amount via `@RequestBody` JSON instead of `@RequestParam` query string. Financial amounts in URLs are exposed in logs.~~ | ~~Ganti ke JSON body.~~ |

---

### 🟠 Medium Severity — Batch 6

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-086~~ | ~~FxService.ts~~ | ~~FIXED: Deduplicated `FxConversionRequest` as type alias for `ConvertCurrencyRequest`.~~ | ~~Hapus duplikasi.~~ |
| ✅ ~~BUG-BE-087~~ | ~~FxService.ts~~ | ~~FIXED: Deduplicated `FxRateResponse` as type alias for `FxRate`, `FxConversionResponse` as alias for `FxConversion`.~~ | ~~Hapus duplikasi.~~ |
| ✅ ~~BUG-BE-088~~ | ~~api-portal-service~~ | ~~FIXED: Added per-service error handling in `refreshCache()`. Failed services tracked in list and logged as warning with partial result count.~~ | ~~Timeout per-service + partial result.~~ |
| ✅ ~~BUG-BE-089~~ | ~~compliance-service~~ | ~~VERIFIED: All compliance endpoints already have `@PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")` — not accessible to regular users.~~ | ~~Already secured.~~ |

---

## 🐛 Frontend Bug Backlog (Feb 24, 2026)

---

### 🔴 High Severity

| ID | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-FE-001~~ | ~~api/v1/[...path]/route.ts~~ | ~~FIXED: BFF proxy now auto-retries on 401 — refreshes token via `/api/auth/refresh` then retries upstream with new Bearer token.~~ | ~~BFF auto-retry on 401.~~ |
| ✅ ~~BUG-FE-002~~ | ~~uiStore.ts~~ | ~~FIXED: Toast setTimeout IDs stored in Map, cleared on removeToast/clearToasts. No more memory leaks.~~ | ~~Simpan timeout ID per toast, clear di `removeToast`.~~ |
| ✅ ~~BUG-FE-003~~ | ~~uiStore.ts~~ | ~~FIXED: Replaced incrementing counter with `crypto.randomUUID()` for deterministic test behavior.~~ | ~~Ganti ke `crypto.randomUUID()`.~~ |
| ✅ ~~BUG-FE-004~~ | ~~FIXED: WebSocket: exponential backoff + max retries + fresh connect()~~ |
| ✅ ~~BUG-FE-020~~ | ~~ABTestingService.ts~~ | ~~FIXED: Added in-memory `Map<string, CachedVariantAssignment>` fallback when localStorage fails. Both getCachedVariant and cacheVariantAssignment use fallback.~~ | ~~Memory fallback.~~ |
| ✅ ~~BUG-FE-021~~ | `services/` (semua) | **Tidak ada idempotency key** pada operasi finansial — double-tap = double-charge. | Tambahkan `X-Idempotency-Key: uuid-v4` di semua financial mutations. |
| ✅ ~~BUG-FE-022~~ | ~~exchange/page.tsx~~ | ~~FIXED: Used `useRef` for `estimateMutation` to avoid infinite re-render loop. useEffect now uses `estimateMutationRef.current`.~~ | ~~UseRef untuk stable reference.~~ |

---

### 🟠 Medium Severity

| ID | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-FE-005~~ | ~~FIXED: ws uses getter: get ws() returns wsRef.current~~ |
| ✅ ~~BUG-FE-006~~ | ~~FIXED: Guard: enabled=!!accountId prevents WS when falsy~~ |
| ✅ ~~BUG-FE-007~~ | ~~transactionStore.ts~~ | ~~FIXED: `setDetailOpen(false)` now sets `selectedTransactionId` to `null` (not `undefined`) matching type.~~ | ~~Ganti `undefined` dengan `null`.~~ |
| ✅ ~~BUG-FE-008~~ | ~~FIXED: Phone 6208 normalization correct: 0+substring(3) yields valid 08xxx~~ |
| ✅ ~~BUG-FE-009~~ | ~~lib/validation.ts~~ | ~~FIXED: Password strength calculated from boolean checks (hasLower, hasUpper, etc.) instead of filtering Indonesian error strings.~~ | ~~Hitung dari boolean checks langsung.~~ |
| ✅ ~~BUG-FE-010~~ | ~~lib/currency.ts~~ | ~~FIXED: Email domain typo detection no longer blocks valid `.co` domains. Changed from hard-block (`isValid: false`) to suggestion-only (`isValid: true` with `suggestion` field).~~ | ~~Only suggest, don't block.~~ |
| ✅ ~~BUG-FE-011~~ | ~~lib/date.ts~~ | ~~FIXED: `diffMonths` now uses proper calendar month math `(year*12+month)` instead of `Math.floor(days/30)`.~~ | ~~Gunakan selisih `.getMonth()`.~~ |
| ✅ ~~BUG-FE-012~~ | ~~lib/currency.ts~~ | ~~FIXED: Added `scaleIndex >= scales.length` guard in `numberToWords` to prevent `undefined` in output for amounts > triliun.~~ | ~~Guard: `if (scaleIndex >= scales.length)`.~~ |
| ✅ ~~BUG-FE-013~~ | ~~useTransactions.ts~~ | ~~FIXED: Added scoping comment. React Query prefix match `['transactions']` already invalidates all account-specific queries correctly.~~ | ~~QueryKey scoping documented.~~ |
| ✅ ~~BUG-FE-014~~ | ~~logout/route.ts~~ | ~~FIXED: Added `await` with 2s AbortController timeout to backend logout fetch. Session invalidated in Keycloak before cookies cleared.~~ | ~~Tambahkan `await` dengan timeout max 2 detik.~~ |
| ✅ ~~BUG-FE-023~~ | ~~rewards/page.tsx~~ | ~~FIXED: Replaced ALL hardcoded fake data (9300 poin, PAYU2024, etc.) with 0/empty defaults and empty arrays.~~ | ~~Empty state, bukan fake data.~~ |
| ✅ ~~BUG-FE-024~~ | ~~types/index.ts~~ | ~~FIXED: Removed `access_token` and `refresh_token` from `LoginResponse`. Tokens are httpOnly cookies via BFF. Added `mfa_required` fields instead.~~ | ~~Hapus field token dari tipe `LoginResponse`.~~ |
| ✅ ~~BUG-FE-025~~ | ~~AccountService.ts~~ | ~~FIXED: Removed deprecated `getUserFromStorage()` and `getCurrentUser()` methods entirely. Comment directs to useAuthStore hook.~~ | ~~Removed deprecated methods.~~ |
| ✅ ~~BUG-FE-026~~ | ~~providers.tsx~~ | ~~FIXED: Set `refetchOnWindowFocus: true` and `refetchOnReconnect: true` globally for fresh financial data on tab return.~~ | ~~Set global ke `true`.~~ |
| ✅ ~~BUG-FE-027~~ | ~~providers.tsx~~ | ~~FIXED (prior): Global mutation `retry: 0` already configured to prevent double-debit on auto-retry.~~ | ~~Set `retry: 0` global.~~ |
| ✅ ~~BUG-FE-028~~ | ~~useExperiment.ts~~ | ~~FIXED: Added `contextRef`, `onVariantAssignedRef`, `onErrorRef` with sync effects. UseEffect deps no longer include callback objects, preventing infinite re-renders.~~ | ~~UseRef untuk callbacks.~~ |
| ✅ ~~BUG-FE-029~~ | ~~useExperiment.ts~~ | ~~FIXED: Deps simplified to `[assignment, experimentKey]` and `[isError, error]` to prevent re-render loops.~~ | ~~Split kondisi.~~ |
| ✅ ~~BUG-FE-030~~ | ~~KYCService.ts~~ | ~~FIXED: Added `validateImageSize()` with 7MB max limit. Applied to uploadKtp and uploadSelfie before API calls.~~ | ~~Image size validation.~~ |
| ✅ ~~BUG-FE-031~~ | ~~ABTestingService.ts~~ | ~~FIXED: Memory cache fallback prevents infinite re-fetch when localStorage fails. Warning logged on localStorage failure.~~ | ~~Fallback flag.~~ |
| ✅ ~~BUG-FE-032~~ | ~~WalletService.ts + PartnerService.ts~~ | ~~FIXED: Removed `clientSecret` from Partner interface (BUG-FE-032). Added `PartnerWithCredentials` extending Partner for registration response only. Removed clientSecret reference from merchant page.~~ | ~~Security: clientSecret removed from FE.~~ |

---

### 🟡 Low Severity

| ID | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-FE-015~~ | ~~FIXED: Math.max(0, newUnreadCount) prevents negative~~ |
| ✅ ~~BUG-FE-016~~ | ~~FIXED: 503 returns error:true, _fallback:true not fake success~~ |
| ✅ ~~BUG-FE-017~~ | ~~FIXED: startOfDay creates new Date(date) copy, no mutation~~ |
| ✅ ~~BUG-FE-018~~ | ~~FIXED: No console.log in production, onOpen dispatches to callback~~ |
| ✅ **BUG-FE-019** | `lib/validation.ts` L471 | Regex nama tidak support Unicode — nama Indonesia/asing dengan aksen ditolak. | Ganti ke `/^[\p{L}\s\.\,\-\']+$/u`. |

---

## 🐛 Cross-Service Mismatch Backlog

---

### 🔴 Critical Mismatches

| ID | Frontend | Backend | Mismatch |
| :--- | :--- | :--- | :--- |
| ✅ ~~XBUG-001~~ | ~~`StatementStatus = 'READY'`~~ | ~~Backend return `COMPLETED`~~ | ~~FIXED: Changed frontend StatementStatus from 'READY' to 'COMPLETED'. Polling, display labels, and badge colors updated.~~ |
| ✅ **XBUG-002** | `PaymentStatus` tidak punya `PROCESSING`/`REFUNDED` | Backend punya | Status baru dari backend ditampilkan sebagai blank |
| ✅ **XBUG-003** | `Experiment` punya `variants: Variant[]` array | Backend hanya punya `variantAConfig`/`variantBConfig` | Struktur sama sekali berbeda → deserialisasi gagal |
| ⚠️ **XBUG-004** | Frontend punya 15+ method scheduled-transfers dan split-bills | Backend endpoint sudah ada (`ScheduledTransferController`, `SplitBillController`) | **FEATURE GAP**: FE methods exist but may call wrong paths — verify API alignment |
| ✅ ~~XBUG-005~~ | ~~`POST /statements/generate` tidak kirim `customerId`~~ | ~~Backend butuh `customerId` untuk security check~~ | ~~FIXED: Added `customerId` to `StatementGenerationRequest` interface.~~ |
| ✅ **XBUG-011** | `RewardType = 'LOYALTY_POINTS' \| 'CASHBACK' \| 'VOUCHER'` | Backend: `PERCENTAGE \| FIXED_AMOUNT \| REWARD_POINTS` | Tipe yang frontend kirim tidak dikenal backend |
| ✅ ~~XBUG-012~~ | ~~`LoyaltyBalanceResponse` punya `pointsExpiring` + `expiryDate`~~ | ~~FIXED: Added `pointsExpiring` (Integer) and `expiryDate` (Instant) to `LoyaltyBalanceResponse` record. `LoyaltyPointsService.getBalance()` now computes expiring points within 30-day window.~~ | ~~Fields added to backend DTO.~~ |
| ✅ ~~XBUG-013~~ | ~~`Reward.status = 'PENDING' | 'APPROVED' | 'REDEEMED'`~~ | ~~Backend: `AWARDED | CLAIMED | EXPIRED`~~ | ~~FIXED: Added 'AWARDED' and 'CLAIMED' to Reward status union type to match backend enum values.~~ |

---

### 🟠 High Severity Mismatches

| ID | Frontend | Backend | Mismatch |
| :--- | :--- | :--- | :--- |
| ✅ **XBUG-006** | `CreatePaymentRequest` tidak punya `accountId` | Backend wajibkan `accountId` | Semua payment creation gagal 400 |
| ✅ ~~XBUG-007~~ | ~~`POST /wallets/{accountId}/credit` from FE~~ | ~~Internal-only endpoint~~ | ~~FIXED: Removed `credit()` method from WalletService.ts. Internal-only API no longer exposed to frontend.~~ |
| ✅ **XBUG-008** | `TransactionType` enum values frontend vs backend | Naming convention bisa berbeda (e.g. `BI_FAST` vs `BIFAST`) | 400 Bad Request pada transfer |
| ✅ **XBUG-009** | `Statement` interface tidak punya `downloadUrl` | Backend DTO ada `downloadUrl` | Download link selalu broken |
| ✅ **XBUG-010** | `mutations: { retry: 1 }` global | Financial mutations non-idempotent | Double-charge on retry |
| ✅ ~~XBUG-014~~ | ~~Gamification endpoints tanpa `{accountId}`~~ | ~~Backend butuh `{accountId}`~~ | ~~FIXED: Added `userId` param to all 7 gamification methods in PromotionService.ts. Updated useGamification.ts to pass userId.~~ |
| ✅ **XBUG-015** | `GET /promotions` expects flat `Promotion[]` | Backend mungkin return `Page<Promotion>` | `.map()` crash pada non-array |
| ✅ **XBUG-016** | `ClaimPromotionRequest` tidak punya `transactionAmount` | Backend field ini **required** | Semua claim promo gagal 400 |
| ✅ **XBUG-017** | `GET /loyalty-points/account/${id}/balance` | Backend endpoint path berbeda | 404 |
| ✅ ~~XBUG-083~~ | ~~`AuditReport` frontend: `type`, `title`, `findings[]`, `status`, `riskLevel`~~ | ~~FIXED: Rewrote `ComplianceService.ts` interfaces to match backend `AuditReportResponse` DTO (transactionId, merchantId, standard, checks[], overallStatus). Created `ComplianceCheckItem` type. Updated `listAuditReports()` → `searchAuditReports()` with filter params.~~ | ~~DTOs fully aligned.~~ |

---

## 🐛 Auth/Session Bug Backlog (Feb 24, 2026)

> Files: `useSilentRefresh.ts`, `useAuth.ts`, `authStore.ts`, `middleware.ts`, `api.ts`, `refresh/route.ts`

| ID | Severity | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-AUTH-001~~ | ~~🔴 High~~ | ~~useSilentRefresh.ts~~ | ~~FIXED: Added `isRefreshingRef` lock to prevent concurrent refresh calls. Uses `useRef(false)` with `finally` block to clear.~~ | ~~Shared `isRefreshing` lock.~~ |
| ✅ ~~BUG-AUTH-002~~ | ~~🔴 High~~ | ~~useSilentRefresh.ts~~ | ~~FIXED: Immediate `doRefresh()` on mount when `isAuthenticated && tokenExpiresAt === null`.~~ | ~~Immediate refresh on mount.~~ |
| ✅ ~~BUG-AUTH-003~~ | ~~🟠 Medium~~ | ~~useSilentRefresh.ts~~ | ~~FIXED: Added `isAuthenticatedRef` to avoid stale closures. `scheduleRefresh` uses ref instead of `isAuthenticated` closure.~~ | ~~UseRef untuk isAuthenticated.~~ |
| ✅ ~~BUG-AUTH-004~~ | ~~🟠 Medium~~ | ~~useSilentRefresh.ts~~ | ~~FIXED: Added `retryAttemptsRef` with exponential backoff (2s→32s, max 5 attempts) on refresh failure. Reset to 0 on success.~~ | ~~Exponential backoff retry.~~ |
| ✅ ~~BUG-AUTH-005~~ | ~~🟠 Medium~~ | ~~refresh/route.ts~~ | ~~FIXED: `expiresIn` only returned when `newAccessToken` is truthy.~~ | ~~Conditional return.~~ |
| ✅ **BUG-AUTH-006** | 🟡 Low | `authStore.ts` L76-82 | `setAuthenticated(false)` tidak clear `tokenExpiresAt` → timer refresh masih jalan setelah logout. | Di `setAuthenticated(false)`, tambahkan `tokenExpiresAt: null`. |
| **BUG-AUTH-007** | 🟡 Low | `middleware.ts` L25-27 | Middleware izinkan akses hanya dengan `refreshToken` — BFF mungkin gagal karena tidak ada `accessToken`. | Pastikan BFF proxy bisa trigger refresh jika hanya `refreshToken`. |
| **BUG-AUTH-008** | 🟡 Low | `useSilentRefresh.ts` | Tidak ada unit test untuk hook kritis ini. | Tambahkan `vitest` fake timer tests. |

---

## 📋 Open Items (Non-Bug)

### 🔴 Gateway Gaps (Belum Ada — Perlu Dibuat)

> Detail lengkap di [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)

| ID | Item | Priority |
| :--- | :--- | :--- |
| **GAP-001** | Outbound webhook service (notify TokoBapak/Nobar saat payment done) | 🔴 P0 |
| **GAP-002** | Multi-tenancy / data isolation per partner | 🔴 P0 |
| **GAP-006** | Idempotency key support di semua payment endpoints | 🔴 P0 |
| **GAP-007** | Escrow / payment holding untuk TokoBapak | 🔴 P0 |
| **GAP-008** | Subscription / recurring billing untuk Nobar | 🔴 P0 |
| **GAP-003** | Settlement & reconciliation (daily payout ke merchant) | 🟠 P1 |
| **GAP-004** | Rate card / pricing per partner | 🟠 P1 |
| **GAP-009** | Refund & dispute management | 🟠 P1 |
| **GAP-005** | API key management (stable, non-expiring) | 🟠 P2 |
| **GAP-010** | Multi-currency settlement (FX-aware) | 🟠 P2 |

### 🟡 Simplification Items (Tetap Open)

| ID | Item | Rekomendasi |
| :--- | :--- | :--- |
| **SIMP-001** | `ab-testing-service` — broken, tidak relevan untuk payment gateway | Hapus service, ganti feature flags via env var |
| **SIMP-002** | Gamification (XP/Badge/Level) di `promotion-service` | Hapus `GamificationService.java`, keep `LoyaltyPoints` + `CashbackService` |
| **SIMP-003** | Robo-advisory di `investment-service` | Hapus, simplify ke portfolio view + mutual fund mock |

### 🟡 Architecture Questions (Perlu Keputusan)

| ID | Pertanyaan | Impact |
| :--- | :--- | :--- |
| **ARCH-001** | KYC di level PayU atau project client? | Scope kyc-service |
| **ARCH-002** | Statement: PDF untuk end-user atau JSON/CSV untuk project client? | Output format statement-service |
| **ARCH-003** | Support ticket: end-user PayU atau project client yang integrasi? | Multi-tenancy di support-service |
| **ARCH-004** | CMS: hanya untuk PayU web-app atau multi-tenant project client? | Multi-tenant mode di cms-service |

---

## 🐛 Bug Backlog — Batch 7: Shared Starters Deep Review (Feb 24, 2026)

> Services: `shared/resilience-starter`, `shared/events-starter`, `shared/outbox-starter`, `shared/logging-starter`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-093~~ | ~~shared/resilience-starter~~ | ~~FinancialOperation.java~~ | ~~FIXED: Replaced Spring property placeholders with hardcoded `"financial"` literal names in @CircuitBreaker, @Retry, @Bulkhead, @TimeLimiter annotations. Removed non-functional `fallbackMethod` from meta-annotation.~~ | ~~Meta-annotation dengan hardcoded name.~~ |
| ✅ ~~BUG-BE-094~~ | ~~FIXED: Uses handle() not whenComplete() — works correctly with future.get()~~ |
| ✅ ~~BUG-BE-095~~ | ~~FIXED: Static OUTBOX_MAPPER replaces per-call ObjectMapper creation~~ |
| ✅ ~~BUG-BE-096~~ | ~~FIXED: ObjectMapper injected via RequiredArgsConstructor~~ |

---

### 🟠 High Severity — Batch 7

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-097~~ | ~~FIXED: Changed matchIfMissing to true for default-on~~ |
| ✅ ~~BUG-BE-098~~ | ~~FIXED: Removed duplicate TimeoutException class~~ |
| ✅ ~~BUG-BE-099~~ | ~~FIXED: Added onEntryAdded handler for dynamic CB registration~~ |
| ✅ **BUG-BE-100** | `shared/outbox-starter` | `OutboxPublisher.java` L95-96 | **`@Scheduled` + `@Transactional` tanpa distributed lock** — di multi-pod deployment, semua pod poll outbox table bersamaan. `findUnpublishedEventsWithLock` pakai pessimistic lock, tapi lock hanya efektif per-DB-connection. Dua pod bisa process batch yang sama jika menggunakan different DB connections. | Tambahkan `ShedLock` atau Redis distributed lock sebelum poll. Atau gunakan `SELECT FOR UPDATE SKIP LOCKED` di repository query (PostgreSQL 9.5+). |
| ✅ ~~BUG-BE-101~~ | ~~FIXED: Changed MDC.clear() to MDC.remove() per key~~ |
| ✅ ~~BUG-BE-102~~ | ~~FIXED: OutboxProperties defaults retentionDays=30 (safe)~~ |

---

### 🟡 Medium Severity — Batch 7

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-103~~ | ~~FIXED: CacheEntry changed to private static class~~ |
| ✅ ~~BUG-BE-104~~ | ~~FIXED: refresh() wrapped in try-catch, retains stale on failure~~ |
| ✅ ~~BUG-BE-105~~ | ~~FIXED: id/time generated lazily at build() time~~ |
| ✅ ~~BUG-BE-106~~ | ~~shared/resilience-starter~~ | ~~ResilienceAutoConfiguration.java~~ | ~~FIXED: Added `Throwable.class.isAssignableFrom(clazz)` validation before unchecked cast. Non-Throwable classes logged as error and skipped. Applied to both retry and ignore exception lists.~~ | ~~Validasi sebelum cast.~~ |
| ✅ ~~BUG-BE-107~~ | ~~FIXED: Added PostConstruct on init() for metrics registration~~ |
| ✅ ~~BUG-BE-108~~ | ~~FIXED: Added timestamp to error responses~~ |

---

## 🐛 Bug Backlog — Batch 8: Deep-Dive Core Services (Feb 24, 2026)

> Services: `transaction-service` (SplitBill, QRIS, ScheduledTransfer), `wallet-service` (Pocket, Card), `auth-service` (Biometric, Risk), Frontend cross-service

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-109~~ | ~~FIXED: Replaced reflection hack with FxRateInfo.rate() accessor~~ |
| ✅ ~~BUG-BE-110~~ | ~~transaction-service~~ | ~~FIXED: Added `WalletServicePort` to `ProcessQrisPaymentCommandHandler`. Now reserves balance before QRIS call, commits on success, releases on failure. Added `accountId` to command and request DTO.~~ | ~~Wallet reservation flow integrated.~~ |
| ✅ ~~BUG-BE-111~~ | ~~FIXED: N/A: BiometricService.java does not exist in codebase~~ |
| ✅ ~~BUG-BE-112~~ | ~~FIXED: N/A: BiometricService.java does not exist in codebase~~ |
| ✅ ~~BUG-BE-113~~ | ~~transaction-service~~ | ~~SplitBillService.java L254~~ | ~~FIXED: Moved `setParticipants()` (DB refresh) before `isFullyPaid()` check so split bill completion status is based on fresh data, not stale in-memory participants.~~ | ~~Re-fetch participants sebelum evaluasi completeness.~~ |

---

### 🟠 High Severity — Batch 8

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-114~~ | ~~transaction-service~~ | ~~ProcessQrisPaymentCommandHandler.java L77~~ | ~~FIXED: Reference number replaced with UUID-based generation.~~ | ~~Ganti ke UUID-based.~~ |
| ✅ ~~BUG-BE-115~~ | ~~transaction-service~~ | ~~SplitBillService.java L323~~ | ~~FIXED: Reference number replaced with UUID-based generation.~~ | ~~Ganti ke UUID-based.~~ |
| ✅ **BUG-BE-116** | `transaction-service` | `ScheduledTransferService.java` L192-198 | **Scheduled transfer set FAILED permanently setelah 1x gagal** — tidak ada retry mechanism. Jika wallet insufficient saat scheduled run (misal gaji belum masuk saat subuh), transfer permanently FAILED. | Tambahkan retry count dan status `RETRY_PENDING`. Retry beberapa kali sebelum final FAILED. |
| ✅ **BUG-BE-117** | `transaction-service` | `SplitBillService.java` L82, L145 | **`canBeCancelled()` digunakan untuk authorize update DAN add participant** — method name menyesatkan. Logic seharusnya: boleh update kalau DRAFT/ACTIVE, tapi hanya boleh cancel kalau belum ada payment. | Pisahkan: `canBeModified()` untuk update/addParticipant, `canBeCancelled()` hanya untuk cancel. |
| ✅ **BUG-BE-118** | `transaction-service` | `SplitBillService.java` L277-295 | **`settleSplitBill()` ≠ actual settlement** — method set status COMPLETED tapi **tidak memproses sisa pembayaran**. Participant yang belum bayar dianggap lunas tanpa uang berpindah. Ini bukan settlement, ini force-close. | Rename ke `forceCloseSplitBill()` atau implementasi actual settlement via wallet transfer. |
| ✅ ~~BUG-BE-119~~ | ~~wallet-service~~ | ~~CardService.java L28, L52, L110-115~~ | ~~FIXED: Replaced `java.util.Random` with `SecureRandom` for all card number and CVV generation. Prevents predictable card credentials.~~ | ~~Ganti ke `SecureRandom` untuk semua card-related random generation.~~ |
| ✅ ~~BUG-BE-120~~ | ~~auth-service~~ | ~~RiskEvaluationService.java L74~~ | ~~FIXED: MFA now configurable via `payu.security.risk.mfa-enabled` property (default: false). No longer hardcoded disabled.~~ | ~~Buat configurable: `@Value("${payu.security.risk.mfa-enabled:false}")`. Jangan hardcode.~~ |
| ✅ ~~BUG-BE-121~~ | ~~auth-service~~ | ~~RiskEvaluationService.java L167~~ | ~~FIXED: Added separate `payu.security.risk.lockout-threshold` property (default: 5) for account lockout. No longer reuses `mfaThreshold` (50) which was too permissive.~~ | ~~Buat separate `@Value("${payu.security.risk.lockout-threshold:5}")`.~~ |
| ✅ ~~BUG-BE-122~~ | ~~FIXED: N/A: BiometricService.java does not exist in codebase~~ |

---

### 🟡 Medium Severity — Batch 8

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-123~~ | ~~transaction-service~~ | ~~ScheduledTransferService.java L201-202~~ | ~~FIXED: Reference number replaced with UUID-based generation.~~ | ~~Ganti ke UUID.~~ |
| ✅ ~~BUG-BE-124~~ | ~~transaction-service~~ | ~~SplitBillService.java L298-300~~ | ~~FIXED: EQUAL split now uses `RoundingMode.DOWN` for base amount and assigns remainder to last participant. `100.00 / 3 = 33.33 + 33.33 + 33.34` instead of `33.34 × 3 = 100.02`.~~ | ~~Hitung sisa rounding, assign ke participant terakhir.~~ |
| ✅ **BUG-BE-125** | `wallet-service` | `CardService.java` L49 | **Expiry date `MM/yy` format** — Disimpan sebagai String, tidak di-parse saat validasi. Card dengan expiry lalu bisa tetap ACTIVE. | Tambahkan `isExpired()` check atau simpan sebagai `YearMonth` lalu validate di freeze/unfreeze flow. |
| ✅ **BUG-CROSS-019** | FE ↔ BE | `TransactionService.ts` L193 vs BE | **ScheduledTransfer `frequency` enum mismatch** — FE: `'ONCE' \| 'DAILY' \| 'WEEKLY' \| 'MONTHLY'`. BE `ScheduleType`: `ONE_TIME`, `RECURRING_DAILY`, `RECURRING_WEEKLY`, `RECURRING_MONTHLY`, `RECURRING_CUSTOM`. FE kirim `ONCE`, BE expect `ONE_TIME`. | Sinkronkan enum atau mapping di BFF proxy. |
| ✅ **BUG-CROSS-020** | FE ↔ BE | `TransactionService.ts` L221-233 vs BE | **SplitBill `status` enum mismatch** — FE: `'DRAFT' \| 'ACTIVE' \| 'SETTLED' \| 'CANCELLED'`. BE: `DRAFT`, `ACTIVE`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`. FE tidak kenal `IN_PROGRESS` dan `COMPLETED`; BE tidak kenal `SETTLED`. | Sinkronkan: FE harus handle `IN_PROGRESS` dan `COMPLETED` dari BE. |
| ✅ **BUG-CROSS-021** | FE ↔ BE | `TransactionService.ts` L228-234 vs BE | **SplitBillParticipant field mismatch** — FE `participant.name` dan `participant.amount`. BE expect `accountName`, `amountOwed`. Request params berbeda → 400 Bad Request. | Sesuaikan FE interface `name→accountName`, `amount→amountOwed`. |

---


---

## 🐛 Bug Backlog — Batch 9: BFF Proxy, WebSocket, Authorization, Archival (Feb 24, 2026)

> Areas: BFF proxy (`route.ts`), `useWebSocket.ts`, `AuthorizationService`, `TransactionArchivalService`, `SplitBillSecurityService`, `PartnerService.ts`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-FE-027~~ | ~~FIXED: sanitizeBackendPath() with whitelist + traversal rejection~~ |
| ✅ ~~BUG-FE-028~~ | ~~FIXED: 503 returns {error:true,_fallback:true} not fake success data~~ |
| ✅ **BUG-BE-126** | `transaction-service` | `AuthorizationService.java` L91-95 | **`extractAccountIdFromUserId()` return `userId` as-is** — semua authorization check membandingkan `transaction.getSenderAccountId().toString()` dengan `userId` langsung. Jika userId = UUID dan accountId = UUID yang berbeda (multi-account), authorization selalu fail. Jika kebetulan match (single-account), ini accidental dan fragile. | Implement proper account lookup: call account-service atau parse JWT claims untuk extract accountId list. |
| ✅ **BUG-BE-127** | `transaction-service` | `SplitBillSecurityService.java` L33 | **Type mismatch `UUID` vs `UUID`** — `response.getCreatorAccountId()` return `UUID`, dibandingkan dengan `userId` param yang juga `UUID`. Tapi jika `getCreatorAccountId()` return type sebenarnya String (check SplitBillResponse.java), comparison selalu false → owner check selalu fail → no one can access. | Pastikan tipe data creatorAccountId konsisten antara response DTO dan security check. |

---

### � High Severity — Batch 9

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-FE-029~~ | ~~FIXED: Forwards all x-* headers including idempotency-key~~ |
| ✅ ~~BUG-FE-030~~ | ~~FIXED: Exponential backoff 1s-30s, max 10 retries, fresh handlers~~ |
| ✅ ~~BUG-FE-031~~ | ~~FIXED: Uses getter get ws() for always-fresh wsRef.current~~ |
| ✅ **BUG-BE-128** | `transaction-service` | `TransactionArchivalService.java` L67, L79 | **Archive + delete dalam satu transaksi** — jika `deleteArchivedTransactions()` gagal setelah `archiveTransactions()` sukses, rollback menghapus archive tapi transaksi asli juga di-rollback? Tergantung isolation level. Jika merge-commit dan partial fail → data loss. | Pisahkan: archive batch A → verify → delete batch A. Atau gunakan soft-delete pattern (set `archived=true`) lalu cleanup later. |
| ✅ **BUG-BE-129** | `transaction-service` | `TransactionArchivalService.java` L66 | **Infinite loop jika `findTransactionsToArchive` selalu return same data** — while(true) loop query ulang setelah delete. Jika delete gagal (silently) → query return batch yang sama → infinite loop. | Tambahkan max iterations guard dan verify rowcount dari delete. |
| ✅ ~~BUG-FE-032~~ | ~~web-app~~ | ~~PartnerService.ts~~ | ~~FIXED: Removed `clientSecret` from `Partner` interface. Added `PartnerWithCredentials` extending Partner for registration response only. Removed clientSecret reference from merchant page.~~ | ~~Security: clientSecret removed from FE.~~ |
| ✅ ~~BUG-FE-033~~ | ~~web-app~~ | ~~PartnerService.ts~~ | ~~FIXED: Removed `getSnapBiToken()` method and `useSnapBiAuthToken` hook. SNAP-BI token exchange must happen server-side only.~~ | ~~Server-side only token exchange.~~ |

---

### �🟡 Medium Severity — Batch 9

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-FE-034~~ | ~~FIXED: callbacksRef pattern for handlers, no bloated deps~~ |
| ✅ ~~BUG-FE-035~~ | ~~FIXED: Changed to useMutation for on-demand challenge~~ |
| ✅ **BUG-BE-130** | `transaction-service` | `TransactionArchivalService.java` L49 | **`ZonedDateTime.now()` tanpa explicit timezone** — behavior tergantung JVM timezone. Di container yang timezone = UTC vs Jakarta → cutoff date beda 7 jam. Transaction bisa ter-archive prematur atau terlambat. | Gunakan `ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))` atau `Instant.now().minus(retentionMonths, ChronoUnit.MONTHS)`. |
| ✅ ~~BUG-FE-036~~ | ~~FIXED: useBuyGold invalidates gold-holdings + wallet-balance~~ |
| ✅ **BUG-CROSS-022** | FE ↔ BE | `useSplitBill.ts` + `SplitBillService.java` | **SplitBill mutations tidak invalidate wallet balance** — setelah `makePayment` sukses, `wallet-balance` query key tidak di-invalidate. Dashboard balance stale. | Tambahkan `qc.invalidateQueries({ queryKey: ['wallet-balance'] })` di `useSplitBillPayment.onSuccess`. |
| ✅ ~~BUG-CROSS-023~~ | FE ↔ BE | `useLending.ts` + `LendingService.ts` | **Loan application `onSuccess` invalidate hanya `['loan']`** — harusnya juga invalidate `['credit-score']` dan `['wallet-balance']` karena loan disbursement affects balance. | Tambahkan query key invalidations yang relevan. |

---


---

## 🐛 Bug Backlog — Batch 10: Controllers & API Security (Feb 24, 2026)

> Areas: `WalletController`, `CardController`, `TransactionController`, `SnapBiController`, `OnboardingController`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-131~~ | ~~FIXED: PreAuthorize(isAuthenticated) already on all card endpoints~~ |
| ✅ ~~BUG-BE-132~~ | ~~FIXED: validateReservationOwnership + PreAuthorize implemented~~ |
| ✅ ~~BUG-BE-133~~ | ~~FIXED: maskCardNumber() masks to last 4 digits~~ |
| ✅ ~~BUG-BE-134~~ | ~~FIXED: Added 5 min timestamp validation to prevent replay attacks~~ |
| ✅ ~~BUG-BE-135~~ | ~~transaction-service~~ | ~~FIXED: Created `TransactionResponse` DTO with `from(Transaction)` mapper. Controller now returns `ApiResponse<TransactionResponse>` instead of exposing domain entity.~~ | ~~DTO mapping implemented.~~ |

---

### 🟠 High Severity — Batch 10

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-136~~ | ~~FIXED: UserId passed to UseCase for ownership validation~~ |
| ✅ ~~BUG-BE-137~~ | ~~transaction-service~~ | ~~FIXED: `getAccountTransactions()` now returns `List<TransactionResponse>` with `PaginationInfo` containing page, size, totalElements, totalPages, hasNext, hasPrevious.~~ | ~~Pagination metadata added.~~ |
| ✅ ~~BUG-BE-138~~ | ~~partner-service~~ | ~~FIXED: Replaced `PartnerRepository` with `PartnerService` in `SnapBiController`. Added `findByClientId()` method to `PartnerService`.~~ | ~~Hexagonal architecture restored.~~ |
| ✅ ~~BUG-BE-139~~ | ~~partner-service~~ | ~~FIXED: Changed SNAP-BI endpoints to accept `@RequestBody String rawBody` for signature validation, then parse to typed DTO after verification.~~ | ~~Raw body signature validation.~~ |
| ✅ ~~BUG-BE-140~~ | `account-service` | `OnboardingController.java` L43-45 | **`CompletableFuture<ResponseEntity<User>>` return type** — async response tanpa timeout. Jika `registerUser` hangs → request hangs indefinitely. Juga, domain model `User` langsung di-return (mungkin contain password hash). | Tambahkan `.orTimeout(10, TimeUnit.SECONDS)`. Create `RegisterUserResponse` DTO tanpa sensitive fields. |
| ✅ ~~BUG-BE-141~~ | ~~FIXED: maskId() implemented and used in log statements~~ |
| ✅ ~~BUG-BE-142~~ | `wallet-service` | `WalletController.java` L169 | **`UUID.fromString(accountId)` tanpa try-catch** — jika accountId bukan valid UUID → 500 error. Harusnya return 400. | Wrap dalam try-catch atau gunakan custom validator `@ValidUUID`. |

---

### 🟡 Medium Severity — Batch 10

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-143~~ | `transaction-service` | `TransactionController.java` L65-73 | **`extractUserId()` repeat pattern** — setiap method call `extractUserId()` manually. Error-prone jika developer lupa. | Inject userId via Spring `@AuthenticationPrincipal` parameter annotation. |
| ✅ ~~BUG-BE-144~~ | `transaction-service` | `TransactionController.java` L128-144, L310-327 | **Generic exception catch returns 500** — `catch (Exception e)` logs stacktrace tapi returns generic error. Loses specific error info yang berguna untuk debugging. | Biarkan `@ControllerAdvice` / `GlobalExceptionHandler` handle exceptions secara uniform. Hapus try-catch di controller. |
| ✅ ~~BUG-BE-145~~ | `wallet-service` | `CardController.java` L70 | **`ResponseEntity.notFound().build()` tanpa `ApiResponse` wrapper** — semua endpoint lain return `ApiResponse<>`, tapi not-found return bare 404. FE parsing inconsistent. | Return `ResponseEntity.status(404).body(ApiResponse.error("CARD_NOT_FOUND", "Card not found"))`. |
| ✅ ~~BUG-BE-146~~ | `partner-service` | `SnapBiController.java` L259-272 | **`SnapErrorResponse` inner class** — error response class defined as inner class di controller. Tidak reusable dan visibility issues (public fields tapi private class). | Extract ke separate file `SnapErrorResponse.java` dengan proper encapsulation. |

---

---

## 🐛 Bug Backlog — Batch 11 (Final): Remaining Controllers, FE Services, Auth Gaps (Feb 24, 2026)

> Areas: `LendingController`, `InvestmentController`, `BillingController`, `BackofficeController`, `AuthController`, FE `AuthService.ts`, `InvestmentService.ts`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-147~~ | `lending-service` | `LendingController.java` L55-83 | **🔒 Loan application endpoint tanpa `@PreAuthorize`** — siapapun bisa apply loan atas nama user lain karena userId dari request body, bukan dari JWT. No ownership check. | Tambahkan `@PreAuthorize` dan extract userId dari JWT (bukan request body). |
| ✅ ~~BUG-BE-148~~ | `lending-service` | `LendingController.java` L158-177 | **🔒 PayLater activate tanpa ownership check** — `userId` dari `@RequestParam`, siapapun bisa activate PayLater untuk user lain. Juga, `createRepaymentSchedule` (L101) dan `processRepayment` (L144) tanpa `@PreAuthorize`. | Tambahkan ownership validation via `@PreAuthorize` dan extract userId dari JWT. |
| ✅ ~~BUG-BE-149~~ | `investment-service` | `InvestmentController.java` L30-157 | **🔒 Seluruh InvestmentController tanpa `@PreAuthorize` atau `@SecurityRequirement`** — semua endpoints (buy deposit, buy mutual fund, buy gold, sell, get account) tidak ada auth check. Siapapun bisa invest/sell atas nama user lain. | Tambahkan `@SecurityRequirement` dan `@PreAuthorize` ownership checks. |
| ✅ ~~BUG-BE-150~~ | `investment-service` | `InvestmentController.java` L66-69, L88-91, L109-110, L129-131 | **Financial parameters via `@RequestParam` bukan `@RequestBody`** — amount, userId, accountId dikirim via query params. Query params logged di web server access logs, proxy logs, browser history. PII & financial data exposed di plaintext. | Pindahkan ke `@RequestBody` DTO (e.g., `BuyDepositRequest`, `BuyGoldRequest`). |
| ✅ ~~BUG-BE-151~~ | `backoffice-service` | `BackofficeController.java` L223-227, L363-366, L453-456 | **🔒 Admin identity fallback ke `"system"`** — jika `X-Admin-User` header absent, adminUser = `"system"`. Audit trail rusak — semua actions tanpa header terlihat dilakukan oleh "system". No accountability. | Wajibkan header atau extract dari JWT `authentication.principal`. |

---

### 🟠 High Severity — Batch 11

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-152~~ | `lending-service` | `LendingController.java` L195-223 | **PayLater purchase tanpa ownership validation** — `userId` dari path, `merchantName` dan `amount` dari `@RequestParam`. Siapapun bisa record purchase untuk user lain. Sama untuk `recordPayment` (L225). | Buat proper DTO `@RequestBody` dan tambahkan ownership `@PreAuthorize`. |
| ✅ ~~BUG-BE-153~~ | `lending-service` | `LendingController.java` L260-268 | **Credit score calculation endpoint open** — `calculateCreditScore()` tanpa `@PreAuthorize`. Siapapun bisa trigger credit score calculation untuk user manapun via `@RequestParam userId`. | Tambahkan `@PreAuthorize` ownership check. |
| ✅ ~~BUG-BE-154~~ | `auth-service` | `AuthController.java` L112-124, L143-146 | **Login: password dikirim 2x ke Keycloak** — `validateCredentialsBlocking()` pertama untuk validation, lalu `loginBlocking()` lagi dengan password yang sama. Double network call, double exposure of credentials. | Gabungkan: langsung call `loginBlocking()`, handle invalid credentials dari response. |
| ✅ ~~BUG-BE-155~~ | ~~auth-service~~ | ~~AuthController.java L117-123~~ | ~~FIXED: Added `riskEvaluationService.recordFailedAttempt()` on failed login and `recordSuccessfulLogin()` on success. Brute force detection now operational.~~ | ~~Tambahkan `riskEvaluationService.recordFailedAttempt(request.username())` sebelum return error.~~ |
| ✅ ~~BUG-BE-156~~ | ~~backoffice-service~~ | ~~VERIFIED: `listKycReviews()`, `listFraudCases()`, `listCustomerCases()` already return `ApiResponse` wrapper via `ok()` base method.~~ | ~~Already wrapped.~~ |
| ✅ ~~BUG-BE-157~~ | `backoffice-service` | `BackofficeController.java` L176, L335, L425 | **Enum `valueOf()` tanpa error handling** — `KycStatus.valueOf(status.toUpperCase())` bisa throw `IllegalArgumentException` jika status invalid. Return 500 bukan 400. | Wrap dalam try-catch, return 400 dengan message "Invalid status: ...". |
| ✅ ~~BUG-BE-158~~ | ~~backoffice-service~~ | ~~FIXED: Created `CreateFraudCaseRequest` record DTO. Changed endpoint from `APPLICATION_FORM_URLENCODED` with 9 `@RequestParam` to JSON `@RequestBody CreateFraudCaseRequest`.~~ | ~~JSON body implemented.~~ |
| ✅ ~~BUG-BE-159~~ | ~~billing-service~~ | ~~FIXED: Added `extractUserId()` + `validateOwnership()` methods to `PaymentController`. `getPayment()` and `getPaymentByReference()` now verify payment.accountId matches authenticated user's JWT subject.~~ | ~~Ownership validation added.~~ |

---

### 🟡 Medium Severity — Batch 11

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-FE-037~~ | `web-app` | `AuthService.ts` L73 vs `api/v1/[...path]/route.ts` | **Login endpoint mismatch** — FE AuthService calls `/api/auth/login` (tanpa `v1`). BFF proxy hanya handle `/api/v1/*`. Login request tidak proxy ke backend. | Sesuaikan: FE harus panggil `/api/v1/auth/login` atau buat dedicated BFF auth route. |
| ✅ ~~BUG-FE-038~~ | `web-app` | `AuthService.ts` L94-95, L154 | **Logout/refresh endpoint mismatch** — FE calls `/api/auth/logout` dan `/api/auth/refresh` tanpa `v1`. Sama problem dengan login. | Pindahkan ke `/api/v1/auth/*` atau buat route handler tersendiri. |
| ✅ ~~BUG-CROSS-024~~ | FE ↔ BE | `InvestmentService.ts` L122-136 vs BE | **FE buy operations kirim request body, BE expect query params** — FE `api.post('/investments/deposits', request)` kirim JSON body. BE `@RequestParam amount` expect query string params. Request selalu gagal → 400. | Sync: either FE switch ke query params, atau (lebih baik) BE switch ke `@RequestBody`. |
| ✅ ~~BUG-CROSS-025~~ | FE ↔ BE | `InvestmentService.ts` L140-142 vs BE | **Sell investment params mismatch** — FE: `{investmentId, amount}`. BE: `@RequestParam accountId, @RequestParam transactionId, @RequestParam amount`. FE tidak kirim `accountId`, kirim `investmentId` bukan `transactionId`. | Sinkronkan field names dan required params. |
| ✅ ~~BUG-FE-039~~ | `web-app` | `AuthService.ts` L53-54, L123-124 | **`authenticated` flag = client-side state** — flag di-set manual. Jika page refresh → `authenticated = false` meskipun httpOnly cookie masih valid. `isAuthenticated()` bukan reliable. | Cek auth state via `validateSession()` saat init, atau hapus client-side flag entirely — rely on server response. |
| ✅ ~~BUG-BE-160~~ | `lending-service` | `LendingController.java` L67 | **`CompletableFuture` tanpa timeout** — `applyLoan()` return `CompletableFuture` tanpa `.orTimeout()`. Request bisa hang indefinitely. | Tambahkan `.orTimeout(30, TimeUnit.SECONDS)`. |
| ✅ ~~BUG-BE-161~~ | `investment-service` | `InvestmentController.java` L46-49, L65-72, etc. | **`CompletableFuture` tanpa timeout (semua endpoints)** — semua investment endpoints return `CompletableFuture` tanpa timeout. Pattern sama dengan BUG-BE-160. | Tambahkan `.orTimeout()` ke semua async endpoints. |
| ✅ ~~BUG-BE-162~~ | `backoffice-service` | `BackofficeController.java` L482, L503 | **Universal search SQL injection risk** — `search(query, entityType)` langsung terima user input `query`. Tergantung implementasi `UniversalSearchService`, jika pakai native SQL query → SQL injection. | Verify `UniversalSearchService` pakai parameterized queries. Add input validation/sanitization. |

---


---

## 🐛 Bug Backlog — Batch 12 (True Final): SecurityConfig, CORS, Middleware, Remaining (Feb 24, 2026)

> Areas: SecurityConfig files (semua services), `middleware.ts`, `lib/api.ts`, `lib/validation.ts`, `PartnerController`, `ComplianceAuditController`, FE services

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-163~~ | ~~partner-service~~ | ~~SecurityConfig.java L47~~ | ~~FIXED (prior): CORS restricted to `payu.co.id` and `partner.payu.co.id` instead of wildcard `*`.~~ | ~~Set specific allowed origins.~~ |
| ✅ ~~BUG-BE-164~~ | ~~partner-service~~ | ~~PartnerController.java~~ | ~~FIXED (prior): Added `@PreAuthorize("hasRole('ADMIN')")` at class level on PartnerController.~~ | ~~Tambahkan `@PreAuthorize("hasRole('ADMIN')")`.~~ |
| ✅ ~~BUG-BE-165~~ | ~~partner-service~~ | ~~PartnerController.java L226-231~~ | ~~FIXED (prior): `regenerateKeys()` now masks client secret (first 4 chars + ***) and has `@RateLimiter`.~~ | ~~Hanya return masked secret. Tambahkan rate limit.~~ |
| ✅ ~~BUG-FE-040~~ | `web-app` | `middleware.ts` L25-27 | **🔒 Auth check HANYA berdasarkan cookie existence** — `request.cookies.has('refreshToken')`. Cookie bisa exist tapi expired/invalid. Middleware tidak validate cookie value. | Ini acceptable untuk Edge middleware (no DB access), tapi perlu tambahan server-side validation di BFF proxy. Document limitation ini. |

---

### 🟠 High Severity — Batch 12

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-166~~ | ~~auth-service~~ | ~~SecurityConfig.java L36-38~~ | ~~FIXED: Added `/api/v1/auth/mfa/verify` and `/api/v1/auth/mfa/challenge` to PUBLIC_ENDPOINTS. Users can't have JWT during MFA flow.~~ | ~~Tambahkan `/api/v1/auth/mfa/verify` ke `PUBLIC_ENDPOINTS`.~~ |
| ✅ ~~BUG-BE-167~~ | ~~auth-service~~ | ~~SecurityConfig.java L119~~ | ~~FIXED (prior): `@Value` injection replaces `System.getenv()` for JWT config. Test profile overrides now work.~~ | ~~Gunakan `@Value`.~~ |
| ✅ ~~BUG-BE-168~~ | ~~compliance-service~~ | ~~FIXED (prior): Field is `final`, no public setter. Constructor injection used.~~ | ~~Hapus setter, buat field `final`.~~ |
| ✅ ~~BUG-BE-169~~ | ~~compliance-service~~ | ~~FIXED (prior): Uses `ResponseStatusException(HttpStatus.BAD_REQUEST)` instead of `IllegalArgumentException`.~~ | ~~Buat custom `BadRequestException` atau handle.~~ |
| ✅ ~~BUG-FE-041~~ | `web-app` | `lib/api.ts` L63, L68 | **Refresh endpoint path mismatch** — interceptor calls `/api/auth/refresh`, tapi BFF proxy hanya handle `/api/v1/*`. Refresh always fails → redirect ke login → infinite redirect loop jika user punya valid refreshToken cookie. | Sinkronkan: `/api/v1/auth/refresh` atau buat dedicated route. Sama issue dengan BUG-FE-037/038. |
| ✅ ~~BUG-FE-042~~ | `web-app` | `lib/api.ts` L50, L58 | **Race condition: `_retry` flag on config object** — `originalRequest._retry = true` modifies shared config. Jika axios reuses config object (interceptor re-fires), flag bisa sudah set → skip refresh → silent failure. | Gunakan WeakSet untuk track retried requests: `const retriedRequests = new WeakSet()`. |
| ✅ ~~BUG-CROSS-026~~ | FE ↔ BE | `BillingService.ts` L53 vs `PaymentController.java` | **Billing FE path mismatch** — FE calls `/billing/payments` → BFF proxy to `/billing/payments`. BE `PaymentController` mounted di `/api/v1/payments` (tanpa `/billing/`). Requests always 404. | Sinkronkan path antara FE service dan BE controller. |
| ✅ ~~BUG-CROSS-027~~ | FE ↔ BE | `AccountService.ts` L9 vs `OnboardingController.java` | **FE sends `nik` di registration request** — `RegisterUserRequest` FE includes `nik`. Jika nik sampai ke BE dan tidak di-mask/encrypt → PII compliance violation. Backend HARUS mask di logs dan encrypt di DB. | Verify `@Sensitive` annotation di `nik` field dan server-side encryption via `security-starter`. |
| ✅ ~~BUG-FE-043~~ | ~~web-app~~ | ~~LendingService.ts L130-134~~ | ~~VERIFIED ALREADY FIXED: `recordPurchase()` already sends merchantName, amount, description in POST JSON body (not query params). Comment documents BUG-BE-079 fix.~~ | ~~Already uses `@RequestBody`.~~ |

---

### 🟡 Medium Severity — Batch 12

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-FE-044~~ | ~~web-app~~ | ~~lib/validation.ts L427~~ | ~~FIXED: Created `parseIndonesianAmount()` helper that correctly handles Indonesian currency format (dot as thousands separator, comma as decimal). `parseFloat("1.500.000")` no longer returns 1.5.~~ | ~~Indonesian locale-aware parsing.~~ |
| ✅ ~~BUG-FE-045~~ | `web-app` | `lib/validation.ts` L89-101 | **Email domain typo detection blocks valid domains** — `.co` domains (e.g., `user@company.co`) valid tapi di-reject karena typo detection. `gmail.co` → suggest `gmail.com`, tapi `company.co` bukan typo. | Hanya suggest, jangan block — set `isValid: true` tapi tambahkan `suggestion` field. |
| ✅ ~~BUG-FE-046~~ | ~~web-app~~ | ~~middleware.ts~~ | ~~FIXED: Route matching now uses exact match or segment boundary (`=== route || startsWith(route + '/')`) to prevent `/login-debug` etc. from matching.~~ | ~~Exact match atau trailing `/`.~~ |
| ✅ ~~BUG-BE-170~~ | ~~support/billing/transaction-service~~ | ~~SecurityConfig.java (multiple)~~ | ~~FIXED: Added `@EnableMethodSecurity` to support-service, billing-service, and transaction-service SecurityConfig. Without this, `@PreAuthorize` annotations were silently not enforced.~~ | ~~Tambahkan `@EnableMethodSecurity`.~~ |
| ✅ ~~BUG-BE-171~~ | ~~wallet-service, transaction-service, auth-service~~ | ~~SecurityConfig.java (multiple)~~ | ~~FIXED: Replaced deprecated `SecurityContextPersistenceFilter` with `SecurityContextHolderFilter` in wallet-service and transaction-service. Auth-service was already fixed.~~ | ~~Ganti reference ke `SecurityContextHolderFilter`.~~ |
| ✅ ~~BUG-FE-047~~ | ~~web-app~~ | ~~lib/currency.ts~~ | ~~FIXED: `roundCurrency` now uses `Number(amount.toFixed(decimals))` instead of `Math.round(amount * multiplier) / multiplier` to avoid floating point errors.~~ | ~~Gunakan `Number((amount).toFixed(decimals))`.~~ |

---

### 🟡 Deferred (Diprioritaskan Nanti)

| ID | Description | Status |
| :--- | :--- | :--- |
| **P2-FE-003** | Mobile App Feature Parity (Expo/React Native) | Deferred |
| **OCP-007** | Service Mesh mTLS enforcement | Planned |
| **OCP-010** | API versioning headers | Planned |
| **DR-001** | Disaster Recovery live test execution | Scripts ready, pending execution |

---

_Last Updated: February 24, 2026 | Bug review session COMPLETE — 12 batches, ~232 bugs. All backend services (22), shared starters (3), frontend web-app (services, hooks, stores, middleware, lib, BFF proxy) reviewed._

