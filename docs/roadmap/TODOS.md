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
| **Last Release** | `1.10.52` (2026-08-11) |
| **Core Banking MVP** | 🔴 Belum MVP — blocker tersisa: ACCOUNT-005/006/007 (P1) + PROD-044/046 (P1); **login web live** (LOGIN-001..006 closed: PKCE + gate CI + browser E2E), money-flow live (PROD-043/045/047, CB-014/016/020/021/023 closed). Belum ada service production ready. |
| **Backlog Aktif** | 5 tickets + 32 action items (CB-*/PROD-*/READY-*/DEVSECOPS-*) + gates partner/platform (2026-08-12) |
| **Last Updated** | 2026-08-12 (CB-034 closed; Active Tickets urut P0→P1) |

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
| ACCOUNT-005 | P1 | Onboarding: IAM provision tanpa kompensasi (orphan user); `externalId` dari request publik kalau IAM tak return ID (UserApplicationService.java:57-64). Done: external ID dari IAM + saga/compensation. | 🟠 Consistency/trust gap |
| ACCOUNT-006 | P1 | Coverage account ~21% line/19% branch; integration test tidak required di CI. Done: ≥80% overall, 100% core domain, required CI. | 🟠 Test gate insufficient |
| ACCOUNT-007 | P1 | Belum ada deployment `payu-prod`; rollout Recreate; UAT HPA min 1, tanpa PDB. Done: prod deploy via gates + HA + drill + E2E onboarding→lookup→ownership. | 🟠 Not production deployed |
| PROD-044 | P1 | Notification false success: SMS LOG mode `return true`, push mock, mailer `smtp.example.com` + `mock:true` (SmsSender.java:26-54, PushSender.java:8-23). Done: provider nyata fail-closed + delivery ID + E2E. | 🔴 Feature unusable |
| PROD-046 | P1 | Kontrak referral web↔backend tidak cocok (referralCode/totalEarnings). Done: DTO selaras + E2E atau hapus klaim fitur. | 🟠 Partial feature |
| INFRA-029 | P1 | Audit log forwarding: CLF live (CIS satisfied), sisa Wazuh SIEM sink (INFRA-011) + verifikasi log arrival. | 🟢 Live — sink pending |

---

## 🎯 Backlog Aksi (urut per priority)

### P1 — Quality & Reliability (In-Scope MVP)

| Key | Domain | Item | Done saat |
|:---|:---|:---|:---|
| CB-005 | qa | Coverage gate: account ≥80% + integration tests wajib (ACCOUNT-006) | JaCoCo gate di CI |
| CB-029 | notification | Provider nyata fail-closed + delivery ID (PROD-044) — PII log sudah dimask (PROD-045 closed) | E2E terima; log tanpa PII |
| CB-006 | platform | Prod deploy core banking: gates + HPA≥2 + PDB2 + DR drill (ACCOUNT-007) | ACCOUNT-007 closed |
| CB-007 | qa | Money-safety regression suite lintas core (idempotency, outbox, DECIMAL(19,4), reversal, DLQ) | Suite green di CI |
| CB-015 | transaction | E2E transfer hop-by-hop incl. kompensasi | E2E green |
| CB-018 | shared | Outbox failed-event: archive + alert, bukan DELETE (OUTBOX-001) | Event tidak hilang tanpa alert |
| CB-026 | promotion | Dedup cashback: unique transaction_id (PROMO-001) — jalur SNAP in-scope | Replay tanpa duplikat |
| CB-028 | dispute | Lock over-refund di `assertRefundable` (DISPUTE-001) | Concurrent partial refund aman |
| CB-037 | notification | **IMP-4 Retry + fallback channel** (FLOWS.md): notification retry backoff + fallback push→email→SMS (pendamping CB-029 provider) | Gagal channel → fallback sukses, test green |
| PROD-002 | fx | Approved FX provider URL/credential + live evidence | Rate live + audit pair |
| PROD-018 | analytics | Aktifkan `analytics-tests` sebagai required branch protection | CI gate aktif |

### P2 — Defer (Out-of-Scope MVP, ADR-0023)

| Key | Domain | Item | Done saat |
|:---|:---|:---|:---|
| CB-008 | transaction | MVP-003 VA settlement live E2E di payu-dev (QRIS/VA di luar MVP) | Live E2E green |
| CB-011 | transaction | Versioning topic split-bills (TX-001) | Topic `.v1`, consumer updated |
| CB-017 | transaction | QRIS idempotency DB fallback + fail-closed (QRIS-001 / IMP-6 FLOWS.md) — QRIS Phase 2 | Replay tidak double-charge |
| CB-022 | billing | Subscription charge: wire wallet debit checkpoint (SUB-001) atau suspend | Charge hanya setelah debit sukses |
| CB-024 | lending | PayLater: @Version + idempotency + money movement (PAYLATER-001) | Race/idempotency tests green |
| CB-025 | fx | FX reverse guard: status REVERSED + setScale (FX-002) | Double-reverse ditolak |
| CB-031 | transaction | Scheduled transfer idempotency (TX-004) | Overlap tidak double debit |
| CB-036 | statement | **IMP-3 Statement closing balance dari ledger `balance_after`** (FLOWS.md) — bukan derive transaksi pasca-periode | Opening/closing = ledger snapshot, test green |

### P3 — Backlog Lanjutan

| Key | Domain | Item |
|:---|:---|:---|
| CB-009 | lending | Lending financial E2E fixture + integration test lending/fx/statement (defer) |
| CB-027 | promotion | Dedup loyalty redeem (PROMO-002) (defer) |
| CB-032 | promotion | Dedup promo claim by transactionId (PROMO-003) + unique constraint (defer) |
| CB-033 | promotion | Reward percentage scale 4 (PROMO-004) — ADR-0022 compliance (defer) |
| CB-030 | promotion | Referral lock + dedup (REFERRAL-001) (defer) |
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
| SUB-001 | 🔴 | billing | Subscription charge `markSucceeded()` tanpa debit | SubscriptionService.java:395-401 |
| PAYLATER-001 | 🔴 | lending | Race + non-idempotent + tanpa money movement | PayLaterTransactionService.java:36-115 |
| NOTIF-001 | 🔴 | notification | LOG-mode false success tanpa delivery ID (PII log sudah dimask — PROD-045 closed) | SmsSender.java:26-54 |
| OUTBOX-001 | 🔴 | shared | Failed event di-DELETE setelah 7 hari tanpa DLQ/alert | OutboxCleanupScheduler |

| FX-002 | 🟠 | fx | Reverse tanpa status REVERSED; toAmount tanpa setScale | FxConversionService.java:118-160 |
| TX-001 | 🟠 | transaction | Topic split-bills tanpa `.v<n>` | SplitBillEventPublisherAdapter.java:46 |
| TX-004 | 🟠 | transaction | Scheduled transfer tanpa idempotency key | ScheduledTransferService.java:172-230 |
| QRIS-001 | 🟠 | transaction | Idempotency cache-only fail-open (TTL 24h) | DistributedCacheIdempotencyRepository |
| PROMO-001 | 🟠 | promotion | Cashback record duplikat saat replay | CashbackSagaOrchestrator.java:119-140 |
| PROMO-002 | 🟠 | promotion | Loyalty redeem tanpa dedup | LoyaltyPointsService.java:82-109 |
| PROMO-003 | 🟠 | promotion | `claimPromotion` tanpa dedup by transactionId — replay/double-submit → 2 reward AWARDED (maxRedemptions atomik ✓, tapi per-user/per-transaction tidak ada guard) | PromotionService.java:139-180 |
| PROMO-004 | 🟠 | promotion | `calculateRewardAmount` PERCENTAGE `divide(..., 2, HALF_EVEN)` — scale 2, melanggar ADR-0022 (scale 4 wajib) | PromotionService.java:184-191 |
| DISPUTE-001 | 🟠 | dispute | Over-refund race (sum-then-check tanpa lock) | RefundService.java:153-164 |
| REFERRAL-001 | 🟠 | promotion | completeReferral tanpa lock | ReferralService.java:79-107 |
| TEST-GAP | 🟠 | qa | 6/8 core banking tanpa integration test; wallet 31 @Test | src/test structure |
| IMP-3 | 🟠 | statement | Flow improvement target: closing balance derive → ledger `balance_after` — CB-036 | FLOWS.md IMP-3 |
| IMP-4 | 🟠 | notification | Flow improvement target: retry + fallback channel — CB-037 | FLOWS.md IMP-4 |
| IMP-6 | 🟠 | transaction | Flow improvement target: QRIS idempotency DB — CB-017 | FLOWS.md IMP-6 |
| INTEGRATION-CTX | 🟠 | qa | Account-service @SpringBootTest context pre-existing broken: `No bean named 'entityManagerFactory'` (HibernateJpaAutoConfiguration tidak aktif di test; VaultConfigurationTest + OnboardingIntegrationTest red juga di HEAD bersih 2026-08-11). Blokir integration tests account & bukti CB-005; workaround sementara: verifikasi DB langsung (podman postgres) | surefire context load errors |
| — | 🟢 | wallet | Reserve/commit flow solid; escrow & split-payment state machine solid | WalletService, EscrowTransaction |
| — | 🟢 | partner | Refund concurrency, callback HMAC, SNAP signature | SnapBiPaymentService, CallbackSignatureFilter |

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
