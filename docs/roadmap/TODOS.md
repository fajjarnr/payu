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
| **Last Release** | `1.11.0` (2026-08-13) |
| **Core Banking MVP** | 🔴 Belum MVP — blocker tersisa: ACCOUNT-006/007 (P1) + PROD-044 (P1); **login web live** (LOGIN-001..006 closed: PKCE + gate CI + browser E2E), money-flow live (PROD-043/045/047, CB-014/016/020/021/023 closed). Belum ada service production ready. |
| **Backlog Aktif** | 3 tickets + 46 action items (CB-*/PROD-*/READY-*/DEVSECOPS-*/ARCH-*/QAMVP-*) + gates partner/platform (2026-08-13) |
| **Last Updated** | 2026-08-13 (audit QA v3 deep: test content 4 money service + kyc/analytics + frontend + contract/coverage/security — 10 gap baru QAMVP-011..020; 29 fix di P1) |

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
| ACCOUNT-006 | P1 | Coverage account ~21% line/19% branch; integration test tidak required di CI. Done: ≥80% overall, 100% core domain, required CI. | 🟠 Test gate insufficient |
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
| ARCH-INTG-001 | integration | Route swift/ojk → outbox-starter; hapus `kafka:` endpoint; pakai `MessagePublisherAdapter` | 0 `.to("kafka:")` |
| ARCH-TOPIC-002 | platform | KafkaTopic deklaratif untuk semua topic kode (RF 3, partisi sesuai consumer); hapus resource legacy `*-events`; audit auto-create off | `oc get kafkatopic` lengkap vs EVENT_CATALOG |
| ARCH-PROD-001 | platform | ~~Producer default `acks=all` + `enable.idempotence=true` + retries di outbox-starter (satu tempat semua service)~~ **CLOSED 2026-08-13** — `outboxProducerFactory` sebelum `KafkaAutoConfiguration` (`@ConditionalOnMissingBean`); guard `OutboxProducerFactoryTest` | Property terverifikasi di producer config |
| ARCH-DECIMAL-001 | promotion | ~~Widening `discount_value` → DECIMAL(19,4) + sync domain scale 4~~ **CLOSED 2026-08-13** — V13, entity precision 19, scale 4 (PROMO-004 + normalizeAmount floor 4), 261 test green | Kolom 19,4 + test |
| QAMVP-001 | platform | CI backend: workflow PR changed-service — unit + integration semua service (sekarang 0) | PR status red/green per service |
| QAMVP-002 | transaction, wallet | Integration test Testcontainers (PG+Kafka) money journey: transfer, reserve/commit, outbox atomic | Suite jalan di CI |
| QAMVP-003 | billing, partner | Contract test: billing payment, SNAP-BI payment/refund/auth-token + CloudEvents contract | `tests/contract` bertambah |
| QAMVP-004 | kyc | Security test (auth/RBAC) + integration test kyc; provider OCR/liveness nyata gate (analog PROD-002) | Test + live evidence |
| QAMVP-005 | platform | k6 smoke+load di pipeline staging + SLO threshold per service | Laporan k6 di CI |
| QAMVP-006 | platform | PRD launch criteria tracker: prod deploy OCP, app stores, legal ToS, security hardening (lanjut CB-006) | Checklist PRD §12 hijau |
| QAMVP-007 | wallet | Escrow test: unit domain + integration + E2E (sekarang 0 test semua layer) | Escrow money journey hijau |
| QAMVP-008 | transaction | Split-bill test: unit + integration + E2E (sekarang 0) + fix topic (ARCH-TXN-002) | Split-bill journey hijau |
| QAMVP-009 | transaction | BI-FAST transfer integration test (Testcontainers PG+Kafka + simulator) + E2E blackbox | BI-FAST journey hijau |
| QAMVP-010 | transaction, loan-origination | Disbursement integration + E2E; wajib setelah ARCH-LOAN-001 fix | Disbursement journey hijau |
| QAMVP-011 | wallet, transaction, billing, partner | ~~Test idempotency concurrency: 10 thread key sama → 1 mutasi, 1 ledger, 1 outbox~~ **CLOSED 2026-08-13** — 4 service: `WalletControllerConcurrencyIdempotencyTest` (credit), `TransactionControllerConcurrencyIdempotencyTest` (transfer), `PaymentControllerConcurrencyIdempotencyTest` (bill payment), `MerchantControllerConcurrencyIdempotencyTest` (QR) — 10 thread X-Idempotency-Key sama → 1 mutasi, 1 successful atomic claim, 0 5xx, 0 throw; 5× run stabil | Test thread lulus CI |
| QAMVP-012 | wallet, transaction, billing, partner | ~~Test same-key + different-payload ditolak (conflict, bukan replay)~~ **CLOSED 2026-08-13** — `sameKeyDifferentPayloadIsConflict` di 4 service (wallet/transaction/billing/partner): 409 conflict, 1 mutasi | Test lulus CI |
| QAMVP-013 | wallet, transaction | ~~Test outbox atomicity dengan Testcontainers PG+Kafka: business row + outbox row commit/rollback bersama~~ **wallet DONE 2026-08-13** — `OutboxAtomicityIntegrationTest` (PostgreSQL real via Testcontainers + podman socket): commit → business row + outbox row ada; rollback → 0 keduanya. Sisa: transaction | Test lulus CI |
| QAMVP-014 | wallet, kyc, analytics, billing, backoffice, cms, api-portal | ~~Security test (401/403/RBAC) — sekarang 0 di 7 service~~ **wallet + billing DONE 2026-08-13** — `WalletSecurityTest` (401/403/RBAC real security chain, `payu.grpc.server.port=0`), `PaymentSecurityTest` (401 + ownership). Sisa: kyc/analytics (Python), api-portal (Quarkus) | Test lulus CI |
| QAMVP-015 | platform | Contract test error case (401/422 RFC 9457) + wiring CI + fix README stale | `tests/contract` hijau di CI |

### P2 — Defer (Out-of-Scope MVP, ADR-0023)

> ✅ **Seluruh backlog P2 aksi CLOSED 2026-08-12** (CB-008/011/017/022/024/025/031/036) — lihat CHANGELOG `1.10.63`. Tidak ada item tersisa.

### P3 — Backlog Lanjutan

| Key | Domain | Item |
|:---|:---|:---|
| CB-009 | lending | Lending financial E2E fixture + integration test lending/fx/statement (defer) |
| READY-022 | qa | 80% coverage audited 4-22% (4 service) |
| READY-060 | card | Card tokenization + 3DS |
| READY-062 | ml | ONNX fraud detection model |
| DEVSECOPS-015 | devsecops | Security Findings Dashboard Grafana |
| DEVSECOPS-016 | devsecops | Service template scaffolder |

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
| OPS-2026-08-01-04 | P2 | Observability | Log delivery: vector connect OK; blocked 403 `lokistack-gateway.rego` kosong (operator bug LOG-2236 → RH support / tenant workaround) |
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

| ACCOUNT-003-RLS | 🟠 | account | ACCOUNT-003 closed via trusted-credential tenant + Hibernate filter + cross-tenant tests; PostgreSQL RLS (defense-in-depth) belum aktif — sama seperti remaining PARTNER-PROD-006 | V105/V106, TenantEnforcementAspect |
| NOTIF-001 | 🔴 | notification | LOG-mode false success tanpa delivery ID — **PARTIAL 2026-08-12** (fail-closed live, lihat PROD-044); sisa provider nyata + delivery ID butuh credential eksternal | SmsSender.java:26-54 |
| PROMO-002 | 🟠 | promotion | Loyalty redeem tanpa dedup | LoyaltyPointsService.java:82-109 |
| PROMO-003 | 🟠 | promotion | `claimPromotion` tanpa dedup by transactionId — replay/double-submit → 2 reward AWARDED (maxRedemptions atomik ✓, tapi per-user/per-transaction tidak ada guard) | PromotionService.java:139-180 |
| PROMO-004 | 🟠 | promotion | ~~`calculateRewardAmount` PERCENTAGE `divide(..., 2, HALF_EVEN)` — scale 2, melanggar ADR-0022 (scale 4 wajib)~~ **CLOSED 2026-08-13** dengan ARCH-DECIMAL-001 — `PromoCode.calculateDiscount` + `PromoUsagePersistenceMapper.normalizeAmount` kini scale 4 | PromoCode.java:116 |
| REFERRAL-001 | 🟠 | promotion | completeReferral tanpa lock | ReferralService.java:79-107 |
| TEST-GAP | 🟠 | qa | 6/8 core banking tanpa integration test; wallet 31 @Test | src/test structure |
| INTEGRATION-CTX | 🟠 | qa | Account-service integration test context: **VaultConfigurationTest FIXED** (2026-08-12: mock DataSource di TestJpaConfig) → default suite 132/132. Sisa: OnboardingIntegrationTest + BlindIndexAndTenantIsolationIntegrationTest masih `No bean named 'entityManagerFactory'` — test tanpa `@ActiveProfiles("test")` (activeProfiles=[]), dan app pakai multi-DS custom (`spring.datasource.primary.*`, bukan `spring.datasource.*`) sehingga dynamic property + `@ServiceConnection` tidak di-honor; workaround sementara: verifikasi DB langsung (podman postgres) | surefire context load errors |
| — | 🟢 | wallet | Reserve/commit flow solid; escrow & split-payment state machine solid | WalletService, EscrowTransaction |
| — | 🟢 | partner | Refund concurrency, callback HMAC, SNAP signature | SnapBiPaymentService, CallbackSignatureFilter |

## 📋 Open Findings — Audit Arsitektur 2026-08-13 (26 service vs AGENTS.md rules + ADR)

> Audit hexagonal/ArchUnit/money/idempotency/events/RFC 9457/DTO/container. Verifikasi berbasis source code.

### 🔴 Kritis (money/PII/event integrity)

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| ARCH-DECIMAL-001 | 🔴 | promotion | ~~`discount_value DECIMAL(10,4)` — kolom money wajib (19,4); widening migration + cek cast~~ **CLOSED 2026-08-13** — V13 widening + domain scale 4 (PROMO-004) | promotion V13 |
| ARCH-INTG-001 | 🔴 | integration | ~~Route swift/ojk → outbox-starter; hapus `kafka:` endpoint; pakai `MessagePublisherAdapter`~~ **CLOSED 2026-08-13** — SwiftRouteBuilder/OjkRouteBuilder publish via `MessagePublisherPort` (outbox), 0 `.to("kafka:")`, camel-kafka-starter dihapus | SwiftRouteBuilder.java, OjkRouteBuilder.java |
| ARCH-TOPIC-002 | 🔴 | platform | Hanya 10 KafkaTopic deklaratif (`transaction.*.v1`, dispute, lending-repayment, partner-refunded + 3 dlq); ~30 topic dari kode auto-create tanpa RF/partisi eksplisit (risiko data-loss event finansial); resource legacy `account-events`/`wallet-events`/`notification-events`/`transaction-events` tanpa `topicName: payu.*` | kafka-amqstreams.yaml:98-345 |

### 🟠 Sistematis (lintas-service)

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| ARCH-DTO-001 | 🟠 | semua | 20+ service menaruh DTO di `dto/` root / `domain.dto` / `adapter.web.dto`, bukan `interfaces.dto` | dto/QrisPaymentRequest.java, dto/TopUpRequest.java, dsb. |
| ARCH-TOPIC-001 | 🟠 | investment, partner, notification, promotion, lending | Topic generik/off-standard: `payu.investment.event.v1`, `payu.partner.payment-link-event.v1`; konsumsi `transaction.completed`, `payu.transactions.*`, `subscription.events`, `payu.kyc.verified` (tanpa `.v<n>`); bean dead `loan.approved`/`loan.rejected` | KafkaInvestmentEventPublisherAdapter.java:27; application.yml |
| ARCH-DLQ-001 | 🟠 | promotion, cms, dispute, statement, platform | ~~Tanpa `.dlq` wiring; outbox event gagal-permanen cuma di-archive/log, tidak pernah ke `.dlq`~~ **PLATFORM DONE 2026-08-13** — outbox-starter: event gagal permanen (> maxRetries) kini di-copy best-effort ke `destinationTopic + .dlq` (`sendToDlq`, guard test); sisa = consumer per service yang belum konsumsi `.dlq` | OutboxCleanupScheduler.java:77-85 |
| ARCH-DEAD-001 | 🟠 | platform | `resilience-starter` 0 pemakaian di 18 service (tim pakai resilience4j langsung); KafkaTemplate bean tak terpakai (billing/cms/lending); `EmitterPlaceholder` di promotion | NikVerificationService.java:9-12 |
| ARCH-ADR17-001 | 🟠 | account, api-portal, auth | Sisa config Redis/RESP pasca ADR-0017 (Infinispan HotRod) masih aktif | account-service application.yaml:100,118-121 |
| ARCH-SECRET-001 | 🟠 | kyc, auth, compliance, gateway | Default secret hardcoded: `ARTEMIS_PASSWORD: "admin"`; Keycloak client-secret di application-local/dev.yml; `payu_secret` gateway local | kyc config.py:32 |
| ARCH-LOG-001 | 🟠 | analytics | Structlog tanpa PII-masking processor | logging_config.py:72-79 |
| ARCH-HEX-001 | 🟠 | statement, support, auth, loan-origination, api-portal, kyc, lending-rules | Hex bocor: application import adapter langsung (statement/support/auth); JPA entity bocor ke controller + `Map.of("error")` (loan-origination); tanpa domain layer (api-portal, kyc, lending-rules); tanpa ArchUnit (loan-origination, api-portal, lending-rules); lending-rules 0 test | StatementService.java:3-6; LoanOriginationController.java:41 |
| ARCH-STATEMENT-001 | 🟠 | statement | Endpoint partner `/v1/partner/statements` (ADR-0019) tidak ada sama sekali | src/main code |
| ARCH-PARTNER-001 | 🟠 | partner | API unversioned (`/merchants`, `/partners`, `/webhooks`, `/payment-links`); inbound webhook HMAC handler (PaymentWebhookHandler) tidak di-wire ke mana pun | MerchantController.java:31 |
| ARCH-TOPIC-003 | 🟠 | wallet, transaction, billing | Consumer pakai topic off-standard: `fx-rates-updated` (default, bukan `payu.fx.rates-updated.v1`), `disbursement-batch`; orphan consumer billing `payu.billing.subscription-due.v1` tanpa publisher | FxRateEventConsumer.java:27; BatchDisbursementService.java:174 |
| ARCH-PROD-001 | 🟠 | platform | ~~Producer config tidak seragam: `acks=all` + retries 3 cuma wallet; `enable.idempotence` tidak dideklarasi di mana pun; outbox-starter tidak set producer props (ikut default client acks=1)~~ **CLOSED 2026-08-13** — outbox-starter default acks=all + idempotence + retries 5 | outbox-starter OutboxAutoConfiguration |
| ARCH-CONS-001 | 🟠 | platform | Consumer manual ack cuma integration-service; lain auto-commit default. Dedup konsumen belum merata (wallet `RefundRequestedConsumer` tanpa claim/dedup layer) | integration application.yml:38; RefundRequestedConsumer.java:21 |
| ARCH-CDC-001 | 🟢 | platform | Tanpa Debezium; relay outbox = polling dispatcher `SKIP LOCKED` (legal pola). Note: evaluasi CDC bila throughput naik | OutboxPublisher.java:119-121 |
| ARCH-CE-002 | 🟠 | account, billing, fx, transaction | 4 publisher kirim payload plain Map tanpa atribut CloudEvents (id/source/type/time) — hanya wallet/transaction-main/billing-subscription pakai `CloudEventBuilder` | KafkaUserEventPublisherAdapter.java:44; SplitBillEventPublisherAdapter.java:33-49 |
| ARCH-RLS-001 | 🟠 | billing, dispute, lending, transaction, wallet | RLS: 0 migrasi semua service — isolasi tenant cuma app-filter; RLS defense-in-depth belum (lanjutan ACCOUNT-003-RLS/PARTNER-PROD-006) | grep ROW LEVEL SECURITY = 0 |
| ARCH-DEDUP-001 | 🟠 | partner, promotion | Migrasi dedup DELETE baris finansial pre-constraint (`snap_bi_payments`/`refunds`/`cashbacks`/`rewards`) — legal hanya jika belum pernah jalan di prod; perlu bukti env + policy | partner V16/V17; promotion V11/V12 |
| ARCH-FLYWAY-001 | 🟠 | account | Destruktif historis `DROP COLUMN` + `RENAME COLUMN` di migrasi ter-aplikasi — anti-pattern, risiko fresh-restore; jangan diulang | account V10:16-27 |
| ARCH-PAGE-001 | 🟠 | transaction, wallet | Pagination Pageable = OFFSET default; history finansial besar butuh keyset cursor `(created_at, id)` | TransactionJpaRepository.java:53 |
| ARCH-PROJ-001 | 🟠 | wallet | Materialized views (V5) tanpa dokumentasi refresh lag; butuh reconcile job terjadwal vs ledger | wallet V5__Create_materialized_views.sql |

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
| 16 Escrow (wallet) | **0 test semua layer** — money movement tanpa bukti | 🔴 |
| 11 Split Bill (transaction) | **0 test semua layer** | 🔴 |
| 7 Transfer Interbank BI-FAST | Tanpa E2E + tanpa integration test (fitur flagship MVP) | 🔴 |
| 10 Disbursement | Tanpa E2E + IT; ditambah ARCH-LOAN-001 (idempotency/ref random) | 🔴 |
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
| QAMVP-013 | Outbox atomicity: wallet **FIXED 2026-08-13** (`OutboxAtomicityIntegrationTest` commit+rollback, real PG); `TestcontainersConfig` wallet kini terpakai; sisa transaction | 🟡 |
| QAMVP-014 | Security test: wallet/billing/transaction/backoffice/cms **FIXED 2026-08-13**; sisa kyc/analytics (Python) + api-portal (Quarkus) | 🟡 |
| QAMVP-015 | Contract test: 3 happy-path, **0 error case** (401/422), tidak dijalankan CI; README klaim 4 pair padahal 3 file | 🟠 |
| QAMVP-016 | Coverage: jacoco goal tidak di-bound (Makefile malah hapus jacoco.exec); kyc 65% < gate 80%; READY-022 unresolved | 🟠 |
| QAMVP-017 | Pitest 1.15.0 dikonfigurasi, **0 bukti eksekusi** (dead config, threshold 60% mutation di doc) | 🟠 |
| QAMVP-018 | ZAP + Schemathesis cuma di Tekton SIT, tidak ada di GitHub CI per-PR | 🟠 |
| QAMVP-019 | Frontend: halaman statement **tidak ada** (cuma service test); budget tanpa E2E; a11y filter color-contrast/button-name keluar ("design debt") → bukan WCAG strict; refresh-token expiry tanpa E2E; `forgot-password` + `not-found` spec = stub | 🟠 |
| QAMVP-020 | Money test: HALF_EVEN half-way cuma transaction `MoneyTest`; wallet cuma validasi scale; billing/partner nol; double-entry numerik cuma `LedgerInvariantTest`; RFC 9457 test cuma partner | 🟠 |

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
