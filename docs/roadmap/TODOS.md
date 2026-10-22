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

| **Last Release** | `1.18.103` (2026-09-10) |
| **Core Banking MVP** | 🟢 MVP workloads live di 5 environment; CNPG **payu-dev 3/3 2/2 Healthy** `barman-cloud 1/1` `ObjectStore 5/5` `S3 WAL archiving True` `RPO=0`, Tekton **31/31 Succeeded** (cnpg storage 20Gi wal 10Gi 1.18.42, fx-service 1.18.41 FX 0 WARN, transaction 1.18.40 Topics+KEDA, partner SLO 1.18.21, HPA/PDB 1.18.20, Cache Plain 1.18.19, WORM 1.18.27), workloads `49/49 1/1` `1.18.77` `coraza 2/2` `KEDA RH-CMA 5 ScaledObjects` `Litmus 6 pods + Kraken/Cerberus` `SSO sso-dev/sso-sit/sso-uat/preprod/prod 5 env` `CNPG/Kafka/EFS/3scale/RHACS` verified. |
| **Backlog Aktif** | **0 OPEN + 2 DEFERRED (2026-10-22 loop-4)**: `PROMO-2026-08` CLOSED (UAT 18/18 + money 5000 + Kraken/Cerberus/Litmus Pass) → `CHANGELOG.md` `[Unreleased]`; `GRPC-AUTH-001` sisa tuntas (`recipient_account_id` `1.18.138`, locator ok); file dibersihkan per aturan (51 baris CLOSED dihapus). Sisa: `PLAT-DRIFT-001` (prod) + gates (`DEPLOY-010`, `SEC-020`, `PROD-READINESS`) — eksternal. DEFERRED: `CICD-PERF-004`, `CICD-FUZZ-001`. |
| **Last Updated** | 2026-10-22 loop-4 (actionable run): shell E2E multi-env (`PAYU_ENV`, `env.sh`), UAT final 18/18 + transfer 5000 COMPLETED, Kraken preprod (etcd kill 10s recovery + Cerberus go) + Litmus pod-delete Pass + probe 100%, scope `account:verify` 3 env, tx `1.18.138` recipient fix; lessons L-498..L-500. Loop-3: SIT/UAT/preprod bootstrap. |

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

| Key | Domain | Item                                         | ADR Ref |
| :-- | :----- | :------------------------------------------- | :------ |
| —   | —      | _Legacy P3 kosong — items sebelumnya CLOSED_ | —       |

## 🔗 TokoBapak PayU Integration — Audit 2026-08-30 (Sisa OPEN)

No open — **5/5 CLOSED 1.18.77** (PAYU-TB-001..005 TokoBapak SNAP-BI E2E `partner-service` `tokobapak-mvp` `ACC_TOKOBAPAK_ESCROW`/`ACC_SELLER_*` seed `V23`/`V122` + `Keycloak tokobapak-mvp` + `SnapBiTokoBapakContractTest` 5/5 `4002501/4002502/2002500` idempoten `uq_snap_payment_partner_ref` + `payu_client.go` HMAC-SHA512 `SignForB2B`/`SignWithToken` `source/beneficiaryAccountNo` + `PAYU_BASE_URL http://payu-partner-service:8080` `payu-network` + `TOKOBAPAK_SNAPBI.md` HMAC `±300s` `4012504/4012506`) → `CHANGELOG.md` `1.18.77` — **TokoBapak BELUM → BISA bayar via PayU**.

## 🏦 Partner Service Production Readiness Gate

Status `partner-service` hanya Production Ready setelah seluruh gate memiliki bukti live. `PARTNER-001..006` CLOSED (2026-08-08).

No open gate — PARTNER-PROD-007..011 ✅ Selesai 1.18.9–1.18.21 → `CHANGELOG.md`.

> Local APIcast (profile `api-management`) tidak bisa authless — public edge butuh APIManager (cluster-level).

---

## 🚀 Platform Deploy Queue

| PLAT-DRIFT-001 | P3 | Env convergence | ✅ CONVERGED payu-dev 2026-10-12 + **payu-sit/payu-uat/payu-preprod 2026-10-22** — tiap env 33/33 1/1 dari overlay git (digest re-pin dari ImageStream dev + skopeo split-creds, sit/uat/preprod-plain-cache, OIDC per-env, Route SSO first-class). Sisa: prod — butuh namespace + backing-store + approval (`PROD-READINESS`). |
| DEPLOY-010 | — | Deploy 3scale APIManager after external backing-store/Vault secrets exist | `docs/operations/INFRASTRUCTURE_DEPLOYMENT.md#Current Known Gates` |
| SEC-020 | — | Remediate CIS platform failures | `docs/operations/INFRASTRUCTURE_DEPLOYMENT.md#Current Known Gates` |
| PROD-READINESS | — | Argo Rollouts, production storage, Vault HA/DR, approvals, and signed-image admission evidence | `docs/operations/INFRASTRUCTURE_DEPLOYMENT.md#Current Known Gates` |

---

## 📋 Open Findings — Sisa OPEN Only (FIXED → CHANGELOG/PROGRESS)

### Audit 2026-10-09 — Live Web-App CRUD + Infra Sweep (REMEDIATED 2026-10-09)

> Run: sweep 24 rute customer + 9 rute backoffice via `https://payu-dev.apps.fajjjar.my.id`, probe ~35 endpoint read, eksekusi mutasi nyata, spot-check 5 database, scan log 24h 26 service. 15/16 tiket FIXED + deployed (`web-app 1.18.127`, backend `1.18.126` — detail + bukti di `CHANGELOG.md [Unreleased]`); sisa 1 tiket di bawah. Env imperative (tx Redis, promo topic) → konvergen git via `PLAT-DRIFT-001`.

| Key            | Pri | Temuan                                                                                                                                                                               | Bukti                                                                                                                                                                                                           | Sisa                                                                                                                      |
| :------------- | :-- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------ |

**Catatan keputusan (tanpa tiket):** `ACCT-BAL-001` — `payu_account.accounts.balance` tidak punya writer runtime (hanya seed `V113`); keputusan: **wallet-service adalah satu-satunya sumber saldo**, kolom lama dibiarkan dormant (dipertahankan untuk pelaporan historis). `PORTAL-LOG-001` — access log Quarkus dinyalakan; 0 baris = api-portal tidak menerima trafik (terbukti idle, bukan buta).

> Aturan: section ini hanya untuk temuan yang masih OPEN. Seluruh temuan ✅ FIXED/CLOSED sudah dipindah ke `CHANGELOG.md` `1.12.0`/`1.13.0`/`1.13.70` dan `PROGRESS.md`. Jangan tambahkan baris duplikat yang sudah ada di Backlog Aksi / Platform Deploy Queue.

### Audit 2026-10-09 — Ponytail Dead-Code Sweep (EXECUTED, commit `941041f8c`)

> Run: whole-repo ponytail-audit (delete/stdlib/native/reuse/yagni/shrink) — grep konsumen termasuk test, fixture, dan referensi dinamis. Eksekusi "hapus yang aman": **-3.080 baris, +6, 0 perubahan perilaku**. Bukti: `tsc --noEmit` 0 error; vitest 1126 lulus / 23 gagal = baseline pre-existing identik (42 test hilang = file mati yang dihapus); `next build` compiled successfully.

| Key      | Pri | Temuan                                                                                                                                                                                                                                                                                                    | Aksi                                         | Bukti                                                                                |
| :------- | :-- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------- | :----------------------------------------------------------------------------------- |
| PONY-001 | P3  | 5 komponen dashboard + barrel `components/dashboard/index.ts` tak direferensikan (BudgetTracking, SpendingInsights, FinancialHealthScore, InvestmentPerformance, StatsCharts) — dashboard asli hanya render BalanceCard/QuickActions/TransferActivity                                                     | ✅ DELETED (+5 file test)                    | grep consumer = 0; `DashboardPage.test.tsx` mem-mock barrel stale (mock dibersihkan) |
| PONY-002 | P3  | Hook mati `useBudgets` (0 consumer) dan `useAntdMessage` (digantikan `lib/notify`, 24 consumer)                                                                                                                                                                                                           | ✅ DELETED (+barrel `hooks/index.ts` + test) | grep 0 consumer                                                                      |
| PONY-003 | P3  | `mutation-config.ts`: 4 const internal (`defaultRetryDelay`, `financial/nonFinancial/readOnlyMutationConfig`) diekspor tanpa perlu                                                                                                                                                                        | ✅ SHRINK — export dilepas                   | hanya `MutationPresets` publik                                                       |
| PONY-004 | P3  | `a11y.tsx` `visuallyHidden` + `useA11yAnnouncer` tanpa consumer                                                                                                                                                                                                                                           | ✅ DELETED (+properti `a11yUtils` dibuang)   | `a11yUtils` sendiri tetap (FeedbackWidget pakai `useFocusTrap`)                      |
| PONY-005 | P3  | 3 alias icon mati (`BellRing`, `Inbox`, `Send`) dari 127 alias                                                                                                                                                                                                                                            | ✅ DELETED (127→124)                         | grep boundary word                                                                   |
| PONY-006 | P3  | `validateUUID` tanpa consumer; `generateUUID` hanya internal                                                                                                                                                                                                                                              | ✅ DELETED / SHRINK                          | `assertUUID` yang dipakai service                                                    |
| PONY-007 | P3  | ~~25 type export mati di `src/services/*.ts`~~ — **FALSE POSITIVE**: audit awal memakai regex yang mengabaikan referensi ter-qualify (`WalletService.UpdateCardRequest`) dan pemakaian dalam-file; script sempat hapus, `tsc` langsung error 5, `src/services/` di-restore, audit ulang = **0 type mati** | ❌ TIDAK DIHAPUS (dibatalkan)                | `git checkout -- src/services/`; script ulang dengan pola benar → nol                |
| PONY-008 | P3  | `backend/shared/python-logging/` tanpa consumer in-repo                                                                                                                                                                                                                                                   | ✅ DELETED                                   | tak ada FastAPI service yang depend                                                  |
| PONY-009 | P3  | ~~`useWebSocket`/`useAnalyticsWebSocket`~~ (degrade ke REST karena WS server belum ada)                                                                                                                                                                                                                        | ✅ SELESAI 2026-10-13 lewat `WEB-OS-008` — hooks kini tersambung ke server live (bukan dead code; consumer nyata) | `analytics/page.tsx` Live Update badge + REST fallback |
| PONY-010 | P3  | `shared/flyway/migrations/` sebagai "Source:" origin untuk migration per-service                                                                                                                                                                                                                          | ⏳ TETAP (dokumentasi, bukan dead code)      | comment origin di auth/billing/fx/…                                                  |

**Catatan:** koreksi utama audit repo-wide = jangan andalkan regex tunggal; `tsc` adalah jaring terakhir sebelum hapus type/export (dua kali menyelamatkan: qualified-ref + same-file usage).

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
| CICD-FUZZ-001 | P3 | **Schemathesis SIT flaky** — `request_timeout` 0/N pada path sampah fuzz (`GET /v1.0/{bytes}`, `/v1/partner/{bytes}`), `[FLAKY]` tak terreproduksi; direct curl path sama → 400 0.25s. Tiap fail = 44-45 passed + 1-2 timeout; biller lolos percobaan ke-5, dukcapil+qris ke-4, va ke-6 (`wwf96` 8/8, fuzz 6m46s bersih). Teori kontensi GUGUR: run solo tetap gagal pola sama → murni luck per-run | `biller gwvwl/hksbp/smqvn/qzg8p FAIL → 6b4tm PASS`; `dukcapil 7n6px/24mjc/5z2f6`, `qris 9jkrb/666f8/j7xxf`, `va vd9xj/lgjfp/qkg6f/pfp5c` FAIL; `dukcapil slhmg`, `qris dkhl7`, `va wwf96` PASS 2026-09-12 | Semua hijau 2026-09-12; tindak lanjut DEFERRED = timeout khusus fuzz-path atau seed tetap agar reproduksibel |

### Audit 2026-09-11 — Identity Drift SIT/UAT

No open findings — `IDN-001/002/003` + `KAFKA-UAT-001` CLOSED (SIT+UAT bootstrapped, bukti live) → `CHANGELOG.md` `[Unreleased]`.

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

### Deep Dive: Queue, Message Broker, Notification (2026-10-07)

#### Kafka Topics & Consumers

| Topic                              | Producer                                           | Consumer Group                   | Purpose                                 |
| ---------------------------------- | -------------------------------------------------- | -------------------------------- | --------------------------------------- |
| `payu.account.user-created.v1`     | `KafkaUserEventPublisherAdapter` (account-service) | `wallet-service-group`           | User registration → wallet provisioning |
| `payu.account.user-updated.v1`     | `KafkaUserEventPublisherAdapter` (account-service) | —                                | User profile update                     |
| `payu.dispute.refund-requested.v1` | dispute-service                                    | `wallet-service-refund-reversal` | Refund reversal execution               |
| `payu.promotion.notification.v1`   | `KafkaNotificationAdapter` (promotion-service)     | —                                | Cashback notification                   |

**Outbox Pattern**: Semua event publish via `OutboxService.createEvent()` — atomic business + event. `MdcKafkaConsumerInterceptor` untuk correlation ID propagation.

#### Notification Service Architecture

```
NotificationResource (REST API)
  ├── GET /user/{userId} — list notifications
  ├── GET /{id} — get by ID
  ├── POST /{id}/read — mark as read
  └── POST /send — send notification

NotificationService
  ├── Idempotency check (IDEMP-003)
  ├── attemptSendWithFallback()
  │   ├── SmsSender (fail-closed default)
  │   ├── PushSender (fail-closed default)
  │   └── EmailSender (Quarkus Mailer)
  └── handleFailedNotification()
      ├── Exponential backoff retry (2, 4, 8 min)
      └── Max retry → FAILED
```

**Sender Chain**: SMS → Push → Email. Fail-closed default (provider `NONE` → return false). Recipient masking via `RecipientMasker`.

#### WebSocket & Real-Time

| Component                          | Status       | Catatan                                          |
| ---------------------------------- | ------------ | ------------------------------------------------ |
| `useWebSocket` hook (frontend)     | ✅ Ada       | Exponential backoff, reconnect max 10            |
| `useAnalyticsWebSocket` (frontend) | ✅ Ada       | Listen `BALANCE_UPDATE` events                   |
| WebSocket server                   | ❌ Tidak ada | `/ws/analytics` → 404, `/ws/notifications` → 404 |
| Analytics broadcast                | ❌ Tidak ada | Consumer simpan ke DB saja                       |

#### Redis & Queue

| Usage               | Service                               | Catatan                          |
| ------------------- | ------------------------------------- | -------------------------------- |
| Velocity check      | `VelocityGuard` (transaction-service) | Redis INCR + EXPIRE              |
| Session cache       | `authStore`                           | Zustand in-memory (no persist)   |
| API rate limiting   | `gateway-service`                     | Bucket4j                         |
| Scheduled transfers | `ScheduledTransferService`            | DB scheduler (bukan Redis queue) |

#### Gateway Route Gaps

| Route                   | Status          | Impact                                          |
| ----------------------- | --------------- | ----------------------------------------------- |
| `/api/v1/notifications` | ✅ Registered   | Notification API reachable (verified 200)       |
| `/ws/analytics`         | ❌ No WS server | Real-time analytics unavailable (REST fallback) |
| `/ws/notifications`     | ❌ No WS server | Real-time notifications unavailable             |
| `/api/v1/transactions`  | ✅ Registered   | Transfer works                                  |
| `/api/v1/wallets`       | ✅ Registered   | Wallet works                                    |

**Summary**: Dev auth restored (SSO-DEV-KC-001, NOTIF-PRINCIPAL-001 closed) — 44/44 pods Ready, live e2e login-flow 14/14 + notifications 6/6, web-app vitest 1173/1174. The "notification API not exposed via gateway" claim (WEB-OS-006/007/009) was wrong: routing was always present; 401/403/500 traced to the Keycloak-host drift and the principal-claim mismatch. Only WEB-OS-008 (WebSocket server) remains, as a product decision.

### Audit 2026-10-20: E2E payu-dev fresh cluster

No open findings — `GRPC-AUTH-001` CLOSED (auth fix `1.18.132` + `recipient_account_id` backfill `1.18.138`, locator `[data-testid="amount-input"]`) → `CHANGELOG.md` `[Unreleased]`.

### Audit 2026-10-21: Service Mesh 3.4 live di payu-dev — follow-up mesh (OPEN)

> Konteks: OSSM 3.4 (Sail Operator) sudah live di `payu-dev` — 24 service app-tier ter-mesh, mutual TLS mesh-to-mesh terbukti. **Status 2026-10-21**: `MESH-005` **CLOSED** (console Kiali live untuk `payu-dev`), `MESH-002` **advanced** (posture mTLS pindah ke overlay per env; file legacy root-namespace STRICT tetap di luar apply path), `MESH-003` **advanced** (STRICT di repo + ter-apply untuk `payu-uat`/`payu-preprod` tapi belum terverifikasi karena env tsb belum di-bootstrap; separuh `AuthorizationPolicy` masih terbuka), `MESH-001` **sebagian** (objek legacy prod ditandai SUPERSEDED, selector `app:` yang tak match masih perlu ditulis ulang). **Temuan baru 2026-10-21**: `MESH-007` **P1** (jalur simulator bocor ke internet lewat route mesh publik), `MESH-008` **P1** (prasyarat rollout STRICT: plaintext dari router harus lewat mesh gateway), `MESH-009` **P2** (aturan mesh SLO di repo tidak termuat di cluster ini) dan `MESH-010` **P2** (konflik listener TCP Envoy di 12 sidecar). ADR: [ADR-0072](../adr/0072-openshift-service-mesh-3-4-sail-operator-dev-rollout.md) dan [ADR-0073](../adr/0073-istio-ingress-gateway-and-shared-domain-ownership.md).; **Temuan lanjutan 2026-10-21**: `MESH-011` **P1** (BFF `web-app` tidak ber-mesh -> hop ke `gateway-service` plaintext dan akan putus saat namespace prod STRICT), `MESH-012` **P2** (belum ada `DestinationRule`/`VirtualService`/`ServiceEntry` di cluster, aset `base/destination-rules.yaml` yatim dan subset labelnya tidak pernah match) dan `MESH-013` **P2** (`ServiceEntry` host eksternal yatim, jadi traffic ke partner asli di prod belum termodelkan di mesh)

| Key | Pri | Temuan | Bukti | Sisa |
| :-- | :-- | :----- | :---- | :--- |
