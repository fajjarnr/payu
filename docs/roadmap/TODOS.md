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

## 📊 Board Summary

| Metric | Value |
|:---|:---|
| **Cluster Status** | 🟢 OCP 4.20.29, 8 nodes Ready (5 workers across 3 AZs). `payu-dev` 33 deployments + infra all 1/1 Running (snapshot 2026-08-11); 0 HPA; prod & sit/uat/preprod empty di cluster ini (lab env di `cluster-nkk8q`). Keycloak Ready=True (root cause restart = DB endpoint race, resolved). |
| **Last Release** | `1.13.0` (2026-08-18) |
| **Core Banking MVP** | 🔴 Belum MVP production ready — ACCOUNT-007/PROD-044 tetap terbuka dan audit 2026-08-17 menemukan critical/high defects pada wallet, payment, ownership, idempotency, dan web journeys; **login web live** (LOGIN-001..006 closed), money-flow test evidence ada tetapi belum menutup finding baru. |
| **Backlog Aktif** | 2 tickets + 7 cross-layer backend/web findings + 14 DX, deep infra & spec findings (2026-08-18) |
| **Last Updated** | 2026-08-18 — Release `1.13.0`: fixed GW-ROUTING-001/002/004, BFF-ROUTING-001/002, BE-BILL-001/002, SEC-AUTH-001, BE-CARD-001, BE-ACC-001, BE-INVEST-001, BE-PROMO-001/002, FE-IDM-002/003, FE-MONEY-002/003, FE-LEND-001, FE-PROXY-AUTH-001, LEND-SCHED-001. Podman stack deployed 1.13.0, all 37 healthy. |

---

## ⏸️ Deferred Scope

| Key | Item |
|:---|:---|
| READY-061 | Mobile app (seluruh `frontend/mobile`) — ditunda dari MVP/production gate sampai diaktifkan product owner. Jangan kerjakan upgrade/bug/test mobile. |
| PROD-035 | Mobile idempotency durability (SecureStore 2048B limit) — deferred bersama mobile |
| PROD-038 | Mobile money precision (JS `number` untuk amount) — deferred bersama mobile |

---

## 🔴 Active Tickets

| Key | Pri | Summary | Status |
|:---|:---:|:---|:---|
| PROD-044 | P1 | Notification false success — **PARTIAL 2026-08-12**: fail-closed live (SMS/PUSH default NONE → false, LOG hanya eksplisit, `mailer.mock` tidak diwariskan ke prod, `KEYCLOAK_REALM` default). Solusi arsitektur telah diadopsi di [ADR-0027](../adr/0027-notification-service-architecture-and-multi-channel-delivery.md); sisa implementasi provider (Telegram zero-cost lab / Simulator / FCM v1) + multi-channel contact model + data encryption at-rest. | 🟠 Fail-closed live — implementasi ADR-0027 pending |
| INFRA-029 | P1 | Audit log forwarding: CLF live (CIS satisfied), sisa Wazuh SIEM sink (INFRA-011) + verifikasi log arrival. | 🟢 Live — sink pending |

---

## 🎯 Backlog Aksi (urut per priority)

### P1 — Quality & Reliability (In-Scope MVP)

| Key | Domain | Item | Done saat |
|:---|:---|:---|:---|
| CB-006 | platform | Prod deploy core banking: gates + HPA≥2 + PDB2 + DR drill (ACCOUNT-007) | ACCOUNT-007 closed |
| ARCH-NOTIF-001 | notification | Implementasi arsitektur multi-kanal & zero-cost provider sesuai [ADR-0027](../adr/0027-notification-service-architecture-and-multi-channel-delivery.md): (1) Perbaiki mismatch Kafka topic `payment-events` vs `payu.billing.payment-completed.v1` / `payu.transaction.payment-expired.v1`, (2) Multi-channel `contacts` payload & isolasi fallback (hapus string token reuse), (3) Zero-cost lab senders (`TelegramSender` & `SmsSimulatorSender`), (4) `FcmPushSender` via FCM v1 REST API, (5) Enkripsi AES-256 GCM kolom PII `recipient` & `body` di database `notifications` (UU PDP). | Test suite notification-service green (unit + mock integration) + E2E OTP delivery via Telegram / Simulator |
| PROD-002 | fx | Approved FX provider URL/credential + live evidence | Rate live + audit pair |
| PROD-018 | analytics | Aktifkan `analytics-tests` sebagai required branch protection — workflow `.github/workflows/analytics-tests.yml` SUDAH ada (push/PR paths + workflow_dispatch); sisa = setting GitHub branch protection (butuh `gh`/admin repo, belum tersedia di sesi ini) | CI gate aktif via GitHub settings |
| ARCH-TOPIC-002 | platform | ~~KafkaTopic deklaratif untuk semua topic kode (RF 3, partisi sesuai consumer); hapus resource legacy `*-events`; audit auto-create off~~ **MANIFEST DONE 2026-08-13** — `01-kafka-topics-code.yaml` (75 KafkaTopic). **DLQ EXPANSION 2026-08-18** — +33 DLQ topics (financial-critical: transaction, wallet, partner, lending, account), total 107 KafkaTopic (65 normal + 42 DLQ); `v42` test-only topic dihapus dari manifest; DLQ retention dinaikkan 7d→30d; EVENT_CATALOG.md regenerated dari manifest (17 domain, service ownership, gap analysis). Kustomize render OK. Sisa: apply ke cluster + audit auto-create off (butuh OCP creds) | `oc get kafkatopic` lengkap vs EVENT_CATALOG |
| QAMVP-004 | kyc | ~~Security test (auth/RBAC) + integration test kyc; provider OCR/liveness nyata gate (analog PROD-002)~~ **security DONE 2026-08-13** (QAMVP-014 `test_security.py` 401/403 IDOR); e2e workflow test ADA (`tests/e2e/test_kyc_workflow.py`, provider di-mock); CI `.github/workflows/kyc-tests.yml` (unit+e2e, cov gate 80%). Sisa: provider OCR/liveness nyata gate (butuh credential eksternal) | Test + live evidence |
| QAMVP-005 | platform | ~~k6 smoke+load di pipeline staging + SLO threshold per service~~ **CI WIRED 2026-08-13** — `.github/workflows/k6-tests.yml` (grafana/k6-action, smoke/load/stress via workflow_dispatch + cron 02:00, `GATEWAY_URL`/`KEYCLOAK_URL` env, SLO threshold `p95<500ms`/`p99<1s`/`avg<300ms`/`rate<0.01`, summary artifact). Verified terhadap local stack: gateway `/q/health` 200 + keycloak OIDC 200; token acquisition butuh credential client (TEST_USERS) — external. Sisa: green run dengan kredensial staging | Laporan k6 di CI |
| ARCH-GLOBAL-002 | security | Implementasi Step-Up Auth & Dynamic Linking sesuai [ADR-0028](../adr/0028-step-up-authentication-and-dynamic-linking-standard.md): (1) Skema `user_pins` di `auth_db` + Argon2id memory-hard hasher (`Argon2PasswordEncoder`) + 3-strike lockout policy (15m soft-lock), (2) Endpoint `POST /internal/v1/auth/step-up/challenge` (Redis TTL 180s, `payload_digest = SHA256(sender+recipient+amount+currency+nonce)`) dan `POST /internal/v1/auth/step-up/verify`, (3) Integrasi 2-phase flow (`/prepare` -> `/execute`) di `transaction-service` memanggil `StepUpVerificationPort` sebelum reservasi saldo `wallet-service`, (4) Test suite: PIN valid/invalid, attempt counter, lockout 3x, challenge expired, dan tampering rejection. | Test suite auth & transaction step-up green (unit + contract + abuse case) |
| ARCH-GLOBAL-003 | core-banking | Implementasi ISO 20022 Interbank Clearing & Suspense Account Ledgering sesuai [ADR-0029](../adr/0029-iso20022-interbank-clearing-and-suspense-ledgering.md): (1) Inisialisasi Chart of Accounts sistem (`SYSTEM_BI_FAST_CLEARING`, `SYSTEM_SKN_CLEARING`, `SYSTEM_RTGS_CLEARING`, `SYSTEM_QRIS_CLEARING`, `NOSTRO_BI_FAST`) via Flyway di `wallet-service`, (2) Input port & service `WalletClearingUseCase` (`reserveAndHoldClearing`, `settleClearing`, `reverseClearing`) dengan jaminan atomic balanced double-entry (`JournalEntry.isBalanced()`), (3) Refactor `InitiateTransferCommandHandler` di `transaction-service` untuk memanggil clearing port pada saat inisiasi & callback settlement/reversal, (4) Invariant test suite: verifikasi double-entry balance debit==credit pada siklus transfer sukses, reject, timeout, dan QRIS. | Double-entry ledger clearing audit match (debit==credit 100%) + invariant tests green |
| ARCH-GLOBAL-004 | risk-aml | Implementasi Real-Time Velocity Counter & AML Risk Scoring Pre-Check sesuai [ADR-0030](../adr/0030-realtime-transaction-velocity-and-aml-risk-scoring.md): (1) In-memory sliding-window velocity guard di Redis (`evaluate_velocity.lua`: ZSET 10m/24h per-user + daily amount counter), (2) Integrasi `RiskEvaluationPort` di `transaction-service` memanggil `POST /api/v1/analytics/fraud/score` di `analytics-service` (< 30ms fast path), (3) 4-tier decision engine: `ALLOW` (< 40), `REQUIRE_STEP_UP` (40-70), `HOLD_FOR_REVIEW` (71-85, status `PENDING_COMPLIANCE_REVIEW` + event `payu.compliance.transaction-held.v1`), `BLOCK_REJECT` (> 85 / velocity breach 429), (4) Test suite: velocity burst limit (5 tx / 10m), daily amount threshold breach, dan transisi AML compliance hold. | Test velocity limit breach & hold review pass + AML decision matrix green |

### P2 — Defer (Out-of-Scope MVP, ADR-0023)

> ✅ **Seluruh backlog P2 aksi CLOSED 2026-08-12** (CB-008/011/017/022/024/025/031/036) — lihat CHANGELOG `1.10.63`. Tidak ada item tersisa.

### P3 — Backlog Lanjutan

| Key | Domain | Item |
|:---|:---|:---|
| READY-060 | card | Card tokenization + 3DS |
| READY-062 | ml | ONNX fraud detection model |

---

## 🏦 Partner Service Production Readiness Gate

Status `partner-service` hanya Production Ready setelah seluruh gate berikut memiliki bukti live. Manifest/unit test bukan bukti production. `PARTNER-001..006` CLOSED (2026-08-08). Progress per gate:

| Gate | Pri | Status | Sisa |
|:---|:---:|:---|:---|
| PARTNER-PROD-001 | P0 | 🟢 Public edge APIcast LIVE (sandbox): E2E luar cluster 200, quota 429, failover OK, bypass route dihapus ([ADR-0025](../adr/0025-snap-bi-and-partner-gateway-security-standard.md)) | WAF Coraza (DEPLOY-006), mTLS APIcast→gateway, rate-limit per-IP, runbook restart apicast |
| PARTNER-PROD-002 | P0 | 🟢 Enkripsi at-rest + rotation + backfill LIVE (V18, 0 plaintext) | Vault key management production |
| PARTNER-PROD-003 | P0 | 🟢 Webhook trust boundary LIVE (URL validator, SSRF block, DNS-rebind guard, 64KiB limit — [ADR-0025](../adr/0025-snap-bi-and-partner-gateway-security-standard.md)) | Egress policy eksplisit, response-body scan endpoint penerima |
| PARTNER-PROD-004 | P0 | 🟢 Delivery durability LIVE (retry 3× + DLQ + replay, `uq_webhook_delivery_event` — [ADR-0025](../adr/0025-snap-bi-and-partner-gateway-security-standard.md)) | DLQ consumer/alert otomatis, double-dispatch race window non-atomik |
| PARTNER-PROD-005 | P0 | 🟢 Reconciliation LIVE (`SnapBiReconciliationService` + V19 cases, 0 unmatched) | Reconcile outbox, auto-resolve workflow, alert destination |
| PARTNER-PROD-006 | P0 | 🟢 Tenant isolation LIVE (ownership semua resource, isolation matrix 295/295, audit) | PostgreSQL RLS, partner-scoped Keycloak roles, audit list query |
| PARTNER-PROD-007 | P1 | ⏸️ Belum | HPA≥3, PDB minAvailable 2, topology spread, bounded timeout |
| PARTNER-PROD-008 | P0 | ⏸️ Belum | PG HA+PITR via CNPG Barman Cloud in-cluster & AWS RDS Multi-AZ target ([ADR-0006](../adr/0006-postgresql-primary-database.md)), restore drill, RPO=0 / RTO<5m, retention/archive |
| PARTNER-PROD-009 | P1 | ⏸️ Belum | SLI/SLO, dashboard+alert, traces end-to-end |
| PARTNER-PROD-010 | P0 | ⏸️ Belum | Contract/conformance, k6 load/soak, chaos ([ADR-0024](../adr/0024-chaos-engineering-and-fault-injection-strategy.md): Litmus `pod-delete` + Microcks/WireMock 3.x di SIT, Istio fault injection di UAT, Kraken+Cerberus di preprod), pentest, partner sign-off |
| PARTNER-PROD-011 | P1 | ⏸️ Belum | Dual-control onboarding, SLA/escalation, runbook, on-call |

> Local APIcast (profile `api-management`) tidak bisa authless (verified via Context7) — public edge butuh APIManager (cluster-level).

---

## 🚀 Platform Deploy Queue

| Key | Pri | Category | Summary |
|:---|:---:|:---|:---|
| DEPLOY-006 | P1 | Security | Coraza WAF (INFRA-015) + Wazuh SIEM (INFRA-011) + sisa CIS `audit-log-forwarding-enabled` sink |
| DEPLOY-011 | P1 | Promotion | SIT/UAT/preprod LIVE di lab `cluster-nkk8q` (ArgoCD 18 apps, Vault HA, pipeline SIT green: sync-wait + k6 + ZAP + Schemathesis). Sisa: litmus gate (`pod-delete` only via [ADR-0024](../adr/0024-chaos-engineering-and-fault-injection-strategy.md)), preprod kraken gate (Cerberus guard via [ADR-0024](../adr/0024-chaos-engineering-and-fault-injection-strategy.md)), Infinispan Hot Rod mTLS (analytics 500), prod sync window + promotion via pipeline |
| INFRA-026 | P1 | Secrets | Vault HA live + restore drill verified. Sisa: snapshot S3 CronJob verify, kv readback via k8s auth, auto-unseal key backup |
| DEPLOY-009 | P2 | CI/CD | Tekton Results live (365d); sisa: external HA PostgreSQL, Chains SLSA/Rekor evidence, Renovate |
| DEVSECOPS-017 | P1 | Secrets | Tekton Buildah butuh `redhat-registry-pull` workspace + Vault `secret/payu/cicd/redhat-registry` (prerequisite eksternal — jangan placeholder) |
| OPS-2026-08-01-05 | P2 | Chaos | Kraken manifest fixed (emptyDir + SCC); re-run preprod gate saat CPU pulih (refer [ADR-0024](../adr/0024-chaos-engineering-and-fault-injection-strategy.md) for steady-state Cerberus gate) |
| OPS-2026-08-01-04 | P2 | Observability | Log delivery: vector connect OK; blocked 403 `lokistack-gateway.rego` kosong (operator bug LOG-2236 → RH support / tenant workaround). **2026-08-13**: recurring ERROR stack lokal dibersihkan — product-catalog cache `Optional` serialization (jackson-datatype-jdk8 di cache-starter), integration OJK timer DNS (disabled di container/local/dev); account IAM 401 business-rejection = legitimate |
| OPS-2026-04-08-02 | P2 | Performance | k6 via operator/port-forward only (gateway unreachable dari host) |
| READY-029 | P2 | Performance | Gatling defer ke cluster phase |
| READY-030 | P2 | Performance | SOAK 24h defer ke staging |
| INFRA-018 | P3 | Registry | Image hilang dari registry saat upgrade (31 tag) — investigasi prune + policy GC eksplisit |
| INFRA-019 | P3 | Registry | Quay.io auto-prune policy |
| DEVSECOPS-005 | P3 | Network | EgressNetworkPolicy + Istio egress gateway |
| DEVSECOPS-007 | P3 | Security | LUKS encryption PV + Vault DEK rotation |
| DEVSECOPS-012 | P3 | Cost | Monthly cost report workflow |

---

## 📋 Open Findings — Audit 2026-08-11 (ringkas, detail lengkap di source code)

> Verifikasi berbasis source code (bukan docs). Detail trace per fitur: `FEATURES.md`, `ASYNC_COMPONENTS.md`, PROGRESS.md.

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| NOTIF-001 | 🔴 | notification | LOG-mode false success tanpa delivery ID — **PARTIAL 2026-08-12** (fail-closed live, lihat PROD-044); arsitektur zero-cost provider & multi-channel fallback diadopsi di [ADR-0027](../adr/0027-notification-service-architecture-and-multi-channel-delivery.md) (ARCH-NOTIF-001) | SmsSender.java:26-54 |
| — | 🟢 | wallet | Reserve/commit flow solid; escrow & split-payment state machine solid | WalletService, EscrowTransaction |
| — | 🟢 | partner | Refund concurrency, callback HMAC, SNAP signature | SnapBiPaymentService, CallbackSignatureFilter |

## 📋 Open Findings — Audit Arsitektur 2026-08-13 (26 service vs AGENTS.md rules + ADR)

> Audit hexagonal/ArchUnit/money/idempotency/events/RFC 9457/DTO/container. Verifikasi berbasis source code.

### 🔴 Kritis (money/PII/event integrity)

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| ARCH-TOPIC-002 | 🟡 | platform | ~~Hanya 10 KafkaTopic deklaratif~~ **REMEDIATED 2026-08-18** — 107 KafkaTopic deklaratif (65 normal + 42 DLQ), v42 test-only dihapus, legacy resource removed, DLQ retention 30d, EVENT_CATALOG.md regenerated. Sisa: apply ke cluster + auto-create off (OCP creds blocker) | 01-kafka-topics-code.yaml |

### 🟠 Sistematis (lintas-service)

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| ARCH-DLQ-001 | 🟠 | promotion, cms, dispute, statement, platform | ~~Tanpa `.dlq` wiring; outbox event gagal-permanen cuma di-archive/log, tidak pernah ke `.dlq`~~ **PLATFORM DONE 2026-08-13** — outbox-starter: event gagal permanen (>maxRetries) kini di-copy best-effort ke `destinationTopic + .dlq` (`sendToDlq`, guard test); **DLQ HARDENED 2026-08-18** — 42 DLQ topics declared (tiered: financial-critical only), retention 30 hari (2592000000ms), kustomize render OK. **DELIBERATELY DEFERRED** — consumer per service menunggu alert destination (DEVSECOPS-017); `OutboxCleanupScheduler` log `OUTBOX-001 ALERT` sebagai safety net. Consumer DLQ = YAGNI sampai destination nyata tersedia. Sisa: `scripts/dlq-replay.sh` (P1) | OutboxCleanupScheduler.java:77-85 |
| ARCH-PARTNER-001 | 🟡 | partner | ~~PaymentWebhookHandler tidak di-wire~~ **CLOSED 2026-08-17** — `WebhookDispatcherService.dispatch` memanggil handler matching; API unversioned telah distandardisasi dengan dual mapping `/v1/...` di seluruh controller manajemen (`MerchantController`, `PartnerController`, `WebhookController`, `PaymentLinkController`, `CertificateController`, `ApiKeyController`, `PublicPaymentLinkController`). | WebhookDispatcherService, Partner Controllers |
| ARCH-CONS-001 | 🟡 | platform | ~~wallet `RefundRequestedConsumer` tanpa claim/dedup~~ **VERIFIED 2026-08-13** — dedup via natural key `refund_id` (PRIMARY KEY) + COMPLETED guard + reconcile; `RefundReversalExecutorTest` replay+invalid-event tests ditambah (3/3). Sisa: manual ack seragam lintas consumer | RefundRequestedConsumer + RefundReversalExecutor |
| ARCH-CDC-001 | 🟢 | platform | Tanpa Debezium; relay outbox = polling dispatcher `SKIP LOCKED` (legal pola). Note: evaluasi CDC bila throughput naik | OutboxPublisher.java:119-121 |
| ARCH-DEDUP-001 | 🟠 | partner, promotion | Migrasi dedup DELETE baris finansial pre-constraint (`snap_bi_payments`/`refunds`/`cashbacks`/`rewards`) — legal hanya jika belum pernah jalan di prod; perlu bukti env + policy | partner V16/V17; promotion V11/V12 |
| ARCH-FLYWAY-001 | 🟠 | account | Destruktif historis `DROP COLUMN` + `RENAME COLUMN` di migrasi ter-aplikasi — anti-pattern, risiko fresh-restore; jangan diulang | account V10:16-27 |

---

## 📋 Open Findings — Audit 2026-08-16 (Deep Quality, Business Invariants & Platform Audit)

> Verifikasi mendalam berbasis source code aktif (bukan docs/asumsi). Mencakup mathematical rounding, financial invariant, container file persistence, SNAP-BI compliance, memory leaks, dan test runners.

### 🔴 Kritis (Financial Invariants, Persistence & Compliance)

> Seluruh finding 🔴 audit 2026-08-16 **CLOSED 2026-08-16** (AI-AUTH-001, KYC-FACE-001, SDK-TS-001, SDK-JAVA-001) — lihat CHANGELOG `1.11.7`. Tidak ada item tersisa.

### 🟠 Sistematis (Performance, Data Integrity & Tooling)

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| MOBILE-JSX-001 | 🟠 | mobile | `useTransactionQuery.test.ts` dan `useWalletQuery.test.ts` menggunakan sintaks JSX `<QueryClientProvider>` di dalam file berekstensi `.ts` alih-alih `.tsx`. Babel parser gagal dengan syntax parse error | src/__tests__/hooks/useTransactionQuery.test.ts:84; useWalletQuery.test.ts:72 |

### 🟡 Minor & Simulator Gaps

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| MOBILE-MOCK-001 | 🟡 | mobile | Test `accessibility.test.tsx` me-mock `react-native` tanpa menyertakan stub `NativeSettingsManager` / `SettingsManager`, melanggar invariant TurboModule Registry di React Native 0.76 | frontend/mobile/src/testing/accessibility.test.tsx:58-63 |

---

## 📋 MVP Feature Readiness — Audit QA 2026-08-13 (PRD Phase 1 vs bukti test)

> PRD Phase 1 MVP: account opening + eKYC, transfer (internal/BI-FAST), bill payment, single pocket, virtual debit card, integrasi TokoBapak (SNAP-BI). Verdict: **belum MVP production ready** — bukti test layer tidak lengkap di jalur uang + CI tidak ada.

| Fitur MVP | Status | Blocker |
|:---|:---:|:---|
| Account opening + eKYC | 🔴 | ARCH-KYC-001 (NIK plaintext), ARCH-KYC-002 (event), INTEGRATION-CTX (integration broken), kyc 0 security test |
| Transfer internal + BI-FAST | 🟡 | 0 integration test transaction/wallet (TEST-GAP), ARCH-TXN-002, ARCH-IDM-001 |
| Bill payment | 🟡 | ARCH-BILL-001 (JMS), 0 contract test billing |
| Single pocket | 🟢 | Unit solid (LedgerInvariantTest), 0 integration test |
| Virtual debit card | 🟡 | Freeze/unfreeze ada; 0 integration; tokenization/3DS deferred (READY-060) |
| TokoBapak (SNAP-BI) | 🟠 | PARTNER-PROD-008 (PG HA/PITR) + 010 (contract/k6/pentest/sign-off) P0 belum |

### Bukti QA layer (2026-08-13)

| Layer | Status | Bukti |
|:---|:---:|:---|
| CI backend | 🔴 | Hanya 3 workflow: `analytics-tests`, `drift-detection`, `login-gate`. Tidak ada CI unit/integration backend di PR |
| Unit test core domain | 🟡 | account 26, transaction 27, wallet 25, billing 16, partner 35, lending 20, investment 12, kyc 18 file — ada; lending-rules 0 |
| Integration test (Testcontainers PG/Kafka) | 🔴 | transaction 0, wallet 0, kyc 0, billing 1, partner 2, account 2 (context broken), lending 4, investment 3 |
| Contract test (tests/contract) | 🔴 | Cuma 3: wallet getBalance, transaction createTransfer, auth loginUser. Billing/QRIS/SNAP-BI/CloudEvents tidak ada |
| E2E blackbox | 🟢 | 20 journey file (onboarding, transfer, bill payment, QRIS, dll) |
| Frontend | 🟢 | 91 test file + login gate live; tapi Vitest tidak dijalankan CI |
| Performance | 🟠 | k6 suite + baseline ada; tidak jalan di CI; load/soak deferred (READY-029/030); SLO belum (PARTNER-PROD-009) |
| PRD launch criteria | 🔴 | Belum: production deploy OCP (CB-006), app stores, legal ToS, security hardening |

### Gap per flow (FLOWS.md 47 flow; MVP 22 flow di-map ke bukti test — 2026-08-17)

| Flow | Status & Bukti Test | Sev |
|:---|:---|:---:|
| 16 Escrow (wallet) | **CLOSED 2026-08-17** — unit (8) + integration (2) + E2E `TestEscrowFlow` (4/4 endpoints) | 🟢 |
| 11 Split Bill (transaction) | **CLOSED 2026-08-17** — integration (real PG + outbox) + E2E `TestSplitBillFlow` | 🟢 |
| 7 Transfer Interbank BI-FAST | **CLOSED 2026-08-17** — integration (real PG + outbox) + E2E `TestBifastFlow` | 🟢 |
| 10 Disbursement | **CLOSED 2026-08-17** — integration (real PG, idempotency dedup) + E2E `TestDisbursementFlow` | 🟢 |
| 9 VA Payment | **CLOSED 2026-08-17** — domain invariant (`VirtualAccountPaymentInvariantTest`) + E2E `TestVirtualAccountPaymentFlow` | 🟢 |
| 22 Settlement Batch | **CLOSED 2026-08-17** — domain invariant (`SettlementBatchInvariantTest`) + E2E `TestSettlementBatchFlow` | 🟢 |
| 5 SNAP Refund | **CLOSED 2026-08-17** — domain invariant (`SnapRefundInvariantTest`) + E2E `TestSnapRefundFlow` | 🟢 |
| 14 Investment Jual | **CLOSED 2026-08-17** — domain invariant (`InvestmentSellInvariantTest`) + E2E `TestInvestmentSellFlow` | 🟢 |
| 19 Payment Link | **CLOSED 2026-08-17** — domain invariant (`PaymentLinkInvariantTest`) + E2E `TestPaymentLinkFlow` | 🟢 |
| 3 Transfer Internal / 8 QRIS / 12 Top-up | **CLOSED 2026-08-17** — domain invariants (`InternalTransferInvariantTest`, `QrisPaymentInvariantTest`, `WalletTopupInvariantTest`) + E2E `TestInternalTransferFlow`, `TestQrisPaymentFlow`, `TestWalletTopupFlow` | 🟢 |

### Bukti tambahan (2026-08-13)

| Temuan | Status | Bukti |
|:---|:---:|:---|
| Pentest report | 🟠 | `PENTEST_REPORT.md` SIGNED OFF **2025-01-22** — stale; mayoritas fitur live setelah tanggal itu; re-pentest = PARTNER-PROD-010 |
| E2E blackbox run evidence | 🟠 | 20 file ada; tidak ada hasil run otomatis di CI; run manual, bukti terakhir di PROGRESS.md (luar cluster via APIcast) |

### Deep test content audit (2026-08-13) — kedalaman isi, bukan jumlah file

| Key | Temuan | Sev |
|:---|:---|:---:|
| QAMVP-011 | Idempotency concurrency: **FIXED 2026-08-13** 4 service (wallet/transaction/billing/partner, 10 thread → 1 mutasi). Bonus: bug in-progress duplicate → 500 (ConflictException uncaught) di `IdempotencyInterceptor` → kini clean 409 | 🟢 |
| QAMVP-012 | Same-key + different-payload rejection: **FIXED 2026-08-13** semua 4 money service (409 conflict) | 🟢 |
| QAMVP-013 | Outbox atomicity: **FIXED 2026-08-13** — 4 service (wallet/transaction/billing/partner) commit+rollback vs real PG | 🟢 |
| QAMVP-014 | Security test: **FIXED 2026-08-13** — 7 service (wallet/billing/transaction/backoffice/cms/kyc/analytics); api-portal public-by-design | 🟢 |
| QAMVP-015 | Contract test: **FIXED 2026-08-13** — 3 error case RFC 9457 (transaction 400, wallet 404, auth 400) + CI workflow + README akurat; verifier jalan di CI via `-Pcontract-test` | 🟢 |
| QAMVP-016 | Coverage: jacoco `check` TER-BIND — account **GATE GREEN**; CI workflow account/backend/kyc ada. **kyc gate GREEN 2026-08-16** — 80.82% ≥ 80% (152 unit tests): fix `limiter.check()` → `@limiter.limit` shared limiter (semua KYC upload selalu return KYC_RAT_001 — bug produksi nyata), fix `ApiResponse.success()` → `create_success()` (regresi, semua success path 500), `KycServiceTest` 12 test baru, API test stale di-update (auth override + rate-limit reset). Sisa: Makefile `clean-test` hapus jacoco.exec (wajar) | 🟡 |
| QAMVP-017 | Pitest **ALIVE 2026-08-13** — 1.15.0 → 1.25.9 (1.15 gagal baca class Java 25, major 69) + junit5 plugin 1.2.3; `-Pmutation-testing org.pitest:pitest-maven:mutationCoverage` jalan (wallet domain: 627 mutasi, score 9%). Sisa: score < 60% threshold (butuh domain tests) — gate opt-in, tidak pecahkan CI | 🟠 |
| QAMVP-018 | ZAP + Schemathesis **CI WIRED 2026-08-13** — `.github/workflows/security-tests.yml` (ZAP baseline `zaproxy/action-baseline` + Schemathesis `--checks all` vs OpenAPI, workflow_dispatch + cron mingguan, URL env). Catatan: api-docs springdoc di-prod nonaktif (`SPRINGDOC_API_DOCS_ENABLED:false`) → target scan butuh docs enabled (dev/staging). Fix: `NoResourceFoundException` di-map 500→404 di `Rfc9457GlobalExceptionHandler` | 🟡 |
| QAMVP-019 | Frontend: statement page, forgot-password page + 3 test, not-found page + 2 test, E2E `forgot-password.spec.ts` + `not-found.spec.ts` HIJAU (2 passed) — 2026-08-13. a11y color-contrast/button-name di-test (49/49), refresh-token expiry di-test, full suite 94 files/1210 test green. Sisa minor: budget E2E spec, WCAG-strict tuning | 🟡 |

### Positif terverifikasi (isi test)

- kyc: `test_nik_crypto.py` (enc:v1 prefix, nonce unik), liveness threshold, OCR parse, outbox envelope CE.
- analytics: fraud score deterministik (==50.0), consumer replay dedup (`rowcount=0`), money Decimal 1 test.
- frontend: `currency.test.ts` 423 baris (format IDR, decimal-string arithmetic), axe-core real di 4 halaman + keyboard/SR, login E2E PKCE+CSRF+httpOnly cookie.

## 📋 Open Findings - Audit 2026-08-17 (Backend + Web App)

> Audit source-backed terhadap flow `docs/product/FLOWS.md`. Tidak ada production code atau test yang diubah. Selected unit/controller tests pass, tetapi full wallet suite membutuhkan Docker/Testcontainers.

| Key | Sev | Domain | Ringkasan | Bukti | Status |
|:---|:---:|:---|:---|:---|:---:|
| SEC-WALLET-001 | 🔴 | wallet | Customer bearer dapat memanggil `POST /wallets/{accountId}/credit` untuk menambah saldo sendiri tanpa inbound payment atau service identity. | `WalletController.java:246-276`; `WalletService.java:335-364` | ✅ FIXED |
| SEC-WALLET-002 | 🟠 | wallet/identity | Trusted-service bypass hanya memeriksa claim `azp`; client `payu-backend` juga mengaktifkan direct access grant, sehingga token user dari client tersebut dapat melewati ownership check. | `WalletController.java:80-96`; `payu-realm-export.json:257-273` | ✅ FIXED |
| PAY-LINK-001 | 🔴 | partner | Public payment-link confirm menerima `paymentMethod` dan `paymentReference` arbitrer, lalu menandai link PAID tanpa verifikasi settlement/provider. | `PublicPaymentLinkController.java:37-47`; `PaymentLinkService.java:145-171` | ✅ FIXED |
| PAY-SETTLE-001 | 🔴 | wallet | Settlement batch dapat menjadi COMPLETED hanya dengan perubahan status; tidak ada per-item merchant credit/journal seperti flow target. | `SettlementService.java:103-126,357-381`; FLOWS `782-792` | ✅ FIXED |
| TXN-TRANSFER-001 | 🟠 | transaction | Internal transfer sudah memindahkan uang, tetapi kegagalan membuat completed outbox masuk catch yang mengubah transaksi menjadi FAILED tanpa reversal. | `InitiateTransferCommandHandler.java:273-292`; `TransactionEventPublisherAdapter.java:114-121` | ✅ FIXED |
| SEC-AUTH-001 | 🔴 | auth | `DELETE /api/v1/auth/users/{userId}` hanya memerlukan bearer authenticated; tidak ada role atau service-account authorization untuk operasi IAM destruktif. | `AuthController.java:421-438`; `SecurityConfig.java:120-127` | ✅ FIXED |
| SEC-ACCOUNT-001 | 🔴 | account | Endpoint inter-service user profile/account IDs terbuka untuk bearer biasa dan mengembalikan email, phone, full name, serta NIK tanpa ownership/service boundary. | `UserAccountController.java:25-86`; `UserProfileResponse.java:13-36` | ✅ FIXED |
| SEC-NOTIF-002 | 🔴 | notification | List, detail, list-by-user, send, dan mark-read tidak memeriksa subject pemanggil; user dapat membaca atau mengubah notifikasi user lain. Response juga mengembalikan recipient/body mentah. | `NotificationResource.java:130-179,238-318,375-383`; `NotificationResponse.java:75-86` | ✅ FIXED |
| SEC-VA-001 | 🔴 | transaction | VA dibuat dengan `partnerId` dan `settlementAccountId` dari request tanpa ownership/partner authorization; callback kemudian mengkredit account tersebut. | `VirtualAccountController.java:44-68`; `VirtualAccountService.java:51-77,159-165` | ✅ FIXED |
| SEC-DISB-001 | 🟠 | transaction | GET disbursement by UUID atau idempotency key tidak melakukan ownership check dan response memuat source account, idempotency key, bank, account number, dan amount. | `DisbursementController.java:86-107`; `DisbursementResponse.java:65-81` | ✅ FIXED |
| PAY-DISB-001 | 🟠 | transaction | Disbursement melakukan remote wallet reserve sebelum `persistNew`; kegagalan insert dapat meninggalkan reservation orphan tanpa compensation. | `DisbursementService.java:101-117` | ✅ FIXED |
| SEC-STATEMENT-001 | 🟠 | statement | Partner statement menerima `customerId` arbitrer dan hanya mengecek role PARTNER/ADMIN, sehingga partner dapat meminta statement customer lain. | `PartnerStatementController.java:46-74` | ✅ FIXED |
| SEC-KYC-001 | 🟠 | account/kyc | Cache Dukcapil memakai NIK saja, padahal hasil verifikasi juga bergantung pada full name dan data matching lain; response verified dapat terbawa ke nama berbeda. | `KycVerificationAdapter.java:30-55`; `VerifyNikRequest.java:10-21` | ✅ FIXED |
| SEC-PROMO-001 | 🟠 | promotion | Promo apply mempercayai `request.userId` dari client dan menyimpan usage/reward atas ID tersebut tanpa subject binding. | `PromoRedemptionController.java:63-79`; `PromoRedemptionService.java:90-99` | ✅ FIXED |
| SEC-REFERRAL-001 | 🟠 | promotion | Create/complete referral tidak memiliki `@PreAuthorize` dan menyimpan referrer/referee IDs dari request, sehingga reward dapat diarahkan ke account arbitrer. | `ReferralResource.java:22-75`; `ReferralService.java:52-68,96-99` | ✅ FIXED |
| PROMO-REPLAY-001 | 🟠 | promotion | `transactionId` claim boleh null dan dedup index mengecualikan null; claim promo yang sama dapat membuat reward berulang. | `ClaimPromotionRequest.java:5-11`; `PromotionService.java:152-185`; `V12__dedup_reward_claim_and_loyalty_redeem.sql:15-17` | ✅ FIXED |
| SNAP-IDM-001 | 🟠 | partner | `partnerReferenceNo` tidak divalidasi wajib dan kolom nullable; natural-key dedup gagal sehingga request tanpa reference dapat membuat payment baru berulang. | `PaymentRequest.java:6-23`; `SnapBiPaymentService.java:85-123`; `SnapBiPaymentEntity.java:33-34` | ✅ FIXED |
| SNAP-TIME-001 | 🟡 | partner | Validasi timestamp memakai `Duration.toMinutes() <= 5`; request berumur 359 detik masih diterima walau window ditetapkan 300 detik. | `SnapBiController.java:64-72` | ✅ FIXED |
| FX-IDOR-001 | 🟠 | fx | GET conversion by UUID tidak memeriksa account ownership, berbeda dari endpoint reverse yang sudah melakukan check. | `FxController.java:179-190,230-239`; `FxConversionService.java:101-109` | ✅ FIXED |
| FX-IDM-001 | 🟠 | fx | `@Retry` membungkus seluruh conversion termasuk debit/credit wallet, sementara conversion tidak memiliki durable idempotency key; timeout setelah debit dapat mengulang mutation. | `FxConversionService.java:45-88`; `FxConversion.java:9-19` | ✅ FIXED |
| STATEMENT-PDF-001 | 🟡 | statement | PDF selalu dibuat satu halaman; renderer berhenti saat ruang habis dan hanya menulis pesan continued, sehingga transaksi tidak tampil walau totals/count mencakup semuanya. | `StatementService.java:391-441,572-617` | ✅ FIXED |
| SPLITBILL-SEC-001 | 🟠 | transaction | Create split bill menyimpan `creatorAccountId` dari request tanpa membandingkan JWT account. | `SplitBillController.java:58-68`; `SplitBillService.java:42-63` | ✅ FIXED |
| PAY-LINK-002 | 🟡 | partner | Expiry scheduler mengubah row ke EXPIRED tetapi mengirim entity stale; webhook dapat melaporkan status ACTIVE. | `PaymentLinkService.java:199-206,255-265` | ✅ FIXED |
| API-CONTRACT-001 | 🟡 | account/web | FLOWS mendokumentasikan register sebagai 201, tetapi controller mengembalikan 200; regression test juga mengunci 200. | `OnboardingController.java:47-64`; FLOWS `20,36` | ✅ FIXED |
| WEB-BILL-001 | 🔴 | web/billing | Bills page memanggil `/api/v1/billing/payments`, sedangkan gateway/backend mendaftarkan `/api/v1/payments`; mutation juga tidak mengirim `X-Idempotency-Key`. | `frontend/web-app/src/app/[locale]/bills/page.tsx:38-50`; `RouteRegistry.java:144-147` | ✅ FIXED |
| WEB-TRANSFER-001 | 🔴 | web/transaction | Manual recipient hanya mengisi `toAccountId`; schema mewajibkan `fromAccountId`, yang hanya diisi saat memilih favorite contact, sehingga confirm gagal validasi tanpa error source yang terlihat. | `transfer/page.tsx:86-117,181-197,519-529`; `types/index.ts:87-100` | ✅ FIXED |
| WEB-QRIS-001 | 🔴 | web/qris | QRIS page memiliki scanner state tetapi tidak pernah mengubahnya; tombol kamera/upload/personal QR/history tidak memiliki handler atau API mutation. | `frontend/web-app/src/app/[locale]/qris/page.tsx:7-10,50-78,120-145`; E2E `qris-flow.spec.ts:123-149` hanya memeriksa tombol tetap terlihat | ✅ FIXED |
| WEB-KYC-001 | 🔴 | web/kyc | KTP hanya disimpan dalam state dan tidak dikirim ke registration/KYC API; upload base64 KYC juga melebihi BFF limit 1 MiB untuk ukuran yang diizinkan service. | `onboarding/page.tsx:40-57,157-204`; `KYCService.ts:91-114`; BFF `route.ts:5-68` | ✅ FIXED |
| WEB-IDM-001 | 🔴 | web/financial | Axios me-retry semua 429 termasuk POST financial, key helper membuat key baru tiap invocation, dan beberapa mutation FX/scheduled/split tidak mengirim header idempotency. | `lib/api.ts:150-177`; `lib/utils.ts:25-32`; `FxService.ts:150-174`; `TransactionService.ts:109-153` | ✅ FIXED |
| WEB-AUTH-001 | 🟠 | web/account | Settings mengirim PUT ke `/accounts/users/{id}` sementara account controller hanya memiliki GET; jika response sukses tersedia, `useUpdateUser` juga mengganti `accountId` dengan `user.id`. | `UserService.ts:28-35`; `UserAccountController.java:45-86`; `useUser.ts:25-36`; `settings/page.tsx:61-67` | ✅ FIXED |
| WEB-LOG-001 | 🟠 | web/security | Registration dan financial handlers menulis Axios error object penuh ke browser console; error config dapat membawa password, NIK, PII, atau payload finansial. | `onboarding/page.tsx:52-64`; `useTransactions.ts:39-72`; `bills/page.tsx:47-60` | ✅ FIXED |
| WEB-INVEST-001 | 🟠 | web/investment | Investment page hanya menampilkan balance dan placeholder unavailable; tidak ada aksi buy/sell walau flow I1-I5 tersedia di catalog. | `investments/page.tsx:9-72`; `FEATURES.md:83-87` | ✅ FIXED |
| WEB-LEND-001 | 🟠 | web/lending | Tombol aktivasi PayLater, apply loan, dan bayar tidak memiliki handler; transaction ID juga dipaksa ke `number`, merusak UUID/string ID. | `lending/page.tsx:89-95,167-201,254-258`; `lending/page.tsx:56-62` | ✅ FIXED |
| WEB-STATEMENT-002 | 🟡 | web/statement | Selector monthly/quarterly/annual tidak dikirim ke request; load more selalu memanggil page 0, sehingga period dan pagination UI tidak sesuai hasil. | `statement-downloader.tsx:31-35,64-76,414-424`; `StatementService.ts:33-40,115-119` | ✅ FIXED |
| WEB-NOTIF-001 | 🟡 | web/notification | Mark-all/delete-all/item delete tidak punya handler; detail diarahkan ke route yang tidak ada dan filter UI memakai PROMO/SECURITY sementara backend mengirim channel PUSH/EMAIL/SMS/IN_APP. | `notifications/page.tsx:73-79,97-109,158-176`; `NotificationService.ts:7-18,32-33` | ✅ FIXED |
| WEB-MONEY-001 | 🟡 | web/transaction | Edit scheduled transfer memakai `number` dan `parseInt`, sehingga nilai decimal seperti `1000.50` terkirim sebagai `1000`. | `scheduled-transfers/page.tsx:68-84,358-366`; `TransactionService.ts:194-234` | ✅ FIXED |
| WEB-WALLET-001 | 🟡 | web/wallet | Saving goals dan shared pockets dirender dari data hard-coded termasuk balance dan member names; reserve progress juga hard-coded 15%. | `pockets/page.tsx:211-266,338-345` | ✅ FIXED |
| WEB-TXN-001 | 🟡 | web/transaction | Ringkasan transaksi menganggap hanya TOP_UP sebagai credit; internal transfer masuk akan dihitung sebagai Total Keluar. | `transactions/page.tsx:61-89`; `types/index.ts:128-143` | ✅ FIXED |
| WEB-QA-001 | 🟡 | web/qa | Playwright E2E tidak dapat memberi evidence karena config memaksa Chrome system channel yang tidak tersedia; run QRIS gagal sebelum aplikasi dijalankan. | `playwright.config.ts:36-45`; run `npm run test:e2e -- e2e/qris-flow.spec.ts --project=chromium` | ✅ FIXED |
| WEB-DEP-001 | 🟠 | web/security | `npm audit --omit=dev --audit-level=high` menemukan high vulnerability pada `nanoid <3.3.18` di lockfile; upgrade harus diverifikasi dengan build/test. | `frontend/web-app/package-lock.json`; audit run 2026-08-17 | ✅ FIXED |

---

## 📋 Open Findings - Audit 2026-08-18 (Web App <-> Gateway <-> Backend Cross-Layer vs FLOWS/FEATURES)

> Audit source-backed terhadap integrasi end-to-end: `frontend/web-app` (BFF & services), `backend/gateway-service` (`RouteRegistry` & `application.yaml`), dan backend microservices (`account`, `billing`, `kyc`, `compliance`, `partner`, `promotion`, `transaction`, `lending`, `investment`), diverifikasi terhadap `docs/product/FLOWS.md` & `docs/product/FEATURES.md`.

| Key | Sev | Domain | Ringkasan | Bukti | Status |
|:---|:---:|:---|:---|:---|:---:|
| GW-ROUTING-001 | 🔴 | gateway/kyc | Route `kyc` tidak terdaftar di `gateway.routes` (`application.yaml`) maupun `RouteRegistry.java`; seluruh request eKYC dari web-app (`KYCService.ts` / `/api/v1/kyc/*`) menerima 404 No Route Found dari Gateway. | `application.yaml:458-651`; `RouteRegistry.java:123-200`; `KYCService.ts:85-127` | ✅ FIXED 1.13.0 |
| GW-ROUTING-002 | 🔴 | gateway/compliance | Route `gdpr-audit` tidak ada di `gateway.routes` dan `RouteRegistry.java`, serta `/api/v1/gdpr-audit` tidak ada di BFF `ALLOWED_PATH_PREFIXES`; seluruh endpoint GDPR audit di `ComplianceService.ts` di-block 400 di BFF dan 404 di Gateway. | `ComplianceService.ts:137-202`; `route.ts:89-135`; `application.yaml:458-651`; `GdprAuditController.java:35` | ✅ FIXED 1.13.0 |
| BFF-ROUTING-001 | 🟠 | bff/partner | `PartnerService.ts` memanggil `/partner/payments` (singular), namun BFF whitelist hanya mengizinkan `/api/v1/partners` dan `/v1/partner`, serta Gateway hanya me-route `partners` dan `v1/partner`; request SNAP-BI payment via web-app langsung di-reject 400 Bad Request di BFF. | `PartnerService.ts:150-166`; `route.ts:89-135`; `application.yaml:585-603` | ✅ FIXED 1.13.0 |
| BE-ACC-001 | 🔴 | account | `account-service` `UserAccountController.java` hanya mengimplementasikan `GET /{userId}` dan `GET /{userId}/account-ids`; tidak ada endpoint `PUT /api/v1/accounts/users/{userId}` sehingga fungsi update profile di `UserService.ts` / `settings/page.tsx` menghasilkan 405 Method Not Allowed. | `UserAccountController.java:81-127`; `UserService.ts:28-36`; `useUser.ts:25-36` | ✅ FIXED 1.13.0 |
| BE-BILL-001 | 🔴 | billing | `PaymentController.java` di `billing-service` me-map `GET /api/v1/payments` ke `getPaymentStatus()` yang mengembalikan static health map (`operational`), bukan list transaksi billing terpaginasi seperti yang diharapkan `BillingService.ts` dan `bills/page.tsx`. | `PaymentController.java:72-87`; `BillingService.ts:74-88`; `bills/page.tsx:28-34` | ✅ FIXED 1.13.0 |
| BE-PARTNER-001 | 🟠 | partner | `merchant/page.tsx` mem-parse Keycloak UUID string dengan `Number(user.id)` yang menghasilkan `NaN`, dan `partner-service` (`PartnerController.java`) hanya mendukung ID numerik `Long` tanpa endpoint lookup by Keycloak User ID (`/partners/by-user/{userId}` atau `/partners/me`), membuat halaman merchant dashboard tidak bisa memuat profil partner. | `merchant/page.tsx:21-38`; `PartnerController.java:98-105`; `PartnerService.ts:58-61` | 🔴 OPEN |
| BE-PROMO-001 | 🟠 | promotion | `promotion-service` memiliki entity database dan migrasi Flyway untuk `CustomerSegmentEntity`, namun tidak memiliki REST controller / `@Path` untuk `/api/v1/segments` yang dipanggil oleh `SegmentationService.ts`. | `SegmentationService.ts:78-135`; `promotion-service/src/main/java/.../adapter/web/` | ✅ FIXED 1.13.0 |
| FE-MONEY-002 | 🟠 | web/financial | `TransactionService.ts` (`makeParticipantPayment`, `CreateScheduledTransferRequest`, `ScheduledTransfer`) dan `StatementService.ts` (`openingBalance`, `closingBalance`, `totalCredits`, `totalDebits`) menggunakan tipe data `number` alih-alih `Money` (string decimal), melanggar aturan non-negotiable money precision. | `TransactionService.ts:60-68,110-120,200-210`; `StatementService.ts:12-31` | ✅ FIXED 1.13.0 |
| FE-IDM-002 | 🟠 | web/financial | Berbagai fungsi mutasi pada `TransactionService.ts` (`makeParticipantPayment`, `updateScheduledTransfer`, `cancelScheduledTransfer`, `pauseScheduledTransfer`, `resumeScheduledTransfer`, `createSplitBill`, `updateSplitBill`, `cancelSplitBill`, `activateSplitBill`) meng-instantiate UUID baru secara langsung di dalam method daripada menerima idempotency key dari pemanggil/state manager untuk mendukung safe retry. | `TransactionService.ts:114,131,147,163,222,238,254,270,286` | ✅ FIXED 1.13.0 |
| FE-STUB-001 | 🟠 | web/investment-lending | `investments/page.tsx` (fitur I1-I5 katalog) dan `lending/page.tsx` (fitur L1-L7: apply loan & pay bill) hanya menjalankan toast notifikasi kosmetik (`toast.info`/`toast.success`) tanpa memanggil hook/mutation ke backend service. | `investments/page.tsx:60-70`; `lending/page.tsx:93,203,261` | 🔴 OPEN |
| FE-ONBOARD-001 | 🟠 | web/kyc | `onboarding/page.tsx` mewajibkan upload foto KTP di Step 1 (`disabled={!ktpFile}`), tetapi saat submit Step 2 ke `POST /accounts/register`, file KTP diabaikan dan tidak dikirim ke `account-service` maupun di-upload ke `kyc-service` (Flow #28 terputus di UI). | `onboarding/page.tsx:40-66,157-215`; FLOWS `835-860` | 🔴 OPEN |
| FE-SEC-001 | 🟡 | web/security | `security/page.tsx` mengirimkan string kosong untuk `challengeId` dan `credential` ke `registerBiometric.mutate`, menyebabkan kegagalan validasi WebAuthn di backend `biometric-service`. | `security/page.tsx:23-33`; `BiometricService.ts:50-65` | 🔴 OPEN |
| BE-CARD-001 | 🔴 | wallet | `CardController.java` di `wallet-service` hanya mendukung create, list, detail, freeze, dan unfreeze; endpoint `PUT /api/v1/cards/{cardId}` (update daily limit) dan `DELETE /api/v1/cards/{cardId}` (hapus kartu) sama sekali belum diimplementasikan di backend padahal UI `cards/page.tsx` dan `WalletService.ts` menyediakannya. | `CardController.java:60-194`; `WalletService.ts:121-131`; `cards/page.tsx:350-400` | ✅ FIXED 1.13.0 |
| FE-SPLIT-001 | 🔴 | web/transaction | `split-bill/page.tsx` `handleCreate()` mengirim `participants: []` kosong; backend `CreateSplitBillRequest.java` mewajibkan `@NotEmpty List<ParticipantRequest> participants`, menyebabkan create split bill dari UI modal selalu gagal validasi 400 Bad Request. | `split-bill/page.tsx:79-99`; `CreateSplitBillRequest.java:36-37` | ✅ FIXED 1.13.0 |
| FE-MONEY-003 | 🟠 | web/financial | `scheduled-transfers/page.tsx` (`parseFloat` di editForm) dan `cards/page.tsx` (`parseInt` di limitForm daily/monthly limit) serta `WalletService.ts` (`VirtualCard.dailyLimit: number`) mem-parse dan mentransmisikan nilai finansial sebagai tipe `number`, melanggar aturan money precision `Money` (string decimal). | `scheduled-transfers/page.tsx:365`; `cards/page.tsx:364,374`; `WalletService.ts:206,214,218` | ✅ FIXED 1.13.0 |
| BE-BILL-002 | 🟠 | billing | Di `PaymentController.java` (`billing-service`), method `extractIdempotencyKey()` melakukan fallback ke `UUID.randomUUID().toString()` saat header `X-Idempotency-Key` tidak ada, alih-alih me-reject request, sehingga menyembunyikan pelanggaran kontrak idempotensi pemanggil non-compliant. | `PaymentController.java:118-129` | ✅ FIXED 1.13.0 |
| FE-IDM-003 | 🟠 | web/billing | `bills/page.tsx` men-generate `X-Idempotency-Key: crypto.randomUUID()` dan `referenceNumber: REF-${Date.now()}` baru secara acak pada setiap klik `handlePay()`, menggagalkan fungsi deduplikasi dan safe-retry idempotensi jika request mengalami timeout/retry. | `bills/page.tsx:47-80` | ✅ FIXED 1.13.0 |
| SEC-AUTH-001 | 🔴 | security/rbac | `KeycloakJwtAuthoritiesConverter.java` mengonversi role Keycloak menjadi authority berprefix `ROLE_` (`ROLE_admin`, `ROLE_backoffice`, `ROLE_cms_editor`, dll). Sementara itu puluhan controller di `backoffice-service`, `cms-service`, dan `integration-service` menggunakan `@PreAuthorize("hasAnyAuthority('admin', 'backoffice')")` tanpa prefix `ROLE_`, menyebabkan seluruh request admin dan operator ditolak 403 Forbidden. | `KeycloakJwtAuthoritiesConverter.java:87,127,134`; `BackofficeController.java:83,136,176`; `ContentController.java:42,68,193`; `IntegrationController.java:69,95,121` | ✅ FIXED 1.13.0 |
| GW-ROUTING-003 / BE-BIO-001 | 🔴 | gateway/biometric | `AuthService.ts` dan `security/page.tsx` memanggil 5 endpoint WebAuthn biometric (`/api/v1/biometric/*`), namun tidak ada microservice backend yang mengimplementasikan controller untuk `/api/v1/biometric/*` dan Gateway tidak memiliki route untuk `biometric`, menyebabkan seluruh fitur autentikasi biometrik 404 No Route Found. | `AuthService.ts:152-187`; `application.yaml:458-651`; `RouteRegistry.java:123-200` | 🔴 OPEN |
| BE-INVEST-001 | 🔴 | investment | `InvestmentController.java` menyimpan `account.userId` dari claim `account_id` saat `createAccount`, namun anotasi `@PreAuthorize` pada `buyDeposit`, `buyMutualFund`, dan `sellInvestment` memeriksa `@investmentSecurityService.isAccountOwner(#request.accountId(), authentication.principal.subject)` (yang merupakan `sub` Keycloak, bukan `account_id`), menghasilkan perbandingan mismatch dan 403 Forbidden pada akun milik user sendiri. | `InvestmentController.java:73,80,98,105,154`; `InvestmentSecurityService.java:28-41` | ✅ FIXED 1.13.0 |
| FE-LEND-001 | 🔴 | lending | `backend/lending-service` `LoanPreApprovalRequest.java` mewajibkan `@NotNull LoanType loanType`, `@NotNull BigDecimal principalAmount`, dan `@NotNull Integer tenureMonths`. `LendingService.ts` (`PreApprovalCheckRequest`) hanya mengirim `requestedAmount` (bukan `principalAmount`) dan tidak menyertakan `loanType` maupun `tenureMonths`, menyebabkan seluruh request pre-approval pinjaman selalu gagal validasi 400 Bad Request. | `LoanPreApprovalRequest.java:15-32`; `LendingService.ts:214-220`; `LendingController.java:388` | ✅ FIXED 1.13.0 |
| BE-PROMO-002 | 🟠 | promotion | `promotion-service` telah menghapus tabel dan controller gamifikasi pada migrasi `V5__drop_gamification_tables.sql` (`SIMP-002`), namun `PromotionService.ts` dan `useGamification.ts` masih menyediakan 8 fungsi dan hook aktif ke `/api/v1/gamification/*` yang tidak lagi dilayani oleh backend. | `V5__drop_gamification_tables.sql:1-8`; `PromotionService.ts:258-305`; `useGamification.ts:1-92` | ✅ FIXED 1.13.0 |
| BE-SUPP-001 / FE-STUB-002 | 🟠 | support | `support-service` di backend hanya mengimplementasikan manajemen pelatihan internal support agent (`SupportController.java`), tanpa menyediakan API tiket bantuan pelanggan (`/tickets`) maupun FAQ publik, dan UI `support/page.tsx` hanya berupa tombol statis tanpa modal atau action handler. | `SupportController.java:25-100`; `SupportService.ts:240-256`; `support/page.tsx:56-58` | 🔴 OPEN |
| FE-STUB-003 | 🟠 | qris | `qris/page.tsx` merupakan simulasi interaktif semata (`setTimeout` + `toast.success`) tanpa decoding payload QRIS EMVCo, tanpa query personal QR/limit harian dinamis, dan tanpa eksekusi mutasi SNAP-BI QRIS ke `partner-service`. | `qris/page.tsx:25-34,109-124,185-195` | 🔴 OPEN |
| FE-STUB-004 | 🟠 | auth | Halaman `forgot-password/forgot-password-form.tsx` hanya menampilkan toast informasi kosmetik `Fitur reset password akan segera hadir` tanpa menghubungkan ke alur reset password Keycloak/auth-service. | `forgot-password-form.tsx:22` | 🔴 OPEN |

---

## 📋 Open Findings - Audit 2026-08-18 (DX Engineering, Tooling, Specs Drift & Type Safety)

> Audit standar Developer Experience (`dx-engineer`), Backstage Software Catalog, Git hooks & CI quality gates, CodeGraph & Context7 verification, RTK rules, serta sinkronisasi spesifikasi dokumen (`docs/architecture/ARCHITECTURE.md`, `docs/architecture/SERVICE_CATALOG.md`, `catalog-info.yaml`, `mkdocs.yml`).

| Key | Sev | Domain | Ringkasan | Bukti | Status |
|:---|:---:|:---|:---|:---|:---:|
| DX-HOOKS-001 | 🔴 | dx/git-hygiene | Root repository tidak memiliki konfigurasi Husky v9 (`.husky/`), root `package.json` / `prepare` script, maupun commitlint/lint-staged di level root repository. Git hooks hanya terisolasi di `frontend/mobile/.husky` yang tidak aktif pada level git root, sehingga commit dan push untuk backend, web-app, infra, dan docs tidak pernah melewati pre-commit type/lint check atau Conventional Commits gate. | `.husky/` absen di root; `frontend/mobile/.husky/commit-msg` memakai grep regex custom alih-alih `commitlint --edit $1` | 🔴 OPEN |
| DX-CI-FE-001 | 🔴 | ci/web-app | Pipeline CI GitHub Actions tidak memiliki workflow otomatis untuk lint, type-check (`tsc --noEmit`), dan Vitest unit/component tests untuk `frontend/web-app`. Hanya ada `login-gate.yml` (Playwright E2E login); 94 file test (1200+ unit/component tests) web-app tidak pernah di-gate secara otomatis pada pull request. | `.github/workflows/` (absen `frontend-tests.yml` / `web-app-ci.yml`); `frontend/web-app/package.json:11` | ✅ FIXED 1.13.0 |
| DX-CI-COMMITS-001 | 🟠 | ci/governance | Tidak ada CI PR gate untuk validasi Conventional Commits (`commitlint-action` / action PR title lint) pada repository GitHub Actions, memungkinkan merge non-compliant commit headers yang merusak automated semantic versioning dan release changelog generation. | `.github/workflows/` tidak memiliki workflow commitlint/pr-title | ✅ FIXED 1.13.0 |
| DX-CATALOG-001 | 🟠 | dx/backstage | Inskripsi `catalog-info.yaml` mengalami drift arsitektural terhadap codebase aktual: (a) Mendaftarkan ghost component `ab-testing-service` yang tidak ada di `backend/`; (b) Melewatkan 3 backend microservices (`dispute-service`, `product-catalog-service`, `integration-service`), 2 lending components (`loan-origination-process`, `lending-rules`), dan 5 simulators (`backend/simulators/*`); (c) Melewatkan 14 dari 17 shared starters; (d) `backstage.io/techdocs-ref: dir:backend/<service>` gagal build di Backstage/RHDH karena service tidak memiliki `mkdocs.yml` lokal. | `catalog-info.yaml:369`; `backend/dispute-service`, `product-catalog-service`, `integration-service`; `mkdocs.yml:5` | ✅ FIXED 1.13.0 (ghost dihapus; 5 service + 5 simulator + 14 starter ditambahkan; 50 komponen tanpa duplikat; (d) mkdocs tetap open — gap Terpisah) |
| DX-TS-BRANDED-001 | 🟠 | web/types | Model tipe data pada `frontend/web-app/src/types/index.ts` dan service clients menggunakan plain `string` untuk seluruh domain identifier (`AccountId`, `UserId`, `TransactionId`, `PocketId`, `Money`), tanpa branded/nominal types (`type AccountId = string & { readonly __brand: unique symbol }`). Ketiadaan compile-time nominal type distinction telah berulang kali memicu runtime bug ID mismatch (seperti `user.id` menggantikan `accountId` pada BE-PARTNER-001 / BE-INVEST-001). | `frontend/web-app/src/types/index.ts:21-80` (`id: string`, `accountId: string`, `externalId: string`, `Money = string`) | 🔴 OPEN |
| DX-DOCS-DRIFT-001 | 🟡 | docs/architecture | Terdapat ketidaksesuaian versi stack dan inventaris antara dokumen arsitektur dan source code: (a) `SERVICE_CATALOG.md` mendokumentasikan `Java 21, Spring Boot 3.4` dan hanya 4 simulator, sementara `backend/pom.xml` menggunakan `Java 25, Spring Boot 4.1.0` dan `backend/simulators/` memiliki 5 simulator (`biller-simulator` hilang di katalog); (b) `catalog-info.yaml` line 45 mencantumkan `Spring Boot 3.4`; (c) `ARCHITECTURE.md` service specs mencantumkan `Java 21` alih-alih `Java 25`. | `SERVICE_CATALOG.md:27,45,63,81`; `backend/pom.xml:10,23`; `catalog-info.yaml:45`; `ARCHITECTURE.md:334,380,398` | ✅ FIXED 1.13.0 |
| DX-CODEGRAPH-001 | 🟡 | dx/codegraph | Repositori memiliki database CodeGraph `.codegraph/codegraph.db` (4036 files terindeks), namun tidak ada helper script atau Makefile target (`make codegraph-refresh` / `scripts/refresh-codegraph.sh`) untuk me-reindex atau memvalidasi sinkronisasi index setelah perombakan arsitektur besar. | `.codegraph/` ada tetapi tidak ada script automasi refresh di `scripts/` atau `Makefile` | ✅ FIXED 1.13.0 |
| DX-RTK-ENV-001 | 🟡 | dx/rtk | Rule token killer `.agents/rules/antigravity-rtk-rules.md` mewajibkan prefix `rtk` untuk command shell, namun CLI binary `rtk` belum terpasang di sistem PATH linux environment aktif (fallback raw bash). Perlu script helper setup atau fallback wrapper agar perintah shell agent tetap konsisten dan kompatibel. | `which rtk` exit non-zero; `.agents/rules/antigravity-rtk-rules.md:1-33` | ✅ FIXED 1.13.0 (rtk 0.45.0 terpasang di `~/.local/bin/rtk`, `which rtk` kini exit 0) |
| DX-CONTEXT7-001 | 🟡 | dx/context7 | Workflow Context7 verification telah diwajibkan di `AGENTS.md` dan `.agents/skills/dx-engineer/SKILL.md`, namun belum ada standard npm/mvn hook atau developer guide di `docs/guides/` yang memandu developer manusia saat upgrade library pihak ketiga (misal: Jackson, TanStack Query, Next.js, Zod, Spring Cloud). | `docs/guides/` belum memiliki panduan ringkas Context7 workflow untuk developer internal | ✅ FIXED 1.13.0 |
| LEND-SCHED-001 | 🔴 | lending | `LoanManagementService.java` memakai `@Scheduled` dan `@SchedulerLock`, namun `LendingServiceApplication.java` tidak memiliki `@EnableScheduling` / `@EnableSchedulerLock` dan migrasi Flyway belum memiliki tabel `shedlock` (`V11__add_shedlock_table.sql` absen); rekonsiliasi repayment pinjaman tidak pernah jalan. | `LendingServiceApplication.java:8-17`; `LoanManagementService.java:197`; `db/migration/` | ✅ FIXED 1.13.0 |
| GW-ROUTING-004 | 🔴 | gateway/dispute | Route prefix `/api/v1/disputes` absen di `gateway.routes` (`application.yaml`) dan tidak terdaftar di fallback `RouteRegistry.java`; seluruh endpoint refund dispute / chargeback menerima 404 No Route Found dari Gateway. | `application.yaml:458-651`; `RouteRegistry.java:122-200` | ✅ FIXED 1.13.0 |
| BFF-ROUTING-002 | 🟠 | bff/security | Di `route.ts`, validasi `sanitizeBackendPath` mencocokkan `fullPath = '/api/v1/' + backendPath` dengan `ALLOWED_PATH_PREFIXES` yang memuat `'/v1/partner'` (tanpa `/api`); path `/api/v1/partner/*` (singular) selalu ditolak 400 Bad Request oleh SSRF whitelist. | `frontend/web-app/src/app/api/v1/[...path]/route.ts:133,188-195` | ✅ FIXED 1.13.0 |
| GW-CONCUR-001 | 🟠 | gateway/concurrency | Quarkus `gateway-service` mengeksekusi multiple scheduled task (`ApiKeyRotationService.java:119`, `PersistentAnalyticsService.java:123,162,183`, `CheckoutService.java:29`) tanpa concurrency lock; pada multi-instance pod terjadi eksekusi rotasi API key dan flushing analytics ganda. | `ApiKeyRotationService.java:119`; `PersistentAnalyticsService.java:123,162`; `CheckoutService.java:29` | 🔴 OPEN |
| FE-PROXY-AUTH-001 | 🟠 | web/auth | Middleware Next.js `src/proxy.ts` memanggil `/api/v1/auth/validate` di setiap request route terproteksi; transient network timeout ke gateway menyebabkan request gagal dan me-redirect paksa active user ke `/login`, merusak form state. | `frontend/web-app/src/proxy.ts:54-70,83-86` | ✅ FIXED 1.13.0 |

---

## 🛡️ DEVSECOPS-017 — Production-Ready Architecture

Success criteria: setiap mandatory control di `architecture/DEVSECOPS_ARCHITECTURE.md` punya repository tests + bukti live cluster.

- [ ] Vault-backed Argo CD credential via ESO (`payu-vault` ClusterSecretStore); revoke/rotate deploy key lama + Git-history purge MOP
- [ ] Pipelines-as-Code Repository/webhook (changed-service dispatch) dengan Vault Git credential
- [ ] RHTAS CNPG archive failure (`barman-cloud-wal-archive` exit 4) — 3-instance cluster readyInstances=3
- [ ] Chains SLSA/Rekor fresh evidence + signed-image admission Enforce (31 image)
- [ ] Promosi digest Buildah semua env + Results HA 365d
- [ ] Platform stores: prod Vault/KMS, LokiStack KMS/S3, Tekton Results HA PG
- [ ] Rightsize MachineSet `1a` 3→1 replica (setelah disruption-budget review)
- [ ] Drift alert destination nyata (Slack/PagerDuty) via Vault
- [ ] E2E security gates + DR/rollback exercise + reviewer audit + reconcile evidence docs
