# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| Status         | Count | Breakdown                                                       |
| :------------- | :---: | :-------------------------------------------------------------- |
| **Epics**      |  13   | 8 improvement, 4 partner/gateway, 1 infra                      |
| **Stories**     |  33   | IMP-001 – IMP-033                                               |
| **GAP Stories** |  13   | GAP-001 – GAP-013                                               |
| **Tech Debt**  |   3   | SIMP-001 – SIMP-003                                             |
| **Spikes**     |   4   | ARCH-001 – ARCH-004                                             |
| **Deferred**   |   4   | P2-FE-003, OCP-007, OCP-010, DR-001                            |
| **Bugs**       | 0/232 | ✅ 229 fixed, 4 Won't Do (BUG-BE-061, 076, 080, 091)           |

### 🐛 Bug Scorecard

| Kategori                  | Open  | Won't Do | Done | Total      |
| :------------------------ | :---: | :------: | :--: | :--------: |
| Backend Logic             |   0   |    3     | 144  | **147**    |
| Frontend Logic            |   0   |    0     |  46  | **46**     |
| Frontend-Backend Mismatch |   0   |    0     |  29  | **29**     |
| Auth / Session            |   0   |    0     |  10  | **10**     |
| **TOTAL**                 | **0** |  **4**   | 229  | **~232**   |

### Won't Do (4 items)

| Key            | Summary                                   | Resolution                                                 |
| :------------- | :---------------------------------------- | :--------------------------------------------------------- |
| BUG-BE-061     | Promotion `getTransactionAmount()` → ZERO | Won't Do — gamification opsional, bukan core banking       |
| BUG-BE-076     | API Portal sandbox in-memory              | Won't Do — partner belum ada, sandbox belum relevan        |
| BUG-BE-080     | Lending pre-approval endpoints missing    | Won't Do — feature belum aktif di frontend                 |
| BUG-BE-091     | Fixed-window rate limit burstable         | Won't Do — low-traffic fase awal. Superseded oleh IMP-005 |

---

## 🗂️ Epics Overview

### Priority Heatmap

| Epic | Name                              | Priority     | Stories | SP  | Quarter | Status      |
| :--- | :-------------------------------- | :----------- | :-----: | :-: | :------ | :---------- |
| E-01 | Core Banking Ledger               | 🔴 Highest   |    3    | 13  | Q1 2026 | 📋 To Do    |
| E-02 | Gateway Hardening                 | 🔴 Highest   |    5    | 11  | Q1 2026 | 📋 To Do    |
| E-03 | Frontend Quality                  | 🟠 High      |    5    |  7  | Q1 2026 | 📋 To Do    |
| E-04 | API Management & Analytics        | 🟠 High      |    5    | 19  | Q2 2026 | 📋 To Do    |
| E-05 | Product Catalog                   | 🟠 High      |    1    |  5  | Q2 2026 | 📋 To Do    |
| E-06 | Developer Hub (Backstage)         | 🟡 Medium    |    5    | 13  | Q2 2026 | 📋 To Do    |
| E-07 | gRPC Inter-Service Communication  | 🟡 Medium    |    8    | 25  | Q2 2026 | 📋 To Do    |
| E-08 | Legacy Integration Layer          | ⚪ Low       |    1    |  5  | Future  | 📋 To Do    |
| E-09 | Partner Integration Foundation    | 🔴 Highest   |    4    | 18  | Q1 2026 | 📋 To Do    |
| E-10 | Escrow & Marketplace Payments     | 🔴 Highest   |    2    | 10  | Q1 2026 | 📋 To Do    |
| E-11 | Subscription & Recurring Billing  | 🔴 Highest   |    2    |  8  | Q1 2026 | 📋 To Do    |
| E-12 | Settlement & Financial Operations | 🟠 High      |    4    | 16  | Q2 2026 | 📋 To Do    |
| E-13 | Dispute Resolution                | 🟠 High      |    1    |  5  | Q2 2026 | 📋 To Do    |

> **Story Points**: XS=1, S=2, M=3, L=5, XL=8
> **Labels**: `backend`, `frontend`, `gateway`, `platform`, `partner`, `security`, `grpc`, `dx`

---

## 🟦 E-01 — Core Banking Ledger

> **Goal**: Evolve `wallet-service` dari single-sided ledger ke true double-entry accounting
> system dengan Chart of Accounts dan General Ledger. Foundation untuk settlement, reconciliation,
> dan regulatory reporting OJK.

| Key     | Type  | Summary                     | Priority   | SP | Component(s)      | Labels                  | Status   |
| :------ | :---- | :-------------------------- | :--------- | :-: | :---------------- | :---------------------- | :------- |
| IMP-001 | Story | True Double-Entry Ledger    | 🔴 Highest | 5  | `wallet-service`  | `backend` `core`        | 📋 To Do |
| IMP-002 | Story | Chart of Accounts (CoA)     | 🔴 Highest | 3  | `wallet-service`  | `backend` `core`        | 📋 To Do |
| IMP-012 | Story | GL Engine Ringan            | 🟡 Medium  | 5  | new `gl-service`  | `backend` `finops`      | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-001 — True Double-Entry Ledger** `L` `5 SP`
> Saat ini `LedgerEntry` single-sided (1 DEBIT _atau_ 1 CREDIT per row, tanpa pairing).
> Implementasi `JournalEntry` parent yang enforce matching DEBIT+CREDIT pairs.
> Tambah trial balance verification.
>
> **Acceptance Criteria**:
> - [ ] `JournalEntry` entity dengan paired debit+credit `LedgerEntry`
> - [ ] Constraint: sum(debit) == sum(credit) per journal
> - [ ] Trial balance endpoint (`GET /wallets/trial-balance`)
> - [ ] Migration script Flyway untuk existing data
> - [ ] Unit tests + integration tests
>
> ⚠️ **FE Impact**: `WalletService.ts` perlu tambah `journalId` field + trial balance endpoint

**IMP-002 — Chart of Accounts (CoA)** `M` `3 SP`
> GL account classification: `ASSET:USER_WALLET`, `LIABILITY:ESCROW_HOLDING`,
> `REVENUE:TRANSACTION_FEE`. Enabler untuk settlement reconciliation & reporting OJK.
>
> **Acceptance Criteria**:
> - [ ] `ChartOfAccount` entity dengan hierarchical code structure
> - [ ] Seed data untuk standard banking CoA
> - [ ] Link `LedgerEntry` → `ChartOfAccount`
>
> ✅ No FE impact — backend/backoffice only

**IMP-012 — GL Engine Ringan** `L` `5 SP`
> General Ledger untuk settlement reconciliation (neraca, laba-rugi).
> Bisa di `wallet-service` atau service baru `gl-service`.
> Enabler untuk daily settlement report TokoBapak.
>
> **Acceptance Criteria**:
> - [ ] Balance sheet generation
> - [ ] Income statement generation
> - [ ] Daily settlement report endpoint
>
> **Blocked by**: IMP-001, IMP-002
>
> ✅ No FE impact — backoffice/reporting only

</details>

---

## 🟦 E-02 — Gateway Hardening

> **Goal**: Perkuat `gateway-service` dari pass-through proxy ke production-grade API Gateway
> dengan resilience, validation, rate limiting, dan response masking.

| Key     | Type  | Summary                      | Priority   | SP | Component(s)       | Labels              | Status   |
| :------ | :---- | :--------------------------- | :--------- | :-: | :----------------- | :------------------ | :------- |
| IMP-003 | Story | Wire Circuit Breaker + Retry | 🔴 Highest | 2  | `gateway-service`  | `backend` `gateway` | 📋 To Do |
| IMP-005 | Story | Konsolidasi Rate Limiting    | 🔴 Highest | 3  | `gateway-service`  | `backend` `gateway` | 📋 To Do |
| IMP-007 | Story | Dynamic Route Registry       | 🟠 High    | 3  | `gateway-service`  | `backend` `gateway` | 📋 To Do |
| IMP-008 | Story | Request Validation (Schema)  | 🟠 High    | 3  | `gateway-service`  | `backend` `gateway` | 📋 To Do |
| IMP-009 | Story | Response Masking             | 🟠 High    | 2  | `gateway-service`  | `backend` `gateway` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-003 — Wire Circuit Breaker + Retry** `S` `2 SP`
> `RetryAndTimeoutService` dan `@CircuitBreaker` sudah di-code tapi **tidak di-wire**
> ke `proxy()` method di `ApiGatewayResource`. Tinggal connect.
>
> **Acceptance Criteria**:
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
> - [ ] Load OpenAPI specs dari service registry
> - [ ] Validate request body + query params
> - [ ] 400 Bad Request dengan detail error
>
> ✅ No FE impact

**IMP-009 — Response Masking** `S` `2 SP`
> Strip internal fields (trace IDs, internal error codes) dari partner API response.
>
> **Acceptance Criteria**:
> - [ ] Configurable field whitelist/blacklist per partner
> - [ ] Hanya untuk partner/external API path
>
> ✅ No FE impact — masking hanya untuk external partner path

</details>

---

## 🟦 E-03 — Frontend Quality

> **Goal**: Fix bugs dan technical debt di `web-app` frontend.
> Semua item independent — bisa dikerjakan tanpa backend changes (kecuali IMP-015).

| Key     | Type  | Summary                     | Priority   | SP | Component(s) | Labels               | Status   |
| :------ | :---- | :-------------------------- | :--------- | :-: | :----------- | :------------------- | :------- |
| IMP-004 | Story | 429 Rate Limit Handling     | 🔴 Highest | 2  | `web-app`    | `frontend`           | 📋 To Do |
| IMP-010 | Bug   | FxService Double-Prefix     | 🟠 High    | 1  | `web-app`    | `frontend` `bug`     | 📋 To Do |
| IMP-011 | Bug   | Pocket Type Inconsistency   | 🟠 High    | 1  | `web-app`    | `frontend` `bug`     | 📋 To Do |
| IMP-014 | Story | Duplicate Type Definitions  | 🟡 Medium  | 2  | `web-app`    | `frontend` `cleanup` | 📋 To Do |
| IMP-015 | Story | Financial Data in URL       | 🟡 Medium  | 1  | `web-app`    | `frontend` `security`| 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-004 — 429 Rate Limit Handling** `S` `2 SP`
> Frontend **zero awareness** terhadap rate limiting. Tidak ada handling HTTP 429.
>
> **Acceptance Criteria**:
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

| Key     | Type  | Summary                          | Priority   | SP | Component(s)       | Labels                  | Status   |
| :------ | :---- | :------------------------------- | :--------- | :-: | :----------------- | :---------------------- | :------- |
| IMP-016 | Story | Persistent API Analytics         | 🟠 High    | 3  | `gateway-service`  | `backend` `gateway`     | 📋 To Do |
| IMP-017 | Story | Rate Plan per Partner            | 🟠 High    | 3  | `gateway-service`  | `backend` `partner`     | 📋 To Do |
| IMP-018 | Story | Request/Response Transformation  | 🟠 High    | 3  | `gateway-service`  | `backend` `gateway`     | 📋 To Do |
| IMP-019 | Story | Adopt Red Hat 3scale             | ⚪ Low     | 5  | Platform           | `platform` `partner`    | 📋 To Do |
| IMP-020 | Story | Alternative: Kong/Gravitee       | ⚪ Low     | 5  | Platform           | `platform` `partner`    | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-016 — Persistent API Analytics** `M` `3 SP`
> `ApiAnalyticsService` in-memory (data hilang saat pod restart).
> Pindah ke Redis/TimescaleDB. Foundation untuk usage dashboard & billing.
>
> **Acceptance Criteria**:
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
> - [ ] Rate plan entity + CRUD
> - [ ] Link partner → rate plan
> - [ ] Override per-endpoint dalam plan
>
> ✅ No FE impact

**IMP-018 — Request/Response Transformation** `M` `3 SP`
> Lightweight transformation: header injection, field masking, request enrichment.
>
> **Acceptance Criteria**:
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

| Key     | Type  | Summary                        | Priority | SP | Component(s)       | Labels                  | Status   |
| :------ | :---- | :----------------------------- | :------- | :-: | :----------------- | :---------------------- | :------- |
| IMP-006 | Story | Product Catalog (DB-driven)    | 🟠 High  | 5  | Multi-service      | `backend` `core`        | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-006 — Product Catalog (DB-driven)** `L` `5 SP`
> Semua parameter produk hardcoded: `MINIMUM_SAVINGS_BALANCE = 10000`, `LoanType` enum,
> interest rates per instance. Buat entity `ProductDefinition` di DB.
>
> **Acceptance Criteria**:
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

| Key     | Type  | Summary                          | Priority  | SP | Component(s)         | Labels              | Status   |
| :------ | :---- | :------------------------------- | :-------- | :-: | :------------------- | :------------------ | :------- |
| IMP-021 | Story | Deploy Developer Hub             | 🟡 Medium | 3  | Platform             | `platform` `dx`     | 📋 To Do |
| IMP-022 | Story | Service Catalog (catalog-info)   | 🟡 Medium | 2  | All services         | `platform` `dx`     | 📋 To Do |
| IMP-023 | Story | OpenAPI Coverage 16% → 80%+     | 🟡 Medium | 3  | `api-portal-service` | `backend` `dx`      | 📋 To Do |
| IMP-024 | Story | Backstage Software Templates     | 🟡 Medium | 3  | Platform             | `platform` `dx`     | 📋 To Do |
| IMP-025 | Task  | Backstage TechDocs Integration   | 🟡 Medium | 2  | Platform             | `platform` `dx`     | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-021 — Deploy Developer Hub** `M` `3 SP`
> Red Hat Developer Hub (Backstage) on OpenShift. Service catalog, CI/CD visibility,
> dependency graph, Keycloak SSO pre-integrated.
>
> **Acceptance Criteria**:
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
> - Protobuf binary ~5-10x lebih compact dari JSON
> - ~2-5x latency improvement pada hot path
> - Compile-time contract (`.proto`) — breaking change ketahuan saat build
> - HTTP/2 multiplexing = 1 connection handle semua call
> - Istio native gRPC-aware load balancing
> - Spring Boot 3.4 `spring-grpc` (GA), Quarkus built-in gRPC support
>
> **Current State (Audit Feb 26, 2026):**
> - RestTemplate: 8 services | OpenFeign: 2 aktif, 2 unused | WebClient: 1 (Keycloak)
> - gRPC: 0 — tidak ada `.proto` atau dependency
> - Circular dep: `wallet-service` ↔ `fx-service`
> - Hottest service: `wallet-service` (6 synchronous callers)

| Key     | Type  | Summary                          | Priority  | SP | Component(s)          | Labels                   | Blocked By    | Status   |
| :------ | :---- | :------------------------------- | :-------- | :-: | :-------------------- | :----------------------- | :------------ | :------- |
| IMP-026 | Story | Shared gRPC Starter Library      | 🟠 High   | 3  | `shared/grpc-starter` | `backend` `grpc`         | —             | 📋 To Do |
| IMP-027 | Story | Wallet gRPC Server               | 🟠 High   | 3  | `wallet-service`      | `backend` `grpc`         | IMP-026       | 📋 To Do |
| IMP-028 | Story | Migrate Wallet Callers to gRPC   | 🟠 High   | 5  | Multi-service (6)     | `backend` `grpc`         | IMP-027       | 📋 To Do |
| IMP-029 | Story | Account gRPC Server              | 🟡 Medium | 3  | `account-service`     | `backend` `grpc`         | IMP-026       | 📋 To Do |
| IMP-030 | Story | Transaction gRPC Server          | 🟡 Medium | 3  | `transaction-service` | `backend` `grpc`         | IMP-026       | 📋 To Do |
| IMP-031 | Story | Break wallet↔fx Circular Dep     | 🟠 High   | 3  | `wallet` + `fx`       | `backend` `architecture` | IMP-027       | 📋 To Do |
| IMP-032 | Task  | Standardize REST Client Cleanup  | 🟡 Medium | 2  | Multi-service         | `backend` `cleanup`      | IMP-028       | 📋 To Do |
| IMP-033 | Story | Gateway gRPC→REST Bridge         | 🟡 Medium | 3  | `gateway-service`     | `backend` `grpc`         | IMP-027,29,30 | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-026 — Shared gRPC Starter Library** `M` `3 SP` 🏁 Foundation
> `backend/shared/grpc-starter`: common protobuf types (`Money`, `Timestamp`, `PageRequest`,
> `ErrorDetail`), gRPC interceptors (tracing, auth propagation, error mapping),
> Spring Boot auto-configuration.
>
> **Acceptance Criteria**:
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

| Key     | Type  | Summary                      | Priority | SP | Component(s)   | Labels                  | Status   |
| :------ | :---- | :--------------------------- | :------- | :-: | :------------- | :---------------------- | :------- |
| IMP-013 | Story | Apache Camel Integration     | ⚪ Low   | 5  | New module     | `backend` `integration` | 📋 To Do |

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

| Key     | Type  | Summary                                | Priority     | SP | Component(s)        | Labels                  | Status   |
| :------ | :---- | :------------------------------------- | :----------- | :-: | :------------------ | :---------------------- | :------- |
| GAP-001 | Story | Outbound Webhook Service               | 🔴 Highest   | 5  | New module          | `partner` `gateway`     | 📋 To Do |
| GAP-002 | Story | Multi-tenancy / Data Isolation         | 🔴 Highest   | 5  | Multi-service       | `partner` `security`    | 📋 To Do |
| GAP-006 | Story | Idempotency Key (All Payment Endpoints)| 🔴 Highest   | 3  | `gateway` + services| `partner` `core`        | 📋 To Do |
| GAP-005 | Story | API Key Management                     | 🟠 High      | 5  | `partner-service`   | `partner` `security`    | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**GAP-001 — Outbound Webhook Service** `L` `5 SP`
> Notify partner saat payment done. Retry logic, signature verification,
> delivery tracking dashboard.
>
> **Acceptance Criteria**:
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
> - [ ] Idempotency filter di gateway
> - [ ] Redis-backed dedup store (TTL 24h)
> - [ ] Return cached response for duplicate keys
>
> **Relevan untuk**: Semua partner

**GAP-005 — API Key Management** `L` `5 SP`
> Stable, non-expiring API keys. CRUD, rotation, per-partner rate plan linkage.
>
> **Acceptance Criteria**:
> - [ ] API key generation + rotation
> - [ ] Key → Partner → Rate Plan mapping
> - [ ] Revocation support
>
> **Relevan untuk**: Semua partner

</details>

---

## 🟦 E-10 — Escrow & Marketplace Payments

> **Goal**: Payment holding dan split payment untuk marketplace-style partners.

| Key     | Type  | Summary                        | Priority     | SP | Component(s)        | Labels                        | Status   |
| :------ | :---- | :----------------------------- | :----------- | :-: | :------------------ | :---------------------------- | :------- |
| GAP-007 | Story | Escrow / Payment Holding       | 🔴 Highest   | 5  | `wallet-service`    | `partner` `core` `escrow`     | 📋 To Do |
| GAP-011 | Story | Split Payment (Multi-merchant) | 🟠 High      | 5  | `transaction-svc`   | `partner` `marketplace`       | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**GAP-007 — Escrow / Payment Holding** `L` `5 SP`
> Hold payment sampai condition terpenuhi (barang diterima, event selesai).
> Release/refund flow. Escrow wallet account type.
>
> **Acceptance Criteria**:
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
> - [ ] Split config (merchant list + percentage/fixed)
> - [ ] Atomic multi-wallet debit/credit
> - [ ] Settlement per merchant
>
> **Relevan untuk**: Dolan

</details>

---

## 🟦 E-11 — Subscription & Recurring Billing

> **Goal**: Subscription engine dan installment/PayLater integration.

| Key     | Type  | Summary                            | Priority     | SP | Component(s)      | Labels                  | Status   |
| :------ | :---- | :--------------------------------- | :----------- | :-: | :---------------- | :---------------------- | :------- |
| GAP-008 | Story | Subscription / Recurring Billing   | 🔴 Highest   | 5  | `billing-service` | `partner` `billing`     | 📋 To Do |
| GAP-012 | Story | Installment / PayLater Integration | 🟠 High      | 3  | `lending-service` | `partner` `lending`     | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**GAP-008 — Subscription / Recurring Billing** `L` `5 SP`
> Recurring charge engine: daily/weekly/monthly. Trial period, grace period,
> dunning (retry failed charges). Webhook notification per cycle.
>
> **Acceptance Criteria**:
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
> - [ ] Tenor options endpoint
> - [ ] Approval + disbursement flow
> - [ ] Installment schedule generation
>
> **Relevan untuk**: Dolan, Sinau

</details>

---

## 🟦 E-12 — Settlement & Financial Operations

> **Goal**: Settlement, reconciliation, pricing, dan multi-currency capabilities.

| Key     | Type  | Summary                            | Priority | SP | Component(s)       | Labels                      | Status   |
| :------ | :---- | :--------------------------------- | :------- | :-: | :----------------- | :-------------------------- | :------- |
| GAP-003 | Story | Settlement & Reconciliation        | 🟠 High  | 5  | `wallet-service`   | `partner` `finops`          | 📋 To Do |
| GAP-004 | Story | Rate Card / Pricing per Partner    | 🟠 High  | 3  | `partner-service`  | `partner` `billing`         | 📋 To Do |
| GAP-010 | Story | Multi-currency Settlement          | 🟠 High  | 5  | `fx-service`       | `partner` `fx`              | 📋 To Do |
| GAP-013 | Story | Revenue Share / Royalty Engine     | 🟠 High  | 3  | New module         | `partner` `finops`          | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**GAP-003 — Settlement & Reconciliation** `L` `5 SP`
> Payout ke merchant/instructor/author. Daily settlement cycle,
> reconciliation report, discrepancy flagging.
>
> **Acceptance Criteria**:
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
> - [ ] Rate card entity + CRUD
> - [ ] Fee calculation engine (flat, percentage, tiered)
> - [ ] Link partner → rate card
>
> **Relevan untuk**: Semua partner

**GAP-010 — Multi-currency Settlement** `L` `5 SP`
> FX-aware settlement. Convert & settle in partner's preferred currency.
>
> **Acceptance Criteria**:
> - [ ] Partner currency preference configuration
> - [ ] Auto-conversion at settlement time
> - [ ] FX rate locking for settlement window
>
> **Relevan untuk**: Dolan, Maca

**GAP-013 — Revenue Share / Royalty Engine** `M` `3 SP`
> Auto split revenue per sale. Configurable split ratio per product/partner.
>
> **Acceptance Criteria**:
> - [ ] Revenue split config (percentage per stakeholder)
> - [ ] Auto-split at settlement time
> - [ ] Royalty statement generation
>
> **Relevan untuk**: Sinau, Maca

</details>

---

## 🟦 E-13 — Dispute Resolution

> **Goal**: Refund dan dispute management untuk partner transactions.

| Key     | Type  | Summary                        | Priority | SP | Component(s)       | Labels                     | Status   |
| :------ | :---- | :----------------------------- | :------- | :-: | :----------------- | :------------------------- | :------- |
| GAP-009 | Story | Refund & Dispute Management    | 🟠 High  | 5  | `transaction-svc`  | `partner` `dispute`        | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**GAP-009 — Refund & Dispute Management** `L` `5 SP`
> Full + partial refund. Dispute lifecycle: open → investigate → resolve.
> Chargeback handling, evidence upload.
>
> **Acceptance Criteria**:
> - [ ] Full refund endpoint
> - [ ] Partial refund endpoint
> - [ ] Dispute lifecycle (OPEN → INVESTIGATING → RESOLVED/REJECTED)
> - [ ] Evidence attachment (file upload)
> - [ ] Webhook: refund.created, dispute.opened, dispute.resolved
>
> **Relevan untuk**: TokoBapak, Dolan, Sinau

</details>

---

## 🔧 Tech Debt

| Key      | Type      | Summary                                | Priority  | SP | Component(s)          | Status   |
| :------- | :-------- | :------------------------------------- | :-------- | :-: | :-------------------- | :------- |
| SIMP-001 | Tech Debt | Remove `ab-testing-service`            | 🟠 High   | 2  | `ab-testing-service`  | 📋 To Do |
| SIMP-002 | Tech Debt | Remove Gamification from promotion-svc | 🟡 Medium | 2  | `promotion-service`   | 📋 To Do |
| SIMP-003 | Tech Debt | Remove Robo-advisory from investment   | 🟡 Medium | 2  | `investment-service`  | 📋 To Do |

<details>
<summary>📄 Details</summary>

> **SIMP-001**: `ab-testing-service` broken, tidak relevan untuk payment gateway. Ganti feature flags via env var.
> **SIMP-002**: Hapus `GamificationService.java`, keep `LoyaltyPoints` + `CashbackService`.
> **SIMP-003**: Hapus robo-advisory, simplify ke portfolio view + mutual fund mock.

</details>

---

## 🔍 Spikes (Research / Architecture Decision)

| Key      | Type  | Question                                                        | Impact                            | Status   |
| :------- | :---- | :-------------------------------------------------------------- | :-------------------------------- | :------- |
| ARCH-001 | Spike | KYC di level PayU atau project client?                          | Scope `kyc-service`               | 📋 To Do |
| ARCH-002 | Spike | Statement: PDF end-user atau JSON/CSV project client?           | Output format `statement-service` | 📋 To Do |
| ARCH-003 | Spike | Support ticket: end-user PayU atau project client?              | Multi-tenancy `support-service`   | 📋 To Do |
| ARCH-004 | Spike | CMS: hanya PayU web-app atau multi-tenant project client?       | Multi-tenant mode `cms-service`   | 📋 To Do |

---

## 🔮 Deferred (Icebox)

| Key         | Type  | Summary                                 | Notes                            |
| :---------- | :---- | :-------------------------------------- | :------------------------------- |
| P2-FE-003   | Story | Mobile App Feature Parity (Expo/RN)     | ❄️ Deferred                      |
| OCP-007     | Story | Service Mesh mTLS enforcement           | ❄️ Planned                       |
| OCP-010     | Story | API versioning headers                  | ❄️ Planned                       |
| DR-001      | Story | Disaster Recovery live test execution   | ❄️ Scripts ready                  |

---

## 📊 Metrics

### Story Points by Epic

| Epic | Name                           | Stories | SP  |
| :--- | :----------------------------- | :-----: | :-: |
| E-01 | Core Banking Ledger            |    3    | 13  |
| E-02 | Gateway Hardening              |    5    | 13  |
| E-03 | Frontend Quality               |    5    |  7  |
| E-04 | API Management & Analytics     |    5    | 19  |
| E-05 | Product Catalog                |    1    |  5  |
| E-06 | Developer Hub (Backstage)      |    5    | 13  |
| E-07 | gRPC Inter-Service Comm.       |    8    | 25  |
| E-08 | Legacy Integration             |    1    |  5  |
| E-09 | Partner Foundation             |    4    | 18  |
| E-10 | Escrow & Marketplace           |    2    | 10  |
| E-11 | Subscription & Billing         |    2    |  8  |
| E-12 | Settlement & FinOps            |    4    | 16  |
| E-13 | Dispute Resolution             |    1    |  5  |
|      | **TOTAL**                      | **46**  |**157**|

### Story Points by Label

| Label        | Total SP |
| :----------- | :------: |
| `backend`    |    81    |
| `partner`    |    62    |
| `gateway`    |    22    |
| `platform`   |    21    |
| `frontend`   |     7    |
| `dx`         |    13    |

### FE Impact Summary

| Kategori                          | Count | Keys                                |
| :-------------------------------- | :---: | :---------------------------------- |
| ✅ No FE impact (backend only)    |  26   | IMP-002,003,005,007–009,012,013,016–033 |
| ⚠️ Extend FE (non-breaking)       |   3   | IMP-001, IMP-006, IMP-015          |
| 🔴 FE-only (independent)          |   4   | IMP-004, IMP-010, IMP-011, IMP-014 |

---

_Last Updated: February 26, 2026 | 13 Epics · 46 Stories · 157 SP · 3 Tech Debt · 4 Spikes · 4 Deferred_
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
