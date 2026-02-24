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
| **BUG-BE-002** | `auth-service` | In-memory `failedAttempts`, `tokenStore`, `otpStore`, `challengeStore` — multi-pod tidak sync | MFA/brute-force protection gagal di scale-out |
| ✅ ~~BUG-BE-035~~ | ~~partner-service~~ | ~~FIXED: Token store moved to Redis with TTL — token persistent antar pod~~ | ~~Partner integration gagal di HPA~~ |
| **BUG-BE-062** | `promotion-service` | Cashback langsung `CREDITED` tanpa credit wallet | User tidak terima uang cashback |
| **BUG-BE-060** | `promotion-service` | Race condition di loyalty points balance — lost update | Saldo poin salah |
| **BUG-BE-090** | `shared/api-commons` | `RateLimitAspect`: `increment` + `expire` non-atomic — permanent rate-limit possible | User bisa di-block selamanya |
| **BUG-FE-021** | All financial services | Tidak ada `X-Idempotency-Key` — double-tap bisa double-charge | Transfer/payment duplikat |
| **BUG-FE-027** | `providers.tsx` | Global `mutations: { retry: 1 }` — auto-retry financial mutations | Double debit on network error |
| **GAP-001** | *(belum ada)* | Outbound webhook ke partner (TokoBapak/Nobar) tidak ada | Partner tidak bisa tahu payment status |
| **GAP-002** | `partner-service` | Multi-tenancy tidak ada — data isolation antar partner | Data TokoBapak bisa bocor ke Nobar |
| **GAP-006** | All payment endpoints | Idempotency key tidak didukung | Double-charge on retry |
| **GAP-007** | `wallet-service` | Escrow/payment holding belum ada | TokoBapak tidak bisa implement checkout |
| **GAP-008** | *(belum ada)* | Recurring/subscription billing belum ada | Nobar tidak bisa auto-debit |

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
| **BUG-BE-008** | `wallet-service` | `WalletService.java` L162-163 | **Type mismatch**: `accountId` String di-cast ke UUID → `IllegalArgumentException` runtime. | Standardisasi: pilih satu, `accountId` selalu UUID atau selalu String. |
| ✅ ~~BUG-BE-009~~ | ~~lending-service~~ | ~~LoanManagementService.java L103-130~~ | ~~FIXED: Last installment amount now uses actual remaining principal + interest instead of the standard monthly rate.~~ | ~~Pada last installment: `installmentAmount = outstandingPrincipal + interestAmount`.~~ |
| **BUG-BE-010** | `auth-service` | `KeycloakService.java` L199-215 | **`Mono.block()` di Spring MVC thread** — Blocking WebFlux di Tomcat thread pool → thread starvation under load. | Ganti ke synchronous `RestTemplate` atau migrasi ke WebFlux. |
| **BUG-BE-011** | `transaction-service` | `ScheduledTransferScheduler.java` L22 | **`@Scheduled` tanpa distributed lock** — Multi-pod: semua pod proses transfer yang sama bersamaan. | Tambahkan distributed lock via Redis (`ShedLock` atau custom). |

---

### 🟡 Low Severity — Batch 1

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-012~~ | ~~promotion-service~~ | ~~FIXED: Replaced `Math.random()` with `SecureRandom` for referral code generation.~~ | ~~Gunakan `SecureRandom`.~~ |
| ✅ ~~BUG-BE-013~~ | ~~wallet-service~~ | ~~FIXED: Reused first `findByAccountId` result instead of querying DB twice.~~ | ~~Gunakan result dari cek pertama.~~ |
| ✅ ~~BUG-BE-014~~ | ~~lending-service~~ | ~~FIXED: Added `@Transactional` to `processRepayment` method.~~ | ~~Tambahkan `@Transactional`.~~ |
| **BUG-BE-015** | `transaction-service` | Komentar TODO: pagination info tidak dikembalikan ke client. | Implementasi `Page<Transaction>` return. |
| ✅ ~~BUG-BE-016~~ | ~~auth-service~~ | ~~FIXED: Username masked in all log statements via `maskUsername()` — shows only first 2 + last 2 chars.~~ | ~~Mask or hash username in logs.~~ |
| ✅ ~~BUG-BE-017~~ | ~~gateway-service~~ | ~~FIXED: Authorization header no longer logged. Downgraded to DEBUG with only `hasAuth` boolean.~~ | ~~Remove or downgrade to DEBUG.~~ |

---

### 🟠 Medium Severity — Frontend-Backend Mismatch Batch 1

| ID | Area | Mismatch | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-CROSS-001** | Auth | `expiresIn` frontend camelCase, backend `expires_in` snake_case. | Pastikan BFF mapping `expires_in` → `expiresIn`. |
| **BUG-CROSS-002** | Transaction | Frontend kirim `accountId` bisa non-UUID, backend expect `UUID`. | Validasi UUID format di frontend. |
| **BUG-CROSS-003** | Wallet | Frontend `response.data` langsung array, backend `ApiResponse<List<>>` wrapper. | Terapkan Axios interceptor auto-unwrap atau konsisten `response.data.data`. |
| **BUG-CROSS-004** | Transfer | Frontend kirim `BILL_PAYMENT`/`TOP_UP` ke transfer endpoint, backend tidak mengenal enum ini. | Sinkronkan TransactionType enum. |
| **BUG-CROSS-005** | Auth | Cookie `maxAge: 900` hardcode tapi Keycloak token lifetime 3600s → logout 15 menit. | Baca `expiresIn` dari Keycloak response, jangan hardcode. |
| **BUG-CROSS-006** | Biometric | Frontend tidak punya `BiometricService.ts` padahal backend punya endpoint lengkap. | Implementasi `BiometricService.ts` atau hapus backend endpoint. |

---

## 🐛 Bug Backlog — Batch 2: Extended Services (Feb 24, 2026)

> Services: `account-service`, `investment-service`, `fx-service`, `notification-service`, `shared/security-starter`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-018** | `investment-service` | `WalletServiceAdapter.java` L29-31 | **Endpoint wallet tidak ada** — `POST /wallets/{userId}/deduct` dan `/credit` tidak terdefinisi di `WalletController`. Semua beli/jual investasi 404. | Sesuaikan dengan flow reserve-commit yang ada di `wallet-service`. |
| ✅ ~~BUG-BE-019~~ | ~~shared/security-starter~~ | ~~EncryptionService.java L263~~ | ~~FIXED: PBKDF2 salt now configurable via `payu.security.encryption.salt` property. Default fallback preserved for backward compat.~~ | ~~Jadikan configurable via env var.~~ |
| ✅ ~~BUG-BE-020~~ | ~~account-service~~ | ~~UserApplicationService.java L35-36~~ | ~~FIXED: Removed `@Async` from registerUser to ensure DB ops run synchronously within the transaction.~~ | ~~Pisahkan: sync untuk DB ops, async hanya untuk event publishing.~~ |

---

### 🟠 High Severity — Batch 2

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-021** | `investment-service` | `InvestmentApplicationService.java` L115 | **No saga compensation** — `deductBalance` sukses tapi `saveDeposit` gagal → uang hilang tanpa deposit tersimpan. | Implementasikan saga: jika save gagal, `creditBalance()` rollback. |
| ✅ ~~BUG-BE-022~~ | ~~investment-service~~ | ~~Multiple files~~ | ~~FIXED: Reference numbers (DEP, MF, SELL) replaced with UUID-based generation.~~ | ~~Ganti ke UUID-based.~~ |
| ✅ ~~BUG-BE-023~~ | ~~fx-service~~ | ~~FxRateService.java L59-61~~ | ~~FIXED: Caught exception per-currency to continue updating other rates even if one fails.~~ | ~~Catch exception per-currency, lanjutkan ke berikutnya.~~ |
| **BUG-BE-024** | `fx-service` | `FxConversionService.java` L27-35 | **FX conversion tidak pernah gerakkan wallet** — status PENDING dibuat tapi tidak ada debit/kredit. | Integrasikan dengan wallet reservation flow. |
| ✅ ~~BUG-BE-025~~ | ~~notification-service~~ | ~~NotificationService.java L75~~ | ~~FIXED: Added retry scheduling logic with exponential backoff and a scheduled job to process pending retries.~~ | ~~Implementasi retry scheduler untuk FAILED notifications.~~ |
| **BUG-BE-026** | `notification-service` | `SmsSender.java` L16-29 | **SMS sender adalah mock** — OTP tidak pernah terkirim ke user. | Integrasikan Twilio/Vonage atau provider SMS lokal. |
| **BUG-BE-027** | `account-service` | `UserApplicationService.java` L64 | **User `ACTIVE` meski KYC `REJECTED`** — user bisa login dan transaksi meski gagal KYC. | Jika `kycStatus == REJECTED`, set `status = PENDING_VERIFICATION`. |

---

### 🟠 Medium Severity — Batch 2

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-BE-028** | `investment-service` | Fee BUY mutual fund menggunakan `redemptionFee` (fee yang harusnya saat SELL). | Gunakan `subscriptionFee` saat BUY. |
| **BUG-BE-029** | `investment-service` | `hasSufficientBalance()` baca key `"balance"` tapi field-nya `availableBalance` → always false. | Baca `"availableBalance"` dari response. |
| **BUG-BE-030** | `shared/security-starter` | `DataMaskingAspect` pointcut terlalu broad — setiap method di `id.payu..service`. Overhead signifikan. | Batasi ke method dengan `@Audited` annotation. |
| **BUG-BE-031** | `account-service` | Race condition registrasi — tanpa handler `DataIntegrityViolationException` dari `save()`. | Tangkap `DataIntegrityViolationException` → return 409. |
| **BUG-BE-032** | `fx-service` | `fee = BigDecimal.ZERO` semua FX conversion — frontend tampilkan fee estimasi tapi backend gratis. | Dokumentasikan sebagai intentional atau implementasikan fee. |

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
| **BUG-BE-039** | `billing-service` | `PaymentService.java` L61-78 | **Balance reserved tapi tidak di-commit** — `reserveBalance()` dipanggil, `commitReservation()` tidak pernah dipanggil. Balance stuck di "reserved" selamanya. | Setelah biller sukses: `walletPort.commitReservation(reservationId)`. Jika gagal: `releaseReservation()`. |
| **BUG-BE-040** | `backoffice-service` | `UniversalSearchService.java` L26-55 | **Search load semua hasil ke memory** — fetch ALL records, paginate di Java `subList()`. OOM risk untuk data besar. | Implementasi pagination di repository dengan `Pageable`. |
| **BUG-BE-041** | `partner-service` | `SnapBiSignatureService.java` L19-22 | **Format SNAP-BI signature salah** — tidak menggunakan `SHA-256 hex(body)` sesuai standar BI. Signature verifikasi di BI akan gagal. | Ikuti spesifikasi SNAP-BI: `method + ":" + sha256hex(body) + ":" + timestamp`. |
| **BUG-BE-042** | `outbox-starter` | `OutboxPublisher.java` L192 | `throw` dalam `whenComplete` lambda tidak sampai ke caller — exception hilang. | Gunakan `exceptionally()` atau flag ke outer try-catch. |

---

### 🟠 Medium Severity — Batch 3

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-BE-043** | `backoffice-service` | `listByStatus()` abaikan `page`/`size` parameter — return semua data. | Gunakan `repository.findByStatus(status, PageRequest.of(page, size))`. |
| **BUG-BE-044** | `partner-service` | `SimpleDateFormat` tidak thread-safe di `getCurrentTimestamp()`. | Ganti ke `DateTimeFormatter` (thread-safe). |
| **BUG-BE-045** | `billing-service` | `WalletAdapter` hanya punya `reserveBalance`, tidak ada `commit`/`release`. | Tambahkan `commitReservation` + `releaseReservation` ke `WalletPort`. |
| **BUG-BE-046** | `outbox-starter` | `new ObjectMapper()` setiap call `serializePayload()` — sangat expensive. | Inject `ObjectMapper` sebagai bean. |
| **BUG-BE-047** | `partner-service` | `rotateExpiringCertificates()` tidak ada `@Scheduled` trigger. | Tambahkan `@Scheduled(cron = "0 0 8 * * *")`. |
| **BUG-BE-048** | `kyc-service` + `analytics-service` | Development CORS origins (`localhost:3000`) aktif di production. | Pisahkan CORS config berdasarkan `ENVIRONMENT` env var. |

---

## 🐛 Bug Backlog — Batch 4: Statement, CMS, A/B Testing (Feb 24, 2026)

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-049** | `statement-service` | `StatementService.java` L69-70 | **`@Async` + `@Transactional` anti-pattern** — `@Transactional` tidak efektif di thread async. State bisa stuck di `GENERATING`. | Pisahkan inner `@Transactional` untuk DB ops dari `@Async` outer. |
| **BUG-BE-050** | `statement-service` | `StatementService.java` L54 | **PDF disimpan ke `/tmp/statements`** — ephemeral di Kubernetes, hilang saat pod restart. | Ganti ke persistent volume atau upload ke object storage (S3/MinIO). |

---

### 🟠 High Severity — Batch 4

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-051** | `statement-service` | `WalletServiceClient.java` L31-39 | **`getBalanceAtDate()` return saldo SAAT INI**, bukan historis. Statement opening/closing balance selalu sama (saldo terkini). | Implementasikan balance history endpoint di wallet-service. |
| **BUG-BE-052** | `statement-service` | `TransactionServiceClient.java` L24-25 | `new RestTemplate()` — tidak pakai Spring bean, timeout/resilience config tidak berlaku. | Inject `RestTemplate` via Spring. |
| **BUG-BE-053** | `statement-service` | `TransactionServiceClient.java` L49-52 | Exception fetch transactions di-swallow diam-diam → statement kosong tanpa error. | Minimal log error, throw exception agar statement gagal tegas. |
| **BUG-BE-054** | `statement-service` | `StatementService.java` L447 | **PDF max 20 transaksi saja** — 100+ transaksi di-truncate. Statement tidak lengkap. | Implementasi multi-page PDF. |
| **BUG-BE-055** | `ab-testing-service` | `ExperimentService.java` L220-246 | `@CacheEvict` di `trackConversion()` — setiap conversion event invalidate experiment cache. Ribuan/menit = constant DB fetch. | Pisahkan metrics update, jangan evict experiment cache. |
| **BUG-BE-056** | `ab-testing-service` | `ExperimentService.java` L228-243 | **Race condition metrics update** — read-modify-write tanpa lock. Lost update pada concurrent requests. | Atomic DB update: `UPDATE SET metrics = jsonb_set(...)` atau Redis counter. |
| **BUG-BE-057** | `cms-service` | `ContentService.java` L45-48 | Race condition unique title check — dua create concurrent bisa lolos, `DataIntegrityViolationException` tidak di-handle. | `UNIQUE` constraint di DB + tangkap `DataIntegrityViolationException` → 409. |

---

### 🟠 Medium Severity — Batch 4

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-BE-058** | `cms-service` | `getContentByType()` return ALL content tanpa pagination → memory-intensive jika ribuan banner. | Tambahkan pageable parameter atau limit. |
| **BUG-BE-059** | `statement-service` | `getStatement()` dalam `@Transactional(readOnly=true)` melakukan `recordAccess()` + `save()`. | Hapus `readOnly=true` atau pisahkan `recordAccess()`. |

---

## 🐛 Bug Backlog — Batch 5: Promotion & Shared (Feb 24, 2026)

> Services: `promotion-service`, `compliance-service`, `shared/saga-starter`, `shared/cache-starter`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-060** | `promotion-service` | `LoyaltyPointsService.java` L124-130 | **Race condition pada balance calculation** — `balanceAfter` dibaca dari record terakhir. Concurrent writes → lost update → saldo salah. | `SELECT FOR UPDATE` atau atomic balance column di table terpisah. |
| **BUG-BE-061** | `promotion-service` | `GamificationService.java` L442-444 | **`getTransactionAmount()` selalu return `ZERO`** — badge berbasis total amount tidak bisa diraih. | Inject `TransactionServiceClient` dan query jumlah transaksi real. |
| **BUG-BE-062** | `promotion-service` | `CashbackService.java` L56 | **Cashback `CREDITED` tanpa credit ke wallet** — cashback tercatat tapi saldo tidak bertambah. | Panggil wallet-service credit sebelum set `CREDITED`. Wrap dengan saga. |
| **BUG-BE-063** | `promotion-service` | `PromotionService.java` L148-149 | **Race condition max redemptions** — dua concurrent claim bisa keduanya lolos check, kuota melebihi limit. | Optimistic locking `@Version` atau atomic: `UPDATE SET count = count + 1 WHERE count < max`. |
| **BUG-BE-064** | `shared/cache-starter` | `CacheService.java` L169-172 | **Stale-while-revalidate tidak async** — saat stale, hanya return data lama tanpa trigger refresh. | Inject executor + `CompletableFuture.runAsync(() -> put(key, fallback.get()))` saat stale. |

---

### 🟠 High Severity — Batch 5

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-065** | `promotion-service` | `LoyaltyPointsService.java` L100-121 | **`getBalance()` count transaksi bukan sum poin** — `.count()` bukan `.mapToInt(getPoints).sum()`. Saldo poin selalu = jumlah record. | Ganti ke `.mapToInt(LoyaltyPoints::getPoints).sum()`. |
| **BUG-BE-066** | `promotion-service` | `GamificationService.java` L127-174 | Idempotency check O(n) in-memory, tanpa DB unique constraint — concurrent request bisa duplicate insert. | Tambahkan `UNIQUE INDEX (account_id, transaction_id)` di DB. |
| **BUG-BE-067** | `promotion-service` | `GamificationService.java` L374-427 | **N+1 query** — `badgeRepository.findById()` di-call per badge dalam loop. 50 badge = 50 queries. | Gunakan `findAllById(ids)` (1 query) + Map lookup. |
| **BUG-BE-068** | `shared/saga-starter` | `SagaOrchestrator.java` L154-156 | **`executeAsync()` pakai `ForkJoinPool.commonPool()`** — kompete dengan HTTP workers, risk starvation. | Inject custom `TaskExecutor` dan gunakan di `supplyAsync(..., customExecutor)`. |
| **BUG-BE-069** | `shared/saga-starter` | `SagaOrchestrator.java` L277-283 | **`Thread.sleep()` di retry** — blocking Tomcat thread pool. | Gunakan `ScheduledExecutorService` atau Spring `TaskScheduler`. |
| **BUG-BE-070** | `compliance-service` | `SecurityConfig.java` | **No role-based authorization** — `.authenticated()` saja, user biasa bisa akses compliance endpoints. | `@PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")` pada semua endpoints. |

---

### 🟠 Medium Severity — Batch 5

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-BE-071** | `promotion-service` | `getOrCreateUserLevel()` tidak thread-safe — duplicate `UserLevel` bisa tercipta. | `INSERT ... ON CONFLICT DO NOTHING` atau retry on `DataIntegrityViolationException`. |
| **BUG-BE-072** | `promotion-service` | `getTotalCheckins()` fetch ALL data ke app lalu `.count()` — seharusnya `COUNT(*)` di DB. | Tambahkan `countByAccountId(String accountId)` di repository. |
| **BUG-BE-073** | `promotion-service` | Kafka publish errors hanya `LOG.warn` — tidak ada alert. Events hilang diam-diam jika Kafka down. | Gunakan outbox-starter atau tambahkan metric counter + alert. |
| **BUG-BE-074** | `shared/cache-starter` | `localCache.get(key, Object.class)` — type mismatch exception possible jika type berbeda per-key. | Tambahkan type safety check atau gunakan `ConcurrentHashMap` dengan type token. |
| **BUG-BE-075** | `promotion-service` | `calculateRewardAmount()` pakai deprecated `BigDecimal.ROUND_HALF_UP` constant (Java 9+). | Ganti ke `RoundingMode.HALF_UP`. |

---

## 🐛 Bug Backlog — Batch 6: API Portal, Lending, Compliance, FX (Feb 24, 2026)

> Services: `api-portal-service`, `lending-service`, `compliance-service`, `fx-service`, `shared/api-commons`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-076** | `api-portal-service` | `SandboxService.java` L28-29 | **Sandbox store in-memory** — `paymentStore` + `refundStore` hilang saat pod restart. Tim dev TokoBapak/Nobar kehilangan test data. | Gunakan Redis atau database. |
| ✅ ~~BUG-BE-077~~ | ~~api-portal-service~~ | ~~SandboxService.java L34~~ | ~~FIXED: Reference numbers (PAY, REF) replaced with UUID-based generation.~~ | ~~Ganti ke `UUID.randomUUID()`.~~ |
| **BUG-BE-078** | `fx-service` | `FxService.ts` L138 | **`/fx-api/v1` prefix hardcode** — berbeda dari semua service lain, tidak ada BFF routing untuk ini. Semua FX calls 404. | Unify ke `/api/fx/v1`, update BFF routing. |
| **BUG-BE-079** | `lending-service` | `LendingService.ts` L130-134 | **Data finansial di URL query params** — `amount`, `merchantName` dikirim sebagai `params: {}` → ter-log di server access logs, browser history. | Ubah ke POST JSON body. |
| **BUG-BE-080** | `lending-service` | `LendingService.ts` L161-173 | **Pre-approval endpoints ada di frontend, tidak ada di backend** — 404. | Expose di `LendingController.java` atau hapus dari `LendingService.ts`. |
| **BUG-BE-090** | `shared/api-commons` | `RateLimitAspect.java` L45-50 | **Race condition rate limit** — `increment` + `expire` dua operasi Redis terpisah. Jika `expire` gagal: counter tanpa TTL → user permanently blocked. | Gunakan Redis Lua script untuk atomic increment+expire. |
| **BUG-BE-091** | `shared/api-commons` | `RateLimitAspect.java` L69 | **Fixed-window rate limit mudah di-burst** — 59 req/menit di detik 59 + 59 req di detik 0 next = 118 req dalam 2 detik. | Gunakan sliding window atau Token Bucket. |
| **BUG-BE-092** | `shared/api-commons` | `WebhookProcessor.java` L226 | **`Thread.sleep()` di `@Async` retry** — meski di thread pool terpisah, pool bisa habis jika banyak webhook retry bersamaan. | Gunakan `ScheduledExecutorService.schedule()` non-blocking. |

---

### 🟠 High Severity — Batch 6

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-BE-081** | `compliance-service` | `DELETE /gdpr-audit/{auditId}` di frontend — **menghapus audit log melanggar prinsip immutability**. | Hapus endpoint DELETE. Implementasikan soft-delete dengan approval workflow jika memang perlu. |
| **BUG-BE-082** | `api-portal-service` | `getPaymentStatus()` return `null` jika tidak ditemukan — NPE blind spot, return 200 dengan body null. | Return `Optional<>` atau throw `PaymentNotFoundException` → 404. |
| **BUG-BE-083** | `compliance-service` | `AuditReport` frontend vs backend **zero field overlap** — model sama sekali berbeda. | Sinkronkan DTO sebelum compliance feature bisa fungsi. |
| **BUG-BE-084** | `fx-service` | `estimateConversion()` call `POST /conversions/estimate` — endpoint `/estimate` tidak ada di backend. | Implementasi `/estimate` atau hitung di frontend dari `GET /rates`. |
| **BUG-BE-085** | `lending-service` | `processRepayment()` kirim `amount` sebagai query param — tidak aman (log exposure). | Ganti ke JSON body. |

---

### 🟠 Medium Severity — Batch 6

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-BE-086** | `FxService.ts` | `FxConversionRequest` dan `ConvertCurrencyRequest` adalah interface identik — duplikasi. | Hapus salah satu. |
| **BUG-BE-087** | `FxService.ts` | `FxRate` dan `FxRateResponse` adalah interface identik — duplikasi. | Hapus `FxRate`, gunakan `FxRateResponse`. |
| **BUG-BE-088** | `api-portal-service` | OpenAPI aggregation dari 22 services — tidak ada error handling per-service, timeout jika satu down. | Tambahkan timeout per-service + partial result. |
| **BUG-BE-089** | `compliance-service` | `createAuditReport()` + `listAuditReports()` exposed ke frontend — seharusnya internal only. | Hapus dari user-facing frontend service. |

---

## 🐛 Frontend Bug Backlog (Feb 24, 2026)

---

### 🔴 High Severity

| ID | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-FE-001** | `api/v1/[...path]/route.ts` L28-29 | BFF proxy tidak retry saat `accessToken` expired — loop 401 tanpa attempt refresh. | BFF deteksi 401 → panggil `/api/auth/refresh` → retry upstream. |
| **BUG-FE-002** | `uiStore.ts` L37-47 | Toast `setTimeout` ID tidak disimpan → memory leak, update state pada unmounted component. | Simpan timeout ID per toast, clear di `removeToast`. |
| **BUG-FE-003** | `uiStore.ts` L24 | `toastIdCounter` tidak reset → non-deterministik di tests. | Ganti ke `crypto.randomUUID()`. |
| **BUG-FE-004** | `useWebSocket.ts` L62-73 | WebSocket reconnect leak — handler closure lama + orphan connection. | Track koneksi baru di `wsRef.current` sebelum assign handler. |
| **BUG-FE-020** | `ABTestingService.ts` L164-212 | **A/B test cache pakai `localStorage`** — satu-satunya yang melanggar policy no-localStorage di codebase. | Ganti ke cookie non-httpOnly atau Zustand store dengan `persist`. |
| **BUG-FE-021** | `services/` (semua) | **Tidak ada idempotency key** pada operasi finansial — double-tap = double-charge. | Tambahkan `X-Idempotency-Key: uuid-v4` di semua financial mutations. |
| **BUG-FE-022** | `exchange/page.tsx` L72-91 | `useEffect` deps `estimateMutation` (object baru per-render) → infinite loop. | Gunakan `useCallback`/`useRef` untuk stable reference. |

---

### 🟠 Medium Severity

| ID | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-FE-005** | `useWebSocket.ts` L84-86 | `ws: null as unknown as WebSocket` — type lie, crash jika consumer akses `ws.send()`. | Return `wsRef.current` atau hapus `ws` dari return. |
| **BUG-FE-006** | `useAnalytics.ts` L20-22 | WebSocket dibuka meski `accountId` undefined — URL terbentuk tanpa ID, SSR crash `window.location.host`. | Guard: jangan panggil `useWebSocket` jika `accountId` falsy. |
| **BUG-FE-007** | `transactionStore.ts` L48-49 | `setDetailOpen(false)` set `selectedTransactionId` ke `undefined` bukan `null` — type mismatch. | Ganti `undefined` dengan `null`. |
| **BUG-FE-008** | `lib/validation.ts` L44-46 | Normalisasi `6208xxx` salah — `'0' + normalized.substring(3)` hasilkan nomor invalid. | Review prefix `6208`, hapus atau dokumentasikan use case. |
| **BUG-FE-009** | `lib/validation.ts` L390-397 | Password strength hitung dari filter string error bahasa Indonesia — fragile jika terjemahan berubah. | Hitung dari boolean checks langsung. |
| **BUG-FE-010** | `lib/currency.ts` L94-101 | `yahoo.co` dianggap typo — `yahoo.co.id` valid ditolak. | Check exact: `domain === 'yahoo.co'` saja. |
| **BUG-FE-011** | `lib/date.ts` L148 | `diffMonths` approximation 30 hari → tidak akurat. | Gunakan `Intl.RelativeTimeFormat` atau selisih `.getMonth()`. |
| **BUG-FE-012** | `lib/currency.ts` L202-246 | `numberToWords` tidak handle > triliun → return `"undefined"` di string. | Guard: `if (scaleIndex >= scales.length)` return fallback. |
| **BUG-FE-013** | `useTransactions.ts` L33-34 | `invalidateQueries({ queryKey: ['transactions'] })` terlalu broad — invalidate semua account. | Gunakan `queryKey: ['transactions', accountId]` spesifik. |
| **BUG-FE-014** | `logout/route.ts` L18-24 | Backend logout `fetch()` tanpa `await` — cookie clear duluan sebelum invalidate session di Keycloak. | Tambahkan `await` dengan timeout max 2 detik. |
| **BUG-FE-023** | `rewards/page.tsx` L28-63 | **Hardcoded fake data di production** — `9300 poin`, `PAYU2024`, dll. ditampilkan ke user. | Tampilkan skeleton/empty state, bukan fake data. |
| **BUG-FE-024** | `types/index.ts` L36-41 | `LoginResponse` expose `access_token` + `refresh_token` — kontradiksi arsitektur httpOnly cookie. | Hapus field token dari tipe `LoginResponse`. |
| **BUG-FE-025** | `AccountService.ts` L85-101 | Deprecated methods tanpa removal plan + `console.warn` tidak cukup. | Tambahkan JSDoc `@deprecated` + schedule removal. |
| **BUG-FE-026** | `providers.tsx` L12-13 | `refetchOnReconnect: false` + `refetchOnWindowFocus: false` global — balance stale tidak auto-refresh. | Set global ke `true`, override per-query yang tidak perlu. |
| **BUG-FE-027** | `providers.tsx` L19-21 | **`mutations: { retry: 1 }` global** — auto-retry financial mutations → double debit. | Set `retry: 0` global, override per-mutation non-finansial. |
| **BUG-FE-028** | `useExperiment.ts` L155-157 | `error = experimentError \|\| assignmentError` — hanya error pertama tersimpan, error kedua hilang. | Gunakan `??` atau simpan keduanya. |
| **BUG-FE-029** | `useExperiment.ts` L149 | `enabled` condition terlalu kompleks — jika experiment loading gagal, assignment tidak pernah di-fetch. | Split kondisi atau tambahkan `isError: false` check. |
| **BUG-FE-030** | `KYCService.ts` L14-18 | KTP image sebagai base64 > 10MB → lampaui limit request body. | Gunakan `FormData` multipart atau resize sebelum encode. |
| **BUG-FE-031** | `ABTestingService.ts` L209-211 | `localStorage.setItem` gagal silent → infinite re-fetch karena cache miss. | Flag bahwa caching gagal agar tidak retry terus. |
| **BUG-FE-032** | `WalletService.ts` + `TransactionService.ts` | Return type pagination tidak akurat — crash jika `data.length` dipanggil pada `undefined`. | Definisi `PaginatedResponse<T>` dan gunakan konsisten. |

---

### 🟡 Low Severity

| ID | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-FE-015** | `notificationStore.ts` L60-68 | `unreadCount` bisa negatif jika desync. | Tambahkan `Math.max(0, ...)`. |
| **BUG-FE-016** | `api/v1/[...path]/route.ts` L87 | Error response GET pakai status 503 tapi body mirip sukses — consumer hanya cek `response.data`. | Konsistenkan: 503 + pastikan semua consumer handle error state. |
| **BUG-FE-017** | `lib/date.ts` L283 | `startOfDay` mutate input `Date` object. | Copy: `new Date(date.getTime())` pada branch `instanceof Date`. |
| **BUG-FE-018** | `useWebSocket.ts` L40 | `console.log('WebSocket connected')` di production. | Ganti ke env-aware logger atau hapus. |
| **BUG-FE-019** | `lib/validation.ts` L471 | Regex nama tidak support Unicode — nama Indonesia/asing dengan aksen ditolak. | Ganti ke `/^[\p{L}\s\.\,\-\']+$/u`. |

---

## 🐛 Cross-Service Mismatch Backlog

---

### 🔴 Critical Mismatches

| ID | Frontend | Backend | Mismatch |
| :--- | :--- | :--- | :--- |
| **XBUG-001** | `StatementStatus = 'READY'` | Backend return `COMPLETED` | Frontend polling status `READY` → loop selamanya |
| **XBUG-002** | `PaymentStatus` tidak punya `PROCESSING`/`REFUNDED` | Backend punya | Status baru dari backend ditampilkan sebagai blank |
| **XBUG-003** | `Experiment` punya `variants: Variant[]` array | Backend hanya punya `variantAConfig`/`variantBConfig` | Struktur sama sekali berbeda → deserialisasi gagal |
| **XBUG-004** | Frontend punya 15+ method scheduled-transfers dan split-bills | Backend tidak punya endpoint ini | Semua call 404 |
| **XBUG-005** | `POST /statements/generate` tidak kirim `customerId` | Backend butuh `customerId` untuk security check | User bisa generate statement orang lain |
| **XBUG-011** | `RewardType = 'LOYALTY_POINTS' \| 'CASHBACK' \| 'VOUCHER'` | Backend: `PERCENTAGE \| FIXED_AMOUNT \| REWARD_POINTS` | Tipe yang frontend kirim tidak dikenal backend |
| **XBUG-012** | `LoyaltyBalanceResponse` punya `pointsExpiring` + `expiryDate` | Backend DTO tidak punya field ini | UI selalu tampilkan `undefined` |
| **XBUG-013** | `Reward.status = 'PENDING' \| 'APPROVED' \| 'REDEEMED'` | Backend: `AWARDED \| CLAIMED \| EXPIRED` | Status dari backend muncul sebagai blank di frontend |

---

### 🟠 High Severity Mismatches

| ID | Frontend | Backend | Mismatch |
| :--- | :--- | :--- | :--- |
| **XBUG-006** | `CreatePaymentRequest` tidak punya `accountId` | Backend wajibkan `accountId` | Semua payment creation gagal 400 |
| **XBUG-007** | `POST /wallets/{accountId}/credit` dipanggil dari frontend | Endpoint ini internal-only | User-facing bisa trigger credit langsung |
| **XBUG-008** | `TransactionType` enum values frontend vs backend | Naming convention bisa berbeda (e.g. `BI_FAST` vs `BIFAST`) | 400 Bad Request pada transfer |
| **XBUG-009** | `Statement` interface tidak punya `downloadUrl` | Backend DTO ada `downloadUrl` | Download link selalu broken |
| **XBUG-010** | `mutations: { retry: 1 }` global | Financial mutations non-idempotent | Double-charge on retry |
| **XBUG-014** | Gamification endpoints tanpa `{accountId}` path variable | Backend butuh `{accountId}` | Semua gamification call 404 atau 400 |
| **XBUG-015** | `GET /promotions` expects flat `Promotion[]` | Backend mungkin return `Page<Promotion>` | `.map()` crash pada non-array |
| **XBUG-016** | `ClaimPromotionRequest` tidak punya `transactionAmount` | Backend field ini **required** | Semua claim promo gagal 400 |
| **XBUG-017** | `GET /loyalty-points/account/${id}/balance` | Backend endpoint path berbeda | 404 |
| **XBUG-083** | `AuditReport` frontend: `type`, `title`, `findings[]`, `status`, `riskLevel` | Backend: `transactionId`, `merchantId`, `standard`, `checks[]` | Zero overlap, compliance feature 100% broken |

---

## 🐛 Auth/Session Bug Backlog (Feb 24, 2026)

> Files: `useSilentRefresh.ts`, `useAuth.ts`, `authStore.ts`, `middleware.ts`, `api.ts`, `refresh/route.ts`

| ID | Severity | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-AUTH-001** | 🔴 High | `useSilentRefresh.ts` + `api.ts` | **Race condition double-refresh** — tanpa shared `isRefreshing` lock, dua refresh paralel → 401 storm. | Expose `isRefreshing` flag sebagai shared state atau satu koordinator tunggal. |
| **BUG-AUTH-002** | 🔴 High | `useSilentRefresh.ts` L86 | `tokenExpiresAt` null setelah page reload → fallback 15 menit padahal token mungkin tinggal 1 menit. | Saat `tokenExpiresAt === null && isAuthenticated`: immediate refresh on mount. |
| **BUG-AUTH-003** | 🟠 Medium | `useSilentRefresh.ts` L95 | Stale closure → timer reset tidak perlu tiap kali `isAuthenticated` berubah. | Gunakan `useRef` untuk `isAuthenticated`. |
| **BUG-AUTH-004** | 🟠 Medium | `useSilentRefresh.ts` L73-76 | Network error di silent refresh → refresh berhenti selamanya tanpa retry. | Schedule retry dengan exponential backoff setelah network error. |
| **BUG-AUTH-005** | 🟠 Medium | `refresh/route.ts` L52-53 | `expiresIn` dikembalikan meski `newAccessToken` tidak diterima dari gateway. | Return `expiresIn` hanya jika `newAccessToken` truthy. |
| **BUG-AUTH-006** | 🟡 Low | `authStore.ts` L76-82 | `setAuthenticated(false)` tidak clear `tokenExpiresAt` → timer refresh masih jalan setelah logout. | Di `setAuthenticated(false)`, tambahkan `tokenExpiresAt: null`. |
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
| **BUG-BE-093** | `shared/resilience-starter` | `FinancialOperation.java` L48-54 | **`@FinancialOperation` annotation tidak berfungsi** — menggunakan `${payu.resilience.financial.circuit-breaker:default}` sebagai `name` pada `@CircuitBreaker`, tapi Resilience4j annotations **tidak support Spring property placeholders**. Semua `name` dan `fallbackMethod` literal string, bukan resolved value. Annotation ini decorative — tidak menerapkan resilience apapun. | Buat custom AOP `@Around` aspect yang membaca annotation attributes dan secara programmatic apply CircuitBreaker/Retry/Bulkhead/TimeLimiter dari registry. Atau ubah ke meta-annotation dengan hardcoded `name="financial-default"`. |
| **BUG-BE-094** | `shared/outbox-starter` | `OutboxPublisher.java` L189-192 | **`throw` di dalam `whenComplete()` lambda** — exception dari `throw new OutboxPublishException(...)` di async callback tidak sampai ke caller. Exception hilang diam-diam, event dianggap sukses padahal Kafka gagal. | Hapus `throw` dari lambda. Gunakan `future.get(10, TimeUnit.SECONDS)` di L200 sebagai satu-satunya error detection (sudah ada), atau tangani error di callback dan set flag. |
| **BUG-BE-095** | `shared/outbox-starter` | `OutboxPublisher.java` L322-329 | **`new ObjectMapper()` di setiap call `serializePayload()`** — ObjectMapper mahal untuk instantiate, dan `.registerModule(JavaTimeModule)` dipanggil setiap kali. Di `pollAndPublish` yang berjalan tiap 1 detik dengan 100 events per batch = 100 ObjectMapper baru per detik. | Inject `ObjectMapper` bean di constructor, atau buat static final instance. |
| **BUG-BE-096** | `shared/outbox-starter` | `OutboxService.java` L68-69 | **`new ObjectMapper()` langsung di field** — `objectMapper = new ObjectMapper().findAndRegisterModules()`. Tidak menggunakan Spring-managed ObjectMapper bean dengan konfigurasi global (Jackson locale, date format, module). Bisa menyebabkan serialization yang berbeda dari ObjectMapper Spring yang digunakan REST controller. | Inject `ObjectMapper` via constructor `@RequiredArgsConstructor`. |

---

### 🟠 High Severity — Batch 7

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-097** | `shared/resilience-starter` | `ResilienceAutoConfiguration.java` L36 | **`matchIfMissing = false`** — resilience-starter **tidak auto-enable**. Service yang dependency ke resilience-starter tapi lupa set `payu.resilience.enabled=true` di `application.yml` → resilience patterns silently disabled. Semua `@CircuitBreaker`, `@Retry` dsb. tidak berfungsi tanpa explicit config. | Ubah ke `matchIfMissing = true` agar default-on. Atau dokumentasikan requirement di README. |
| **BUG-BE-098** | `shared/resilience-starter` | `FallbackHandler.java` L98 | **Duplicate exception class di `@ExceptionHandler`** — `@ExceptionHandler({java.util.concurrent.TimeoutException.class, java.util.concurrent.TimeoutException.class})` — class yang sama di-list dua kali. Tidak error, tapi menunjukkan copy-paste typo. | Hapus duplikat. Jika ingin handle `io.github.resilience4j.timelimiter.exception.TimeoutException` juga, tambahkan class yang benar. |
| **BUG-BE-099** | `shared/resilience-starter` | `ResilienceAspect.java` L28-66 | **`registerCircuitBreakerEventPublisher()` hanya register existing CB saat `@PostConstruct`** — CircuitBreaker yang dibuat SETELAH init (lazy-created saat first call) tidak di-register. Log monitoring untuk CB baru silently missing. | Register event publisher lewat `CircuitBreakerRegistry.getEventPublisher().onEntryAdded()` agar dinamis. |
| **BUG-BE-100** | `shared/outbox-starter` | `OutboxPublisher.java` L95-96 | **`@Scheduled` + `@Transactional` tanpa distributed lock** — di multi-pod deployment, semua pod poll outbox table bersamaan. `findUnpublishedEventsWithLock` pakai pessimistic lock, tapi lock hanya efektif per-DB-connection. Dua pod bisa process batch yang sama jika menggunakan different DB connections. | Tambahkan `ShedLock` atau Redis distributed lock sebelum poll. Atau gunakan `SELECT FOR UPDATE SKIP LOCKED` di repository query (PostgreSQL 9.5+). |
| **BUG-BE-101** | `shared/logging-starter` | `CorrelationIdFilter.java` L50 | **`MDC.clear()` terlalu agresif** — membersihkan SELURUH MDC context, termasuk entries yang di-set oleh framework lain (Spring Security, OpenTelemetry, Micrometer). | Gunakan `MDC.remove()` untuk setiap key yang di-set oleh filter ini, bukan `MDC.clear()`. |
| **BUG-BE-102** | `shared/outbox-starter` | `OutboxCleanupScheduler.java` L39 | **`matchIfMissing = true` untuk cleanup** — Cleanup scheduler aktif by default. Jika service lupa konfigurasi `payu.outbox.cleanup.retention-days`, default mungkin `0` → langsung hapus semua published events termasuk yang baru. | Pastikan default `retentionDays` minimal 7 di `OutboxProperties`. Atau ubah ke `matchIfMissing = false` agar explicit opt-in. |

---

### 🟡 Medium Severity — Batch 7

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-103** | `shared/resilience-starter` | `CachedFallback.java` L187 | **`CacheEntry<V>` inner class tidak `static`** — inner class non-static membawa implicit reference ke outer `CachedFallback<T>`, menyebabkan potential memory leak jika `CacheEntry` di-retain. | Ubah ke `private static class CacheEntry<V>`. Akses `ttl` melalui parameter constructor. |
| **BUG-BE-104** | `shared/resilience-starter` | `CachedFallback.java` L83-88 | **`refresh()` memanggil `supplier.get()` tanpa error handling** — jika supplier throw exception, cache tidak ter-update dan exception propagate ke caller tanpa fallback. | Wrap `supplier.get()` dalam try-catch. Jika gagal, retain value lama dan log warning. |
| **BUG-BE-105** | `shared/events-starter` | `CloudEventBuilder.java` L16 | **`UUID.randomUUID()` dipanggil di field initializer** — setiap builder instance langsung generate UUID meski mungkin `.id()` akan dipanggil kemudian. Waste resource minor. | Lazy generate: set `id = null`, generate di `build()` jika masih null. |
| **BUG-BE-106** | `shared/resilience-starter` | `ResilienceAutoConfiguration.java` L138 | **Unchecked cast `(Class<? extends Throwable>) Class.forName()`** — jika className bukan subclass Throwable, ClassCastException di-catch tapi exception tidak di-propagate. Silent misconfiguration. | Validasi `Throwable.class.isAssignableFrom(clazz)` sebelum cast. Log error jika bukan Throwable subclass. |
| **BUG-BE-107** | `shared/outbox-starter` | `OutboxPublisher.java` L78-85 | **`init()` method tidak dipanggil otomatis** — tidak ada `@PostConstruct` annotation. Metrics gauge `outbox.pending.events` dan `outbox.unpublished.count` tidak pernah di-register kecuali dipanggil manual. | Tambahkan `@PostConstruct` pada `init()` method. |
| **BUG-BE-108** | `shared/resilience-starter` | `FallbackHandler.java` L156-162 | **`getCircuitBreakerOpenResponse()` static helper tidak include `timestamp`** — response error tanpa timestamp menyulitkan debugging. Semua resilience error response MAP juga tidak immutable. | Tambahkan `response.put("timestamp", Instant.now())` dan gunakan `Map.of()` atau `Collections.unmodifiableMap()`. |

---

## 🐛 Bug Backlog — Batch 8: Deep-Dive Core Services (Feb 24, 2026)

> Services: `transaction-service` (SplitBill, QRIS, ScheduledTransfer), `wallet-service` (Pocket, Card), `auth-service` (Biometric, Risk), Frontend cross-service

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-109** | `wallet-service` | `PocketService.java` L156-172 | **Reflection hack untuk baca FX rate** — `rate.getClass().getDeclaredField("rate"); rateField.setAccessible(true)`. Port interface mengembalikan `Optional<?>` (wildcard), lalu pakai reflection untuk extract field `rate`. Fragile, security risk (`setAccessible`), dan akan break jika FxRate class berubah. | Definisikan proper return type di `FxRateProviderPort.getCurrentRate()`. Return `Optional<FxRate>` dengan getter `getRate()`, bukan wildcard. |
| **BUG-BE-110** | `transaction-service` | `ProcessQrisPaymentCommand.java` L59, L61-68 | **QRIS payment tidak debit wallet** — setelah QRIS simulator return SUCCESS, `transaction.status = COMPLETED` tapi **tidak ada wallet debit**. Uang tidak berkurang dari user wallet. | Integrate wallet reservation flow: `reserve → qris → commit/release`. |
| **BUG-BE-111** | `auth-service` | `BiometricService.java` L29, L44 | **`challengeStore` in-memory `ConcurrentHashMap`** — challenges hilang saat pod restart. Multi-pod: challenge generate di pod A, verify di pod B → selalu gagal. Sama dengan BUG-BE-002 pattern. | Pindahkan ke Redis dengan TTL `challengeExpirySeconds`. |
| **BUG-BE-112** | `auth-service` | `BiometricService.java` L58, L124-125 | **Biometric auth return mock JWT token** — `"mock-jwt-access-token-" + UUID.randomUUID()`. Token ini bukan valid JWT dan tidak di-verify di gateway-service. User yang biometric auth tidak bisa akses protected endpoints. | Integrate dengan Keycloak untuk issue real token, atau delegate ke `AuthController` setelah biometric verified. |
| **BUG-BE-113** | `transaction-service` | `SplitBillService.java` L254 | **`isFullyPaid()` dipanggil sebelum participant disimpan** — di `makePayment()`, `splitBill.isFullyPaid()` di L254 membaca dari old participants list (belum di-refresh dari DB setelah save di L252). `isFullyPaid` mungkin return salah karena ia cek stale data. | Pindahkan `splitBill.setParticipants(...)` di L266 ke sebelum `isFullyPaid()` check, atau re-fetch participants sebelum evaluasi completeness. |

---

### 🟠 High Severity — Batch 8

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-114~~ | ~~transaction-service~~ | ~~ProcessQrisPaymentCommandHandler.java L77~~ | ~~FIXED: Reference number replaced with UUID-based generation.~~ | ~~Ganti ke UUID-based.~~ |
| ✅ ~~BUG-BE-115~~ | ~~transaction-service~~ | ~~SplitBillService.java L323~~ | ~~FIXED: Reference number replaced with UUID-based generation.~~ | ~~Ganti ke UUID-based.~~ |
| **BUG-BE-116** | `transaction-service` | `ScheduledTransferService.java` L192-198 | **Scheduled transfer set FAILED permanently setelah 1x gagal** — tidak ada retry mechanism. Jika wallet insufficient saat scheduled run (misal gaji belum masuk saat subuh), transfer permanently FAILED. | Tambahkan retry count dan status `RETRY_PENDING`. Retry beberapa kali sebelum final FAILED. |
| **BUG-BE-117** | `transaction-service` | `SplitBillService.java` L82, L145 | **`canBeCancelled()` digunakan untuk authorize update DAN add participant** — method name menyesatkan. Logic seharusnya: boleh update kalau DRAFT/ACTIVE, tapi hanya boleh cancel kalau belum ada payment. | Pisahkan: `canBeModified()` untuk update/addParticipant, `canBeCancelled()` hanya untuk cancel. |
| **BUG-BE-118** | `transaction-service` | `SplitBillService.java` L277-295 | **`settleSplitBill()` ≠ actual settlement** — method set status COMPLETED tapi **tidak memproses sisa pembayaran**. Participant yang belum bayar dianggap lunas tanpa uang berpindah. Ini bukan settlement, ini force-close. | Rename ke `forceCloseSplitBill()` atau implementasi actual settlement via wallet transfer. |
| **BUG-BE-119** | `wallet-service` | `CardService.java` L28, L52, L110-115 | **`new Random()` dipakai untuk CVV dan card number** — `java.util.Random` bukan cryptographically secure. Card number dan CVV bisa diprediksi. | Ganti ke `SecureRandom` untuk semua card-related random generation. |
| **BUG-BE-120** | `auth-service` | `RiskEvaluationService.java` L74 | **MFA hardcode disabled** — `boolean mfaRequired = false; // riskScore >= mfaThreshold`. Seluruh risk evaluation engine berjalan tapi hasilnya di-override. Risk score dihitung sia-sia. | Buat configurable: `@Value("${payu.security.risk.mfa-enabled:false}")`. Jangan hardcode. |
| **BUG-BE-121** | `auth-service` | `RiskEvaluationService.java` L167 | **`isAccountActive()` pakai `mfaThreshold` sebagai lockout threshold** — `profile.getFailedAttempts() < mfaThreshold`. `mfaThreshold` (default 50) bukan lockout threshold. 50 failed attempts sebelum lock = brute force friendly. | Buat separate `@Value("${payu.security.risk.lockout-threshold:5}")`. |
| **BUG-BE-122** | `auth-service` | `BiometricService.java` L58 | **Challenge ID tidak divalidasi saat register** — di `registerBiometric()`, `challengeKey` dibangun dengan `UUID.randomUUID().toString()` (baru!), bukan challengeId dari request. Challenge store lookup selalu miss → challenge validation bypassed. | Terima `challengeId` dari request dan gunakan itu untuk lookup di `challengeStore`. |

---

### 🟡 Medium Severity — Batch 8

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| ✅ ~~BUG-BE-123~~ | ~~transaction-service~~ | ~~ScheduledTransferService.java L201-202~~ | ~~FIXED: Reference number replaced with UUID-based generation.~~ | ~~Ganti ke UUID.~~ |
| **BUG-BE-124** | `transaction-service` | `SplitBillService.java` L298-300 | **EQUAL split rounding error** — `totalAmount / participants.size()` dengan `HALF_UP`. 100.00 / 3 = 33.34 * 3 = 100.02 (off by 0.02). | Hitung sisa rounding, assign ke participant terakhir: `lastParticipant.amountOwed = total - sum(others)`. |
| **BUG-BE-125** | `wallet-service` | `CardService.java` L49 | **Expiry date `MM/yy` format** — Disimpan sebagai String, tidak di-parse saat validasi. Card dengan expiry lalu bisa tetap ACTIVE. | Tambahkan `isExpired()` check atau simpan sebagai `YearMonth` lalu validate di freeze/unfreeze flow. |
| **BUG-CROSS-019** | FE ↔ BE | `TransactionService.ts` L193 vs BE | **ScheduledTransfer `frequency` enum mismatch** — FE: `'ONCE' \| 'DAILY' \| 'WEEKLY' \| 'MONTHLY'`. BE `ScheduleType`: `ONE_TIME`, `RECURRING_DAILY`, `RECURRING_WEEKLY`, `RECURRING_MONTHLY`, `RECURRING_CUSTOM`. FE kirim `ONCE`, BE expect `ONE_TIME`. | Sinkronkan enum atau mapping di BFF proxy. |
| **BUG-CROSS-020** | FE ↔ BE | `TransactionService.ts` L221-233 vs BE | **SplitBill `status` enum mismatch** — FE: `'DRAFT' \| 'ACTIVE' \| 'SETTLED' \| 'CANCELLED'`. BE: `DRAFT`, `ACTIVE`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`. FE tidak kenal `IN_PROGRESS` dan `COMPLETED`; BE tidak kenal `SETTLED`. | Sinkronkan: FE harus handle `IN_PROGRESS` dan `COMPLETED` dari BE. |
| **BUG-CROSS-021** | FE ↔ BE | `TransactionService.ts` L228-234 vs BE | **SplitBillParticipant field mismatch** — FE `participant.name` dan `participant.amount`. BE expect `accountName`, `amountOwed`. Request params berbeda → 400 Bad Request. | Sesuaikan FE interface `name→accountName`, `amount→amountOwed`. |

---


---

## 🐛 Bug Backlog — Batch 9: BFF Proxy, WebSocket, Authorization, Archival (Feb 24, 2026)

> Areas: BFF proxy (`route.ts`), `useWebSocket.ts`, `AuthorizationService`, `TransactionArchivalService`, `SplitBillSecurityService`, `PartnerService.ts`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-FE-027** | `web-app` | `api/v1/[...path]/route.ts` L31-32 | **SSRF via path traversal** — `path.join('/')` langsung digunakan di `new URL()` tanpa sanitasi. Attacker bisa kirim `../../../internal/admin/secrets` dan BFF proxy forward ke internal endpoint yang seharusnya tidak accessible dari luar. | Sanitize `backendPath` — reject jika contains `..`, absolute paths, atau whitelist allowed prefixes. |
| **BUG-FE-028** | `web-app` | `api/v1/[...path]/route.ts` L84-92 | **GET fallback returns 503 dengan data palsu** — saat gateway offline, GET return `{data: null, items: [], total: 0}` dengan status `503`. FE service yang check `response.data` tanpa check status → render empty state bukan error state. Users think no transactions exist. | Return only 503 error, jangan campur dengan data shape. Atau FE harus check `_fallback` flag. |
| **BUG-BE-126** | `transaction-service` | `AuthorizationService.java` L91-95 | **`extractAccountIdFromUserId()` return `userId` as-is** — semua authorization check membandingkan `transaction.getSenderAccountId().toString()` dengan `userId` langsung. Jika userId = UUID dan accountId = UUID yang berbeda (multi-account), authorization selalu fail. Jika kebetulan match (single-account), ini accidental dan fragile. | Implement proper account lookup: call account-service atau parse JWT claims untuk extract accountId list. |
| **BUG-BE-127** | `transaction-service` | `SplitBillSecurityService.java` L33 | **Type mismatch `UUID` vs `UUID`** — `response.getCreatorAccountId()` return `UUID`, dibandingkan dengan `userId` param yang juga `UUID`. Tapi jika `getCreatorAccountId()` return type sebenarnya String (check SplitBillResponse.java), comparison selalu false → owner check selalu fail → no one can access. | Pastikan tipe data creatorAccountId konsisten antara response DTO dan security check. |

---

### � High Severity — Batch 9

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-FE-029** | `web-app` | `api/v1/[...path]/route.ts` L40-46 | **BFF proxy tidak forward security headers** — hanya `Content-Type`, `Accept`, `Authorization`, `X-Correlation-Id` yang di-forward. Headers penting seperti `X-Idempotency-Key`, `X-Device-Id`, `X-Client-Version` STRIPPED. Gateway-first payment ops yang butuh idempotency key → key hilang di proxy layer. | Whitelist dan forward semua `X-*` custom headers. |
| **BUG-FE-030** | `web-app` | `useWebSocket.ts` L62-73 | **WebSocket reconnect tanpa backoff** — reconnect selalu setelah 3 detik flat. Jika server down lama, client spam reconnect setiap 3 detik indefinitely. Juga, reconnect di `onclose` bikin WebSocket baru tapi **tidak reuse event handlers dari closure** — closure captured old callbacks. | Implementasi exponential backoff dengan max retries. Gunakan `connect()` function untuk reconnect agar handlers fresh. |
| **BUG-FE-031** | `web-app` | `useWebSocket.ts` L85 | **`ws` return value selalu `null`** — `return { ws: null as unknown as WebSocket }`. Consumer yang akses `ws.readyState` atau `ws.send()` → runtime error. | Return `wsRef.current` atau wrap dalam getter. |
| **BUG-BE-128** | `transaction-service` | `TransactionArchivalService.java` L67, L79 | **Archive + delete dalam satu transaksi** — jika `deleteArchivedTransactions()` gagal setelah `archiveTransactions()` sukses, rollback menghapus archive tapi transaksi asli juga di-rollback? Tergantung isolation level. Jika merge-commit dan partial fail → data loss. | Pisahkan: archive batch A → verify → delete batch A. Atau gunakan soft-delete pattern (set `archived=true`) lalu cleanup later. |
| **BUG-BE-129** | `transaction-service` | `TransactionArchivalService.java` L66 | **Infinite loop jika `findTransactionsToArchive` selalu return same data** — while(true) loop query ulang setelah delete. Jika delete gagal (silently) → query return batch yang sama → infinite loop. | Tambahkan max iterations guard dan verify rowcount dari delete. |
| **BUG-FE-032** | `web-app` | `PartnerService.ts` L9-10 | **`clientSecret` exposed di FE** — `Partner` interface punya `clientSecret?: string`. BFF proxy meneruskan ini dari backend ke browser. Client secret TIDAK BOLEH ada di frontend code. | Hapus `clientSecret` dari FE interface. Backend harus strip field ini di response (kecuali saat registration). |
| **BUG-FE-033** | `web-app` | `PartnerService.ts` L146-148 | **SNAP-BI token via FE** — `getSnapBiToken(clientId, clientSecret)`. FE mengirim clientId + clientSecret ke backend melalui browser. Credentials di-expose di browser network tab. | SNAP-BI token harus diminta server-side only. Partner service harus handle token exchange di backend tanpa FE involvement. |

---

### �🟡 Medium Severity — Batch 9

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-FE-034** | `web-app` | `useWebSocket.ts` L75 | **`connect` dependency array bloated** — semua callbacks (`onMessage`, `onError`, etc.) dalam deps. Jika consumer tidak `useCallback` wrap → infinite reconnect loop. | Gunakan refs untuk callbacks: `const onMessageRef = useRef(onMessage)`. |
| **BUG-FE-035** | `web-app` | `useBiometric.ts` L9-12 | **`useBiometricChallenge` sebagai `useQuery` dengan `enabled: false`** — idiomatic TanStack Query untuk manual triggers adalah `useMutation`, bukan disabled query. Disabled query != on-demand fetch. `refetch()` dari disabled query masih cache dan stale logic apply. | Ubah ke `useMutation` untuk challenge generation yang truly on-demand. |
| **BUG-BE-130** | `transaction-service` | `TransactionArchivalService.java` L49 | **`ZonedDateTime.now()` tanpa explicit timezone** — behavior tergantung JVM timezone. Di container yang timezone = UTC vs Jakarta → cutoff date beda 7 jam. Transaction bisa ter-archive prematur atau terlambat. | Gunakan `ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))` atau `Instant.now().minus(retentionMonths, ChronoUnit.MONTHS)`. |
| **BUG-FE-036** | `web-app` | `useInvestments.ts` L27, L35, L43, L51, L59 | **Buy mutation `onSuccess` invalidate hanya `investment-account`** — setelah beli deposit/mutual fund/gold, cache `gold-holdings` dan query lain tidak di-invalidate. Halaman gold holdings show stale data. | Tambahkan `qc.invalidateQueries({ queryKey: ['gold-holdings'] })` di relevant mutations. |
| **BUG-CROSS-022** | FE ↔ BE | `useSplitBill.ts` + `SplitBillService.java` | **SplitBill mutations tidak invalidate wallet balance** — setelah `makePayment` sukses, `wallet-balance` query key tidak di-invalidate. Dashboard balance stale. | Tambahkan `qc.invalidateQueries({ queryKey: ['wallet-balance'] })` di `useSplitBillPayment.onSuccess`. |
| **BUG-CROSS-023** | FE ↔ BE | `useLending.ts` + `LendingService.ts` | **Loan application `onSuccess` invalidate hanya `['loan']`** — harusnya juga invalidate `['credit-score']` dan `['wallet-balance']` karena loan disbursement affects balance. | Tambahkan query key invalidations yang relevan. |

---


---

## 🐛 Bug Backlog — Batch 10: Controllers & API Security (Feb 24, 2026)

> Areas: `WalletController`, `CardController`, `TransactionController`, `SnapBiController`, `OnboardingController`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-131** | `wallet-service` | `CardController.java` L36-91 | **🔒 Tidak ada `@PreAuthorize` atau ownership check pada card endpoints** — semua card operations (create, freeze, unfreeze, list) tidak ada authorization. Siapapun yang terautentikasi bisa freeze/unfreeze card orang lain hanya dengan mengetahui cardId. | Tambahkan `@PreAuthorize` dan ownership validation sama seperti WalletController. |
| **BUG-BE-132** | `wallet-service` | `WalletController.java` L103-127 | **🔒 Commit/release reservation tanpa ownership check** — Endpoint `POST /reservations/{reservationId}/commit` dan `/release` tidak punya `@PreAuthorize`. Siapapun bisa commit/release reservation orang lain. | Tambahkan ownership validation: lookup reservation → check wallet → verify accountId matches authenticated user. |
| **BUG-BE-133** | `wallet-service` | `CardController.java` L93-103 | **🔒 Full card number exposed di API response** — `toCardResponse()` mengirim `card.getCardNumber()` (16 digit lengkap) ke client. PCI-DSS violation — hanya 4 digit terakhir yang boleh ditampilkan. | Mask card number: return `"**** **** **** " + cardNumber.substring(12)`. |
| **BUG-BE-134** | `partner-service` | `SnapBiController.java` L62-101 | **🔒 SNAP-BI auth: no replay attack protection** — `X-TIMESTAMP` diterima apa adanya tanpa validasi window. Attacker bisa re-use old valid signature. Juga `X-EXTERNAL-ID` tidak dicek uniqueness (hanya di `createPayment` header declaration, not validated). | Validasi timestamp window ±5 menit. Enforce `X-EXTERNAL-ID` uniqueness dengan idempotency store. |
| **BUG-BE-135** | `transaction-service` | `TransactionController.java` L172-184 | **🔒 Domain model `Transaction` langsung di-return via API** — `ApiResponse<Transaction>`. Domain entity bisa mengandung internal fields (audit timestamps, internal status, database IDs) yang tidak boleh exposed. | Buat `TransactionResponse` DTO dan mapping, sama seperti yang sudah dilakukan di `WalletController` (pakai `BalanceResponse`). |

---

### 🟠 High Severity — Batch 10

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-136** | `transaction-service` | `TransactionController.java` L216, L247-250 | **Account transaction list tanpa ownership validation** — `@PreAuthorize("hasAuthority('read:transaction')")` cek authority tapi tidak cek apakah `accountId` milik user. Anyone with `read:transaction` authority bisa query semua account transactions. | Tambahkan SpEL ownership check: `#accountId == authentication.principal.accountId` atau gunakan `AuthorizationService`. |
| **BUG-BE-137** | `transaction-service` | `TransactionController.java` L253 | **Pagination TODO: `List<Transaction>` tanpa pagination metadata** — method accepts `page`/`size` params tapi return `List`, bukan `Page`. FE tidak tahu total pages/items. | Return `Page<Transaction>` dan include `PaginationInfo` di response. |
| **BUG-BE-138** | `partner-service` | `SnapBiController.java` L37 | **Controller langsung akses `PartnerRepository`** — melanggar Hexagonal Architecture. Controller → Repository langsung, bypass domain/application layer. | Inject `PartnerService` (use case port), bukan `PartnerRepository`. |
| **BUG-BE-139** | `partner-service` | `SnapBiController.java` L78-93 | **Signature validation: serialize request lalu compare** — `objectMapper.writeValueAsString(request)` re-serialize request yang sudah di-deserialize dari JSON. Jika field ordering atau whitespace berbeda dari original request → signature mismatch yang legitimate. | Gunakan raw request body (`@RequestBody String rawBody` + manual parsing) untuk signature validation, kemudian parse ke DTO. |
| **BUG-BE-140** | `account-service` | `OnboardingController.java` L43-45 | **`CompletableFuture<ResponseEntity<User>>` return type** — async response tanpa timeout. Jika `registerUser` hangs → request hangs indefinitely. Juga, domain model `User` langsung di-return (mungkin contain password hash). | Tambahkan `.orTimeout(10, TimeUnit.SECONDS)`. Create `RegisterUserResponse` DTO tanpa sensitive fields. |
| **BUG-BE-141** | `wallet-service` | `WalletController.java` L56 | **Account ID logged tanpa masking** — `log.info("Getting balance for account: {}", accountId)`. Di production, accountId bisa PII. | Gunakan masked log: `log.info("Getting balance for account: {}", maskAccountId(accountId))`. |
| **BUG-BE-142** | `wallet-service` | `WalletController.java` L169 | **`UUID.fromString(accountId)` tanpa try-catch** — jika accountId bukan valid UUID → 500 error. Harusnya return 400. | Wrap dalam try-catch atau gunakan custom validator `@ValidUUID`. |

---

### 🟡 Medium Severity — Batch 10

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-143** | `transaction-service` | `TransactionController.java` L65-73 | **`extractUserId()` repeat pattern** — setiap method call `extractUserId()` manually. Error-prone jika developer lupa. | Inject userId via Spring `@AuthenticationPrincipal` parameter annotation. |
| **BUG-BE-144** | `transaction-service` | `TransactionController.java` L128-144, L310-327 | **Generic exception catch returns 500** — `catch (Exception e)` logs stacktrace tapi returns generic error. Loses specific error info yang berguna untuk debugging. | Biarkan `@ControllerAdvice` / `GlobalExceptionHandler` handle exceptions secara uniform. Hapus try-catch di controller. |
| **BUG-BE-145** | `wallet-service` | `CardController.java` L70 | **`ResponseEntity.notFound().build()` tanpa `ApiResponse` wrapper** — semua endpoint lain return `ApiResponse<>`, tapi not-found return bare 404. FE parsing inconsistent. | Return `ResponseEntity.status(404).body(ApiResponse.error("CARD_NOT_FOUND", "Card not found"))`. |
| **BUG-BE-146** | `partner-service` | `SnapBiController.java` L259-272 | **`SnapErrorResponse` inner class** — error response class defined as inner class di controller. Tidak reusable dan visibility issues (public fields tapi private class). | Extract ke separate file `SnapErrorResponse.java` dengan proper encapsulation. |

---

---

## 🐛 Bug Backlog — Batch 11 (Final): Remaining Controllers, FE Services, Auth Gaps (Feb 24, 2026)

> Areas: `LendingController`, `InvestmentController`, `BillingController`, `BackofficeController`, `AuthController`, FE `AuthService.ts`, `InvestmentService.ts`

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-147** | `lending-service` | `LendingController.java` L55-83 | **🔒 Loan application endpoint tanpa `@PreAuthorize`** — siapapun bisa apply loan atas nama user lain karena userId dari request body, bukan dari JWT. No ownership check. | Tambahkan `@PreAuthorize` dan extract userId dari JWT (bukan request body). |
| **BUG-BE-148** | `lending-service` | `LendingController.java` L158-177 | **🔒 PayLater activate tanpa ownership check** — `userId` dari `@RequestParam`, siapapun bisa activate PayLater untuk user lain. Juga, `createRepaymentSchedule` (L101) dan `processRepayment` (L144) tanpa `@PreAuthorize`. | Tambahkan ownership validation via `@PreAuthorize` dan extract userId dari JWT. |
| **BUG-BE-149** | `investment-service` | `InvestmentController.java` L30-157 | **🔒 Seluruh InvestmentController tanpa `@PreAuthorize` atau `@SecurityRequirement`** — semua endpoints (buy deposit, buy mutual fund, buy gold, sell, get account) tidak ada auth check. Siapapun bisa invest/sell atas nama user lain. | Tambahkan `@SecurityRequirement` dan `@PreAuthorize` ownership checks. |
| **BUG-BE-150** | `investment-service` | `InvestmentController.java` L66-69, L88-91, L109-110, L129-131 | **Financial parameters via `@RequestParam` bukan `@RequestBody`** — amount, userId, accountId dikirim via query params. Query params logged di web server access logs, proxy logs, browser history. PII & financial data exposed di plaintext. | Pindahkan ke `@RequestBody` DTO (e.g., `BuyDepositRequest`, `BuyGoldRequest`). |
| **BUG-BE-151** | `backoffice-service` | `BackofficeController.java` L223-227, L363-366, L453-456 | **🔒 Admin identity fallback ke `"system"`** — jika `X-Admin-User` header absent, adminUser = `"system"`. Audit trail rusak — semua actions tanpa header terlihat dilakukan oleh "system". No accountability. | Wajibkan header atau extract dari JWT `authentication.principal`. |

---

### 🟠 High Severity — Batch 11

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-152** | `lending-service` | `LendingController.java` L195-223 | **PayLater purchase tanpa ownership validation** — `userId` dari path, `merchantName` dan `amount` dari `@RequestParam`. Siapapun bisa record purchase untuk user lain. Sama untuk `recordPayment` (L225). | Buat proper DTO `@RequestBody` dan tambahkan ownership `@PreAuthorize`. |
| **BUG-BE-153** | `lending-service` | `LendingController.java` L260-268 | **Credit score calculation endpoint open** — `calculateCreditScore()` tanpa `@PreAuthorize`. Siapapun bisa trigger credit score calculation untuk user manapun via `@RequestParam userId`. | Tambahkan `@PreAuthorize` ownership check. |
| **BUG-BE-154** | `auth-service` | `AuthController.java` L112-124, L143-146 | **Login: password dikirim 2x ke Keycloak** — `validateCredentialsBlocking()` pertama untuk validation, lalu `loginBlocking()` lagi dengan password yang sama. Double network call, double exposure of credentials. | Gabungkan: langsung call `loginBlocking()`, handle invalid credentials dari response. |
| **BUG-BE-155** | `auth-service` | `AuthController.java` L117-123 | **Failed login tidak increment risk counter** — setelah `isValid == false`, langsung return error. `riskEvaluationService.recordFailedAttempt()` tidak dipanggil. Failed attempts counter tetap 0. Brute force undetected. | Tambahkan `riskEvaluationService.recordFailedAttempt(request.username())` sebelum return error. |
| **BUG-BE-156** | `backoffice-service` | `BackofficeController.java` L159, L325, L415 | **List endpoints return raw `List<>` tanpa `ApiResponse` wrapper** — `listKycReviews()`, `listFraudCases()`, `listCustomerCases()` return `List<Response>` langsung, bukan `ResponseEntity<ApiResponse<List>>`. Response format inconsistent dengan endpoints lain. | Wrap dalam `ResponseEntity<ApiResponse<List<>>>` untuk consistency. |
| **BUG-BE-157** | `backoffice-service` | `BackofficeController.java` L176, L335, L425 | **Enum `valueOf()` tanpa error handling** — `KycStatus.valueOf(status.toUpperCase())` bisa throw `IllegalArgumentException` jika status invalid. Return 500 bukan 400. | Wrap dalam try-catch, return 400 dengan message "Invalid status: ...". |
| **BUG-BE-158** | `backoffice-service` | `BackofficeController.java` L262 | **Fraud case create pakai `APPLICATION_FORM_URLENCODED`** — satu-satunya endpoint yang pakai form-encoded bukan JSON. Inconsistent dengan semua endpoint lain. Juga, 9 `@RequestParam` di method signature = code smell. | Buat `CreateFraudCaseRequest` DTO JSON body seperti endpoint lain. |
| **BUG-BE-159** | `billing-service` | `PaymentController.java` L62, L77 | **🔒 Bill payment lookup tanpa ownership check** — `getPayment()` dan `getPaymentByReference()` hanya check `isAuthenticated()`. Siapapun bisa query payment details orang lain dengan known ID/reference. | Tambahkan ownership validation: check if payment.accountId matches authenticated user. |

---

### 🟡 Medium Severity — Batch 11

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-FE-037** | `web-app` | `AuthService.ts` L73 vs `api/v1/[...path]/route.ts` | **Login endpoint mismatch** — FE AuthService calls `/api/auth/login` (tanpa `v1`). BFF proxy hanya handle `/api/v1/*`. Login request tidak proxy ke backend. | Sesuaikan: FE harus panggil `/api/v1/auth/login` atau buat dedicated BFF auth route. |
| **BUG-FE-038** | `web-app` | `AuthService.ts` L94-95, L154 | **Logout/refresh endpoint mismatch** — FE calls `/api/auth/logout` dan `/api/auth/refresh` tanpa `v1`. Sama problem dengan login. | Pindahkan ke `/api/v1/auth/*` atau buat route handler tersendiri. |
| **BUG-CROSS-024** | FE ↔ BE | `InvestmentService.ts` L122-136 vs BE | **FE buy operations kirim request body, BE expect query params** — FE `api.post('/investments/deposits', request)` kirim JSON body. BE `@RequestParam amount` expect query string params. Request selalu gagal → 400. | Sync: either FE switch ke query params, atau (lebih baik) BE switch ke `@RequestBody`. |
| **BUG-CROSS-025** | FE ↔ BE | `InvestmentService.ts` L140-142 vs BE | **Sell investment params mismatch** — FE: `{investmentId, amount}`. BE: `@RequestParam accountId, @RequestParam transactionId, @RequestParam amount`. FE tidak kirim `accountId`, kirim `investmentId` bukan `transactionId`. | Sinkronkan field names dan required params. |
| **BUG-FE-039** | `web-app` | `AuthService.ts` L53-54, L123-124 | **`authenticated` flag = client-side state** — flag di-set manual. Jika page refresh → `authenticated = false` meskipun httpOnly cookie masih valid. `isAuthenticated()` bukan reliable. | Cek auth state via `validateSession()` saat init, atau hapus client-side flag entirely — rely on server response. |
| **BUG-BE-160** | `lending-service` | `LendingController.java` L67 | **`CompletableFuture` tanpa timeout** — `applyLoan()` return `CompletableFuture` tanpa `.orTimeout()`. Request bisa hang indefinitely. | Tambahkan `.orTimeout(30, TimeUnit.SECONDS)`. |
| **BUG-BE-161** | `investment-service` | `InvestmentController.java` L46-49, L65-72, etc. | **`CompletableFuture` tanpa timeout (semua endpoints)** — semua investment endpoints return `CompletableFuture` tanpa timeout. Pattern sama dengan BUG-BE-160. | Tambahkan `.orTimeout()` ke semua async endpoints. |
| **BUG-BE-162** | `backoffice-service` | `BackofficeController.java` L482, L503 | **Universal search SQL injection risk** — `search(query, entityType)` langsung terima user input `query`. Tergantung implementasi `UniversalSearchService`, jika pakai native SQL query → SQL injection. | Verify `UniversalSearchService` pakai parameterized queries. Add input validation/sanitization. |

---


---

## 🐛 Bug Backlog — Batch 12 (True Final): SecurityConfig, CORS, Middleware, Remaining (Feb 24, 2026)

> Areas: SecurityConfig files (semua services), `middleware.ts`, `lib/api.ts`, `lib/validation.ts`, `PartnerController`, `ComplianceAuditController`, FE services

---

### 🔴 Critical / P0

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-163** | `partner-service` | `SecurityConfig.java` L47 | **🔒 CORS `allowedOrigins("*")` — allow ALL origins** — wildcard CORS di payment gateway service. Any website bisa make cross-origin requests ke partner API. Combined with SNAP-BI token (yang juga vulnerable), ini critical. | Set specific allowed origins atau gunakan env config: `List.of("https://payu.co.id", "https://partner.payu.co.id")`. |
| **BUG-BE-164** | `partner-service` | `PartnerController.java` L30, L38-63, L93-99, L140-147 | **🔒 PartnerController tanpa `@PreAuthorize`** — seluruh CRUD partner (create, read all, read by id, update, delete, regenerate keys) TANPA authorization check. `@SecurityRequirement` hanya OpenAPI decoration, bukan enforcement. Siapapun terautentikasi bisa manage semua partners. | Tambahkan `@PreAuthorize("hasRole('ADMIN')")` di setiap endpoint. |
| **BUG-BE-165** | `partner-service` | `PartnerController.java` L226-231 | **🔒 `regenerateKeys()` return client secret di response** — setelah regenerate, DTO penuh (termasuk clientSecret) dikembalikan. Secret di-expose di network. Juga tidak ada rate limit — attacker bisa spam regenerate untuk invalidate partner credentials. | Hanya return masked secret (first 4 chars + ***). Tambahkan rate limit. |
| **BUG-FE-040** | `web-app` | `middleware.ts` L25-27 | **🔒 Auth check HANYA berdasarkan cookie existence** — `request.cookies.has('refreshToken')`. Cookie bisa exist tapi expired/invalid. Middleware tidak validate cookie value. | Ini acceptable untuk Edge middleware (no DB access), tapi perlu tambahan server-side validation di BFF proxy. Document limitation ini. |

---

### 🟠 High Severity — Batch 12

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-166** | `auth-service` | `SecurityConfig.java` L36-38 | **MFA verify endpoint TIDAK di-list sebagai public** — `PUBLIC_ENDPOINTS` hanya `login`, `register`, `refresh`, dll. Endpoint `POST /api/v1/auth/mfa/verify` require JWT (filter chain Order 3) → tapi user belum punya JWT saat MFA! Login flow broken. | Tambahkan `/api/v1/auth/mfa/verify` ke `PUBLIC_ENDPOINTS`. |
| **BUG-BE-167** | `auth-service` | `SecurityConfig.java` L119 | **JwtDecoder pakai `System.getenv()` bukan `@Value`** — tidak bisa override di `application.yml` test profiles. Environment-specific config hardcoded. | Gunakan `@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")`. |
| **BUG-BE-168** | `compliance-service` | `ComplianceAuditController.java` L44-46 | **Mutable service field via public setter** — `setComplianceAuditService()` public setter memungkinkan service diganti at runtime. Hapus field mutability. | Hapus setter, buat field `final`, inject via constructor. |
| **BUG-BE-169** | `compliance-service` | `ComplianceAuditController.java` L117 | **`IllegalArgumentException` thrown tanpa `@ExceptionHandler`** — `throw new IllegalArgumentException("At least one search parameter is required")` → 500 response. | Buat custom `BadRequestException` atau handle di `@ControllerAdvice`. |
| **BUG-FE-041** | `web-app` | `lib/api.ts` L63, L68 | **Refresh endpoint path mismatch** — interceptor calls `/api/auth/refresh`, tapi BFF proxy hanya handle `/api/v1/*`. Refresh always fails → redirect ke login → infinite redirect loop jika user punya valid refreshToken cookie. | Sinkronkan: `/api/v1/auth/refresh` atau buat dedicated route. Sama issue dengan BUG-FE-037/038. |
| **BUG-FE-042** | `web-app` | `lib/api.ts` L50, L58 | **Race condition: `_retry` flag on config object** — `originalRequest._retry = true` modifies shared config. Jika axios reuses config object (interceptor re-fires), flag bisa sudah set → skip refresh → silent failure. | Gunakan WeakSet untuk track retried requests: `const retriedRequests = new WeakSet()`. |
| **BUG-CROSS-026** | FE ↔ BE | `BillingService.ts` L53 vs `PaymentController.java` | **Billing FE path mismatch** — FE calls `/billing/payments` → BFF proxy to `/billing/payments`. BE `PaymentController` mounted di `/api/v1/payments` (tanpa `/billing/`). Requests always 404. | Sinkronkan path antara FE service dan BE controller. |
| **BUG-CROSS-027** | FE ↔ BE | `AccountService.ts` L9 vs `OnboardingController.java` | **FE sends `nik` di registration request** — `RegisterUserRequest` FE includes `nik`. Jika nik sampai ke BE dan tidak di-mask/encrypt → PII compliance violation. Backend HARUS mask di logs dan encrypt di DB. | Verify `@Sensitive` annotation di `nik` field dan server-side encryption via `security-starter`. |
| **BUG-FE-043** | `web-app` | `LendingService.ts` L130-134 | **PayLater purchase kirim merchantName & amount via query params** — `params: { merchantName, amount, description }`. Financial data (amount) exposed di URL dan server logs. Sama issue dengan BE yang juga pakai `@RequestParam`. | Ubah ke `@RequestBody` di kedua sisi (BE first, lalu FE). |

---

### 🟡 Medium Severity — Batch 12

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-FE-044** | `web-app` | `lib/validation.ts` L427 | **`parseFloat` untuk currency amounts** — `parseFloat(amount.replace(...))` bisa produce floating point errors (e.g., `0.1 + 0.2 ≠ 0.3`). Di financial app ini bisa cause rounding discrepancies. | Gunakan integer arithmetic (simpan dalam smallest unit — sen/cents) atau library decimal (e.g., `decimal.js`). |
| **BUG-FE-045** | `web-app` | `lib/validation.ts` L89-101 | **Email domain typo detection blocks valid domains** — `.co` domains (e.g., `user@company.co`) valid tapi di-reject karena typo detection. `gmail.co` → suggest `gmail.com`, tapi `company.co` bukan typo. | Hanya suggest, jangan block — set `isValid: true` tapi tambahkan `suggestion` field. |
| **BUG-FE-046** | `web-app` | `middleware.ts` L60 | **Route match logic too broad** — `publicRoutes.some(route => pathWithoutLocale.startsWith(route))`. `/login-debug`, `/onboarding-secret`, `/legal/privacy-backdoor` semua match. | Gunakan exact match atau match dengan trailing `/`: `pathWithoutLocale === route || pathWithoutLocale.startsWith(route + '/')`. |
| **BUG-BE-170** | all services | `SecurityConfig.java` (multiple) | **`EnableMethodSecurity` missing di sebagian besar services** — `@PreAuthorize` hanya berfungsi jika `@EnableMethodSecurity` aktif. Hanya `partner-service` yang punya. Service lain pakai `@PreAuthorize` tapi mungkin tidak enforced. | Tambahkan `@EnableMethodSecurity` di semua SecurityConfig yang punya `@PreAuthorize` endpoints. |
| **BUG-BE-171** | `wallet-service`, `transaction-service`, `auth-service` | `SecurityConfig.java` (multiple) | **`SecurityContextPersistenceFilter` deprecated** — `addFilterBefore(..., SecurityContextPersistenceFilter.class)`. Filter ini deprecated sejak Spring Security 6.0. Gunakan `SecurityContextHolderFilter.class`. | Ganti reference ke `SecurityContextHolderFilter`. |
| **BUG-FE-047** | `web-app` | `lib/currency.ts` L281-282 | **`roundCurrency()` pakai `Math.round(amount * multiplier) / multiplier`** — floating point arithmetic. `Math.round(1.005 * 100) / 100 = 1.00` bukan `1.01`. | Gunakan `Number((amount).toFixed(decimals))` atau integer-based rounding. |

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

