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
| **Cluster Status** | 🟢 OCP 4.20.29, 8 nodes Ready. **5 environment hidup penuh** (dev/sit/uat/preprod/prod): 25 microservices + 5 simulators + web-app + Keycloak + PostgreSQL (CNPG) **5/5 Healthy** `payu-database-1 Running` + Kafka sit/uat True + DataGrid litmus Running, ArgoCD 1/1 Running (was CrashLoop OOM 4Gi fix), Tekton 31/31 per-service pipeline **dev hijau** `account-service-build-wmtfl Succeeded 15/18 3 Skipped` `zap 66 PASS k6 4780 0 failed` `cosign/rhacs non-blocking chain false` + `analytics-service-pipelinerun-d45hb Running` sequential polyrepo proven, 0 ERROR log. Platform: cert-manager 5/5 True, 3scale Available, RHACS Healthy, EFS 4/4, Litmus 1/1. |
| **Last Release** | `1.18.4` (2026-08-24) |
| **Core Banking MVP** | 🟢 MVP workloads live di 5 environment; CNPG 5/5 Healthy, Litmus live, Tekton dev Succeeded (pilot), sit/uat/preprod/prod Health proven, partner prod credentials queue remains. |
| **Backlog Aktif** | Chaos agent per-env (Litmus now Running but ChaosEngine per env pending), Schemathesis credentials, SSO per-env isolation (lihat Platform Backlog) |
| **Last Updated** | 2026-08-24 — v1.18.4: NetworkPolicy 5/5 allow-all + DNS 53 + ArgoCD 4Gi OOM fix + Tekton RBAC pipeline SA + CNPG 5/5 Healthy + Litmus + pipeline wmtfl Succeeded 15/18 (zap/k6) + analytics sequential 1.18.4. |

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

| SX-AUTH-001 | platform / Tekton | **Schemathesis gate pakai kredensial** — client-credentials dari `payu-keycloak-client-secrets` per env → header Bearer; nyalakan kembali `content_type_conformance` + `response_schema_conformance` yang kini di-exclude | Gate 5xx-only aktif |
| CHAOS-ENV-001 | platform / chaos | **Litmus agent + Kraken/Cerberus di namespace promoted** — pasang agent payu-sit/uat/preprod/payu agar ChaosEngine benar-benar dieksekusi; lengkapi RBAC cross-ns untuk SA pipeline (CHAOS-RBAC-001); lepas skip-infra pada kedua gate setelah live | Skip eksplisit saat infra absen |
| SSO-ENV-002 | platform / identity | **Isolasi Keycloak per-environment** — seed client secrets per env (realm import membaca `payu-keycloak-client-secrets`), lalu arahkan issuer/JWK workloads ke `sso-<env>` route | Saat ini semua env memakai SSO bersama dev |
| PROMOTE-003 | platform / Tekton | **Promotion run rutin per rilis** untuk seluruh service (bukan hanya pilot) — jalankan `<svc>-pipeline` target-env=sit→uat→preprod→prod saat tag baru dirilis; mekanik sudah terbukti | Pilot account-service selesai |

| TXN-HARDEN-002 | transaction-service / ADR-0060 | **Domain vs Entity split (Q5/BUG-ARCH-003)** — ponytail: `TransactionEntity` keep JPA, `domain/model/Transaction` VO when strict hex needed (ArchUnit forbids `jakarta.persistence` in domain, upgrade: add `TransactionDomain` + `TransactionPersistencePort` return domain + `Money` via `api-commons`) | ArchUnit deferred, 142/142 green after split |
| TXN-HARDEN-003 | transaction-service / ADR-0060 + ADR-0041 | **Inbox + Result Table + Outbox outside-TX (Q4/Q6)** — ponytail: `FOR UPDATE` keep, inbox `referenceNo` dedup via `idempotency_key` unique already; add `inbox_events` + `aggregate_results` + outbox outside-TX when rail replay scale needed | Replay 2× same `referenceNo` → 1 commit deferred |
| TXN-HARDEN-004 | transaction-service / ADR-0060 + PADG 14/2025 | **Reconciliation job (Q4)** — ponytail: `ShedLock` `usingDbTime` via existing `shedlock` table; add `ReconciliationScheduler` `@SchedulerLock` + `GET /snap/v1.0/transfer/status` when BI-FAST prod creds live | Scheduler lock log deferred |
| TXN-HARDEN-005 | transaction-service / ADR-0060 + ADR-0042 | **Resilience & scheduling correctness** — ponytail: `Resilience4j` per-rail `CircuitBreaker/Retry/Bulkhead` via `resilience-starter` when rail latency observed; `ShedLockConfig usingDbTime()+forceUtcTimeZone()` when DB clock drift risk | `actuator/metrics` per-rail deferred |
| TXN-HARDEN-006 | transaction-service / ADR-0060 + ADR-0025 | **Callback HMAC + mTLS ready** — ponytail: `CallbackSignatureFilter` HMAC keep, `FOR UPDATE` keep, prod mTLS via `security-starter` Vault when BI-FAST prod; package `transfer/disbursement/va/splitbill/routing` ArchUnit deferred | `VirtualAccountServiceTest` paket isolation deferred |
| ACC-HARDEN-002 | account-service / ADR-0061 + ADR-0040 | **PII encrypt + blind index + KMS per-tenant (Q2)** — ponytail: `EncryptedStringConverter` AES-GCM + `pgcrypto` NIK + `V105 email_hash/phone_hash` already via `BlindIndexService`; KMS `alias/payu/<tenant>` BYOK when tier-1 HSM needed | `findByEmail` via blind index already O(1), KMS BYOK deferred |
| ACC-HARDEN-003 | account-service / ADR-0061 + ADR-0041 + ADR-0049 | **Lifecycle + balance reconcile (Q3)** — ponytail: `AccountStatus` + `Pocket` close `balance==0` already live, `accounts.balance 19,4` cached; nightly `SUM(ledger)` vs `accounts.balance` drift alert when ledger scale needed | ArchUnit top-level enum already, reconciler deferred |
| AUTH-HARDEN-001 | auth-service / ADR-0062 + RHBK 26.4 | **DPoP sender-constrained (Q4)** — ponytail: `Bearer` + mTLS via `security-starter` live, DPoP `cnf.jkt` via RHBK 26.4 when public client strict proof needed | `DPoP` 401 nonce retry deferred |
| AUTH-HARDEN-002 | auth-service / ADR-0062 | **Refresh rotation strict + BFF (Q5)** — ponytail: `revokeRefreshToken=true` + `refreshTokenMaxReuse=0` via `RealmModel` when Keycloak 26.4 `payu` realm strict; BFF HttpOnly `__Host-` session already via ADR-0039 | Reuse old refresh → `BadCredentialsException` deferred |
| AUTH-HARDEN-003 | auth-service / ADR-0062 + ADR-0028 | **Flows + device binding (Q6)** — ponytail: `password`/`implicit` off via `directAccessGrantsEnabled:false` when `payu-web` constrained; PKCE S256 + Device Grant when RHBK flow migration ready | `directAccessGrantsEnabled:false` deferred |
| COMPLIANCE-HARDEN-001 | compliance-service / ADR-0063 | **AML/CFT + PCI-DSS Req10 audit trail** — ponytail: `REVOKE UPDATE,DELETE` + `DataAccessAudit` append-only via `V3` already, structured JSON `traceId` + `WORM` via `audit-syslog` rsyslog `5514:514`; dual approval `>50M` via `maker≠checker` when compliance rule engine ready | `REVOKE` check deferred, WORM 1y/7y via Loki KMS deferred |
| GATEWAY-HARDEN-001 | gateway-service / ADR-0064 | **3scale APIcast edge + rate limiting** — ponytail: `gateway-service` Hot Rod `tryLock` ShedLock-lite via `GatewaySchedulerLock` already (1.13.8), `edge limiting` via 3scale `leaky_bucket/fixed_window` when prod `user_keys` scale 1000-apps burst | `edge_limited_total` deferred, HotRod lock keep |
| PORTAL-HARDEN-001 | api-portal-service / ADR-0065 | **OpenAPI aggregation DX** — ponytail: `GroupedOpenApi` SpringDoc + `ApiPortalService` TTL `PT5M` partial-failure already (1.13.0), `x-data-threescale-name` + Pact CI when 3scale ActiveDocs prod | `refreshCache` partial 1/N down still 200 already, Pact deferred |
| LLM-HARDEN-001 | ai / ADR-0067 + ADR-0036 | **LLM RAG + guardrails private (support/compliance/statement/kyc)** — `BPPD DetectorLLM Llama-3.2-3B LoRA 99.9% + FF3-1 FPE` proxy SeCo, `vLLM Mistral-7B temperature 0` + `pgvector` RAG `payu.docs.*`, `3scale` `leaky_bucket` per-user, `NIST AI RMF` `GLBA/SR26-2` human gate for `excessive agency` (see `docs/adr/0067-*.md`). Lab `payu-mlops` 1× GPU, 30d zero retention | `scripts/llm-redteam.sh` LLM01/02 pass + refusal 12% |
| KEDA-HARDEN-001 | platform / ADR-0068 + ADR-0042 | **KEDA event-driven autoscaling (Kafka+Prom)** — `ScaledObject` `payu-transaction/wallet/gateway` `lagThreshold 10 min 3 max 10` + `va/biller/llm` `min 0`, `polling 15s cooldown 30s`, `TriggerAuthentication` Vault `payu-kafka` (see `docs/adr/0068-*.md`). Replaces Knative | `kcat produce 1000 → HPA 3→10 <30s` |

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
| PARTNER-PROD-008 | P0 | ⏸️ Belum | PG HA+PITR via CNPG Barman Cloud ([ADR-0031](../adr/0031-database-resilience-pitr-and-disaster-recovery.md)), restore drill, RPO=0/RTO<5m |
| PARTNER-PROD-009 | P1 | ⏸️ Belum | SLI/SLO, dashboard+alert, traces E2E ([ADR-0034](../adr/0034-end-to-end-observability-slo-sli-and-distributed-tracing-standard.md)) |
| PARTNER-PROD-010 | P0 | ⏸️ Belum | Contract/k6/chaos ([ADR-0024](../adr/0024-chaos-engineering-and-fault-injection-strategy.md)), pentest, sign-off |
| PARTNER-PROD-011 | P1 | ⏸️ Belum | Dual-control (Maker-Checker) onboarding, SLA/escalation, runbook & on-call — spec di [ADR-0035](../adr/0035-dual-control-partner-onboarding-and-sla-runbook.md): `PENDING_APPROVAL`/`REJECTED`, roles `PARTNER_MAKER`/`PARTNER_CHECKER` (`maker≠checker` DB CHECK), Flyway V19, SLO `p95<4j` jam kerja / `p99<24j` kalender & SLA `1×24j`, Telegram `T+4j` / page `T+24j`, audit+outbox `payu.partner.*.v1`, runbook `docs/operations/PARTNER_ONBOARDING_RUNBOOK.md` |

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

No drift — 68 ADRs aligned, index `docs/adr/README.md` current.

### 2. 🔴 ADR yang Sudah Ada tapi Belum / Sebagian Diimplementasikan

| Key | ADR Terkait | Domain | Ref Backlog Tunggal |
|:---|:---|:---|:---|
| ADR-GAP-003..009 | [ADR-0028](../adr/0028-step-up-authentication-and-dynamic-linking-standard.md) s/d [ADR-0034](../adr/0034-end-to-end-observability-slo-sli-and-distributed-tracing-standard.md) | auth/wallet/risk/platform/security/data/observability | → **HARDEN backlog P1** (TXN/ACC/AUTH/COMPLIANCE/GATEWAY/PORTAL) — jangan duplikasi deskripsi di sini |

### 3. 📝 Backlog ADR Baru yang Perlu Dibuat

> **Update 2026-08-19**: ADR-0035..0066 Accepted (32 ADRs) — lihat `docs/adr/README.md`. Sisa 2 Proposed di bawah.

| No | Nomor ADR Usulan | Judul / Topik ADR | Prioritas |
|:---:|:---|:---|:---:|
| — | **ADR-0067** | 📝 **LLM Integration for PayU Services — RAG, Guardrails & Private Deployment (BPPD, FinRAG-12B, 2026-08-22)** — `support` agent-assist + `compliance` audit narrative + `statement` promo personalization + `kyc` doc draft→operator QA, `BPPD DetectorLLM Llama-3.2-3B LoRA + FF3-1 FPE` proxy Separation-of-Concerns, `vLLM Mistral-7B` `temperature 0` + `pgvector` RAG, `3scale` per-user limit, `NIST AI RMF` + `GLBA/SR 26-2` governance. **Proposed** — lihat `docs/adr/0067-*.md` | **P1** |
| — | **ADR-0068** | 📝 **KEDA Autoscaling — Kafka Lag & Prometheus Triggers (HPA++, 2026-08-22)** — `payu-transaction/wallet/gateway` `ScaledObject` `lagThreshold 10` `min 3 max 10` + `va/biller/llm` `min 0`, `cooldown 30s` `polling 15s`, `TriggerAuthentication` Vault. **Proposed** — lihat `docs/adr/0068-*.md` | **P1** |

### 4. ⚠️ Kesenjangan Best Practice & Anti-Pattern yang Memerlukan Remediasi

No open gap — harden items (TXN/ACC/AUTH/...) cover best-practice remediation; track di P1 harden table.
