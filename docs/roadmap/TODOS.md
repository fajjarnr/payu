# 🐛 PayU — Bug Backlog & Open Items

> **Dokumen ini hanya berisi item yang BELUM selesai dan perlu tindakan.**
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Bug Summary

| Kategori | Open | Skipped | Fixed | Original Total |
| :--- | :---: | :---: | :---: | :---: |
| Backend Logic | 3 | 4 | 140 | **147** |
| Frontend Logic | 0 | 0 | 46 | **46** |
| Frontend-Backend Mismatch | 2 | 0 | 27 | **29** |
| Auth / Session | 2 | 0 | 8 | **10** |
| **TOTAL** | **7** | **4** | **221** | **~232** |

> ✅ **221 of ~232 bugs fixed** (~95%) dari code review mendalam (Feb 24-25, 2026).
> Sisa 7 open bugs + 4 intentionally skipped. Detail di bawah.

---

## 🐛 Open Bugs (7 remaining)

### 🟠 P1 — High Severity

| ID | Service | Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-BE-026** | `notification-service` | **SMS sender adalah mock** — `SmsSender.java` selalu return success tanpa kirim OTP. | Integrasikan Twilio/Vonage atau provider SMS lokal (e.g., Zenziva). |
| **BUG-BE-037** | `billing-service` | **Biller processing adalah mock** — `PaymentService.java` selalu set `COMPLETED` tanpa panggil biller API. Balance terpotong, tagihan tidak dibayar. | Implementasi adapter per-biller (PLN, PDAM, dll.) atau set `PROCESSING` + callback. |
| **BUG-BE-051** | `statement-service` | **`getBalanceAtDate()` return saldo SAAT INI**, bukan historis. Statement opening/closing balance selalu sama. | Implementasi balance history endpoint di wallet-service, atau snapshot balance per-period. |

### 🟡 P2 — Medium Severity

| ID | Service | Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-CROSS-006** | FE ↔ BE | **Frontend tidak punya `BiometricService.ts`** padahal backend punya endpoint lengkap (sudah di-remove ke Keycloak MFA). | Hapus sisa referensi biometric di backend, atau implementasi Keycloak WebAuthn di frontend. |
| **XBUG-004** | FE ↔ BE | **Scheduled transfers & split bills** — FE methods exist tapi API path alignment belum diverifikasi. | Audit path matching antara `TransactionService.ts` dan BE controllers. |

### 🟢 P3 — Low Severity

| ID | Service | Issue | Solusi |
| :--- | :--- | :--- | :--- |
| **BUG-AUTH-007** | `middleware.ts` | Middleware izinkan akses hanya dengan `refreshToken` cookie — BFF mungkin gagal karena tidak ada `accessToken`. | Pastikan BFF proxy bisa trigger refresh jika hanya `refreshToken`. Acceptable untuk Edge middleware. |
| **BUG-AUTH-008** | `useSilentRefresh.ts` | Tidak ada unit test untuk hook kritis ini. | Tambahkan `vitest` fake timer tests untuk refresh scheduling, backoff, dan race conditions. |

---

## ⏭️ Intentionally Skipped (4 items)

> Item ini di-triage dan di-skip karena impact rendah pada fase saat ini.

| ID | Service | Issue | Alasan Skip |
| :--- | :--- | :--- | :--- |
| **BUG-BE-061** | `promotion-service` | `getTransactionAmount()` selalu return `ZERO` — badge berbasis amount tidak work. | Gamification/badge opsional, tidak pengaruh core banking. |
| **BUG-BE-076** | `api-portal-service` | Sandbox store in-memory — data hilang saat pod restart. | Partner belum ada, sandbox belum relevan. |
| **BUG-BE-080** | `lending-service` | Pre-approval endpoints ada di frontend, tidak ada di backend. | Feature belum aktif di frontend. |
| **BUG-BE-091** | `shared/api-commons` | Fixed-window rate limit mudah di-burst (118 req/2 detik). | Low-traffic fase awal masih aman. Optimize ke sliding window nanti. |

---

## 📋 Open Items (Non-Bug)

### 🔴 Gateway Gaps (Future Features — Belum Dibutuhkan)

> Detail lengkap di [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md).
> Semua GAP items adalah fitur integrasi untuk TokoBapak/Nobar yang belum di-develop.

| ID | Item | Priority |
| :--- | :--- | :--- |
| **GAP-001** | Outbound webhook service (notify partner saat payment done) | 🔴 P0 |
| **GAP-002** | Multi-tenancy / data isolation per partner | 🔴 P0 |
| **GAP-006** | Idempotency key support di semua payment endpoints | 🔴 P0 |
| **GAP-007** | Escrow / payment holding untuk TokoBapak | 🔴 P0 |
| **GAP-008** | Subscription / recurring billing untuk Nobar | 🔴 P0 |
| **GAP-003** | Settlement & reconciliation (daily payout ke merchant) | 🟠 P1 |
| **GAP-004** | Rate card / pricing per partner | 🟠 P1 |
| **GAP-009** | Refund & dispute management | 🟠 P1 |
| **GAP-005** | API key management (stable, non-expiring) | 🟡 P2 |
| **GAP-010** | Multi-currency settlement (FX-aware) | 🟡 P2 |

### 🟡 Simplification Candidates

| ID | Item | Rekomendasi |
| :--- | :--- | :--- |
| **SIMP-001** | `ab-testing-service` — broken, tidak relevan untuk payment gateway | Hapus service, ganti feature flags via env var |
| **SIMP-002** | Gamification (XP/Badge/Level) di `promotion-service` | Hapus `GamificationService.java`, keep `LoyaltyPoints` + `CashbackService` |
| **SIMP-003** | Robo-advisory di `investment-service` | Hapus, simplify ke portfolio view + mutual fund mock |

### ❓ Architecture Questions (Perlu Keputusan)

| ID | Pertanyaan | Impact |
| :--- | :--- | :--- |
| **ARCH-001** | KYC di level PayU atau project client? | Scope kyc-service |
| **ARCH-002** | Statement: PDF untuk end-user atau JSON/CSV untuk project client? | Output format statement-service |
| **ARCH-003** | Support ticket: end-user PayU atau project client yang integrasi? | Multi-tenancy di support-service |
| **ARCH-004** | CMS: hanya untuk PayU web-app atau multi-tenant project client? | Multi-tenant mode di cms-service |

### 🔮 Deferred

| ID | Description | Status |
| :--- | :--- | :--- |
| **P2-FE-003** | Mobile App Feature Parity (Expo/React Native) | Deferred |
| **OCP-007** | Service Mesh mTLS enforcement | Planned |
| **OCP-010** | API versioning headers | Planned |
| **DR-001** | Disaster Recovery live test execution | Scripts ready, pending execution |

---

_Last Updated: February 25, 2026 | Cleanup: Removed 221 fixed bugs — full history di CHANGELOG.md_
