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
| **Last Release** | `1.11.12` (2026-08-16) |
| **Core Banking MVP** | 🟡 Mendekati MVP — blocker tersisa: ACCOUNT-007 (P1) + PROD-044 (P1); **login web live** (LOGIN-001..006 closed: PKCE + gate CI + browser E2E), money-flow live (PROD-043/045/047, CB-014/016/020/021/023 closed). Belum ada service production ready. |
| **Backlog Aktif** | 2 tickets + action items (CB-*/PROD-*/READY-*/DEVSECOPS-*/ARCH-*/QAMVP-*) + gates partner/platform (2026-08-16) |
| **Last Updated** | 2026-08-16 — backlog dirapikan: hapus semua item CLOSED/DONE/FIXED/VERIFIED-STALE (14 finding Python AI/kyc/analytics + SDK + READY-022 4 service + ARCH-SECRET-001 + DEVSECOPS-015/016 + ARCH-TOPIC/CE/ADR17/PROJ) — detail di CHANGELOG `1.11.7..1.11.12` |

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
| PROD-044 | P1 | Notification false success — **PARTIAL 2026-08-12**: fail-closed live (SMS/PUSH default NONE → false, LOG hanya eksplisit, `mailer.mock` tidak diwariskan ke prod, `KEYCLOAK_REALM` default). Sisa (butuh credential provider eksternal): provider nyata + delivery ID + E2E terima. | 🟠 Fail-closed live — provider pending |
| INFRA-029 | P1 | Audit log forwarding: CLF live (CIS satisfied), sisa Wazuh SIEM sink (INFRA-011) + verifikasi log arrival. | 🟢 Live — sink pending |

---

## 🎯 Backlog Aksi (urut per priority)

### P1 — Quality & Reliability (In-Scope MVP)

| Key | Domain | Item | Done saat |
|:---|:---|:---|:---|
| CB-006 | platform | Prod deploy core banking: gates + HPA≥2 + PDB2 + DR drill (ACCOUNT-007) | ACCOUNT-007 closed |
| PROD-002 | fx | Approved FX provider URL/credential + live evidence | Rate live + audit pair |
| PROD-018 | analytics | Aktifkan `analytics-tests` sebagai required branch protection — workflow `.github/workflows/analytics-tests.yml` SUDAH ada (push/PR paths + workflow_dispatch); sisa = setting GitHub branch protection (butuh `gh`/admin repo, belum tersedia di sesi ini) | CI gate aktif via GitHub settings |
| ARCH-TOPIC-002 | platform | ~~KafkaTopic deklaratif untuk semua topic kode (RF 3, partisi sesuai consumer); hapus resource legacy `*-events`; audit auto-create off~~ **MANIFEST DONE 2026-08-13** — `01-kafka-topics-code.yaml` (75 KafkaTopic dari grep kode, RF 3 / partisi 3, kustomize render OK) + semua 14 KafkaTopic legacy (name tidak match topic nyata, `transaction-initiated` ≠ `payu.transaction.initiated.v1`) dihapus dari `kafka-amqstreams.yaml`. Sisa: apply ke cluster + audit auto-create off (butuh OCP creds) | `oc get kafkatopic` lengkap vs EVENT_CATALOG |
| QAMVP-004 | kyc | ~~Security test (auth/RBAC) + integration test kyc; provider OCR/liveness nyata gate (analog PROD-002)~~ **security DONE 2026-08-13** (QAMVP-014 `test_security.py` 401/403 IDOR); e2e workflow test ADA (`tests/e2e/test_kyc_workflow.py`, provider di-mock); CI `.github/workflows/kyc-tests.yml` (unit+e2e, cov gate 80%). Sisa: provider OCR/liveness nyata gate (butuh credential eksternal) | Test + live evidence |
| QAMVP-005 | platform | ~~k6 smoke+load di pipeline staging + SLO threshold per service~~ **CI WIRED 2026-08-13** — `.github/workflows/k6-tests.yml` (grafana/k6-action, smoke/load/stress via workflow_dispatch + cron 02:00, `GATEWAY_URL`/`KEYCLOAK_URL` env, SLO threshold `p95<500ms`/`p99<1s`/`avg<300ms`/`rate<0.01`, summary artifact). Verified terhadap local stack: gateway `/q/health` 200 + keycloak OIDC 200; token acquisition butuh credential client (TEST_USERS) — external. Sisa: green run dengan kredensial staging | Laporan k6 di CI |
| QAMVP-006 | platform | ~~PRD launch criteria tracker: prod deploy OCP, app stores, legal ToS, security hardening (lanjut CB-006)~~ **TRACKER DONE 2026-08-13** — `docs/roadmap/PRD_LAUNCH_CRITERIA.md`: 15 kriteria PRD §12.1 → evidence/status (7 🟢, 7 🟡, 2 🔴 + 1 ⏸️ deferred). CI/CD hardening evidence (7 workflow) tercatat. Sisa hijau penuh: prod deploy OCP (CB-006), app stores (deferred), legal ToS, re-pentest | Checklist PRD §12 hijau |

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
| PARTNER-PROD-001 | P0 | 🟢 Public edge APIcast LIVE (sandbox): E2E luar cluster 200, quota 429, failover OK, bypass route dihapus | WAF Coraza (DEPLOY-006), mTLS APIcast→gateway, rate-limit per-IP, runbook restart apicast |
| PARTNER-PROD-002 | P0 | 🟢 Enkripsi at-rest + rotation + backfill LIVE (V18, 0 plaintext) | Vault key management production |
| PARTNER-PROD-003 | P0 | 🟢 Webhook trust boundary LIVE (URL validator, SSRF block, DNS-rebind guard, 64KiB limit) | Egress policy eksplisit, response-body scan endpoint penerima |
| PARTNER-PROD-004 | P0 | 🟢 Delivery durability LIVE (retry 3× + DLQ + replay, `uq_webhook_delivery_event`) | DLQ consumer/alert otomatis, double-dispatch race window non-atomik |
| PARTNER-PROD-005 | P0 | 🟢 Reconciliation LIVE (`SnapBiReconciliationService` + V19 cases, 0 unmatched) | Reconcile outbox, auto-resolve workflow, alert destination |
| PARTNER-PROD-006 | P0 | 🟢 Tenant isolation LIVE (ownership semua resource, isolation matrix 295/295, audit) | PostgreSQL RLS, partner-scoped Keycloak roles, audit list query |
| PARTNER-PROD-007 | P1 | ⏸️ Belum | HPA≥3, PDB minAvailable 2, topology spread, bounded timeout |
| PARTNER-PROD-008 | P0 | ⏸️ Belum | PG HA+PITR, restore drill, RPO/RTO, retention/archive |
| PARTNER-PROD-009 | P1 | ⏸️ Belum | SLI/SLO, dashboard+alert, traces end-to-end |
| PARTNER-PROD-010 | P0 | ⏸️ Belum | Contract/conformance, k6 load/soak, chaos, pentest, partner sign-off |
| PARTNER-PROD-011 | P1 | ⏸️ Belum | Dual-control onboarding, SLA/escalation, runbook, on-call |

> Local APIcast (profile `api-management`) tidak bisa authless (verified via Context7) — public edge butuh APIManager (cluster-level).

---

## 🚀 Platform Deploy Queue

| Key | Pri | Category | Summary |
|:---|:---:|:---|:---|
| DEPLOY-006 | P1 | Security | Coraza WAF (INFRA-015) + Wazuh SIEM (INFRA-011) + sisa CIS `audit-log-forwarding-enabled` sink |
| DEPLOY-011 | P1 | Promotion | SIT/UAT/preprod LIVE di lab `cluster-nkk8q` (ArgoCD 18 apps, Vault HA, pipeline SIT green: sync-wait + k6 + ZAP + Schemathesis). Sisa: litmus gate, preprod kraken gate, Infinispan Hot Rod mTLS (analytics 500), prod sync window + promotion via pipeline |
| INFRA-026 | P1 | Secrets | Vault HA live + restore drill verified. Sisa: snapshot S3 CronJob verify, kv readback via k8s auth, auto-unseal key backup |
| DEPLOY-009 | P2 | CI/CD | Tekton Results live (365d); sisa: external HA PostgreSQL, Chains SLSA/Rekor evidence, Renovate |
| DEVSECOPS-017 | P1 | Secrets | Tekton Buildah butuh `redhat-registry-pull` workspace + Vault `secret/payu/cicd/redhat-registry` (prerequisite eksternal — jangan placeholder) |
| OPS-2026-08-01-05 | P2 | Chaos | Kraken manifest fixed (emptyDir + SCC); re-run preprod gate saat CPU pulih |
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
| NOTIF-001 | 🔴 | notification | LOG-mode false success tanpa delivery ID — **PARTIAL 2026-08-12** (fail-closed live, lihat PROD-044); sisa provider nyata + delivery ID butuh credential eksternal | SmsSender.java:26-54 |
| — | 🟢 | wallet | Reserve/commit flow solid; escrow & split-payment state machine solid | WalletService, EscrowTransaction |
| — | 🟢 | partner | Refund concurrency, callback HMAC, SNAP signature | SnapBiPaymentService, CallbackSignatureFilter |

## 📋 Open Findings — Audit Arsitektur 2026-08-13 (26 service vs AGENTS.md rules + ADR)

> Audit hexagonal/ArchUnit/money/idempotency/events/RFC 9457/DTO/container. Verifikasi berbasis source code.

### 🔴 Kritis (money/PII/event integrity)

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| ARCH-TOPIC-002 | 🔴 | platform | Hanya 10 KafkaTopic deklaratif (`transaction.*.v1`, dispute, lending-repayment, partner-refunded + 3 dlq); ~30 topic dari kode auto-create tanpa RF/partisi eksplisit (risiko data-loss event finansial); resource legacy `account-events`/`wallet-events`/`notification-events`/`transaction-events` tanpa `topicName: payu.*` | kafka-amqstreams.yaml:98-345 |

### 🟠 Sistematis (lintas-service)

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| ARCH-DTO-001 | 🟠 | semua | 20+ service menaruh DTO di `dto/` root / `domain.dto` / `adapter.web.dto`, bukan `interfaces.dto` | dto/QrisPaymentRequest.java, dto/TopUpRequest.java, dsb. |
| ARCH-DLQ-001 | 🟠 | promotion, cms, dispute, statement, platform | ~~Tanpa `.dlq` wiring; outbox event gagal-permanen cuma di-archive/log, tidak pernah ke `.dlq`~~ **PLATFORM DONE 2026-08-13** — outbox-starter: event gagal permanen (> maxRetries) kini di-copy best-effort ke `destinationTopic + .dlq` (`sendToDlq`, guard test); **DELIBERATELY DEFERRED 2026-08-16** — sisa "consumer per service yang konsumsi `.dlq`" menunggu alert destination (Slack/PagerDuty via Vault, DEVSECOPS-017); sampai itu ada, `OutboxCleanupScheduler` sudah log `OUTBOX-001 ALERT` untuk failed archived (retention), sehingga event tak pernah hilang tanpa jejak. Consumer DLQ log-only akan duplikat alert ini (YAGNI); tambah saat destination nyata tersedia | OutboxCleanupScheduler.java:77-85 |
| ARCH-HEX-001 | 🟠 | statement, support, auth, loan-origination, api-portal, kyc, lending-rules | Hex bocor: application import adapter langsung (statement/support/auth); JPA entity bocor ke controller + `Map.of("error")` (loan-origination); tanpa domain layer (api-portal, kyc, lending-rules); tanpa ArchUnit (loan-origination, api-portal, lending-rules); lending-rules 0 test | StatementService.java:3-6; LoanOriginationController.java:41 |
| ARCH-PARTNER-001 | 🟡 | partner | ~~PaymentWebhookHandler tidak di-wire~~ **WIRED 2026-08-13** — `WebhookDispatcherService.dispatch` kini memanggil handler yang `supportedEventTypes` match (processWebhook/onSuccess/onError; null-guard; sebelum early-return subscription). Sisa: API unversioned (`/merchants`, `/partners`, `/webhooks`, `/payment-links`) | WebhookDispatcherService |
| ARCH-CONS-001 | 🟡 | platform | ~~wallet `RefundRequestedConsumer` tanpa claim/dedup~~ **VERIFIED 2026-08-13** — dedup via natural key `refund_id` (PRIMARY KEY) + COMPLETED guard + reconcile; `RefundReversalExecutorTest` replay+invalid-event tests ditambah (3/3). Sisa: manual ack seragam lintas consumer | RefundRequestedConsumer + RefundReversalExecutor |
| ARCH-CDC-001 | 🟢 | platform | Tanpa Debezium; relay outbox = polling dispatcher `SKIP LOCKED` (legal pola). Note: evaluasi CDC bila throughput naik | OutboxPublisher.java:119-121 |
| ARCH-DEDUP-001 | 🟠 | partner, promotion | Migrasi dedup DELETE baris finansial pre-constraint (`snap_bi_payments`/`refunds`/`cashbacks`/`rewards`) — legal hanya jika belum pernah jalan di prod; perlu bukti env + policy | partner V16/V17; promotion V11/V12 |
| ARCH-FLYWAY-001 | 🟠 | account | Destruktif historis `DROP COLUMN` + `RENAME COLUMN` di migrasi ter-aplikasi — anti-pattern, risiko fresh-restore; jangan diulang | account V10:16-27 |
| ARCH-PAGE-001 | 🟠 | transaction, wallet | Pagination Pageable = OFFSET default; history finansial besar butuh keyset cursor `(created_at, id)` | TransactionJpaRepository.java:53 |

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

### Gap per flow (FLOWS.md 47 flow; MVP 22 flow di-map ke bukti test — 2026-08-13)

| Flow | Gap | Sev |
|:---|:---|:---:|
| 16 Escrow (wallet) | unit (8) + integration (2) **ADDED 2026-08-13** (real PG); sisa E2E blackbox | 🟡 |
| 11 Split Bill (transaction) | integration **ADDED 2026-08-13** (real PG + outbox); sisa unit domain + E2E | 🟡 |
| 7 Transfer Interbank BI-FAST | integration test **ADDED 2026-08-13** (real PG + outbox); sisa E2E blackbox | 🟡 |
| 10 Disbursement | integration **ADDED 2026-08-13** (real PG, idempotency dedup); sisa E2E blackbox | 🟡 |
| 9 VA Payment | Tanpa E2E + IT | 🟠 |
| 22 Settlement Batch | Tanpa E2E + IT | 🟠 |
| 5 SNAP Refund | Tanpa E2E | 🟠 |
| 14 Investment Jual | Tanpa E2E + IT | 🟠 |
| 19 Payment Link | Tanpa E2E + IT | 🟠 |
| 3 Transfer Internal / 8 QRIS / 12 Top-up | Ada E2E, tanpa integration test | 🟠 |

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
