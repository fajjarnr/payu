# 🏦 PayU Gateway Architecture — Integration Guide

> **Dokumen ini menjelaskan arsitektur PayU sebagai Bank/Payment Gateway**
> yang akan diintegrasikan dengan project-project eksternal seperti TokoBapak dan Nobar.
>
> Untuk bug yang perlu diperbaiki → [`TODOS.md`](./TODOS.md)
> Untuk deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)

---

## 🎯 Visi: PayU sebagai Payment Gateway

PayU bukan hanya digital banking standalone — ini adalah **payment infrastructure**
yang akan melayani multiple project eksternal:

| Project | Tipe | Kebutuhan PayU |
| :--- | :--- | :--- |
| **TokoBapak** | E-commerce (a la Tokopedia) | Checkout payment, escrow, settlement ke merchant, refund/dispute |
| **Nobar** | Streaming subscription (a la Netflix) | Recurring billing, auto-debit bulanan, grace period |
| *(future)* | Projekt lain | API key, sandbox, SNAP-BI compliant endpoint |

---

## 🔁 Revisi Evaluasi: Service yang Awalnya Dikira Overkill

Dengan konteks sebagai payment gateway, beberapa service yang sempat dievaluasi
sebagai "terlalu kompleks" justru menjadi **essential**:

| Service | Alasan Dievaluasi Overkill | Kenapa Justru Penting | Keputusan |
| :--- | :--- | :--- | :--- |
| `partner-service` + SNAP-BI | Tanpa BI test environment | SNAP-BI adalah standar integrasi resmi BI Indonesia untuk semua mitra | ✅ **PERTAHANKAN — fix bugs** |
| `api-portal-service` | "Tidak ada user portal" | Tim dev TokoBapak/Nobar butuh sandbox untuk test sebelum production | ✅ **PERTAHANKAN — fix in-memory** |
| `compliance-service` | "Tidak ada regulator" | OJK mensyaratkan AML audit trail untuk payment processor | ✅ **PERTAHANKAN — fix model mismatch** |
| `shared/saga-starter` | "Enterprise overkill" | Escrow TokoBapak (buyer→PayU→merchant) butuh saga compensation | ✅ **PERTAHANKAN** |
| `fx-service` | "IDR only cukup" | TokoBapak mungkin butuh FX jika ada merchant luar negeri | ⚠️ **PERTAHANKAN — fix estimate endpoint** |
| `shared/cache-starter` | "Spring @Cacheable cukup" | Gateway serve multiple clients — performance multi-layer justified | ✅ **PERTAHANKAN — fix stale-while-revalidate** |

---

## 🔴 Yang Tetap Direkomendasikan Dihapus/Disederhanakan

Meski konteks berubah, beberapa fitur tetap tidak relevan untuk payment gateway:

| ID | Fitur | Service | Alasan | Rekomendasi |
| :--- | :--- | :--- | :--- | :--- |
| **SIMP-001** | **A/B Testing Service** | `ab-testing-service` | Payment gateway tidak perlu A/B test infrastrukturnya sendiri. TokoBapak/Nobar yang akan A/B test — bukan PayU. Struktur data juga broken (XBUG-003). | Hapus. Pakai feature flags via config/env var. |
| **SIMP-002** | **Gamification XP/Badge/Level** | `promotion-service` | XP, badge, streak bukan concern payment gateway. TokoBapak/Nobar punya gamifikasi sendiri. Loyalty points & cashback tetap relevan sebagai banking feature. | Hapus `GamificationService.java` (518 baris, banyak bugs). Keep `LoyaltyPointsService` + `CashbackService`. |
| **SIMP-003** | **Robo-Advisory** | `investment-service` | Butuh ML model, data historical, dan izin OJK (SPAM). Jauh di luar scope payment gateway. | Simplify investment: portfolio view + buy mutual fund (mocked). Hapus robo-advisory. |

---

## ⚠️ Pertanyaan Arsitektur yang Perlu Dijawab

Beberapa komponen memerlukan klarifikasi scope sebelum bisa diputuskan:

| ID | Komponen | Pertanyaan | Dampak |
| :--- | :--- | :--- | :--- |
| **ARCH-001** | **KYC Service** | KYC di level PayU (untuk buka akun) atau di level project client? Saat ini KYC ada di PayU untuk user PayU. Tapi jika user TokoBapak tidak perlu akun PayU terpisah, KYC redundant. | Menentukan apakah `kyc-service` perlu simplify atau justru diperkuat |
| **ARCH-002** | **Statement Service** | Statement untuk end-user PayU (PDF download di web-app) atau untuk project client (JSON/CSV export via API)? | Jika untuk project client: ubah output format, tidak perlu PDF |
| **ARCH-003** | **Support Service** | Support ticket dari end-user PayU, atau dari project client yang mengalami masalah integrasi? | Jika keduanya: perlu multi-tenancy di support-service (ticket per partner) |
| **ARCH-004** | **CMS Service** | Banner/konten hanya untuk PayU web-app, atau project client juga bisa push konten mereka via CMS PayU? | Menentukan apakah CMS perlu multi-tenant mode |

---

## 🔑 Gap Kritis yang HARUS Diisi untuk Gateway Role

Berikut adalah fitur yang **belum ada sama sekali** tapi wajib untuk payment gateway:

### 🔴 P0 — Blocker untuk integrasi apapun

| ID | Gap | Detail | Relevan Untuk |
| :--- | :--- | :--- | :--- |
| **GAP-001** | **Outbound Webhook** | PayU harus bisa notify TokoBapak/Nobar saat transaksi selesai/gagal. **Ada `WebhookProcessor` di `api-commons` tapi itu untuk INBOUND** (menerima webhook dari bank eksternal). Yang missing: outbound delivery ke partner URL. | TokoBapak (payment confirmation), Nobar (subscription activated) |
| **GAP-002** | **Multi-Tenancy** | `partner-service` menyimpan partner tapi tidak ada data isolation. Transaksi TokoBapak tidak boleh terlihat di Nobar dashboard. | Semua project client |
| **GAP-006** | **Idempotency Global** | Semua payment endpoint harus support `X-Idempotency-Key`. TokoBapak: double-click checkout = double-charge. Nobar: billing retry = double-debit. | Semua financial endpoints |
| **GAP-007** | **Escrow / Payment Holding** | TokoBapak: buyer bayar → PayU tahan uang → delivery confirmed → release ke merchant. Tidak ada escrow mechanism di `wallet-service`. | TokoBapak |
| **GAP-008** | **Recurring / Subscription Billing** | Nobar: auto-debit langganan bulanan. Butuh: scheduled task, retry on failure, grace period, dunning notification. Tidak ada di codebase sama sekali. | Nobar |

### 🟠 P1 — Diperlukan setelah integrasi pertama

| ID | Gap | Detail | Relevan Untuk |
| :--- | :--- | :--- | :--- |
| **GAP-003** | **Settlement & Reconciliation** | Daily settlement: berapa yang harus dibayarkan PayU ke TokoBapak merchant. Seller bisa terima uang setelah buyer confirm terima barang. | TokoBapak |
| **GAP-004** | **Rate Card per Partner** | TokoBapak: 1.5% per transaksi. Nobar: Rp500 flat/bulan. Saat ini tidak ada pricing config per partner. | Semua project client |
| **GAP-009** | **Refund & Dispute** | TokoBapak buyer refund jika barang tidak sampai. `billing-service` ada `REFUNDED` status tapi tidak ada refund flow atau dispute process. | TokoBapak |

### 🟠 P2 — Nice to have

| ID | Gap | Detail |
| :--- | :--- | :--- |
| **GAP-005** | **API Key Management** | Project client butuh stable API key (server-to-server), bukan OAuth token yang expire. `partner-service` hanya handle OAuth. |
| **GAP-010** | **Multi-Currency Settlement** | Jika TokoBapak/Nobar dapat international users, settlement perlu FX-aware. FX service ada tapi belum connect ke wallet. |

---

## 🟢 Arsitektur yang Sudah Tepat

Yang sudah ada dan relevan untuk payment gateway:

| Komponen | Kenapa Tepat |
| :--- | :--- |
| `gateway-service` (Quarkus) | Rate limiting per partner client, routing — essential |
| `partner-service` + SNAP-BI | Standar BI Indonesia untuk integrasi mitra — arah benar, fix bugs |
| `shared/outbox-starter` | Exactly-once Kafka delivery — critical untuk financial events |
| `shared/saga-starter` | Distributed transaction compensation — vital untuk escrow TokoBapak |
| `transaction-service` sharding | Handle concurrent high-volume dari TokoBapak + Nobar |
| `wallet-service` double-entry ledger | Audit-grade untuk reconciliation + escrow balance tracking |
| `api-portal-service` sandbox | Onboarding tim dev TokoBapak/Nobar sebelum production |
| `compliance-service` AML | OJK requirement untuk payment processor |
| `auth-service` risk-based MFA | Payment gateway handle uang orang lain — security tidak bisa dikurangi |
| `api-commons` `WebhookProcessor` | **Inbound** webhook handling (dari bank, QRIS) — sudah bagus, perlu tambah outbound |
| `api-commons` `RateLimitAspect` | Rate limiting per endpoint — ada bug (BUG-BE-090,091), tapi foundation benar |

---

## 🏗️ Target Integration Architecture

```
TokoBapak / Nobar / Project Client
        │
        │  SNAP-BI / REST API
        ▼
  partner-service ──── api-portal-service (sandbox, docs)
        │
        │  API Key Auth
        ▼
  gateway-service (rate limiting, routing, JWT)
        │
        ├──── transaction-service (one-time payment, QRIS, BI-FAST)
        │           └── wallet-service (escrow hold, commit, release)
        │
        ├──── [future] subscription-service (Nobar recurring billing)
        │
        ├──── [future] webhook-service (notify TokoBapak/Nobar)
        │
        ├──── compliance-service (AML, audit trail)
        │
        └──── notification-service (internal + partner alert)

Settlement:
  transaction-service → [future] settlement-service → partner wallet → bank transfer
```

---

## 📋 Roadmap Integrasi

### Phase 1: Foundation (Prerequisite sebelum TokoBapak/Nobar bisa integrasi)
- [ ] Fix BUG-BE-001 — Gateway JWT validation (saat ini placeholder!)
- [ ] Fix BUG-BE-002 — Auth in-memory state → Redis
- [ ] Fix BUG-BE-035 — Partner token store → Redis
- [ ] Implement GAP-002 — Multi-tenancy (data isolation per partner)
- [ ] Implement GAP-006 — Idempotency key support di semua payment endpoints

### Phase 2: TokoBapak Integration
- [ ] Implement GAP-007 — Escrow mechanism di `wallet-service`
- [ ] Implement GAP-009 — Refund & dispute flow
- [ ] Implement GAP-001 — Outbound webhook delivery service
- [ ] Implement GAP-003 — Basic settlement & reconciliation
- [ ] Fix BUG-BE-062 — Cashback tidak credit ke wallet (related ke seller payout)

### Phase 3: Nobar Integration
- [ ] Implement GAP-008 — Subscription/recurring billing service
- [ ] Implement GAP-004 — Rate card per partner (flat fee model)
- [ ] Implement GAP-005 — Stable API key management

### Phase 4: Scale & Compliance
- [ ] Fix BUG-BE-090 — Rate limit race condition (atomic Redis op)
- [ ] Fix BUG-BE-060 — Loyalty points race condition (ledger integrity)
- [ ] Implement GAP-010 — FX-aware settlement (jika ada international)
