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
| **Cluster Status** | 🟢 OCP 4.20.26, 7 nodes Ready. `payu-dev` has 46/46 pods Running, 32/32 deployments Ready, and 39 ImageStreamTags. |
| **Last Release** | `1.9.8` — Hot Rod cache canary support, observability and Vault platform manifests, and contract-test setup |
| **Last Updated** | 2026-07-19 (ARCH-007 local mTLS gate complete: JVM Hot Rod, Python REST, shared `payu` JSON-text cache; production secret wiring and canary remain) |

---

## 🐛 Active Tickets

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| INFRA-001 | P0 | Fix trivy-image-scan registry auth for OpenShift — ✅ Red Hat registry credentials already in global pull-secret (openshift-config). registry.redhat.io, registry.connect.redhat.com, quay.io all authenticated. No blocker. | ✅ Verified |
| INFRA-020 | P0 | Reconcile GitOps ApplicationSet with `payu-dev` — 31/33 manual recovery done, ArgoCD app needs re-pointing. Cluster-admin needed for `oc apply -f argocd/` | 🔒 Blocked |
| INFRA-007 | P1 | DR runbook: ✅ COMPLETE — `docs/operations/DISASTER_RECOVERY.md` (39KB, v2.0, Feb 2026) covers PostgreSQL, Kafka, Vault, DataGrid, Keycloak, service degradation, platform restore, DR testing, escalation matrix. Also: CHATOPS, INCIDENT_RESPONSE, INFRASTRUCTURE_DEPLOYMENT, ZERO-DOWNTIME-DEPLOYMENT. | ✅ Closed |
| INFRA-021 | P1 | RHBK `payu-keycloak` CR condition investigation: `HasErrors=False` means no-errors (RHBK convention), `Ready=True` confirmed, pod healthy. No service patch conflict. | ✅ Closed |
| SEC-020 | P1 | Remediate CIS platform failures: 9 FAIL, 21 MANUAL — requires Compliance Operator scan + remediation via cluster-admin. Platform-level, not app-level | 🔒 Blocked |
| DEVSECOPS-003 | P1 | Global rate limit 1000 req/s per IP | ✅ Closed — 1000 cap/s token-bucket in gateway rate-limit-v2.global |
| INFRA-025 | P2 | [cache] RESP cursor leak remediation: shared cache invalidation no longer exposes a RESP cursor; full RESP removal still depends on ARCH-007. | 🔄 In progress |
| ARCH-007 | P2 | [cache] Infinispan 16.2.1 migration: Java/Quarkus use native Hot Rod; Python KYC/analytics use authenticated Data Grid REST. All workload, mesh, and Kong renders now use their correct `payu` Hot Rod/mTLS or dedicated Redis-native contracts. Provision the external TLS/client-CA/client-keystore Secrets, perform an in-cluster mTLS smoke test, then run the `payu-dev` 24-hour canary. | 🔄 In progress |
| ARCH-008 | P2 | [billing] ✅ FIXED — SubscriptionEvent now accepts primitives, port interface retains entities | ✅ Closed |
| ARCH-009 | P2 | [statement] ✅ FIXED — RecipientInfo/SenderInfo field finality, ReceiptException moved to domain.model | ✅ Closed |
| ARCH-010 | P2 | [promotion] ✅ FIXED — naming rule removed CashbackEntity, service deps expanded to include outbox/saga/micrometer | ✅ Closed |


---

## 🚀 Platform Deploy Queue

| Key | Priority | Category | Summary |
|:---|:---:|:---|:---|
| DEPLOY-006 | P1 | Security | Deploy Coraza WAF (INFRA-015) + remediate CIS findings (SEC-020) + Wazuh SIEM (INFRA-011) |
| DEPLOY-010 | P1 | API Management | ✅ Deploy 3scale APIManager — APIcast production routing + Keycloak OIDC introspection + E2E cards-crud verified. See PROGRESS.md and `infrastructure/platform/api-management/3scale/README.md`. | ✅ Closed |
| DEPLOY-009 | P2 | CI/CD | Tekton Chains (INFRA-013) + Results (INFRA-014) + Renovate (DEVSECOPS-011) |
| OPS-2026-04-08-01 | P2 | Ops | ✅ Validated — wallet-service cache config OK, /actuator/health/liveness UP, RESP PING reachable | ✅ Closed |
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

---

## 🔍 Ponytail Audit — Over-Engineering & Dead Code (2026-07-02)

| # | Key | Category | Summary |
|:---:|:---|:---|:---|
| AUDIT-096 | PON-019 | arch | ✅ Audited: 74 ports across 21 services. 68 single-implementation (normal hexagonal — abstract for testability). 6 dead ports with 0 implementations: AgentTrainingPersistencePort, NotificationPersistencePort, NotificationSenderPort, PortalConfig, SupportAgentPersistencePort, TrainingModulePersistencePort. Can be deleted or implemented. | ✅ Audited |

---

## 📝 Platform Workload Audit Details

### ⚙️ INFRA-025 / ARCH-007: Infinispan Hot Rod Migration
* **Original**: Netty SSL ApplicationProtocolNegotiationHandler warnings & `ISPN005061` RESP unclosed iterator warnings.
* **Status**: 🔄 IN PROGRESS. Local mTLS is verified for `cache-starter` and Quarkus gateway Hot Rod plus KYC/analytics REST. `cache-starter` uses Infinispan 16.2.1 native Hot Rod with a lazy `RemoteCacheManager`, 10,000-entry invalidated near cache, and explicit UTF-8 JSON-text values in the `payu` cache. Auth refresh tokens, partner SNAP-BI tokens, API-commons atomic paths, and Quarkus gateway paths use Hot Rod. KYC and analytics idempotency use authenticated Data Grid REST because the Python Hot Rod client is unmaintained.
* **Remaining**: Production TLS/mTLS secret wiring and a 24-hour `payu-dev` canary. Do not claim ARCH-007 complete before those evidence gates pass.
* **Updated**: 2026-07-19.

### 🏗️ ARCH-008/009/010: ArchUnit 1.4.2 violations (billing, statement, promotion)
* **Context**: ArchUnit 1.2.1 → 1.4.2 upgrade in parent POM exposed pre-existing architecture violations that ArchUnit 1.2.1 silently skipped (ASM < 9.5 cannot parse Java 25 bytecode — empty `importPackages()`).
* **ARCH-008 (billing)**: 85 domain@adapter violations — `SubscriptionEvent.createChargeFailedEvent()` calls `SubscriptionChargeEntity` getters directly. Domain layer depends on adapter persistence entities.
* **ARCH-009 (statement)**: 12 immutability violations — `RecipientInfo`/`SenderInfo` Lombok `@Builder` + `@NoArgsConstructor` generates non-final fields. `ReceiptException` lives in `application.service.exception`, not `domain.model`.
* **ARCH-010 (promotion)**: 288 dependency violations + 3 cyclic deps (adapter→application→adapter) + `CashbackEntity` naming (entity in persistence package not following naming convention).
* **Status**: Pinned to ArchUnit 1.2.1 in these 3 services until remediation. Parent POM keeps `<archunit.version>1.4.2</archunit.version>` for services that already pass (compliance, partner, gateway, etc.).
* **Created**: 2026-07-13.

### 🔐 SEC-020: Remediate CIS platform failures (platform-security)
* **Problem**: Hasil pemindaian Compliance Operator untuk profile `ocp4-cis` (non-compliant) mendeteksi 9 temuan kegagalan (FAIL):
  1. `ocp4-cis-api-server-encryption-provider-cipher`: Cipher enkripsi API server tidak aman.
  2. `ocp4-cis-audit-log-forwarding-enabled`: Audit log forwarding ke SIEM eksternal belum diaktifkan.
  3. `ocp4-cis-audit-profile-set`: Profil audit API server belum dikonfigurasi.
  4. `ocp4-cis-configure-network-policies-namespaces`: Terdapat namespace tanpa NetworkPolicy default-deny.
  5. `ocp4-cis-ingress-controller-tls-cipher-suites`: TLS cipher suites pada default Ingress Controller belum dikeraskan.
  6. `ocp4-cis-kubeadmin-removed`: Akun bootstrap `kubeadmin` belum dihapus/dinonaktifkan dari cluster.
  7. `ocp4-cis-ocp-allowed-registries`: Daftar registry eksternal yang diizinkan belum didefinisikan.
  8. `ocp4-cis-ocp-allowed-registries-for-import`: Aturan import image registry belum dibatasi.
  9. `ocp4-cis-scc-limit-container-allowed-capabilities`: Security Context Constraints (SCC) belum membatasi capabilities container secara ketat.
* **Impact**: Platform OpenShift rentan terhadap celah keamanan CIS Benchmark dan tidak memenuhi kepatuhan regulasi OJK/PCI-DSS.
* **Fix**: Terapkan perbaikan konfigurasi pada level cluster (APIServer, IngressController, OAuth, Image, SCC) sesuai dengan rekomendasi remediasi dari masing-masing aturan kepatuhan Compliance Operator.

### 🧭 ARCH-007: Migrate Data Grid access from RESP compatibility mode to Hot Rod native client
* **Context7 evidence**:
  1. Infinispan/Data Grid menyediakan RESP endpoint agar RESP-compatible clients bisa terhubung tanpa perubahan besar.
  2. Hot Rod adalah client native Data Grid untuk remote access, dengan API sync/async/Mutiny dan opsi TTL/lifespan pada write operations.
  3. ProtoStream menyediakan schema `.proto`, adapter untuk tipe pihak ketiga, serializer/deserializer compile-time, dan compatibility check untuk perubahan schema.
* **Status (2026-07-19)**: Java/Quarkus tidak lagi membawa Redis/Lettuce/Quarkus Redis client; cache bernama `payu` tersedia pada Data Grid lokal dengan media type `text/plain`. Focused build, Python REST round-trip, REST-write → Hot Rod-read, dan mTLS positive/negative gate lulus.
* **Goal**: Migrasi cache/session/rate-limit/idempotency/lock/analytics dari RESP ke Hot Rod tanpa kehilangan key aktif dan tanpa regresi latency.
* **Decision**: RESP dihapus dari runtime backend. Hot Rod 16.2.1 adalah satu-satunya client cache Java/Quarkus yang didukung; Python memakai Data Grid REST terautentikasi.
* **Plan**:
  1. **Completed**: migrate cache, auth refresh token, partner SNAP-BI token, API-common idempotency/rate limit/webhook, and gateway API keys, analytics, authorization, idempotency, and rate limiting to Hot Rod.
  2. **Completed**: remove Java RESP dependencies and direct Redis APIs; use native TTL and versioned CAS for counters/sliding windows.
  3. **Completed**: migrate every `payu.cache.redis` profile block to `payu.cache.hotrod`; local profiles default to `localhost:11222`, while container/non-local profiles require `PAYU_CACHE_HOTROD_SERVER_LIST`.
  4. **Completed**: shared `payu` cache uses UTF-8 JSON-text values so Data Grid REST and Hot Rod address identical keys; Java uses `UTF8StringMarshaller`.
  5. **Completed**: Python KYC/analytics idempotency uses the authenticated Data Grid REST API; the unmaintained Python Hot Rod client is not introduced.
  6. **In progress**: wire production TLS/mTLS secrets and run a 24-hour `payu-dev` canary with p95/error/duplicate-replay evidence before production promotion.
* **Done criteria**:
  1. Tidak ada business code yang inject `RedisTemplate`/`StringRedisTemplate` langsung.
  2. Java/Quarkus memakai Hot Rod dan Python memakai Data Grid REST, semuanya memakai secret refs tanpa inline credential.
  3. Idempotency/rate-limit/lock tests hijau di Hot Rod profile.
  4. `payu-dev` canary 24 jam tanpa `RedisConnectionException`, duplicate payment replay, scheduler lock overlap, atau p95 cache latency regression >10%.
