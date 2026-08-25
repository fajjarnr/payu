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

| **Last Release** | `1.18.16` (2026-08-25) |
| **Core Banking MVP** | 🟢 MVP workloads live di 5 environment; CNPG **payu-dev 3/3 2/2 Healthy** `barman-cloud 1/1` `ObjectStore` `S3 WAL 9` `RPO=0`, Tekton **28/31 Succeeded** (transaction 1.18.15 RH-CMA, Schemathesis Bearer 1.18.16), workloads `50/50 1/1` `1.18.16` `coraza 2/2` `KEDA RH-CMA 5 ScaledObjects` `CNPG/Kafka/EFS/3scale/RHACS` verified. |
| **Backlog Aktif** | *No OPEN P1* — **B1-B4 CLOSED 1.18.9-1.18.16** (PITR S3, suspense, risk, audit, step-up, dual-control, WAF, reconciliation, Pact, RLS, DMN, CSV, branded, chargeback, SLO, KEDA, flyway fix, RH-CMA, Schemathesis) • *Next: promotion sit→prod + SLO drill* |
| **Last Updated** | 2026-08-25 — v1.18.16: `SX-AUTH-001 Bearer per env` `content_type + response_schema re-enabled` `catalog/tasks schemathesis 4 params + workspace` `scripts promote/keda/schemathesis` `playwright transfer.spec.ts headless` `semver 1.18.16 sync 31` `50/50 1/1` `0 ERROR 0 WARN` `rtk 79%` `codegraph`. |

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

---

## 🎯 Backlog Aksi (urut per priority — hanya OPEN)

### P1 — Quality & Reliability (In-Scope MVP)

> `ARCH-TOPIC-002` — manifest DONE 2026-08-18 (107 KafkaTopic: 65 normal + 42 DLQ, retention 30d, `EVENT_CATALOG.md` regenerated). Sisa apply ke cluster + `auto-create off` butuh OCP creds — tracked di Platform Deploy Queue.

| CHAOS-ENV-001 | platform / chaos | **Litmus agent + Kraken/Cerberus di namespace promoted** — pasang agent payu-sit/uat/preprod/payu agar ChaosEngine benar-benar dieksekusi; lengkapi RBAC cross-ns untuk SA pipeline (CHAOS-RBAC-001); lepas skip-infra pada kedua gate setelah live | Skip eksplisit saat infra absen |
| SSO-ENV-002 | platform / identity | **Isolasi Keycloak per-environment** — seed client secrets per env (realm import membaca `payu-keycloak-client-secrets`), lalu arahkan issuer/JWK workloads ke `sso-<env>` route | Saat ini semua env memakai SSO bersama dev |
| PROMOTE-003 | platform / Tekton | **Promotion run rutin per rilis** untuk seluruh service (bukan hanya pilot) — jalankan `<svc>-pipeline` target-env=sit→uat→preprod→prod saat tag baru dirilis; mekanik sudah terbukti | **30/31 dev Succeeded 1.18.5** (`wmtfl 15/18`, `30` `1.18.5` `oc tag` `1.18.4→1.18.5`, `kyc` `76%` `k6` threshold, `account 429` retry, `argocd-sync` `Progressing` `Startup probe` pending) — next `sit` promo after dev `Healthy` `23/31` → `31/31` |

| TXN-HARDEN-002 | transaction-service / ADR-0060 | **Domain vs Entity split (Q5/BUG-ARCH-003)** — ponytail: `TransactionEntity` keep JPA, `domain/model/Transaction` VO when strict hex needed (ArchUnit forbids `jakarta.persistence` in domain, upgrade: add `TransactionDomain` + `TransactionPersistencePort` return domain + `Money` via `api-commons`) | ArchUnit deferred, 142/142 green after split |
| TXN-HARDEN-003 | transaction-service / ADR-0060 + ADR-0041 | **Inbox + Result Table + Outbox outside-TX (Q4/Q6)** — ponytail: `FOR UPDATE` keep, inbox `referenceNo` dedup via `idempotency_key` unique already; add `inbox_events` + `aggregate_results` + outbox outside-TX when rail replay scale needed | Replay 2× same `referenceNo` → 1 commit deferred |
| TXN-HARDEN-004 | transaction-service / ADR-0060 + PADG 14/2025 | **Reconciliation job (Q4)** — ponytail: `ShedLock` `usingDbTime` via existing `shedlock` table; add `ReconciliationScheduler` `@SchedulerLock` + `GET /snap/v1.0/transfer/status` when BI-FAST prod creds live | Scheduler lock log deferred |
| TXN-HARDEN-005 | transaction-service / ADR-0060 + ADR-0042 | **Resilience & scheduling correctness** — ponytail: `Resilience4j` per-rail `CircuitBreaker/Retry/Bulkhead` via `resilience-starter` when rail latency observed; `ShedLockConfig usingDbTime()+forceUtcTimeZone()` when DB clock drift risk | `actuator/metrics` per-rail deferred |
| TXN-HARDEN-006 | transaction-service / ADR-0060 + ADR-0025 | **Callback HMAC + mTLS ready** — ponytail: `CallbackSignatureFilter` HMAC keep, `FOR UPDATE` keep, prod mTLS via `security-starter` Vault when BI-FAST prod; package `transfer/disbursement/va/splitbill/routing` ArchUnit deferred | `VirtualAccountServiceTest` paket isolation deferred |
| ACC-HARDEN-002 | account-service / ADR-0061 + ADR-0040 | **PII encrypt + blind index + KMS per-tenant (Q2)** — ponytail: `EncryptedStringConverter` AES-GCM + `pgcrypto` NIK + `V105 email_hash/phone_hash` already via `BlindIndexService`; KMS `alias/payu/<tenant>` BYOK when tier-1 HSM needed | `findByEmail` via blind index already O(1), KMS BYOK deferred |
| ACC-HARDEN-003 | account-service / ADR-0061 + ADR-0041 + ADR-0049 | **Lifecycle + balance reconcile (Q3)** — ponytail: `AccountStatus` + `Pocket` close `balance==0` already live, `accounts.balance 19,4` cached; nightly `SUM(ledger)` vs `accounts.balance` drift alert when ledger scale needed | ArchUnit top-level enum already, reconciler deferred |
| COMPLIANCE-HARDEN-001 | compliance-service / ADR-0063 | **AML/CFT + PCI-DSS Req10 audit trail** — `DataAccessAudit` append-only hex stack + structured JSON `traceId` live, `WORM` via `audit-syslog` rsyslog `5514:514` live. **B1 CLOSED 1.18.9**: `V4 rename compliance_standard→standard` `V5 REVOKE UPDATE,DELETE` pada `audit_reports/compliance_checks/data_access_audits` untuk `payu/payu_test/sit/uat/preprod` (71 green) — klaim V3 salah sudah dikoreksi. Sisa WORM 1y/7y via Loki KMS deferred | `REVOKE` DONE 1.18.9, WORM KMS deferred |
| GATEWAY-HARDEN-001 | gateway-service / ADR-0064 | **3scale APIcast edge + rate limiting** — ponytail: `gateway-service` Hot Rod `tryLock` ShedLock-lite via `GatewaySchedulerLock` already (1.13.8), `edge limiting` via 3scale `leaky_bucket/fixed_window` when prod `user_keys` scale 1000-apps burst | `edge_limited_total` deferred, HotRod lock keep |
| PORTAL-HARDEN-001 | api-portal-service / ADR-0065 | **OpenAPI aggregation DX** — ponytail: `GroupedOpenApi` SpringDoc + `ApiPortalService` TTL `PT5M` partial-failure already (1.13.0), `x-data-threescale-name` + Pact CI when 3scale ActiveDocs prod | `refreshCache` partial 1/N down still 200 already, Pact deferred |
| LLM-HARDEN-001 | ai / ADR-0067 + ADR-0036 | **LLM RAG + guardrails private — DEFERRED NO-GO (B4.6 2026-08-24)** — ADR-0067 BPPD+FF3-1+vLLM+pgvector+3scale+NIST AI RMF **Deferred ponytail YAGNI**: cost 1×GPU + OpenShift AI + pgvector + 300ms FPE + quota `ExceededNodeResources 23 svcs` vs benefit 5k/mo triage rule/heuristic belum breach, residency sudah `EncryptedStringConverter` tanpa LLM, 0 artifacts (audit 2026-08-24). No code — decision di `infrastructure/platform/mlops/README.md` + `infrastructure/platform/data/pgvector/README.md`; go criteria: GPU quota `payu-mlops` + validated demand + pgvector approved, re-evaluasi Q. ADR-0067 Proposed→Deferred. | Deferred — no manifest, ADR-0067 Deferred 2026-08-24 |
| KEDA-HARDEN-001 | platform / ADR-0068 + ADR-0042 | **KEDA event-driven autoscaling — GO Accepted (B4.6 2026-08-24) → RH CMA 2.19.0 1.18.15** — manifest minimal DONE `infrastructure/platform/keda/base/` (`namespace` + `keda-operator 2.14 HA PDB` + `TriggerAuthentication Vault payu/prod/kafka` + `scaledobject-core lagThreshold 10 min3 max10 prometheus 1000 QPS polling 15s cooldown 30s fallback 3` + `scaledobject-sim min0 lag5`) + overlays `dev min1 max3 / prod min3 max10`, **RH Custom Metrics Autoscaler Operator** `openshift-keda` `Subscription redhat-operators stable` `KedaController Installation Succeeded v2.19.0` `helm uninstall kedacore 2.14 -n keda` `oc apply -k rh-custom-metrics-autoscaler` `oc apply -k keda/base` `5 ScaledObjects 3/5 True` `HPA 5` `prod overlay fix wallet prometheus + va/biller bootstrap payu-prod`. | **DONE 1.18.15** `RH CMA 2.19.0` `openshift-keda 4/4` `payu-dev 5 ScaledObjects` `kcat test pending Vault payu-vault NotFound`. |

### P2 — Defer (Out-of-Scope MVP, ADR-0023)

No open P2 — 8 items CLOSED 2026-08-12 (CB-008/011/017/022/024/025/031/036) → `CHANGELOG.md` `1.10.63`.

### P3 — Backlog Lanjutan

| Key | Domain | Item |
|:---|:---|:---|

---

## 🏦 Partner Service Production Readiness Gate

Status `partner-service` hanya Production Ready setelah seluruh gate memiliki bukti live. `PARTNER-001..006` CLOSED (2026-08-08).

| Gate | Pri | Status | Sisa |
|:---|:---:|:---|:---|
| PARTNER-PROD-007 | P1 | ⏸️ Belum | HPA≥3, PDB minAvailable 2, topology spread, bounded timeout — locks via [ADR-0042](../adr/0042-distributed-job-scheduling-and-cluster-wide-concurrency-lock-standard-using-shedlock.md) |
| PARTNER-PROD-008 | P0 | ✅ Selesai 1.18.9 | PG HA+PITR via CNPG Barman Cloud ([ADR-0031](../adr/0031-database-resilience-pitr-and-disaster-recovery.md)), restore drill, RPO=0/RTO<5m — `payu-dev 3/3 2/2 Healthy` `barman-cloud 1/1` `ObjectStore payu-database-backup` `s3://payu-backups-368694075944/payu-database 9 WAL` `ContinuousArchiving True` `LimitRange 20Gi + ResourceQuota 150` `S3 bucket + IAM payu-backup` |
| PARTNER-PROD-009 | P1 | ⏸️ Belum | SLI/SLO, dashboard+alert, traces E2E ([ADR-0034](../adr/0034-end-to-end-observability-slo-sli-and-distributed-tracing-standard.md)) |
| PARTNER-PROD-010 | P0 | ⏸️ Belum | Contract/k6/chaos ([ADR-0024](../adr/0024-chaos-engineering-and-fault-injection-strategy.md)), pentest, sign-off |
| PARTNER-PROD-011 | P1 | ✅ Selesai 1.18.10 | Dual-control (Maker-Checker) onboarding — `V21 dual_control_maker_checker` `maker_id<>checker_id CHECK` `PENDING_APPROVAL→ACTIVE/REJECTED` `356 green` `SlaScheduler T+4j Telegram T+24j Page payu.partner.sla-*.v1` `runbook PARTNER_ONBOARDING_RUNBOOK.md` |

> Local APIcast (profile `api-management`) tidak bisa authless — public edge butuh APIManager (cluster-level).

---

## 🚀 Platform Deploy Queue

| Key | Pri | Category | Summary |
|:---|:---:|:---|:---|

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

---

## 🏛️ Architecture Decision Records (ADR) Governance & Backlog

> Hasil audit strategis (`principal-architect`): Penyelarasan status ADR, gap implementasi, ADR baru, dan anti-pattern.

### 1. 🔄 ADR Status Alignment & Maintenance (Drift Dokumen)

Drift ditemukan audit sweep 2026-08-24 (70 ADR vs repo): 3 klaim bukti salah dikoreksi di P1 harden table (`COMPLIANCE-HARDEN-001` REVOKE, `LLM-HARDEN-001` redteam script, `KEDA-HARDEN-001` kcat→HPA) — lihat tabel §2 untuk registry gap lengkap. Index `docs/adr/README.md` current.

### 2. 🔴 ADR yang Sudah Ada tapi Belum / Sebagian Diimplementasikan

| Key | ADR Terkait | Domain | Ref Backlog Tunggal | Pri |
|:---|:---|:---|:---|:---:|
| ADR-GAP-003..009 | [ADR-0028](../adr/0028-step-up-authentication-and-dynamic-linking-standard.md) s/d [ADR-0034](../adr/0034-end-to-end-observability-slo-sli-and-distributed-tracing-standard.md) | auth/wallet/risk/platform/security/data/observability | → **HARDEN backlog P1** (TXN/ACC/AUTH/COMPLIANCE/GATEWAY/PORTAL) — jangan duplikasi deskripsi di sini | B1–B3 |
| ADR-GAP-015 | [ADR-0015](../adr/0015-process-automation-rhpam.md) | lending/process | Phase 1 embedded Drools live (`rules-starter` + `credit_scoring.drl`); BPMN/Kogito runtime *not applied* (`kogito-runtime.yaml`), `TaskInboxController:40` proxy `/usertasks` tanpa target; fork `lending-rules` duplikat | B3 |
| ADR-GAP-019 | [ADR-0019](../adr/0019-statement-dual-format.md) | statement | PDF + JSON partner API live; **ekspor CSV absen** padahal judul ADR "PDF + JSON/CSV" | B4 |
| ADR-GAP-028W | [ADR-0028](../adr/0028-step-up-authentication-and-dynamic-linking-standard.md) | auth/txn | Step-up engine live di auth-service; **wired to transaction flow** `StepUpVerificationPort` `StepUpVerificationAdapter` `POST /internal/v1/auth/step-up/verify` `SHA-256 digest` `payloadDigest` `challengeId+pin` `PENDING_STEP_UP` `19 green` `requiresStepUp 40-70 or >10M` `dynamic linking` live | **B2 CLOSED 1.18.10** |
| ADR-GAP-029 | [ADR-0029](../adr/0029-iso20022-interbank-clearing-and-suspense-ledgering.md) | wallet/integration | COA suspense seeded (V115); `WalletClearingService` **persisted double-entry** via `JournalPersistencePort` `V119/V120` `4 green` (hold/settle/reverse balanced + idempotent), wire ke transfer pending next rail iteration; `pacs.008/pain.001` mapping deferred (ponytail) | **B1 CLOSED 1.18.9** |
| ADR-GAP-030E | [ADR-0030](../adr/0030-realtime-transaction-velocity-and-aml-risk-scoring.md) | risk | Detection live (`analytics-service fraud_detection.py`); **enforcement live**: `VelocityGuard` Redis lua ZSET `5/10m 50M/day` `isAllowed` fail-secure `false` + `RiskEvaluationAdapter` `POST /fraud/score` `22 green` fail-closed HOLD (80) → `PENDING_COMPLIANCE_REVIEW` | **B1 CLOSED 1.18.9** |
| ADR-GAP-032W | [ADR-0032](../adr/0032-perimeter-security-waf-coraza-and-siem-wazuh.md) | security | Wazuh helm + CLF RFC5424 sink live; **Coraza live** `coraza-waf 2/2 Running` `ConfigMap coraza.conf CRS PL1/PL2 threshold 5` `waf-proxy.py` `XSS 403` `SQLi 403` `Loki+Wazuh syslog <134> RFC5424` `Route coraza-waf-payu-dev` `Service coraza-waf:8080` `PDB 1` | **B2 CLOSED 1.18.10** |
| ADR-GAP-047 | [ADR-0047](../adr/0047-frontend-nominal-branded-types-and-strict-financial-money-precision-standard.md) | frontend/web-app | Branded types ada tapi deviasi spek: `__brand?` optional, `Money` = plain string alias, tanpa eslint enforcement | B4 |
| ADR-GAP-048 | [ADR-0048](../adr/0048-lending-eligibility-and-pricing-via-dmn-decision-tables.md) | lending | **0 file `.dmn` repo-wide**; eligibility/pricing hardcode (`LoanPreApprovalService.java:41,144`); modul fork `lending-rules` belum didelete (step 6 ADR) | B3 |
| ADR-GAP-054C | [ADR-0054](../adr/0054-dispute-and-chargeback-standard.md) | dispute | Refund + dispute state machine live; **chargeback 0 kode** di dispute-service (hanya deskripsi katalog) | B4 |
| ADR-GAP-056 | [ADR-0056](../adr/0056-simulator-fidelity-and-contract-testing-standard.md) | qa/simulators | Idempotency dedup + Pact broker & `pact-verify` task CI wired; **0 kontrak Pact**, `FAIL_ON_NO_PACTS=false`; header `X-Simulate` & QR EMVCo TLV CRC16 absen | B3 |

> **Urutan eksekusi sweep 2026-08-24** (blast radius × ireversibilitas × dependensi):
> **B1 — SELESAI 1.18.9:** fix PITR barman+S3+restore drill ([ADR-0031] → PARTNER-PROD-008 P0) · suspense ledger persist + wire ke transfer ([ADR-0029], GAP-029) · risk enforcement wire ([ADR-0030], GAP-030E) · migration `REVOKE UPDATE,DELETE` audit ([ADR-0063] → COMPLIANCE-HARDEN-001) — **4/4 CLOSED**
> **B2 — SELESAI 1.18.10:** step-up wiring + dynamic linking ([ADR-0028], GAP-028W) · dual-control onboarding ([ADR-0035] → PARTNER-PROD-011) · Coraza WAF deploy nyata (GAP-032W) — **3/3 CLOSED**
> **B3 — correctness infra:** reconciliation job + inbox_events (TXN-HARDEN-003/004) · isi kontrak Pact lalu `FAIL_ON_NO_PACTS=true` (GAP-056) · RLS FORCE rollout sisa service · cleanup/wire Kogito TaskInbox (GAP-015) · DMN lending (GAP-048)
> **B4 — menunggu trigger / keputusan: SISA setelah B4.6 2026-08-24:** CSV export (GAP-019) · branded types ketat + lint (GAP-047) · chargeback (GAP-054C) · verifikasi SLO live (PARTNER-PROD-009) · apply topik cluster (blokir OCP creds); **B4.6 CLOSED 2026-08-24: LLM DEFERRED no-code, KEDA GO manifest DONE → apply+test pending**.

### 3. 📝 Backlog ADR Baru yang Perlu Dibuat

> **Update 2026-08-19**: ADR-0035..0066 Accepted (32 ADRs) — lihat `docs/adr/README.md`. Sisa 2 Proposed di bawah.

| — | **ADR-0067** | 📝 **LLM Integration — DEFERRED 2026-08-24 (B4.6 NO-GO ponytail)** — `infrastructure/platform/mlops/README.md` decision log, `infrastructure/platform/data/pgvector/README.md` deferred, `docs/adr/README.md` Deferred. Re-evaluasi Q when GPU quota + demand. | **Deferred** |
| — | **ADR-0068** | 📝 **KEDA Autoscaling — ACCEPTED 2026-08-24 (B4.6 GO)** — `infrastructure/platform/keda/base/` + overlays `dev/prod` manifests DONE (`ScaledObject` lag 10, `TriggerAuthentication` Vault). Next `oc apply -k` + `kcat produce 1000 → HPA 3→10 <30s`. | **Accepted** |

### 4. ⚠️ Kesenjangan Best Practice & Anti-Pattern yang Memerlukan Remediasi

No open gap — harden items (TXN/ACC/AUTH/...) cover best-practice remediation; track di P1 harden table.
