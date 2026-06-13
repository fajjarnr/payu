# 🏦 PayU Gateway Architecture — Integration Guide

> **Dokumen ini menjelaskan arsitektur PayU sebagai Bank/Payment Gateway**
> yang akan diintegrasikan dengan project-project eksternal (TokoBapak, Nobar, Dolan, Sinau, Maca).
>
> Untuk bug yang perlu diperbaiki → [`TODOS.md`](./TODOS.md)
> Untuk deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)

---

## 🎯 Visi: PayU sebagai Payment Gateway

PayU bukan hanya digital banking standalone — ini adalah **payment infrastructure**
yang akan melayani multiple project eksternal:

| Project       | Tipe                                              | Kebutuhan PayU                                                                                                                             |
| :------------ | :------------------------------------------------ | :----------------------------------------------------------------------------------------------------------------------------------------- |
| **TokoBapak** | E-commerce (a la Tokopedia)                       | Checkout payment, escrow, settlement ke merchant, refund/dispute                                                                           |
| **Nobar**     | Streaming subscription (a la Netflix)             | Recurring billing, auto-debit bulanan, grace period                                                                                        |
| **Dolan**     | Travel & booking (a la Traveloka/Booking/Airbnb)  | Booking payment, split payment (flight+hotel), partial refund, multi-currency (international travel), escrow host payout (Airbnb model)    |
| **Sinau**     | Online learning (a la Udemy)                      | Course purchase (one-time), subscription plan (monthly/yearly), instructor payout/settlement, promo/coupon redemption, installment payment |
| **Maca**      | Digital publishing (a la Gramedia/Packt/O'Reilly) | E-book purchase, subscription (library access), author royalty settlement, bundle pricing, in-app purchase                                 |
| _(future)_    | Projekt lain                                      | API key, sandbox, SNAP-BI compliant endpoint                                                                                               |

---

## 🔁 Revisi Evaluasi: Service yang Awalnya Dikira Overkill

Dengan konteks sebagai payment gateway, beberapa service yang sempat dievaluasi
sebagai "terlalu kompleks" justru menjadi **essential**:

| Service                     | Alasan Dievaluasi Overkill | Kenapa Justru Penting                                                                                                                                                 | Keputusan                                                                    |
| :-------------------------- | :------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------- |
| `partner-service` + SNAP-BI | Tanpa BI test environment  | SNAP-BI adalah standar integrasi resmi BI Indonesia untuk semua mitra                                                                                                 | ✅ **PERTAHANKAN — fix bugs**                                                |
| `api-portal-service`        | "Tidak ada user portal"    | Tim dev TokoBapak/Nobar/Dolan/Sinau/Maca butuh sandbox untuk test sebelum production                                                                                  | ✅ **PERTAHANKAN — fix in-memory**                                           |
| `compliance-service`        | "Tidak ada regulator"      | OJK mensyaratkan AML audit trail untuk payment processor                                                                                                              | ✅ **PERTAHANKAN — fix model mismatch**                                      |
| `shared/saga-starter`       | "Enterprise overkill"      | Escrow TokoBapak (buyer→PayU→merchant) butuh saga compensation                                                                                                        | ✅ **PERTAHANKAN**                                                           |
| `fx-service`                | "IDR only cukup"           | Dolan **pasti butuh FX** untuk booking hotel/flight internasional. TokoBapak juga jika ada merchant luar negeri. Maca untuk pembelian e-book publisher internasional. | ✅ **PERTAHANKAN & PRIORITASKAN — fix estimate endpoint, connect ke wallet** |
| `shared/cache-starter`      | "Spring @Cacheable cukup"  | Gateway serve multiple clients — performance multi-layer justified                                                                                                    | ✅ **PERTAHANKAN — fix stale-while-revalidate**                              |

---

## 🔴 Yang Tetap Direkomendasikan Dihapus/Disederhanakan

Meski konteks berubah, beberapa fitur tetap tidak relevan untuk payment gateway:

| ID           | Fitur                           | Service              | Alasan                                                                                                                                                      | Rekomendasi                                                                                                 |
| :----------- | :------------------------------ | :------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------- | :---------------------------------------------------------------------------------------------------------- |
| **SIMP-001** | **A/B Testing Service**         | `ab-testing-service` | Payment gateway tidak perlu A/B test infrastrukturnya sendiri. TokoBapak/Nobar yang akan A/B test — bukan PayU. Struktur data juga broken (XBUG-003).       | Hapus. Pakai feature flags via config/env var.                                                              |
| **SIMP-002** | **Gamification XP/Badge/Level** | `promotion-service`  | XP, badge, streak bukan concern payment gateway. TokoBapak/Nobar punya gamifikasi sendiri. Loyalty points & cashback tetap relevan sebagai banking feature. | Hapus `GamificationService.java` (518 baris, banyak bugs). Keep `LoyaltyPointsService` + `CashbackService`. |
| **SIMP-003** | **Robo-Advisory**               | `investment-service` | Butuh ML model, data historical, dan izin OJK (SPAM). Jauh di luar scope payment gateway.                                                                   | Simplify investment: portfolio view + buy mutual fund (mocked). Hapus robo-advisory.                        |

---

## ⚠️ Pertanyaan Arsitektur yang Perlu Dijawab

Beberapa komponen memerlukan klarifikasi scope sebelum bisa diputuskan:

| ID           | Komponen              | Pertanyaan                                                                                                                                                                          | Dampak                                                                     |
| :----------- | :-------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------- |
| **ARCH-001** | **KYC Service**       | KYC di level PayU (untuk buka akun) atau di level project client? Saat ini KYC ada di PayU untuk user PayU. Tapi jika user TokoBapak tidak perlu akun PayU terpisah, KYC redundant. | Menentukan apakah `kyc-service` perlu simplify atau justru diperkuat       |
| **ARCH-002** | **Statement Service** | Statement untuk end-user PayU (PDF download di web-app) atau untuk project client (JSON/CSV export via API)?                                                                        | Jika untuk project client: ubah output format, tidak perlu PDF             |
| **ARCH-003** | **Support Service**   | Support ticket dari end-user PayU, atau dari project client yang mengalami masalah integrasi?                                                                                       | Jika keduanya: perlu multi-tenancy di support-service (ticket per partner) |
| **ARCH-004** | **CMS Service**       | Banner/konten hanya untuk PayU web-app, atau project client juga bisa push konten mereka via CMS PayU?                                                                              | Menentukan apakah CMS perlu multi-tenant mode                              |

---

## 🔑 Gap Kritis yang HARUS Diisi untuk Gateway Role

Berikut adalah fitur yang **belum ada sama sekali** tapi wajib untuk payment gateway:

### 🔴 P0 — Blocker untuk integrasi apapun

| ID          | Gap                                     | Detail                                                                                                                                                                                                                                              | Relevan Untuk                                                                                                                                                  |
| :---------- | :-------------------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| ~~GAP-001~~ | ✅ **Outbound Webhook**                 | ~~PayU harus bisa notify partner saat transaksi selesai/gagal.~~ **COMPLETED Mar 16**: Created `FinancialEventConsumer` in partner-service — multi-topic Kafka consumer (20 financial + 5 escrow topics) routing events to `WebhookDispatcherService` with HMAC-SHA256 signed delivery. Refactored `SubscriptionEventConsumer` for StringDeserializer compatibility. | TokoBapak (payment confirmation), Nobar (subscription activated), Dolan (booking confirmed/cancelled), Sinau (enrollment confirmed), Maca (purchase delivered) |
| ~~GAP-002~~ | ✅ **Multi-Tenancy**                    | ~~`partner-service` menyimpan partner tapi tidak ada data isolation.~~ **COMPLETED Mar 16**: Added `@TenantAware` + `TenantEntityListener` + `tenantId` column across 23 microservices. Flyway migrations for all tables. Gateway `TenantFilter` updated with `X-Partner-Id` fallback. | Semua project client                                                                                                                                           |
| ~~GAP-006~~ | ✅ **Idempotency Global**               | ~~Semua payment endpoint harus support `X-Idempotency-Key`.~~ **COMPLETED Mar 16**: `@Idempotent(required=true)` annotations added to 48 financial endpoints across 5 services (lending, fx, dispute, transaction, wallet). Gateway `IdempotencyFilter` FINANCIAL_PATHS expanded from 9 to 28. | Semua financial endpoints                                                                                                                                      |
| ~~GAP-007~~ | ✅ **Escrow / Payment Holding Enhanced** | ~~Escrow mechanism sudah ada di wallet-service.~~ **COMPLETED Mar 16**: Added Kafka event publishing for escrow state changes (held/released/settled/refunded/expired) via transactional outbox pattern. `WalletEventPublisherPort` extended with 5 escrow event methods. `FinancialEventConsumer` listens to 5 escrow topics for webhook delivery. | TokoBapak, Dolan                                                                                                                                               |
| ~~GAP-008~~ | ✅ **Recurring / Subscription Billing** | ~~Nobar: auto-debit streaming bulanan.~~ **COMPLETED Feb 28**: Subscription event webhooks dengan `SubscriptionEventAdapter`, event types: `subscription.created`, `subscription.renewed`, `subscription.cancelled`, `subscription.payment_failed`. | Nobar, Sinau, Maca                                                                                                                                             |

### 🟠 P1 — Diperlukan setelah integrasi pertama

| ID          | Gap                                | Detail                                                                                                                                                                                                                        | Relevan Untuk                 |
| :---------- | :--------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :---------------------------- |
| ~~GAP-003~~ | ✅ **Settlement & Reconciliation** | ~~Daily/weekly settlement ke masing-masing partner.~~ **COMPLETED Feb 28**: Settlement batch job dengan state machine (PENDING→PROCESSING→COMPLETED/FAILED/OVERRIDDEN), reconciliation report, discrepancy detection + alert. | TokoBapak, Dolan, Sinau, Maca |
| ~~GAP-004~~ | ✅ **Rate Card per Partner**       | ~~TokoBapak: 1.5% per transaksi.~~ **COMPLETED Feb 28**: Rate card engine dengan 3 fee types (FLAT, PERCENTAGE, TIERED), min/max caps, partner-specific pricing.                                                              | Semua project client          |
| ~~GAP-009~~ | ✅ **Refund & Dispute**            | ~~TokoBapak: refund jika barang tidak sampai.~~ **COMPLETED Feb 28**: New `dispute-service` with full refund (PENDING→PROCESSING→COMPLETED/FAILED) and dispute (OPEN→INVESTIGATING→RESOLVED/ESCALATED) lifecycle.             | TokoBapak, Dolan, Sinau       |

### 🟠 P2 — Nice to have

| ID          | Gap                                       | Detail                                                                                                                                                                                                                                             |
| :---------- | :---------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| ~~GAP-005~~ | ✅ **API Key Management**                 | ~~5 project client butuh stable API key.~~ **COMPLETED Feb 26**: Full API key lifecycle in `partner-service`. SHA-256 hash storage, `payu_live_`/`payu_test_` prefixes, rotation with 30-day grace period, max 5 keys/partner.                     |
| ~~GAP-010~~ | ✅ **Multi-Currency Settlement**          | ~~Dolan FX untuk international booking.~~ **COMPLETED Feb 28**: FX rate locking 15m window, partner currency preference, auto-conversion at settlement time.                                                                                       |
| ~~GAP-011~~ | ✅ **Split Payment**                      | ~~Dolan: flight + hotel + insurance dalam 1 checkout.~~ **COMPLETED Feb 26**: Multi-merchant split payment in `wallet-service` with percentage/fixed/mixed types, largest-remainder rounding, atomic execution, full reversal support.             |
| ~~GAP-012~~ | ✅ **Installment / PayLater Integration** | ~~Dolan: booking tiket pesawat Rp5jt bisa dicicil 3x/6x/12x.~~ **COMPLETED Feb 28**: Gateway-facing installment checkout in `lending-service`. Tenor options (3x/6x/12x), checkout flow with PayLater credit check, repayment schedule generation. |
| ~~GAP-013~~ | ✅ **Revenue Share / Royalty Engine**     | ~~Sinau: 70% instructor + 30% platform.~~ **COMPLETED Feb 28**: Revenue split engine dengan priority-based stakeholder ordering, monthly royalty statements.                                                                                       |

---

## 🟢 Arsitektur yang Sudah Tepat

Yang sudah ada dan relevan untuk payment gateway:

| Komponen                             | Kenapa Tepat                                                                        |
| :----------------------------------- | :---------------------------------------------------------------------------------- |
| `gateway-service` (Quarkus)          | Rate limiting per partner client, routing — essential                               |
| `partner-service` + SNAP-BI          | Standar BI Indonesia untuk integrasi mitra — arah benar, fix bugs                   |
| `shared/outbox-starter`              | Exactly-once Kafka delivery — critical untuk financial events                       |
| `shared/saga-starter`                | Distributed transaction compensation — vital untuk escrow TokoBapak                 |
| `transaction-service` sharding       | Handle concurrent high-volume dari 5 partner (TokoBapak, Nobar, Dolan, Sinau, Maca) |
| `wallet-service` double-entry ledger | Audit-grade untuk reconciliation + escrow balance tracking                          |
| `api-portal-service` sandbox         | Onboarding tim dev 5 partner project sebelum production                             |
| `compliance-service` AML             | OJK requirement untuk payment processor                                             |
| `auth-service` risk-based MFA        | Payment gateway handle uang orang lain — security tidak bisa dikurangi              |
| `api-commons` `WebhookProcessor`     | **Inbound** webhook handling (dari bank, QRIS) — sudah bagus, perlu tambah outbound |
| `api-commons` `RateLimitAspect`      | Rate limiting per endpoint — ada bug (BUG-BE-090,091), tapi foundation benar        |

---

## 🏗️ Target Integration Architecture

```text
┌──────────────────────────────────────────────────────────────────┐
│                    PROJECT CLIENT ECOSYSTEM                      │
├──────────┬──────────┬──────────┬──────────┬─────────────────────┤
│TokoBapak │  Nobar   │  Dolan   │  Sinau   │  Maca              │
│e-commerce│streaming │travel    │learning  │digital publishing   │
│escrow    │recurring │booking   │course    │e-book + subscription│
│refund    │auto-debit│split-pay │installmt │royalty settlement   │
└────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬────────────────┘
     │          │          │          │          │
     └──────────┴──────────┴──────────┴──────────┘
                           │
                    SNAP-BI / REST API
                           ▼
              ┌─────────────────────────────┐
              │  ✅ 3scale (APIcast)       │  ← API Management (5+ partner)
              │  developer portal, analytics │     (IMP-019/020) — see §3scale below
              │  rate plans, API lifecycle   │
              └──────────┬──────────────────┘
                         ▼
  partner-service ──── api-portal-service (sandbox, docs)
        │
        │  API Key Auth + HMAC Signing
        ▼
  gateway-service (rate limiting, routing, JWT, idempotency)
        │
        ├──── transaction-service (one-time payment, QRIS, BI-FAST)
        │           └── wallet-service (escrow hold, commit, release)
        │
        ├──── [future] subscription-service (Nobar/Sinau/Maca recurring)
        │
        ├──── [future] webhook-service (notify all partners)
        │
        ├──── [future] settlement-service (payout, revenue share, royalty)
        │           └── fx-service (multi-currency for Dolan/Maca)
        │
        ├──── lending-service (installment/PayLater for Dolan/Sinau)
        │
        ├──── compliance-service (AML, audit trail)
        │
        └──── notification-service (internal + partner alert)
```

### Partner Payment Model Summary

| Partner       | Payment Model                                              | Settlement Model                               | Unique Needs                                                    |
| :------------ | :--------------------------------------------------------- | :--------------------------------------------- | :-------------------------------------------------------------- |
| **TokoBapak** | One-time checkout, escrow                                  | Daily payout ke merchant setelah buyer confirm | Escrow, refund/dispute, multi-merchant                          |
| **Nobar**     | Recurring monthly subscription                             | Monthly flat fee                               | Auto-debit, grace period, dunning                               |
| **Dolan**     | Booking payment (flight+hotel+insurance), split payment    | Payout ke hotel/airline/host setelah check-in  | Multi-currency (FX), split payment, partial refund, escrow host |
| **Sinau**     | One-time course purchase + subscription plan + installment | 70/30 revenue share ke instructor per sale     | Revenue share engine, installment, promo/coupon                 |
| **Maca**      | E-book purchase + library subscription                     | 80/20 royalty ke author/publisher per sale     | Royalty engine, bundle pricing, subscription                    |

---

## 📋 Roadmap Integrasi

### Phase 1: Foundation ✅ COMPLETED

- [x] ~~Fix BUG-BE-001 — Gateway JWT validation~~ ✅ Fixed with `nimbus-jose-jwt`
- [x] ~~Fix BUG-BE-002 — Auth in-memory state → Redis~~ ✅ Fully moved to Redis
- [x] ~~Fix BUG-BE-035 — Partner token store → Redis~~ ✅ Fixed
- [x] ~~Implement GAP-002 — Multi-tenancy (data isolation per partner)~~ ✅ Feb 26 — `TenantFilter`, `@TenantAware`, `TenantEntityListener` in `security-starter`
- [x] ~~Implement GAP-006 — Idempotency key support~~ ✅ Feb 26 — Redis-backed `X-Idempotency-Key` in gateway `IdempotencyFilter`, 24h TTL
- [x] ~~Implement IMP-003 — Wire circuit breaker + retry di gateway~~ ✅ Feb 26 — Resilience4j `@CircuitBreaker`, `@Retry`, `@Bulkhead` on gateway proxy
- [x] ~~Implement IMP-005 — Konsolidasi rate limiting~~ ✅ Feb 26 — Redis sliding-window `RateLimitFilter` with per-endpoint categories

### Phase 2: TokoBapak Integration ✅ COMPLETED

- [x] ~~Implement GAP-007 — Escrow mechanism~~ ✅ Feb 26 — Full escrow lifecycle in `wallet-service` (CREATED→HELD→RELEASED→SETTLED/REFUNDED), scheduled expiry
- [x] ~~Implement GAP-009 — Refund & dispute flow~~ ✅ Feb 28 — New `dispute-service` with refund + dispute lifecycle
- [x] ~~Implement GAP-001 — Outbound webhook delivery~~ ✅ Feb 26 — HMAC-SHA256 signed webhooks in `partner-service`, exponential backoff retry
- [x] ~~Implement GAP-003 — Basic settlement & reconciliation~~ ✅ Feb 28
- [x] ~~Implement IMP-001 — True double-entry ledger~~ ✅ Feb 26 — `JournalEntry`/`LedgerEntry` domain, trial balance endpoint

### Phase 3: Nobar Integration ✅ COMPLETED

- [x] ~~Implement GAP-008 — Subscription/recurring billing service~~ ✅ Feb 28
- [x] ~~Implement GAP-004 — Rate card per partner~~ ✅ Feb 28
- [x] ~~Implement GAP-005 — Stable API key management~~ ✅ Feb 26 — SHA-256 hash storage, rotation with grace period, max 5 keys/partner

### Phase 4: Dolan Integration ✅ COMPLETED

- [x] ~~Implement GAP-011 — Split payment~~ ✅ Feb 26 — Multi-merchant split in `wallet-service`, 34 unit tests
- [x] ~~Implement GAP-010 — Multi-currency settlement (FX-aware)~~ ✅ Feb 28
- [x] ~~Extend GAP-007 — Escrow untuk host payout~~ ✅ Feb 26 — Escrow supports buyer/seller/partner model
- [x] ~~Extend GAP-009 — Partial refund~~ ✅ Feb 28 — `CreatePartialRefundRequest` in `dispute-service`
- [x] ~~Implement GAP-012 — Installment/PayLater integration~~ ✅ Feb 28 — Tenor options (3x/6x/12x) in `lending-service`

### Phase 5: Sinau + Maca Integration ✅ COMPLETED

- [x] ~~Implement GAP-013 — Revenue share / royalty engine~~ ✅ Feb 28
- [x] ~~Extend GAP-008 — Subscription webhooks~~ ✅ Feb 28 — CloudEvent envelopes via Kafka
- [x] ~~Extend GAP-003 — Settlement per instructor/author~~ ✅ Feb 28 — Monthly royalty statements in `wallet-service`
- [x] ~~Extend GAP-012 — Installment untuk bootcamp Sinau~~ ✅ Feb 28 — Installment checkout flow in `lending-service`

### Phase 6: Developer Hub & Platform Maturity ✅ COMPLETED

- [x] ~~Implement IMP-021 — Deploy Red Hat Developer Hub~~ ✅ Mar 02 — RHDH manifests in `infrastructure/backstage/`
- [x] ~~Implement IMP-022 — `catalog-info.yaml` untuk 23 services~~ ✅ Mar 22
- [x] ~~Implement IMP-023 — OpenAPI coverage 80%+~~ ✅ Mar 01 — Annotations on gateway, account, partner, transaction, wallet
- [x] ~~Implement IMP-024 — Backstage software template~~ ✅ Mar 01 — `.agent/resources/templates/payu-microservice-template/`
- [x] ~~Implement IMP-025 — TechDocs integration~~ ✅ Mar 01 — `mkdocs.yml` with Material theme

### Phase 7: Scale & API Management ✅ COMPLETED

- [x] ~~Evaluate IMP-019 — Adopt 3scale/Kong~~ ✅ Mar 02 — ADR-0014, 3scale + Kong infrastructure manifests
- [x] ~~Implement IMP-016 — Persistent API analytics~~ ✅ Feb 28 — Redis-backed analytics with 90d retention
- [x] ~~Implement IMP-017 — Rate plan per partner~~ ✅ Feb 28 — Per-endpoint overrides in gateway
- [x] ~~Implement IMP-002 — Chart of Accounts~~ ✅ Feb 26 — 18 PSAK-based categories, 22 seed accounts
- [x] ~~Implement IMP-012 — GL engine ringan~~ ✅ Feb 26 — Balance sheet, income statement, daily settlement endpoints

---

## 🚀 3scale API Management — Deployment Workflow

The 3scale stack lives in the `payu-api-management` namespace and fronts the `gateway-service` in `payu-dev`. From outside the cluster, every API call hits APIcast (Nginx + Lua) which then proxies to `gateway-service.payu-dev.svc.cluster.local:8080` over plain HTTP inside the cluster.

### Components

| Component | Type | Role |
|:---|:---|:---|
| `system-app` (3 containers) | Deployment | `system-master` (port 3002), `system-provider` (port 3000), `system-developer` — all share one pod, services `system-master` / `system-provider` / `system-developer` point to the right container port via named port `master` |
| `apicast-production` / `apicast-staging` | Deployment | APIcast v2.16, `APICAST_CONFIGURATION_LOADER=boot`, fetches JSON config from `system-master:3000/master/api/proxy/configs/{env}.json` at startup |
| `backend-listener` / `backend-worker` / `backend-cron` | Deployment | Apisonator — handles rate-limit counters, auth lookups, async jobs |
| `system-sidekiq` | Deployment | ActiveJob worker (Sphinx reindex, etc) — 3scale is a Rails app |
| `system-memcache` / `system-searchd` | Deployment | Memcached (counters) + Sphinx (search) |
| `zync` / `zync-que` | Deployment | Notifies 3scale of OpenShift route changes |

### The ProxyConfig Boot-Loader Trap

APIcast runs with `APICAST_CONFIGURATION_LOADER=boot`, which means **the JSON config is fetched exactly once at container start** and cached in Nginx worker Lua state. A `ProxyConfigPromote` that writes a new `proxy_config` row into the system-master DB does **nothing** until you restart the APIcast pod. This is by design (low-latency routing decisions) but easy to miss.

### Promoting a Product to Production (the only reliable path)

**Do NOT** use `POST /admin/api/services/{id}/proxy/deploy.json` — it returns 200 but does not write a `proxy_config` row when the service is in `state: incomplete` (typical for products created via the admin portal). Always go through the operator-managed `ProxyConfigPromote` CRD.

```bash
# 1. Ensure Product CR exists in source: infrastructure/platform/api-management/3scale/payu-capabilities.yaml
oc apply -f infrastructure/platform/api-management/3scale/payu-capabilities.yaml

# 2. Promote (one-shot, deleteCR: true cleans up after success)
cat <<'EOF' | oc apply -f -
apiVersion: capabilities.3scale.net/v1beta1
kind: ProxyConfigPromote
metadata:
  name: payu-product-production
  namespace: payu-api-management
spec:
  productCRName: payu-product
  production: true
  deleteCR: true
EOF

# 3. Verify the proxy_config row exists in master API
oc exec -n payu-api-management apicast-production-... -c apicast-production -- \
  curl -sS -u "fcBnx0Oo:fcBnx0Oo" \
  "http://system-master:3000/master/api/proxy/configs/production.json" | jq

# 4. Restart APIcast so the boot-loader picks up the new config
oc delete pod -n payu-api-management -l threescale_component=apicast,threescale_component_element=production

# 5. E2E sanity
curl -skS "https://payu-product-payu-apicast-production.apps.payu.ocp.fajjjar.my.id/api/v1/products?user_key=04dc03f2e2a776bffcb9b16eb9f93796"
```

### E2E Test Matrix (validated 2026-06-13)

| Route | user_key | Result |
|:---|:---|:---|
| `api-payu-apicast-production` | `f0a4fe95cc59a7e279896f241263b02f` | 200 (public endpoints) / 401 (JWT-protected) |
| `payu-product-payu-apicast-production` | `04dc03f2e2a776bffcb9b16eb9f93796` | 200 / 401 |
| `api-payu-apicast-staging` | same | 200 |
| `payu-product-payu-apicast-staging` | same | 200 |

Invalid `user_key` returns 403 (`Authentication parameters missing` or `Authentication failed`). Valid `user_key` + missing JWT on protected paths returns 401 with `{"error":"MISSING_TOKEN","message":"Valid JWT token required"}` from the gateway, proving the auth chain works end-to-end.
