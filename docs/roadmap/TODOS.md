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
| **Cluster Status** | 🟢 OCP 4.20.29, 8 nodes Ready (5 workers across 3 AZs). `payu-dev` has 46/46 pods Running and 33/33 deployments Ready. |
| **Last Release** | `1.9.8` — Hot Rod cache canary support, observability and Vault platform manifests, and contract-test setup |
| **Last Updated** | 2026-07-31 (dev loop ARCH-007 selesai di dev: Data Grid operator-managed mTLS, login E2E 6/6 hijau, canary 24h berjalan sejak 20:47Z) |

---

## 🐛 Active Tickets

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| INFRA-029 | P1 | Enable audit log forwarding: install cluster-logging + ClusterLogForwarder dengan `inputRefs: [audit]` ke SIEM (Wazuh INFRA-011) — satu-satunya kontrol CIS tersisa (`ocp4-cis-audit-log-forwarding-enabled`). Percobaan Logging 6.6 (2026-07-31) dihentikan: API 6.6 berubah + Kyverno NP block (L-143/144). | 🔒 Blocked — butuh keputusan log sink |
| INFRA-025 | P2 | [cache] RESP cursor leak remediation: shared cache invalidation no longer exposes a RESP cursor; full RESP removal still depends on ARCH-007. | 🔄 In progress |
| ARCH-007 | P2 | [cache] Java/Quarkus use native Hot Rod; Python KYC/analytics use authenticated Data Grid REST. `payu-dev` Data Grid is operator-managed, `WellFormed=True`, mTLS wired (server TLS + client CA + client keystore, identities literal-password contract), workloads connect via `payu-cache:11222` with SSL. Dev `SPRING_MAIN_SOURCES` bridge replaced by durable starter auto-configuration metadata (2026-07-31); 24-hour `payu-dev` canary running since 20:47Z — checkpoint 0 error, Hot Rod round-trip 2ms (p95 evidence accumulating via `/actuator/health` detail sampler) — evidence gate still open before production promotion. | 🔄 In progress |
| FX-001 | P2 | [fx] Fixed 2026-07-31: preset `id` di controller membuat entity detached (version null) → `DataIntegrityViolation`; `account_id` JWT fallback `sub`; `conversion_date` tak diset; `/estimate` sempat memanggil `createConversion` (gerakkan uang!) → kini `estimateConversion` tanpa persist/wallet. Test `FxConversionServiceTest` 3 kasus hijau; live T1-T6 200. | ✅ Done |
| FX-002 | P2 | [fx] Fixed 2026-07-31: spring-grpc server tak pernah start → `grpc-starter` kini punya bean `grpcServer` (start builder, destroy shutdown); `@GrpcService` di `WalletGrpcService`; Service + container port 9090; `WALLET_GRPC_ADDRESS=static://wallet-service:9090`; topic outbox wallet dipatuhkan ke `payu.wallet.*.v1` (kontrak #4); reverse + bad-pair 404 (fallback rethrow + handler `FX_404`). `fx-rates.sh` ALL 13 PASSED. | ✅ Done |
| NOTIF-001 | P3 | [notification] `GET /api/v1/notifications` 401 "Not Authenticated": notification-service tidak punya `spring.security.oauth2.resourceserver.jwt` config + SecurityFilterChain (gap pre-existing, ditemukan E2E 2026-07-31). Broker AMQ fix: policy readonly-root-fs exclusion `application: payu-broker-app` + selector `payu-broker-hdls-svc` di-patch ke label operator (drift operator 7.14; L-154) — `/q/health` notification kini 200. | 🔄 Planned |


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

Dev loop (2026-07-31): `web-app:1.5.3` deployed; unit 1187 pass; live `/login` form, `/forgot-password` form, 404, sitemap dev domain verified. ARCH-007 selesai di dev: `SPRING_MAIN_SOURCES` bridge dihapus (auto-config metadata include `HotRodCacheConfig`); label `app.kubernetes.io/managed-by: platform-team` di semua base deployment (L-146); health check Hot Rod lazy-start + cache bernama `payu` (`HotRodCacheSupport`); Data Grid dev dimigrasi dari deployment manual `infinispan/server:15.0` ke operator-managed Infinispan CR (`WellFormed=True`, mTLS: server TLS, client CA, client keystore; credentials `identities.yaml` literal-password — L-148); cache `payu` text/plain dibuat operator (custom config); deployment manual + Service `payu-cache-resp` dihapus; semua workload pakai `payu-cache:11222` SSL, aggregate health `account-service` UP, 0 error cache. E2E login-flow live pass lagi: password `customer1` di-reset ke `Customer1-test` di Keycloak dev (partial import tidak membawa password benar — L-149), `scripts/e2e/auth-login.sh` host SSO di-fix, `ALL 6 TESTS PASSED`, `/dashboard` 200 dengan cookie; 0 error cache selama login. E2E lain ALL PASS (15 suite): wallet-balance 8/8, transaction-history, cards-crud 14/14, api-portal 4/4, partner-integration 5/5, billing-billers 6/6, lending-investment 8/8, transaction-disbursements 9/9, account-service 5/5, promotion-catalog 7/7, fx-rates 13/13, cms-statement 7/7, verify-nik-cache T1/T2 200 (round-trip Dukcapil + scope `account:verify` + simulator route/URL — L-155). Broker AMQ CrashLoop diperbaiki (L-154); notification `/q/health` 200. Sisa minor data/config drift (bukan cache): integration-dispute T2/T4/T5, support-compliance T4 (no tickets), notification T2 (NOTIF-001 JWT config). Canary 24 jam final berjalan sejak 20:47Z.

## 🚀 Platform Deploy Queue

| Key | Priority | Category | Summary |
|:---|:---:|:---|:---|
| DEPLOY-006 | P1 | Security | Deploy Coraza WAF (INFRA-015) + remediate CIS findings (SEC-020) + Wazuh SIEM (INFRA-011) |
| DEPLOY-011 | P1 | Promotion | Make the existing SIT/UAT/preprod/prod workload overlays deploy-safe: remove dev secrets/endpoints, add environment-isolated DB/Kafka/Data Grid paths and immutable images, correct prod namespace/RBAC, raise quotas, and run gates sequentially. Secrets delivery is unblocked — VSO (VaultStaticSecret) is Ready 15/15 in all four envs (`aafa0b03`). | 🔄 Blocked before SIT workload deploy |
| INFRA-026 | P1 | Secrets | Replace ephemeral dev-mode Vault before SIT with HA durable Vault, auto-unseal, backup/restore evidence, and non-root short-lived ESO authentication. Dev Vault inmem restart on 2026-07-31 wiped KV and broke 8 External Secrets; paths repopulated as recovery (L-135), durable replacement remains required. | 🔄 Planned |
| DEPLOY-009 | P2 | CI/CD | Tekton Chains (INFRA-013) + Results (INFRA-014) + Renovate (DEVSECOPS-011) |
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

---

## 📝 Platform Workload Audit Details

### ⚙️ INFRA-025 / ARCH-007: Infinispan Hot Rod Migration
* **Original**: Netty SSL ApplicationProtocolNegotiationHandler warnings & `ISPN005061` RESP unclosed iterator warnings.
* **Status**: 🔄 IN PROGRESS. Local mTLS is verified for `cache-starter` and Quarkus gateway Hot Rod plus KYC/analytics REST. `cache-starter` uses Infinispan 16.2.1 native Hot Rod with a lazy `RemoteCacheManager`, 10,000-entry invalidated near cache, and explicit UTF-8 JSON-text values in the `payu` cache. Auth refresh tokens, partner SNAP-BI tokens, API-commons atomic paths, and Quarkus gateway paths use Hot Rod. KYC and analytics idempotency use authenticated Data Grid REST because the Python Hot Rod client is unmaintained.
* **Remaining**: Completion of the 24-hour `payu-dev` canary (final stack since 2026-07-31 20:47Z: operator-managed mTLS Data Grid; so far zero `RedisConnectionException`/`ISPN005061` across backend logs), then production promotion (TLS/mTLS secrets for SIT/UAT/preprod/prod already operator-managed where provisioned). Do not claim ARCH-007 complete before those evidence gates pass.
* **Updated**: 2026-07-19.

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
* **Status (2026-07-31)**: Java/Quarkus tidak lagi membawa Redis/Lettuce/Quarkus Redis client; cache bernama `payu` tersedia pada Data Grid dengan media type `text/plain`. Focused build, Python REST round-trip, REST-write → Hot Rod-read, dan mTLS positive/negative gate lulus. `SPRING_MAIN_SOURCES` bridge diganti metadata auto-config durable (`AutoConfiguration.imports` + `HotRodCacheConfig`); semua deployment backend diberi label Kyverno exclusion (L-146); health check lazy-start cache bernama (`HotRodCacheSupport`); Data Grid dev operator-managed + mTLS lengkap (server TLS, client CA, keystore, identities literal-password — L-147/148); deployment manual + `payu-cache-resp` dihapus; rollout dev aggregate health UP, 0 error cache, canary 24 jam final berjalan sejak 20:47Z.
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
