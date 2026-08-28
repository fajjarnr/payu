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

| **Last Release** | `1.18.55` (2026-08-28) |
| **Core Banking MVP** | 🟢 MVP workloads live di 5 environment; CNPG **payu-dev 3/3 2/2 Healthy** `barman-cloud 1/1` `ObjectStore 5/5` `S3 WAL archiving True` `RPO=0`, Tekton **31/31 Succeeded** (cnpg storage 20Gi wal 10Gi 1.18.42, fx-service 1.18.41 FX 0 WARN, transaction 1.18.40 Topics+KEDA, partner SLO 1.18.21, HPA/PDB 1.18.20, Cache Plain 1.18.19, WORM 1.18.27), workloads `49/49 1/1` `1.18.55` `coraza 2/2` `KEDA RH-CMA 5 ScaledObjects` `Litmus 6 pods + Kraken/Cerberus` `SSO sso-dev/sso-sit/sso.uat/preprod/prod 5 env` `CNPG/Kafka/EFS/3scale/RHACS` verified. |
| **Backlog Aktif** | *No OPEN item* — seluruh B1–B4 + harden sweep **CLOSED 1.18.9–1.18.48** → [`CHANGELOG.md`](../../CHANGELOG.md). |
| **Last Updated** | 2026-08-28 — DPoP per-client **1.18.55** `verify-dpop.sh` + Rate-limit **1.18.54** + ArgoCD **1.18.53** + Tekton Results **1.18.52** + UBI9 1.24-3 **1.18.51** + SSO 5 env **1.18.50** + RLS **1.18.49**; sisa OPEN: PERF-004 DEFERRED. |

---

## ⏸️ Deferred Scope

| Key | Item |
| READY-061 | Mobile app (seluruh `frontend/mobile`) — ditunda dari MVP/production gate sampai diaktifkan product owner. Jangan kerjakan upgrade/bug/test mobile. |
| PROD-035 | Mobile idempotency durability (SecureStore 2048B limit) — deferred bersama mobile |
| PROD-038 | Mobile money precision (JS `number` untuk amount) — deferred bersama mobile |
| LLM-HARDEN-001 | LLM/RAG + guardrails ([ADR-0067](../adr/0067-llm-integration-for-payu-services-standard.md) DEFERRED NO-GO, B4.6 2026-08-24) — no code; re-evaluasi saat GPU quota `payu-mlops` + demand tervalidasi + pgvector approved. |

---

## 🔴 Active Tickets

No open tickets — `RLS-ROLLOUT-001` `SSO-ISSUER-002` `CICD-RESULTS-001` `ARGOCD-SYNC-001` `SSO-DPOP-003` `NET-RATELIMIT-004` **CLOSED 1.18.49–1.18.55** `verify-*` PASS → `CHANGELOG.md` `1.18.55`; sisa `PERF-004 DEFERRED` + `Deferred Scope` only.

---

## 🎯 Backlog Aksi (urut per priority — hanya OPEN)

### P1 — Quality & Reliability (In-Scope MVP)

No open P1 — harden TXN/ACC/COMPLIANCE/GATEWAY/PORTAL **CLOSED 1.18.19–1.18.27**, KEDA + Topics (ARCH-TOPIC-002) **CLOSED 1.18.40**, LLM-HARDEN-001 **DEFERRED** (→ Deferred Scope). Bukti: `CHANGELOG.md` + `PROGRESS.md`.

### P2 — Defer (Out-of-Scope MVP, ADR-0023)

No open P2 — 8 items CLOSED 2026-08-12 (CB-008/011/017/022/024/025/031/036) → `CHANGELOG.md` `1.10.63`.

### P3 — Backlog Lanjutan

| Key | Domain | Item |

---

## 🏦 Partner Service Production Readiness Gate

Status `partner-service` hanya Production Ready setelah seluruh gate memiliki bukti live. `PARTNER-001..006` CLOSED (2026-08-08).

No open gate — PARTNER-PROD-007..011 ✅ Selesai 1.18.9–1.18.21 → `CHANGELOG.md`.

> Local APIcast (profile `api-management`) tidak bisa authless — public edge butuh APIManager (cluster-level).

---

## 🚀 Platform Deploy Queue

| Key | Pri | Category | Summary |

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

| Key | ADR | Status | Gap | Bukti |
|:---|:---|:---|:---|:---|
| ADR-GAP-0014 | [ADR-0014 API Management Platform](adr/0014-api-management-platform.md) | **Proposed** (2026-03-02) — trigger ≥5 partners | Status masih `Proposed` padahal **3scale `APIManager Available True` `system-app 3/3` `apicast-production` `backend-listener/worker/cron` `system-memcache/searchd` `zync` live** `infrastructure/platform/api-management/3scale` + `GATEWAY_ARCH.md` `3scale` verified `payu-dev 6 pods` — perlu update status `Proposed→Accepted` + `PROPOSED` note sync | `grep -r "status.*Proposed" docs/adr/0014` + `oc get apimanager -n payu-api-management` `Available True` `oc get pods -n payu-api-management` `system-app 3/3` |
| ADR-GAP-0016 | [ADR-0016 ARCH-006 Spring Boot 4.1.0](adr/0016-arch-006-phase-a-strategy.md) | **Deferred** (2026-06-14) — shared starters blocker | Status `Deferred` karena 4 shared starters `Spring Boot 3.x APIs` gagal, tapi **live `backend/pom.xml` `spring-boot-starter-parent:4.1.0` `java.version 25` `AccountServiceApplication v4.1.0` `Started` `BUILD SUCCESS` `31/31` `1.18.55`** — migrasi sudah live, perlu update status `Deferred→Accepted` + `Implementation Notes` sync | `grep -r "spring-boot-starter-parent:4.1.0" backend/pom.xml` + `grep "java.version.*25" backend/pom.xml` + `oc logs account-service` `Spring Boot v4.1.0` |
| ADR-GAP-0069 | [ADR-0069 OpenShift 4.22](adr/0069-openshift-4-22-platform-standard.md) | **Accepted** (2026-08-24) — doc sweep `4.20→4.22` | Status `Accepted` tapi **normative docs masih `4.20+`** `grep -R "4\.20\+" docs/ AGENTS.md README.md` `~20` hits (`ADR-0032/0034/0024/0031` `AGENTS.md:7` `README.md` `ARCHITECTURE.md:78` `INFRASTRUCTURE_DEPLOYMENT.md:30` vs live `OCP 4.22.7` `K8s 1.35.6`), perlu sweep `~40` files per `Implementation Notes` | `grep -R "4\.20\+" docs/ | wc -l` `~20` + `grep -R "4\.22\+" docs/ | wc -l` `~5` |
| ADR-GAP-0071 | [ADR-0071 Test Quality Gates](adr/0071-test-quality-gates-mutation-accessibility-exploratory-canary-standard.md) | **Accepted** (2026-08-26) — PIT `≥70%` core 5 | Status `Accepted` tapi **parent `backend/pom.xml` `mutationThreshold 60` untuk semua**, 5 core `transaction/wallet/partner/account/auth` **tidak override `70`** (`grep -R mutationThreshold backend/partner-service/pom.xml` 0), `axe-core` `test:a11y` exists tapi `a11y:audit` baseline burn-down belum, `Tekton` `-Pmutation-testing` nightly `CronJob` belum checked-in | `grep -R "mutationThreshold" backend --include=pom.xml` `only parent 60` + `grep -R "@axe-core/playwright" frontend/web-app/package.json` + `ls frontend/web-app/scripts/a11y-audit.ts` |

No open gap — seluruh ADR-GAP (003..009, 015, 019, 028W, 029, 030E, 032W, 047, 048, 054C, 056) **CLOSED 1.18.9–1.18.40** → `CHANGELOG.md` (di atas adalah gap baru hasil audit 2026-08-28).

### 3. 📝 Backlog ADR Baru yang Perlu Dibuat

Semua ADR backlog sudah dibuat & terindeks — ADR-0067 **Deferred** (NO-GO B4.6), ADR-0068 **Accepted** (live 1.18.15, verified 1.18.40). Tidak ada ADR pending.

### 4. ⚠️ Kesenjangan Best Practice & Anti-Pattern yang Memerlukan Remediasi

No open gap — remediasi best-practice tuntas via harden items **CLOSED 1.18.19–1.18.40** → `CHANGELOG.md`.
