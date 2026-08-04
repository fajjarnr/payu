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
| **Cluster Status** | 🟢 OCP 4.20.29, 8 nodes Ready (5 workers across 3 AZs). Snapshot 2026-08-04: `payu-dev` has 47 Running/Ready pods and 33 deployments; quota `limits.cpu` is `30/64` and `requests.cpu` is `4/16`; no HPA is installed in `payu-dev`. VSO 2/2 Running; vector→Loki delivery remains blocked by gateway RBAC (operator 6.5.1 empty rego). |
| **Last Release** | `1.10.30` (2026-08-04) — mobile storage runtime safety |
| **Last Updated** | 2026-08-04 (PROD-037 fixed; PROD-002 still awaits approved FX provider evidence) |

---

## 🐛 Active Tickets

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| INFRA-029 | P1 | Enable audit log forwarding: install cluster-logging + ClusterLogForwarder dengan `inputRefs: [audit]` ke SIEM (Wazuh INFRA-011) — satu-satunya kontrol CIS tersisa (`ocp4-cis-audit-log-forwarding-enabled`). **2026-08-01**: Logging 6.5 + LokiStack (S3/KMS) + CLF `instance` audit→lokiStack, CLF Authorized/Valid/Ready=True, collector 9/9; CIS kontrol terpenuhi. Sisa: Wazuh SIEM (INFRA-011) sebagai sink tambahan + verifikasi log arrival. | 🟢 Live (CIS satisfied) — Wazuh sink + log delivery pending |
| MVP-001 | P1 | `SnapBiPaymentService.createPayment` kini settle melalui port wallet (reserve → commit → credit dengan kompensasi), menandai record `COMPLETED`, dan menerbitkan webhook + outbox `payment.completed`. | ✅ Closed 2026-08-04 — live SNAP payment `2002500`, wallet `ACC-001 → ACC-002` IDR 100, replay identik; partner `1.8.98`, wallet `1.8.110`, gateway `1.9.9` |
| MVP-002 | P1 | `TransferSagaOrchestrator` (transaction-service) = **DEAD CODE**: nol pemanggil di seluruh repo (hanya self-ref + javadoc `@see` di `SagaConfig.java:17`). Saga ini (reserve→commit→compensation + BI-FAST, pub via outbox) tidak pernah dipanggil controller/service manapun — jalur transfer yang ter-wire justru `InitiateTransferCommandHandler`. Dua implementasi logika uang paralel → risiko divergensi (satu di-fix, satu tetap rusak) & duplikasi. Keputusan: **hapus** (YAGNI) atau **wire** ke endpoint. Sangat direkomendasikan wiring kompensasi saga ke `InitiateTransferCommandHandler` atau hapus demi satu source of truth. | ✅ Dead code removed (MVP-002, 2026-08-01) — saga deleted (TransferSagaOrchestrator/Context), SagaConfig javadoc updated; `mvn test` transaction-service SUCCESS (2026-08-03) |
| MVP-004 | P1 | Idempotency boundary untuk SNAP payment/refund dan disbursement callback kini wajib `X-Idempotency-Key`; natural-key/unique index V16/V17, HMAC callback, dan parent-row lock untuk cumulative refund sudah tersedia. | ✅ Closed 2026-08-04 — live disbursement callback completed; transaction `1.8.108`, BI-FAST simulator `1.8.84` |





---

## 🔍 Web App URL Audit — `payu-dev` (2026-07-31)

Hasil audit semua path URL di `https://payu-dev.apps.fajjjar.my.id` (47 cek: 41 path halaman incl. locale `en`/`id` + API + sitemap/robots):

| Path | Hasil |
|:---|:---|
| `/` | ✅ 200, konten statis tampil |
| `/login` | ✅ 200, form tampil (2 input + submit) — nonce CSP ter-inject (30 attr, dynamic render), di-fix di `web-app:1.5.3` |
| `/forgot-password` | ✅ 200, form tampil (1 input email) — publicRoutes + dynamic render, di-fix di `web-app:1.5.3` |
| `/onboarding`, `/merchant/register`, `/legal/privacy`, `/legal/terms` | ✅ 200, konten server-rendered ada; JS-interactive bagian masih bergantung hydration (terkena WEB-001) |
| 31 path terproteksi (`/dashboard`, `/transactions`, `/transfer`, `/cards`, `/bills`, `/rewards`, `/investments`, `/lending`, `/exchange`, `/pockets`, `/split-bill`, `/notifications`, `/settings`, `/support`, `/analytics`, `/security`, `/merchant`, `/qris`, `/scheduled-transfers`, semua `/backoffice/*`) | 🔒 200 → redirect ke `/login?callbackUrl=...` (expected; login form sekarang bisa dipakai) |
| `/api/health` | ✅ 200 `{"status":"healthy"}` |
| `/api/v1/cards` (no-auth) | ✅ 401 (gateway reachable) |
| `/api/auth/login` (POST) | ✅ 200 + Set-Cookie; login E2E live (browser) → dashboard |
| `/sitemap.xml`, `/robots.txt` | ✅ base URL dev `payu-dev.apps.fajjjar.my.id` (di-fix di `web-app:1.5.3`) |
| `/nope` (unknown) | ✅ 404 (di-fix di `web-app:1.5.3`) |

Bukti kunci (2026-07-31): HTML `/login` punya 32 `<script>` tanpa satu pun atribut `nonce`; header CSP `script-src 'self' 'nonce-…'`; console browser "Executing inline script violates the following Content Security Policy directive"; log web-app `Login proxy error ... SSL routines:tls_get_more_records:packet length too long`.

Dev loop (2026-07-31 → 2026-08-04): `web-app:1.5.3` deployed; unit 1187 pass; live `/login` form, `/forgot-password` form, 404, sitemap dev domain verified. ARCH-007 selesai di dev: `SPRING_MAIN_SOURCES` bridge dihapus (auto-config metadata include `HotRodCacheConfig`); label `app.kubernetes.io/managed-by: platform-team` di semua base deployment (L-146); health check Hot Rod lazy-start + cache bernama `payu` (`HotRodCacheSupport`); Data Grid dev dikelola operator dengan plain Hot Rod/no endpoint auth dan cache `payu` text/plain; production overlays retain mTLS; deployment manual + Service `payu-cache-resp` dihapus. E2E login-flow live pass lagi: password `customer1` di-reset ke `Customer1-test` di Keycloak dev (partial import tidak membawa password benar — L-149), `scripts/e2e/auth-login.sh` host SSO di-fix, `ALL 6 TESTS PASSED`, `/dashboard` 200 dengan cookie; 0 error cache selama login. E2E lain ALL PASS (16 suite): wallet-balance 8/8, transaction-history, cards-crud 14/14, api-portal 4/4, partner-integration 5/5, billing-billers 6/6, lending-investment 8/8, transaction-disbursements 9/9, account-service 5/5, promotion-catalog 7/7, fx-rates 13/13, cms-statement 7/7, verify-nik-cache T1/T2 200, notification-health 3/3 (NOTIF-001 — L-156). Broker AMQ CrashLoop diperbaiki (L-154). Sisa skenario role-scoped/endpoint-absent (bukan cache, bukan bug runtime): integration-dispute T2 (endpoint `messages` belum ada di integration-service), T4/T5 + support T4 (`@PreAuthorize` admin/backoffice — customer1 bukan role itu). Canary gate diterima 2026-08-01 (pods 23/23, errs 0, latency 1–2ms); ARCH-007 ditutup, release `1.10.0`; promosi SIT→UAT→preprod→prod = langkah deploy berikutnya.

## 🚀 Platform Deploy Queue

| Key | Priority | Category | Summary |
|:---|:---:|:---|:---|
| DEPLOY-006 | P1 | Security | Deploy Coraza WAF (INFRA-015) + remediate CIS findings (SEC-020) + Wazuh SIEM (INFRA-011) |
| DEPLOY-011 | P1 | Promotion | Make the existing SIT/UAT/preprod/prod workload overlays deploy-safe: remove dev secrets/endpoints, add environment-isolated DB/Kafka/Data Grid paths and immutable images, correct prod namespace/RBAC, raise quotas, and run gates sequentially. **2026-08-01**: overlays deploy-safe + SIT workloads deployed (40/40 pods Running) + Tekton `payu-deploy-gitops-pipeline` SIT pilot SUCCEEDED (argocd-sync-wait, ZAP baseline, Schemathesis, Litmus pod-delete Pass, k6 smoke). Sisa: UAT→preprod→prod promotion via pipeline (digest pinning + E2E gates per env). | 🟢 SIT pilot green — UAT/preprod/prod pending |
| DEPLOY-011 | P1 | Promotion | **2026-08-01 update**: UAT pipeline SUCCEEDED (gates: sync-wait, Schemathesis, k6 load, k6 smoke) — UAT 31/31 live. Preprod workloads live + app Synced/Healthy; preprod pipeline run: sync-wait Succeeded, `preprod-kraken-gate` blocked (OPS-2026-08-01-05). Sisa: kraken gate → preprod pipeline green → prod promotion. | 🟢 SIT+UAT green — preprod gate pending, prod pending |
| OPS-2026-08-01-05 | P2 | Chaos | Kraken/preprod gate runtime: cerberus FIX (kubeconfig_path — L-185, "client set"; SCC anyuid + FS writable). krkn rootfs `ro` oleh CRI-O → FIX manifest: emptyDir di `/home/krkn/kraken` + `/tmp` utk fixperms+kraken container (OPS-2026-08-01-05, L-188 pattern). Sisa: re-run `preprod-kraken-gate` saat kapasitas CPU pulih (HPA max 5→3 dikomit). | 🔄 Manifest fixed — gate re-run pending |
| OPS-2026-08-01-04 | P2 | Observability | Log delivery ke Loki: DNS fixed (egress allow-all, L-188) + TLS CA fixed (`tls.ca: loki-gateway-ca-bundle` di CLF) — vector sekarang connect & handshake OK. Sisa: gateway 403 Forbidden karena loki-operator 6.5.1 render `loki-gateway` ConfigMap dgn `lokistack-gateway.rego` + `rbac.yaml` KOSONG (0 bytes) utk `tenants.mode: openshift-logging` (reproduksi: delete cm + recreate LokiStack → sama). SAR logcollector→collect audit logs = allowed; SA gateway punya tokenreview+SAR. Dugaan bug operator (keluarga LOG-2236) → butuh RH support/upgrade 6.5.x; workaround bila perlu: tenant static/dynamic. | 🔄 Delivery blocked (operator bug) |
| INFRA-026 | P1 | Secrets | **2026-08-01**: DR restore drill: `vault operator raft snapshot restore -force` SUCCEEDED — prod state loaded (recovery shares 5/3, raft peers vault-0/1/2, auth roles payu-sit/uat/preprod/prod/vault-admin/vault-snapshot), KMS auto-unseal OK. Sisa: verifikasi `kv get` via k8s auth gagal 403 (snapshot 00:54 pre-date current token-reviewer context; L-192 auth-delegator added) → refresh snapshot pasca-HA-migration + re-verify via generate-root/recovery. | 🟢 Restore verified — kv readback pending |
| DEPLOY-009 | P2 | CI/CD | **2026-08-01**: Tekton Results live + dimigrasi ke HA PostgreSQL (CNPG `tekton_results`, 17 records, API OK) — INFRA-014 selesai. Sisa: Chains SLSA provenance verification + retention evidence 365d, Renovate (DEVSECOPS-011). | 🔄 Sebagian selesai |
| OPS-2026-04-08-02 | P2 | Ops | 🔄 Verified k6 script structure OK. Gateway unreachable from local (sock/dns). Must run via k6 Operator in OCP or port-forward gateway. See `tests/performance/k6/RUNBOOK.md` | 🔄 Operator-only |
| READY-029 | P2 | Performance | Gatling: defer to cluster integration test phase (needs port-forward or in-cluster runner) | 🔄 Operator-only |
| READY-030 | P2 | Performance | SOAK 24h: defer to staging environment | 🔄 Staging-only |
| READY-022 | P2 | Test | 80% coverage: audited 4-22% across 4 services. Sprint planning needed | 🔄 Planned |
| READY-060 | P3 | Card | Card tokenization + 3DS |
| READY-061 | P3 | Mobile | Expo SDK 55 + RN 0.85 upgrade |
| READY-062 | P3 | ML | ONNX fraud detection model |
| DEVSECOPS-015 | P3 | DevSecOps | Security Findings Dashboard Grafana |
| DEVSECOPS-016 | P3 | DevSecOps | Service template scaffolder |
| INFRA-018 | P3 | Registry | Setup registry GC policy |
| INFRA-019 | P3 | Registry | Configure Quay.io auto-prune policy |
| DEVSECOPS-005 | P3 | Network | EgressNetworkPolicy + Istio egress gateway |
| DEVSECOPS-007 | P3 | Security | LUKS encryption PV + Vault DEK rotation |
| DEVSECOPS-012 | P3 | Cost | Monthly cost report workflow |

## 🛡️ DEVSECOPS-017 — Production-Ready Architecture Implementation

Success criteria: every mandatory control in `infrastructure/DEVSECOPS_ARCHITECTURE.md`
has repository tests plus live-cluster evidence. A manifest existing in Git is not
completion evidence.

- [x] Add failing infrastructure contract tests for Kustomize rendering, secret hygiene, fail-closed Java/Python/Next.js gates, digest-pinned Task images, immutable image promotion, and policy ownership. (`34/34` green on 2026-07-22.)
- [x] Repair `payu-dev` rendering and workload port contracts before enabling GitOps reconciliation. (Web route verified HTTP 200.)
- [ ] Remove tracked credentials/private keys; replace runtime delivery with Vault and External Secrets. The Argo CD image-updater key is removed from the current tree, but its deploy key must be revoked/rotated and Git-history purge requires an approved coordinated MOP.
- [ ] Bootstrap a real `payu-vault` ClusterSecretStore backed by production Vault/KMS, then provision the Argo CD repository credential through External Secrets. Back up/rotate the operator-generated Chains key or migrate signing to approved KMS; do not create placeholder Secrets.
- [x] Bootstrap Argo CD Applications/ApplicationSets with Git/live parity before enabling prune and self-heal. Paritas tercapai: 22 Applications, 3 AppSet tersisa semua manual sync; file repo disinkronkan (AppSet automated dihapus dari file). Prune/self-heal tetap off sampai promotion gate SIT/UAT/preprod/prod selesai.
- [ ] Tekton Tasks/Pipelines are live and fail-closed. Scoped 10-minute RHACS CI identity, OCI signature/attestation, and internal Rekor transparency are verified; signed-image admission sudah Enforce (`require-cosign-signature`, 31 image `payu-dev` di-sign); sisa: SBOM attestation retention dan provider opt-in Pact gate.
- [ ] Promote the Buildah-produced digest through all environments; retain signed SLSA provenance and pipeline results for 365 days.
- [x] Enforce security controls in ACS and operational controls in Kyverno without overlapping ownership. Semua policy Kyverno sekarang `Enforce`; pelanggaran `payu-dev` diremediasi (0 policy FAIL per PolicyReport 2026-07-31), negative tests lulus (root, registry, labels, cosign).
- [ ] Complete the remaining durable platform stores: production Vault/KMS bootstrap, LokiStack on the dedicated KMS/S3 bucket, and Tekton Results on HA PostgreSQL. ESO is cluster-wide Ready; placeholder Vault and community non-FIPS Loki remain excluded.
- [x] Measure scheduler pressure and MachineSet topology; add workers only for a verified constraint. Required zone anti-affinity exposed the single-AZ worker layout, so workers were added in `1b/1c`; five workers are currently Ready across three AZs.
- [ ] After workload redistribution and a disruption-budget review, rightsize the original `1a` MachineSet from three replicas to one so steady state is one worker per AZ.
- [ ] Run positive and negative E2E security gates, DR/rollback exercises, reviewer audit, then reconcile architecture and PCI evidence documents with runtime truth.

---

## 🔍 Ponytail Audit — Over-Engineering & Dead Code (2026-07-02)

| # | Key | Category | Summary |
|:---:|:---|:---|:---|

## 📝 Platform Workload Audit Details
### ✅ MVP-001: SNAP-BI `createPayment` money flow — live E2E selesai (2026-08-04)
* **Implemented (2026-08-03)**: `SnapBiPaymentService` sekarang menggunakan `WalletSettlementPort`; adapter wallet memanggil reserve → commit → credit dengan `X-Idempotency-Key` per langkah dan mencoba credit kompensasi jika beneficiary credit gagal.
* **Event contract**: payment yang berhasil menjadi `COMPLETED`, webhook memakai `payment.completed` + event ID stabil, dan outbox memakai topic `payu.partner.payment-completed.v1`. Refund/status terminal juga tidak lagi memakai `LOG.info` stub.
* **Verification**: `SnapBiPaymentServiceTest` 8/8 dan seluruh `partner-service` 237/237 test lulus pada 2026-08-03.
* **Verification**: SNAP token 200, payment IDR 100 dari `ACC-001` ke `ACC-002` mengembalikan `2002500`; replay dengan body dan idempotency key yang sama menghasilkan response identik. Partner `1.8.98`, wallet `1.8.110`, dan gateway `1.9.9` berhasil rollout.

### 🔴 MVP-002: `TransferSagaOrchestrator` dead code — dua implementasi transfer paralel
* **Trace (2026-08-01)**: `grep -rn 'TransferSagaOrchestrator'` di seluruh repo hanya menemukan: (a) definisi kelas `application/saga/TransferSagaOrchestrator.java`, (b) self constructor, (c) javadoc `@see` di `config/SagaConfig.java:17`. **Tidak ada controller/service yang memanggil** saga ini → unreachable.
* **Konflik**: jalur `/api/v1/transactions/transfer` yang ter-wire memakai `InitiateTransferCommandHandler` (reserve→commit→credit + outbox + compensation), bukan saga. Jadi ada 2 implementasi logika transfer yang identik tapi terpisah.
* **Risiko**: divergensi uang — perbaikan di satu path tidak menjangkau path lain; dua source of truth untuk keputusan money.
* **Rekomendasi**: hapus `TransferSagaOrchestrator` (YAGNI) atau wire kompensasinya ke `InitiateTransferCommandHandler`. Satu alur uang, bukan dua.

### ✅ MVP-004: Idempotency boundary — payment/refund/disbursement callback live (2026-08-04)
* **Scan idempotency (2026-08-01)**: semua endpoint money pindah-dana sudah `@Idempotent(required=true)`: transfer, qris/pay, billing/topup/subscription, disbursement/batch-create, VA-create, split-bill payment/settle, wallet reserve/commit/release, merchant qr-pay, payment-link, public confirm. ✅
* **Implemented (2026-08-03)**: `SnapBiController` `POST /payments` dan `POST /payments/{id}/refund`, serta `DisbursementController` `POST /api/v1/disbursements/callback`, sekarang memakai `@Idempotent(required=true)`. Disbursement callback juga sudah dilindungi HMAC oleh `CallbackSignatureFilter`; V15/V16 menjaga natural-key/unique delivery.
* **Implemented (2026-08-03)**: `createRefund` sekarang mengambil parent payment dengan `PESSIMISTIC_WRITE` sebelum menghitung cumulative refund, sehingga refund berbeda pada payment yang sama terserialisasi dalam satu transaksi.
* **Fixed (2026-08-04)**: SNAP refund sekarang menyimpan `PENDING`, menjalankan wallet reversal atomik melalui trusted service endpoint, lalu baru menjadi `COMPLETED`; `refundId` deterministik menjaga retry aman pada ledger reversal. Kafka topic + DLQ `payu.partner.payment-refunded.v1` juga dideklarasikan.
* **Fixed (2026-08-03)**: partner-service CrashLoop karena dua migration `V15` diselesaikan dengan mempertahankan migration schema `V15` dan memindahkan unique-index/idempotency migration ke `V17`; regression test memastikan semua versi Flyway unik. Live `1.8.94` tervalidasi Flyway schema version 17, pod 1/1, health 200.
* **Fixed (2026-08-04)**: activated the existing `ExternalSecretsConfig/cluster`, synced CNPG `payu-database-app` into `payu-sso/payu-keycloak-db` through a declarative Kubernetes `SecretStore`, corrected the dev DB FQDN, and verified Keycloak Ready `1/1`; source/sync password hashes match without exposing the value.
* **Fixed (2026-08-04)**: dev client credentials are generated by the installed ESO `Password` generator with `ExternalSecret.refreshPolicy: OnChange`, synced from `payu-dev` to `payu-sso` through the existing least-privilege Kubernetes `SecretStore`, and loaded by `auth-service` through a declarative pod-template revision. The RHBK realm import Job completed successfully; source/sync key hashes match without exposing credential material.
* **Verification (2026-08-04)**: authenticated disbursement `c313201f-94a1-4560-8319-04560c097e46` using bank code `014` returned `201`, then reached `COMPLETED` for `1 IDR`; BI-FAST delivered the signed callback, the HMAC filter verified it, and gRPC committed the persisted wallet reservation ID. Transaction reactor `142` tests, BI-FAST simulator reactor `2` tests, and numeric bank-code regression `1/1` passed; deployed images are transaction `1.8.108` and BI-FAST simulator `1.8.84`, both Ready `1/1` with restart `0`. Simulator startup has no deprecated-property or OTLP-export warning.

### ⚙️ INFRA-025 / ARCH-007: Infinispan Hot Rod Migration
* **Original**: Netty SSL ApplicationProtocolNegotiationHandler warnings & `ISPN005061` RESP unclosed iterator warnings.
* **Status**: 🟢 DEV STABLE / PROMOTION OPEN. Local mTLS is verified for `cache-starter` and Quarkus gateway Hot Rod plus KYC/analytics REST. `payu-dev` now deliberately uses plain Hot Rod without endpoint authentication; production overlays retain mTLS. `cache-starter` uses Infinispan 16.2.1 native Hot Rod with a lazy `RemoteCacheManager`, 10,000-entry invalidated near cache, and explicit UTF-8 JSON-text values in the `payu` cache. Auth refresh tokens, partner SNAP-BI tokens, API-commons atomic paths, and Quarkus gateway paths use Hot Rod. KYC and analytics idempotency use authenticated Data Grid REST because the Python Hot Rod client is unmaintained.
* **Remaining**: Production promotion (SIT/UAT/preprod/prod) — canary gate diterima 2026-08-01 (pods 23/23 Running/Ready, errs 0, latency 1–2ms; detail `ARCH007_CANARY.md`). TLS/mTLS secrets untuk env promosi sudah operator-managed di Vault (SIT `WellFormed=True`).
* **Updated**: 2026-08-04; CRD fields validated with `oc explain` before apply.

### 🏗️ ARCH-008/009/010: ArchUnit 1.4.2 violations (billing, statement, promotion)
* **Context**: ArchUnit 1.2.1 → 1.4.2 upgrade in parent POM exposed pre-existing architecture violations that ArchUnit 1.2.1 silently skipped (ASM < 9.5 cannot parse Java 25 bytecode — empty `importPackages()`).
* **ARCH-008 (billing)**: 85 domain@adapter violations — `SubscriptionEvent.createChargeFailedEvent()` calls `SubscriptionChargeEntity` getters directly. Domain layer depends on adapter persistence entities.
* **ARCH-009 (statement)**: 12 immutability violations — `RecipientInfo`/`SenderInfo` Lombok `@Builder` + `@NoArgsConstructor` generates non-final fields. `ReceiptException` lives in `application.service.exception`, not `domain.model`.
* **ARCH-010 (promotion)**: 288 dependency violations + 3 cyclic deps (adapter→application→adapter) + `CashbackEntity` naming (entity in persistence package not following naming convention).
* **Status**: Pinned to ArchUnit 1.2.1 in these 3 services until remediation. Parent POM keeps `<archunit.version>1.4.2</archunit.version>` for services that already pass (compliance, partner, gateway, etc.).
* **Created**: 2026-07-13.

### 🔐 SEC-020: Remediate CIS/PCI platform failures (platform-security)
* **Problem**: Scan live `ocp4-cis-1-9` + `ocp4-pci-dss-4-0` pada 2026-07-22 berstatus `NON-COMPLIANT`. Terdapat 25 FAIL (16 kontrol unik); sembilan kontrol bersama adalah:
  1. `ocp4-cis-api-server-encryption-provider-cipher`: Cipher enkripsi API server tidak aman.
  2. `ocp4-cis-audit-log-forwarding-enabled`: Audit log forwarding ke SIEM eksternal belum diaktifkan.
  3. `ocp4-cis-audit-profile-set`: Profil audit API server belum dikonfigurasi.
  4. `ocp4-cis-configure-network-policies-namespaces`: Terdapat namespace tanpa NetworkPolicy default-deny.
  5. `ocp4-cis-ingress-controller-tls-cipher-suites`: TLS cipher suites pada default Ingress Controller belum dikeraskan.
  6. `ocp4-cis-kubeadmin-removed`: Akun bootstrap `kubeadmin` belum dihapus/dinonaktifkan dari cluster.
  7. `ocp4-cis-ocp-allowed-registries`: Daftar registry eksternal yang diizinkan belum didefinisikan.
  8. `ocp4-cis-ocp-allowed-registries-for-import`: Aturan import image registry belum dibatasi.
  9. `ocp4-cis-scc-limit-container-allowed-capabilities`: Security Context Constraints (SCC) belum membatasi capabilities container secara ketat.
* **PCI-only gaps**: Container Security Operator, File Integrity Operator + notification, OAuth inactivity timeout, non-HTPasswd IDP, TLS on every Route, dan Security Profiles Operator.
* **Impact**: Platform OpenShift rentan terhadap celah keamanan CIS Benchmark dan tidak memenuhi kepatuhan regulasi OJK/PCI-DSS.
* **Fix**: Susun MOP per kontrol dengan backup, diff, canary, dan rollback. Jangan aktifkan `autoApplyRemediations`; perubahan APIServer, IngressController, OAuth, Image, SCC, dan audit forwarding memerlukan review dampak cluster.
* **Status (2026-07-31)**: ✅ 8/9 remediated. Scan ulang `payu-cis` (TailoredProfile) = **1 FAIL tersisa**:
  1. `api-server-encryption-provider-cipher` → PASS — `APIServer.spec.encryption.type: aesgcm` (ComplianceRemediation `Applied`).
  2. `audit-profile-set` → PASS — `audit.profile: WriteRequestBodies` (Applied).
  3. `ingress-controller-tls-cipher-suites` → PASS — custom TLS profile, min TLS 1.2, cipher kuat (Applied).
  4. `configure-network-policies-namespaces` → PASS — `default-deny-ingress` di `payu-cicd`; namespace operator di-exempt via `ocp4-var-network-policies-namespaces-exempt-regex` (TailoredProfile `payu-cis`).
  5. `kubeadmin-removed` → PASS — secret `kubeadmin` di `kube-system` dihapus.
  6. `ocp-allowed-registries` / `allowed-registries-for-import` → PASS — `image.config.openshift.io/cluster` dibatasi ke internal registry + 8 registry publik yang dipakai workloads.
  7. `scc-limit-container-allowed-capabilities` → PASS — SCC ODF/pipelines di-exempt via variable regex TailoredProfile; SCC default tidak diubah.
  8. `audit-log-forwarding-enabled` → ❌ FAIL — belum ada `ClusterLogForwarder`/`openshift-logging`; butuh SIEM sink → INFRA-029.
* **Catatan**: `ocp4-cis-node-master` COMPLIANT; `ocp4-cis-node-worker` ERROR (pre-existing, investigasi terpisah). Compliance Operator tetap `autoApplyRemediations: false`.

### 🧭 ARCH-007: Migrate Data Grid access from RESP compatibility mode to Hot Rod native client
* **Context7 evidence**:
  1. Infinispan/Data Grid menyediakan RESP endpoint agar RESP-compatible clients bisa terhubung tanpa perubahan besar.
  2. Hot Rod adalah client native Data Grid untuk remote access, dengan API sync/async/Mutiny dan opsi TTL/lifespan pada write operations.
  3. ProtoStream menyediakan schema `.proto`, adapter untuk tipe pihak ketiga, serializer/deserializer compile-time, dan compatibility check untuk perubahan schema.
* **Status (2026-08-01)**: Java/Quarkus tidak lagi membawa Redis/Lettuce/Quarkus Redis client; cache bernama `payu` tersedia pada Data Grid dengan media type `text/plain`. Focused build, Python REST round-trip, REST-write → Hot Rod-read, dan mTLS positive/negative gate lulus. `SPRING_MAIN_SOURCES` bridge diganti metadata auto-config durable (`AutoConfiguration.imports` + `HotRodCacheConfig`); semua deployment backend diberi label Kyverno exclusion (L-146); health check lazy-start cache bernama (`HotRodCacheSupport`); Data Grid dev operator-managed + mTLS lengkap (server TLS, client CA, keystore, identities literal-password — L-147/148); deployment manual + `payu-cache-resp` dihapus; rollout dev aggregate health UP, 0 error cache; canary gate diterima 2026-08-01; **ARCH-007 DONE** (release `1.10.0`).
* **Goal**: Migrasi cache/session/rate-limit/idempotency/lock/analytics dari RESP ke Hot Rod tanpa kehilangan key aktif dan tanpa regresi latency.
* **Decision**: RESP dihapus dari runtime backend. Hot Rod 16.2.1 adalah satu-satunya client cache Java/Quarkus yang didukung; Python memakai Data Grid REST terautentikasi.
* **Plan**:
  1. **Completed**: migrate cache, auth refresh token, partner SNAP-BI token, API-common idempotency/rate limit/webhook, and gateway API keys, analytics, authorization, idempotency, and rate limiting to Hot Rod.
  2. **Completed**: remove Java RESP dependencies and direct Redis APIs; use native TTL and versioned CAS for counters/sliding windows.
  3. **Completed**: migrate every `payu.cache.redis` profile block to `payu.cache.hotrod`; local profiles default to `localhost:11222`, while container/non-local profiles require `PAYU_CACHE_HOTROD_SERVER_LIST`.
  4. **Completed**: shared `payu` cache uses UTF-8 JSON-text values so Data Grid REST and Hot Rod address identical keys; Java uses `UTF8StringMarshaller`.
  5. **Completed**: Python KYC/analytics idempotency uses the authenticated Data Grid REST API; the unmaintained Python Hot Rod client is not introduced.
  6. **In progress**: 24-hour `payu-dev` canary started 2026-07-31 20:47Z on the final operator-managed mTLS stack (p95/error/duplicate-replay evidence required); promotion to SIT/UAT/preprod/prod proceeds after the canary evidence gates pass.
* **Done criteria**:
  1. Tidak ada business code yang inject `RedisTemplate`/`StringRedisTemplate` langsung.
  2. Java/Quarkus memakai Hot Rod dan Python memakai Data Grid REST, semuanya memakai secret refs tanpa inline credential.
  3. Idempotency/rate-limit/lock tests hijau di Hot Rod profile.
  4. `payu-dev` canary 24 jam tanpa `RedisConnectionException`, duplicate payment replay, scheduler lock overlap, atau p95 cache latency regression >10%.

## 🔎 Deep Production-Readiness Audit — Backend & Web App (2026-08-03)

Audit berbasis source, CodeGraph, focused build/test, dan verifikasi dokumentasi library via Context7. Item di bawah ini adalah blocker/gap yang perlu ditutup; bukan klaim bahwa implementasinya sudah production-ready.

| ID | Pri | Area | Bukti | Minimum done |
|---|---|---|---|---|
| PROD-002 | P0 | FX | Stub hanya aktif pada profile `local`; profile non-local punya HTTP provider configurable dan fail-closed bila provider belum dikonfigurasi. `FX_PROVIDER_URL` kini dibind ke `fx.provider.url`, blank URL tetap memilih unavailable adapter, dan deployment mengekspos URL/source ConfigMap serta API-key Secret reference. Provider response wajib pair/base, rate positif, source, dan timestamp fresh; `source`/`observed_at` diaudit di `fx_rates` (Flyway V6). Approved provider URL/credential dan live provider evidence masih open. | Konfigurasikan approved provider melalui `service-endpoints`/`fx-provider-credentials`, lalu buktikan rate live, freshness, source, dan pair audit di cluster. |
| PROD-018 | P2 | Analytics CI | First GitHub run `30836757966` failed at the coverage step: CI lacked `SECRET_KEY` and test dependency `Faker`; local reproduction also found API tests using unresolved `Depends`, real DB/Kafka/OTLP startup, stale response-envelope assertions, and WebSocket importing unavailable `PyJWT`. Workflow now supplies deterministic CI-only settings/dependencies, app tests isolate external services, response factories avoid Pydantic field collisions, and WebSocket uses the existing `python-jose` dependency. Local gate: `189 passed, 1 skipped`, coverage `84.86%`; analytics image `1.8.95` is live; post-fix GitHub run `30878225559` is green. | Activate job `analytics-tests` as a required branch-protection check (admin API verification currently returns `401`). |

### Additional findings from deeper pass

| ID | Pri | Area | Bukti | Minimum done |
|---|---|---|---|---|
| PROD-022 | P0 | Loan repayment money movement | Repayment menjadi command durable: `loan_repayment_payments` menyimpan state/idempotency, schedule memakai row-lock + unique `(loan_id, installment_number)`, wallet melakukan debit + balanced journal, dan hasil dipublish via outbox CloudEvent. Live authenticated replay pada fixture schedule `479741ae-d96a-4c7f-907a-5b8e0c9fd675` mengembalikan 200 pada dua request dengan key sama; ledger debit/credit seimbang `341141.4100`, payment `COMPLETED`, schedule `FULLY_PAID`, outbox published dengan retry `0`. Images lending `1.8.113` dan wallet `1.8.109` live. | ✅ Closed 2026-08-04 — tests, build, declarative apply, rollout, ledger, replay, and outbox evidence complete. |

### Follow-up audit — Graphify + CodeGraph

| ID | Pri | Area | Bukti | Minimum done |
|---|---|---|---|---|
| PROD-034 | P1 | Mobile request deduplication | `frontend/mobile/services/api.ts:111-124,269-274` membentuk request key hanya dari method/url/params, lalu abort request pending dengan key sama. Dua POST financial dengan body berbeda ke endpoint yang sama (`transfer`, `topup`, `qris`) saling membatalkan. | Dedupe hanya exact request: sertakan body + idempotency key atau hapus dedupe untuk mutation; tambahkan concurrent mutation test. |
| PROD-035 | P1 | Mobile idempotency durability | `frontend/mobile/utils/idempotency.ts:124-152` menulis hingga 100 record metadata ke satu SecureStore value dan menelan error write. Expo SecureStore membatasi value sekitar 2048 byte; recovery key dapat hilang jauh sebelum 100 record, sementara request tetap diteruskan. | Simpan record per key atau gunakan storage yang sesuai untuk metadata; write failure harus mengubah flow menjadi queued/failed, bukan diam-diam lanjut. |
| PROD-036 | P0 | Offline false success | `frontend/mobile/hooks/useOfflineMode.ts` benar-benar memanggil API untuk transfer/topup/qris, tetapi `payment` dan `bill_payment` sebelumnya mengisi `{}` sebagai `Transaction`; queue lalu menghapus idempotency key dan melaporkan sukses tanpa post ke backend. | ✅ Closed 2026-08-04 — unsupported types removed; legacy items now remain retry/failed and retain their idempotency key; focused hook test passes. |
| PROD-038 | P1 | Mobile money precision | `frontend/mobile/types/index.ts:55-95` memodelkan transaction/transfer/top-up/QRIS amount sebagai JavaScript `number`, sehingga arithmetic dan round-trip nominal tidak exact. | Gunakan decimal string atau minor-unit integer di boundary; formatting/arithmetic exact dan precision test wajib. |

### Verification evidence

- Pass: `mvn -f backend/pom.xml -pl fx-service -am test`, `gateway-service -am test`, dan `transaction-service -am test`.
- Pass: `mvn -f backend/pom.xml -pl dispute-service,lending-service,investment-service,billing-service,promotion-service -am test` — reactor `BUILD SUCCESS`; target services reported 88, 81, 48, 107, dan 242 tests tanpa failure.
- Pass: web `npm run lint` dan `npm run build`.
- Pass: web `npm test -- --run` — 90 test files, 1203 passed, 1 skipped; `npm run lint` (changed files), `npm run type-check`, `npm run build`, dan `npm audit --omit=dev` (0 vulnerabilities) pass. Analytics pytest dan full lint legacy warning tetap open findings terpisah.
- Pass: lending PayLater boundary red-first regression, focused controller/validation suite `9/9`, full lending reactor `93/93`, package `BUILD SUCCESS`; image `1.8.107` pod Ready `1/1`, restart `0`, liveness/readiness `UP`. No authenticated financial mutation was run without an isolated fixture.
- Pass: mobile clean install `npm ci --ignore-scripts` added `1667` packages with exit `0`; focused Jest `1/1`, changed-file ESLint `0 errors/0 warnings`, Expo web export, dan Expo Android export semuanya exit `0`. Metro NativeWind wrapper dan package exports diaktifkan agar CSS Tailwind serta Axios memilih entrypoint platform yang benar.
- Known baseline: full mobile Jest dan `tsc --noEmit` masih gagal pada TurboModule `SettingsManager`, JSX di file test `.ts`, dan fixture expiry idempotency; finding ini tidak berasal dari PROD-039/037 dan tetap perlu audit berikutnya.
- Graph evidence: fast-path `graphify query` menghubungkan mobile mutation/idempotency dengan backend payment/ledger/outbox; `codegraph explore` menelusuri controller → service → port/adapter pada wallet, billing, dan mobile API.
- Context7 checks: Expo SecureStore value limit/error behavior dan web availability; Axios retry reuses original request config. Rujukan: [Expo SecureStore](https://github.com/expo/expo/blob/main/docs/public/llms-sdk-v51.0.0.txt), [Axios retry](https://github.com/axios/axios/blob/v1.x/docs/pages/advanced/retry.md).
- Context7 checks: Spring Boot actuator liveness/readiness; Next.js session validation in Proxy, environment variables, CSP, dan error boundaries. Rujukan resmi: [Spring Boot Actuator](https://github.com/spring-projects/spring-boot/blob/main/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/actuator/endpoints.adoc), [Next.js authentication](https://github.com/vercel/next.js/blob/canary/docs/01-app/02-guides/authentication.mdx), [Next.js Proxy](https://github.com/vercel/next.js/blob/canary/docs/01-app/03-api-reference/03-file-conventions/proxy.mdx), [Next.js CSP](https://github.com/vercel/next.js/blob/canary/docs/01-app/02-guides/content-security-policy.mdx).
