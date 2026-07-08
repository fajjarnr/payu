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
| **Last Release** | `1.9.3` — P2 workload stability audit after `payu-dev` recovery |
| **Last Updated** | 2026-07-08 (INFRA-023 and DEV-105 closed from live cluster evidence; INFRA-025 cache RESP warning remains open) |

---

## 🐛 Active Tickets

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| INFRA-001 | P0 | Fix trivy-image-scan registry auth for OpenShift | ⬜ Open |
| INFRA-020 | P0 | Reconcile GitOps ApplicationSet with manually recovered `payu-dev` workloads | ⬜ Open |
| INFRA-007 | P1 | Document DR runbook for Vault, ArgoCD, ACS, Wazuh | ⬜ Open |
| INFRA-021 | P1 | Clear RHBK `payu-keycloak` CR `HasErrors=True` service patch conflict | ⬜ Open |
| SEC-020 | P1 | Remediate CIS platform failures: 9 FAIL, 21 MANUAL | ⬜ Open |
| DEVSECOPS-003 | P1 | Global rate limit 1000 req/s per IP | ⬜ Open |
| INFRA-025 | P2 | [cache] Resolve Netty SSL ApplicationProtocolNegotiationHandler warnings on port 11222 | ⬜ Open |
| ARCH-007 | P2 | [cache] Migrate Data Grid access from RESP compatibility mode to Hot Rod native client | ⬜ Open |
| DEVSECOPS-018 | P3 | [scripts] Update test-health-check.sh to support podman as fallback when docker is missing | ⬜ Open |


---

## 🚀 Platform Deploy Queue

| Key | Priority | Category | Summary |
|:---|:---:|:---|:---|
| DEPLOY-006 | P1 | Security | Deploy Coraza WAF (INFRA-015) + remediate CIS findings (SEC-020) + Wazuh SIEM (INFRA-011) |
| DEPLOY-010 | P1 | API Management | Deploy 3scale APIManager after production external backing-store/Vault secrets exist |
| DEPLOY-007 | P1 | Observability | OTel→Tempo (READY-019) + Loki (READY-020) + Prometheus alerts (READY-021) |
| DEPLOY-008 | P1 | DR/Security | Vault auto-snapshot (DEVSECOPS-001) + auto-unseal (DEVSECOPS-002) + DR runbook (INFRA-007) |
| DEPLOY-009 | P2 | CI/CD | Tekton Chains (INFRA-013) + Results (INFRA-014) + Renovate (DEVSECOPS-011) |
| OPS-2026-04-08-01 | P2 | Ops | Validate wallet-service cache rollout |
| OPS-2026-04-08-02 | P2 | Ops | Re-run k6 crud-stress-test.js via k6 Operator |
| READY-029 | P2 | Performance | Gatling load test: 1000 concurrent users |
| READY-030 | P2 | Performance | Stress: SOAK test 24h |
| READY-022 | P2 | Test | Unit test coverage 80%+ core domain |
| READY-023 | P2 | Test | Contract tests (Pact/SCC) |
| READY-060 | P3 | Card | Card tokenization + 3DS |
| READY-061 | P3 | Mobile | Expo SDK 55 + RN 0.85 upgrade |
| READY-062 | P3 | ML | ONNX fraud detection model |
| DEVSECOPS-014 | P3 | DevSecOps | Local Pipeline Simulation |
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
| AUDIT-096 | **PON-019** | arch | ~95 single-implementation hexagonal ports across 21 services. Consolidate when refactoring |

---

## 📝 Platform Workload Audit Details

### ⚙️ INFRA-025: Netty SSL ApplicationProtocolNegotiationHandler warnings on port 11222 (cache)
* **Problem**: Pod `payu-cache-0` mencatat warning netty SSL negotiation pada port 11222 (`ApplicationProtocolNegotiationHandler`).
* **Impact**: Kegagalan SSL/TLS ALPN negotiation saat mencoba terhubung ke cache server Infinispan.
* **Fix**: Konfigurasi client SSL context dengan ALPN protocol list yang tepat (seperti HTTP/1.1 atau h2) atau sesuaikan konfigurasi client dengan SSL profile Infinispan.

### 🛠️ DEVSECOPS-018: Update test-health-check.sh to support podman as fallback (scripts)
* **Problem**: Skrip `./scripts/test-health-check.sh` mengalami error `docker: command not found` di environment lokal yang menggunakan `podman` dan `podman-compose`.
* **Impact**: Developer workflow terganggu karena skrip pemeriksaan kesehatan environment pengujian gagal dijalankan.
* **Fix**: Modifikasi skrip agar mendeteksi keberadaan perintah `docker` dan `podman`, lalu secara dinamis menggunakan container command yang tersedia.

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
* **Problem**: PayU saat ini memakai Data Grid melalui RESP compatibility mode (`RedisTemplate`, Lettuce, Quarkus Redis client, dan env `*REDIS*`). Ini cepat untuk recovery cluster, tetapi aplikasi masih terikat pada semantik Redis dan belum memakai kemampuan native Data Grid.
* **Goal**: Migrasi cache/session/rate-limit/idempotency/lock/analytics dari RESP ke Hot Rod tanpa kehilangan key aktif dan tanpa regresi latency.
* **Decision**: RESP tetap jalur stabilisasi saat ini. Hot Rod dikerjakan sebagai migration track berikutnya setelah cluster `payu-dev` stabil.
* **Plan**:
  1. **Stabilkan RESP sekarang**: selesaikan hardening Data Grid RESP di `cache-starter` dan Quarkus Redis client path, termasuk handshake RESP2, auth `developer`, endpoint `payu-cache-resp:11222`, log scan, dan pod rollout. Tujuan fase ini hanya recovery/stabilitas, bukan redesign.
  2. **Catat semua Redis API sebagai debt**: inventory penggunaan `RedisTemplate`, `StringRedisTemplate`, `ReactiveRedisTemplate`, Lettuce/Jedis/Redisson, `quarkus-redis-client`, Python `redis`, `QUARKUS_REDIS_HOSTS`, `SPRING_DATA_REDIS_*`, `PAYU_CACHE_REDIS_*`, serta repository/cache class yang memakai command Redis langsung.
  3. **Mulai Hot Rod dari shared starter**: tambah `datagrid-hotrod-starter` atau refactor `cache-starter` dengan provider switch sementara (`resp`/`hotrod`) untuk canary dan rollback. Business service harus bergantung ke port/adaptor cache, bukan langsung ke `RedisTemplate` atau Hot Rod `RemoteCache`.
  4. **Canary satu service low-risk**: mulai dari `cms-service` atau `product-catalog-service`. Petakan operasi `GET`, `SETEX`, `DEL`, `EXISTS`, `INCR+EXPIRE`, `SCAN`, lock, idempotency replay, dan rate-limit ke operasi Hot Rod/Data Grid. Validasi TTL expiry, serialization round-trip, atomicity, rollback provider switch, dan p95 latency.
  5. **Migrasi path kritis bertahap**: setelah canary stabil, lanjut ke scheduler-heavy `partner-service`/`billing-service`, lalu gateway/rate-limit/idempotency paths, terakhir Quarkus/Python services. Untuk idempotency key/payment replay, lock scheduler, rate-limit counter, dan session, pakai dual-read/dual-write atau tunggu TTL aktif selesai sebelum cutover.
  6. **Cleanup setelah stabil**: setelah canary `payu-dev` minimal 24 jam tanpa `RedisConnectionException`, duplicate payment replay, scheduler lock overlap, atau p95 cache latency regression >10%, hapus RESP-only env `PAYU_CACHE_REDIS_*`, direct `RedisTemplate` usage dari aplikasi, RESP service alias, dan dokumentasi compatibility mode yang tidak lagi berlaku.
* **Done criteria**:
  1. Tidak ada business code yang inject `RedisTemplate`/`StringRedisTemplate` langsung.
  2. Semua workload memakai Hot Rod endpoint Data Grid dan secret refs, tanpa inline credential.
  3. Idempotency/rate-limit/lock tests hijau di Hot Rod profile.
  4. `payu-dev` canary 24 jam tanpa `RedisConnectionException`, duplicate payment replay, scheduler lock overlap, atau p95 cache latency regression >10%.
