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

- **[BUG-SECURITY-027]** Broken Access Control pada endpoint administrasi `promotion-service`. File `SecurityConfig.java` hanya mengatur `.anyRequest().authenticated()` tanpa pembedaan role, sementara endpoint `createPromotion`, `updatePromotion`, dan `activatePromotion` di `PromotionResource.java` tidak memiliki `@PreAuthorize`/role check. Akibatnya user biasa yang hanya terautentikasi dapat membuat, mengubah, atau mengaktifkan kampanye promosi yang seharusnya eksklusif untuk admin/backoffice.
- **[BUG-SECURITY-008]** _Account Lockout Bypass_ akibat _Hardcoded Cache TTL_. Di `KeycloakService.java`, durasi _lockout_ (misal: 60 menit) dikonfigurasi melalui `payu.security.lockout-duration-minutes`. Akan tetapi penyimpanan sesi gagal ke Redis selalu menggunakan konstanta mutlak `FAILED_ATTEMPTS_TTL = Duration.ofMinutes(15)`. Hal ini menyebabkan akun yang terkunci akan otomatis dijebol kembali setiap 15 menit begitu Redis membersihkan memori.
- **[BUG-SECURITY-009]** _Race Condition_ Penanggulangan Brute-Force (_Read-Modify-Write Anti-Pattern_). Di fungsi `recordFailedAttemptInternal` (`KeycloakService.java`), perhitungan _failed login_ ke Redis dilakukan dengan metode ambil-tambah-simpan (`get` -> `count++` -> `put`) alih-alih menggunakan operasi _atomic_ (via operasi `INCR` Redis). _Attacker_ yang meluncurkan 100 hitungan login per detik secara bersamaan akan dicatat sebagai 1 kali gagal saja, sepenuhnya menghindari _Rate Limiting Lockout_.

#### 🟠 Priority 1 (High)

- **[BUG-LOGIC-013]** _Null ReservationId_ pada Operasi `commit`/`release` di `DisbursementService.java` (P1 — Integritas Finansial). Saat memanggil `walletService.commitBalance()` dan `walletService.releaseBalance()` di method `completeDisbursement()` dan `failDisbursement()`, kode secara eksplisit melewatkan `null` sebagai parameter `reservationId` (baris 179 dan 203 — disertai komentar `// reservationId would be stored in a real implementation`). Pada lingkungan produksi, hal ini berisiko: (1) _Commit_ atau _release_ dilakukan pada reservasi yang salah jika wallet service menggunakan `reservationId` sebagai kunci, (2) _Wallet balance inconsistency_ — dana terjepit selamanya di status _reserved_ tanpa bisa di-release jika pencocokan reservasi gagal.
- **[BUG-SECURITY-022]** IDOR pada endpoint receipt di `statement-service`. Di `StatementController.java`, endpoint `GET /receipts/{receiptId}`, `GET /receipts/transaction/{transactionId}`, dan dua endpoint download PDF hanya meneruskan `receiptId`/`transactionId` ke `ReceiptService` tanpa mengirim identitas user terautentikasi. Di `ReceiptService.java`, lookup dilakukan langsung via `findById()` atau `findByTransactionId()` tanpa validasi kepemilikan. Akibatnya user yang mengetahui UUID receipt atau `transactionId` pihak lain dapat membaca atau mengunduh bukti transaksi milik user lain.
- **[BUG-SECURITY-023]** Kebocoran ledger lintas akun pada endpoint `GET /wallets/{accountId}/ledger/transaction/{transactionId}` di `wallet-service`. `WalletController.java` memang memverifikasi ownership terhadap `accountId` pada path, tetapi lalu memanggil `walletUseCase.getLedgerEntriesByTransactionId(...)` yang di `WalletService.java` mengembalikan semua entri ledger berdasarkan `transactionId` saja. Tidak ada filter ulang ke `accountId`, sehingga caller dapat memakai `accountId` miliknya sendiri namun membaca baris ledger akun lain yang ikut berada dalam transaksi yang sama.
- **[BUG-SECURITY-024]** Broken Access Control pada endpoint loyalty points di `promotion-service`. `LoyaltyPointsResource.java` menerima `accountId` dari body/path untuk operasi tambah poin, redeem, histori, dan balance; `LoyaltyPointsService.java` kemudian langsung memproses `request.accountId()` atau `accountId` tersebut tanpa binding ke principal JWT. Karena `SecurityConfig.java` hanya mensyaratkan `.anyRequest().authenticated()`, user biasa yang terautentikasi dapat membaca atau memodifikasi saldo poin akun pengguna lain.
- **[BUG-SECURITY-025]** _Identity Spoofing_ pada flow claim/apply promotion di `promotion-service`. `PromotionService.claimPromotion()` menulis reward ke `request.accountId()` dari body, sedangkan `PromoRedemptionService.applyPromo()` mengecek kuota `ONCE_PER_USER` dan merekam penggunaan promo berdasarkan `request.userId()` yang juga sepenuhnya berasal dari request. Tidak ada verifikasi bahwa `accountId`/`userId` tersebut cocok dengan principal JWT, sehingga attacker dapat mengklaim reward atas akun lain atau membakar jatah promo one-time milik korban.
- **[BUG-LOGIC-016]** Endpoint validasi promo di `promotion-service` selalu mengembalikan sukses palsu. Metode `validatePromo()` pada `PromoRedemptionController.java` menerima `promoCode`, `amount`, dan `userId`, tetapi tidak memanggil service/repository apa pun lalu langsung merespons `{"valid": true}`. Frontend atau partner yang mengandalkan endpoint ini akan menerima _false positive_, melanjutkan checkout dengan promo yang sebenarnya tidak valid, sudah expired, atau sudah pernah dipakai user.

#### 🟡 Priority 2 (Medium)

- **[BUG-ARCH-002]** Pelanggaran arsitektur standar _Error Handling_. Belasan _custom exceptions_ (seperti `InsufficientBalanceException`, `WalletNotFoundException`, dll.) tidak mewarisi base `BusinessException`. Serta melewatkan penggunaan Unique Error Code (e.g., `WAL_001`), mereka alih-alih melakukan `extends RuntimeException` secara langsung.
- **[BUG-FE-007]** Ketimpangan `loading.tsx` Skeleton (P2 – UX). Dari 23 _route segment_ di bawah `[locale]/`, hanya 5 yang memiliki `loading.tsx` (bills, dashboard, investments, lending, transfer). Sisa **18 route** (cards, exchange, backoffice, merchant, notifications, pockets, qris, rewards, scheduled-transfers, security, settings, split-bill, support, transactions, analytics, legal, login, onboarding) tidak memiliki loading state — sehingga user melihat **blank page** saat navigasi menunggu data fetch.
- **[BUG-FE-008]** _Hardcoded id-ID Locale_ pada Format Tanggal Frontend (P2 – I18n). Ditemukan puluhan penggunaan fungsi `.toLocaleDateString('id-ID', ...)` yang dipukul rata di seluruh komponen (misal: `BalanceCard.tsx`, `PromoPopup.tsx`, `TransferActivity.tsx`). Hal ini menyebabkan pengguna yang memilih bahasa Inggris (`/en/dashboard`) tetap melihat format tanggal bahasa Indonesia ("20 Maret 2026"), melanggar prinsip internasionalisasi dan standar UX global. Seharusnya menggunakan locale dinamis dari `next-intl`.
- **[BUG-FE-009]** Risiko Skalabilitas & _Memory Leak_ Akibat `WeakMap` Berlebihan pada Interceptor HTTP di `api.ts`. Variabel _state_ global seperti `let isRefreshing = false` dan _array promise_ `failedQueue` digunakan untuk antrean _request_ saat pembaruan _token_ (`Token Refresh`). Mekanisme konfirmasi ini menggunakan mutasi _global array_ secara kasar yang rentan mangkrak (tergantung di RAM tanpa dikumpul oleh _Garbage Collector_) pada koneksi yang sangat putus-nyambung karena penolakan `reject` yang mungkin terlambat atau referensi yang mengikat `originalRequest`.
- **[BUG-FE-010]** Penggunaan Eksekusi Navigasi Kasar (`window.location.href`) di Dalam _React SSR/BFF Ecosystem_. Pada utilitas `api.ts`, jika mekanisme _token refresh_ gagal total, pengguna didorong keluatr (_redirect-out_) menggunakan skrip mutlak `window.location.href = /${locale}/login`. Di Next.js App Router (13+), cara ini sangat dikutuk karena secara instan membuang seluruh _React Context_ di memori klien (_Hard Reload_) sehingga pengalaman navigasi SPA menjadi patah (_Flashing Screen_). Ini terlewat ditangani karena komponen semestinya memanggil _router_ `next-intl` (contoh: `useRouter().push()`) bukan menyabotase jendela `window` langsung.
- **[BUG-FE-011]** Penumpukan Entri Penjelajahan Web Gagal (_Broken Navigation History_) di Frontend. Pada komponen pemasaran `BannerCarousel.tsx`, interaksi _klik_ pada _banner_ menggunakan `router.push(banner.actionUrl);` (dari _hook_ Next-Intl) untuk _deep linking_. Tetapi pada interaksi ganda/cepat dari pengguna, riwayat navigasi (`history stack`) kebanjiran _path_ yang sama. Pengguna secara menyiksa harus memencet tombol `Back` di Android berpuluh kali. Pemanggilan semacam rotasi promosi seharusnya mengeksekusi `router.replace` atau diberikan mekanisme pengecekan `debounce/throttle`.

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
| Bugs Fixed        | 690 done + 4 Won't Do (archived to CHANGELOG)           |
| Open Bugs         | 12 — Temuan Logical Inspection Tahap Akhir (March 2026) |
| Tech Debt         | 3/3 completed (SIMP-001, SIMP-002, SIMP-003)            |

---

_Last Updated: April 7, 2026 | 0 Active Epics · 0 Open Stories · 12 Open Bugs · 0 Tech Debt · 6 Spikes · 9 Deferred_
_All 690 bugs fixed + 4 Won't Do archived to CHANGELOG.md_
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
