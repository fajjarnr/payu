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
| **Last Release** | `1.13.69` (2026-08-21) |
| **Core Banking MVP** | 🔴 Belum MVP production ready — PARTNER-PROD-007..011 + DEVSECOPS-017 OPEN; **login web live** (LOGIN-001..006 closed) |
| **Backlog Aktif** | 0 P1 aksi + 0 cross-layer findings (ARCH-GLOBAL-002, GW-ROUTING-003/BE-BIO-001, BE-SUPP-001 CLOSED 1.13.69) + 5 gate PARTNER-PROD + DEVSECOPS-017 |
| **Last Updated** | 2026-08-21 — ARCH-GLOBAL-002 + GW-ROUTING-003/BE-BIO-001 + BE-SUPP-001 CLOSED, StepUp 4/4 + biometric + tickets, podman 5.7.0 cek (6.1.0 desktop only) |

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

| Key | Domain | Item | Done saat |
|:---|:---|:---|:---|
| — | — | No open P1 aksi — ARCH-GLOBAL-002 CLOSED 1.13.69 | — |

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
| — | **ADR-0057** | 📝 **Billing Provider & Biller Catalogue Governance** — biller-simulator (Proposed P3, deferred per ADR-0023) | **P3** |
| — | **ADR-0058** | 📝 **Backoffice RBAC & Admin Audit Trail** — backoffice-service (Proposed P3, deferred) | **P3** |
| — | **ADR-0059** | 📝 **Product Catalog & Partner Product Governance** — product-catalog-service (Proposed P3, deferred) | **P3** |

### 4. ⚠️ Kesenjangan Best Practice & Anti-Pattern yang Memerlukan Remediasi

| Key | Domain | Deskripsi Masalah & Rekomendasi Best Practice | Status |
|:---|:---|:---|:---:|