# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| Status          | Count | Breakdown                                              |
| :-------------- | :---: | :----------------------------------------------------- |
| **Active Epics** |   4   | E-04 (partial), E-06 (partial), E-07 (partial), E-24  |
| **Open Stories** |  10   | IMP-019,020,021,028,032,033,070,071,072,073            |
| **Tech Debt**   |   3   | SIMP-001 – SIMP-003                                    |
| **Spikes**      |   4   | ARCH-001 – ARCH-004                                    |
| **Deferred**    |   5   | P2-FE-003, OCP-007, OCP-010, DR-001, Card Token/3DS   |
| **Bugs**        | 0/232 | 229 fixed, 4 Won't Do (BUG-BE-061, 076, 080, 091)     |

> **Completed Epics**: 20 fully done + 3 partially done (completed stories moved to CHANGELOG.md).
> See [`PROGRESS.md`](./PROGRESS.md) for completed Epics summary.

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

## 🗂️ Active Epics Overview

| Epic | Name                              | Priority   | Open Stories | Open SP | Quarter | Status    |
| :--- | :-------------------------------- | :--------- | :----------: | :-----: | :------ | :-------- |
| E-04 | API Management & Analytics        | 🟠 High    |      2       |   10    | Q2 2026 | 🔶 Partial |
| E-06 | Developer Hub (Backstage)         | 🟡 Medium  |      1       |    3    | Q2 2026 | 🔶 Partial |
| E-07 | gRPC Inter-Service Communication  | 🟡 Medium  |      3       |   10    | Q2 2026 | 🔶 Partial |
| E-24 | E2E Test & Gateway Test Readiness | 🔴 Highest |      4       |    8    | Q1 2026 | 📋 To Do  |

> **Story Points**: XS=1, S=2, M=3, L=5, XL=8
> **Labels**: `backend`, `frontend`, `gateway`, `platform`, `partner`, `security`, `grpc`, `dx`, `mobile`

---

## 🟦 E-04 — API Management & Analytics (Partial)

> **Goal**: Evolve gateway dari basic proxy ke API management-capable platform.
> Quick wins done, 3scale/Kong adoption nanti saat 5+ partner.
>
> **Completed**: IMP-016 (Persistent Analytics), IMP-017 (Rate Plan per Partner), IMP-018 (Request/Response Transformation) — see CHANGELOG.md

| Key     | Type  | Summary                    | Priority | SP  | Component(s) | Labels               | Status   |
| :------ | :---- | :------------------------- | :------- | :-: | :----------- | :------------------- | :------- |
| IMP-019 | Story | Adopt Red Hat 3scale       | ⚪ Low   |  5  | Platform     | `platform` `partner` | 📋 To Do |
| IMP-020 | Story | Alternative: Kong/Gravitee | ⚪ Low   |  5  | Platform     | `platform` `partner` | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

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

## 🟦 E-06 — Developer Hub (Backstage) (Partial)

> **Goal**: Deploy Red Hat Developer Hub (Backstage) sebagai Internal Developer Portal.
> Strategi hybrid: Backstage untuk internal, developer-docs untuk external partner.
>
> **Completed**: IMP-022 (Service Catalog), IMP-023 (OpenAPI Coverage), IMP-024 (Software Templates), IMP-025 (TechDocs Integration) — see CHANGELOG.md

| Key     | Type  | Summary              | Priority  | SP  | Component(s) | Labels          | Status   |
| :------ | :---- | :------------------- | :-------- | :-: | :----------- | :-------------- | :------- |
| IMP-021 | Story | Deploy Developer Hub | 🟡 Medium |  3  | Platform     | `platform` `dx` | 📋 To Do |

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

</details>

---

## 🟦 E-07 — gRPC Inter-Service Communication (Partial)

> **Goal**: Migrasi inter-service communication dari REST/JSON ke gRPC/Protobuf.
> REST tetap untuk gateway→frontend/partner.
>
> **Completed**: IMP-026 (gRPC Starter), IMP-027 (Wallet gRPC Server), IMP-029 (Account gRPC Server), IMP-030 (Transaction gRPC Server), IMP-031 (Break wallet↔fx Circular Dep) — see CHANGELOG.md

| Key     | Type  | Summary                        | Priority  | SP  | Component(s)      | Labels              | Blocked By    | Status   |
| :------ | :---- | :----------------------------- | :-------- | :-: | :---------------- | :------------------ | :------------ | :------- |
| IMP-028 | Story | Migrate Wallet Callers to gRPC | 🟠 High   |  5  | Multi-service (6) | `backend` `grpc`    | IMP-027       | 📋 To Do |
| IMP-032 | Task  | Standardize REST Client Cleanup | 🟡 Medium |  2  | Multi-service    | `backend` `cleanup` | IMP-028       | 📋 To Do |
| IMP-033 | Story | Gateway gRPC→REST Bridge       | 🟡 Medium |  3  | `gateway-service` | `backend` `grpc`    | IMP-027,29,30 | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

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

> **Execution Order** (remaining):
> `IMP-028` → `IMP-033` → `IMP-032`

---

## 🟦 E-24 — E2E Test Infrastructure & Gateway Test Readiness

> **Goal**: Resolve gateway-level blockers that prevent E2E blackbox tests from passing.
> Currently 115/169 tests skip due to rate limiting and auth misconfiguration.
> These are NOT test bugs — they are real gateway configuration issues that also
> affect partner integrations.
>
> **Source**: E2E blackbox test analysis (Mar 2, 2026)
>
> **Impact**: Without these fixes, no automated regression testing is possible
> for any auth-dependent flow (registration, login, wallet, transfer, etc.).

| Key     | Type  | Summary                                | Priority   | SP  | Component(s)         | Labels               | Status   |
| :------ | :---- | :------------------------------------- | :--------- | :-: | :------------------- | :------------------- | :------- |
| IMP-070 | Bug   | Gateway Rate Limiter Blocks E2E Tests  | 🔴 Highest |  2  | `gateway-service`    | `gateway` `quality`  | 📋 To Do |
| IMP-071 | Bug   | Registration Endpoint Requires JWT     | 🔴 Highest |  2  | `gateway-service`    | `gateway` `security` | 📋 To Do |
| IMP-072 | Bug   | Backoffice IP Whitelist Blocks E2E     | 🟠 High    |  1  | `backoffice-service` | `backend` `quality`  | 📋 To Do |
| IMP-073 | Story | E2E Shared User Fixture                | 🟡 Medium  |  3  | `tests/e2e_blackbox` | `quality` `test`     | 📋 To Do |

<details>
<summary>📄 Story Details</summary>

**IMP-070 — Gateway Rate Limiter Blocks E2E Tests** `S` `2 SP` 🔴

> When running the full E2E blackbox test suite (21 test files), each file's fixture
> calls `POST /api/v1/accounts/register` to create a test user. After ~3 requests,
> the gateway returns `429 RATE_LIMIT_EXCEEDED` with `{"category":"accounts","retryAfter":60}`.
> This causes **~111 tests to skip** — the single biggest blocker for automated testing.
>
> **Root Cause**: The `accounts` rate limit category has a low per-minute threshold.
> E2E tests hit this within seconds because all test files run concurrently.
>
> **Options** (pick one or combine):
> - (a) Add test-mode config that raises limits when `payu.test-mode=true`
> - (b) Whitelist `X-E2E-Test` header from rate limiting in `RateLimitFilter`
> - (c) Increase rate limit for `/accounts/register` category
> - (d) Add IP-based whitelist for localhost/CI in rate limiter
>
> **Acceptance Criteria**:
>
> - [ ] E2E test suite can register 21+ users without hitting 429
> - [ ] Production rate limits unchanged for real traffic
> - [ ] Configuration documented in `.env.example`
>
> **Relevan untuk**: All E2E tests, CI/CD pipeline
>
> ✅ No FE impact

**IMP-071 — Registration Endpoint Requires JWT Through Gateway** `S` `2 SP` 🔴

> When the rate limiter does NOT block (e.g., running a single test file),
> `POST /api/v1/accounts/register` returns `401 Unauthorized` through the gateway.
> The registration endpoint should be public (no JWT required) — users cannot
> authenticate before they have an account.
>
> **Root Cause**: Gateway's JWT validation filter does not whitelist the registration
> endpoint. The `JwtValidationFilter` or security config in `gateway-service` enforces
> auth on all `/api/v1/*` paths without excluding `/api/v1/accounts/register`.
>
> **Acceptance Criteria**:
>
> - [ ] `POST /api/v1/accounts/register` accessible without JWT through gateway
> - [ ] `POST /api/v1/auth/login` accessible without JWT through gateway
> - [ ] Other `/api/v1/*` endpoints still require JWT
> - [ ] Security audit: ensure only safe endpoints are whitelisted
>
> **Relevan untuk**: All E2E tests, partner integration, consumer app
>
> ✅ No FE impact — gateway config only

**IMP-072 — Backoffice IP Whitelist Blocks E2E Tests** `XS` `1 SP`

> `test_backoffice.py` E2E test gets `{"error":"IP_NOT_ALLOWED"}` when calling
> backoffice endpoints. The IP whitelist in `backoffice-service` only allows
> specific IPs (likely internal network) but E2E tests run from localhost.
>
> **Acceptance Criteria**:
>
> - [ ] Backoffice IP whitelist includes `127.0.0.1` and `172.x.x.x` (container network)
> - [ ] Or: add test-mode config to bypass IP check
> - [ ] Production whitelist unchanged
>
> **Relevan untuk**: Backoffice E2E tests
>
> ✅ No FE impact

**IMP-073 — E2E Test Suite Shared User Fixture** `M` `3 SP`

> Instead of each of the 21 test files independently registering a user (hitting
> the rate limiter 21 times), implement a session-scoped shared fixture in `conftest.py`
> that registers ONE user and shares the auth token across all test files.
>
> **Acceptance Criteria**:
>
> - [ ] Session-scoped `registered_user` fixture in `conftest.py`
> - [ ] Auth token shared across all test files
> - [ ] Individual test files can still run independently
> - [ ] Test isolation maintained (read-only tests share user, write tests create own)
>
> **Blocked by**: IMP-070, IMP-071 (registration must work first)
>
> **Relevan untuk**: E2E test performance, CI/CD pipeline
>
> ✅ No FE impact — test infrastructure only

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

## 📊 Metrics (Open Items Only)

### Open Story Points by Epic

| Epic | Name                              | Open Stories | Open SP |
| :--- | :-------------------------------- | :----------: | :-----: |
| E-04 | API Management & Analytics        |      2       |   10    |
| E-06 | Developer Hub (Backstage)         |      1       |    3    |
| E-07 | gRPC Inter-Service Comm.          |      3       |   10    |
| E-24 | E2E Test & Gateway Readiness      |      4       |    8    |
|      | **TOTAL (Epics)**                 |   **10**     | **31**  |
|      | Tech Debt (SIMP-001–003)          |      3       |    6    |
|      | **GRAND TOTAL (Open)**            |   **13**     | **37**  |

### Completed Summary

| Metric            | Value                                        |
| :---------------- | :------------------------------------------- |
| Completed Epics   | 20 fully done (see PROGRESS.md)              |
| Completed Stories | 76/86 (IMP + GAP)                            |
| Completed SP      | 228/265                                      |
| Completion Rate   | ~86% stories, ~86% SP                        |
| Bugs Fixed        | 229/232 (~99%)                               |

---

_Last Updated: March 2, 2026 | 4 Active Epics · 10 Open Stories · 37 Open SP · 3 Tech Debt · 4 Spikes · 5 Deferred_
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
_Referensi: BCA Digital (blu), Xendit, Midtrans, GoPay, OVO, DANA, Flip, Jago_
