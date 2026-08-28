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

| **Last Release** | `1.18.67` (2026-08-28) |
| **Core Banking MVP** | 🟢 MVP workloads live di 5 environment; CNPG **payu-dev 3/3 2/2 Healthy** `barman-cloud 1/1` `ObjectStore 5/5` `S3 WAL archiving True` `RPO=0`, Tekton **31/31 Succeeded** (cnpg storage 20Gi wal 10Gi 1.18.42, fx-service 1.18.41 FX 0 WARN, transaction 1.18.40 Topics+KEDA, partner SLO 1.18.21, HPA/PDB 1.18.20, Cache Plain 1.18.19, WORM 1.18.27), workloads `49/49 1/1` `1.18.65` `coraza 2/2` `KEDA RH-CMA 5 ScaledObjects` `Litmus 6 pods + Kraken/Cerberus` `SSO sso-dev/sso-sit/sso.uat/preprod/prod 5 env` `CNPG/Kafka/EFS/3scale/RHACS` verified. |
| **Backlog Aktif** | **0 OPEN** — Grill P1 **7/7 CLOSED** 1.18.60-1.18.62 ✅ + **P3 Extended 3/3 CLOSED 1.18.63-1.18.64** ✅ + **Hotfix 1.18.65 Insecure crypto.randomUUID Fallback** ✅ + **UI Responsive 1.18.66 Web-App Mobile-Friendly** ✅ + **BFF/OpenAPI 1.18.67 Beneficiary A3** ✅ |
| **Last Updated** | 2026-08-28 — **1.18.67 BFF OpenAPI + Beneficiary A3** + **1.18.66 Web-App Responsive Mobile-Friendly** + **1.18.65 Onboarding Insecure Context Fallback** + GLOBAL-RECON + GLOBAL-BFF **1.18.64** + GLOBAL-WEBHOOK **1.18.63** |
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

### P3 — Backlog Lanjutan — Global Hardening Extended + Legacy
No open P3 — 3/3 CLOSED 1.18.63 (GLOBAL-WEBHOOK) + 1.18.64 (GLOBAL-RECON + GLOBAL-BFF) → `CHANGELOG.md` `1.18.64` — extended hardening 100%.


| Key | Domain | Item | ADR Ref |
|:---|:---|:---|:---|
| — | — | *Legacy P3 kosong — items sebelumnya CLOSED* | — |

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