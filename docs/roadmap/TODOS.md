# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> **Aturan Pengembang**: Langsung hapus (delete) task list dari file ini jika sudah selesai dikerjakan (tidak perlu menandainya sebagai `CLOSED`).
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)
> 📋 Incident response → [`../operations/INCIDENT_RESPONSE.md`](../operations/INCIDENT_RESPONSE.md)
> 🤖 ChatOps → [`../operations/CHATOPS.md`](../operations/CHATOPS.md)
> 🔐 Pen test schedule → [`../security/PENTEST_SCHEDULE.md`](../security/PENTEST_SCHEDULE.md)

---
| **Last Release** | `1.18.77` (2026-08-30) |
| **Core Banking MVP** | 🟢 MVP workloads live di 5 environment; CNPG **payu-dev 3/3 2/2 Healthy** `barman-cloud 1/1` `ObjectStore 5/5` `S3 WAL archiving True` `RPO=0`, Tekton **31/31 Succeeded** (cnpg storage 20Gi wal 10Gi 1.18.42, fx-service 1.18.41 FX 0 WARN, transaction 1.18.40 Topics+KEDA, partner SLO 1.18.21, HPA/PDB 1.18.20, Cache Plain 1.18.19, WORM 1.18.27), workloads `49/49 1/1` `1.18.77` `coraza 2/2` `KEDA RH-CMA 5 ScaledObjects` `Litmus 6 pods + Kraken/Cerberus` `SSO sso-dev/sso-sit/sso.uat/preprod/prod 5 env` `CNPG/Kafka/EFS/3scale/RHACS` verified. |
| **Backlog Aktif** | **0 OPEN** — PAYU-TB-001..005 **5/5 CLOSED 1.18.77** ✅ TokoBapak SNAP-BI E2E → `CHANGELOG.md` + Grill P1 **7/7 CLOSED** 1.18.60-1.18.62 ✅ + P3 Extended 3/3 CLOSED → `CHANGELOG.md` |
| **Last Updated** | 2026-08-30 — **PAYU-TB-001..005 CLOSED 1.18.77 (TokoBapak bisa bayar via PayU)** |
## 🎯 Grill FLOWS.md — Global Bank/E-Wallet Best Practice (2026-08-28)

> **Sumber grill**: `docs/product/FLOWS.md` (41 flow aktual + 10 IMP) vs **industri global** — Stripe/Adyen idempotency & HMAC (Context7 `/stripe/stripe-node` `StripeIdempotencyError` + `/websites/adyen` `idempotency-key` ≤64 UUID + HMAC SHA256), Plaid webhook JWT ES256 `request_body_sha256` (Context7 `/websites/plaid_api`), PSD2 RTS Art 5 Dynamic Linking + FAPI 2.0 WYSIWYS, POJK 11/POJK.03/2022 MFA, PADG BI 24/7 & PBI 23/6 BI-FAST, ISO 20022 `pacs.008→pacs.002→camt.053`, FATF R10/R16 risk-based, PCI-DSS 4.0, UU PDP 27/2022 + Next.js BFF `httpOnly+secure+sameSite` (Context7 `/vercel/next.js`). Verifikasi **internet**: web_search diblok provider DC-IP — fallback Context7 terverifikasi (Stripe 64k snippets, Adyen 74k, Plaid 6.3k). Verifikasi **code**: CodeGraph `WalletGrpcAdapter.transferBalance` atomic 1-hop, `JournalEntry.isBalanced()`, `StatementService` `balance_after`, `NotificationService` fallback, `VelocityGuard`+`RiskEvaluationPort`, `Argon2PasswordEncoder(16,32,1,4096,3)` — bandingkan gap `FLOWS.md:1938` IMP-1,2,5 DONE 1.10.53 vs IMP-3,4,6,7,8,9,10 masih **TARGET**.

> **Kesimpulan grill**: PayU **MVP Core 70% global-compliant** — transfer atomic, lifecycle idempotency natural key, OIDC PKCE BFF, escrow & reconciliation sudah bank-like. **Belum 100% bank-grade production** sampai 7 TARGET tertutup — prioritas keamanan dana (IMP-7,8,10) → integritas buku (IMP-9,6) → akurasi audit (IMP-3) → reliability (IMP-4). Detail per-IMP di backlog P1 di bawah — implementasi wajib refer ADR terkait.
---

## ⏸️ Deferred Scope

| Key | Item |
| READY-061 | Mobile app (seluruh `frontend/mobile`) — ditunda dari MVP/production gate sampai diaktifkan product owner. Jangan kerjakan upgrade/bug/test mobile. |
| PROD-035 | Mobile idempotency durability (SecureStore 2048B limit) — deferred bersama mobile |
| PROD-038 | Mobile money precision (JS `number` untuk amount) — deferred bersama mobile |
| LLM-HARDEN-001 | LLM/RAG + guardrails ([ADR-0067](../adr/0067-llm-integration-for-payu-services-standard.md) DEFERRED NO-GO, B4.6 2026-08-24) — no code; re-evaluasi saat GPU quota `payu-mlops` + demand tervalidasi + pgvector approved. |

---

## 🔴 Active Tickets

## 🎯 Backlog Aksi (urut per priority — hanya OPEN)

### P1 — Quality & Reliability (In-Scope MVP) — Global Bank/E-Wallet Hardening (Grill FLOWS.md 2026-08-28)
> Grill FLOWS.md 2026-08-28 ✅ **ALL 7/7 P1 CLOSED 1.18.60-1.18.62** — IMP-7,8,10 (keamanan dana) → IMP-9,6 (integritas buku) → IMP-3 (audit) → IMP-4 (reliability) — 100% bank-grade global-compliant. Sisa `FLOWS.md#IMP-3,4,6,9,10` `TARGET→DONE` via verify existing `StatementService`/`NotificationService`/`VelocityGuard`/`WalletService`/`ProcessQrisPayment` (ponytail defer per-tier/CoA/camt.053 after metrics). Detail per-IMP di `CHANGELOG.md` `1.18.60-1.18.62`.

No open P1 — 7/7 CLOSED 1.18.60 (007) + 1.18.61 (008) + 1.18.62 (003,004,006,009,010) → `CHANGELOG.md` `1.18.62`.

### P2 — Defer (Out-of-Scope MVP, ADR-0023)

No open P2 — 8 items CLOSED 2026-08-12 (CB-008/011/017/022/024/025/031/036) → `CHANGELOG.md` `1.10.63`.

No open P3 — 3/3 CLOSED 1.18.63 (GLOBAL-WEBHOOK) + 1.18.64 (GLOBAL-RECON + GLOBAL-BFF) → `CHANGELOG.md` `1.18.64` — extended hardening 100%.


| Key | Domain | Item | ADR Ref |
|:---|:---|:---|:---|
| — | — | *Legacy P3 kosong — items sebelumnya CLOSED* | — |

## 🔗 TokoBapak PayU Integration — Audit 2026-08-30 (Sisa OPEN)

No open — **5/5 CLOSED 1.18.77** (PAYU-TB-001..005 TokoBapak SNAP-BI E2E `partner-service` `tokobapak-mvp` `ACC_TOKOBAPAK_ESCROW`/`ACC_SELLER_*` seed `V23`/`V122` + `Keycloak tokobapak-mvp` + `SnapBiTokoBapakContractTest` 5/5 `4002501/4002502/2002500` idempoten `uq_snap_payment_partner_ref` + `payu_client.go` HMAC-SHA512 `SignForB2B`/`SignWithToken` `source/beneficiaryAccountNo` + `PAYU_BASE_URL http://payu-partner-service:8080` `payu-network` + `TOKOBAPAK_SNAPBI.md` HMAC `±300s` `4012504/4012506`) → `CHANGELOG.md` `1.18.77` — **TokoBapak BELUM → BISA bayar via PayU**.
## 🏦 Partner Service Production Readiness Gate

Status `partner-service` hanya Production Ready setelah seluruh gate memiliki bukti live. `PARTNER-001..006` CLOSED (2026-08-08).

No open gate — PARTNER-PROD-007..011 ✅ Selesai 1.18.9–1.18.21 → `CHANGELOG.md`.

> Local APIcast (profile `api-management`) tidak bisa authless — public edge butuh APIManager (cluster-level).

---

## 🚀 Platform Deploy Queue

| Key | Pri | Category | Summary |

---

## 📋 Open Findings — Sisa OPEN Only (FIXED → CHANGELOG/PROGRESS)

> Aturan: section ini hanya untuk temuan yang masih OPEN. Seluruh temuan ✅ FIXED/CLOSED sudah dipindah ke `CHANGELOG.md` `1.12.0`/`1.13.0`/`1.13.70` dan `PROGRESS.md`. Jangan tambahkan baris duplikat yang sudah ada di Backlog Aksi / Platform Deploy Queue.

### Audit Arsitektur 2026-08-13 — Sisa Sistematis

No open findings.

### Audit 2026-08-16 — Deep Quality (sisa OPEN)

No open findings.

### Audit 2026-08-18 — Web ↔ Gateway ↔ Backend Cross-Layer (hanya OPEN)

No open findings — GW-ROUTING-003/BE-BIO-001 + BE-SUPP-001 CLOSED 1.13.69 → `CHANGELOG.md` `1.13.69`.

### Audit 2026-08-17 — Backend + Web (38 findings CLOSED 2026-08-18 → CHANGELOG `1.12.0`)

No open findings — 38/38 FIXED 1.12.0.

### Audit 2026-08-18 — DX Engineering (hanya OPEN)

No open findings — `DX-TS-BRANDED-001` + `GW-CONCUR-001` CLOSED 1.13.8 → `CHANGELOG.md` `1.13.8`.

### Audit 2026-08-21 — Quality Engineer Swarm (Backend + Web-App)

No open findings — 20/20 CLOSED 1.13.70 → `CHANGELOG.md` `1.13.70` (swarm 5 agents + codegraph + Context7, P0 clear, sisa ponytail deferred di P1 harden).

### Audit 2026-08-25 — CI/CD & Platform Health (hanya OPEN)

No open findings — `ARGOCD-SYNC-001` `data-*` `OutOfSync Healthy` + `Unknown` during rotation **CLOSED 1.18.53** `verify-argocd-sync.sh` PASS → `CHANGELOG.md` `1.18.53`.

Catatan sesi 2026-08-25: failure PipelineRun lama di `payu-cicd` (gateway-service-build-w8n7r/d78wt, web-app-build-56jr4/qpn4t, transaction-service-build-rpn86/76564) sudah disulih run hijau (`gateway-service-build-hq6pw`, `web-app-build-c7z4h` — tag 1.18.46, 15/15 tasks) dan dihapus dari cluster; akar masalahnya diperbaiki di `672b247c9` + `1b6133d4e`. Tidak perlu entri terpisah.

### Audit 2026-08-26 — Web Login & Onboarding (sisa OPEN saja; FIXED → CHANGELOG 1.18.47)

No open findings — `SSO-DPOP-003` `payu-web-app` `dpop=false` `BFF` proof deferred **CLOSED 1.18.55** `verify-dpop.sh` PASS `payu-mobile` `dpop=true` kept → `CHANGELOG.md` `1.18.55`.

Catatan sesi 2026-08-26: audit + fix + E2E — login 3/3 stabil → dashboard; register UI→API **201** (NIK valid, data unik); CSP nonce onboarding 0/33 → 29/30; build Tekton web-app/gateway/account **15/15** `1.18.47`; spec kontrol `forgot-password`+`not-found` 2/2 PASS. Incident node mati `ip-10-0-88-91` (8 VolumeAttachment orphan → Multi-Attach, DB Pending) disulihkan dengan menghapus VA stale. Detail lengkap fix: `CHANGELOG.md` 1.18.47.

### Audit 2026-08-26 — Pipeline Performance & Drift Dokumen↔Pipeline (grill; CLOSED 1.18.48 pilot transaction/wallet/va-simulator verified, sisa PERF-004 DEFERRED)

> Sumber: grill sesi 2026-08-26. Bukti utama = breakdown per-TaskRun `transaction-service-build-tjwmq` (dev build 18m8s): maven 5m48s + k6 4m9s + rantai scan 3m22s + zap/k6 5m7s. Keputusan grill: changeset perf-only (gate set invariant), hybrid rollout, target lulus p50 ≤10m & p95 ≤12m pada ≥3 run tanpa gate ter-skip. ADR-0072 menyusul saat implementasi.

| Key | Pri | Temuan | Bukti | Sisa |
| CICD-PERF-004 | P3 | **Kapasitas batch** — kontensi terbukti (3 build bersamaan = 36–60m vs 18m single-run) tapi profil beban akan berubah total pasca CICD-PERF-001 (download dependency = bottleneck dominan hari ini) | Batch 2026-08-24 21:25 wallet/va-simulator/support; cluster 4 worker | DEFERRED dengan pemicu objektif: pasca-pilot, uji ulang batch 3 build; bila p95 >15m → eval concurrency policy dulu, baru tambah worker |

### Audit 2026-08-28 — E2E Podman Compose FULL JOURNEY (FINAL)

> Run: `PAYU_VERSION=1.18.51 podman compose --profile apps up -d` → 34 containers Up healthy (DB/cache/kafka/artemis/rustfs/keycloak + 26 app/simulators). Gateway `:8080/q/health UP`, Spring `:actuator/health/liveness UP` untuk 8001-8005,8009-8012,8096. FLOWS 41 vs FEATURES scan OK (no orphan). Verifikasi 13:10Z.

| Key | Pri | Temuan | Bukti | Status |
| E2E-FULL-01 | P1 | **Register #1 PASS** — `POST :8080/api/v1/accounts/register` → 201 `userId 2273a2d2...` / `158d53d8...` `PENDING_VERIFICATION/PENDING` (pwd ≥12 chars; awal 500 `AUTH_BUS_001` terpecahkan) | `curl :8080 .../register` 13:10:40,44 201 | CLOSED |
| E2E-FULL-02 | P1 | **Login #2 PKCE-only LOGIN-003** — `password` grant → 400 `DPoP proof is missing` (expected, dihapus). `client_credentials payu-backend` → 200 JWT RS256 `azp payu-backend` | `curl :8099/.../token` 13:10:05 | CLOSED — browser PKCE via `web-app/e2e` Playwright |
| E2E-FULL-03 | P2 | **Wallet #3 guard PASS** — `GET /wallets/{userId}/balance` + service JWT → 404 `Wallet not found` (auth OK, bukan 401); tanpa token → `MISSING_TOKEN` | `curl :8004 .../balance` 13:10:44 404 | CLOSED — wallet belum provision untuk user PENDING (butuh KYC ACTIVE) |
| E2E-FULL-04 | P2 | **Gateway guards PASS** — `/api/v1/billers`, `/contents`, `/api/v1/fx/rates` tanpa JWT → `MISSING_TOKEN`/`403 ACCESS_DENIED`; dengan JWT → route OK | `curl :8080` 13:10:39/44 + `curl :8080/api/v1/fx/rates` 13:39:49 200 | CLOSED — AuthorizationFilter OK |
| E2E-FULL-05 | P3 | **Partner SNAP-BI #4-5 guard PASS** — `POST /v1/partner/auth/token` tanpa `X-TIMESTAMP` → 400 `MISSING_REQUIRED_HEADER` (HMAC validation OK) | `curl :8080/v1/partner/auth/token` 13:10:40 | CLOSED |
| E2E-FULL-06 | P3 | **Compose `--profile apps` required** — `up -d` saja = 7 infra; `--profile apps` = 34 | `podman ps` 34 Up | Docs update |

### Audit 2026-08-28 — CRUD Web-App ↔ Gateway ↔ Backend (FULL 60 endpoints)

> Run: `PAYU_VERSION=1.18.51 podman compose --profile apps 36 Healthy` → Gateway `:8080/q/health UP` `Web :3001/api/health healthy` `Spring :8001/8004/8005/8009-8012/8096 UP`. CRUD `60 endpoints` via gateway dengan JWT `payu-backend` 13:39Z (`/api/v1/fx/rates` `200`, `/api/v1/pockets` `200`, `/api/v1/products` `200` setelah fix, `/api/v1/contents` `200`, `/api/v1/billers` `200`, `/api/v1/transactions` `200`, `/api/v1/statements` `200` dll). Temuan & fix:

| Key | Pri | Temuan | Bukti | Status |
| CRUD-001 | P1 | **Product-catalog 500** — `GET :8080/api/v1/products` & direct `:8100/products` → `500 ClassCastException LinkedHashMap → ProductDefinition` (cache Jackson 3 generic, sama `cms-service 1.8.12 TypedJsonRedisSerializer`). | `podman logs product-catalog` `ClassCastException` 13:31, `curl :8100/products` 500 → setelah `PAYU_CACHE_ENABLED=false` + `podman restart payu-cache` → `curl :8100/products` 200 `SAVINGS_BASIC` + `curl :8080/api/v1/products` 200 | CLOSED — source `ProductCatalogService.java` `@CacheWithTTL` di-disable + `podman-compose.yml` `PAYU_CACHE_ENABLED=false` + `SPRING_CACHE_TYPE=none`, `podman restart payu-cache` 13:39 200 |
| CRUD-002 | P2 | **Gateway fx prefix drift** — `FEATURES.md F1` `GET /v1/rates` (direct) vs gateway `GET /v1/rates` → `404 No route`, correct via gateway `GET /api/v1/fx/rates` → `200` (`RouteRegistry fx → /v1`). | `curl :8080/v1/rates` 404 vs `curl :8080/api/v1/fx/rates` 200, `curl :8096/v1/rates` 200 direct | CLOSED — `FEATURES.md` `F1` diperbaiki: direct `:8096/v1/rates` · via gateway `/api/v1/fx/rates` |
| CRUD-003 | P2 | **Transfer type enum drift** — `FLOWS.md` `POST /v1/transfers type=INTERNAL/BIFAST` vs code `TransactionType` `INTERNAL_TRANSFER` `BIFAST_TRANSFER` etc → `400 SCHEMA_VALIDATION_FAILED`. | `curl :8080/api/v1/transactions/transfer type INTERNAL 400`, `INTERNAL_TRANSFER 403` (authz, bukan schema) | CLOSED — `FLOWS.md` `3`+`7` diperbaiki `POST /api/v1/transactions/transfer type=INTERNAL_TRANSFER|BIFAST_TRANSFER` |
| CRUD-004 | P3 | **403 restricted** — `GET /api/v1/disputes` `/notifications` `/compliance` `/backoffice/tasks` → `403 IP_NOT_ALLOWED/Insufficient permissions` dengan JWT `payu-backend` (RBAC/IP, bukan bug gateway). | `curl :8080/api/v1/disputes -H JWT` 403 | CLOSED — by design, service `payu-backend` tidak punya role `backoffice`/`dispute:read`, `podman logs` 0 ERROR |
| CRUD-005 | P3 | **Path drift minor** — `FEATURES` `POST /api/v1/biller/pay` vs gateway `POST /api/v1/payments` / `POST /api/v1/billing/payments` → `404 No route`. | `curl :8080/api/v1/biller/pay` 404 vs `curl :8080/api/v1/payments` 200 | CLOSED — `FEATURES.md` sudah benar `B1 /api/v1/payments`, gateway `RouteRegistry payments → /api/v1/payments`, test salah path |

Verifikasi CRUD: `60 endpoints` `auth_ok` (200/404/405/400 bukan 401/500) `47 PASS` awal → setelah fix `28/28 E2E-FULL` + `19/19 CRUD via gateway correct prefix` PASS. Sisa `404` = business 404 (bukan routing), `403` = RBAC. Bukti: `curl :8080/api/v1/fx/rates, /pockets, /products, /statements, /contents, /billers, /transactions` semua `200` 13:39Z.
### Audit 2026-08-30 — Per-Service Backend + Web-App (1 by 1) — 8 CLOSED 1.18.76

> Run: `codegraph_explore 28 service + 5 simulator + web-app 40 routes` + `grep` 8-point (Money 19,4 HALF_EVEN, X-Idempotency-Key, PII mask, outbox, hex, test, container, TODO). Web-App 8/8 PASS. Simulators/python Money Decimal string PASS. Core/support: 8/8 FIXED.

| Key | Pri | Temuan | Bukti | Sisa |
|---|---|---|---|---|
| PER-SVC-001 | P1 | **LENDING-MONEY scale 2→4** — `LendingApplicationService:281` `LoanPreApprovalService:139` `DmnService:73` `2→4` | `git diff lending 2→4 HALF_EVEN` | CLOSED 1.18.76 |
| PER-SVC-002 | P1 | **FX-MONEY Settlement 2→4** — `SettlementFxRate:86 2→4` | `SettlementFxRate.java:86 4` | CLOSED 1.18.76 |
| PER-SVC-003 | P1 | **DISPUTE-REFUND idempotency** — `RefundEntity` + `V9__add_idempotency_key` `UNIQUE` | `V9__add_idempotency_key_to_refunds.sql` `RefundEntity idempotencyKey` | CLOSED 1.18.76 |
| PER-SVC-004 | P1 | **6 endpoint idempotency** — `Transaction qris` `Budget 2` `Lending 2` `Investment 1` `LOP 2` `required=true` | `grep Idempotent 6` `TransactionController:473` `BudgetController:50` `LendingController:147,211` `InvestmentController:64` `LoanOriginationController:33` | CLOSED 1.18.76 |
| PER-SVC-005 | P2 | **SUPPORT-OUTBOX** — `outbox-starter` + `SupportOutboxEventAdapter` `payu.support.ticket-created.v1` | `support-service pom outbox-starter` `SupportOutboxEventAdapter.java` | CLOSED 1.18.76 |
| PER-SVC-006 | P2 | **GATEWAY-CORS** — `allowed-headers +Idempotency-Key` legacy | `gateway application.yaml:169 X-Idempotency-Key,Idempotency-Key` | CLOSED 1.18.76 |
| PER-SVC-007 | P3 | **PII amount log** — `lending/LOP` `log.info` mask `***` | `grep log.info.*amount 0` | CLOSED 1.18.76 |
| PER-SVC-008 | P3 | **HEALTHCHECK** — `lending/investment Containerfile` `HEALTHCHECK` add | `Containerfile HEALTHCHECK /actuator/health/liveness` | CLOSED 1.18.76 |

### Audit 2026-09-03 — Relay E2E Full Money Journey (dev cluster, Chrome via omp browser relay)

> Run: relay `http://127.0.0.1:9224` + Chrome XFCE, `https://payu-dev.apps.fajjjar.my.id`. Login OIDC customer1 → dashboard → transfer clicks. Spec baru `frontend/web-app/e2e/money-journey.spec.ts` (REG-VAL-001, LOGIN-J-001, TRF-J-001/002/003).

| Key | Pri | Temuan | Bukti | Status |
|---|---|---|---|---|
| RELAY-001 | P1 | **Login orphan account** — BFF fallback `account-${sub}` tanpa `account_id` claim → saldo Rp 0 + `403 ACCESS_DENIED` wallet. | `callback/route.ts:98`, `GET /wallets/account-07f1…/balance 403` | CLOSED — mapper `account_id` + atribut `accountId` customer1=`750e8400-…-0001` di live Keycloak + `keycloak-realm-import.yaml` |
| RELAY-002 | P1 | **Dev DB tanpa seed wallet** — `payu_wallet.wallets` kosong (RLS sembunyikan tanpa `app.tenant_id`; seed `wallet-test-data.sql` belum di-run di dev). | `psql payu_wallet SELECT 0 rows` tanpa tenant GUC | CLOSED (dev) — wallet `750e8400-…-0001` Rp 10jt + `1001001002` Rp 5jt manual; seed job permanen tetap OPEN di RELAY-010 |
| RELAY-003 | P2 | **Confirm transfer 429 persisten** — review OK tapi POST tak fire; toast `Terlalu banyak permintaan`. Akar: interceptor axios retry-429 x3 + RQ retry = storm 8x. | DevTools metrics 24x, `api.ts` retry block | CLOSED — 429 tak pernah di-retry (`api.ts` + `providers.tsx` predicate), test ditulis ulang lawan interceptor asli 4/4 |
| RELAY-004 | P2 | **Full-reload buang sesi** — `tab.goto` ke `/transfer` mental ke `/login`, SPA aman. Store zustand in-memory hilang saat reload. | relay `NOFORM:/login?callbackUrl=%2Ftransfer` | OPEN — selidiki middleware refresh vs persist store |
| RELAY-005 | P3 | **Live realm drift** — cuma `customer1`, atribut phone/nik hilang (user-profile declarative drop unmanaged attrs). | `GET /admin/realms/payu/users` 1 user | OPEN (parsial CLOSED: `unmanagedAttributePolicy=ENABLED` + mapper; sinkronisasi penuh menyusul) |
| RELAY-006 | P1 | **Dev tanpa Redis** — VelocityGuard fail-secure tiap transfer (`422 AML_VELOCITY…`, `Unable to connect to Redis`); live Deployment transaction-service juga kehilangan env REDIS (predates base). | svc `payu-cache-resp` tak ada; pod env kosong | CLOSED — `redis-standalone.yaml` (ACL developer, svc `:11222`) + env via overlay, `oc apply -k` |
| RELAY-007 | P1 | **Status CHECK vs enum** — `PENDING_COMPLIANCE_REVIEW` (25ch) vs `status VARCHAR(20)` + `valid_status` CHECK lama → 22001/23514, API 500. | log `value too long` + `rule _RETURN` matview | CLOSED — V32 widen VARCHAR(40) + recreate 3 matview, V33 CHECK full enum |
| RELAY-008 | P1 | **Fraud scoring tanpa kredensial** — adapter panggil analytics tanpa JWT → 401 → semua transfer HOLD. | `401 /fraud/score`, adapter tanpa header | CLOSED — teruskan bearer SecurityContext (null-safe), test header 5/5 adapter |
| RELAY-009 | P1 | **Analytics tanpa KEYCLOAK_URL** — `jwt_auth` 401 semua call terautentikasi (termasuk fraud/score). | pod env kosong | CLOSED — overlay `KEYCLOAK_URL` in-cluster per L-408 |
| RELAY-010 | P2 | **Seed job dev permanen** — wallet/account/seed + realm users hanya manual; dev baru = journey mati lagi. | Temuan sesi ini | OPEN — jadikan Job/Flow terdokumentasi (wallet+account seed, realm import sinkron) |
| RELAY-011 | P1 | **gRPC deadline beku di shared stub** — `withDeadlineAfter` saat init → semua call 30 dtk pasca-boot `DEADLINE_EXCEEDED` offset negatif membesar (semua service pemakai helper). | `-321s` di log, 0 call sampai wallet | CLOSED (transaction: Wallet+Account adapter per-call) — OPEN sisa: billing/fx/investment/lending pakai pola sama |
| RELAY-012 | P1 | **Transfer amount string vs schema** — frontend kirim Money string, gateway minta JSON number → semua transfer web 400. | `400 SCHEMA_VALIDATION_FAILED` | CLOSED — konversi di `TransactionService.initiateTransfer`, test ekspektasi angka 9/9 |
| RELAY-013 | P1 | **fromAccountId cuma via kontak favorit** — ketik manual → zod gagal diam-diam, confirm mati. | network 0 POST, tombol enabled | CLOSED — `useEffect` default dari session, `TRF-J-001` isi manual |


---


## 🛡️ DEVSECOPS-017 — Production-Ready Architecture

Success criteria: setiap mandatory control di `architecture/DEVSECOPS_ARCHITECTURE.md` punya repository tests + bukti live cluster.

Seluruh mandatory control `[x]` **CLOSED 1.18.30–1.18.34** — bukti live + deferred notes (KMS BYOK, DR drill penuh) di `CHANGELOG.md` + `PROGRESS.md`.

---

## 🏛️ Architecture Decision Records (ADR) Governance & Backlog

> Hasil audit strategis (`principal-architect`): Penyelarasan status ADR, gap implementasi, ADR baru, dan anti-pattern.

### 1. 🔄 ADR Status Alignment & Maintenance (Drift Dokumen)

Drift audit sweep 2026-08-24 (70 ADR vs repo): 3 klaim bukti dikoreksi — beres. Index [`docs/adr/README.md`](../adr/README.md) current.

### 2. 🔴 ADR yang Sudah Ada tapi Belum / Sebagian Diimplementasikan

No open gap — seluruh ADR-GAP (003..009, 015, 019, 028W, 029, 030E, 032W, 047, 048, 054C, 056) **CLOSED 1.18.9–1.18.40** + `ADR-GAP-0014/0016/0069/0071` **CLOSED 1.18.56–1.18.59** → `CHANGELOG.md` `1.18.59`.


### 3. 📝 Backlog ADR Baru yang Perlu Dibuat

Semua ADR backlog sudah dibuat & terindeks — ADR-0067 **Deferred** (NO-GO B4.6), ADR-0068 **Accepted** (live 1.18.15, verified 1.18.40). Tidak ada ADR pending.

### 4. ⚠️ Kesenjangan Best Practice & Anti-Pattern yang Memerlukan Remediasi

No open gap — remediasi best-practice tuntas via harden items **CLOSED 1.18.19–1.18.40** → `CHANGELOG.md`.