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

| **Last Release** | `1.18.45` (2026-08-25) |
| **Core Banking MVP** | 🟢 MVP workloads live di 5 environment; CNPG **payu-dev 3/3 2/2 Healthy** `barman-cloud 1/1` `ObjectStore 5/5` `S3 WAL archiving True` `RPO=0`, Tekton **31/31 Succeeded** (cnpg storage 20Gi wal 10Gi 1.18.42, fx-service 1.18.41 FX 0 WARN, transaction 1.18.40 Topics+KEDA, partner SLO 1.18.21, HPA/PDB 1.18.20, Cache Plain 1.18.19, WORM 1.18.27), workloads `49/49 1/1` `1.18.45` `coraza 2/2` `KEDA RH-CMA 5 ScaledObjects` `Litmus 6 pods + Kraken/Cerberus` `SSO sso-dev/sso-sit/sso.uat/preprod/prod 5 env` `CNPG/Kafka/EFS/3scale/RHACS` verified. |
| **Backlog Aktif** | *No OPEN item* — seluruh B1–B4 + harden sweep **CLOSED 1.18.9–1.18.45** → [`CHANGELOG.md`](../../CHANGELOG.md). |
| **Last Updated** | 2026-08-26 — audit login/register payu-dev: 6 lapis defect SSO + CSP hydration + RLS GUC diperbaiki, E2E hijau (`CHANGELOG.md` 1.18.47); sisa OPEN: RLS rollout, issuer audit env lain, DPoP, rate-limit keying. |

---

## ⏸️ Deferred Scope

| Key | Item |
|:---|:---|
| READY-061 | Mobile app (seluruh `frontend/mobile`) — ditunda dari MVP/production gate sampai diaktifkan product owner. Jangan kerjakan upgrade/bug/test mobile. |
| PROD-035 | Mobile idempotency durability (SecureStore 2048B limit) — deferred bersama mobile |
| PROD-038 | Mobile money precision (JS `number` untuk amount) — deferred bersama mobile |
| LLM-HARDEN-001 | LLM/RAG + guardrails ([ADR-0067](../adr/0067-llm-integration-for-payu-services-standard.md) DEFERRED NO-GO, B4.6 2026-08-24) — no code; re-evaluasi saat GPU quota `payu-mlops` + demand tervalidasi + pgvector approved. |

---

## 🔴 Active Tickets

| Key | Pri | Summary | Status |
| RLS-ROLLOUT-001 | P1 | `TenantDataSource` decorator (SET LOCAL per tx) baru ter-build di account-service `1.18.47`; service lain masih bertahan pada mitigasi `ALTER ROLE payu SET app.tenant_id='default'` — rebuild + deploy per service di rilis berikutnya, lalu evaluasi pencabutan mitigasi | Detail di [Open Findings → Audit 2026-08-26] |
| SSO-ISSUER-002 | P1 | Issuer alignment + realm drift audit untuk sit/uat/preprod/prod — dev terbukti mati oleh pola identik (web-app issuer publik vs backend internal + realm drift); env lain kemungkinan besar sama | Detail di [Open Findings → Audit 2026-08-26] |

---

## 🎯 Backlog Aksi (urut per priority — hanya OPEN)

### P1 — Quality & Reliability (In-Scope MVP)

No open P1 — harden TXN/ACC/COMPLIANCE/GATEWAY/PORTAL **CLOSED 1.18.19–1.18.27**, KEDA + Topics (ARCH-TOPIC-002) **CLOSED 1.18.40**, LLM-HARDEN-001 **DEFERRED** (→ Deferred Scope). Bukti: `CHANGELOG.md` + `PROGRESS.md`.

### P2 — Defer (Out-of-Scope MVP, ADR-0023)

No open P2 — 8 items CLOSED 2026-08-12 (CB-008/011/017/022/024/025/031/036) → `CHANGELOG.md` `1.10.63`.

### P3 — Backlog Lanjutan

| Key | Domain | Item |
|:---|:---|:---|

---

## 🏦 Partner Service Production Readiness Gate

Status `partner-service` hanya Production Ready setelah seluruh gate memiliki bukti live. `PARTNER-001..006` CLOSED (2026-08-08).

No open gate — PARTNER-PROD-007..011 ✅ Selesai 1.18.9–1.18.21 → `CHANGELOG.md`.

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

### Audit 2026-08-25 — CI/CD & Platform Health (hanya OPEN)

| Key | Pri | Temuan | Bukti | Sisa |
|:---|:---:|:---|:---|:---|
| WAF-CORAZA-001 | P1 | **coraza-waf CrashLoopBackOff** — pod restart loop `python3: can't open file '/etc/coraza/waf-proxy.py': No such file or directory`. Drift manifest vs cluster: git `configmap.yaml` (line 56) punya key `waf-proxy.py`, live CM `coraza-waf-config -n coraza-waf` hanya `['coraza.conf','crs-setup.conf']`; deployment command butuh script itu (`deployment.yaml:38`). | `oc get cm coraza-waf-config -n coraza-waf -o jsonpath='{.data}'` · `oc logs deploy/coraza-waf` | Re-apply `oc apply -f infrastructure/platform/security/coraza/configmap.yaml`, verify pod 2/2 Running + WAF route 403 test |
| CICD-RESULTS-001 | P2 | **Tekton Results Postgres single-instance rapuh saat node rotation** — `tekton-results-postgres-0` recreate lama ketika 4 worker SchedulingDisabled (rotasi nodeset 2026-08-24/25) → `error upserting record ... dial tcp :5432 connection refused` pada record PipelineRun/TaskRun, plus finalizer `results.tekton.dev/taskrun` macet (TaskRun lama dibersihkan manual via strip finalizer 2026-08-25). Build tetap sehat (`gateway-service/web-app 1.18.46 Completed 15/15`), tapi riwayat Tekton Results bisa bolong tiap rotasi node. | events `tekton-results-postgres statefulset is not ready` · `GetResult ... connection refused` | Evaluasi HA/CNPG untuk results PG atau dokumentasikan tolerance; monitor setelah rotasi selesai |
| ARGOCD-SYNC-001 | P3 | **4 Application ArgoCD OutOfSync** — `data-preprod/data-prod/data-sit/data-uat` OutOfSync tapi Healthy; banyak app lain sync status `Unknown` selama rotasi node. | `oc get applications.argoproj.io -n openshift-gitops` | Refresh/reconcile setelah rotasi selesai; telusuri sumber drift data-* |

Catatan sesi 2026-08-25: failure PipelineRun lama di `payu-cicd` (gateway-service-build-w8n7r/d78wt, web-app-build-56jr4/qpn4t, transaction-service-build-rpn86/76564) sudah disulih run hijau (`gateway-service-build-hq6pw`, `web-app-build-c7z4h` — tag 1.18.46, 15/15 tasks) dan dihapus dari cluster; akar masalahnya diperbaiki di `672b247c9` + `1b6133d4e`. Tidak perlu entri terpisah.

### Audit 2026-08-26 — Web Login & Onboarding (sisa OPEN saja; FIXED → CHANGELOG 1.18.47)

| Key | Pri | Temuan | Bukti | Sisa |
|:---|:---:|:---|:---|:---|
| RLS-ROLLOUT-001 | P1 | `TenantDataSource` decorator baru ter-build di account-service `1.18.47`; service lain (transaction/wallet/billing/partner/auth/dll.) masih bergantung pada mitigasi DB-level `ALTER ROLE payu SET app.tenant_id='default'` | `oc get deploy <svc> -o jsonpath={.image}` vs `1.18.47` | Rebuild + deploy service bertabel RLS di rilis berikutnya; regression test non-superuser per service; evaluasi pencabutan ALTER ROLE setelah semua ter-cover |
| SSO-ISSUER-002 | P1 | Pola issuer mismatch + realm drift kemungkinan besar terulang di sit/uat/preprod/prod (web-app issuer publik per-env vs backend internal; realm per env diimpor dari manifest berbeda-beda) | Pola identik terbukti di dev (6 lapis) | Audit per env dengan checklist yang sama: issuer token vs validator, realm users/secret/redirectUris vs git, DPoP attributes |
| SSO-DPOP-003 | P3 | DPoP (ADR-0062) enforcement dinonaktifkan di client `payu-web-app` karena BFF belum menghasilkan DPoP proof — sender-constrained tokens belum aktif | `GET /admin/realms/payu/clients/…/attributes` dpop=false | Implementasi DPoP proof generation di BFF/auth-service, lalu nyalakan kembali enforcement |
| NET-RATELIMIT-004 | P3 | Rate-limit auth gateway masih per-IP — seluruh user berbagi IP pod BFF; bump 120/min cukup untuk dev, per-user keying (dari refresh token/session) lebih tepat untuk prod | `gateway-service/application.yaml` rate-limit v1+v2 auth | Keying per-user untuk endpoint auth di gateway |
| GITOPS-LEGACY-005 | P3 | (a) Overlay monolith `overlays/payu-dev/kustomization.yaml` + app ArgoCD `payu-dev` sudah dihapus/orphan (duplikat management + render duplikat); file legacy di git belum dibersihkan. (b) 5 overlay simulator (`bi-fast/biller/dukcapil/qris/va-simulator`) rusak pre-existing: `resources` menunjuk file `base/*.yaml`, kustomize menolak | `oc kustomize overlays/payu-dev/bi-fast-simulator` → file-not-directory | Hapus file overlay monolith legacy; perbaiki resource simulator overlays atau arahkan ke base dir |

Catatan sesi 2026-08-26: audit + fix + E2E — login 3/3 stabil → dashboard; register UI→API **201** (NIK valid, data unik); CSP nonce onboarding 0/33 → 29/30; build Tekton web-app/gateway/account **15/15** `1.18.47`; spec kontrol `forgot-password`+`not-found` 2/2 PASS. Incident node mati `ip-10-0-88-91` (8 VolumeAttachment orphan → Multi-Attach, DB Pending) disulihkan dengan menghapus VA stale. Detail lengkap fix: `CHANGELOG.md` 1.18.47.
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

No open gap — seluruh ADR-GAP (003..009, 015, 019, 028W, 029, 030E, 032W, 047, 048, 054C, 056) **CLOSED 1.18.9–1.18.40** → `CHANGELOG.md`.

### 3. 📝 Backlog ADR Baru yang Perlu Dibuat

Semua ADR backlog sudah dibuat & terindeks — ADR-0067 **Deferred** (NO-GO B4.6), ADR-0068 **Accepted** (live 1.18.15, verified 1.18.40). Tidak ada ADR pending.

### 4. ⚠️ Kesenjangan Best Practice & Anti-Pattern yang Memerlukan Remediasi

No open gap — remediasi best-practice tuntas via harden items **CLOSED 1.18.19–1.18.40** → `CHANGELOG.md`.
