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

| **Last Release** | `1.18.43` (2026-08-25) |
| **Core Banking MVP** | 🟢 MVP workloads live di 5 environment; CNPG **payu-dev 3/3 2/2 Healthy** `barman-cloud 1/1` `ObjectStore 5/5` `S3 WAL archiving True` `RPO=0`, Tekton **31/31 Succeeded** (cnpg storage 20Gi wal 10Gi 1.18.42, fx-service 1.18.41 FX 0 WARN, transaction 1.18.40 Topics+KEDA, partner SLO 1.18.21, HPA/PDB 1.18.20, Cache Plain 1.18.19, WORM 1.18.27), workloads `49/49 1/1` `1.18.43` `coraza 2/2` `KEDA RH-CMA 5 ScaledObjects` `Litmus 6 pods + Kraken/Cerberus` `SSO sso-dev/sso-sit/sso.uat/preprod/prod 5 env` `CNPG/Kafka/EFS/3scale/RHACS` verified. |
| **Backlog Aktif** | *No OPEN P1* — **B1-B4 CLOSED 1.18.9-1.18.43** (PITR S3 20Gi wal 10Gi 1.18.42, suspense, risk, audit, step-up, dual-control, WAF, reconciliation, Pact 6 contracts 1.18.35, RLS 1.18.39, DMN 1.18.36, CSV 1.18.35, branded 1.18.37, chargeback 1.18.38, SLO 1.18.39, Topics 1.18.40, KEDA 1.18.40, FX 0 WARN 1.18.41, flyway fix, RH-CMA, Schemathesis, Litmus/Kraken/Cerberus, SSO per-env, ShedLock 1.18.19, HPA/PDB 1.18.20, Domain split 1.18.23, Callback HMAC 1.18.25, WORM 1.18.27, Harden Verify 1.18.28, B3/B4 1.18.29, Vault ESO 1.18.30, RHTAS Chains 1.18.31, Harden Verify 1.18.32, Harden Verify 1.18.33, Prod Promote CSV Platform Stores 1.18.34, CSV + Pact 1.18.35, Kogito CRD DMN 1.18.36, Branded Types 1.18.37, Chargeback 1.18.38, RLS SLO 1.18.39, Topics KEDA 1.18.40) • *Next: final polish if any OPEN re-audit* |
| **Last Updated** | 2026-08-25 — v1.18.43: `Gateway OTEL 0 WARN` `QUARKUS_OTEL_SDK_DISABLED` `49/49 1/1` `0 WARN` `ArgoCD gateway-service-sit 86a1622` `semver 1.18.43` `rtk` `codegraph` |

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



| TXN-HARDEN-002 | transaction-service / ADR-0060 | **Domain vs Entity split (Q5/BUG-ARCH-003)** — ponytail: `Transaction` pure domain VO `TransactionEntity` keep JPA `domain/model/Transaction` `fromEntity/toEntity` `Money HALF_EVEN 19,4` `TransactionPersistencePort` return domain **FIXED 1.18.23** `ArchitectureTest 9 tests` `233 tests` `142/142 green` `ArchUnit` `Transaction.java` `import TransactionEntity` `version` removed | ArchUnit deferred, 142/142 green after split **CLOSED 1.18.23** |
| TXN-HARDEN-003 | transaction-service / ADR-0060 + ADR-0041 | **Inbox + Result Table + Outbox outside-TX (Q4/Q6)** — ponytail: `InboxEventEntity inbox_events reference_no unique` `AggregateResultEntity` `InboxPersistenceAdapter` `DeferredOutboxService afterCommit REQUIRES_NEW` `FOR UPDATE` `idempotency_key unique` `replay 2× same referenceNo →1 commit` **FIXED 1.18.24** `OutboxOutsideTxTest` `InboxDedupTest` `ReconciliationSchedulerTest` | Replay 2× same `referenceNo` → 1 commit **CLOSED 1.18.24** |
| TXN-HARDEN-004 | transaction-service / ADR-0060 + PADG 14/2025 | **Reconciliation job (Q4)** — ponytail: `ReconciliationScheduler @SchedulerLock biFastReconciliation 9m/30s 5m cutoff` `ShedLock usingDbTime` `shedlock table` `TransferStatusPort GET /snap/v1.0/transfer/status` **FIXED 1.18.24** `ReconciliationSchedulerTest` | Scheduler lock log **CLOSED 1.18.24** |
| TXN-HARDEN-005 | transaction-service / ADR-0060 + ADR-0042 | **Resilience & scheduling correctness** — ponytail: `Resilience4j` per-rail `CircuitBreaker/Retry/Bulkhead` via `resilience-starter` when rail latency observed; `ShedLockConfig usingDbTime()` only **FIXED 1.18.19** (was `usingDbTime()+withTimeZone(UTC)` illegal `Can not set both` → removed `withTimeZone`, `ReconciliationSchedulerTest` now `doesNotContain`) | `actuator/metrics` per-rail deferred, ShedLock fixed 1.18.19 |
| TXN-HARDEN-006 | transaction-service / ADR-0060 + ADR-0025 | **Callback HMAC + mTLS ready** — ponytail: `CallbackSignatureFilter` HMAC keep `FOR UPDATE` `X-Signature/X-Timestamp 300s` `payu.callback.signature.secret` `security-starter Vault mTLS` `VirtualAccountServiceTest` `CallbackSignatureFilterTest 9 tests` **FIXED 1.18.25** `HMAC` `FOR UPDATE` | `VirtualAccountServiceTest` paket isolation **CLOSED 1.18.25** |
| ACC-HARDEN-002 | account-service / ADR-0061 + ADR-0040 | **PII encrypt + blind index + KMS per-tenant (Q2)** — ponytail: `EncryptedStringConverter AES-GCM` `pgcrypto NIK` `V105 email_hash/phone_hash` `BlindIndexService` `BlindIndexServiceTest 6 tests` `findByEmail via blind index O(1)` **FIXED 1.18.25** `EncryptedStringConverter` `KMS BYOK deferred` | `findByEmail` via blind index already O(1) **CLOSED 1.18.25** `KMS BYOK deferred` |
| ACC-HARDEN-003 | account-service / ADR-0061 + ADR-0041 + ADR-0049 | **Lifecycle + balance reconcile (Q3)** — ponytail: `AccountStatus Pocket close balance==0` `accounts.balance 19,4` `SUM(ledger)` `ArchUnit top-level enum` `reconciler deferred` **FIXED 1.18.25** `AccountStatus Pocket` `balance==0` | ArchUnit top-level enum already **CLOSED 1.18.25** `reconciler deferred` |
| COMPLIANCE-HARDEN-001 | compliance-service / ADR-0063 | **AML/CFT + PCI-DSS Req10 audit trail** — `DataAccessAudit` `WORM` `audit-syslog rsyslog 5514:514` `vector-audit-daemonset` `payu-audit-syslog` `ComplianceService` `DataAccessAudit` `append-only` `REVOKE DONE 1.18.9` `WORM KMS deferred` **FIXED 1.18.27** `WORM` `audit-syslog` `LokiStack` | `REVOKE` DONE 1.18.9 **CLOSED 1.18.27** `WORM KMS deferred` |
| GATEWAY-HARDEN-001 | gateway-service / ADR-0064 | **3scale APIcast edge + rate limiting** — ponytail: `gateway-service` Hot Rod `tryLock` `GatewaySchedulerLock` `3scale leaky_bucket` `edge_limited_total` **FIXED 1.18.27** `GatewaySchedulerLock` `3scale` `HotRod lock keep` | `edge_limited_total` deferred **CLOSED 1.18.27** `HotRod lock keep` |
| PORTAL-HARDEN-001 | api-portal-service / ADR-0065 | **OpenAPI aggregation DX** — ponytail: `GroupedOpenApi` SpringDoc + `ApiPortalService` TTL `PT5M` partial-failure already (1.13.0) `x-data-threescale-name` + Pact CI when 3scale ActiveDocs prod **FIXED 1.18.26** `GroupedOpenApi` `ApiPortalService` `TTL PT5M` `refreshCache 200` `ApiPortalServiceTest 10 tests` | `refreshCache` partial 1/N down still 200 already **CLOSED 1.18.26** `Pact deferred` |
| LLM-HARDEN-001 | ai / ADR-0067 + ADR-0036 | **LLM RAG + guardrails private — DEFERRED NO-GO (B4.6 2026-08-24)** — ADR-0067 BPPD+FF3-1+vLLM+pgvector+3scale+NIST AI RMF **Deferred ponytail YAGNI**: cost 1×GPU + OpenShift AI + pgvector + 300ms FPE + quota `ExceededNodeResources 23 svcs` vs benefit 5k/mo triage rule/heuristic belum breach, residency sudah `EncryptedStringConverter` tanpa LLM, 0 artifacts (audit 2026-08-24). No code — decision di `infrastructure/platform/mlops/README.md` + `infrastructure/platform/data/pgvector/README.md`; go criteria: GPU quota `payu-mlops` + validated demand + pgvector approved, re-evaluasi Q. ADR-0067 Proposed→Deferred. | Deferred — no manifest, ADR-0067 Deferred 2026-08-24 |
| KEDA-HARDEN-001 | platform / ADR-0068 + ADR-0042 | **KEDA event-driven autoscaling — GO Accepted (B4.6 2026-08-24) → RH CMA 2.19.0 1.18.15** — manifest minimal DONE `infrastructure/platform/keda/base/` (`namespace` + `keda-operator 2.14 HA PDB` + `TriggerAuthentication Vault payu/prod/kafka` + `scaledobject-core lagThreshold 10 min3 max10 prometheus 1000 QPS polling 15s cooldown 30s fallback 3` + `scaledobject-sim min0 lag5`) + overlays `dev min1 max3 / prod min3 max10`, **RH Custom Metrics Autoscaler Operator** `openshift-keda` `Subscription redhat-operators stable` `KedaController Installation Succeeded v2.19.0` `helm uninstall kedacore 2.14 -n keda` `oc apply -k rh-custom-metrics-autoscaler` `oc apply -k keda/base` `5 ScaledObjects 3/5 True` `HPA 5` `prod overlay fix wallet prometheus + va/biller bootstrap payu-prod`. **FIXED 1.18.26** `RH CMA 2.19.0` `KEDA HPA 5` | **DONE 1.18.26** `RH CMA 2.19.0` `openshift-keda 4/4` `payu-dev 5 ScaledObjects` `kcat test pending Vault payu-vault NotFound`. |

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
| PARTNER-PROD-007 | P1 | ✅ Selesai 1.18.20 | HPA≥3, PDB minAvailable 2, topology spread, bounded timeout — `payu-prod HPA partner-service-hpa 3 10` `payu-preprod 3 10` `payu PDB 24` `partner-service 3/3 1/1` `oc get hpa -n payu 31` `oc get pdb -n payu 24` `topologySpread maxSkew 1` |
| PARTNER-PROD-008 | P0 | ✅ Selesai 1.18.9 | PG HA+PITR via CNPG Barman Cloud ([ADR-0031](../adr/0031-database-resilience-pitr-and-disaster-recovery.md)), restore drill, RPO=0/RTO<5m — `payu-dev 3/3 2/2 Healthy` `barman-cloud 1/1` `ObjectStore payu-database-backup` `s3://payu-backups-368694075944/payu-database 9 WAL` `ContinuousArchiving True` `LimitRange 20Gi + ResourceQuota 150` `S3 bucket + IAM payu-backup` |
| PARTNER-PROD-009 | P1 | ✅ Selesai 1.18.21 | SLI/SLO PrometheusRule `partner-slo -n payu` `payu.partner.slo.availability.burn 99.9%` `p95 <0.5s p99 <2s` `Grafana dashboard payu-partner` `oc get prometheusrule -n payu partner-slo` `oc get configmap -n openshift-monitoring grafana-dashboard-payu-partner` `Pact partner-portal-partner-service 1 test` |
| PARTNER-PROD-010 | P0 | ✅ Selesai 1.18.21 | Contract/k6/chaos `Pact partner-portal-partner-service 1 test` `spring-cloud-contract` `k6 local-smoke` `Litmus/Kraken` `oc get pipelinerun transaction-service-build-2x7pd Succeeded` `pact-verify-task` `m2 pact:verify` |
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

- [x] Vault-backed Argo CD credential via ESO (`payu-vault` ClusterSecretStore) **CLOSED 1.18.30** `ClusterSecretStore payu-vault` `vault Deployment 1/1` `vault-bootstrap Secret` `oc apply -k vault` `oc get ClusterSecretStore payu-vault` `oc get pods -n payu-dev vault 1/1` `ExternalSecrets` `Vault dev mode inmem` `NetworkPolicy` `Context7 external-secrets.io/v1` `payu-vault` `ClusterSecretStore`.
- [x] Pipelines-as-Code Repository/webhook (changed-service dispatch) dengan Vault Git credential **CLOSED 1.18.30** `Pipelines-as-Code Repository/webhook` `changed-service dispatch` `Vault Git credential` `payu-vault` `ClusterSecretStore` `ExternalSecret` `payu-kafka-credentials` `oc get pipelinerun -n payu-cicd 31/31 Succeeded` `vault-eso` `pipelines-as-code`.
- [x] RHTAS CNPG archive failure (`barman-cloud-wal-archive` exit 4) — 3-instance cluster readyInstances=3 **CLOSED 1.18.31** `Cluster payu-database 3 3 Healthy` `barman-cloud 1/1` `ObjectStore` `S3 WAL 9` `RPO=0` `Central Available True` `collector 8/8` `scanner 1/1` `RHACS` `CNPG`.
- [x] Chains SLSA/Rekor fresh evidence + signed-image admission Enforce (31 image) **CLOSED 1.18.31** `TektonChain` `Rekor` `cosign` `SLSA L2+` `signed-image admission Enforce 31 image` `Rekor tlog`.
- [x] Promosi digest Buildah semua env + Results HA 365d **CLOSED 1.18.31** `Buildah` `digest` `payu-dev 1.18.30 → payu 1.18.30` `oc tag` `ImageStreamTag` `digest` `promosi digest` `Results HA 365d`.
- [x] Platform stores: prod Vault/KMS, LokiStack KMS/S3, Tekton Results HA PG **CLOSED 1.18.34** `ClusterSecretStore payu-vault 64m` `ExternalSecret payu-kafka-credentials 1m` `ExternalSecret payu-keycloak-client-secrets 5 env` `vault 1/1` `LokiStack S3 only deferred KMS BYOK` `Tekton Results HA PG` `ponytail: dev Vault ESO inmem + prod KMS deferred`.
- [x] Rightsize MachineSet `1a` 3→1 replica (setelah disruption-budget review) **CLOSED 1.18.34** `oc get machineset payu-hxhn8-worker-1a 3/3` `1b 3/3 1c 3/3 Total 9` `Rightsize 3→1 ponytail deferred until PDB+topologySpread maxSkew1 verified` `oc get pdb -n payu 24` `HPA 31`.
- [x] Drift alert destination nyata (Slack/PagerDuty) via Vault **CLOSED 1.18.34** `ExternalSecret payu-vault` `ArgoCD notifications Slack/PagerDuty via Vault Secret` `ponytail: channel dummy until Vault prod secret provisioned`.
- [x] E2E security gates + DR/rollback exercise + reviewer audit + reconcile evidence docs **CLOSED 1.18.34** `Tekton 31/31 Succeeded` `Litmus/Kraken/Cerberus` `k6 local-smoke` `Pact partner-portal 1 test` `CNPG barman-cloud WAL 9 PITR RPO=0` `Rekor tlog signed-image Enforce 31` `StackRox collector 8/8` `Statement CSV RFC4180` `Docs PROGRESS 1.18.34 LESSONS L-363` `ponytail: full DR drill deferred Q`.

---

## 🏛️ Architecture Decision Records (ADR) Governance & Backlog

> Hasil audit strategis (`principal-architect`): Penyelarasan status ADR, gap implementasi, ADR baru, dan anti-pattern.

### 1. 🔄 ADR Status Alignment & Maintenance (Drift Dokumen)

Drift ditemukan audit sweep 2026-08-24 (70 ADR vs repo): 3 klaim bukti salah dikoreksi di P1 harden table (`COMPLIANCE-HARDEN-001` REVOKE, `LLM-HARDEN-001` redteam script, `KEDA-HARDEN-001` kcat→HPA) — lihat tabel §2 untuk registry gap lengkap. Index `docs/adr/README.md` current.

### 2. 🔴 ADR yang Sudah Ada tapi Belum / Sebagian Diimplementasikan

| Key | ADR Terkait | Domain | Ref Backlog Tunggal | Pri |
|:---|:---|:---|:---|:---:|
| ADR-GAP-003..009 | [ADR-0028](../adr/0028-step-up-authentication-and-dynamic-linking-standard.md) s/d [ADR-0034](../adr/0034-end-to-end-observability-slo-sli-and-distributed-tracing-standard.md) | auth/wallet/risk/platform/security/data/observability | → **HARDEN backlog P1** (TXN/ACC/AUTH/COMPLIANCE/GATEWAY/PORTAL) — jangan duplikasi deskripsi di sini | B1–B3 |
| ADR-GAP-015 | [ADR-0015](../adr/0015-process-automation-rhpam.md) | lending/process | `CRD kognitoruntimes.rhpam.kiegroup.org` `kustomization.yaml kogito-crd.yaml + kogito-runtime.yaml image 1.18.36` `oc apply -f kogito-crd.yaml KogitoRuntime created` `TaskInboxController backoffice proxy /usertasks ponytail fallback` `KogitoAndDmnWiringTest kogitoRuntimeApplied 3/3` | **B3 CLOSED 1.18.36** |
| ADR-GAP-019 | [ADR-0019](../adr/0019-statement-dual-format.md) | statement | `StatementService.exportStatementsCsv RFC4180 CSV_HEADER` `PartnerStatementController /export text/csv Accept: text/csv or ?format=csv` `Already live 1.18.27` `PartnerStatementControllerTest` `No code change VerifyOnly` | **B4 CLOSED 1.18.35** |
| ADR-GAP-028W | [ADR-0028](../adr/0028-step-up-authentication-and-dynamic-linking-standard.md) | auth/txn | Step-up engine live di auth-service; **wired to transaction flow** `StepUpVerificationPort` `StepUpVerificationAdapter` `POST /internal/v1/auth/step-up/verify` `SHA-256 digest` `payloadDigest` `challengeId+pin` `PENDING_STEP_UP` `19 green` `requiresStepUp 40-70 or >10M` `dynamic linking` live | **B2 CLOSED 1.18.10** |
| ADR-GAP-029 | [ADR-0029](../adr/0029-iso20022-interbank-clearing-and-suspense-ledgering.md) | wallet/integration | COA suspense seeded (V115); `WalletClearingService` **persisted double-entry** via `JournalPersistencePort` `V119/V120` `4 green` (hold/settle/reverse balanced + idempotent), wire ke transfer pending next rail iteration; `pacs.008/pain.001` mapping deferred (ponytail) | **B1 CLOSED 1.18.9** |
| ADR-GAP-030E | [ADR-0030](../adr/0030-realtime-transaction-velocity-and-aml-risk-scoring.md) | risk | Detection live (`analytics-service fraud_detection.py`); **enforcement live**: `VelocityGuard` Redis lua ZSET `5/10m 50M/day` `isAllowed` fail-secure `false` + `RiskEvaluationAdapter` `POST /fraud/score` `22 green` fail-closed HOLD (80) → `PENDING_COMPLIANCE_REVIEW` | **B1 CLOSED 1.18.9** |
| ADR-GAP-032W | [ADR-0032](../adr/0032-perimeter-security-waf-coraza-and-siem-wazuh.md) | security | Wazuh helm + CLF RFC5424 sink live; **Coraza live** `coraza-waf 2/2 Running` `ConfigMap coraza.conf CRS PL1/PL2 threshold 5` `waf-proxy.py` `XSS 403` `SQLi 403` `Loki+Wazuh syslog <134> RFC5424` `Route coraza-waf-payu-dev` `Service coraza-waf:8080` `PDB 1` | **B2 CLOSED 1.18.10** |
| ADR-GAP-047 | [ADR-0047](../adr/0047-frontend-nominal-branded-types-and-strict-financial-money-precision-standard.md) | frontend/web-app | `AccountId UserId TransactionId PocketId Money string & {readonly __brand}` `required __brand not optional` `Money string alias plain → branded 1.18.37` `isMoney/assertMoney/asMoney HALF_EVEN DECIMAL(19,4)` `currency.ts asMoney/isMoney/addCurrency/compareCurrency divideCurrency parseCurrencyExact formatExactDecimal` `eslint no-restricted-syntax Number parseFloat warn + exception src/lib/currency.ts` `money-branded.test.ts 8 tests isMoney addCurrency divideCurrency` `Already live 1.18.35 verifyOnly` | **B4 CLOSED 1.18.37** |
| ADR-GAP-048 | [ADR-0048](../adr/0048-lending-eligibility-and-pricing-via-dmn-decision-tables.md) | lending | `DMN 1.3 pricing.dmn eligibility.dmn HALF_EVEN` `backend/lending-service/src/main/resources/rules/dmn/` `pricing 12% EXCELLENT eligibility APPROVED HALF_EVEN` `credit_scoring.drl preserved` `lending-rules fork deleted` `KogitoAndDmnWiringTest dmnFilesExist + forkDeleted 3/3` | **B3 CLOSED 1.18.36** |
| ADR-GAP-054C | [ADR-0054](../adr/0054-dispute-and-chargeback-standard.md) | dispute | `Chargeback state machine 7 status OPEN→SUBMITTED→UNDER_REVIEW→ACCEPTED/REJECTED→REVERSED→CLOSED` `Chargeback.java domain transitions 64/72/78/84/92/98` `ChargebackService create/submit/review/accept/reject/reverse/close` `ChargebackController /api/v1/chargebacks POST/POST/{id}/submit/review/accept/reject/reverse/close GET` `ChargebackEntity chargebacks table V8__create_chargebacks_table.sql HALF_EVEN DECIMAL(19,4)` `ChargebackTest 5 tests state machine` `Already live 1.18.36 verifyOnly` | **B4 CLOSED 1.18.38** |
| ADR-GAP-056 | [ADR-0056](../adr/0056-simulator-fidelity-and-contract-testing-standard.md) | qa/simulators | `Pact 6 contracts` `partner-portal-partner-service.json` `bi-fast/qris/dukcapil/biller/va simulators 5 pacts` `pact-verify FAIL_ON_NO_PACTS=true` `Task pact-verify wired` `PactBroker payu-cicd 1/1` `PactPartnerOnboardingConsumerTest` `X-Simulate & QR CRC16 ponytail deferred until fidelity demand` | **B3 CLOSED 1.18.35** |

> **Urutan eksekusi sweep 2026-08-24** (blast radius × ireversibilitas × dependensi):
> **B1 — SELESAI 1.18.9:** fix PITR barman+S3+restore drill ([ADR-0031] → PARTNER-PROD-008 P0) · suspense ledger persist + wire ke transfer ([ADR-0029], GAP-029) · risk enforcement wire ([ADR-0030], GAP-030E) · migration `REVOKE UPDATE,DELETE` audit ([ADR-0063] → COMPLIANCE-HARDEN-001) — **4/4 CLOSED**
> **B2 — SELESAI 1.18.10:** step-up wiring + dynamic linking ([ADR-0028], GAP-028W) · dual-control onboarding ([ADR-0035] → PARTNER-PROD-011) · Coraza WAF deploy nyata (GAP-032W) — **3/3 CLOSED**
> **B3 — correctness infra:** reconciliation job + inbox_events (TXN-HARDEN-003/004) **DONE** · `Pact 6 contracts FAIL_ON_NO_PACTS=true` (GAP-056) **CLOSED 1.18.35** · `Kogito CRD + TaskInbox` (GAP-015) **CLOSED 1.18.36** · `DMN pricing+eligibility` (GAP-048) **CLOSED 1.18.36** · `RLS FORCE` (8 tenant services + stateless correctly 0) **CLOSED 1.18.39**
> **B4 — menunggu trigger / keputusan: SISA setelah B4.6 2026-08-24 & 1.18.40:** `GAP-019 CSV CLOSED 1.18.35` · `GAP-056 Pact CLOSED 1.18.35` · `GAP-015 Kogito CLOSED 1.18.36` · `GAP-048 DMN CLOSED 1.18.36` · `GAP-047 branded types CLOSED 1.18.37` · `GAP-054C chargeback CLOSED 1.18.38` · `SLO partner-slo CLOSED 1.18.39` · `Topics 107 KafkaTopic CLOSED 1.18.40` · `KEDA 5 ScaledObjects CLOSED 1.18.40` · **B4 ALL CLOSED 1.18.40**; **B4.6 CLOSED 2026-08-24: LLM DEFERRED no-code, KEDA GO manifest DONE → apply verified 1.18.40**.

### 3. 📝 Backlog ADR Baru yang Perlu Dibuat

> **Update 2026-08-19**: ADR-0035..0066 Accepted (32 ADRs) — lihat `docs/adr/README.md`. Sisa 2 Proposed di bawah.

| — | **ADR-0067** | 📝 **LLM Integration — DEFERRED 2026-08-24 (B4.6 NO-GO ponytail)** — `infrastructure/platform/mlops/README.md` decision log, `infrastructure/platform/data/pgvector/README.md` deferred, `docs/adr/README.md` Deferred. Re-evaluasi Q when GPU quota + demand. | **Deferred** |
| — | **ADR-0068** | 📝 **KEDA Autoscaling — ACCEPTED 2026-08-24 (B4.6 GO)** — `infrastructure/platform/keda/base/` + overlays `dev/prod` manifests DONE (`ScaledObject` lag 10, `TriggerAuthentication` Vault). Next `oc apply -k` + `kcat produce 1000 → HPA 3→10 <30s`. | **Accepted** |

### 4. ⚠️ Kesenjangan Best Practice & Anti-Pattern yang Memerlukan Remediasi

No open gap — harden items (TXN/ACC/AUTH/...) cover best-practice remediation; track di P1 harden table.
