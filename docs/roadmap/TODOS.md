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
| Backend Logic | 14 | 26 | 18 | 5 | **63** |
| Frontend Logic | 4 | 5 | 12 | 5 | **26** |
| Frontend-Backend Mismatch | 8 | 7 | 3 | — | **18** |
| Auth / Session | 2 | 2 | 3 | 3 | **10** |
| **TOTAL** | **28** | **40** | **36** | **13** | **~117** |

> ⚠️ **Catatan**: Scorecard "Production Readiness 100/100" di PROGRESS.md mencerminkan infra/deploy coverage,
> **bukan** correctness business logic. Bug di bawah ini adalah temuan dari code review mendalam (Feb 24, 2026).

---

## 🔴 Priority Fix List (P0 — Must Fix Before Any Integration)

> Fix items ini sebelum TokoBapak / Nobar mulai integrasi!

| ID | Service | Issue | Impact |
| :--- | :--- | :--- | :--- |
| **BUG-BE-001** | `gateway-service` | **JWT validation adalah PLACEHOLDER** — siapapun dengan token ≥10 karakter masuk | Seluruh platform tidak aman |
| **BUG-BE-002** | `auth-service` | In-memory `failedAttempts`, `tokenStore`, `otpStore`, `challengeStore` — multi-pod tidak sync | MFA/brute-force protection gagal di scale-out |
| **BUG-BE-035** | `partner-service` | In-memory `tokenStore` SNAP-BI — token tidak persistent antar pod | Partner integration gagal di HPA |
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
| **BUG-BE-001** | `gateway-service` | `AuthorizationFilter.java` L154-184 | **JWT validation adalah PLACEHOLDER** — `validateToken()` hanya cek `token.length() < 10`. Siapapun dengan token ≥10 karakter masuk sebagai user random. | Implementasi JWT validation benar via Quarkus OIDC atau `nimbus-jose-jwt`. Verifikasi signature dari Keycloak JWKS. |
| **BUG-BE-002** | `auth-service` | `KeycloakService.java` L45 + `MFATokenService.java` L17-18 | **In-memory state di scaled environment** — `failedAttempts`, `tokenStore`, `otpStore`, `challengeStore` di `ConcurrentHashMap`. Multi-pod (HPA min 2): state pod A ≠ pod B. | Pindahkan semua state ke Redis via `CacheService`. |
| **BUG-BE-003** | `transaction-service` | `InitiateTransferCommandHandler.java` L164-166 | **Reference number generator collision-prone** — `"TXN" + currentTimeMillis() + random(1000)`. Bug sama di 5 titik lain. | Ganti ke `UUID.randomUUID()`. |
| **BUG-BE-004** | `wallet-service` | `WalletService.java` L47-54 | **Cache invalidation tidak complete** — `reserveBalance` tidak invalidate `wallet:id:` cache key. | Tambah `cacheService.invalidate("wallet:id:" + wallet.getId())` di semua mutasi. |

---

### 🟠 High Severity — Batch 1

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-005** | `auth-service` | `KeycloakService.java` L89 | **Token plaintext di-log** — `log.info(..., jsonResponse)` cetak `access_token` + `refresh_token` ke log. | Hapus log ini, atau log hanya status sukses/gagal. |
| **BUG-BE-006** | `gateway-service` | `AuthorizationFilter.java` L37 | **`/api/v1/accounts` sepenuhnya public** — Semua path prefix ini skip JWT validation. Hanya `/register` yang harusnya public. | Hapus prefix `/api/v1/accounts` dari `PUBLIC_ENDPOINTS`, ganti dengan `/api/v1/accounts/register` exact. |
| **BUG-BE-007** | `transaction-service` | `InitiateTransferCommandHandler.java` L79-81 | **Non-BIFAST transfer tidak diproses** — `INTERNAL_TRANSFER`, `SKN`, `RTGS` create DB record di status `VALIDATING` tapi tidak pernah diproses. | Tambahkan processing branch per transfer type. |
| **BUG-BE-008** | `wallet-service` | `WalletService.java` L162-163 | **Type mismatch**: `accountId` String di-cast ke UUID → `IllegalArgumentException` runtime. | Standardisasi: pilih satu, `accountId` selalu UUID atau selalu String. |
| **BUG-BE-009** | `lending-service` | `LoanManagementService.java` L103-130 | **Repayment schedule calculation error** — installment terakhir pakai `monthlyInstallment` bukan `outstandingPrincipal + interest`. | Pada last installment: `installmentAmount = outstandingPrincipal + interestAmount`. |
| **BUG-BE-010** | `auth-service` | `KeycloakService.java` L199-215 | **`Mono.block()` di Spring MVC thread** — Blocking WebFlux di Tomcat thread pool → thread starvation under load. | Ganti ke synchronous `RestTemplate` atau migrasi ke WebFlux. |
| **BUG-BE-011** | `transaction-service` | `ScheduledTransferScheduler.java` L22 | **`@Scheduled` tanpa distributed lock** — Multi-pod: semua pod proses transfer yang sama bersamaan. | Tambahkan distributed lock via Redis (`ShedLock` atau custom). |

---

### 🟡 Low Severity — Batch 1

| ID | Service | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-BE-012** | `promotion-service` | `ReferralService.java` L184 — `Math.random()` untuk referral code, potential collision. | Gunakan `SecureRandom`. |
| **BUG-BE-013** | `wallet-service` | `createWallet` query `findByAccountId` dua kali jika wallet sudah ada. | Gunakan result dari cek pertama. |
| **BUG-BE-014** | `lending-service` | `processRepayment` tidak `@Transactional`. | Tambahkan `@Transactional`. |
| **BUG-BE-015** | `transaction-service` | Komentar TODO: pagination info tidak dikembalikan ke client. | Implementasi `Page<Transaction>` return. |
| **BUG-BE-016** | `auth-service` | Username (PII) di-log saat sukses login. | Mask atau hash username di log. |
| **BUG-BE-017** | `gateway-service` | `authHeader` (mengandung Bearer token) di-log di INFO level. | Hapus atau turunkan ke DEBUG. |

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
| **BUG-BE-019** | `shared/security-starter` | `EncryptionService.java` L263 | **PBKDF2 salt hardcoded** di source code — sama semua environment. | Jadikan configurable via env var `${payu.security.encryption.salt}`. |
| **BUG-BE-020** | `account-service` | `UserApplicationService.java` L35-36 | **`@Transactional` + `@Async` anti-pattern** — `@Transactional` tidak efektif di thread async. Bug sama di `InvestmentApplicationService.java`. | Pisahkan: sync untuk DB ops, async hanya untuk event publishing. |

---

### 🟠 High Severity — Batch 2

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-021** | `investment-service` | `InvestmentApplicationService.java` L115 | **No saga compensation** — `deductBalance` sukses tapi `saveDeposit` gagal → uang hilang tanpa deposit tersimpan. | Implementasikan saga: jika save gagal, `creditBalance()` rollback. |
| **BUG-BE-022** | `investment-service` | Multiple files | Reference number `"DEP-" + currentTimeMillis()` collision-prone. | Ganti ke UUID-based. |
| **BUG-BE-023** | `fx-service` | `FxRateService.java` L59-61 | `updateRates()` abort semua jika satu currency error. | Catch exception per-currency, lanjutkan ke berikutnya. |
| **BUG-BE-024** | `fx-service` | `FxConversionService.java` L27-35 | **FX conversion tidak pernah gerakkan wallet** — status PENDING dibuat tapi tidak ada debit/kredit. | Integrasikan dengan wallet reservation flow. |
| **BUG-BE-025** | `notification-service` | `NotificationService.java` L75 | `retryCount++` tanpa retry logic — notifikasi FAILED tidak pernah dicoba ulang. | Implementasi retry scheduler untuk FAILED notifications. |
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
| **BUG-BE-033** | `backoffice-service` | `SecurityConfig.java` L46 | **CORS wildcard di admin service** — `allowedOrigins("*")`. Internal admin tool seharusnya paling strict. | Ganti dengan `List.of("https://backoffice.payu.id")`. |
| **BUG-BE-034** | `support-service` | Seluruh controller | **No role-based authorization** — `.authenticated()` saja, user biasa bisa akses semua endpoint termasuk create agent, delete module. | Tambahkan `@PreAuthorize("hasRole('SUPPORT_MANAGER')")` pada endpoint sensitif. |
| **BUG-BE-035** | `partner-service` | `SnapBiTokenService.java` L31 | **Partner token store in-memory** — token pod A tidak dikenali pod B. Revoke tidak berlaku cross-pod. | Pindahkan `tokenStore` ke Redis dengan TTL. |
| **BUG-BE-036** | `partner-service` | `SnapBiTokenService.java` L115 | **`cleanupExpiredTokens()` tidak pernah dijadwalkan** — memory leak token expired. | Tambahkan `@Scheduled(fixedRate = 60000)`. |

---

### 🟠 High Severity — Batch 3

| ID | Service | File | Bug / Logic Issue | Solusi |
| :--- | :--- | :--- | :--- | :--- |
| **BUG-BE-037** | `billing-service` | `PaymentService.java` L69 | **Biller processing adalah mock** — selalu set `COMPLETED` tanpa panggil biller API. Balance terpotong, tagihan tidak dibayar. | Implementasi adapter per-biller (PLN, PDAM, dll.) atau set `PROCESSING` + callback. |
| **BUG-BE-038** | `billing-service` | `BillPayment.java` L85 | Reference number `"BILL" + currentTimeMillis() + random(1000)` collision-prone. | Ganti ke UUID-based. |
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
| **BUG-BE-077** | `api-portal-service` | `SandboxService.java` L34 | Reference number `"PAY" + currentTimeMillis() + random(1000)` — collision guaranteed di load test concurrent. | Ganti ke `UUID.randomUUID()`. |
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

### 🟡 Deferred (Diprioritaskan Nanti)

| ID | Description | Status |
| :--- | :--- | :--- |
| **P2-FE-003** | Mobile App Feature Parity (Expo/React Native) | Deferred |
| **OCP-007** | Service Mesh mTLS enforcement | Planned |
| **OCP-010** | API versioning headers | Planned |
| **DR-001** | Disaster Recovery live test execution | Scripts ready, pending execution |

---

_Last Updated: February 24, 2026 | Bug review session complete_
