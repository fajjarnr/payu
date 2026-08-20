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
| **Last Release** | `1.13.15` (2026-08-19) |
| **Core Banking MVP** | 🔴 Belum MVP production ready — ACCOUNT-007/PROD-044 tetap terbuka; **login web live** (LOGIN-001..006 closed) |
| **Backlog Aktif** | 2 Active Tickets + 13 P1 aksi + 2 P3 + 4 cross-layer findings (🔴1 + 🟠3) + 3 best-practice OPEN (P1 2 + P2 1) + 1 infra/DX (sisa OPEN only — FIXED di `CHANGELOG.md`/`PROGRESS.md`) |
| **Last Updated** | 2026-08-20 — ADR-0048..0056 + TODOS priority tidy (WALLET-001 P1, SIM-001 P2, findings severity sort) |

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
| PROD-044 | P1 | Notification false success — **fail-closed live** (SMS/PUSH default NONE → false, LOG hanya eksplisit, `mailer.mock` tidak diwariskan ke prod, `KEYCLOAK_REALM` default). Sisa: implementasi provider [ADR-0027](../adr/0027-notification-service-architecture-and-multi-channel-delivery.md) (Telegram/SMS simulator/FCM v1) + multi-channel contacts + enkripsi PII `recipient`/`body` (lihat ARCH-NOTIF-001). | 🟠 Fail-closed live — implementasi ADR-0027 pending |
| INFRA-029 | P1 | Audit log forwarding: CLF live (CIS satisfied), sisa Wazuh SIEM sink via Syslog RFC5424 (INFRA-011 / [ADR-0032](../adr/0032-perimeter-security-waf-coraza-and-siem-wazuh.md)) + verifikasi log arrival. | 🟢 Live — sink pending |

---

## 🎯 Backlog Aksi (urut per priority — hanya OPEN)

### P1 — Quality & Reliability (In-Scope MVP)

| Key | Domain | Item | Done saat |
|:---|:---|:---|:---|
| PROD-002 | fx | Approved FX provider URL/credential + live evidence — std di [ADR-0050](../adr/0050-fx-provider-and-rate-governance-standard.md): single provider + BI fallback, cache TTL 5m, BigDecimal 19,4 HALF_EVEN, idempotency | Rate live + audit pair |
| PROD-018 | analytics | Aktifkan `analytics-tests` sebagai required branch protection — workflow `.github/workflows/analytics-tests.yml` sudah ada; sisa = setting GitHub branch protection (butuh `gh`/admin repo) | CI gate aktif via GitHub settings |
| QAMVP-004 | kyc | Security test DONE 2026-08-13; e2e workflow + CI `.github/workflows/kyc-tests.yml` DONE. Sisa: provider OCR/liveness nyata gate (butuh credential eksternal) — std Python di [ADR-0036](../adr/0036-python-fastapi-microservice-architecture-for-ai-ml-kyc-analytics.md): `python-starter` hexagonal-lite, PG/TimescaleDB + RLS + AES-GCM/HMAC, Kafka outbox `payu.<domain>.<event>.v<n>`, ONNX `<30ms`, OCR sidecar | Test + live evidence + ADR-0036 green |
| QAMVP-005 | platform | CI k6 wired 2026-08-13 — `.github/workflows/k6-tests.yml` (smoke/load/stress, SLO `p95<500ms`/`p99<1s`/`avg<300ms`/`rate<0.01`). Sisa: green run dengan kredensial staging | Laporan k6 di CI |
| ARCH-GLOBAL-002 | security | Step-Up Auth & Dynamic Linking [ADR-0028](../adr/0028-step-up-authentication-and-dynamic-linking-standard.md): `user_pins` Argon2id + 3-strike lockout, `POST /internal/v1/auth/step-up/{challenge,verify}` (Redis TTL 180s, `payload_digest = SHA256(sender+recipient+amount+currency+nonce)`), 2-phase `/prepare`→`/execute` di transaction-service, test suite PIN/lockout/expiry/tampering. | Test suite auth & transaction step-up green |
| ARCH-GLOBAL-003 | core-banking | ISO 20022 Clearing & Suspense Ledgering [ADR-0029](../adr/0029-iso20022-interbank-clearing-and-suspense-ledgering.md) + Saga [ADR-0038](../adr/0038-distributed-transaction-management-orchestrated-saga-pattern-with-persistent-state-machine.md): Chart of Accounts (`SYSTEM_BI_FAST_CLEARING` dll), `WalletClearingUseCase` (reserve/settle/reverse) via `TransferSagaOrchestrator` (`STARTED→COMPLETED/COMPENSATED`), `SagaState` persistent + `COMPENSATION_FAILED` alert, `X-Idempotency-Key=sagaId` | Double-entry clearing audit match + saga `COMPLETED/COMPENSATED` green |
| ARCH-GLOBAL-004 | risk-aml | Velocity & AML Risk Scoring [ADR-0030](../adr/0030-realtime-transaction-velocity-and-aml-risk-scoring.md) + Python [ADR-0036](../adr/0036-python-fastapi-microservice-architecture-for-ai-ml-kyc-analytics.md) + Saga [ADR-0038](../adr/0038-distributed-transaction-management-orchestrated-saga-pattern-with-persistent-state-machine.md): Redis `evaluate_velocity.lua` → `POST /api/v1/analytics/fraud/score` (`onnxruntime` p99 `<30ms`), 4-tier (ALLOW/REQUIRE_STEP_UP/HOLD_FOR_REVIEW/BLOCK_REJECT) sebagai `SagaStep` `HOLD_FOR_REVIEW→PAUSED` | Velocity & hold-review tests green + saga pause/resume green |
| ARCH-GLOBAL-005 | platform | DB HA, PITR & DR Drill [ADR-0031](../adr/0031-database-resilience-pitr-and-disaster-recovery.md): `barmanObjectStore` S3 WAL (`archive_timeout=60s`) + VolumeSnapshot harian, runbook CNPG, skrip `scripts/backup-dr/`, RTO<5m/RPO=0. | PITR drill verified + failover RTO<15s |
| ARCH-GLOBAL-006 | security | Perimeter Security [ADR-0032](../adr/0032-perimeter-security-waf-coraza-and-siem-wazuh.md): Coraza WAF (CRS v4.x, PL1/PL2, SNAP-BI exclusions), Wazuh cluster + CLF Syslog RFC5424 `tcp://wazuh-manager.wazuh.svc.cluster.local:514`. | Wazuh dashboard live + CLF arriving + Coraza block test |
| ARCH-GLOBAL-007 | data-security | RLS & Multi-Tenant Isolation [ADR-0033](../adr/0033-database-row-level-security-and-multi-tenant-isolation-standard.md): `TenantAwareTransactionSynchronization` (`SET LOCAL app.tenant_id`), role `payu_migrator` vs `payu_app`, `FORCE ROW LEVEL SECURITY` 27 tabel, JWT `partner_id`/`tenant_id` + gateway sanitization. | `BlindIndexAndTenantIsolationIntegrationTest` green + 0-row mismatch |
| ARCH-GLOBAL-008 | observability | Observability & Tracing [ADR-0034](../adr/0034-end-to-end-observability-slo-sli-and-distributed-tracing-standard.md): Multi-Window Multi-Burn-Rate alerts (1h/14.4x & 6h/6x page, 24h/3x & 3d/1x ticket), W3C `traceparent` (HTTP/Kafka CE/Postgres), OTel tail-sampling (100% financial/errors, 5% reads), PII masking, Grafana partner dashboard. | Alerts active + trace E2E + Tempo arriving |

> `ARCH-TOPIC-002` — manifest DONE 2026-08-18 (107 KafkaTopic: 65 normal + 42 DLQ, retention 30d, `EVENT_CATALOG.md` regenerated). Sisa apply ke cluster + `auto-create off` butuh OCP creds — tracked di Platform Deploy Queue.

### P2 — Defer (Out-of-Scope MVP, ADR-0023)

> ✅ **Seluruh backlog P2 aksi CLOSED 2026-08-12** (CB-008/011/017/022/024/025/031/036) — lihat CHANGELOG `1.10.63`.

### P3 — Backlog Lanjutan

| Key | Domain | Item |
|:---|:---|:---|
| READY-060 | card | Card tokenization + 3DS |
| READY-062 | ml | ONNX fraud detection model — `onnxruntime` per [ADR-0036](../adr/0036-python-fastapi-microservice-architecture-for-ai-ml-kyc-analytics.md) (`ml/fraud_detection`, S3 `payu-models/<name>/v<n>/model.onnx`, p99 `<30ms`) |

---

## 🏦 Partner Service Production Readiness Gate

Status `partner-service` hanya Production Ready setelah seluruh gate memiliki bukti live. `PARTNER-001..006` CLOSED (2026-08-08).

| Gate | Pri | Status | Sisa |
|:---|:---:|:---|:---|
| PARTNER-PROD-001 | P0 | 🟢 Public edge APIcast LIVE (sandbox) | WAF Coraza (DEPLOY-006 / [ADR-0032](../adr/0032-perimeter-security-waf-coraza-and-siem-wazuh.md)), mTLS APIcast→gateway, rate-limit per-IP, runbook restart |
| PARTNER-PROD-002 | P0 | 🟢 Enkripsi at-rest + rotation + backfill LIVE (V18, 0 plaintext) — std di [ADR-0040](../adr/0040-field-level-encryption-searchable-encryption-via-hmac-blind-indexing-and-key-lifecycle.md): AES-256-GCM + HMAC blind `*_bidx`, Vault KEK/DEK, rotation `90d` | Vault key management production |
| PARTNER-PROD-003 | P0 | 🟢 Webhook trust boundary LIVE | Egress policy eksplisit, response-body scan |
| PARTNER-PROD-004 | P0 | 🟢 Delivery durability LIVE | DLQ consumer/alert otomatis |
| PARTNER-PROD-005 | P0 | 🟢 Reconciliation LIVE — std di [ADR-0041](../adr/0041-transactional-outbox-pattern-with-polling-skip-locked-dispatcher-vs-debezium-cdc.md): polling `SKIP LOCKED` + CloudEvents `payu.*.v<n>` + DLQ `*.dlq` | Reconcile outbox, auto-resolve, alert destination |
| PARTNER-PROD-006 | P0 | 🟢 Tenant isolation LIVE | RLS GUC integration, partner-scoped Keycloak roles, audit list query |
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
| DEPLOY-006 | P1 | Security | Coraza WAF (INFRA-015) + Wazuh SIEM (INFRA-011) + CLF sink ([ADR-0032](../adr/0032-perimeter-security-waf-coraza-and-siem-wazuh.md)) |
| DEPLOY-011 | P1 | Promotion | SIT/UAT/preprod LIVE di lab `cluster-nkk8q` (ArgoCD 18 apps, Vault HA, pipeline SIT green). Sisa: litmus `pod-delete` via [ADR-0024](../adr/0024-chaos-engineering-and-fault-injection-strategy.md), Kraken+Cerberus preprod, Infinispan mTLS, prod sync window |
| INFRA-026 | P1 | Secrets | Vault HA live + restore drill verified. Sisa: snapshot S3 CronJob verify, kv readback, auto-unseal backup |
| DEPLOY-009 | P2 | CI/CD | Tekton Results live (365d); sisa: external HA PostgreSQL, Chains SLSA/Rekor evidence, Renovate |
| DEVSECOPS-017 | P1 | Secrets | Tekton Buildah butuh `redhat-registry-pull` workspace + Vault `secret/payu/cicd/redhat-registry` |
| OPS-2026-08-01-05 | P2 | Chaos | Kraken manifest fixed (emptyDir + SCC); re-run preprod gate saat CPU pulih ([ADR-0024](../adr/0024-chaos-engineering-and-fault-injection-strategy.md)) |
| OPS-2026-08-01-04 | P2 | Observability | Log delivery: vector OK; blocked 403 `lokistack-gateway.rego` kosong (operator bug LOG-2236). **2026-08-13**: recurring ERROR dibersihkan (cache `Optional`, OJK timer DNS, account IAM 401 legit) |
| OPS-2026-04-08-02 | P2 | Performance | k6 via operator/port-forward only (gateway unreachable dari host) |
| READY-029 | P2 | Performance | Gatling defer ke cluster phase |
| READY-030 | P2 | Performance | SOAK 24h defer ke staging |
| INFRA-018 | P3 | Registry | Image hilang saat upgrade (31 tag) — investigasi prune + GC policy |
| INFRA-019 | P3 | Registry | Quay.io auto-prune policy |
| DEVSECOPS-005 | P3 | Network | EgressNetworkPolicy + Istio egress gateway |
| DEVSECOPS-007 | P3 | Security | LUKS encryption PV + Vault DEK rotation |
| DEVSECOPS-012 | P3 | Cost | Monthly cost report workflow |

---

## 📋 Open Findings — Sisa OPEN Only (FIXED → CHANGELOG/PROGRESS)

> Aturan: section ini hanya untuk temuan yang masih OPEN. Seluruh temuan ✅ FIXED/CLOSED sudah dipindah ke `CHANGELOG.md` `1.12.0`/`1.13.0` dan `PROGRESS.md`. Jangan tambahkan baris duplikat yang sudah ada di Backlog Aksi / Platform Deploy Queue.

### Audit Arsitektur 2026-08-13 — Sisa Sistematis

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| ARCH-DLQ-001 | 🟠 | promotion, cms, dispute, statement, platform | Tanpa `.dlq` wiring per-service; outbox event gagal permanen kini di-copy best-effort ke `destinationTopic + .dlq` (platform DONE 2026-08-13) + 42 DLQ topics declared retention 30d (2026-08-18). Sisa: consumer per service menunggu alert destination; `OutboxCleanupScheduler` log `OUTBOX-001 ALERT` sebagai safety net. `scripts/dlq-replay.sh` P1 — std di [ADR-0041](../adr/0041-transactional-outbox-pattern-with-polling-skip-locked-dispatcher-vs-debezium-cdc.md): `SKIP LOCKED` + `*.dlq` | OutboxCleanupScheduler.java:77-85 |
| ARCH-DEDUP-001 | 🟠 | partner, promotion | Migrasi dedup DELETE baris finansial pre-constraint (`snap_bi_payments`/`refunds`/`cashbacks`/`rewards`) — legal hanya jika belum pernah jalan di prod; perlu bukti env + policy | partner V16/V17; promotion V11/V12 |
| ARCH-FLYWAY-001 | 🟠 | account | Destruktif historis `DROP COLUMN` + `RENAME COLUMN` di migrasi ter-aplikasi — anti-pattern, risiko fresh-restore; jangan diulang | account V10:16-27 |

### Audit 2026-08-16 — Deep Quality (sisa OPEN)

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| MOBILE-JSX-001 | 🟠 | mobile | `useTransactionQuery.test.ts` dan `useWalletQuery.test.ts` JSX di file `.ts` (bukan `.tsx`) — Babel parse error | src/__tests__/hooks/useTransactionQuery.test.ts:84 |
| MOBILE-MOCK-001 | 🟡 | mobile | `accessibility.test.tsx` me-mock `react-native` tanpa stub `NativeSettingsManager`/`SettingsManager` — langgar TurboModule Registry RN 0.76 | frontend/mobile/src/testing/accessibility.test.tsx:58-63 |

### Audit 2026-08-18 — Web ↔ Gateway ↔ Backend Cross-Layer (hanya OPEN)

| Key | Sev | Domain | Ringkasan | Bukti |
|:---|:---:|:---|:---|:---|
| GW-ROUTING-003 / BE-BIO-001 | 🔴 | gateway/biometric | 5 endpoint `/api/v1/biometric/*` 404 — **best practice W3C WebAuthn/FIDO2 + ADR-0028 step-up**: challenge 32B, `user_pins` Argon2id 3-strike, Redis TTL 180s `payload_digest=SHA256(sender+recipient+amount+currency+nonce)`, `POST /internal/v1/auth/step-up/{challenge,verify}` + WebAuthn `attestation/assertion` — ref [ADR-0028](../adr/0028-step-up-authentication-and-dynamic-linking-standard.md) + [ADR-0039](../adr/0039-nextjs-app-router-bff-security-token-relay-and-session-management-standard.md) — tambah `biometric-service` atau `auth-service` controller + gateway `RouteRegistry` | `AuthService.ts:152-187`; `RouteRegistry.java:123-200` |
| BE-SUPP-001 / FE-STUB-002 | 🟠 | support | `support-service` hanya training agent; tanpa API `/tickets` & FAQ publik (UI statis) — **best practice ITIL**: Ticket (OPEN→IN_PROGRESS→WAITING_CUSTOMER→RESOLVED→CLOSED), FAQ CMS, SLA 24j, idempotency `X-Idempotency-Key`, outbox `payu.support.ticket-created.v1` + `.dlq`, RLS `tenant_id`, encrypt PII — ref [ADR-0020](../adr/0020-support-centralized.md) (centralized) — implement `POST /api/v1/support/tickets` + `GET /faqs` + persist `support_tickets` | `SupportController.java:25-100`; `support/page.tsx:56-58` |
| FE-STUB-003 | 🟠 | qris | `qris/page.tsx` simulasi `setTimeout` tanpa EMVCo — **best practice EMVCo 4.3 + SNAP-BI QRIS (ADR-0025)**: decode TLV, CRC16 X25, tag 26/30 Merchant, 54 amount, 59 name, validate checksum, query `GET /accounts/{id}/qris` (personal QR hash `qrCodeHash`), limit harian, mutasi `POST /snap-bi/qris` via gateway idempotency — ref [ADR-0025](../adr/0025-snap-bi-and-partner-gateway-security-standard.md) | `qris/page.tsx:25-34,109-124` |
| FE-STUB-004 | 🟠 | auth | `forgot-password-form.tsx` toast tanpa Keycloak — **best practice OIDC Authorization Code + PKCE (ADR-0039)**: `POST /auth/forgot-password` → Keycloak `execute-actions-email` OTP, rate-limit IP, audit `payu.auth.password-reset-requested.v1` — ref [ADR-0039](../adr/0039-nextjs-app-router-bff-security-token-relay-and-session-management-standard.md) + RHBK docs | `forgot-password-form.tsx:22` |

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
| ADR-ALIGN-001 | [ADR-0016](../adr/0016-arch-006-phase-a-strategy.md) | Deferred (2026-06-14) | ✅ Accepted / Completed | Kode 100% migrasi Java 25 + Spring Boot 4.1.0 (`backend/pom.xml` + 17 starters). Update status dokumen. |
| ADR-ALIGN-002 | [ADR-0014](../adr/0014-api-management-platform.md) | Proposed (2026-03-02) | 📝 Accepted (Tiered) | Finalisasi 2-tier: Tier 1 Public Edge (APIcast/3scale) vs Tier 2 Core Gateway (Quarkus). |

### 2. 🔴 ADR yang Sudah Ada tapi Belum / Sebagian Diimplementasikan

| Key | ADR Terkait | Domain | Ref Backlog Tunggal |
|:---|:---|:---|:---|
| ADR-GAP-001 | [ADR-0015](../adr/0015-process-automation-rhpam.md) | lending, compliance, routing | Phase 1 Drools parsial di `lending-rules`; Phase 2 DMN (routing & promo) & Phase 3 Kogito BPMN (loan-origination & KYC) belum — tracked sebagai debt P3/loan |
| ADR-GAP-002 | [ADR-0027](../adr/0027-notification-service-architecture-and-multi-channel-delivery.md) | notification | → **ARCH-NOTIF-001** di Backlog Aksi P1 (fail-closed live, provider + enkripsi pending) |
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
| ARCH-BESTP-003 | lending | 🔴 P1 — Hardcoded rules sisa — migrasi tuntas DRL/DMN (ADR-0015 Phase 2 + ADR-0048): DRL `credit_scoring.drl` stay (15 rules via `rules-starter`), migrasi `LoanPreApprovalService.java:41/144/156` & `LendingApplicationService.java:261/269` (threshold 650/600, interest tiers 12/14/16/18%, conditional 0.85/0.70, tenure cap 24, RiskCategory EXCELLENT..VERY_POOR) ke DMN `eligibility.dmn`+`pricing.dmn` (FEEL, `rules-starter` DMN exec-model), hapus duplikat `backend/lending-rules` fork (`id.payu.lendingrules.domain.CreditScoringFact`), hot-reload Git→classpath dulu (next KieScanner SNAPSHOT/file `kie-ci`), audit `security-starter` — glossary di `CONTEXT.md` §Lending — ref [ADR-0015](../adr/0015-process-automation-rhpam.md) + [ADR-0048](../adr/0048-lending-eligibility-and-pricing-via-dmn-decision-tables.md) | 🔴 OPEN |
| WALLET-001 | wallet | 🔴 P1 — Immutable ledger double-entry — JournalEntry header + LedgerEntry append-only `REVOKE UPDATE/DELETE`, CoA enum, `Wallet` materialized `balance/reservedBalance` `version`, invariant `isBalanced` per journal, idempotency `transactionId` unique, reversal via new journal — std di [ADR-0049](../adr/0049-wallet-immutable-ledger-and-double-entry-standard.md) (ref ADR-0022+0029), glossary `CONTEXT.md` §Wallet — impl `JournalPersistenceAdapter` + outbox `payu.wallet.*.v1` | 🔴 OPEN |
| SIM-001 | simulator | 🟠 P2 — Simulator fidelity & contract parity — SNAP-BI headers `X-TIMESTAMP/X-SIGNATURE/X-PARTNER-ID/X-EXTERNAL-ID`, idempotency dedup `referenceNumber`/`X-Idempotency-Key`, deterministic `X-Simulate` header (success/blocked/timeout/5xx), EMVCo TLV CRC16 for QR, latency `min/max` + `failure-rate` via `SimulatorConfig` `@ConfigMapping`, HMAC lab vs RSA prod, `lab` profile guard — std di [ADR-0056](../adr/0056-simulator-fidelity-and-contract-testing-standard.md) (ref ADR-0025), gaps `biller`/`va` no README + `qris` TLV — fix per simulator | 🔴 OPEN |
| ARCH-BESTP-004 | docs | 🟡 P3 — ADR-0008..0013 sangat singkat — perlu pendalaman (Hot Rod ProtoStream, BFF security, Testcontainers) | 🔴 OPEN |
| ARCH-BESTP-001 | gateway | 🟢 Done — Scheduled tasks Quarkus tanpa distributed lock (lihat `GW-CONCUR-001`) — perlu lock Redis/DB ala ShedLock — std di [ADR-0042](../adr/0042-distributed-job-scheduling-and-cluster-wide-concurrency-lock-standard-using-shedlock.md): `shedlock` `JdbcTemplate` + Quarkus `quarkus-shedlock` | 🟢 Accepted |
| ARCH-BESTP-002 | grpc | 🟢 Done — File `.proto` tersebar per-service tanpa `proto-commons` / Buf central repo — risiko drift — std di [ADR-0037](../adr/0037-internal-synchronous-inter-service-communication-via-grpc-and-protobuf-governance.md): `backend/shared/proto-commons` + Buf lint/breaking, `grpc 1.83.1` `protobuf 3.25.5` gov, Istio mTLS `PLAINTEXT`, deadline 1s + retry idempotent, `common.proto` single source | 🟢 Accepted |