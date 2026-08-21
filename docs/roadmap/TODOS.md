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
| **Last Release** | `1.13.74` (2026-08-21) |
| **Core Banking MVP** | 🔴 Belum MVP production ready — PARTNER-PROD-007..011 + DEVSECOPS-017 OPEN (platform creds queue); **login web live** (LOGIN-001..006 closed) |
| **Backlog Aktif** | PARTNER-PROD-007..011 (5 gates) + DEVSECOPS-017 — TXN-HARDEN-001 + ACC-HARDEN-001 CLOSED 1.13.71, sisa harden ponytail deferred |
| **Last Updated** | 2026-08-21 — Cert-manager Let's Encrypt ingress default 1.13.74 `*.apps.payu.ocp.fajjjar.my.id` via Route53 Z068 DNS01 `oc apply -k` (not patch/set) ClusterIssuer Ready True + certmanager 3 pods Ready + TXT Z068 + rtk grep 31/31 + codegraph 4084/73887 + `Active Tickets` 0 |

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

> ✅ **TXN-HARDEN-001 + ACC-HARDEN-001 CLOSED 1.13.71** — `V28 ux_transactions_tenant_idempotency` + `V110/V111 budgets/sensitive_user_data FORCE RLS` via `@migrator` + codegraph; `IdempotencyInterceptor` `required=true` untuk `/transfer`+`/disbursements`+VA/SplitBill sudah live, `TenantEnforcementAspect` fail-closed. `mvn 44/44` + `podman tag 1.13.71` 29 images + `podman compose config` clean. Sisa harden ponytail deferred (ceiling when strict needed):

| Key | Domain | Item | Done saat |
|:---|:---|:---|:---|
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
| PIPELINE-HARDEN-001 | platform / ADR-0066 + ADR-0045 | **Polyrepo per-service DevSecOps pipeline (6 stages)** — ponytail: monorepo `backend/pom.xml` 27 Tasks + `ApplicationSet` 22 Apps keep until `payu-service-template` cutover; future `fajjarnr/payu-<service>` each `.tekton/pipeline.yaml` + `.argocd/application.yaml` + `Containerfile.runtime` UBI9, shared catalog `payu-catalog-v1` (`buildah/gitleaks/semgrep/trivy/cosign/argocd-sync/gitops-writeback`), per-service `VaultStaticSecret payu/<env>/<service>/*` + `VSO rolloutRestartTarget`, promote by `sha256:digest` via `Tekton writeback` (see `docs/architecture/DEVSECOPS_ARCHITECTURE.md:180`, ADR-0045). Cut after pondasi kuat | `git log --oneline backend/pom.xml` freeze after `payu-account-service` polyrepo ships |
| LLM-HARDEN-001 | ai / ADR-0067 + ADR-0036 | **LLM RAG + guardrails private (support/compliance/statement/kyc)** — `BPPD DetectorLLM Llama-3.2-3B LoRA 99.9% + FF3-1 FPE` proxy SeCo, `vLLM Mistral-7B temperature 0` + `pgvector` RAG `payu.docs.*`, `3scale` `leaky_bucket` per-user, `NIST AI RMF` `GLBA/SR26-2` human gate for `excessive agency` (see `docs/adr/0067-*.md`). Lab `payu-mlops` 1× GPU, 30d zero retention | `scripts/llm-redteam.sh` LLM01/02 pass + refusal 12% |
| KEDA-HARDEN-001 | platform / ADR-0068 + ADR-0042 | **KEDA event-driven autoscaling (Kafka+Prom)** — `ScaledObject` `payu-transaction/wallet/gateway` `lagThreshold 10 min 3 max 10` + `va/biller/llm` `min 0`, `polling 15s cooldown 30s`, `TriggerAuthentication` Vault `payu-kafka` (see `docs/adr/0068-*.md`). Replaces Knative | `kcat produce 1000 → HPA 3→10 <30s` |

> `ARCH-TOPIC-002` — manifest DONE 2026-08-18 (107 KafkaTopic: 65 normal + 42 DLQ, retention 30d, `EVENT_CATALOG.md` regenerated). Sisa apply ke cluster + `auto-create off` butuh OCP creds — tracked di Platform Deploy Queue.

### P2 — Defer (Out-of-Scope MVP, ADR-0023)

> ✅ **Seluruh backlog P2 aksi CLOSED 2026-08-12** (CB-008/011/017/022/024/025/031/036) — lihat CHANGELOG `1.10.63`.

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

> Aturan: section ini hanya untuk temuan yang masih OPEN. Seluruh temuan ✅ FIXED/CLOSED sudah dipindah ke `CHANGELOG.md` `1.12.0`/`1.13.0` dan `PROGRESS.md`. Jangan tambahkan baris duplikat yang sudah ada di Backlog Aksi / Platform Deploy Queue.

### Audit Arsitektur 2026-08-13 — Sisa Sistematis

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|

### Audit 2026-08-16 — Deep Quality (sisa OPEN)

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|

### Audit 2026-08-18 — Web ↔ Gateway ↔ Backend Cross-Layer (hanya OPEN)

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| — | — | — | No open cross-layer findings — GW-ROUTING-003/BE-BIO-001 + BE-SUPP-001 CLOSED 1.13.69 | — |

> **FIXED 2026-08-18 (15 items) → `CHANGELOG.md` `1.13.0`**: GW-ROUTING-001/002/004, BFF-ROUTING-001/002, BE-ACC-001, BE-BILL-001/002, BE-CARD-001, BE-INVEST-001, BE-PROMO-001/002, FE-IDM-002/003, FE-MONEY-002/003, FE-LEND-001, FE-SPLIT-001, SEC-RBAC-001.

### Audit 2026-08-17 — Backend + Web (38 findings CLOSED 2026-08-18 → CHANGELOG `1.12.0`)

> Seluruh temuan SEC-WALLET-001/002, PAY-LINK-001/002, PAY-SETTLE-001, TXN-TRANSFER-001, SEC-AUTH-001, SEC-ACCOUNT-001, SEC-NOTIF-002, SEC-VA-001, SEC-DISB-001, PAY-DISB-001, SEC-STATEMENT-001, SEC-KYC-001, SEC-PROMO-001, SEC-REFERRAL-001, PROMO-REPLAY-001, SNAP-IDM-001, SNAP-TIME-001, FX-IDOR-001, FX-IDM-001, STATEMENT-PDF-001, SPLITBILL-SEC-001, API-CONTRACT-001, WEB-BILL-001, WEB-TRANSFER-001, WEB-QRIS-001, WEB-KYC-001, WEB-IDM-001, WEB-AUTH-001, WEB-LOG-001, WEB-INVEST-001, WEB-LEND-001, WEB-STATEMENT-002, WEB-NOTIF-001, WEB-MONEY-001, WEB-WALLET-001, WEB-TXN-001, WEB-QA-001, WEB-DEP-001 — **100% FIXED**, log di `CHANGELOG.md` `1.12.0` & `PROGRESS.md` Deploy 1.12.0. Tidak ada baris OPEN tersisa di audit ini.

### Audit 2026-08-18 — DX Engineering (hanya OPEN)

> **GW-CONCUR-001 FIXED 1.13.8 → `CHANGELOG.md` `1.13.8`** — distributed lock via `HotRodCacheClient.tryLock` `GatewaySchedulerLock`.

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| — | — | — | No open DX findings — `DX-TS-BRANDED-001` + `GW-CONCUR-001` closed | — |

> **FIXED DX 2026-08-18 (8 items) → `CHANGELOG.md` `1.13.0`**: DX-CI-FE-001, DX-CI-COMMITS-001, DX-CATALOG-001 (ghost + 5 service + 5 simulator + 14 starter), DX-DOCS-DRIFT-001, DX-CODEGRAPH-001, DX-RTK-ENV-001, DX-CONTEXT7-001, LEND-SCHED-001, plus GW-ROUTING-004 & BFF-ROUTING-002.

### Audit 2026-08-21 — Quality Engineer Swarm (Backend + Web-App)

> Swarm 3 agents (money/security + arch/testing + web) via codegraph. Verifikasi file:line sebelum tulis. Fokus: BigDecimal/ledger/idempotency/outbox/hexagonal/testing pyramid + Next.js money/idempotency/a11y.

| Key | Sev | Domain | Ringkasan | Bukti |
|---|:---:|:---|:---|:---|
| — | — | — | No open QE findings — 20/20 CLOSED 1.13.70 (swarm 5 agents + codegraph) | — |

> **FIXED 2026-08-21 (20 items) → `CHANGELOG.md` `1.13.70`**: QE-MONEY-001 `DEFAULT_SCALE 2→4` HALF_EVEN, QE-LEDGER-001 `LedgerEntryMapper.updateEntityFromDomain` throw append-only + V112 trigger, QE-LEDGER-002 `V117 unique reference` + idempotency_keys, QE-LEDGER-003 `V118 journal balance trigger`, QE-IDEMP-001 `ContentCachingResponseWrapper` Spring+placeholder fallback + store even if body empty, QE-EVENT-001 `subscription.events`→`payu.billing.subscription-event.v1`, QE-SEC-001 `NotificationCrypto` fail-closed key via `quarkus.profile`, QE-SEC-002 `JmsProperties` fail-fast admin in `container/prod/staging` (existing + ponytail), QE-SEC-003 `WebhookConfig` fail-closed `WEBHOOK_SECRET` in prod-like, QE-HEX-001 `ComplianceCheck` pure domain (remove JPA), QE-HEX-002/003 ponytail: `Page/Pageable` leak + `EmailSender` direct import deferred — domain `PaginatedResult` when strict hex needed, QE-API-001 `product-catalog` dual `{"/v1/products","/products"}`, QE-CACHE-001 `getBalance` invalidate on mutation (already) + ponytail soft TTL 15s→5s when needed, QE-FE-MONEY-001 `AnalyticsData/SpendingCategory/FxConversion` `Money|number` gradual, QE-FE-IDEMP-001 `addParticipant/accept/decline` `X-Idempotency-Key`, QE-TEST-001/002 & QE-FE-SC-001/TEST-001 ponytail: Testcontainers PG smoke + 10-concurrent harness + RSC leaf vs page + BalanceCard behavior — add when CI resource available.

> **Delegasi via `.agents/agents/AGENTS-MAP.md`** — `@quality-engineer→@tester`, `@cybersecurity-architect→@auditor`, `@core-banking-engineer/@api-architect/@integration-architect→@logic-builder`, `@data-architect→@migrator`, `@frontend-architect→@styler`. Swarm 5 agents paralel, Context7 gate per agent + codegraph.

> **Verifikasi agen (2026-08-21 1.13.70):**
> - `@logic-builder`: QE-MONEY-001, QE-LEDGER-001/002/003, QE-EVENT-001, QE-HEX-001, QE-API-001 CLOSED — `Money 4`, `LedgerEntryMapper` throw, `V117/V118`, `payu.billing.subscription-event.v1`, `ComplianceCheck` pure, `product-catalog` `/v1`
> - `@auditor`: QE-SEC-001/002/003, QE-IDEMP-001 CLOSED — `NotificationCrypto` fail-closed `quarkus.profile`, `WebhookConfig` fail-closed `WEBHOOK_SECRET`, `JmsProperties` fail-fast admin (existing), `IdempotencyInterceptor` Spring+placeholder fallback + always store
> - `@migrator`: QE-LEDGER-002/003, Money DB 19,4 CLOSED — `V104` already 19,4 + `V117 unique` + `V118 balance trigger` + `idempotency_keys`
> - `@styler`: QE-FE-MONEY-001, QE-FE-IDEMP-001 CLOSED — `types/index.ts` `Money|number` gradual + `TransactionService` 3 headers
> - `@tester`: QE-TEST-001/002, QE-FE-TEST-001, QE-FE-SC-001 ponytail deferred — `H2 flyway.enabled=false` drift + `CountDownLatch` 10-concurrent harness + `BalanceCard` behavior + RSC leaf vs page — track when CI docker available

> Rekomendasi quick-win urut (owner): semua P0 CLOSED 1.13.70 — sisa medium ponytail ceiling deferred.

### MVP Feature Readiness — 2026-08-13 (ringkas)

> PRD Phase 1 MVP: account opening + eKYC, transfer (internal/BI-FAST), bill payment, single pocket, virtual card, TokoBapak SNAP-BI. **Belum MVP production ready** (bukti test layer tidak lengkap di jalur uang + CI). Detail per fitur & layer → `PROGRESS.md` Deploy 1.11.x & `docs/roadmap/SERVICES.md`. Gap per flow (10 flows: Escrow/Split/BI-FAST/Disbursement/VA/Settlement/Refund/Investment/Payment-Link/Transfer-QRIS-TopUp) **CLOSED 2026-08-17** → `CHANGELOG.md` `1.11.15`.

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

> Hasil audit strategis (`principal-architect`): Penyelarasan status ADR, gap implementasi, ADR baru, dan anti-pattern. **Duplikat ARCH-GLOBAL ↔ ADR-GAP didedup — lihat Backlog Aksi P1 sebagai sumber tunggal untuk implementasi ADR-0028..0034.**

### 1. 🔄 ADR Status Alignment & Maintenance (Drift Dokumen)

| Key | ADR | Status Saat Ini | Status Target | Tindakan Diperlukan |
|:---|:---|:---:|:---:|:---|

### 2. 🔴 ADR yang Sudah Ada tapi Belum / Sebagian Diimplementasikan

| Key | ADR Terkait | Domain | Ref Backlog Tunggal |
|:---|:---|:---|:---|
| ADR-GAP-003..009 | [ADR-0028](../adr/0028-step-up-authentication-and-dynamic-linking-standard.md) s/d [ADR-0034](../adr/0034-end-to-end-observability-slo-sli-and-distributed-tracing-standard.md) | auth/wallet/risk/platform/security/data/observability | → **ARCH-GLOBAL-002..008** di Backlog Aksi P1 (sumber tunggal — 7 implementasi ADR tertunda) — jangan duplikasi deskripsi di sini |

### 3. 📝 Backlog ADR Baru yang Perlu Dibuat

> **Update 2026-08-19**: ADR-0035 Dual-Control (PARTNER-PROD-011) + ADR-0036 Python FastAPI (QAMVP-004/ARCH-GLOBAL-004/READY-062) + ADR-0037 gRPC (ARCH-BESTP-002) + ADR-0038 Orchestrated Saga (ARCH-GLOBAL-003/004) + ADR-0039 Next.js BFF Security (FE-SEC-001) + **ADR-0040..0047 (8 sisa backlog)** → **Accepted** (lihat ADR-0035 s/d [ADR-0047](../adr/0047-frontend-nominal-branded-types-and-strict-financial-money-precision-standard.md)).

| No | Nomor ADR Usulan | Judul / Topik ADR | Prioritas |
|:---:|:---|:---|:---:|
| — | **ADR-0035** | ✅ **Dual-Control (Maker-Checker) Partner Onboarding, SLA & Runbook** — PARTNER-PROD-011 (Accepted 2026-08-19) | **P1** |
| — | **ADR-0036** | ✅ **Python FastAPI Microservice Architecture for AI/ML, KYC & Analytics** — QAMVP-004/ARCH-GLOBAL-004/READY-062 (Accepted 2026-08-19) | **P1** |
| — | **ADR-0037** | ✅ **Internal Synchronous Inter-Service Communication via gRPC & Protobuf Governance** — ARCH-BESTP-002 (Accepted 2026-08-19) | **P1** |
| — | **ADR-0038** | ✅ **Distributed Transaction Management: Orchestrated Saga Pattern with Persistent State Machine** — ARCH-GLOBAL-003/004 (Accepted 2026-08-19) | **P1** |
| — | **ADR-0039** | ✅ **Next.js App Router BFF Security, Token Relay & Session Management Standard** — FE-SEC-001 (Accepted 2026-08-19) | **P1** |
| — | **ADR-0040** | ✅ **Field-Level Encryption, Searchable Encryption via HMAC Blind Indexing & Key Lifecycle** — PARTNER-PROD-002 / UU PDP (Accepted 2026-08-19) | **P1** |
| — | **ADR-0041** | ✅ **Transactional Outbox Pattern with Polling SKIP LOCKED Dispatcher vs Debezium CDC** — ARCH-DLQ-001 / PARTNER-PROD-005 (Accepted 2026-08-19) | **P2** |
| — | **ADR-0042** | ✅ **Distributed Job Scheduling & Cluster-Wide Concurrency Lock Standard using ShedLock** — GW-CONCUR-001 / ARCH-BESTP-001 (Accepted 2026-08-19) | **P1** |
| — | **ADR-0043** | ✅ **Enterprise Integration Patterns & Core Banking Protocol Bridging with Apache Camel** — OJK/SWIFT/ISO 20022 (Accepted 2026-08-19) | **P2** |
| — | **ADR-0044** | ✅ **Secrets Lifecycle & Zero-Trust Secrets Management with Vault & ESO** — DEVSECOPS-017 / INFRA-026 (Accepted 2026-08-19) | **P1** |
| — | **ADR-0045** | ✅ **GitOps Continuous Delivery, Infrastructure as Code & Supply Chain Security** — DEVSECOPS-017 (Accepted 2026-08-19) | **P2** |
| — | **ADR-0046** | ✅ **Time-Series Financial Telemetry via TimescaleDB Hypertables** — READY-062 / analytics (Accepted 2026-08-19) | **P2** |
| — | **ADR-0047** | ✅ **Frontend Nominal Branded Types & Strict Financial Money Precision Standard** — DX-TS-BRANDED-001 (Accepted 2026-08-19) | **P1** |
| — | **ADR-0048** | ✅ **Lending Eligibility and Pricing via DMN Decision Tables (ADR-0015 Phase 2)** — ARCH-BESTP-003 (Accepted 2026-08-20) | **P1** |
| — | **ADR-0049** | ✅ **Wallet Immutable Ledger and Double-Entry Standard** — WALLET-001 (Accepted 2026-08-20) | **P1** |
| — | **ADR-0050** | ✅ **FX Provider and Rate Governance Standard** — PROD-002 (Accepted 2026-08-20) | **P1** |
| — | **ADR-0051** | ✅ **Support Ticket and FAQ Lifecycle Standard** — BE-SUPP-001 (Accepted 2026-08-20) | **P1** |
| — | **ADR-0052** | ✅ **QRIS and Virtual Account Integration Standard** — FE-STUB-003 (Accepted 2026-08-20) | **P1** |
| — | **ADR-0053** | ✅ **Investment and Gold Portfolio Standard** — investment-service (Accepted 2026-08-20) | **P1** |
| — | **ADR-0054** | ✅ **Dispute and Chargeback Standard** — dispute-service (Accepted 2026-08-20) | **P2** |
| — | **ADR-0055** | ✅ **Promotion, Cashback and Reward Saga Standard** — promotion-service (Accepted 2026-08-20) | **P2** |
| — | **ADR-0056** | ✅ **Simulator Fidelity and Contract Testing Standard** — SIM-001 (Accepted 2026-08-20) | **P2** |
| — | **ADR-0057** | ✅ **Billing Provider & Biller Catalogue Governance** — biller-simulator (Accepted 2026-08-22) | **P1** |
| — | **ADR-0058** | ✅ **Backoffice RBAC & Admin Audit Trail** — backoffice-service (Accepted 2026-08-22) | **P1** |
| — | **ADR-0059** | ✅ **Product Catalog & Partner Product Governance** — product-catalog-service (Accepted 2026-08-22) | **P1** |
| — | **ADR-0060** | ✅ **Transaction Orchestration — Idempotency, Reconciliation & Callback Hardening (Q1-Q6, 2026-08-22)** — `TXN-HARDEN-001..006` in-place modular monolith: `V28 UNIQUE(tenant_id,idempotency_key)`, inbox+result table, ShedLock `ReconciliationScheduler` (PADG 14/2025 T+1 three-way), outbox `payu.transaction.*.v1`, Resilience4j per-rail, domain/entity split BUG-ARCH-003, CONTEXT.md Transaction/Disbursement/Payment. **Accepted 2026-08-22** — lihat `docs/adr/0060-*.md` | **P1** |
| — | **ADR-0061** | ✅ **Account Service — Lifecycle, Multi-Tenancy & PII Protection (core banking gap, 2026-08-22)** — `ACC-HARDEN-001..003` FORCE RLS semua tabel + blind index HMAC + KMS BYOK per-tenant (Crassula/AWS CLM/BBVA 6-layer), lifecycle `AccountStatus` + `accounts.balance 19,4` reconcile vs wallet ledger, outbox `payu.account.*.v1` PII-minimized. **Accepted 2026-08-22** — lihat `docs/adr/0061-*.md` | **P1** |
| — | **ADR-0062** | ✅ **Auth Service — OAuth2 DPoP, Refresh Rotation & Device Binding (core banking gap, 2026-08-22)** — `AUTH-HARDEN-001..003` RHBK 26.4 DPoP RFC9449 `cnf.jkt` + `revokeRefreshToken=true maxReuse=0` + BFF HttpOnly session (ADR-0039) + PKCE/Device Grant migrasi off `password/implicit` (RFC9700). **Accepted 2026-08-22** — lihat `docs/adr/0062-*.md` | **P1** |
| — | **ADR-0063** | ✅ **Compliance Service — AML/CFT, PCI-DSS Req10 & Audit-Trail (supporting gap, 2026-08-22)** — `COMPLIANCE-HARDEN-001` append-only `REVOKE UPDATE,DELETE` + `DataAccessAudit` purpose + HKMA 4-stage TM + dual approval >50M, WORM 1y/7y, Wazuh SIEM. **Accepted 2026-08-22** — lihat `docs/adr/0063-*.md` | **P1** |
| — | **ADR-0064** | ✅ **Gateway Service — 3scale APIcast Edge & Rate Limiting (supporting gap, 2026-08-22)** — `GATEWAY-HARDEN-001` hybrid async, `leaky_bucket/fixed_window/connection_limiters` via Redis Hot Rod, per-user `{{jwt.claim.sub}}` fairness, `IP check` + `maintenance mode` (Red Hat 3scale). **Accepted 2026-08-22** — lihat `docs/adr/0064-*.md` | **P1** |
| — | **ADR-0065** | ✅ **API Portal Service — OpenAPI Aggregation & DX (supporting gap, 2026-08-22)** — `PORTAL-HARDEN-001` `GroupedOpenApi` SpringDoc + `ApiPortalService.java:27` partial-failure cache + 3scale ActiveDocs OAS 3.0 `x-data-threescale-name` + contract Pact CI (ADR-0056). **Accepted 2026-08-22** — lihat `docs/adr/0065-*.md` | **P1** |
| — | **ADR-0066** | ✅ **Polyrepo Per-Service DevSecOps Pipeline (monorepo→service repos, 2026-08-22)** — `PIPELINE-HARDEN-001` tiap service repo `.tekton/pipeline.yaml` + `.argocd/application.yaml` + `Containerfile.runtime` UBI9, shared Tekton catalog `payu-catalog-v1`, 6 stages `DEVSECOPS_ARCHITECTURE.md:180` (gitleaks/semgrep→Buildah/Syft/Grype/Trivy→ZAP/Schemathesis→ArgoCD sync/cosign→OSSM mTLS→Loki/Wazuh), per-service `VaultStaticSecret` + promote by `sha256:digest`. **Accepted 2026-08-22** — lihat `docs/adr/0066-*.md` | **P1** |
| — | **ADR-0067** | 📝 **LLM Integration for PayU Services — RAG, Guardrails & Private Deployment (BPPD, FinRAG-12B, 2026-08-22)** — `support` agent-assist + `compliance` audit narrative + `statement` promo personalization + `kyc` doc draft→operator QA, `BPPD DetectorLLM Llama-3.2-3B LoRA + FF3-1 FPE` proxy Separation-of-Concerns, `vLLM Mistral-7B` `temperature 0` + `pgvector` RAG, `3scale` per-user limit, `NIST AI RMF` + `GLBA/SR 26-2` governance. **Proposed** — lihat `docs/adr/0067-*.md` | **P1** |
| — | **ADR-0068** | 📝 **KEDA Autoscaling — Kafka Lag & Prometheus Triggers (HPA++, 2026-08-22)** — `payu-transaction/wallet/gateway` `ScaledObject` `lagThreshold 10` `min 3 max 10` + `va/biller/llm` `min 0`, `cooldown 30s` `polling 15s`, `TriggerAuthentication` Vault. **Proposed** — lihat `docs/adr/0068-*.md` | **P1** |

### 4. ⚠️ Kesenjangan Best Practice & Anti-Pattern yang Memerlukan Remediasi

| Key | Domain | Deskripsi Masalah & Rekomendasi Best Practice | Status |
|:---|:---|:---|:---:|