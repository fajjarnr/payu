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
| **Last Release** | `1.10.64` (2026-08-12) |
| **Core Banking MVP** | 🔴 Belum MVP — blocker tersisa: ACCOUNT-006/007 (P1) + PROD-044 (P1); **login web live** (LOGIN-001..006 closed: PKCE + gate CI + browser E2E), money-flow live (PROD-043/045/047, CB-014/016/020/021/023 closed). Belum ada service production ready. |
| **Backlog Aktif** | 3 tickets + 18 action items (CB-*/PROD-*/READY-*/DEVSECOPS-*) + gates partner/platform (2026-08-12) |
| **Last Updated** | 2026-08-12 (WALLET-002/WALLET-001/GRPC-008 + CB-027/030/032/033 closed; stack podman live 34 containers, image semver `1.10.64`) |

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
| ACCOUNT-006 | P1 | Coverage account ~21% line/19% branch; integration test tidak required di CI. Done: ≥80% overall, 100% core domain, required CI. | 🟠 Test gate insufficient |
| PROD-044 | P1 | Notification false success — **PARTIAL 2026-08-12**: fail-closed live (SMS/PUSH default NONE → false, LOG hanya eksplisit, `mailer.mock` tidak diwariskan ke prod, `KEYCLOAK_REALM` default). Sisa (butuh credential provider eksternal): provider nyata + delivery ID + E2E terima. | 🟠 Fail-closed live — provider pending |
| PROD-046 | P1 | Kontrak referral web↔backend tidak cocok (referralCode/totalEarnings). Done: DTO selaras + E2E — CLOSED 2026-08-12 (totalEarnings real di backend summary, tipe web selaras, UI pakai referralCode). | ✅ Closed |
| INFRA-029 | P1 | Audit log forwarding: CLF live (CIS satisfied), sisa Wazuh SIEM sink (INFRA-011) + verifikasi log arrival. | 🟢 Live — sink pending |

---

## 🎯 Backlog Aksi (urut per priority)

### P1 — Quality & Reliability (In-Scope MVP)

| Key | Domain | Item | Done saat |
|:---|:---|:---|:---|
| CB-005 | qa | Coverage gate: account ≥80% + integration tests wajib (ACCOUNT-006) | JaCoCo gate di CI |
| CB-029 | notification | Provider nyata fail-closed + delivery ID (PROD-044) — fail-closed DONE 2026-08-12; sisa provider nyata + delivery ID + E2E (butuh credential provider eksternal) | E2E terima; log tanpa PII |
| CB-006 | platform | Prod deploy core banking: gates + HPA≥2 + PDB2 + DR drill (ACCOUNT-007) | ACCOUNT-007 closed |
| CB-007 | qa | Money-safety regression suite lintas core — **CLOSED 2026-08-12** (13 passed / 2 skipped live: atomic transfer + replay idempotency + ledger double-entry + register + statement + ownership guard). Wiring pipeline ikut fase platform Tekton (DEPLOY-009), bukan GH Actions (project pindah dari GH Actions); billing coverage nunggu podman-compose parser fixed (L-225); QRIS positive case saat provider tersedia | Suite green (live) |
| CB-015 | transaction | E2E transfer hop-by-hop incl. kompensasi — **E2E green 2026-08-12** (atomic 1-hop CB-034 + replay idempotency + ledger DEBIT/CREDIT legs + balance after) | E2E green |
| PROD-002 | fx | Approved FX provider URL/credential + live evidence | Rate live + audit pair |
| PROD-018 | analytics | Aktifkan `analytics-tests` sebagai required branch protection — workflow `.github/workflows/analytics-tests.yml` SUDAH ada (push/PR paths + workflow_dispatch); sisa = setting GitHub branch protection (butuh `gh`/admin repo, belum tersedia di sesi ini) | CI gate aktif via GitHub settings |

### P2 — Defer (Out-of-Scope MVP, ADR-0023)

> ✅ **Seluruh backlog P2 aksi CLOSED 2026-08-12** (CB-008/011/017/022/024/025/031/036) — lihat CHANGELOG `1.10.63`. Tidak ada item tersisa.

### P3 — Backlog Lanjutan

| Key | Domain | Item |
|:---|:---|:---|
| CB-009 | lending | Lending financial E2E fixture + integration test lending/fx/statement (defer) |
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
| SUB-001 | 🔴 | billing | Subscription charge `markSucceeded()` tanpa debit — **CLOSED (CB-022, 2026-08-12)**: reserve→commit wallet sebelum markSucceeded; commit gagal → release + dunning; test release-on-commit-failure | SubscriptionService.processCharge |
| PAYLATER-001 | 🔴 | lending | Race + non-idempotent + tanpa money movement — **CLOSED (CB-024, 2026-08-12)**: pessimistic lock `findByUserIdForUpdate` (@Query+@Lock) + idempotency via `X-Idempotency-Key`/externalId (unique) + wallet credit (purchase) / collectRepayment (payment) | PayLaterRepository, PayLaterTransactionService, WalletGrpcPaymentAdapter |
| NOTIF-001 | 🔴 | notification | LOG-mode false success tanpa delivery ID — **PARTIAL 2026-08-12** (fail-closed live, lihat PROD-044); sisa provider nyata + delivery ID butuh credential eksternal | SmsSender.java:26-54 |
| OUTBOX-001 | 🔴 | shared | Failed event di-DELETE setelah 7 hari tanpa DLQ/alert — CLOSED (CB-018, 2026-08-12): archive in-place + ERROR alert `OUTBOX-001 ALERT`, tidak pernah delete. Sisa: sink alert nyata (Slack/PagerDuty) via Vault (DEVSECOPS-017 drift alert) + auto-move ke `.dlq` | OutboxCleanupScheduler |

| FX-002 | 🟠 | fx | Reverse tanpa status REVERSED; toAmount tanpa setScale — **CLOSED (CB-025, 2026-08-12)**: reverse guard COMPLETED→REVERSED (sudah ada, di-test) + `FxConversion.setToAmount` setScale(4) HALF_EVEN (test scale 4) | FxConversion.java:46,72; FxConversionServiceTest |
| GRPC-001 | 🔴 | account | Proto `AccountService` defined tapi **0 implementasi server** — **CLOSED 2026-08-12**: `AccountGrpcService` (GetAccount/GetAccountsByUser/VerifyAccount/GetAccountByNumber/AccountExists; Create/Update UNIMPLEMENTED fail-closed) + `grpc-starter` + protobuf-maven-plugin + port `findByUserId`/`findByAccountNumber`; `AccountGrpcServiceTest` 6 test; live: gRPC 9090 listening di podman. Sisa: migrasi pemanggil REST (transaction `AccountServiceAdapter`, lending `AccountClient`) ke gRPC | AccountGrpcService; AccountGrpcServiceTest |
| GRPC-002 | 🔴 | transaction | Proto `TransactionService` defined tapi **0 implementasi server** — **CLOSED 2026-08-12**: `TransactionGrpcService` (GetTransaction/GetByReference/GetHistory/GetByAccount/ExistsByReference; Create/Update UNIMPLEMENTED fail-closed — money writes butuh idempotency dulu) + `TransactionGrpcServiceTest` 6 test; live: gRPC 9090 listening di podman. Sisa: migrasi statement client (GRPC-005) | TransactionGrpcService; TransactionGrpcServiceTest |
| GRPC-003 | 🔴 | shared | `payu.grpc.enabled` **hanya** menggating `WalletGrpcAdapter` transaction-service — **CLOSED 2026-08-12**: conditional dihapus — gRPC jadi jalur default di SEMUA service (parity); REST adapter tetap ada sebagai fallback | WalletGrpcAdapter transaction |
| GRPC-004 | 🟠 | transaction | `PaymentExpiryScheduler` panggil wallet-service **raw `RestTemplate` langsung ke URL** — **CLOSED 2026-08-12**: endpoint yang dipanggil (`/wallets/{accountId}/release`) tidak ada di wallet (404 diam-diam per 5 menit); kini lewat `WalletServicePort.releaseBalance` (gRPC adapter, reservationId) — hexagonal boundary dipulihkan, test `PaymentExpirySchedulerTest.releasesReservedBalanceThroughWalletPort` | PaymentExpiryScheduler.java:59,120-126 → WalletServicePort |
| GRPC-005 | 🟠 | statement | `TransactionServiceClient` REST **tidak deprecated** (tidak ada gRPC alternatif, karena GRPC-002 server-nya juga belum ada) — statement satu-satunya service yang belum migrasi gRPC sama sekali. **2026-08-12**: (GRPC-008) kontrak client di-align; **(GRPC-005 partial) client dipindah ke `adapter.client`** + `TransactionRecord`/`TransactionType` ke `statement.dto` — hexagonal layering ArchUnit green (56/56). Sisa: migrasi gRPC penuh menunggu GRPC-002 (server transaction gRPC) | TransactionServiceClient.java:14,19 |
| GRPC-006 | 🟡 | integration | `grpc-starter` di pom tapi **0 proto + 0 kode gRPC** — **CLOSED 2026-08-12**: dependency mati dihapus dari pom (0 usage Java/config/proto; build + test green tanpa starter) | integration-service/pom.xml |
| GRPC-007 | 🟡 | wallet | Server gRPC 9090 di-start **grpc-starter sendiri**, spring-grpc aktif parallel (in-process/servlet) → WalletGrpcService diregister 2× — **CLOSED 2026-08-12**: `spring.grpc.server.enabled: false` di application.yml wallet; hanya Netty 9090 starter yang serve | wallet application.yml |
| GRPC-009 | 🟡 | billing | Dua bean REST mati sekaligus: `WalletAdapter` (`walletRestAdapter`) + `WalletClient` — **CLOSED 2026-08-12**: keduanya dihapus (hanya dirujuk javadoc; service pakai `WalletPort` → gRPC @Primary); `BillingIntegrationTest` kini mock `WalletPort` (mock `WalletClient` sebelumnya tidak efektif) | deleted |
| GRPC-010 | 🟡 | gateway | Gateway (Quarkus) punya **salinan proto wallet sendiri** — **CLOSED 2026-08-12**: dihapus bersama bridge (GRPC-019/013) — gateway tidak lagi konsumsi gRPC wallet | deleted |
| GRPC-011 | 🔴 | shared | **Semua client gRPC pakai `ManagedChannelBuilder` raw** — **CLOSED 2026-08-12**: `GrpcChannelSupport` (grpc-starter) dipakai 8 adapter (7 service) — channel + stub blocking dengan deadline 30s (default); call tidak bisa hang tanpa batas. Sisa (tak dikerjakan): migrasi `@GrpcClient` spring-grpc + interceptor starter di channel | GrpcChannelSupport |
| GRPC-016 | 🔴 | shared | **0 resilience di semua 7 gRPC client adapter** — **CLOSED 2026-08-12**: `@CircuitBreaker("walletService")` + `@Retry` class-level di 8 adapter (retry aman — op wallet idempotent by referenceId). Sisa: fallback per-metode (read path) bila perlu | WalletGrpcAdapter* @CircuitBreaker |
| GRPC-017 | 🟡 | shared | **Config client gRPC di `application-grpc.yml` dead** — **PARTIAL 2026-08-12**: (a) prefix dibetulkan `spring.grpc.client.channels.<name>.*`; (b)/(c) belum — interceptor `@GlobalClientInterceptor` + adopsi `@GrpcClient` menunggu GRPC-001/002 server + refactor client | application-grpc.yml |
| GRPC-018 | 🟡 | shared | **grpc-java 1.69.0 outdated** — **CLOSED 2026-08-12**: bump ke **1.83.1** (Context7: Java 8+, backward compat; protoc-gen-grpc-java 1.83.1 + protoc 3.25.x OK) — starter + 8 service suite green | grpc-starter pom grpc.version |
| GRPC-019 | 🔴 | gateway | **Bridge `/api/internal/grpc/wallet/*` authz = valid JWT saja** — **CLOSED 2026-08-12**: bridge dihapus total (0 pemanggil, GRPC-013) bersama `WalletGrpcBridge` + salinan proto wallet (GRPC-010) + dep `quarkus-grpc` — tidak ada lagi path explotable debit/credit/transfer wallet arbitrer; wallet gRPC hanya bisa diakses service-to-service (mesh) | GrpcBridgeResource/WalletGrpcBridge deleted |
| GRPC-020 | 🟡 | wallet | Validasi scale 4 tidak konsisten di jalur gRPC — **CLOSED 2026-08-12**: `reserveBalance` + `credit` kini enforce `scale() > 4` (sama dengan `transfer`); reject sebelum state change; test `WalletScaleValidationTest` (reject scale>4 tanpa save/ledger/event, accept scale 4) | WalletService.java:152,336 vs 438,517; WalletScaleValidationTest |
| GRPC-021 | 🟡 | shared | **Tidak ada enforcement rule** — **CLOSED 2026-08-12**: (a) rule ArchUnit `httpClientsOnlyInClientAdapters` (RestTemplate/WebClient/RestClient/OkHttpClient hanya di `adapter.client`, config exempt) ditambahkan ke `HexagonalArchitectureRules` + base test + di-wire ke wallet & transaction ArchitectureTest — PaymentExpiryScheduler pelanggaran terakhir sudah diperbaiki (GRPC-004); (b) grpc-starter kini punya test (`GrpcStarterPropertiesTest` — binding `payu.grpc.*` server/clients/interceptors) | HexagonalArchitectureRules.httpClientsOnlyInClientAdapters; GrpcStarterPropertiesTest |
| GRPC-022 | 🟡 | shared | **Proto multi-source drift**: 7 service salin `WalletService.proto` sendiri — **CLOSED 2026-08-12**: `WalletProtoContractTest` (snapshot field-number wire contract: Debit/ReserveBalance/Credit/Transfer/GetBalance + subset lending RepayLoan/Credit) dipasang di 8 service — rename/renumber di copy mana pun sekarang memecah build, bukan wire. Terverifikasi: semua copy saat ini wire-identik | WalletProtoContractTest (8 service) |
| WALLET-001 | 🔴 | wallet | **Runtime bug transaksional (verified live)** — **CLOSED 2026-08-12**: `application-local.yml` prefix `spring.datasource.primary.*` tanpa `.hikari` → pool autoCommit=true + `provider_disables_autocommit` → `Cannot commit when autoCommit is true`; fix prefix + regression `TransactionalPoolAutoCommitTest` (assert pool autoCommit=false + tx commit bersih vs Testcontainers PG; red bila auto-commit=true) | application-local.yml; TransactionalPoolAutoCommitTest |
| WALLET-002 | 🔴 | wallet | **Migration gap (verified live)** — **CLOSED 2026-08-12**: `split_payment_legs.settled_at` tidak pernah ada di migrasi (V10 pakai `credited_at`) + `split_recipients.recipient_type` beda nama dgn kolom `type` di V10 → Hibernate validation fail pada fresh DB (live DB hand-patched). Fix: migration idempotent **V113** + `SchemaMigrationIntegrityTest` (boot context fresh DB + Flyway + validate) | V113__align_split_payment_schema_with_entities.sql; SchemaMigrationIntegrityTest |
| GRPC-012 | 🔴 | wallet | **Proto `Debit` salah implementasi** — **CLOSED 2026-08-12**: `WalletGrpcService.debit()` kini memanggil use case `WalletService.debit` (balance berkurang nyata, pessimistic lock, idempotent by referenceId, ledger DEBIT + wallet_transaction DEBIT, scale-4 guard) — sebelumnya `reserveBalance` (balance tidak bergerak, caller dapat reservationId sebagai transactionId; jalur FX conversion mengandalkannya). Test `WalletServiceDebitTest` (debit mengurangi balance, insufficient balance reject tanpa state change, replay idempotent, scale>4 reject) | WalletService.debit; WalletServiceDebitTest |
| GRPC-013 | 🟠 | gateway | **Bridge `/api/internal/grpc/wallet/*` = 0 pemanggil** — **CLOSED 2026-08-12**: dihapus bersama GRPC-019 (dead code) | deleted |
| GRPC-014 | 🟠 | shared | **`GrpcAuthInterceptor.ServerInterceptor` ALLOW anonymous** — **CLOSED 2026-08-12**: enforcement configurable `payu.grpc.interceptors.auth.require-token` (default false — client belum kirim token, mesh mTLS tetap kontrol live; aktifkan per-server setelah client kirim token); anonymous ditolak `UNAUTHENTICATED` saat flag on; `GrpcAuthInterceptorEnforcementTest` 2 test (default allow + enforce reject). Starter kini punya 8 test | GrpcAuthInterceptor; GrpcAuthInterceptorEnforcementTest |
| GRPC-015 | 🟡 | wallet | `getHistory` ignore `PageRequest` — **CLOSED 2026-08-12**: getHistory kini honor page/size (default = semua bila PageRequest kosong, clamp di luar range → kosong); `WalletGrpcServiceGetHistoryPagingTest` 3 test. Sisa (tak dikerjakan): konsistensi status error NOT_FOUND vs INVALID_ARGUMENT | WalletGrpcService.getHistory; WalletGrpcServiceGetHistoryPagingTest |
| GRPC-008 | 🔴 | lending/statement | **Kontrak REST internal sudah drift** — **CLOSED 2026-08-12**: (a) statement client di-align ke `GET /api/v1/transactions?accountId=` + envelope `ApiResponse.data` + `createdAt`→LocalDate; (b) lending `TransactionClient`/`AccountClient` di-align ke endpoint nyata + endpoint baru `GET /api/v1/transactions/accounts/{accountId}/summary` (transaction-service, `AccountTransactionSummaryService`) + `GET /api/v1/accounts/users/{userId}` (account-service, `UserProfileController`/UserAccountController) — credit scoring kini resolve account-ids dulu lalu summary per-account; live smoke: profile 200, account-ids 200, summary 403 (realm client-scope belum `read:transaction`) | TransactionServiceClient.java:30,58; TransactionClient.java:16-22; AccountClient.java:13,16; TransactionController.java:90 |
| TX-004 | 🟠 | transaction | Scheduled transfer tanpa idempotency key — **CLOSED (CB-031, 2026-08-12)**: key deterministik `SCH-<id>-<executedCount>` per eksekusi; replay/overlap dedupe via handler `findByIdempotencyKey` | ScheduledTransferService |
| QRIS-001 | 🟠 | transaction | Idempotency cache-only fail-open (TTL 24h) — **CLOSED (CB-017, 2026-08-12)**: DB fallback `findByIdempotencyKey` sebelum debit + persist key di `transactions.idempotency_key`; replay → `QRIS_001` BusinessException, tidak double-charge | ProcessQrisPaymentCommandHandler |
| PROMO-001 | 🟠 | promotion | Cashback record duplikat saat replay — **CLOSED (verified 2026-08-12)**: saga `recordCashbackStep` menangkap `DataIntegrityViolationException` (unique index `uq_cashback_transaction_id` V11) → no-op deterministik via `findByTransactionId`; wallet credit idempotent by reference (tidak ada money move ganda); `CashbackSagaOrchestratorTest.testSaga_Replay_DuplicateTransactionRecordIsNoOp` 7/7 green | CashbackSagaOrchestrator.java:119-140 |
| PROMO-002 | 🟠 | promotion | Loyalty redeem tanpa dedup | LoyaltyPointsService.java:82-109 |
| PROMO-003 | 🟠 | promotion | `claimPromotion` tanpa dedup by transactionId — replay/double-submit → 2 reward AWARDED (maxRedemptions atomik ✓, tapi per-user/per-transaction tidak ada guard) | PromotionService.java:139-180 |
| PROMO-004 | 🟠 | promotion | `calculateRewardAmount` PERCENTAGE `divide(..., 2, HALF_EVEN)` — scale 2, melanggar ADR-0022 (scale 4 wajib) | PromotionService.java:184-191 |
| DISPUTE-001 | 🟠 | dispute | Over-refund race (sum-then-check tanpa lock) — CLOSED via advisory lock (CB-028, 2026-08-12) | RefundService.java:153-164 |
| DISPUTE-002 | 🟠 | dispute | DisputeService persistence defects — CLOSED 2026-08-12: (a) `save` update managed entity in-place (bukan persist baru, @Version); (b) `dispute_evidence` bidirectional @ManyToOne (FK di INSERT, bukan UPDATE terpisah); (c) resolve auto-refund butuh mock lookup di integration test. DisputeControllerIntegrationTest 6/6 | DisputePersistenceAdapter.save |
| REFERRAL-001 | 🟠 | promotion | completeReferral tanpa lock | ReferralService.java:79-107 |
| TEST-GAP | 🟠 | qa | 6/8 core banking tanpa integration test; wallet 31 @Test | src/test structure |
| IMP-3 | 🟠 | statement | Flow improvement target: closing balance derive → ledger `balance_after` — **CB-036 CLOSED 2026-08-12**: opening/closing = ledger snapshot (`getBalanceAsOf` via gRPC `GetHistory` balance_after, fallback derive) | FLOWS.md IMP-3 |
| IMP-4 | 🟠 | notification | Flow improvement target: retry + fallback channel — CB-037 CLOSED 2026-08-12 (fallback chain PUSH→EMAIL→SMS + backoff retry; sama-recipient cross-channel = ponytail ceiling) | FLOWS.md IMP-4 |
| IMP-6 | 🟠 | transaction | Flow improvement target: QRIS idempotency DB — CB-017 | FLOWS.md IMP-6 |
| INTEGRATION-CTX | 🟠 | qa | Account-service integration test context: **VaultConfigurationTest FIXED** (2026-08-12: mock DataSource di TestJpaConfig) → default suite 132/132. Sisa: OnboardingIntegrationTest + BlindIndexAndTenantIsolationIntegrationTest masih `No bean named 'entityManagerFactory'` — test tanpa `@ActiveProfiles("test")` (activeProfiles=[]), dan app pakai multi-DS custom (`spring.datasource.primary.*`, bukan `spring.datasource.*`) sehingga dynamic property + `@ServiceConnection` tidak di-honor; workaround sementara: verifikasi DB langsung (podman postgres) | surefire context load errors |
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
