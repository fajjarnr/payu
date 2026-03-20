# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| Status           | Count | Breakdown                                             |
| :--------------- | :---: | :---------------------------------------------------- |
| **Active Epics** |   0   | All completed ✅                                      |
| **Open Stories** |   0   | All completed ✅ (archived to CHANGELOG)               |
| **Tech Debt**    |   0   | All completed ✅                                      |
| **Spikes**       |   5   | ARCH-001 – ARCH-005                                   |
| **Deferred**     |   9   | P2-FE-003, OCP-007, OCP-010, DR-001, DEFER-001, RHPAM |
| **Open Bugs**    |  26   | Temuan Logical, Security & Architecture (March 2026)  |

> **Completed Epics**: 24/24 fully done. All stories & tech debt cleared.
> See [`PROGRESS.md`](./PROGRESS.md) for completed Epics summary.
> **Closed bugs, stories & history**: See [`CHANGELOG.md`](../../CHANGELOG.md).

### 🐛 Open Bug Scorecard

| Kategori                   | Open | Priority Range |
| :------------------------- | :--: | :------------- |
| Backend Logic              |   8  | P0-P1          |
| Frontend Logic             |   8  | P1-P2          |
| Frontend-Backend Mismatch  |   0  | —              |
| Auth / Session             |   0  | —              |
| Shared Libraries           |   0  | —              |
| Test Coverage / Quality    |   0  | —              |
| Infrastructure / OpenShift |   1  | P0             |
| Architecture               |   6  | P1-P2          |
| Security (PII Leakage)     |   2  | P0             |
| **TOTAL**                  | **26** |               |

#### 🔴 Priority 0 (Critical)
- **[BUG-SECURITY-001]** Hardcoded default `password`/`secret` di berbagai `application.yml` (seperti `auth-service`, `statement-service`) menggunakan fallback tanpa enkripsi/Vault (`${DATABASE_PASSWORD:payu}`).
- **[BUG-SECURITY-002]** Celah IDOR pada `TopUpController.java` dan `SubscriptionController.java` (`get` endpoint). Validasi kepemilikan (`validateOwnership`) tidak diimplementasikan, sehingga user berpotensi melihat data user lain (berbeda dengan perbaikan yang sudah ada di `PaymentController`).
- **[BUG-SECURITY-003]** Defisiensi Validasi *Payload* API (CWE-20). Ditemukan belasan _controller_ (contohnya `CardController` dan `WebhookController`) yang menerima parameter `@RequestBody` namun luput menyertakan anotasi `@Valid`. Hal ini semakin fatal karena kelas DTO yang dituju (seperti `CreateCardRequest`) sama sekali tidak dibekali anotasi konstrain bawaan JSR-380 (`@NotNull`, `@Positive`, `@NotBlank`). Ini memungkinkan _malicious caller_ mengirimkan data anomali (misal: `dailyLimit = -1000`) dan memicu eksploitasi di lapisan Domain Model.
- **[BUG-LOGIC-002]** Tidak adanya perlindungan `Idempotency` (`@Idempotent`) pada HTTP POST endpoint paling krusial yaitu `@PostMapping("/transfer")` di dalam `TransactionController.java` (`transaction-service`). Berpotensi menimbulkan isu _replay payload_ transfer ganda.
- **[BUG-SECURITY-004]** Kebocoran PII (*Personally Identifiable Information*) ke Log Sistem. Di `AccountLookupController.java` baris 38: `log.info("Looking up account by phone: {}", phone)`. Nomor telepon nasabah dicetak *plain text* ke dalam log tanpa masking, melanggar standar `GEMINI.md` Pasal 1 "PII Protection" dan regulasi UU PDP. Jika log diangkut ke LokiStack atau sistem observability terpusat, maka nomor telepon seluruh nasabah terekam secara permanen dan dapat diakses oleh tim SRE/DevOps.
- **[BUG-SECURITY-005]** *Broad AOP Logging* Kebocoran PII Massal. Ditemukan `AuditLogAspect.java` di `account-service` (dan kemungkinan service lain) yang mencatat **seluruh argumen metode** secara otomatis menggunakan `Arrays.toString(joinPoint.getArgs())`. Karena aspek ini diterapkan secara luas ke seluruh service paket (`id.payu.account.service..*`), maka payload sensitif seperti `CreateUserRequest` (berisi NIK, Nama Lengkap, Email, Tgl Lahir) dicetak dalam **plain text** ke log sistem setiap kali ada registrasi atau update profile. Ini adalah pelanggaran keamanan fatal yang membypass seluruh mekanisme masking manual.

#### 🟠 Priority 1 (High)
- **[BUG-LOGIC-001]** Perhitungan finansial di `CashbackSagaContext.java` menggunakan tipe primitif `double` sebelum di-cast ke `BigDecimal`, berpotensi mengakibatkan masalah *loss of precision*.
- **[BUG-LOGIC-003]** Vulnerabilitas *Unbounded Pagination* / *Denial of Service (DoS)*. Pada puluhan _controller_ (seperti `TransactionController`, `DisbursementController`) parameter populasi list diterima manual memalui `@RequestParam(defaultValue = "20") int size` tanpa dijamin batasan `@Max(100)` atau kelas injeksi standar `Pageable`. Konfigurasi global _max page size_ akan terabaikan yang membiarkan API menerima argumen permintaan ribuan record memori secara masal.
- **[BUG-ARCH-001]** Puluhan `enum` Domain (seperti `UserStatus`, `KycStatus`, `RewardType`) secara tidak sengaja didefinisikan sebagai *inner class* pada Entity (bukan *top level class*), melanggar aturan arsitektur "Enum Placement" pada standar `GEMINI.md`.
- **[BUG-ARCH-003]** Pelanggaran prinsip isolasi arsitektur Hexagonal (`Hexagonal Architecture Isolation`). Kelas-kelas _Domain Model_ utama pada `transaction-service` (seperti `Transaction`, `SplitBill`, `BatchDisbursement`) didekorasi langsung dengan anotasi _persistence_ bawaan kerangka kerja JPA (`@Entity`, `@Table`, `import jakarta.persistence.*`). Hal ini mencampurkan batasan _business logic layer_ murni dan _persistence layer_.
- **[BUG-ARCH-004]** Rekayasa manajemen Zona Waktu. Ditemukan lebih dari 2.400 penggunaan API `LocalDateTime` secara luas di berbagai _Domain/DTO_ (seperti `Promotion`, `Receipt`, `Subscription`) ketimbang menggunakan `OffsetDateTime` atau `Instant`. Ini sangat fatal untuk sebuah _Payment Gateway API_ (yang harus selaras dengan standar `SNAP-BI` ISO-8601 berbasis UTC), karena transisi waktu akan bertabrakan antar klien di zona berbeda.
- **[BUG-ARCH-005]** *Lombok Entity Anti-Pattern*. Kelas pemetaan relasional seperti `UserRiskProfileEntity`, `ReceiptEntity`, dan `DisputeEntity` menyalahgunakan anotasi otomatis `@Data`. Praktik ini akan mencetak referensi secara siklik (*circular referencing*) maupun inisialisasi relasi JPA _lazy-load_ yang dapat memicu `StackOverflowError`, `LazyInitializationException` dan masalah performa N+1.
- **[BUG-ARCH-006]** Risiko Fatal *Cascading Failure* (Arsitektur Resiliensi). Ditemukan belasan injeksi manual `new RestTemplate()` secara langsung tanpa tata kelola *timeout* untuk komunikasi antar *microservice* (contohnya di `WalletServiceClient.java` dan `PaymentExpiryScheduler`). Pendekatan ini secara _default_ melahirkan HTTP Client tanpa batas wakti (_infinite timeout_) sekaligus sepenuhnya membypass benteng `resilience-starter` (Circuit Breaker) bawaan sistem PayU. Praktik ini berisiko melumpuhkan seluruh servis secara masif (_Thread Pool Exhaustion_) seketika jika ada satu _downstream service_ yang lambat atau mati.
- **[BUG-LOGIC-004]** *Manual JSON Serializer* Rentan Injeksi. Ditemukan implementasi `mapToJson()` manual berbasis `StringBuilder` di `PaymentExpiryScheduler.java` (baris 162) dan `MerchantService.java` (baris 324) untuk membuat payload Kafka event. Implementasi ini **tidak melakukan escaping** terhadap karakter khusus JSON (`"`, `\`, newline). Jika `referenceId` atau field lainnya mengandung karakter `"`, payload JSON akan *corrupt* dan *event consumer* di downstream akan crash. Selain itu, ini membuka vektor serangan *JSON Injection*. Seharusnya menggunakan `ObjectMapper` (Jackson) yang sudah ada sebagai dependency.
- **[BUG-LOGIC-005]** Risiko *Duplicate Execution* pada `@Scheduled` Task di Lingkungan Multi-Instance. Ditemukan 31+ metode `@Scheduled` tersebar lintas layanan (misal: `PaymentExpiryScheduler`, `SubscriptionService`, `MerchantService.expireQrPayments()`) tanpa perlindungan *distributed lock* (`ShedLock`, `@SchedulerLock`). Ketika layanan di-*scale* secara horizontal pada OpenShift (>1 pod/replica), seluruh scheduler akan berjalan **ganda secara simultan**, mengakibatkan: double-cancel pembayaran, double-charge langganan, dan data inconsistency parah.
- **[BUG-LOGIC-006]** Anti-Pattern `@Async` + `@Transactional` pada `InvestmentApplicationService.java`. Empat metode krusial (`buyDeposit`, `buyMutualFund`, `buyGold`, `sellInvestment`) ditandai `@Transactional` **dan** `@Async` secara bersamaan. Karena `@Async` menyebabkan eksekusi di thread terpisah, Spring tidak dapat menangkap transaksi proxy dari caller thread — sehingga anotasi `@Transactional` **diam-diam tidak berfungsi**. Ini berarti debit wallet bisa berhasil tapi jika `saveDeposit` atau `saveTransaction` gagal, **rollback JPA tidak terjadi** dan uang nasabah hilang tanpa tercatat.

#### 🟡 Priority 2 (Medium)
- **[BUG-ARCH-002]** Pelanggaran arsitektur standar _Error Handling_. Belasan _custom exceptions_ (seperti `InsufficientBalanceException`, `WalletNotFoundException`, dll.) tidak mewarisi base `BusinessException`. Serta melewatkan penggunaan Unique Error Code (e.g., `WAL_001`), mereka alih-alih melakukan `extends RuntimeException` secara langsung.
- **[BUG-FE-001]** Pelanggaran inkonsistensi `Premium Emerald` Design System pada *frontend*. Terdapat masifikasi elemen UI menggunakan _hardcoded tailwind colors_ repetitif bernada generik seperti (`bg-blue-500`, `text-blue-500`, `bg-red-500`) yang merusak warna sentral `bank-green` secara luas, melanggar *strict rule* `GEMINI.md` terkait kurasi estetika.
- **[BUG-FE-002]** Cacat Logika *Routing* Frontend & Aksesibilitas UX (I18n). Pada `MobileNav.tsx`, navigasi di-injeksi secara manual dengan _prefix locale_ (e.g. `/${locale}${path}`) sebelum dilempar ke komponen `Link` milik pustaka `next-intl`. Hal ini mengacaukan mekanisme internal `next-intl` dan menghasilkan _URL_ ganda (`/en/en/dashboard`), yang memicu respons `404 Not Found` di bahasa Inggris. Selain itu, ditemukan  label statis `aria-label="Navigasi Mobile"` (bahasa Indonesia *hardcoded*) yang mengacaukan pembacaan *Screen Reader* (UX) untuk ekspatriat.
- **[BUG-FE-003]** Cacat Logika *Routing* Duplikat di Landing Page (P2). Di file `page.tsx` (Landing Page) ditemukan pola `locale helper` identik dengan `MobileNav.tsx`: `const l = (path) => locale === 'id' ? path : /${locale}${path}`. Pola ini kemudian digunakan untuk `Link href={l('/onboarding')}`, `Link href={l('/login')}`, dll. Mekanisme ini secara redundan menambahkan *locale prefix* yang sebenarnya sudah di-handle oleh pustaka `next-intl Link`, berpotensi menyebabkan *double-prefix 404* untuk *user* non-Indonesia.
- **[BUG-FE-004]** *Hardcoded Indonesian Error Messages* di Frontend (P1 – UX/i18n). Pada `onboarding/page.tsx` (baris 48), ditemukan pesan galat statis berbahasa Indonesia: `toast.error('Pendaftaran gagal. Silakan coba lagi.')`. Pesan ini tidak melewati sistem internasionalisasi `next-intl`, sehingga pengguna berbahasa Inggris akan menerima pesan error dalam bahasa asing. Pola serupa ditemukan di `login/page.tsx` baris 77 (`'Login gagal'`).
- **[BUG-FE-005]** *Hardcoded PII* pada Kartu Presentasi Landing Page (P1 – Security/Privasi). Di `page.tsx` (Landing Page) baris 222, terpampang nama lengkap individu nyata `Fajar Nur Rohman` pada tampilan kartu bank visual. Ini merupakan pelanggaran langsung terhadap kebijakan `SECURITY.md` dan prinsip PII protection karena informasi personal tertanam dalam *source code* yang terpublikasi secara publik.
- **[BUG-FE-006]** Absennya `error.tsx` dan `global-error.tsx` di Next.js App Router (P1 – Resiliensi). Tidak ada satupun file `error.tsx` maupun `global-error.tsx` pada seluruh 23 *route segment* di `frontend/web-app/src/app/[locale]/`. Padahal ini adalah mekanisme utama Next.js 13+ untuk menangkap *unhandled runtime errors* per segmen. Tanpa file ini, setiap error React yang tidak ter-catch oleh `ErrorBoundary` component akan menyebabkan **white screen of death** di production tanpa pesan apapun kepada user. Komponen `ErrorBoundary` yang ada hanya membungkus layout utama, bukan error recovery per halaman.
- **[BUG-FE-007]** Ketimpangan `loading.tsx` Skeleton (P2 – UX). Dari 23 *route segment* di bawah `[locale]/`, hanya 5 yang memiliki `loading.tsx` (bills, dashboard, investments, lending, transfer). Sisa **18 route** (cards, exchange, backoffice, merchant, notifications, pockets, qris, rewards, scheduled-transfers, security, settings, split-bill, support, transactions, analytics, legal, login, onboarding) tidak memiliki loading state — sehingga user melihat **blank page** saat navigasi menunggu data fetch.
- **[BUG-FE-008]** *Hardcoded id-ID Locale* pada Format Tanggal Frontend (P2 – I18n). Ditemukan puluhan penggunaan fungsi `.toLocaleDateString('id-ID', ...)` yang dipukul rata di seluruh komponen (misal: `BalanceCard.tsx`, `PromoPopup.tsx`, `TransferActivity.tsx`). Hal ini menyebabkan pengguna yang memilih bahasa Inggris (`/en/dashboard`) tetap melihat format tanggal bahasa Indonesia ("20 Maret 2026"), melanggar prinsip internasionalisasi dan standar UX global. Seharusnya menggunakan locale dinamis dari `next-intl`.

> All 648 bugs fixed + 4 Won't Do archived to [`CHANGELOG.md`](../../CHANGELOG.md).
> **Phase 12 E2E Coverage Gaps Closed**: All 27 findings (BUG-TEST-090–116) resolved — 10 new Playwright specs, 2 backend fixes, 12 xfail markers removed.
> **Phase 11 E2E Coverage Gap Analysis**: 27 findings identified (BUG-TEST-090–116).
> **Phase 10 Shared Library Audit**: 31 findings — all fixed.
> **Phase 9 Infrastructure Audit Phase 2**: 44 findings — all fixed.
> **Phase 8 Test Quality Audit**: 39 findings — all fixed.

---

## 🔍 Spikes (Research / Architecture Decision)

| Key      | Type  | Question                                                                              | Impact                               | Status   |
| :------- | :---- | :------------------------------------------------------------------------------------ | :----------------------------------- | :------- |
| ARCH-001 | Spike | KYC di level PayU atau project client?                                                | Scope `kyc-service`                  | 📋 To Do |
| ARCH-002 | Spike | Statement: PDF end-user atau JSON/CSV project client?                                 | Output format `statement-service`    | 📋 To Do |
| ARCH-003 | Spike | Support ticket: end-user PayU atau project client?                                    | Multi-tenancy `support-service`      | 📋 To Do |
| ARCH-004 | Spike | CMS: hanya PayU web-app atau multi-tenant project client?                             | Multi-tenant mode `cms-service`      | 📋 To Do |
| ARCH-005 | Spike | RHPAM/Kogito/Drools PoC: evaluate rules engine untuk credit scoring & fraud detection | ADR-0015, `rules-starter` shared lib | 📋 To Do |

---

## 🔮 Deferred (Icebox)

| Key       | Type  | Summary                                                           | Notes                                            |
| :-------- | :---- | :---------------------------------------------------------------- | :----------------------------------------------- |
| P2-FE-003 | Story | Mobile App Feature Parity (Expo/RN)                               | ❄️ Deferred                                      |
| OCP-007   | Story | Service Mesh mTLS enforcement                                     | ❄️ Planned                                       |
| OCP-010   | Story | API versioning headers                                            | ❄️ Planned                                       |
| DR-001    | Story | Disaster Recovery live test execution                             | ❄️ Scripts ready                                 |
| DEFER-001 | Story | Card Tokenization & 3DS                                           | ❄️ Requires PCI-DSS scope + card network kontrak |
| RHPAM-001 | Story | Phase 1: Create `shared/rules-starter` (Drools 9.x embedded)      | ❄️ Depends on ARCH-005 PoC. See ADR-0015         |
| RHPAM-002 | Story | Phase 2: Migrate `lending-service` credit scoring ke DRL rules    | ❄️ Depends on RHPAM-001                          |
| RHPAM-003 | Story | Phase 3: Payment routing DMN decision tables di `gateway-service` | ❄️ Depends on RHPAM-001                          |
| RHPAM-004 | Story | Phase 4: Lending workflow + KYC/AML BPMN orchestration (Kogito)   | ❄️ Depends on RHPAM-002, evaluasi Q3 2026        |

---

## 📊 Metrics

### Current State

| Metric            | Value                                            |
| :---------------- | :----------------------------------------------- |
| Completed Epics   | 24/24 fully done (see PROGRESS.md)               |
| Completed Stories | 109 done (86 + 23 test stories archived)          |
| Completed SP      | 265/265                                          |
| Bugs Fixed        | 648 done + 4 Won't Do (archived to CHANGELOG)    |
| Open Bugs         | 26 — Temuan Logical Inspection Tahap Akhir (March 2026)|
| Tech Debt         | 3/3 completed (SIMP-001, SIMP-002, SIMP-003)    |

---

_Last Updated: March 20, 2026 | 0 Active Epics · 0 Open Stories · 26 Open Bugs · 0 Tech Debt · 5 Spikes · 9 Deferred_
_All 648 bugs fixed + 4 Won't Do archived to CHANGELOG.md_
_Phase 12 E2E Coverage Gap Fixes: All 27 findings (BUG-TEST-090–116) closed — 10 new Playwright specs, 2 backend routing fixes, 12 xfail markers removed. Pytest 159/159, Maven 38/38 — March 17, 2026_
_Phase 10 Shared Lib Audit: 31 new findings (BUG-SHARED-001–031) from 12 backend/shared/ modules (~170 source files) — March 17, 2026_
_Phase 9 Infra Audit Phase 2: 44 new findings (BUG-INFRA-044–087) from 50+ files across 7 infrastructure directories — March 17, 2026_
_Phase 8 Test Quality Audit: 39 new findings (BUG-TEST-051–089) from 249 test files across 20 services — March 17, 2026_
_Phase 7 Bug Sweep: ✅ COMPLETE (240/240 closed) — March 17, 2026. Verified: Maven 38/38, Frontend OK (44 routes, 79 pages), Playwright 544/544, Pytest 159/159._
_Phase 3 Bug Fixes: ✅ COMPLETE (34/34 closed) — March 16, 2026_
_Phase 2 Gateway Gaps: ✅ COMPLETE (GAP-001, GAP-002, GAP-006, GAP-007) — March 16, 2026_
_Phase 1 E2E Stabilization: ✅ COMPLETE (544 Playwright + 159 Pytest = 703 tests, 0 failures) — March 15, 2026_
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
_Referensi: BCA Digital (blu), Xendit, Midtrans, GoPay, OVO, DANA, Flip, Jago_
