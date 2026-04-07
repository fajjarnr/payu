# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| **Open Bugs**    |  12   | 🟢 Total Open Bugs: 12 items (P0: 2, P1: 4, P2: 6) 🔴 Critical Sec Bugs: 3 items remaining (March 2026) |

> **Completed Epics**: 24/24 fully done. All stories & tech debt cleared.
> See [`PROGRESS.md`](./PROGRESS.md) for completed Epics summary.
> **Closed bugs, stories & history**: See [`CHANGELOG.md`](../../CHANGELOG.md).

### 🐛 Open Bug Scorecard

| Kategori                   |  Open  | Priority Range |
| :------------------------- | :----: | :------------- |
| Backend Logic              |   16   | P0-P2          |
| Frontend Logic             |   12   | P1-P2          |
| Frontend-Backend Mismatch  |   0    | —              |
| Auth / Session             |   0    | —              |
| Shared Libraries           |   0    | —              |
| Test Coverage / Quality    |   0    | —              |
| Infrastructure / OpenShift |   1    | P0             |
| Architecture               |   9    | P1-P2          |
| Security (PII/IDOR)        |   18   | P0-P1          |
| **TOTAL**                  | **56** |                |

#### 🔴 Priority 0 (Critical)

- [x] **[BUG-SECURITY-001]** Hardcoded default `password`/`secret` di berbagai `application.yml` (seperti `auth-service`, `statement-service`) menggunakan fallback tanpa enkripsi/Vault (`${DATABASE_PASSWORD:payu}`). (FIXED: Phase 13)
- [x] **[BUG-SECURITY-006]** Mekanisme In-Memory Fallback untuk Caching Eksperimen AB Testing di Frontend (`ABTestingService.ts`) menyimpan status otorisasi _session-less_ akibat penanganan memori yang melampaui daur hidup _browser_ secara konseptual. (FIXED: Phase 13)
- **[BUG-SECURITY-027]** Broken Access Control pada endpoint administrasi `promotion-service`. File `SecurityConfig.java` hanya mengatur `.anyRequest().authenticated()` tanpa pembedaan role, sementara endpoint `createPromotion`, `updatePromotion`, dan `activatePromotion` di `PromotionResource.java` tidak memiliki `@PreAuthorize`/role check. Akibatnya user biasa yang hanya terautentikasi dapat membuat, mengubah, atau mengaktifkan kampanye promosi yang seharusnya eksklusif untuk admin/backoffice.
- [x] **[BUG-SECURITY-002]** Celah IDOR pada `TopUpController.java` dan `SubscriptionController.java` (`get` endpoint). Validasi kepemilikan (`validateOwnership`) tidak diimplementasikan, sehingga user berpotensi melihat data user lain (berbeda dengan perbaikan yang sudah ada di `PaymentController`). (FIXED: Phase 13)
- [x] **[BUG-SECURITY-003]** Defisiensi Validasi _Payload_ API (CWE-20). Ditemukan belasan _controller_ (contohnya `CardController` dan `WebhookController`) yang menerima parameter `@RequestBody` namun luput menyertakan anotasi `@Valid`. (FIXED: Phase 13)
- **[BUG-SECURITY-008]** _Account Lockout Bypass_ akibat _Hardcoded Cache TTL_. Di `KeycloakService.java`, durasi _lockout_ (misal: 60 menit) dikonfigurasi melalui `payu.security.lockout-duration-minutes`. Akan tetapi penyimpanan sesi gagal ke Redis selalu menggunakan konstanta mutlak `FAILED_ATTEMPTS_TTL = Duration.ofMinutes(15)`. Hal ini menyebabkan akun yang terkunci akan otomatis dijebol kembali setiap 15 menit begitu Redis membersihkan memori.
- **[BUG-SECURITY-009]** _Race Condition_ Penanggulangan Brute-Force (_Read-Modify-Write Anti-Pattern_). Di fungsi `recordFailedAttemptInternal` (`KeycloakService.java`), perhitungan _failed login_ ke Redis dilakukan dengan metode ambil-tambah-simpan (`get` -> `count++` -> `put`) alih-alih menggunakan operasi _atomic_ (via operasi `INCR` Redis). _Attacker_ yang meluncurkan 100 hitungan login per detik secara bersamaan akan dicatat sebagai 1 kali gagal saja, sepenuhnya menghindari _Rate Limiting Lockout_.
- [x] **[BUG-LOGIC-002]** Tidak adanya perlindungan `Idempotency` (`@Idempotent`) pada HTTP POST endpoint paling krusial yaitu `@PostMapping("/transfer")` di dalam `TransactionController.java` (`transaction-service`). (FIXED: Phase 13)
- [x] **[BUG-LOGIC-010]** Absennya Hak Akses (Broken Access Control) untuk Membaca Transaksi _Inbound/Incoming_. Di dalam `AuthorizationService.java` metode `verifyTransactionAccess` hanya mengecek/mengamankan jika akun pengguna (autentikasi) sama dengan `transaction.getSenderAccountId()`. Akibatnya, pengguna sebagai status Penerima (_Recipient_) **benar-benar tidak bisa melihat detail transaksi yang masuk ke dompetnya**. Hal ini memicu `AccessDeniedException` jika nasabah mengklik entri _Incoming Transfer_ di riwayat riil aplikasinya. (FIXED: 2026-03-21)
- [x] **[BUG-LOGIC-011]** Mengeksploitasi Kuota Promosi Tak Terbatas (Unsaved Entity State). Pada `PromoRedemptionService.java` di layanan `promotion-service`, proses iterasi memotong kuota dan menandai promosi dengan memanggil `promo.apply(context)`. Operasi ini memutasi sisa batas kuota promosi _hanya di memori instans_. Arsitektur Hexagonal murni yang digunakan tidak otomatis membilas (`flush`) perubahan POJO ini ke Database karena kode lupa memanggil `promoCodeRepository.save(promo)`. Dampaknya, satu kuota promosi bisa diklaim ribuan kali tanpa pernah mengurangi sisa penggunaan di database (`currentUsageCount` akan tetap di 0). (FIXED: 2026-03-21)
- [x] **BUG-SECURITY-004**: PII Leakage in logs (KYC). OCR results and sensitive user data were logged in plain text. (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-005**: PII Leakage in logs (Auth). Username/identifier logged in plain text during login/refresh. (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-006**: Missing Audit Log for admin operations in Backoffice service. (FIXED: 2026-03-21)
- [x] **BUG-SECURITY-010**: IDOR in transaction status check (`GET /api/v1/transactions/{transactionId}`). (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-011**: Information leakage in audit log. Sensitive context not masked. (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-012**: Potential IDOR in transaction history per account (`GET /api/v1/accounts/{accountId}/transactions`). (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-013**: IDOR in Payment Controller (Billing Service). Users can create payments for other accounts. (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-014**: IDOR in Top-Up (Billing Service). Potential balance theft via cross-account top-up. (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-015**: IDOR in Top-Up info disclosure (Billing Service). (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-016**: Missing Authentication in Analytics Service endpoints. (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-017**: IDOR in Analytics & KYC Retrieval. (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-018**: IDOR in Repayments (`GET /repayment-schedules/{scheduleId}`, `POST /repayment-schedules/{scheduleId}/pay`). (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-019**: IDOR in Pre-Approval (`GET /pre-approval/{preApprovalId}`). (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-020**: IDOR in Installments Checkout. uses `userId` from body without verification. (FIXED: 2026-03-20)
- [x] **BUG-SECURITY-021**: IDOR in Installment Retrieval (`GET /installments/{checkoutId}`). (FIXED: 2026-03-20)
- [x] **[BUG-LOGIC-015]** **Pelanggaran Imutabilitas Audit Log** di `DataAccessAuditService.java` (`compliance-service`). Meskipun endpoint `DELETE` telah dihapus dari `GdprAuditController` (BUG-BE-081), metode `deleteDataAccessAudit` masih aktif dan dapat dipanggil di lapisan _Service/Persistence_. Sesuai kebijakan kepatuhan GDPR dan perbankan, log audit akses data sensitif harus bersifat mutlak _permanently immutable_ dan tidak boleh ada kode yang memungkinkan penghapusan record tersebut. (FIXED: 2026-03-21)

#### 🟠 Priority 1 (High)

- [x] **[BUG-LOGIC-001]** Perhitungan finansial di `CashbackSagaContext.java` menggunakan tipe primitif `double` sebelum di-cast ke `BigDecimal`, berpotensi mengakibatkan masalah _loss of precision_. (FIXED: Phase 13)
- [x] **[BUG-LOGIC-003]** Vulnerabilitas _Unbounded Pagination_ / _Denial of Service (DoS)_. (FIXED: Phase 13)
- [x] **[BUG-ARCH-001]** Puluhan `enum` Domain (seperti `UserStatus`, `KycStatus`, `RewardType`) secara tidak sengaja didefinisikan sebagai _inner class_ pada Entity (bukan _top level class_). (FIXED: Phase 13)
- [x] **[BUG-ARCH-003]** Pelanggaran prinsip isolasi arsitektur Hexagonal (`Hexagonal Architecture Isolation`). (FIXED: Phase 13)
- [x] **[BUG-ARCH-004]** Rekayasa manajemen Zona Waktu. (FIXED: Phase 13)
- [x] **[BUG-ARCH-005]** _Lombok Entity Anti-Pattern_. (FIXED: Phase 13)
- [x] **[BUG-ARCH-006]** Risiko Fatal _Cascading Failure_ (Arsitektur Resiliensi). (FIXED: Phase 13)
- [x] **BUG-AUTH-013**: Inconsistent JWT Claim for User ID. Some services use `sub`, others use `account_id`. (FIXED: 2026-03-20 - Standardized to `account_id` with `sub` fallback)
- [x] **BUG-LOGIC-002**: Missing Idempotency on Transfer endpoint. (ALREADY FIXED in codebase)
- [x] **[BUG-LOGIC-014]** **Metadata Paginasi Korup** di `TransactionController.java` (`transaction-service`). (FIXED: Phase 14)
- [x] **[BUG-LOGIC-004]** _Manual JSON Serializer_ Rentan Injeksi. (FIXED: Phase 13)
- [x] **[BUG-LOGIC-005]** Risiko _Duplicate Execution_ pada `@Scheduled` Task di Lingkungan Multi-Instance. (FIXED: Phase 13)
- [x] **[BUG-LOGIC-006]** Anti-Pattern `@Async` + `@Transactional` pada `InvestmentApplicationService.java`. (FIXED: Phase 13)
- [x] **[BUG-LOGIC-009]** Mekanisme Ketidakcocokan Tanda Tangan (_Signature Mismatch_) pada _Universal Links_. (FIXED: Phase 14)
- [x] **[BUG-LOGIC-008]** Kehilangan Presisi Finansial (_Floating-Point Errors_) pada Entitas Promosi (P2 – Akurasi Backend). (FIXED: Phase 14)
- [x] **[BUG-ARCH-007]** Pelanggaran Resiliensi `Circuit Breaker` pada Asinkronisasi (P1 – Resiliensi Backend). (FIXED: Phase 13)
- [x] **[BUG-ARCH-008]** Eksekusi Destruktif O(N) `redisTemplate.keys()` pada Latar Belakang (P1 - Arsitektur/Kinerja). (FIXED: Phase 14)
- [x] **[BUG-ARCH-009]** _In-Memory Idempotency Store_ di `kyc-service` (P1 - Arsitektur/Ketahanan). (FIXED: Phase 14)
- [x] **[BUG-LOGIC-012]** _Financial Amount di URL Query Parameter_ (P1 — Keamanan API / PCI Compliance). (FIXED: Phase 14)
- **[BUG-LOGIC-013]** _Null ReservationId_ pada Operasi `commit`/`release` di `DisbursementService.java` (P1 — Integritas Finansial). Saat memanggil `walletService.commitBalance()` dan `walletService.releaseBalance()` di method `completeDisbursement()` dan `failDisbursement()`, kode secara eksplisit melewatkan `null` sebagai parameter `reservationId` (baris 179 dan 203 — disertai komentar `// reservationId would be stored in a real implementation`). Pada lingkungan produksi, hal ini berisiko: (1) _Commit_ atau _release_ dilakukan pada reservasi yang salah jika wallet service menggunakan `reservationId` sebagai kunci, (2) _Wallet balance inconsistency_ — dana terjepit selamanya di status _reserved_ tanpa bisa di-release jika pencocokan reservasi gagal.
- **[BUG-SECURITY-022]** IDOR pada endpoint receipt di `statement-service`. Di `StatementController.java`, endpoint `GET /receipts/{receiptId}`, `GET /receipts/transaction/{transactionId}`, dan dua endpoint download PDF hanya meneruskan `receiptId`/`transactionId` ke `ReceiptService` tanpa mengirim identitas user terautentikasi. Di `ReceiptService.java`, lookup dilakukan langsung via `findById()` atau `findByTransactionId()` tanpa validasi kepemilikan. Akibatnya user yang mengetahui UUID receipt atau `transactionId` pihak lain dapat membaca atau mengunduh bukti transaksi milik user lain.
- **[BUG-SECURITY-023]** Kebocoran ledger lintas akun pada endpoint `GET /wallets/{accountId}/ledger/transaction/{transactionId}` di `wallet-service`. `WalletController.java` memang memverifikasi ownership terhadap `accountId` pada path, tetapi lalu memanggil `walletUseCase.getLedgerEntriesByTransactionId(...)` yang di `WalletService.java` mengembalikan semua entri ledger berdasarkan `transactionId` saja. Tidak ada filter ulang ke `accountId`, sehingga caller dapat memakai `accountId` miliknya sendiri namun membaca baris ledger akun lain yang ikut berada dalam transaksi yang sama.
- **[BUG-SECURITY-024]** Broken Access Control pada endpoint loyalty points di `promotion-service`. `LoyaltyPointsResource.java` menerima `accountId` dari body/path untuk operasi tambah poin, redeem, histori, dan balance; `LoyaltyPointsService.java` kemudian langsung memproses `request.accountId()` atau `accountId` tersebut tanpa binding ke principal JWT. Karena `SecurityConfig.java` hanya mensyaratkan `.anyRequest().authenticated()`, user biasa yang terautentikasi dapat membaca atau memodifikasi saldo poin akun pengguna lain.
- **[BUG-SECURITY-025]** _Identity Spoofing_ pada flow claim/apply promotion di `promotion-service`. `PromotionService.claimPromotion()` menulis reward ke `request.accountId()` dari body, sedangkan `PromoRedemptionService.applyPromo()` mengecek kuota `ONCE_PER_USER` dan merekam penggunaan promo berdasarkan `request.userId()` yang juga sepenuhnya berasal dari request. Tidak ada verifikasi bahwa `accountId`/`userId` tersebut cocok dengan principal JWT, sehingga attacker dapat mengklaim reward atas akun lain atau membakar jatah promo one-time milik korban.
- [x] **[BUG-AUTH-014]** Middleware autentikasi web salah menganggap sesi masih valid saat refresh token ditolak. (FIXED: Phase 14)
- [x] **[BUG-AUTH-015]** Mekanisme auto-refresh di BFF proxy `/api/v1/[...path]` gagal memutar sesi pada request pertama yang menerima `401`. (FIXED: Phase 14)
- [x] **[BUG-AUTH-016]** Silent refresh memaksa logout pada kegagalan sementara gateway/BFF. (FIXED: Phase 14)
- [x] **[BUG-AUTH-017]** Proactive silent refresh hanya aktif di sebagian kecil halaman terlindungi. (FIXED: Phase 14)
- [x] **[BUG-CROSS-035]** Sesi berbasis cookie dapat lolos middleware tetapi tetap gagal direfleksikan ke auth store client. (FIXED: Phase 14)
- [x] **[BUG-CROSS-033]** Frontend salah menyamakan `user.id` dengan `accountId` setelah login. (FIXED: Phase 14)
- [x] **[BUG-SECURITY-026]** Persistensi PII sensitif ke `localStorage` pada web auth store. (FIXED: Phase 14)
- [x] **[BUG-SECURITY-028]** PII username masih dicatat ke log di BFF login route web. (FIXED: Phase 14)
- [x] **[BUG-SECURITY-029]** Broken Access Control dan _notification spoofing_ di `notification-service`. (FIXED: Phase 14)
- **[BUG-LOGIC-016]** Endpoint validasi promo di `promotion-service` selalu mengembalikan sukses palsu. Metode `validatePromo()` pada `PromoRedemptionController.java` menerima `promoCode`, `amount`, dan `userId`, tetapi tidak memanggil service/repository apa pun lalu langsung merespons `{"valid": true}`. Frontend atau partner yang mengandalkan endpoint ini akan menerima _false positive_, melanjutkan checkout dengan promo yang sebenarnya tidak valid, sudah expired, atau sudah pernah dipakai user.
- [x] **[BUG-CROSS-034]** Flow onboarding web mengirim `externalId` statis. (FIXED: Phase 14)

#### 🟡 Priority 2 (Medium)

- **[BUG-ARCH-002]** Pelanggaran arsitektur standar _Error Handling_. Belasan _custom exceptions_ (seperti `InsufficientBalanceException`, `WalletNotFoundException`, dll.) tidak mewarisi base `BusinessException`. Serta melewatkan penggunaan Unique Error Code (e.g., `WAL_001`), mereka alih-alih melakukan `extends RuntimeException` secara langsung.
- [x] **[BUG-FE-001]** Pelanggaran inkonsistensi `Premium Emerald` Design System pada _frontend_. (FIXED: Phase 14)
- [x] **[BUG-FE-002]** Cacat Logika _Routing_ Frontend & Aksesibilitas UX (I18n). (FIXED: Phase 14)
- [x] **[BUG-FE-003]** Cacat Logika _Routing_ Duplikat di Landing Page (P2). (FIXED: Phase 14)
- [x] **[BUG-FE-004]** _Hardcoded Indonesian Error Messages_ di Frontend (P1 – UX/i18n). (FIXED: Phase 14)
- [x] **[BUG-FE-005]** _Hardcoded PII_ pada Kartu Presentasi Landing Page (P1 – Security/Privasi). (FIXED: Phase 14)
- [x] **[BUG-FE-006]** Absennya `error.tsx` dan `global-error.tsx` di Next.js App Router (P1 – Resiliensi). (FIXED: Phase 14)
- **[BUG-FE-007]** Ketimpangan `loading.tsx` Skeleton (P2 – UX). Dari 23 _route segment_ di bawah `[locale]/`, hanya 5 yang memiliki `loading.tsx` (bills, dashboard, investments, lending, transfer). Sisa **18 route** (cards, exchange, backoffice, merchant, notifications, pockets, qris, rewards, scheduled-transfers, security, settings, split-bill, support, transactions, analytics, legal, login, onboarding) tidak memiliki loading state — sehingga user melihat **blank page** saat navigasi menunggu data fetch.
- **[BUG-FE-008]** _Hardcoded id-ID Locale_ pada Format Tanggal Frontend (P2 – I18n). Ditemukan puluhan penggunaan fungsi `.toLocaleDateString('id-ID', ...)` yang dipukul rata di seluruh komponen (misal: `BalanceCard.tsx`, `PromoPopup.tsx`, `TransferActivity.tsx`). Hal ini menyebabkan pengguna yang memilih bahasa Inggris (`/en/dashboard`) tetap melihat format tanggal bahasa Indonesia ("20 Maret 2026"), melanggar prinsip internasionalisasi dan standar UX global. Seharusnya menggunakan locale dinamis dari `next-intl`.
- **[BUG-FE-009]** Risiko Skalabilitas & _Memory Leak_ Akibat `WeakMap` Berlebihan pada Interceptor HTTP di `api.ts`. Variabel _state_ global seperti `let isRefreshing = false` dan _array promise_ `failedQueue` digunakan untuk antrean _request_ saat pembaruan _token_ (`Token Refresh`). Mekanisme konfirmasi ini menggunakan mutasi _global array_ secara kasar yang rentan mangkrak (tergantung di RAM tanpa dikumpul oleh _Garbage Collector_) pada koneksi yang sangat putus-nyambung karena penolakan `reject` yang mungkin terlambat atau referensi yang mengikat `originalRequest`.
- **[BUG-FE-010]** Penggunaan Eksekusi Navigasi Kasar (`window.location.href`) di Dalam _React SSR/BFF Ecosystem_. Pada utilitas `api.ts`, jika mekanisme _token refresh_ gagal total, pengguna didorong keluatr (_redirect-out_) menggunakan skrip mutlak `window.location.href = /${locale}/login`. Di Next.js App Router (13+), cara ini sangat dikutuk karena secara instan membuang seluruh _React Context_ di memori klien (_Hard Reload_) sehingga pengalaman navigasi SPA menjadi patah (_Flashing Screen_). Ini terlewat ditangani karena komponen semestinya memanggil _router_ `next-intl` (contoh: `useRouter().push()`) bukan menyabotase jendela `window` langsung.
- **[BUG-FE-011]** Penumpukan Entri Penjelajahan Web Gagal (_Broken Navigation History_) di Frontend. Pada komponen pemasaran `BannerCarousel.tsx`, interaksi _klik_ pada _banner_ menggunakan `router.push(banner.actionUrl);` (dari _hook_ Next-Intl) untuk _deep linking_. Tetapi pada interaksi ganda/cepat dari pengguna, riwayat navigasi (`history stack`) kebanjiran _path_ yang sama. Pengguna secara menyiksa harus memencet tombol `Back` di Android berpuluh kali. Pemanggilan semacam rotasi promosi seharusnya mengeksekusi `router.replace` atau diberikan mekanisme pengecekan `debounce/throttle`.
- [x] **[BUG-FE-012]** _Hydration Mismatch_ pada Implementasi Zustand (P2 – Stabilitas React). (FIXED: Phase 14)
- [x] **[BUG-FE-013]** Halaman notifikasi menavigasi ke route detail yang tidak ada. (FIXED: Phase 14)
- [x] **[BUG-FE-014]** Komposisi `ButtonMotion` menghasilkan markup bertumpuk. (FIXED: Phase 14)
- [x] **[BUG-FE-015]** Halaman utama area terautentikasi melewati `next-intl`. (FIXED: Phase 14)
- [x] **[BUG-FE-016]** Quick Actions dashboard memakai anchor `href` mentah. (FIXED: Phase 14)
- [x] **[BUG-FE-017]** Halaman notifikasi menampilkan empty state palsu. (FIXED: Phase 14)
- [x] **[BUG-FE-018]** Toolbar aksi di halaman notifikasi hanyalah CTA palsu. (FIXED: Phase 14)
- [x] **[BUG-FE-019]** Halaman keamanan selalu melaporkan tidak ada sesi aktif. (FIXED: Phase 14)
- [x] **[BUG-FE-020]** Flow utama QRIS di web hanyalah mock interaktif. (FIXED: Phase 14)
- [x] **[BUG-FE-021]** Tombol `Lihat Semua Fitur` di dashboard CTA palsu. (FIXED: Phase 14)
- [x] **[BUG-FE-022]** Menu `Lihat Detail` pada transaksi inert. (FIXED: Phase 14)
- [x] **[BUG-FE-023]** Halaman keamanan CTA kritikal inert. (FIXED: Phase 14)
- [x] **[BUG-FE-024]** Halaman support facade bantuan tanpa aksi. (FIXED: Phase 14)
- [x] **[BUG-FE-025]** Widget finansial dashboard tombol manajemen inert. (FIXED: Phase 14)
- [x] **[BUG-CROSS-036]** Halaman analytics memalsukan metrik nol. (FIXED: Phase 14)
- [x] **[BUG-CROSS-037]** Halaman lending merender skor kredit palsu. (FIXED: Phase 14)
- [x] **[BUG-FE-026]** Halaman lending merusak daftar transaksi PayLater. (FIXED: Phase 14)
- [x] **[BUG-CROSS-038]** Halaman rewards memalsukan status loyalty. (FIXED: Phase 14)
- [x] **[BUG-CROSS-039]** Tombol `Kartu Baru` dapat mengirim payload placeholder. (FIXED: Phase 14)
- [x] **[BUG-FE-027]** Toggle `Kontrol Operasional` pada kartu kosmetik. (FIXED: Phase 14)
- [x] **[BUG-FE-028]** Dashboard backoffice utama placeholder `—`. (FIXED: Phase 14)
- [x] **[BUG-FE-029]** Halaman partner backoffice action management tidak terhubung. (FIXED: Phase 14)
- [x] **[BUG-FE-030]** Halaman campaigns backoffice facade mock. (FIXED: Phase 14)
- [x] **[BUG-FE-031]** Halaman CMS backoffice facade admin kosong. (FIXED: Phase 14)
- [x] **[BUG-FE-032]** Halaman FX rates backoffice facade mock. (FIXED: Phase 14)
- [x] **[BUG-FE-033]** Halaman broadcast backoffice mesin broadcast palsu. (FIXED: Phase 14)
- [x] **[BUG-FE-034]** Halaman A/B testing backoffice kontrol palsu. (FIXED: Phase 14)
- [x] **[BUG-FE-035]** Search box pada operasi backoffice inert. (FIXED: Phase 14)
- [x] **[BUG-FE-036]** Counter badge pada backoffice hardcoded. (FIXED: Phase 14)
- [x] **[BUG-FE-037]** Halaman compliance filter/export inert. (FIXED: Phase 14)
- [x] **[BUG-FE-038]** Halaman compliance metrik regulatori hardcoded. (FIXED: Phase 14)
- [x] **[BUG-FE-039]** Navigasi backoffice di mobile tidak muncul. (FIXED: Phase 14)
- [x] **[BUG-FE-040]** Chrome global backoffice search and notification inert. (FIXED: Phase 14)

> All 690 bugs fixed + 4 Won't Do archived to [`CHANGELOG.md`](../../CHANGELOG.md).
> **Phase 14 Frontend Remediation**: All 42 findings (BUG-FE-001–BUG-FE-040 + BUG-CROSS-033–039) resolved — i18n, design system, and backoffice connectivity.
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
| ARCH-006 | Spike | Spring Boot 4.0 & Jakarta EE 11 Migration Strategy: Audit Spring Cloud compatibility (specifically Vault) before platform-wide rollout. | Oakwood Release Train | 📋 To Do |

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

| Metric            | Value                                                   |
| :---------------- | :------------------------------------------------------ |
| Completed Epics   | 24/24 fully done (see PROGRESS.md)                      |
| Completed Stories | 109 done (86 + 23 test stories archived)                |
| Completed SP      | 265/265                                                 |
| Bugs Fixed        | 648 done + 4 Won't Do (archived to CHANGELOG)           |
| Open Bugs         | 42 — Temuan Logical Inspection Tahap Akhir (March 2026) |
| Tech Debt         | 3/3 completed (SIMP-001, SIMP-002, SIMP-003)            |

---

_Last Updated: April 7, 2026 | 0 Active Epics · 0 Open Stories · 12 Open Bugs · 0 Tech Debt · 6 Spikes · 9 Deferred_
_All 680 bugs fixed + 4 Won't Do archived to CHANGELOG.md_
_Phase 14 Frontend Remediation: ✅ COMPLETE (All 42 frontend bugs closed) — April 7, 2026_
_Phase 13 Security & Idempotency: ✅ COMPLETE (All 10 critical sec findings closed) — April 7, 2026_
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
