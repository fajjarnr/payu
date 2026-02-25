# 🐛 PayU — Bug Backlog & Open Items

> **Dokumen ini hanya berisi item yang BELUM selesai dan perlu tindakan.**
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Bug Summary

| Kategori                  | Open  | Skipped |  Fixed  | Original Total |
| :------------------------ | :---: | :-----: | :-----: | :------------: |
| Backend Logic             |   0   |    3    |   144   |    **147**     |
| Frontend Logic            |   0   |    0    |   46    |     **46**     |
| Frontend-Backend Mismatch |   0   |    0    |   29    |     **29**     |
| Auth / Session            |   0   |    0    |   10    |     **10**     |
| **TOTAL**                 | **0** |  **3**  | **229** |    **~232**    |

> ✅ **229 of ~232 bugs fixed** (~99%) dari code review mendalam (Feb 24-25, 2026).
> 0 open bugs. 3 intentionally skipped (low impact, future consideration).

---

## 🐛 Open Bugs (0 remaining)

> ✅ **All bugs resolved.** No open items. See Recently Fixed below for latest changes.

### ✅ Recently Fixed (Feb 25, 2026)

| ID                | Service                | Issue                                                           | Resolution                                                                                                                                                  |
| :---------------- | :--------------------- | :-------------------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **BUG-BE-026**    | `notification-service` | SMS sender adalah mock — selalu return success tanpa kirim OTP. | ✅ Fixed: refactored `SmsSender.java` with configurable provider mode (LOG/TWILIO/VONAGE/ZENZIVA). LOG mode shows full SMS content in console for lab use.  |
| **BUG-BE-037**    | `billing-service`      | Biller processing adalah mock — selalu set COMPLETED.           | ✅ Fixed: created `biller-simulator` (Quarkus 3.17.5) with 14 seeded test accounts + `BillerPort`/`BillerAdapter` hexagonal integration in billing-service. |
| **BUG-BE-051**    | `statement-service`    | `getBalanceAtDate()` return saldo saat ini, bukan historis.     | ✅ Fixed: compute historical balances from transactions.                                                                                                    |
| **BUG-CROSS-006** | FE ↔ BE                | Frontend tidak punya `BiometricService.ts`.                     | ✅ Verified: already cleaned up in Keycloak MFA refactor. Mobile biometrics are valid device-level.                                                         |
| **XBUG-004**      | FE ↔ BE                | Scheduled transfers & split bills path alignment.               | ✅ Fixed: corrected controller path, added BFF whitelist entries, aligned response types.                                                                   |
| **BUG-AUTH-007**  | `middleware.ts`        | Middleware izinkan akses hanya dengan `refreshToken`.           | ✅ Verified: correct by design — 401 interceptor handles silent refresh.                                                                                    |
| **BUG-AUTH-008**  | `useSilentRefresh.ts`  | Tidak ada unit test untuk hook kritis ini.                      | ✅ Fixed: added comprehensive vitest tests (9 test cases).                                                                                                  |

---

## ⏭️ Intentionally Skipped (3 items)

> Item ini di-triage dan di-skip karena impact rendah pada fase saat ini.

| ID             | Service              | Issue                                                                             | Alasan Skip                                                         |
| :------------- | :------------------- | :-------------------------------------------------------------------------------- | :------------------------------------------------------------------ |
| **BUG-BE-061** | `promotion-service`  | `getTransactionAmount()` selalu return `ZERO` — badge berbasis amount tidak work. | Gamification/badge opsional, tidak pengaruh core banking.           |
| **BUG-BE-076** | `api-portal-service` | Sandbox store in-memory — data hilang saat pod restart.                           | Partner belum ada, sandbox belum relevan.                           |
| **BUG-BE-080** | `lending-service`    | Pre-approval endpoints ada di frontend, tidak ada di backend.                     | Feature belum aktif di frontend.                                    |
| **BUG-BE-091** | `shared/api-commons` | Fixed-window rate limit mudah di-burst (118 req/2 detik).                         | Low-traffic fase awal masih aman. Optimize ke sliding window nanti. |

---

## 📋 Open Items (Non-Bug)

### 🔴 Gateway Gaps (Future Features — Belum Dibutuhkan)

> Detail lengkap di [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md).
> Semua GAP items adalah fitur integrasi untuk 5 project client (TokoBapak, Nobar, Dolan, Sinau, Maca).

| ID          | Item                                                               | Relevan Untuk                 | Priority |
| :---------- | :----------------------------------------------------------------- | :---------------------------- | :------- |
| **GAP-001** | Outbound webhook service (notify partner saat payment done)        | Semua partner                 | 🔴 P0    |
| **GAP-002** | Multi-tenancy / data isolation per partner                         | Semua partner                 | 🔴 P0    |
| **GAP-006** | Idempotency key support di semua payment endpoints                 | Semua partner                 | 🔴 P0    |
| **GAP-007** | Escrow / payment holding                                           | TokoBapak, Dolan              | 🔴 P0    |
| **GAP-008** | Subscription / recurring billing                                   | Nobar, Sinau, Maca            | 🔴 P0    |
| **GAP-003** | Settlement & reconciliation (payout ke merchant/instructor/author) | TokoBapak, Dolan, Sinau, Maca | 🟠 P1    |
| **GAP-004** | Rate card / pricing per partner                                    | Semua partner                 | 🟠 P1    |
| **GAP-009** | Refund & dispute management (full + partial)                       | TokoBapak, Dolan, Sinau       | 🟠 P1    |
| **GAP-005** | API key management (stable, non-expiring)                          | Semua partner                 | 🟠 P1    |
| **GAP-010** | Multi-currency settlement (FX-aware)                               | Dolan, Maca                   | 🟠 P1    |
| **GAP-011** | Split payment (multi-merchant dalam 1 checkout)                    | Dolan                         | 🟠 P1    |
| **GAP-012** | Installment / PayLater integration di gateway                      | Dolan, Sinau                  | 🟠 P1    |
| **GAP-013** | Revenue share / royalty engine (auto split per sale)               | Sinau, Maca                   | 🟠 P1    |

### 🟡 Simplification Candidates

| ID           | Item                                                               | Rekomendasi                                                                |
| :----------- | :----------------------------------------------------------------- | :------------------------------------------------------------------------- |
| **SIMP-001** | `ab-testing-service` — broken, tidak relevan untuk payment gateway | Hapus service, ganti feature flags via env var                             |
| **SIMP-002** | Gamification (XP/Badge/Level) di `promotion-service`               | Hapus `GamificationService.java`, keep `LoyaltyPoints` + `CashbackService` |
| **SIMP-003** | Robo-advisory di `investment-service`                              | Hapus, simplify ke portfolio view + mutual fund mock                       |

### ❓ Architecture Questions (Perlu Keputusan)

| ID           | Pertanyaan                                                        | Impact                           |
| :----------- | :---------------------------------------------------------------- | :------------------------------- |
| **ARCH-001** | KYC di level PayU atau project client?                            | Scope kyc-service                |
| **ARCH-002** | Statement: PDF untuk end-user atau JSON/CSV untuk project client? | Output format statement-service  |
| **ARCH-003** | Support ticket: end-user PayU atau project client yang integrasi? | Multi-tenancy di support-service |
| **ARCH-004** | CMS: hanya untuk PayU web-app atau multi-tenant project client?   | Multi-tenant mode di cms-service |

### � Improvement Backlog (Dari Diskusi Arsitektur — Feb 26, 2026)

> Hasil perbandingan PayU vs Temenos T24, webMethods, Oracle DB, dan JBoss EAP.
> Setiap item di-review terhadap web-app frontend untuk memastikan kompatibilitas.

#### 🔴 High Priority (Arsitektur Fundamental)

| ID          | Area              | Improvement                                                                                                                                                                                                                                                                                                                                                                           | Frontend Impact                                                                                                                                                                                                                                      | Effort |
| :---------- | :---------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :----- |
| **IMP-001** | `wallet-service`  | **True Double-Entry Ledger** — Saat ini `LedgerEntry` single-sided (1 DEBIT _atau_ 1 CREDIT per row, tanpa pairing). Implementasi `JournalEntry` parent yang enforce matching DEBIT+CREDIT pairs. Tambah trial balance verification.                                                                                                                                                  | ⚠️ **Perlu update FE**: `WalletService.ts` endpoint `/wallets/{id}/ledger` return `LedgerEntry[]` — response shape tetap sama, tapi perlu tambah `journalId` field. Tambah endpoint trial balance baru. FE **tidak break**, hanya perlu extend type. | Large  |
| **IMP-002** | `wallet-service`  | **Chart of Accounts (CoA)** — Tambah GL account classification (`ASSET:USER_WALLET`, `LIABILITY:ESCROW_HOLDING`, `REVENUE:TRANSACTION_FEE`). Enabler untuk settlement reconciliation TokoBapak & regulatory reporting OJK.                                                                                                                                                            | ✅ **No FE impact** — CoA murni backend/backoffice concern. Web-app consumer tidak perlu tahu GL structure. Backoffice dashboard bisa ditambah nanti.                                                                                                | Medium |
| **IMP-003** | `gateway-service` | **Wire Circuit Breaker + Retry** — `RetryAndTimeoutService` dan `@CircuitBreaker` sudah di-code tapi **tidak di-wire** ke `proxy()` method di `ApiGatewayResource`. Tinggal connect.                                                                                                                                                                                                  | ✅ **No FE impact** — BFF proxy di `route.ts` sudah handle 503 fallback (`X-Fallback: gateway-offline`). Circuit breaker di gateway transparan untuk frontend.                                                                                       | Small  |
| **IMP-004** | `web-app`         | **429 Rate Limit Handling** — Frontend **zero awareness** terhadap rate limiting. Tidak ada handling untuk HTTP 429. Jika gateway rate-limit, user lihat cryptic error. Tambah 429 interceptor di Axios + user-friendly "Too many requests" UI.                                                                                                                                       | 🔴 **FE-only change** — Tambah interceptor di `api.ts`, retry-after header parsing, dan toast/banner component.                                                                                                                                      | Small  |
| **IMP-005** | `gateway-service` | **Konsolidasi Rate Limiting** — Ada 3 implementasi terpisah: (1) `RateLimitAspect` di api-commons (fixed window, Redis), (2) `RateLimitFilter` di gateway (klaim sliding window tapi fixed window), (3) `RateLimitV2Filter` di gateway (in-memory token bucket, **tidak distributed** — rate limit 10x lipat jika 10 pod). Konsolidasi ke 1 implementasi Redis-backed sliding window. | ✅ **No FE impact** — rate limiting transparan. Hanya IMP-004 (429 handling) perlu di FE.                                                                                                                                                            | Medium |

#### 🟠 Medium Priority (Feature Enablers)

| ID          | Area              | Improvement                                                                                                                                                                                                                                  | Frontend Impact                                                                                                                                                                                                                                                                                                 | Effort   |
| :---------- | :---------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- | --------- | ---------------------------------------------- | -------- | --------------------------------------------- | ------------------------------------------- | ---- |
| **IMP-006** | Multi-service     | **Product Catalog (DB-driven)** — Semua parameter produk hardcoded di Java (`MINIMUM_SAVINGS_BALANCE = 10000`, `LoanType` enum, interest rates per instance). Buat entity `ProductDefinition` di DB supaya produk baru tidak perlu redeploy. | ⚠️ **Perlu extend FE**: Tambah API fetch product list untuk dropdown di account creation, loan application, investment. Saat ini `accountType`, `LoanType`, `InvestmentType` hardcoded sebagai string literal unions di FE — perlu jadi dynamic dari API. **Tidak break existing**, tapi perlu migrasi gradual. | Large    |
| **IMP-007** | `gateway-service` | **Dynamic Route Registry** — ~70 hardcoded JAX-RS endpoints di `ApiGatewayResource.java` (1 method per HTTP verb per service). Ganti dengan config-driven route table.                                                                       | ✅ **No FE impact** — BFF proxy kirim ke gateway, routing internal transparan.                                                                                                                                                                                                                                  | Medium   |
| **IMP-008** | `gateway-service` | **Request Validation (JSON Schema)** — `RequestValidationFilter` punya infrastructure tapi `getSchemaForPath()` return `null` (stub). Load actual OpenAPI schemas yang sudah ada di tiap service.                                            | ✅ **No FE impact** — validasi di gateway transparan. Request invalid akan return 400 yang FE sudah handle.                                                                                                                                                                                                     | Medium   |
| **IMP-009** | `gateway-service` | **Response Masking** — Gateway saat ini pure pass-through. Untuk partner API, perlu strip internal fields (trace IDs, internal error codes) dari response.                                                                                   | ✅ **No FE impact** — masking hanya untuk partner/external API. BFF proxy tetap dapat full response.                                                                                                                                                                                                            | Small    |
| **IMP-010** | `web-app`         | **FxService Double-Prefix Bug** — `FxService.ts` sets `baseUrl = '/api/v1/fx'` tapi Axios `baseURL` sudah `/api/v1`. Request jadi `/api/v1/api/v1/fx/rates/...` (404).                                                                       | 🔴 **FE-only fix** — Fix `baseUrl` di `FxService.ts` ke `/fx`.                                                                                                                                                                                                                                                  | Tiny     |
| **IMP-011** | `web-app`         | **Pocket Type Inconsistency** — `types/index.ts` defines `'MAIN'                                                                                                                                                                             | 'SAVING'                                                                                                                                                                                                                                                                                                        | 'SHARED' | 'SAVINGS' | 'GOAL'`tapi`WalletService.ts`defines`'SAVINGS' | 'SHARED' | 'GOAL'`. Konsolidasi ke satu source of truth. | 🔴 **FE-only fix** — Unify type definition. | Tiny |

#### 🟡 Low Priority (Future-Proofing)

| ID          | Area       | Improvement                                                                                                                                                                                                     | Frontend Impact                                                                                            | Effort |
| :---------- | :--------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------- | :----- |
| **IMP-012** | New module | **GL Engine Ringan** — Implement General Ledger untuk settlement reconciliation (neraca, laba-rugi). Bisa di `wallet-service` atau service baru `gl-service`. Enabler untuk daily settlement report TokoBapak.  | ✅ **No FE consumer impact** — GL purely backoffice/reporting. Mungkin tambah dashboard di backoffice web. | Large  |
| **IMP-013** | New module | **Apache Camel Integration Layer** — Jika nanti perlu integrasi ke legacy systems (SWIFT XML, format OJK CSV/XML, SOAP endpoints). Lebih ringan dari webMethods, Red Hat supported.                             | ✅ **No FE impact** — integration layer murni backend.                                                     | Large  |
| **IMP-014** | `web-app`  | **Duplicate Type Definitions** — `BalanceResponse`, `Transaction`, `WalletTransaction`, `Pocket` didefinisikan di `types/index.ts` DAN di masing-masing service file. Risiko drift. Konsolidasi ke satu source. | 🔴 **FE-only refactor** — tidak ubah behavior.                                                             | Small  |
| **IMP-015** | `web-app`  | **Financial Data in URL** — `LendingService.processRepayment()` kirim `amount` sebagai query param, `activatePayLater()` kirim `userId` sebagai query param. Pindah ke request body.                            | ⚠️ **FE + BE change** — perubahan contract endpoint, backward compatible jika BE accept both.              | Small  |

#### 🔵 API Management Evolution (Dari Diskusi 3scale — Feb 26, 2026)

> Hasil perbandingan PayU Gateway vs Red Hat 3scale API Management.
> Strategi: improve gateway internal dulu, adopsi 3scale saat partner ecosystem tumbuh.

##### Quick Wins (Tutup ~60% gap 3scale tanpa infra baru)

| ID          | Area              | Improvement                                                                                                                                                                                                                                                                | Frontend Impact                                                                                                | Effort |
| :---------- | :---------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------- | :----- |
| **IMP-016** | `gateway-service` | **Persistent API Analytics** — `ApiAnalyticsService` saat ini in-memory (data hilang saat pod restart). Pindah ke Redis/TimescaleDB untuk usage tracking per-partner, per-endpoint, per-method. Foundation untuk usage dashboard & billing.                                | ✅ **No FE impact** — analytics backend-only. Backoffice dashboard bisa ditambah nanti.                        | Medium |
| **IMP-017** | `gateway-service` | **Rate Plan per Partner** — Saat ini rate limit global. Implementasi config-driven rate plan per partner (TokoBapak: 1000 req/min, Nobar: 500 req/min). Prerequisite: IMP-005 (konsolidasi rate limiting).                                                                 | ✅ **No FE impact** — rate plan untuk partner API, bukan consumer web-app.                                     | Medium |
| **IMP-018** | `gateway-service` | **Request/Response Transformation** — Gateway saat ini pure pass-through. Tambah lightweight transformation layer: header injection, field masking untuk partner response, request enrichment (inject user context ke body). Tidak perlu se-kompleks 3scale policy engine. | ✅ **No FE impact** — BFF proxy tetap dapat full response. Transformasi hanya untuk partner external API path. | Medium |

##### 3scale Migration Path (Future — Saat 5+ Partner)

| ID          | Area     | Recommendation                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Trigger                                           | Effort |
| :---------- | :------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------ | :----- |
| **IMP-019** | Platform | **Adopt Red Hat 3scale** — Saat partner ecosystem tumbuh (5+ partner), manual management rate plans, API keys, usage billing, developer portal menjadi unsustainable. 3scale menyediakan semua ini out-of-box. **Strategi migrasi**: 3scale APIcast di depan PayU gateway (2-tier), bukan replace gateway. PayU gateway tetap handle banking-specific logic (idempotency, HMAC signing, escrow-aware routing). 3scale handle API product management (plans, monetization, developer portal, analytics dashboard). | ≥5 partner aktif, atau kebutuhan API monetization | Large  |
| **IMP-020** | Platform | **Alternative: Kong/Gravitee** — Jika 3scale license tidak feasible, pertimbangkan Kong Gateway (open-source) atau Gravitee.io sebagai API management layer. Sama pattern: 2-tier di depan PayU gateway.                                                                                                                                                                                                                                                                                                          | Budget constraint + ≥5 partner                    | Large  |

> **Arsitektur Target (2-Tier Gateway):**
>
> ```
> Partner (TokoBapak/Nobar/Future)
>        │
>        ▼
> ┌─────────────────────────────┐
> │  3scale / Kong / Gravitee   │  ← API Management Layer
> │  - Developer Portal          │     (plans, keys, monetization,
> │  - Usage Analytics           │      analytics dashboard)
> │  - Rate Plans per Partner    │
> │  - API Lifecycle Management  │
> └──────────┬──────────────────┘
>            ▼
> ┌─────────────────────────────┐
> │  PayU Gateway (Quarkus)     │  ← Banking Logic Layer
> │  - Idempotency Filter        │     (tetap dipertahankan karena
> │  - HMAC Request Signing      │      banking-specific, tidak ada
> │  - JWT Keycloak Validation   │      di 3scale/Kong)
> │  - Tenant Isolation          │
> │  - Circuit Breaker + Retry   │
> └──────────┬──────────────────┘
>            ▼
>     Backend Services
> ```
>
> **Keputusan**: PayU gateway **tidak di-replace** oleh 3scale. 3scale menjadi layer di atasnya
> untuk API product management. Banking-specific filters (idempotency, HMAC signing, escrow routing)
> tetap di PayU gateway karena **tidak tersedia di 3scale/Kong/Gravitee**.

##### Developer Hub & Service Catalog (Dari Diskusi Red Hat Developer Hub — Feb 26, 2026)

> Strategi hybrid: Red Hat Developer Hub (Backstage) untuk internal engineer,
> developer-docs + api-portal-service untuk external partner.

| ID          | Area                 | Improvement                                                                                                                                                                                                                                                                                                                                                                                                  | Frontend Impact                                             | Effort |
| :---------- | :------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :---------------------------------------------------------- | :----- |
| **IMP-021** | Platform             | **Adopt Red Hat Developer Hub (Backstage)** — Deploy sebagai Internal Developer Portal di OpenShift. Provides: service catalog (auto-discovery 22 services), CI/CD visibility (Tekton+ArgoCD), dependency graph, team ownership, software templates. **OSS equivalent**: Backstage.io (CNCF). **Red Hat product**: Red Hat Developer Hub (included in OpenShift Platform Plus). Keycloak SSO pre-integrated. | ✅ **No FE impact** — internal tool untuk engineer.         | Medium |
| **IMP-022** | All services         | **Backstage Service Catalog (`catalog-info.yaml`)** — Buat `catalog-info.yaml` per service (22 files) sebagai machine-readable service registry. Mendefinisikan: owner, lifecycle, dependencies, API spec location. Replace manual `SERVICES_STATUS.md`.                                                                                                                                                     | ✅ **No FE impact** — metadata file saja.                   | Small  |
| **IMP-023** | `api-portal-service` | **OpenAPI Coverage: 16% → 80%+** — Hanya 24/154 endpoint punya OpenAPI annotation. Tambah `@Operation`, `@ApiResponse`, `@Schema` di seluruh controller. Prerequisite supaya Backstage API tab & api-portal aggregation berguna.                                                                                                                                                                             | ✅ **No FE impact** — annotation saja, tidak ubah behavior. | Medium |
| **IMP-024** | Platform             | **Backstage Software Templates** — Buat template scaffolding "New PayU Microservice" di Backstage. Auto-generate: repo structure (hexagonal), Containerfile, Helm chart, CI pipeline, `catalog-info.yaml`. Accelerate onboarding new service.                                                                                                                                                                | ✅ **No FE impact** — scaffolding tool.                     | Medium |
| **IMP-025** | Platform             | **Backstage TechDocs Integration** — Connect existing `docs/` folder ke Backstage TechDocs plugin. Render markdown per-service documentation langsung di portal. Existing `developer-docs` Next.js tetap untuk external partner.                                                                                                                                                                             | ✅ **No FE impact** — internal docs rendering.              | Small  |

##### gRPC Inter-Service Communication (Dari Diskusi Arsitektur — Feb 26, 2026)

> Strategi: **gRPC untuk internal service-to-service**, **REST tetap untuk gateway→frontend/partner**.
> Saat ini semua inter-service call pakai REST (RestTemplate/Feign) dengan JSON — overhead signifikan
> untuk hot path seperti `wallet-service` yang dipanggil 6 service.
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
> **Alasan REST tetap di gateway→frontend:**
> - Browser tidak support gRPC native (gRPC-Web menambah complexity)
> - Partner integration (SNAP-BI) wajib REST
> - OpenAPI docs untuk developer portal
> - Human-readable untuk debugging
>
> **Alasan gRPC di internal:**
> - Protobuf binary ~5-10x lebih compact dari JSON
> - ~2-5x latency improvement
> - Compile-time contract (`.proto`) = breaking change ketahuan saat build
> - HTTP/2 multiplexing = 1 connection handle semua call
> - Istio native gRPC-aware load balancing
> - Spring Boot 3.4 `spring-grpc` (GA), Quarkus built-in gRPC support

| ID          | Area                   | Improvement                                                                                                                                                                                                                                                                                                                                                                  | Frontend Impact                                                                                     | Effort |
| :---------- | :--------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------------------------------- | :----- |
| **IMP-026** | `shared/grpc-starter`  | **Shared gRPC Starter Library** — Buat `backend/shared/grpc-starter` sebagai foundation: common protobuf types (`Money`, `Timestamp`, `PageRequest`, `ErrorDetail`), gRPC interceptors (tracing, auth propagation, error mapping), Spring Boot auto-configuration. Semua service depend on ini, bukan setup gRPC sendiri-sendiri.                                             | ✅ **No FE impact** — shared backend library.                                                       | Medium |
| **IMP-027** | `wallet-service`       | **Wallet gRPC Server** — Expose `WalletService.proto` (getBalance, debit, credit, transfer, getHistory). `wallet-service` paling banyak dipanggil (6 callers) — impact terbesar. Jalankan gRPC server di port terpisah (9090) alongside existing REST. REST endpoints tetap hidup untuk backward compatibility selama migrasi.                                                | ✅ **No FE impact** — FE tetap lewat REST gateway. gRPC hanya internal.                             | Medium |
| **IMP-028** | Multi-service          | **Migrate Wallet Callers ke gRPC Client** — Update 6 service yang call wallet: `transaction-service`, `billing-service`, `investment-service`, `fx-service`, `promotion-service`, `statement-service`. Replace `RestTemplate`/`WalletClient` adapters dengan gRPC stubs. Gunakan hexagonal port interface — hanya ganti adapter, domain logic tidak berubah.                  | ✅ **No FE impact** — internal transport change.                                                    | Large  |
| **IMP-029** | `account-service`      | **Account gRPC Server** — Expose `AccountService.proto` (getAccount, verifyAccount, getAccountsByUser). Callers: `transaction-service` (RestTemplate), `lending-service` (Feign). Migrate kedua caller ke gRPC client.                                                                                                                                                       | ✅ **No FE impact** — internal transport change.                                                    | Medium |
| **IMP-030** | `transaction-service`  | **Transaction gRPC Server** — Expose `TransactionService.proto` (getTransaction, getHistory, getByReference). Callers: `lending-service` (Feign), `statement-service` (RestTemplate). Migrate kedua caller ke gRPC client.                                                                                                                                                   | ✅ **No FE impact** — internal transport change.                                                    | Medium |
| **IMP-031** | `wallet` ↔ `fx`        | **Break Circular Dependency via gRPC** — Saat ini `wallet-service` ↔ `fx-service` saling panggil (circular). Refactor: extract `FxRateProvider` interface, `fx-service` push rate updates via Kafka event, `wallet-service` consume dan cache lokal. Eliminasi synchronous circular call. gRPC hanya 1 arah: `fx-service` → `wallet-service` untuk balance ops.               | ✅ **No FE impact** — internal architecture fix.                                                    | Medium |
| **IMP-032** | Multi-service          | **Standardize HTTP Client for External Calls** — Setelah internal call migrasi ke gRPC, konsolidasi remaining REST client usage: hapus Feign dependency yang unused, standardize ke `RestClient` (Spring 6.1+) untuk external calls saja (simulators, Keycloak, external billers). Buat `rest-client-starter` di shared/ dengan retry + circuit breaker baked in.              | ✅ **No FE impact** — internal client library cleanup.                                              | Small  |
| **IMP-033** | `gateway-service`      | **Gateway gRPC→REST Bridge** — Gateway (Quarkus) terima REST dari frontend/partner, lalu forward ke backend services via gRPC instead of REST. Gunakan `quarkus-grpc` client. Gateway jadi translation layer: REST↔gRPC + Protobuf↔JSON. Prerequisite: IMP-027, 029, 030 (target services punya gRPC server).                                                                | ✅ **No FE impact** — gateway internal routing change. FE tetap kirim REST ke gateway.              | Medium |

> **Execution Order (recommended):**
> 1. **IMP-026** (grpc-starter) → foundation
> 2. **IMP-027** (wallet gRPC server) → highest impact service
> 3. **IMP-028** (migrate 6 wallet callers) → realize gRPC benefit
> 4. **IMP-031** (break wallet↔fx circular) → architecture fix
> 5. **IMP-029** + **IMP-030** (account + transaction gRPC) → parallel
> 6. **IMP-033** (gateway bridge) → complete the picture
> 7. **IMP-032** (cleanup REST clients) → final cleanup
>
> **Current State (Audit Feb 26, 2026):**
> - **RestTemplate**: 8 services (transaction, billing, investment, fx, promotion, statement, wallet, auth)
> - **OpenFeign**: 2 aktif (account, lending); 2 declared tapi unused (transaction, fx)
> - **WebClient**: auth-service saja (Keycloak)
> - **gRPC**: 0 (tidak ada `.proto` file atau dependency)
> - **Circular dep**: wallet-service ↔ fx-service
> - **Hottest service**: wallet-service (6 synchronous callers)

#### Ringkasan Impact ke Web-App

| Kategori                          | Count | Detail                                                                                                          |
| :-------------------------------- | :---- | :-------------------------------------------------------------------------------------------------------------- |
| ✅ No FE impact (backend only)    | 26    | IMP-002, 003, 005, 007, 008, 009, 012, 013, 016, 017, 018, 019, 020, 021, 022, 023, 024, 025, 026–033          |
| ⚠️ Perlu extend FE (non-breaking) | 3     | IMP-001, 006, 015                                                                                               |
| 🔴 FE-only fix (independent)      | 4     | IMP-004, 010, 011, 014                                                                                          |

> **Kesimpulan**: Mayoritas improvement (26/33) **tidak menyentuh frontend sama sekali**. gRPC migration (IMP-026–033) sepenuhnya backend — frontend tetap REST via gateway. 4 item adalah fix independen di FE. Hanya 3 item yang butuh koordinasi FE+BE, dan semuanya **non-breaking** (extend, bukan replace).

### 🔮 Deferred

| ID            | Description                                   | Status                           |
| :------------ | :-------------------------------------------- | :------------------------------- |
| **P2-FE-003** | Mobile App Feature Parity (Expo/React Native) | Deferred                         |
| **OCP-007**   | Service Mesh mTLS enforcement                 | Planned                          |
| **OCP-010**   | API versioning headers                        | Planned                          |
| **DR-001**    | Disaster Recovery live test execution         | Scripts ready, pending execution |

---

_Last Updated: February 26, 2026 | 33 improvement items | 13 GAPs | Partners: TokoBapak, Nobar, Dolan, Sinau, Maca | Added: gRPC Inter-Service Communication (IMP-026–033)_
