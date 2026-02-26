# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| Status          | Count | Breakdown                                            |
| :-------------- | :---: | :--------------------------------------------------- |
| **Epics**       |  23   | 18 improvement, 4 partner/gateway, 1 infra           |
| **Stories**     |  69   | IMP-001 – IMP-069                                    |
| **GAP Stories** |  13   | GAP-001 – GAP-013                                    |
| **Tech Debt**   |   3   | SIMP-001 – SIMP-003                                  |
| **Spikes**      |   4   | ARCH-001 – ARCH-004                                  |
| **Deferred**    |   5   | P2-FE-003, OCP-007, OCP-010, DR-001, Card Token/3DS  |
| **Bugs**        | 0/232 | ✅ 229 fixed, 4 Won't Do (BUG-BE-061, 076, 080, 091) |

### 🐛 Bug Scorecard

| Kategori                  | Open  | Won't Do | Done |  Total   |
| :------------------------ | :---: | :------: | :--: | :------: |
| Backend Logic             |   0   |    3     | 144  | **147**  |
| Frontend Logic            |   0   |    0     |  46  |  **46**  |
| Frontend-Backend Mismatch |   0   |    0     |  29  |  **29**  |
| Auth / Session            |   0   |    0     |  10  |  **10**  |
| **TOTAL**                 | **0** |  **4**   | 229  | **~232** |

### Won't Do (4 items)

| Key        | Summary                                   | Resolution                                                |
| :--------- | :---------------------------------------- | :-------------------------------------------------------- |
| BUG-BE-061 | Promotion `getTransactionAmount()` → ZERO | Won't Do — gamification opsional, bukan core banking      |
| BUG-BE-076 | API Portal sandbox in-memory              | Won't Do — partner belum ada, sandbox belum relevan       |
| BUG-BE-080 | Lending pre-approval endpoints missing    | Won't Do — feature belum aktif di frontend                |
| BUG-BE-091 | Fixed-window rate limit burstable         | Won't Do — low-traffic fase awal. Superseded oleh IMP-005 |

---

## 🗂️ Epics Overview

### Priority Heatmap

| Epic | Name                              | Priority   | Stories | SP  | Quarter | Status   |
| :--- | :-------------------------------- | :--------- | :-----: | :-: | :------ | :------- |
| E-01 | Core Banking Ledger               | 🔴 Highest |    3    | 13  | Q1 2026 | 📋 To Do |
| E-02 | Gateway Hardening                 | 🔴 Highest |    5    | 11  | Q1 2026 | 📋 To Do |
| E-03 | Frontend Quality                  | 🟠 High    |    5    |  7  | Q1 2026 | 📋 To Do |
| E-04 | API Management & Analytics        | 🟠 High    |    5    | 19  | Q2 2026 | 📋 To Do |
| E-05 | Product Catalog                   | 🟠 High    |    1    |  5  | Q2 2026 | 📋 To Do |
| E-06 | Developer Hub (Backstage)         | 🟡 Medium  |    5    | 13  | Q2 2026 | 📋 To Do |
| E-07 | gRPC Inter-Service Communication  | 🟡 Medium  |    8    | 25  | Q2 2026 | 📋 To Do |
| E-08 | Legacy Integration Layer          | ⚪ Low     |    1    |  5  | Future  | 📋 To Do |
| E-09 | Partner Integration Foundation    | 🔴 Highest |    4    | 18  | Q1 2026 | 📋 To Do |
| E-10 | Escrow & Marketplace Payments     | 🔴 Highest |    2    | 10  | Q1 2026 | 📋 To Do |
| E-11 | Subscription & Recurring Billing  | 🔴 Highest |    2    |  8  | Q1 2026 | 📋 To Do |
| E-12 | Settlement & Financial Operations | 🟠 High    |    4    | 16  | Q2 2026 | 📋 To Do |
| E-13 | Dispute Resolution                | 🟠 High    |    1    |  5  | Q2 2026 | 📋 To Do |
| E-14 | Consumer Banking Experience       | 🟠 High    |    6    | 12  | Q2 2026 | 📋 To Do |
| E-15 | Payment Gateway Features          | 🔴 Highest |    7    | 25  | Q2 2026 | 📋 To Do |
| E-16 | Disbursement & Smart Routing      | 🟠 High    |    3    | 12  | Q3 2026 | 📋 To Do |
| E-17 | Promotion Engine Wiring           | 🟠 High    |    2    |  6  | Q2 2026 | 📋 To Do |
| E-18 | Developer Experience (Partner)    | 🟡 Medium  |    3    | 11  | Q3 2026 | 📋 To Do |
| E-19 | Transaction Proof & Receipts      | 🟠 High    |    1    |  2  | Q2 2026 | 📋 To Do |
| E-20 | Code Health & Technical Hygiene   | 🔴 Highest |    8    | 10  | Q1 2026 | 📋 To Do |
| E-21 | Security Hardening                | 🔴 Highest |    2    |  5  | Q1 2026 | 📋 To Do |
| E-22 | Gateway Reactive & Resilience     | 🔴 Highest |    2    |  6  | Q1 2026 | 📋 To Do |
| E-23 | Shared Library Lifecycle          | 🟠 High    |    2    | 11  | Q2 2026 | 📋 To Do |

> **Story Points**: XS=1, S=2, M=3, L=5, XL=8
> **Labels**: `backend`, `frontend`, `gateway`, `platform`, `partner`, `security`, `grpc`, `dx`, `mobile`

---

## 🟦 E-01 — Core Banking Ledger

> **Goal**: Evolve `wallet-service` dari single-sided ledger ke true double-entry accounting
> system dengan Chart of Accounts dan General Ledger. Foundation untuk settlement, reconciliation,
> dan regulatory reporting OJK.

| Key     | Type  | Summary                  | Priority   | SP  | Component(s)     | Labels             | Status    |
| :------ | :---- | :----------------------- | :--------- | :-: | :--------------- | :----------------- | :-------- |
| IMP-001 | Story | True Double-Entry Ledger | 🔴 Highest |  5  | `wallet-service` | `backend` `core`   | ✅ Done   |
| IMP-002 | Story | Chart of Accounts (CoA)  | 🔴 Highest |  3  | `wallet-service` | `backend` `core`   | ✅ Done   |
| IMP-012 | Story | GL Engine Ringan         | 🟡 Medium  |  5  | `wallet-service` | `backend` `finops` | ✅ Done   |

<details>
<summary>📄 Story Details</summary>

**IMP-001 — True Double-Entry Ledger** `L` `5 SP`

> Saat ini `LedgerEntry` single-sided (1 DEBIT _atau_ 1 CREDIT per row, tanpa pairing).
> Implementasi `JournalEntry` parent yang enforce matching DEBIT+CREDIT pairs.
> Tambah trial balance verification.
>
> **Acceptance Criteria**:
>
> - [x] `JournalEntry` entity dengan paired debit+credit `LedgerEntry`
> - [x] Constraint: sum(debit) == sum(credit) per journal
> - [x] Trial balance endpoint (`GET /wallets/trial-balance`)
> - [x] Migration script Flyway untuk existing data
> - [x] Unit tests + integration tests
>
> ⚠️ **FE Impact**: `WalletService.ts` perlu tambah `journalId` field + trial balance endpoint

**IMP-002 — Chart of Accounts (CoA)** `M` `3 SP`

> GL account classification: `ASSET:USER_WALLET`, `LIABILITY:ESCROW_HOLDING`,
> `REVENUE:TRANSACTION_FEE`. Enabler untuk settlement reconciliation & reporting OJK.
>
> **Acceptance Criteria**:
>
> - [x] `ChartOfAccount` entity dengan hierarchical code structure
> - [x] Seed data untuk standard banking CoA
> - [x] Link `LedgerEntry` → `ChartOfAccount`
>
> ✅ No FE impact — backend/backoffice only

**IMP-012 — GL Engine Ringan** `L` `5 SP`

> General Ledger untuk settlement reconciliation (neraca, laba-rugi).
> Implemented in `wallet-service` (not a separate service).
> Enabler untuk daily settlement report TokoBapak.
>
> **Acceptance Criteria**:
>
> - [x] Balance sheet generation
> - [x] Income statement generation
> - [x] Daily settlement report endpoint
>
> **Blocked by**: IMP-001, IMP-002 (resolved)
>
> ✅ No FE impact — backoffice/reporting only

</details>

---

## 🟦 E-02 — Gateway Hardening

> **Goal**: Perkuat `gateway-service` dari pass-through proxy ke production-grade API Gateway
> dengan resilience, validation, rate limiting, dan response masking.

| Key     | Type  | Summary                      | Priority   | SP  | Component(s)      | Labels              | Status   |
| :------ | :---- | :--------------------------- | :--------- | :-: | :---------------- | :------------------ | :------- |
| IMP-003 | Story | Wire Circuit Breaker + Retry | 🔴 Highest |  2  | `gateway-service` | `backend` `gateway` | 📋 To Do |
| IMP-005 | Story | Konsolidasi Rate Limiting    | 🔴 Highest |  3  | `gateway-service` | `backend` `gateway` | 📋 To Do |
| IMP-007 | Story | Dynamic Route Registry       | 🟠 High    |  3  | `gateway-service` | `backend` `gateway` | 📋 To Do |
| IMP-008 | Story | Request Validation (Schema)  | 🟠 High    |  3  | `gateway-service` | `backend` `gateway` | 📋 To Do |
| IMP-009 | Story | Response Masking             | 🟠 High    |  2  | `gateway-service` | `backend` `gateway` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-003 — Wire Circuit Breaker + Retry** `S` `2 SP`

> `RetryAndTimeoutService` dan `@CircuitBreaker` sudah di-code tapi **tidak di-wire**
> ke `proxy()` method di `ApiGatewayResource`. Tinggal connect.
>
> **Acceptance Criteria**:
>
> - [ ] `proxy()` wrapped dengan circuit breaker + retry
> - [ ] Fallback response saat circuit open (503)
> - [ ] Health endpoint reflects circuit state
>
> ✅ No FE impact — BFF sudah handle 503 fallback

**IMP-005 — Konsolidasi Rate Limiting** `M` `3 SP`

> 3 implementasi terpisah: (1) `RateLimitAspect` fixed window Redis,
> (2) `RateLimitFilter` fixed window, (3) `RateLimitV2Filter` in-memory token bucket
> (tidak distributed). Konsolidasi ke 1 implementasi Redis-backed sliding window.
>
> **Acceptance Criteria**:
>
> - [ ] Single `RateLimitFilter` dengan sliding window algorithm
> - [ ] Redis-backed (distributed across pods)
> - [ ] Configurable per-endpoint limits
> - [ ] Remove duplicate implementations
>
> **Blocks**: IMP-017 (Rate Plan per Partner)
>
> ✅ No FE impact

**IMP-007 — Dynamic Route Registry** `M` `3 SP`

> ~70 hardcoded JAX-RS endpoints di `ApiGatewayResource.java`.
> Ganti dengan config-driven route table (YAML/DB).
>
> **Acceptance Criteria**:
>
> - [ ] Route config file/table
> - [ ] Auto-discovery atau config reload
> - [ ] Existing routes tetap bekerja
>
> ✅ No FE impact

**IMP-008 — Request Validation (JSON Schema)** `M` `3 SP`

> `RequestValidationFilter` sudah ada tapi `getSchemaForPath()` return `null` (stub).
> Load actual OpenAPI schemas dari tiap service.
>
> **Acceptance Criteria**:
>
> - [ ] Load OpenAPI specs dari service registry
> - [ ] Validate request body + query params
> - [ ] 400 Bad Request dengan detail error
>
> ✅ No FE impact

**IMP-009 — Response Masking** `S` `2 SP`

> Strip internal fields (trace IDs, internal error codes) dari partner API response.
>
> **Acceptance Criteria**:
>
> - [ ] Configurable field whitelist/blacklist per partner
> - [ ] Hanya untuk partner/external API path
>
> ✅ No FE impact — masking hanya untuk external partner path

</details>

---

## 🟦 E-03 — Frontend Quality

> **Goal**: Fix bugs dan technical debt di `web-app` frontend.
> Semua item independent — bisa dikerjakan tanpa backend changes (kecuali IMP-015).

| Key     | Type  | Summary                    | Priority   | SP  | Component(s) | Labels                | Status   |
| :------ | :---- | :------------------------- | :--------- | :-: | :----------- | :-------------------- | :------- |
| IMP-004 | Story | 429 Rate Limit Handling    | 🔴 Highest |  2  | `web-app`    | `frontend`            | 📋 To Do |
| IMP-010 | Bug   | FxService Double-Prefix    | 🟠 High    |  1  | `web-app`    | `frontend` `bug`      | 📋 To Do |
| IMP-011 | Bug   | Pocket Type Inconsistency  | 🟠 High    |  1  | `web-app`    | `frontend` `bug`      | 📋 To Do |
| IMP-014 | Story | Duplicate Type Definitions | 🟡 Medium  |  2  | `web-app`    | `frontend` `cleanup`  | 📋 To Do |
| IMP-015 | Story | Financial Data in URL      | 🟡 Medium  |  1  | `web-app`    | `frontend` `security` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-004 — 429 Rate Limit Handling** `S` `2 SP`

> Frontend **zero awareness** terhadap rate limiting. Tidak ada handling HTTP 429.
>
> **Acceptance Criteria**:
>
> - [ ] Axios interceptor untuk 429
> - [ ] Parse `Retry-After` header
> - [ ] User-friendly toast: "Terlalu banyak permintaan, coba lagi dalam X detik"
> - [ ] Exponential backoff auto-retry
>
> 🔴 FE-only change

**IMP-010 — FxService Double-Prefix Bug** `XS` `1 SP`

> `FxService.ts` sets `baseUrl = '/api/v1/fx'` tapi Axios `baseURL` sudah `/api/v1`.
> Request jadi `/api/v1/api/v1/fx/rates/...` (404).
>
> **Fix**: Ubah `baseUrl` ke `/fx`.
>
> 🔴 FE-only fix

**IMP-011 — Pocket Type Inconsistency** `XS` `1 SP`

> `types/index.ts` defines `'MAIN' | 'SAVING' | 'SHARED' | 'SAVINGS' | 'GOAL'`
> tapi `WalletService.ts` defines `'SAVINGS' | 'SHARED' | 'GOAL'`. Konsolidasi.
>
> 🔴 FE-only fix

**IMP-014 — Duplicate Type Definitions** `S` `2 SP`

> `BalanceResponse`, `Transaction`, `WalletTransaction`, `Pocket` didefinisikan di
> `types/index.ts` DAN di masing-masing service file. Risiko drift.
>
> 🔴 FE-only refactor

**IMP-015 — Financial Data in URL** `XS` `1 SP`

> `LendingService.processRepayment()` kirim `amount` sebagai query param.
> `activatePayLater()` kirim `userId` sebagai query param. Pindah ke request body.
>
> ⚠️ FE + BE change — backward compatible jika BE accept both

</details>

---

## 🟦 E-04 — API Management & Analytics

> **Goal**: Evolve gateway dari basic proxy ke API management-capable platform.
> Quick wins sekarang, 3scale/Kong adoption nanti saat 5+ partner.
>
> **Arsitektur Target (2-Tier Gateway):**
>
> ```
> Partner (TokoBapak/Nobar/Dolan/Sinau/Maca)
>        │
>        ▼
> ┌─────────────────────────────┐
> │  3scale / Kong / Gravitee   │  ← API Management Layer
> │  - Developer Portal          │     (plans, keys, monetization)
> │  - Usage Analytics           │
> │  - Rate Plans per Partner    │
> └──────────┬──────────────────┘
>            ▼
> ┌─────────────────────────────┐
> │  PayU Gateway (Quarkus)     │  ← Banking Logic Layer
> │  - Idempotency Filter        │     (banking-specific,
> │  - HMAC Request Signing      │      tidak ada di 3scale/Kong)
> │  - JWT Keycloak Validation   │
> │  - Circuit Breaker + Retry   │
> └──────────┬──────────────────┘
>            ▼
>     Backend Services
> ```

| Key     | Type  | Summary                         | Priority | SP  | Component(s)      | Labels               | Status   |
| :------ | :---- | :------------------------------ | :------- | :-: | :---------------- | :------------------- | :------- |
| IMP-016 | Story | Persistent API Analytics        | 🟠 High  |  3  | `gateway-service` | `backend` `gateway`  | 📋 To Do |
| IMP-017 | Story | Rate Plan per Partner           | 🟠 High  |  3  | `gateway-service` | `backend` `partner`  | 📋 To Do |
| IMP-018 | Story | Request/Response Transformation | 🟠 High  |  3  | `gateway-service` | `backend` `gateway`  | 📋 To Do |
| IMP-019 | Story | Adopt Red Hat 3scale            | ⚪ Low   |  5  | Platform          | `platform` `partner` | 📋 To Do |
| IMP-020 | Story | Alternative: Kong/Gravitee      | ⚪ Low   |  5  | Platform          | `platform` `partner` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-016 — Persistent API Analytics** `M` `3 SP`

> `ApiAnalyticsService` in-memory (data hilang saat pod restart).
> Pindah ke Redis/TimescaleDB. Foundation untuk usage dashboard & billing.
>
> **Acceptance Criteria**:
>
> - [ ] Persistent storage (Redis atau TimescaleDB)
> - [ ] Tracking per-partner, per-endpoint, per-method
> - [ ] Retention policy (90 days detailed, 1 year aggregated)
>
> ✅ No FE impact

**IMP-017 — Rate Plan per Partner** `M` `3 SP`

> Rate limit global → config-driven per partner.
> TokoBapak: 1000 req/min, Nobar: 500 req/min.
>
> **Blocked by**: IMP-005 (konsolidasi rate limiting)
>
> **Acceptance Criteria**:
>
> - [ ] Rate plan entity + CRUD
> - [ ] Link partner → rate plan
> - [ ] Override per-endpoint dalam plan
>
> ✅ No FE impact

**IMP-018 — Request/Response Transformation** `M` `3 SP`

> Lightweight transformation: header injection, field masking, request enrichment.
>
> **Acceptance Criteria**:
>
> - [ ] Configurable transformation rules per route
> - [ ] Header injection (add/remove/rewrite)
> - [ ] Body field masking untuk sensitive data
>
> ✅ No FE impact — transformasi hanya untuk partner API path

**IMP-019 — Adopt Red Hat 3scale** `L` `5 SP`

> **Trigger**: ≥5 partner aktif, atau kebutuhan API monetization.
> 3scale APIcast di depan PayU gateway (2-tier).
> PayU gateway tetap handle banking-specific logic.
>
> ✅ No FE impact

**IMP-020 — Alternative: Kong/Gravitee** `L` `5 SP`

> **Trigger**: Budget constraint + ≥5 partner.
> Same 2-tier pattern. Evaluate Kong (OSS) atau Gravitee.io.
>
> ✅ No FE impact

</details>

---

## 🟦 E-05 — Product Catalog

> **Goal**: Pindahkan hardcoded product parameters ke database.
> Produk baru tidak perlu redeploy.

| Key     | Type  | Summary                     | Priority | SP  | Component(s)  | Labels           | Status   |
| :------ | :---- | :-------------------------- | :------- | :-: | :------------ | :--------------- | :------- |
| IMP-006 | Story | Product Catalog (DB-driven) | 🟠 High  |  5  | Multi-service | `backend` `core` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-006 — Product Catalog (DB-driven)** `L` `5 SP`

> Semua parameter produk hardcoded: `MINIMUM_SAVINGS_BALANCE = 10000`, `LoanType` enum,
> interest rates per instance. Buat entity `ProductDefinition` di DB.
>
> **Acceptance Criteria**:
>
> - [ ] `ProductDefinition` entity + CRUD API
> - [ ] Migrate hardcoded constants ke DB seed
> - [ ] Service-specific product lookups (account, loan, investment)
> - [ ] Admin UI di backoffice untuk manage products
>
> ⚠️ FE perlu extend: dropdown jadi dynamic dari API (non-breaking)

</details>

---

## 🟦 E-06 — Developer Hub (Backstage)

> **Goal**: Deploy Red Hat Developer Hub (Backstage) sebagai Internal Developer Portal.
> Strategi hybrid: Backstage untuk internal, developer-docs untuk external partner.

| Key     | Type  | Summary                        | Priority  | SP  | Component(s)         | Labels          | Status   |
| :------ | :---- | :----------------------------- | :-------- | :-: | :------------------- | :-------------- | :------- |
| IMP-021 | Story | Deploy Developer Hub           | 🟡 Medium |  3  | Platform             | `platform` `dx` | 📋 To Do |
| IMP-022 | Story | Service Catalog (catalog-info) | 🟡 Medium |  2  | All services         | `platform` `dx` | 📋 To Do |
| IMP-023 | Story | OpenAPI Coverage 16% → 80%+    | 🟡 Medium |  3  | `api-portal-service` | `backend` `dx`  | 📋 To Do |
| IMP-024 | Story | Backstage Software Templates   | 🟡 Medium |  3  | Platform             | `platform` `dx` | 📋 To Do |
| IMP-025 | Task  | Backstage TechDocs Integration | 🟡 Medium |  2  | Platform             | `platform` `dx` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-021 — Deploy Developer Hub** `M` `3 SP`

> Red Hat Developer Hub (Backstage) on OpenShift. Service catalog, CI/CD visibility,
> dependency graph, Keycloak SSO pre-integrated.
>
> **Acceptance Criteria**:
>
> - [ ] Developer Hub deployed on OpenShift
> - [ ] Keycloak SSO configured
> - [ ] Service catalog populated dari `catalog-info.yaml`
>
> ✅ No FE impact — internal tool

**IMP-022 — Service Catalog** `S` `2 SP`

> `catalog-info.yaml` per service (22 files). Owner, lifecycle, dependencies, API spec.
> Replace manual `SERVICES_STATUS.md`.
>
> **Acceptance Criteria**:
>
> - [ ] 22 `catalog-info.yaml` files
> - [ ] Backstage auto-discovery configured
> - [ ] Dependency graph visible
>
> ✅ No FE impact

**IMP-023 — OpenAPI Coverage 16% → 80%+** `M` `3 SP`

> Hanya 24/154 endpoint punya annotation. Tambah `@Operation`, `@ApiResponse`, `@Schema`.
> Prerequisite supaya Backstage API tab berguna.
>
> **Acceptance Criteria**:
>
> - [ ] ≥80% endpoints annotated
> - [ ] OpenAPI specs auto-generated valid
> - [ ] API Portal aggregation working
>
> ✅ No FE impact — annotation only

**IMP-024 — Software Templates** `M` `3 SP`

> Template scaffolding "New PayU Microservice": repo structure (hexagonal), Containerfile,
> Helm chart, CI pipeline, `catalog-info.yaml`.
>
> ✅ No FE impact

**IMP-025 — TechDocs Integration** `S` `2 SP`

> Connect `docs/` ke Backstage TechDocs plugin. Render per-service markdown di portal.
>
> ✅ No FE impact

</details>

---

## 🟦 E-07 — gRPC Inter-Service Communication

> **Goal**: Migrasi inter-service communication dari REST/JSON ke gRPC/Protobuf.
> REST tetap untuk gateway→frontend/partner.
>
> **Arsitektur Target (Hybrid REST + gRPC):**
>
> ```
> ┌─────────────────────────────────────────────────┐
> │              External (REST/JSON)                │
> │  Frontend (Next.js) ──REST──→ gateway-service    │
> │  Partners (SNAP-BI) ──REST──→ gateway-service    │
> │  Mobile (React Native) ──REST──→ gateway-service │
> └────────────────────────┬────────────────────────┘
>                          │ REST (public API)
>                          ▼
>                   ┌──────────────┐
>                   │   Gateway    │
>                   │  (Quarkus)   │
>                   └──────┬───────┘
>                          │ gRPC (internal)
>           ┌──────────────┼──────────────────┐
>           ▼              ▼                  ▼
>     ┌──────────┐  ┌──────────────┐  ┌──────────────┐
>     │ account  │  │ transaction  │  │   wallet     │
>     │ service  │  │   service    │  │   service    │
>     └──────────┘  └──────┬───────┘  └──────────────┘
>                          │ gRPC              ▲ gRPC
>                          ▼                   │
>                   ┌──────────────┐     ┌─────┴────────┐
>                   │   wallet     │     │ billing, fx  │
>                   │   service    │     │ invest, promo│
>                   └──────────────┘     │ statement    │
>                                        └──────────────┘
> ```
>
> **Rationale**:
>
> - Protobuf binary ~5-10x lebih compact dari JSON
> - ~2-5x latency improvement pada hot path
> - Compile-time contract (`.proto`) — breaking change ketahuan saat build
> - HTTP/2 multiplexing = 1 connection handle semua call
> - Istio native gRPC-aware load balancing
> - Spring Boot 3.4 `spring-grpc` (GA), Quarkus built-in gRPC support
>
> **Current State (Audit Feb 26, 2026):**
>
> - RestTemplate: 8 services | OpenFeign: 2 aktif, 2 unused | WebClient: 1 (Keycloak)
> - gRPC: 0 — tidak ada `.proto` atau dependency
> - Circular dep: `wallet-service` ↔ `fx-service`
> - Hottest service: `wallet-service` (6 synchronous callers)

| Key     | Type  | Summary                         | Priority  | SP  | Component(s)          | Labels                   | Blocked By    | Status   |
| :------ | :---- | :------------------------------ | :-------- | :-: | :-------------------- | :----------------------- | :------------ | :------- |
| IMP-026 | Story | Shared gRPC Starter Library     | 🟠 High   |  3  | `shared/grpc-starter` | `backend` `grpc`         | —             | 📋 To Do |
| IMP-027 | Story | Wallet gRPC Server              | 🟠 High   |  3  | `wallet-service`      | `backend` `grpc`         | IMP-026       | 📋 To Do |
| IMP-028 | Story | Migrate Wallet Callers to gRPC  | 🟠 High   |  5  | Multi-service (6)     | `backend` `grpc`         | IMP-027       | 📋 To Do |
| IMP-029 | Story | Account gRPC Server             | 🟡 Medium |  3  | `account-service`     | `backend` `grpc`         | IMP-026       | 📋 To Do |
| IMP-030 | Story | Transaction gRPC Server         | 🟡 Medium |  3  | `transaction-service` | `backend` `grpc`         | IMP-026       | 📋 To Do |
| IMP-031 | Story | Break wallet↔fx Circular Dep    | 🟠 High   |  3  | `wallet` + `fx`       | `backend` `architecture` | IMP-027       | 📋 To Do |
| IMP-032 | Task  | Standardize REST Client Cleanup | 🟡 Medium |  2  | Multi-service         | `backend` `cleanup`      | IMP-028       | 📋 To Do |
| IMP-033 | Story | Gateway gRPC→REST Bridge        | 🟡 Medium |  3  | `gateway-service`     | `backend` `grpc`         | IMP-027,29,30 | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-026 — Shared gRPC Starter Library** `M` `3 SP` 🏁 Foundation

> `backend/shared/grpc-starter`: common protobuf types (`Money`, `Timestamp`, `PageRequest`,
> `ErrorDetail`), gRPC interceptors (tracing, auth propagation, error mapping),
> Spring Boot auto-configuration.
>
> **Acceptance Criteria**:
>
> - [ ] Maven module `grpc-starter` di `backend/shared/`
> - [ ] Common `.proto` types di `src/main/proto/payu/common/`
> - [ ] gRPC server interceptor: tracing, auth context propagation
> - [ ] gRPC client interceptor: error mapping, retry
> - [ ] Spring Boot auto-configuration
> - [ ] Unit tests
>
> ✅ No FE impact

**IMP-027 — Wallet gRPC Server** `M` `3 SP`

> `WalletService.proto`: getBalance, debit, credit, transfer, getHistory.
> gRPC server port 9090 alongside existing REST. REST tetap hidup selama migrasi.
>
> **Acceptance Criteria**:
>
> - [ ] `wallet-service.proto` dengan semua RPC methods
> - [ ] gRPC server di port 9090
> - [ ] Existing REST endpoints untouched
> - [ ] Integration test dengan gRPC client
>
> ✅ No FE impact

**IMP-028 — Migrate Wallet Callers to gRPC** `L` `5 SP`

> Update 6 services: transaction, billing, investment, fx, promotion, statement.
> Replace `RestTemplate`/`WalletClient` adapters → gRPC stubs.
> Hexagonal port interface — hanya ganti adapter, domain logic untouched.
>
> **Acceptance Criteria**:
>
> - [ ] 6 services migrated ke gRPC wallet client
> - [ ] Old REST adapters removed
> - [ ] All existing tests still pass
> - [ ] Latency benchmark before/after
>
> ✅ No FE impact

**IMP-029 — Account gRPC Server** `M` `3 SP`

> `AccountService.proto`: getAccount, verifyAccount, getAccountsByUser.
> Migrate callers: transaction (RestTemplate), lending (Feign).
>
> ✅ No FE impact

**IMP-030 — Transaction gRPC Server** `M` `3 SP`

> `TransactionService.proto`: getTransaction, getHistory, getByReference.
> Migrate callers: lending (Feign), statement (RestTemplate).
>
> ✅ No FE impact

**IMP-031 — Break wallet↔fx Circular Dependency** `M` `3 SP`

> `fx-service` push rate updates via Kafka event → `wallet-service` consume & cache lokal.
> gRPC hanya 1 arah: `fx-service` → `wallet-service` untuk balance ops.
>
> **Acceptance Criteria**:
>
> - [ ] Kafka topic `fx-rates-updated`
> - [ ] `wallet-service` consume & cache rates lokal
> - [ ] Remove `FxRateProviderAdapter` (REST call ke fx-service) dari wallet
> - [ ] Verify no circular dependency
>
> ✅ No FE impact

**IMP-032 — Standardize REST Client Cleanup** `S` `2 SP`

> Hapus unused Feign deps. Standardize ke `RestClient` (Spring 6.1+) untuk external calls only.
> Buat `rest-client-starter` di shared/ dengan retry + circuit breaker baked in.
>
> ✅ No FE impact

**IMP-033 — Gateway gRPC→REST Bridge** `M` `3 SP`

> Gateway terima REST dari frontend/partner → forward via gRPC ke backend.
> `quarkus-grpc` client. Gateway = REST↔gRPC + Protobuf↔JSON translation layer.
>
> ✅ No FE impact — FE tetap REST

</details>

> **Execution Order**:
> `IMP-026` → `IMP-027` → `IMP-028` → `IMP-031` → `IMP-029` + `IMP-030` (parallel) → `IMP-033` → `IMP-032`

---

## 🟦 E-08 — Legacy Integration Layer

> **Goal**: Siapkan integration layer untuk legacy systems (SWIFT, OJK, SOAP).
> Future-proofing — build saat ada kebutuhan nyata.

| Key     | Type  | Summary                  | Priority | SP  | Component(s) | Labels                  | Status   |
| :------ | :---- | :----------------------- | :------- | :-: | :----------- | :---------------------- | :------- |
| IMP-013 | Story | Apache Camel Integration | ⚪ Low   |  5  | New module   | `backend` `integration` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-013 — Apache Camel Integration Layer** `L` `5 SP`

> Integrasi ke SWIFT XML, format OJK CSV/XML, SOAP endpoints.
> Lebih ringan dari webMethods, Red Hat supported (Red Hat Build of Apache Camel).
>
> **Trigger**: Kebutuhan integrasi ke legacy system dari bank partner.
>
> ✅ No FE impact

</details>

---

## 🟦 E-09 — Partner Integration Foundation

> **Goal**: Core capabilities yang dibutuhkan **semua** 5 partner.
> Ini adalah foundation sebelum partner-specific features bisa dikerjakan.
>
> Detail arsitektur → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)

| Key     | Type  | Summary                                 | Priority   | SP  | Component(s)         | Labels               | Status   |
| :------ | :---- | :-------------------------------------- | :--------- | :-: | :------------------- | :------------------- | :------- |
| GAP-001 | Story | Outbound Webhook Service                | 🔴 Highest |  5  | New module           | `partner` `gateway`  | 📋 To Do |
| GAP-002 | Story | Multi-tenancy / Data Isolation          | 🔴 Highest |  5  | Multi-service        | `partner` `security` | 📋 To Do |
| GAP-006 | Story | Idempotency Key (All Payment Endpoints) | 🔴 Highest |  3  | `gateway` + services | `partner` `core`     | 📋 To Do |
| GAP-005 | Story | API Key Management                      | 🟠 High    |  5  | `partner-service`    | `partner` `security` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**GAP-001 — Outbound Webhook Service** `L` `5 SP`

> Notify partner saat payment done. Retry logic, signature verification,
> delivery tracking dashboard.
>
> **Acceptance Criteria**:
>
> - [ ] Webhook registration API (URL, events, secret)
> - [ ] HMAC signature on payload
> - [ ] Retry with exponential backoff (max 5 attempts)
> - [ ] Delivery log + dashboard
>
> **Relevan untuk**: Semua partner

**GAP-002 — Multi-tenancy / Data Isolation** `L` `5 SP`

> Data isolation per partner. Options: row-level security, schema-per-tenant, atau DB-per-tenant.
>
> **Acceptance Criteria**:
>
> - [ ] Tenant context propagation via JWT/header
> - [ ] Row-level filtering pada semua queries
> - [ ] Audit log per tenant
>
> **Relevan untuk**: Semua partner

**GAP-006 — Idempotency Key** `M` `3 SP`

> `X-Idempotency-Key` header di semua payment/transfer endpoints.
> Store + deduplicate di Redis/DB.
>
> **Acceptance Criteria**:
>
> - [ ] Idempotency filter di gateway
> - [ ] Redis-backed dedup store (TTL 24h)
> - [ ] Return cached response for duplicate keys
>
> **Relevan untuk**: Semua partner

**GAP-005 — API Key Management** `L` `5 SP`

> Stable, non-expiring API keys. CRUD, rotation, per-partner rate plan linkage.
>
> **Acceptance Criteria**:
>
> - [ ] API key generation + rotation
> - [ ] Key → Partner → Rate Plan mapping
> - [ ] Revocation support
>
> **Relevan untuk**: Semua partner

</details>

---

## 🟦 E-10 — Escrow & Marketplace Payments

> **Goal**: Payment holding dan split payment untuk marketplace-style partners.

| Key     | Type  | Summary                        | Priority   | SP  | Component(s)      | Labels                    | Status   |
| :------ | :---- | :----------------------------- | :--------- | :-: | :---------------- | :------------------------ | :------- |
| GAP-007 | Story | Escrow / Payment Holding       | 🔴 Highest |  5  | `wallet-service`  | `partner` `core` `escrow` | 📋 To Do |
| GAP-011 | Story | Split Payment (Multi-merchant) | 🟠 High    |  5  | `transaction-svc` | `partner` `marketplace`   | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**GAP-007 — Escrow / Payment Holding** `L` `5 SP`

> Hold payment sampai condition terpenuhi (barang diterima, event selesai).
> Release/refund flow. Escrow wallet account type.
>
> **Acceptance Criteria**:
>
> - [ ] Escrow wallet (LIABILITY account in CoA)
> - [ ] Hold → Release → Settle flow
> - [ ] Hold → Refund flow
> - [ ] Expiry timeout (auto-refund)
>
> **Relevan untuk**: TokoBapak, Dolan

**GAP-011 — Split Payment** `L` `5 SP`

> Multi-merchant dalam 1 checkout. Split amount ke multiple wallets atomically.
>
> **Acceptance Criteria**:
>
> - [ ] Split config (merchant list + percentage/fixed)
> - [ ] Atomic multi-wallet debit/credit
> - [ ] Settlement per merchant
>
> **Relevan untuk**: Dolan

</details>

---

## 🟦 E-11 — Subscription & Recurring Billing

> **Goal**: Subscription engine dan installment/PayLater integration.

| Key     | Type  | Summary                            | Priority   | SP  | Component(s)      | Labels              | Status   |
| :------ | :---- | :--------------------------------- | :--------- | :-: | :---------------- | :------------------ | :------- |
| GAP-008 | Story | Subscription / Recurring Billing   | 🔴 Highest |  5  | `billing-service` | `partner` `billing` | 📋 To Do |
| GAP-012 | Story | Installment / PayLater Integration | 🟠 High    |  3  | `lending-service` | `partner` `lending` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**GAP-008 — Subscription / Recurring Billing** `L` `5 SP`

> Recurring charge engine: daily/weekly/monthly. Trial period, grace period,
> dunning (retry failed charges). Webhook notification per cycle.
>
> **Acceptance Criteria**:
>
> - [ ] Subscription plan entity (interval, price, trial)
> - [ ] Scheduler for recurring charges
> - [ ] Dunning: retry failed charges (3 attempts, then suspend)
> - [ ] Webhook: subscription.created, charge.succeeded, charge.failed
>
> **Relevan untuk**: Nobar, Sinau, Maca

**GAP-012 — Installment / PayLater Integration** `M` `3 SP`

> PayLater checkout flow via gateway. Tenor selection, approval, disbursement.
>
> **Acceptance Criteria**:
>
> - [ ] Tenor options endpoint
> - [ ] Approval + disbursement flow
> - [ ] Installment schedule generation
>
> **Relevan untuk**: Dolan, Sinau

</details>

---

## 🟦 E-12 — Settlement & Financial Operations

> **Goal**: Settlement, reconciliation, pricing, dan multi-currency capabilities.

| Key     | Type  | Summary                         | Priority | SP  | Component(s)      | Labels              | Status   |
| :------ | :---- | :------------------------------ | :------- | :-: | :---------------- | :------------------ | :------- |
| GAP-003 | Story | Settlement & Reconciliation     | 🟠 High  |  5  | `wallet-service`  | `partner` `finops`  | 📋 To Do |
| GAP-004 | Story | Rate Card / Pricing per Partner | 🟠 High  |  3  | `partner-service` | `partner` `billing` | 📋 To Do |
| GAP-010 | Story | Multi-currency Settlement       | 🟠 High  |  5  | `fx-service`      | `partner` `fx`      | 📋 To Do |
| GAP-013 | Story | Revenue Share / Royalty Engine  | 🟠 High  |  3  | New module        | `partner` `finops`  | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**GAP-003 — Settlement & Reconciliation** `L` `5 SP`

> Payout ke merchant/instructor/author. Daily settlement cycle,
> reconciliation report, discrepancy flagging.
>
> **Acceptance Criteria**:
>
> - [ ] Daily settlement batch job
> - [ ] Reconciliation report generation
> - [ ] Discrepancy detection + alert
> - [ ] Manual override for exceptions
>
> **Relevan untuk**: TokoBapak, Dolan, Sinau, Maca

**GAP-004 — Rate Card / Pricing** `M` `3 SP`

> Config-driven fee/commission per partner per transaction type.
>
> **Acceptance Criteria**:
>
> - [ ] Rate card entity + CRUD
> - [ ] Fee calculation engine (flat, percentage, tiered)
> - [ ] Link partner → rate card
>
> **Relevan untuk**: Semua partner

**GAP-010 — Multi-currency Settlement** `L` `5 SP`

> FX-aware settlement. Convert & settle in partner's preferred currency.
>
> **Acceptance Criteria**:
>
> - [ ] Partner currency preference configuration
> - [ ] Auto-conversion at settlement time
> - [ ] FX rate locking for settlement window
>
> **Relevan untuk**: Dolan, Maca

**GAP-013 — Revenue Share / Royalty Engine** `M` `3 SP`

> Auto split revenue per sale. Configurable split ratio per product/partner.
>
> **Acceptance Criteria**:
>
> - [ ] Revenue split config (percentage per stakeholder)
> - [ ] Auto-split at settlement time
> - [ ] Royalty statement generation
>
> **Relevan untuk**: Sinau, Maca

</details>

---

## 🟦 E-13 — Dispute Resolution

> **Goal**: Refund dan dispute management untuk partner transactions.

| Key     | Type  | Summary                     | Priority | SP  | Component(s)      | Labels              | Status   |
| :------ | :---- | :-------------------------- | :------- | :-: | :---------------- | :------------------ | :------- |
| GAP-009 | Story | Refund & Dispute Management | 🟠 High  |  5  | `transaction-svc` | `partner` `dispute` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**GAP-009 — Refund & Dispute Management** `L` `5 SP`

> Full + partial refund. Dispute lifecycle: open → investigate → resolve.
> Chargeback handling, evidence upload.
>
> **Acceptance Criteria**:
>
> - [ ] Full refund endpoint
> - [ ] Partial refund endpoint
> - [ ] Dispute lifecycle (OPEN → INVESTIGATING → RESOLVED/REJECTED)
> - [ ] Evidence attachment (file upload)
> - [ ] Webhook: refund.created, dispute.opened, dispute.resolved
>
> **Relevan untuk**: TokoBapak, Dolan, Sinau

</details>

---

## � E-14 — Consumer Banking Experience

> **Goal**: Fitur-fitur consumer banking yang ada di BCA Digital (blu), Jago, GoPay, OVO.
> Meningkatkan UX dan personal finance management untuk end-user PayU.
>
> **Referensi**: BCA Digital (blu), Jago, GoPay, OVO, DANA

| Key     | Type  | Summary                         | Priority  | SP  | Component(s)                    | Labels               | Status   |
| :------ | :---- | :------------------------------ | :-------- | :-: | :------------------------------ | :------------------- | :------- |
| IMP-034 | Story | Transaction Notes / Memo        | 🟠 High   |  1  | `transaction-service`           | `backend` `frontend` | 📋 To Do |
| IMP-035 | Story | Beneficiary Management          | 🟠 High   |  2  | `account-service`               | `backend` `frontend` | 📋 To Do |
| IMP-036 | Story | P2P Transfer via Phone Lookup   | 🟠 High   |  2  | `account-svc` + `txn-svc`       | `backend` `frontend` | 📋 To Do |
| IMP-037 | Story | Transaction Tagging             | 🟡 Medium |  2  | `transaction-svc` + `analytics` | `backend` `frontend` | 📋 To Do |
| IMP-038 | Story | QR Pay (P2P Scan-to-Transfer)   | 🟡 Medium |  2  | `account-svc` + `txn-svc`       | `backend` `frontend` | 📋 To Do |
| IMP-039 | Story | Savings Goals (Target Tabungan) | 🟡 Medium |  3  | `wallet-service`                | `backend` `frontend` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-034 — Transaction Notes / Memo** `XS` `1 SP`

> Attach personal note/memo di setiap transaksi. "Bayar makan siang", "Cicilan bulan ke-3".
> Visible ke sender, opsional shared ke recipient.
>
> **Referensi**: BCA Digital (blu), GoPay, Jago
>
> **Acceptance Criteria**:
>
> - [ ] `memo` field di `Transaction` entity (nullable, max 140 chars)
> - [ ] Include `memo` di create transaction request DTO
> - [ ] Include `memo` di transaction history response
> - [ ] Flyway migration tambah kolom
>
> ⚠️ **FE Impact**: Input field `memo` di form transfer + tampilkan di detail transaksi

**IMP-035 — Beneficiary Management (Rekening Favorit)** `S` `2 SP`

> Simpan rekening tujuan yang sering dipakai. Nickname, bank code, account number.
> Validasi via BI-FAST account inquiry.
>
> **Referensi**: BCA Digital (blu), GoPay, OVO
>
> **Acceptance Criteria**:
>
> - [ ] `Beneficiary` entity (userId, bankCode, accountNumber, accountName, nickname)
> - [ ] CRUD endpoints: `POST/GET/PUT/DELETE /accounts/{id}/beneficiaries`
> - [ ] Validate account via BI-FAST inquiry on create
> - [ ] Max 50 beneficiaries per user
>
> ⚠️ **FE Impact**: Page "Daftar Favorit" — list, tambah, edit, hapus saved accounts

**IMP-036 — P2P Transfer via Phone Lookup** `S` `2 SP`

> Transfer uang cukup input nomor HP, system resolve ke PayU account.
> Tampilkan konfirmasi nama sebelum transfer.
>
> **Referensi**: GoPay, OVO, DANA, Flip
>
> **Acceptance Criteria**:
>
> - [ ] `GET /accounts/lookup?phone=08xxxx` → return account name (masked)
> - [ ] `POST /transactions/p2p` accept `destinationPhone` selain `destinationAccount`
> - [ ] Phone number indexed di account table
>
> ⚠️ **FE Impact**: Input nomor HP di transfer flow, konfirmasi nama sebelum kirim

**IMP-037 — Transaction Tagging / Manual Categorization** `S` `2 SP`

> User bisa tag/kategorisasi transaksi manual (override auto-category dari analytics).
> Custom tags untuk personal finance management.
>
> **Referensi**: BCA Digital (blu), Jago, Jenius
>
> **Acceptance Criteria**:
>
> - [ ] `tags` field di Transaction (JSONB array)
> - [ ] `PATCH /transactions/{id}/tags` endpoint
> - [ ] Analytics-service respect user override tags
> - [ ] Predefined categories + custom tags
>
> ⚠️ **FE Impact**: Tag picker / dropdown di detail transaksi

**IMP-038 — QR Pay (P2P Scan-to-Transfer)** `S` `2 SP`

> User tunjukkan QR pribadi, user lain scan → langsung transfer.
> Beda dari merchant QRIS — ini user-to-user.
>
> **Referensi**: GoPay, OVO, DANA, ShopeePay
>
> **Acceptance Criteria**:
>
> - [ ] `GET /accounts/{id}/qr` → generate QR code (encode account ID + checksum)
> - [ ] `POST /transactions/qr-pay` — decode QR, resolve account, initiate P2P transfer
> - [ ] QR contains: `payu://p2p?account={id}&check={hash}`
>
> ⚠️ **FE Impact**: Tab "QR Saya" di wallet + QR scanner

**IMP-039 — Savings Goals (Target Tabungan)** `M` `3 SP`

> Buat target menabung linked ke pocket. Track progress, auto-allocate, celebrate achievement.
> Multi-pocket sudah ada tapi belum ada goal/target mechanism.
>
> **Referensi**: BCA Digital (blu), DANA, Jago
>
> **Acceptance Criteria**:
>
> - [ ] `SavingsGoal` entity (name, targetAmount, deadline, pocketId, progress)
> - [ ] CRUD: `POST/GET/PUT/DELETE /wallets/{id}/savings-goals`
> - [ ] Auto-calculate progress (currentBalance / targetAmount × 100%)
> - [ ] Optional: auto-transfer percentage dari incoming funds
>
> ⚠️ **FE Impact**: UI target tabungan di wallet/pocket section, progress bar

</details>

---

## 🟦 E-15 — Payment Gateway Features

> **Goal**: Core payment gateway capabilities yang diperlukan untuk bersaing dengan Xendit/Midtrans.
> Virtual Account, Payment Link, Hosted Checkout, dan payment lifecycle management.
>
> **Referensi**: Xendit, Midtrans, GoPay, OVO

| Key     | Type  | Summary                      | Priority   | SP  | Component(s)           | Labels               | Status   |
| :------ | :---- | :--------------------------- | :--------- | :-: | :--------------------- | :------------------- | :------- |
| IMP-040 | Story | Payment Link / Invoice       | 🔴 Highest |  3  | `partner-service`      | `partner` `gateway`  | 📋 To Do |
| IMP-041 | Story | Payment Method Selection API | 🟠 High    |  3  | `gateway-service`      | `gateway` `partner`  | 📋 To Do |
| IMP-042 | Story | Virtual Account (VA) Payment | 🟠 High    |  5  | `transaction-service`  | `backend` `gateway`  | 📋 To Do |
| IMP-043 | Story | Hosted Checkout Page         | 🟠 High    |  5  | `gateway-svc` + FE     | `gateway` `frontend` | 📋 To Do |
| IMP-044 | Story | Payment Expiry & Auto-Cancel | 🟠 High    |  2  | `transaction-service`  | `backend` `gateway`  | 📋 To Do |
| IMP-045 | Story | Dynamic QR for Merchants     | 🟡 Medium  |  5  | `partner-service`      | `partner` `gateway`  | 📋 To Do |
| IMP-046 | Story | Checkout Deeplink            | 🟡 Medium  |  2  | `gateway-svc` + mobile | `gateway` `mobile`   | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-040 — Payment Link / Invoice Generation** `M` `3 SP`

> Generate shareable URL pembayaran dengan amount, description, dan expiry.
> Payer buka link, pilih payment method, bayar. Status tracking per link.
>
> **Referensi**: Xendit Payment Links, Midtrans Payment Link, Flip
>
> **Acceptance Criteria**:
>
> - [ ] `PaymentLink` entity (slug, amount, description, expiry, status)
> - [ ] `POST /partners/{id}/payment-links` → generate unique URL
> - [ ] `GET /pay/{slug}` → public endpoint for payer
> - [ ] Status: ACTIVE → PAID / EXPIRED
> - [ ] Webhook notification on payment completion
>
> ✅ No FE impact (web-app) — partner-facing API. Checkout page di IMP-043.

**IMP-041 — Payment Method Selection API** `M` `3 SP`

> Return daftar payment methods yang available (wallet, VA, QRIS, bank transfer, PayLater)
> beserta eligibility check, fee, dan estimated time per method.
>
> **Referensi**: Midtrans Get Payment Methods, Xendit
>
> **Acceptance Criteria**:
>
> - [ ] `GET /payments/{id}/methods` → list available methods
> - [ ] Eligibility check per method (balance, KYC status, limits)
> - [ ] Fee calculation per method
> - [ ] Estimated settlement time
>
> ✅ No FE impact (web-app) — partner/checkout API

**IMP-042 — Virtual Account (VA) Payment Collection** `L` `5 SP`

> Generate nomor VA di partner bank (BCA VA, BNI VA, Mandiri VA, Permata VA).
> Auto-reconcile saat bank konfirmasi pembayaran. VA lifecycle: PENDING → PAID → EXPIRED.
>
> **Referensi**: Xendit, Midtrans, DANA
>
> **Acceptance Criteria**:
>
> - [ ] `VirtualAccount` entity (vaNumber, bankCode, amount, status, expiry)
> - [ ] `POST /payments/va` → create VA with generated number
> - [ ] Bank callback endpoint for payment confirmation (simulated)
> - [ ] VA simulator in `simulators/` directory
> - [ ] Auto-expire unpaid VA after TTL
>
> ✅ No FE impact (web-app) — partner API + simulator

**IMP-043 — Hosted Checkout Page (Snap-style)** `L` `5 SP`

> Server-rendered checkout page yang aggregasi semua payment methods.
> Partner redirect buyer ke sini, PayU handle method selection dan payment.
> Return checkout token untuk embedding.
>
> **Referensi**: Midtrans Snap, Xendit Checkout
>
> **Acceptance Criteria**:
>
> - [ ] `POST /checkout/tokens` → generate checkout token
> - [ ] Checkout HTML page listing available payment methods
> - [ ] Select method → initiate payment → redirect callback
> - [ ] Embeddable via iframe atau redirect
>
> ⚠️ **FE Impact**: Halaman checkout **baru** (bukan di web-app consumer, tapi di gateway/checkout context)

**IMP-044 — Payment Expiry & Auto-Cancel** `S` `2 SP`

> Pembayaran pending (VA, payment links, QR) otomatis expire setelah configurable timeout.
> Release held amount, notify partner via webhook.
>
> **Referensi**: Xendit, Midtrans, semua payment gateway
>
> **Acceptance Criteria**:
>
> - [ ] `expiresAt` field di payment entity
> - [ ] Scheduler job (Quartz/cron) scan & cancel expired payments
> - [ ] Release any reserved balance
> - [ ] Kafka event `payment.expired` → webhook notification
>
> ✅ No FE impact — backend scheduler

**IMP-045 — Dynamic QR for Merchant Payment** `L` `5 SP`

> Merchant generate QRIS dinamis per transaksi (bukan static QR test).
> Customer scan, konfirmasi, bayar. Merchant-facing flow + onboarding.
>
> **Referensi**: GoPay, OVO, DANA, ShopeePay
>
> **Acceptance Criteria**:
>
> - [ ] Merchant entity + onboarding API
> - [ ] `POST /merchants/{id}/qr` → dynamic QR with amount
> - [ ] Payment acceptance flow via QRIS simulator
> - [ ] Settlement to merchant wallet
>
> ✅ No FE impact (web-app) — merchant/partner-facing

**IMP-046 — Checkout Deeplink** `S` `2 SP`

> Generate deep links yang open PayU mobile app ke payment flow.
> `payu://pay?token=xxx`. Partner embed di app mereka.
>
> **Referensi**: GoPay, OVO, DANA, ShopeePay
>
> **Acceptance Criteria**:
>
> - [ ] URL scheme definition: `payu://pay`, `payu://topup`, `payu://transfer`
> - [ ] `POST /deeplinks` → generate signed deeplink URL
> - [ ] Mobile app URL handler (Expo Linking)
>
> ✅ No FE impact (web-app) — mobile app handler

</details>

---

## 🟦 E-16 — Disbursement & Smart Routing

> **Goal**: API untuk kirim uang ke rekening bank luar (disbursement/payout).
> Smart routing pilih jalur termurah. Batch processing untuk payroll/merchant payout.
>
> **Referensi**: Xendit, Flip, DANA

| Key     | Type  | Summary                   | Priority  | SP  | Component(s)          | Labels               | Status   |
| :------ | :---- | :------------------------ | :-------- | :-: | :-------------------- | :------------------- | :------- |
| IMP-047 | Story | Disbursement / Payout API | 🟠 High   |  5  | `transaction-service` | `backend` `gateway`  | 📋 To Do |
| IMP-048 | Story | Bulk/Batch Disbursement   | 🟡 Medium |  5  | `transaction-service` | `backend` `gateway`  | 📋 To Do |
| IMP-049 | Story | Transfer Fee Optimization | 🟡 Medium |  2  | `transaction-service` | `backend` `frontend` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-047 — Disbursement / Payout API** `L` `5 SP`

> API kirim uang dari PayU ke rekening bank luar (single disbursement).
> Route via BI-FAST simulator. Status tracking + callback on completion.
> Beda dari internal transfer — ini hit external banking rails.
>
> **Referensi**: Xendit Disbursement, Flip, DANA
>
> **Acceptance Criteria**:
>
> - [ ] `POST /disbursements` → create single disbursement
> - [ ] Route through BI-FAST simulator
> - [ ] Status: PENDING → PROCESSING → COMPLETED / FAILED
> - [ ] Callback/webhook on completion
> - [ ] Idempotency key support
>
> ✅ No FE impact (web-app) — partner/backoffice API

**IMP-048 — Bulk/Batch Disbursement** `L` `5 SP`

> Upload batch disbursement (JSON array atau CSV). Proses async via Kafka.
> Progress tracking, partial failure handling. Untuk payroll, merchant payouts.
>
> **Referensi**: Xendit Batch Disbursement, Flip
>
> **Acceptance Criteria**:
>
> - [ ] `POST /disbursements/batch` → accept array/CSV
> - [ ] Async processing via Kafka
> - [ ] `GET /disbursements/batch/{id}` → progress + individual statuses
> - [ ] Partial failure: continue processing remaining items
>
> **Blocked by**: IMP-047
>
> ✅ No FE impact (web-app) — partner/backoffice API

**IMP-049 — Transfer Fee Optimization / Smart Routing** `S` `2 SP`

> Pilih jalur transfer termurah (BI-FAST vs RTGS vs SKN/LLG) berdasarkan
> amount, speed, dan fee. Tampilkan perbandingan biaya ke user.
>
> **Referensi**: Flip
>
> **Acceptance Criteria**:
>
> - [ ] Fee table per channel (BI-FAST, RTGS, SKN)
> - [ ] `GET /transfers/routes?amount=X&bank=Y` → return available routes + fees
> - [ ] Auto-select cheapest route (atau user pilih)
> - [ ] Routing config di DB (bukan hardcoded)
>
> ⚠️ **FE Impact**: Tampilkan perbandingan biaya jalur di form transfer

</details>

---

## 🟦 E-17 — Promotion Engine Wiring

> **Goal**: Wire existing promotion/cashback infrastructure ke actual transaction flow.
> Entity sudah ada, tapi belum terintegrasi ke checkout dan post-transaction.
>
> **Referensi**: OVO, GoPay, DANA, ShopeePay

| Key     | Type  | Summary                               | Priority | SP  | Component(s)        | Labels              | Status   |
| :------ | :---- | :------------------------------------ | :------- | :-: | :------------------ | :------------------ | :------- |
| IMP-050 | Story | Checkout Promo Code Redemption        | 🟠 High  |  3  | `promotion-service` | `backend` `partner` | 📋 To Do |
| IMP-051 | Story | Cashback Auto-Apply after Transaction | 🟠 High  |  3  | `promotion-service` | `backend` `partner` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-050 — Checkout Promo Code Redemption** `M` `3 SP`

> Apply kode promo saat checkout. Validasi eligibility, hitung diskon, return adjusted amount.
> Entity promo sudah ada, tapi belum ada "apply at checkout" API.
>
> **Referensi**: GoPay, OVO, DANA, Tokopedia
>
> **Acceptance Criteria**:
>
> - [ ] `POST /promotions/apply` — input: promo code + transaction context
> - [ ] Validate: code valid, not expired, user eligible, min transaction
> - [ ] Return: discount amount, adjusted total, promo details
> - [ ] Mark promo usage (prevent double-use)
>
> ✅ No FE impact (web-app) — partner checkout API. Consumer app bisa extend later.

**IMP-051 — Cashback Auto-Apply after Transaction** `M` `3 SP`

> Setelah transaksi sukses, otomatis evaluasi semua active cashback rules.
> Credit eligible cashback ke user wallet. Entity cashback sudah ada tapi
> belum di-wire ke Kafka transaction events.
>
> **Referensi**: OVO, GoPay, DANA, ShopeePay
>
> **Acceptance Criteria**:
>
> - [ ] Kafka consumer listen `transaction.completed` events
> - [ ] Evaluate active cashback rules (amount threshold, category, partner)
> - [ ] Credit cashback amount ke user wallet via wallet-service
> - [ ] Notification: "Cashback Rp5.000 diterima!"
>
> ⚠️ **FE Impact**: Toast/notification "Cashback diterima" di web-app

</details>

---

## 🟦 E-18 — Developer Experience (Partner)

> **Goal**: Tools dan SDK untuk partner developers agar integrasi lebih cepat.
> Sandbox environment, client libraries, dan dokumentasi interaktif.
>
> **Referensi**: Xendit, Midtrans, Flip (semua punya SDK + sandbox)

| Key     | Type  | Summary                  | Priority  | SP  | Component(s)             | Labels               | Status   |
| :------ | :---- | :----------------------- | :-------- | :-: | :----------------------- | :------------------- | :------- |
| IMP-052 | Story | Sandbox Test Environment | 🟠 High   |  3  | `api-portal` + `partner` | `platform` `dx`      | 📋 To Do |
| IMP-053 | Story | Partner SDK Generation   | 🟡 Medium |  5  | New `sdk/` module        | `platform` `dx`      | 📋 To Do |
| IMP-054 | Story | Spending Limits / Budget | 🟡 Medium |  3  | `account` + `txn`        | `backend` `frontend` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-052 — Sandbox Test Environment** `M` `3 SP`

> Sandbox environment terpisah dengan test API keys, test bank accounts,
> dan deterministic behavior (no random failures).
> Partner dapat sandbox keys saat registrasi.
>
> **Referensi**: Xendit, Midtrans, Flip — semua punya sandbox mode
>
> **Acceptance Criteria**:
>
> - [ ] `sandbox` flag di API key entity
> - [ ] Sandbox requests route ke simulators (tanpa random failure)
> - [ ] Seed test data: test accounts, test merchants, test VA
> - [ ] Sandbox dashboard di developer-docs
>
> ✅ No FE impact (web-app) — developer portal

**IMP-053 — Partner SDK Generation** `L` `5 SP`

> Auto-generate TypeScript/Java SDK dari OpenAPI spec.
> Published sebagai npm/Maven package. Reduce partner integration time.
>
> **Referensi**: Xendit SDK, Midtrans SDK (all have official client libraries)
>
> **Acceptance Criteria**:
>
> - [ ] OpenAPI Generator config untuk TypeScript + Java
> - [ ] Generated SDK handles auth, retry, error parsing
> - [ ] README + quickstart per language
> - [ ] CI pipeline auto-generate on OpenAPI spec change
>
> **Blocked by**: IMP-023 (OpenAPI Coverage 80%+)
>
> ✅ No FE impact — developer tool

**IMP-054 — Spending Limits / Budget Management** `M` `3 SP`

> User set batas pengeluaran bulanan per kategori (e.g., "Max Rp2jt for food")
> atau global daily transfer limit. System block atau warn saat mendekati/melebihi.
>
> **Referensi**: BCA Digital (blu), OVO
>
> **Acceptance Criteria**:
>
> - [ ] `Budget` entity (userId, category, limitAmount, period, currentSpent)
> - [ ] CRUD: `POST/GET/PUT/DELETE /accounts/{id}/budgets`
> - [ ] Check budget pada setiap transaksi (warn/block mode)
> - [ ] Monthly reset scheduler
> - [ ] Notification saat 80% / 100% limit tercapai
>
> ⚠️ **FE Impact**: Settings page budget per kategori, progress bar

</details>

---

## 🟦 E-19 — Transaction Proof & Receipts

> **Goal**: Generate bukti transaksi (receipt) yang bisa di-download dan di-share.
> Beda dari e-statement (ringkasan bulanan) — ini per-transaksi.
>
> **Referensi**: BCA Digital (blu), GoPay, OVO, semua bank

| Key     | Type  | Summary                              | Priority | SP  | Component(s)        | Labels               | Status   |
| :------ | :---- | :----------------------------------- | :------- | :-: | :------------------ | :------------------- | :------- |
| IMP-055 | Story | Transaction Receipt (Bukti Transfer) | 🟠 High  |  2  | `statement-service` | `backend` `frontend` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-055 — Transaction Receipt / Bukti Transfer** `S` `2 SP`

> Generate PDF/image receipt untuk transaksi yang sudah selesai.
> Berisi: transaction ID, amount, timestamp, sender, recipient, bank reference.
> Share via WhatsApp/email. Reuse PDF engine dari statement-service.
>
> **Referensi**: BCA Digital (blu), GoPay, OVO — semua bank punya ini
>
> **Acceptance Criteria**:
>
> - [ ] `GET /statements/receipts/{transactionId}` → return PDF
> - [ ] Receipt template dengan branding PayU
> - [ ] Include: txn ID, date, amount, sender, recipient, reference, status
> - [ ] Reuse existing PDF generation dari statement-service
>
> ⚠️ **FE Impact**: Tombol "Download Bukti" / "Share" di detail transaksi

</details>

---

---

## 🟦 E-20 — Code Health & Technical Hygiene

> **Goal**: Fix bug arsitektur, dead code, dan inkonsistensi konfigurasi yang ditemukan saat deep audit.
> Semua Quick Win — bisa dikerjakan per-item tanpa design besar.
>
> **Source**: Deep architecture audit (Feb 26, 2026)

| Key     | Type | Summary                                      | Priority   | SP  | Component(s)             | Labels                  | Status   |
| :------ | :--- | :------------------------------------------- | :--------- | :-: | :----------------------- | :---------------------- | :------- |
| IMP-056 | Bug  | In-Memory Reservation Map                    | 🔴 Highest |  2  | `transaction-service`    | `backend` `bug`         | 📋 To Do |
| IMP-057 | Task | Remove Dead CloudEventPublisher              | 🟠 High    |  1  | `shared/events-starter`  | `backend` `cleanup`     | 📋 To Do |
| IMP-058 | Bug  | Gateway Query Parameter Loss                 | 🔴 Highest |  1  | `gateway-service`        | `gateway` `bug`         | 📋 To Do |
| IMP-059 | Task | Deduplicate InsufficientFundsException       | 🟠 High    |  1  | `shared/api-commons`     | `backend` `cleanup`     | 📋 To Do |
| IMP-060 | Task | Consume archunit-starter in Services         | 🟡 Medium  |  2  | All Spring Boot services | `backend` `quality`     | 📋 To Do |
| IMP-061 | Task | Disable open-in-view Universally             | 🔴 Highest |  1  | All Spring Boot services | `backend` `performance` | 📋 To Do |
| IMP-062 | Bug  | Fix Kafka Config Path in txn-service         | 🔴 Highest |  1  | `transaction-service`    | `backend` `bug`         | 📋 To Do |
| IMP-063 | Bug  | WalletEntity Missing tenantId in Constructor | 🟠 High    |  1  | `wallet-service`         | `backend` `bug`         | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-056 — In-Memory Reservation Map** `S` `2 SP` 🔴

> `WalletServiceAdapter` di transaction-service simpan `transactionId→reservationId` di
> `ConcurrentHashMap`. Multi-instance deployment = commit/release gagal kalau ke-route ke
> pod berbeda. Harus persist ke DB atau carry via saga context (`TransferSagaContext`).
>
> **Acceptance Criteria**:
>
> - [ ] Remove `ConcurrentHashMap` reservation mapping
> - [ ] Persist reservation ID di `Transaction` entity atau `SagaInstance` context
> - [ ] Verify commit/release works across different pods
> - [ ] Unit test: reservation created on pod A, committed on pod B
>
> ✅ No FE impact

**IMP-057 — Remove Dead CloudEventPublisher** `XS` `1 SP`

> `events-starter` define `CloudEventPublisher<T>` interface tapi **tidak ada implementasi**.
> Services bypass langsung ke outbox-starter. Interface dead code — implement
> `KafkaCloudEventPublisher` atau hapus interface yang misleading.
>
> **Acceptance Criteria**:
>
> - [ ] Either: implement `KafkaCloudEventPublisher` bridging to outbox
> - [ ] Or: remove `CloudEventPublisher` interface entirely
> - [ ] Update imports di services yang reference it
>
> ✅ No FE impact

**IMP-058 — Gateway Query Parameter Loss** `XS` `1 SP` 🔴

> Gateway proxy build downstream URL dari `@PathParam("path")` saja.
> Query parameters (`?page=1&size=10`) **tidak di-forward** ke downstream service.
> Semua paginated/filtered GET via gateway return unfiltered results.
>
> **Acceptance Criteria**:
>
> - [ ] Forward `UriInfo.getQueryParameters()` ke downstream request
> - [ ] Verify: `GET /api/v1/transactions?page=2&size=10` → downstream receives query params
> - [ ] Integration test with pagination
>
> ✅ No FE impact — tapi fix ini **unblock** semua FE pagination yang lewat gateway

**IMP-059 — Deduplicate InsufficientFundsException** `XS` `1 SP`

> Dua class exist: `api.common.exception.InsufficientFundsException` dan
> `api.common.money.InsufficientFundsException`. Services bisa import yang salah →
> `GlobalExceptionHandler` tidak handle karena beda class.
>
> **Acceptance Criteria**:
>
> - [ ] Keep satu, hapus satu (prefer `api.common.exception`)
> - [ ] Update all imports across services
> - [ ] Verify `GlobalExceptionHandler` handles it correctly
>
> ✅ No FE impact

**IMP-060 — Consume archunit-starter in Services** `S` `2 SP`

> `archunit-starter` punya 10+ reusable rules (`HexagonalArchitectureRules`) tapi **0 service
> konsumsi**. Semua 18 service tulis ArchUnit rules sendiri → inkonsistensi enforcement.
>
> **Acceptance Criteria**:
>
> - [ ] Add `archunit-starter` dependency ke all Spring Boot services
> - [ ] Replace custom rules dengan shared `HexagonalArchitectureRules`
> - [ ] Keep service-specific rules yang unique
> - [ ] All architecture tests pass
>
> ✅ No FE impact

**IMP-061 — Disable open-in-view Universally** `XS` `1 SP` 🔴

> Hanya 3/16 Spring Boot services set `spring.jpa.open-in-view: false`.
> Default Spring Boot = `true` → Hibernate session tetap open selama request rendering →
> N+1 query risk, unexpected lazy loading di REST controller.
>
> **Acceptance Criteria**:
>
> - [ ] Add `spring.jpa.open-in-view: false` ke semua Spring Boot services
> - [ ] Fix any `LazyInitializationException` yang muncul (fetch join atau DTO projection)
> - [ ] Verify no regression
>
> ✅ No FE impact

**IMP-062 — Fix Kafka Config Path in txn-service** `XS` `1 SP` 🔴

> Transaction-service pakai `kafka:` bukan `spring.kafka:` di application.yml.
> Spring Boot auto-config baca dari `spring.kafka.*` → properties silently ignored,
> fallback ke default (no `acks=all`, no `enable.idempotence`). **Data loss risk**.
>
> **Acceptance Criteria**:
>
> - [ ] Rename `kafka:` → `spring.kafka:` di application.yml
> - [ ] Verify Kafka producer config applied (`acks=all`, `enable.idempotence=true`)
> - [ ] Verify consumer config applied (group.id, auto-offset-reset)
>
> ✅ No FE impact

**IMP-063 — WalletEntity Missing tenantId in Constructor** `XS` `1 SP`

> `WalletEntity` all-args constructor **tidak include `tenantId`** tapi kolom
> `@Column(nullable = false)`. Saat multi-tenancy enforced (GAP-002) → persistence failure.
>
> **Acceptance Criteria**:
>
> - [ ] Add `tenantId` parameter ke all-args constructor
> - [ ] Or: use `@Builder` pattern yang include all fields
> - [ ] Verify entity creation with tenant context
>
> ✅ No FE impact

</details>

---

## 🟦 E-21 — Security Hardening (Defaults & Audit)

> **Goal**: Perbaiki security defaults di shared libraries agar banking-grade.
> Fail-closed, bukan fail-open.
>
> **Source**: Deep architecture audit (Feb 26, 2026)

| Key     | Type  | Summary                          | Priority   | SP  | Component(s)              | Labels               | Status   |
| :------ | :---- | :------------------------------- | :--------- | :-: | :------------------------ | :------------------- | :------- |
| IMP-064 | Story | Security Auto-Config Fail-Closed | 🔴 Highest |  3  | `shared/security-starter` | `backend` `security` | 📋 To Do |
| IMP-065 | Story | AuditAspect Use SecurityContext  | 🟠 High    |  2  | `shared/security-starter` | `backend` `security` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-064 — Security Auto-Config Fail-Closed** `M` `3 SP`

> `SecurityAutoConfiguration` pakai `matchIfMissing = false` untuk semua features
> (encryption, masking, audit). Artinya tambah dependency `security-starter` **tidak
> aktifkan apapun** tanpa config eksplisit. Banking platform harus fail-closed —
> at minimum `data-masking` dan `audit-logging` harus default `true`.
>
> **Acceptance Criteria**:
>
> - [ ] `payu.security.data-masking-enabled` default → `true`
> - [ ] `payu.security.audit-enabled` default → `true`
> - [ ] `payu.security.encryption-enabled` tetap `false` (butuh key config)
> - [ ] Update all services yang override: verify no regression
> - [ ] Document breaking change di CHANGELOG
>
> ✅ No FE impact

**IMP-065 — AuditAspect Use SecurityContext** `S` `2 SP`

> `AuditAspect.extractUserId()` baca `X-User-Id` header dan `request.getAttribute("principal")`
> tapi **tidak cek** `SecurityContextHolder.getContext().getAuthentication()`.
> Audit entries show "anonymous" untuk JWT-authenticated users.
>
> **Acceptance Criteria**:
>
> - [ ] Check `SecurityContextHolder` first (JWT subject/preferred_username)
> - [ ] Fallback chain: SecurityContext → X-User-Id header → "anonymous"
> - [ ] Unit test: verify audit log contains correct userId from JWT
>
> ✅ No FE impact

</details>

---

## 🟦 E-22 — Gateway Reactive & Resilience

> **Goal**: Fix gateway agar leverage arsitektur reactive Quarkus + protect dari downstream failures.
>
> **Source**: Deep architecture audit (Feb 26, 2026)

| Key     | Type  | Summary                             | Priority   | SP  | Component(s)      | Labels                  | Status   |
| :------ | :---- | :---------------------------------- | :--------- | :-: | :---------------- | :---------------------- | :------- |
| IMP-066 | Story | Remove @Blocking from Gateway Proxy | 🟠 High    |  3  | `gateway-service` | `gateway` `performance` | 📋 To Do |
| IMP-067 | Story | Wire Circuit Breaker to proxy()     | 🔴 Highest |  3  | `gateway-service` | `gateway` `resilience`  | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-066 — Remove @Blocking from Gateway Proxy** `M` `3 SP`

> `ApiGatewayResource` di-annotasi `@Blocking` → force semua request ke worker thread pool
> (bukan Vert.x event loop). Negate arsitektur reactive Quarkus. Vert.x WebClient calls
> sudah reactive (`Uni<Response>`) tapi `@Blocking` force context switch per request.
>
> **Acceptance Criteria**:
>
> - [ ] Remove `@Blocking` dari `ApiGatewayResource`
> - [ ] Ensure all handler methods return `Uni<Response>` (reactive)
> - [ ] Verify filters compatible dengan non-blocking execution
> - [ ] Load test: compare throughput before/after
>
> ✅ No FE impact

**IMP-067 — Wire Circuit Breaker to proxy() Method** `M` `3 SP`

> Gateway `proxy()` method **tidak ada circuit breaker protection**. Kalau downstream
> service mati → setiap request timeout 30 detik (TCP timeout). `@CircuitBreaker` dan
> `@Retry` annotations ada di class tapi tidak applied ke `proxy()`.
>
> **Note**: Terkait IMP-003 (Wire Circuit Breaker) tapi IMP-003 tentang konsolidasi
> RetryAndTimeoutService — ini tentang actual proxy method protection.
>
> **Acceptance Criteria**:
>
> - [ ] Apply `@CircuitBreaker` per downstream service (bukan global)
> - [ ] Fallback: return 503 Service Unavailable dengan retry-after header
> - [ ] Health endpoint reflects circuit state per service
> - [ ] Config: failure threshold 50%, wait 30s, sliding window 10 calls
>
> ✅ No FE impact

</details>

---

## 🟦 E-23 — Shared Library Lifecycle Management

> **Goal**: Improve shared libraries agar production-grade — managed thread pools,
> compile-time type safety, proper Spring lifecycle integration.
>
> **Source**: Deep architecture audit (Feb 26, 2026)

| Key     | Type  | Summary                         | Priority  | SP  | Component(s)                    | Labels                  | Status   |
| :------ | :---- | :------------------------------ | :-------- | :-: | :------------------------------ | :---------------------- | :------- |
| IMP-068 | Story | Spring-Managed Thread Pools     | 🟠 High   |  3  | `shared/saga-starter` + `cache` | `backend` `reliability` | 📋 To Do |
| IMP-069 | Story | MapStruct Entity-Domain Mapping | 🟡 Medium |  8  | New `shared/mapper-starter`     | `backend` `quality`     | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-068 — Spring-Managed Thread Pools** `M` `3 SP`

> `SagaOrchestrator.SAGA_EXECUTOR` dan `CacheService.REFRESH_EXECUTOR` pakai static
> `Executors.newCachedThreadPool()`. Tidak di-manage Spring lifecycle, tidak bisa
> di-monitor via Micrometer, tidak shutdown graceful → thread leak saat pod restart.
>
> **Acceptance Criteria**:
>
> - [ ] Replace static executors dengan `@Bean TaskExecutor` (Spring-managed)
> - [ ] Configure `ThreadPoolTaskExecutor` dengan bounded pool (core=4, max=16, queue=100)
> - [ ] Register executor metrics di Micrometer (`executor.pool.size`, `executor.active`)
> - [ ] Verify graceful shutdown: pending tasks complete before pod termination
>
> ✅ No FE impact

**IMP-069 — MapStruct Entity-Domain Mapping** `XL` `8 SP`

> `WalletPersistenceAdapter` punya ~100 baris manual `toEntity()`/`toDomain()` mapping.
> 22 services × multiple entities = maintenance burden + risiko missed field saat
> entity berubah. MapStruct generate compile-time type-safe mapping.
>
> **Acceptance Criteria**:
>
> - [ ] Add MapStruct dependency ke parent POM (`dependencyManagement`)
> - [ ] Create `shared/mapper-starter` dengan base mapper config
> - [ ] Migrate wallet-service adapters ke MapStruct (pilot)
> - [ ] Document pattern untuk other services
> - [ ] Annotation processor config di maven-compiler-plugin (alongside Lombok)
>
> ✅ No FE impact

</details>

---

## 🔧 Tech Debt

| Key      | Type      | Summary                                | Priority  | SP  | Component(s)         | Status   |
| :------- | :-------- | :------------------------------------- | :-------- | :-: | :------------------- | :------- |
| SIMP-001 | Tech Debt | Remove `ab-testing-service`            | 🟠 High   |  2  | `ab-testing-service` | 📋 To Do |
| SIMP-002 | Tech Debt | Remove Gamification from promotion-svc | 🟡 Medium |  2  | `promotion-service`  | 📋 To Do |
| SIMP-003 | Tech Debt | Remove Robo-advisory from investment   | 🟡 Medium |  2  | `investment-service` | 📋 To Do |

<details>
<summary>📄 Details</summary>

> **SIMP-001**: `ab-testing-service` broken, tidak relevan untuk payment gateway. Ganti feature flags via env var.
> **SIMP-002**: Hapus `GamificationService.java`, keep `LoyaltyPoints` + `CashbackService`.
> **SIMP-003**: Hapus robo-advisory, simplify ke portfolio view + mutual fund mock.

</details>

---

## 🔍 Spikes (Research / Architecture Decision)

| Key      | Type  | Question                                                  | Impact                            | Status   |
| :------- | :---- | :-------------------------------------------------------- | :-------------------------------- | :------- |
| ARCH-001 | Spike | KYC di level PayU atau project client?                    | Scope `kyc-service`               | 📋 To Do |
| ARCH-002 | Spike | Statement: PDF end-user atau JSON/CSV project client?     | Output format `statement-service` | 📋 To Do |
| ARCH-003 | Spike | Support ticket: end-user PayU atau project client?        | Multi-tenancy `support-service`   | 📋 To Do |
| ARCH-004 | Spike | CMS: hanya PayU web-app atau multi-tenant project client? | Multi-tenant mode `cms-service`   | 📋 To Do |

---

## 🔮 Deferred (Icebox)

| Key       | Type  | Summary                               | Notes                                            |
| :-------- | :---- | :------------------------------------ | :----------------------------------------------- |
| P2-FE-003 | Story | Mobile App Feature Parity (Expo/RN)   | ❄️ Deferred                                      |
| OCP-007   | Story | Service Mesh mTLS enforcement         | ❄️ Planned                                       |
| OCP-010   | Story | API versioning headers                | ❄️ Planned                                       |
| DR-001    | Story | Disaster Recovery live test execution | ❄️ Scripts ready                                 |
| DEFER-001 | Story | Card Tokenization & 3DS               | ❄️ Requires PCI-DSS scope + card network kontrak |

---

## 📊 Metrics

### Story Points by Epic

| Epic | Name                           | Stories |   SP    |
| :--- | :----------------------------- | :-----: | :-----: |
| E-01 | Core Banking Ledger            |    3    |   13    |
| E-02 | Gateway Hardening              |    5    |   13    |
| E-03 | Frontend Quality               |    5    |    7    |
| E-04 | API Management & Analytics     |    5    |   19    |
| E-05 | Product Catalog                |    1    |    5    |
| E-06 | Developer Hub (Backstage)      |    5    |   13    |
| E-07 | gRPC Inter-Service Comm.       |    8    |   25    |
| E-08 | Legacy Integration             |    1    |    5    |
| E-09 | Partner Foundation             |    4    |   18    |
| E-10 | Escrow & Marketplace           |    2    |   10    |
| E-11 | Subscription & Billing         |    2    |    8    |
| E-12 | Settlement & FinOps            |    4    |   16    |
| E-13 | Dispute Resolution             |    1    |    5    |
| E-14 | Consumer Banking Experience    |    6    |   12    |
| E-15 | Payment Gateway Features       |    7    |   25    |
| E-16 | Disbursement & Smart Routing   |    3    |   12    |
| E-17 | Promotion Engine Wiring        |    2    |    6    |
| E-18 | Developer Experience (Partner) |    3    |   11    |
| E-19 | Transaction Proof & Receipts   |    1    |    2    |
| E-20 | Code Health & Tech Hygiene     |    8    |   10    |
| E-21 | Security Hardening             |    2    |    5    |
| E-22 | Gateway Reactive & Resilience  |    2    |    6    |
| E-23 | Shared Library Lifecycle       |    2    |   11    |
|      | **TOTAL**                      | **82**  | **257** |

### Story Points by Label

| Label      | Total SP |
| :--------- | :------: |
| `backend`  |   131    |
| `partner`  |    80    |
| `gateway`  |    54    |
| `platform` |    29    |
| `frontend` |    25    |
| `dx`       |    21    |
| `security` |    5     |
| `mobile`   |    2     |

### FE Impact Summary

| Kategori                       | Count | Keys                                                                        |
| :----------------------------- | :---: | :-------------------------------------------------------------------------- |
| ✅ No FE impact (backend only) |  51   | IMP-002,003,005,007–009,012,013,016–033,040–042,044–048,050,052,053,056–069 |
| ⚠️ Extend FE (non-breaking)    |  12   | IMP-001,006,015,034–039,049,051,054,055                                     |
| 🔴 FE-only (independent)       |   4   | IMP-004, IMP-010, IMP-011, IMP-014                                          |
| 📱 Mobile impact               |   2   | IMP-046 (deeplink), IMP-043 (checkout page)                                 |

---

_Last Updated: February 26, 2026 | 23 Epics · 82 Stories · 257 SP · 3 Tech Debt · 4 Spikes · 5 Deferred_
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
_Referensi: BCA Digital (blu), Xendit, Midtrans, GoPay, OVO, DANA, Flip, Jago_
