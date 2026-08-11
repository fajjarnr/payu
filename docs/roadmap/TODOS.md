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
| **Last Release** | `1.10.35` (2026-08-05) |
| **Core Banking MVP** | 🔴 Belum MVP — auth blocked (LOGIN-001..006 open); wallet/transaction money-flow live tapi 1 P0 (PROD-047). Account P0 (ACCOUNT-001..004) CLOSED 2026-08-11 (blind index, IDOR, trusted tenant, PII). Belum ada service production ready. |
| **Backlog Aktif** | 15 tickets + 24 action items (CB-*) + gates partner/platform (2026-08-11) |
| **Last Updated** | 2026-08-11 (ACCOUNT-001..004 closed, CB-001/CB-013 closed) |

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
| LOGIN-001 | P0 | Login web live: dulu HTTP 500 (Keycloak CrashLoop karena DB endpoint race — **root cause resolved 2026-08-11**, Keycloak Ready=True, CB-019 closed). Sisa: re-verify browser E2E login→dashboard setelah cluster up. | 🟡 Keycloak OK — E2E re-verify pending |
| LOGIN-002 | P0 | Logout tanpa revoke: tidak ada endpoint logout di auth-service; raw Keycloak refresh token langsung diteruskan. **CLOSED 2026-08-11**: `POST /api/v1/auth/logout` → Keycloak end_session (client_id+client_secret+refresh_token), gateway+SecurityConfig whitelist logout, replay refresh pasca-revoke → 400 AUTH_BUS_006. Live: login 200 → refresh 200 → logout 200 → replay 400. | ✅ Revoke + replay rejection live |
| LOGIN-003 | P0 | Password grant (KeycloakService.java:435); `evaluateRisk()` tidak dipakai (AUTH-001); MFA disabled. Done: OIDC Authorization Code + PKCE + MFA + E2E. | 🔴 Strong authentication absent |
| LOGIN-004 | P0 | Rate-limit: `RateLimitAspect` baca `value()` bukan `requests=10/20` (RateLimitAspect.java:46); fail-open cache down; client key `unknown`. **CLOSED 2026-08-11**: alias `requests` + fail-closed 503 + key per-account (JWT sub)/per-IP. | ✅ Brute-force control fixed |
| LOGIN-005 | P1 | Error contract login: `IllegalArgumentException` → 500; E2E terima false-green (503 valid / 500 salah password). **CLOSED 2026-08-11**: live-verified 200/401 invalid/423 locked/429 RATE_LIMIT_EXCEEDED/400 revoked-refresh — deterministik tanpa user enumeration; refresh replay 400 AUTH_BUS_006. | ✅ Contract fixed live |
| LOGIN-006 | P0 | Release gate login bukan vertical slice (unit hijau tapi login live gagal). Done: gate browser BFF→gateway→auth→Keycloak fail-closed di CI. | 🔴 CI false green |
| PROD-043 | P0 | Web-app money pakai JS `number`/`parseFloat` (FxService, Investment, split-bill, pocket, promotion, wallet store). Done: decimal string/minor unit + precision tests. | 🔴 Financial integrity |
| PROD-044 | P1 | Notification false success: SMS LOG mode `return true`, push mock, mailer `smtp.example.com` + `mock:true` (SmsSender.java:26-54, PushSender.java:8-23). Done: provider nyata fail-closed + delivery ID + E2E. | 🔴 Feature unusable |
| PROD-045 | P0 | Notification LOG mode bocor PII: recipient/title/body penuh di INFO log. Done: mask + log-sanitization test + scan log. | 🔴 Security/PII |
| PROD-046 | P1 | Kontrak referral web↔backend tidak cocok (referralCode/totalEarnings). Done: DTO selaras + E2E atau hapus klaim fitur. | 🟠 Partial feature |
| PROD-047 | P0 | `transaction-service Money` scale 2 HALF_EVEN vs standar DECIMAL(19,4) — 2 digit hilang (Money.java:40, MoneyTest assert scale 2). Done: scale 4 + round-trip exact tests. | 🔴 Financial data loss |
| INFRA-029 | P1 | Audit log forwarding: CLF live (CIS satisfied), sisa Wazuh SIEM sink (INFRA-011) + verifikasi log arrival. | 🟢 Live — sink pending |

---

## 🎯 Backlog Aksi (urut per priority)

### P0 — Money & Security Blockers

| Key | Domain | Item | Done saat |
|:---|:---|:---|:---|
| CB-002 | auth | Keycloak endpoint benar + E2E login + logout revoke + PKCE/MFA + rate-limit fail-closed (LOGIN-001..004/006) | LOGIN P0 closed + browser E2E green || CB-003 | transaction | `Money` scale 4 (PROD-047) + regression round-trip | PROD-047 closed |
| CB-004 | docs | Refresh `SERVICES.md` (stale, kontradiktif dengan TODOS) | SERVICES.md konsisten |
| CB-010 | fx | Fee `setScale(4, HALF_EVEN)` (FX-001, FxRateService.java:108) | FX-001 closed, test green |
| CB-014 | transaction | Kompensasi internal transfer: reversal bukan release setelah commit (TX-003) | Dana tidak hilang, test green |
| CB-016 | transaction | Bank code BI-FAST dari request + SmartRouting (BIFAST-001, InitiateTransferCommandHandler.java:217) | Transfer non-014 benar, test green |
| CB-020 | transaction | Fee transfer dipungut (FEE-001) atau fee=0 konsisten; ledger fee entry | Ledger = response, test green |
| CB-021 | transaction | Timeout RestTemplate QRIS & BI-FAST + circuit breaker (TIMEOUT-001) | Hang → release/FAILED, test green |
| CB-022 | billing | Subscription charge: wire wallet debit checkpoint (SUB-001) atau suspend | Charge hanya setelah debit sukses |
| CB-023 | investment | Sell idempotent: reference tetap + fee scale (INVEST-001) | Replay → 1 credit, test green |
| CB-024 | lending | PayLater: @Version + idempotency + money movement (PAYLATER-001) | Race/idempotency tests green |
| CB-029 | notification | Provider nyata fail-closed + delivery ID + mask PII (NOTIF-001/PROD-044/045) | E2E terima; log tanpa PII |

### P1 — Quality & Reliability

| Key | Domain | Item | Done saat |
|:---|:---|:---|:---|
| CB-005 | qa | Coverage gate: account ≥80% + integration tests wajib (ACCOUNT-006) | JaCoCo gate di CI |
| CB-006 | platform | Prod deploy core banking: gates + HPA≥2 + PDB2 + DR drill (ACCOUNT-007) | ACCOUNT-007 closed |
| CB-007 | qa | Money-safety regression suite lintas core (idempotency, outbox, DECIMAL(19,4), reversal, DLQ) | Suite green di CI |
| CB-012 | wallet | Ledger immutability di DB: REVOKE/trigger (WL-001) | UPDATE ledger ditolak DB |
| CB-015 | transaction | E2E transfer hop-by-hop incl. kompensasi | E2E green |
| CB-017 | transaction | QRIS idempotency DB fallback + fail-closed (QRIS-001) | Replay tidak double-charge |
| CB-018 | shared | Outbox failed-event: archive + alert, bukan DELETE (OUTBOX-001) | Event tidak hilang tanpa alert |
| CB-026 | promotion | Dedup cashback: unique transaction_id (PROMO-001) | Replay tanpa duplikat |
| CB-028 | dispute | Lock over-refund di `assertRefundable` (DISPUTE-001) | Concurrent partial refund aman |
| PROD-002 | fx | Approved FX provider URL/credential + live evidence | Rate live + audit pair |
| PROD-018 | analytics | Aktifkan `analytics-tests` sebagai required branch protection | CI gate aktif |

### P2 — Hardening & Secondary

| Key | Domain | Item | Done saat |
|:---|:---|:---|:---|
| CB-008 | transaction | MVP-003 VA settlement live E2E di payu-dev | Live E2E green |
| CB-009 | lending | Lending financial E2E fixture + integration test lending/fx/statement | Fixture + tests green |
| CB-011 | transaction | Versioning topic split-bills (TX-001) | Topic `.v1`, consumer updated |
| CB-025 | fx | FX reverse guard: status REVERSED + setScale (FX-002) | Double-reverse ditolak |
| CB-027 | promotion | Dedup loyalty redeem (PROMO-002) | Replay tidak double redeem |
| CB-030 | promotion | Referral lock + dedup (REFERRAL-001) | Double-complete mustahil |
| CB-031 | transaction | Scheduled transfer idempotency (TX-004) | Overlap tidak double debit |

### P3 — Backlog Lanjutan

| Key | Domain | Item |
|:---|:---|:---|
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
| PROD-047 | 🔴 | transaction | Money scale 2 vs DECIMAL(19,4) | Money.java:40 |
| ACCOUNT-003-RLS | 🟠 | account | ACCOUNT-003 closed via trusted-credential tenant + Hibernate filter + cross-tenant tests; PostgreSQL RLS (defense-in-depth) belum aktif — sama seperti remaining PARTNER-PROD-006 | V105/V106, TenantEnforcementAspect |
| LOGIN-003 | 🔴 | auth | password grant masih aktif; PKCE/MFA belum (AUTH-001: `evaluateRisk()` sudah dipanggil AuthController) | KeycloakService.java:435 |
| TX-003 | 🔴 | transfer | Kompensasi release setelah commit → dana hilang | WalletService.java:268-290 |
| BIFAST-001 | 🔴 | transfer | Bank code hardcoded "014" | InitiateTransferCommandHandler.java:217 |
| FEE-001 | 🔴 | transfer | Fee 2500/5000/25000 hanya di response | InitiateTransferCommandHandler.java:366-373 |
| TIMEOUT-001 | 🔴 | transfer | RestTemplate tanpa timeout (QRIS & BI-FAST) | QrisServiceAdapter.java:18-23 |
| SUB-001 | 🔴 | billing | Subscription charge `markSucceeded()` tanpa debit | SubscriptionService.java:395-401 |
| INVEST-001 | 🔴 | investment | Sell double-credit (reference random + @Retry) | InvestmentApplicationService.java:426-471 |
| PAYLATER-001 | 🔴 | lending | Race + non-idempotent + tanpa money movement | PayLaterTransactionService.java:36-115 |
| NOTIF-001 | 🔴 | notification | LOG-mode false success + PII di log | SmsSender.java:26-54 |
| OUTBOX-001 | 🔴 | shared | Failed event di-DELETE setelah 7 hari tanpa DLQ/alert | OutboxCleanupScheduler |
| FX-001 | 🟠 | fx | Fee setScale(2) vs DB 19,4 | FxRateService.java:108 |
| FX-002 | 🟠 | fx | Reverse tanpa status REVERSED; toAmount tanpa setScale | FxConversionService.java:118-160 |
| TX-001 | 🟠 | transaction | Topic split-bills tanpa `.v<n>` | SplitBillEventPublisherAdapter.java:46 |
| TX-004 | 🟠 | transaction | Scheduled transfer tanpa idempotency key | ScheduledTransferService.java:172-230 |
| WL-001 | 🟠 | wallet | Ledger immutability tidak di-enforce DB | V3 schema, LedgerEntryEntity |
| QRIS-001 | 🟠 | transaction | Idempotency cache-only fail-open (TTL 24h) | DistributedCacheIdempotencyRepository |
| AUTH-001 | 🟠 | auth | `evaluateRisk()` tidak dipakai; lockout cache-based | KeycloakService.java:359,412 |
| PROMO-001 | 🟠 | promotion | Cashback record duplikat saat replay | CashbackSagaOrchestrator.java:119-140 |
| PROMO-002 | 🟠 | promotion | Loyalty redeem tanpa dedup | LoyaltyPointsService.java:82-109 |
| DISPUTE-001 | 🟠 | dispute | Over-refund race (sum-then-check tanpa lock) | RefundService.java:153-164 |
| REFERRAL-001 | 🟠 | promotion | completeReferral tanpa lock | ReferralService.java:79-107 |
| TEST-GAP | 🟠 | qa | 6/8 core banking tanpa integration test; wallet 31 @Test | src/test structure |
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
