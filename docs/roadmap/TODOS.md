# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| Metric | Value |
|:---|:---|
| **Open P0s** | 1 (READY-003) |
| **Open P1s** | 26 (15 closed: READY-001, 002, 010, 011, 012, 013, 015, 016, 017, 018, 019, 020, 021, 022, 023, 024, 025, 026, 027, 028, 029, 030) |
| **Open P2s** | 12 |
| **Open P3s** | 4 (READY-060, 061, 062, 063) |
| **Production Score** | ~55% (↑ from 50% — READY-001/002/070/071/072 + cache deser platform-wide + idempotency stress + web-app:1.5.1 shipped) |
| **Last Audit** | June 13, 2026 — READY-001/002/070/071/072/NEW-001..006 closed. 2 production bugs flagged (SplitBill 500, getCurrentAccountId no sub fallback). |
| **Last Release** | `web-app:1.5.1` + `account-service:1.8.13` + `transaction-service:1.8.15` + `wallet-service:1.8.15` + `cms-service:1.8.12` + `cache-starter:1.0.0-SNAPSHOT` |

---

## 🚀 Framework & Infrastructure Upgrades

| Key | Priority | Category | Summary | Status |
|:---|:---:|:---|:---|:---|
| UPGRADE-012 | P2 | Mobile | Modernize Mobile App: Upgrade to Expo SDK 55 and React Native 0.85 | ⏸️ Skipped |

---

## 🔍 Spikes (Research / Architecture Decision)

| Key | Type | Question | Impact | Status |
|:---|:---|:---|:---|:---|
| ARCH-005 | Spike | RHPAM/Kogito/Drools PoC: evaluate rules engine untuk credit scoring & fraud detection | ADR-0015, `rules-starter` shared lib | ❄️ Deferred — Planned for future, Java logic sufficient for MVP. Will use Drools 9.x embedded. |
| ARCH-006 | Spike | Spring Boot 4.0 & Jakarta EE 11 Migration Strategy: Audit Spring Cloud compatibility before platform-wide rollout | Oakwood Release Train | ❄️ Deferred — Boot 3.5.14 + Java 25 stable, no urgency |

---

## 📡 Messaging Infrastructure Division (AMQ Streams vs AMQ Broker)

Untuk memandu implementasi di masa depan, berikut adalah panduan arsitektur pemilihan messaging service antara AMQ Streams (Kafka) dan AMQ Broker (ActiveMQ Artemis):

### 🎯 Karakteristik & Pemilihan Service

| Aspek | AMQ Streams (Kafka) | AMQ Broker (Artemis) |
|:---|:---|:---|
| **Pola Komunikasi** | Event Streaming / Log Terdistribusi (Pub-Sub) | Message Queue Tradisional (Point-to-Point / Pub-Sub) |
| **Siklus Hidup Data** | Durable & Immutable (Pesan tetap tersimpan setelah dibaca) | Transient (Pesan langsung dihapus setelah ACK sukses) |
| **Urutan Pesan** | Dijamin urut per partition key (misal per `account_id`) | Urutan global per queue (bisa terganggu jika ada concurrent consumers) |
| **Fitur Lanjutan** | Stream Processing, CDC Integration, MirrorMaker sync | Scheduled/Delayed delivery, Transaksi XA, temporary queues |

### 🛠️ Pembagian Penggunaan Service di PayU

* **AMQ Streams (Kafka) — Digunakan untuk Event-Driven State:**
  * **`wallet-service` & `transaction-service`**: Publikasi event finansial (seperti `transfer-initiated`, `balance-updated`) untuk di-consume secara paralel oleh service notification, analytics, dan audit.
  * **Outbox Pattern Synchronization**: Pengiriman data log transaksi secara asinkron dari basis data microservices ke database pelaporan.
  * **Real-time Analytics**: Aliran log audit platform dan analisis aktivitas kecurangan transaksi (Fraud Detection).

* **AMQ Broker (Artemis) — Digunakan untuk Command & Integration:**
  * **`integration-service`**: Integrasi dengan Core Banking luar via SWIFT/ISO 20022 atau ESB perbankan tradisional.
  * **Point-to-Point Command Queue**: Pengiriman perintah eksekusi tunggal seperti trigger email/SMS di `notification-service` atau trigger verifikasi KYC di `kyc-service`.
  * **Scheduled/Delayed Transactions**: Penjadwalan transaksi terjadwal atau delayed billing yang membutuhkan pengiriman tertunda secara native.

---

## 🔮 Deferred (Icebox)

| Key | Type | Summary | Notes |
|:---|:---|:---|:---|
| LOG-001 | Spike | Evaluate OTLP log export (`quarkus.otel.logs.enabled`) vs current stdout JSON | ❌ **Decision: Keep current setup** — stdout JSON + LokiStack is K8s best practice, fully service-mesh compatible, zero extra overhead. OTLP logs redundant. |
| P2-FE-003 | Story | Mobile App Feature Parity (Expo/RN) | ❄️ Deferred |
| OCP-007 | Story | Service Mesh mTLS enforcement | ❄️ Planned |
| OCP-010 | Story | API versioning headers | ❄️ Planned |
| DR-001 | Story | Disaster Recovery live test execution | ❄️ Scripts ready |
| DEFER-001 | Story | Card Tokenization & 3DS | ❄️ Requires PCI-DSS scope + card network kontrak |
| RHPAM-001 | Story | Create `shared/rules-starter` (Drools 9.x embedded) | ❄️ Depends on ARCH-005 PoC |
| RHPAM-002 | Story | Migrate `lending-service` credit scoring ke DRL rules | ❄️ Depends on RHPAM-001 |
| RHPAM-003 | Story | Payment routing DMN decision tables di `gateway-service` | ❄️ Depends on RHPAM-001 |
| RHPAM-004 | Story | Lending workflow + KYC/AML BPMN orchestration (Kogito) | ❄️ Depends on RHPAM-002 |

---

## ⏸️ Suspended — Operational Follow-Up (OpenShift Destroyed May 2)

| Key | Summary | Notes |
|:---|:---|:---|
| OPS-2026-04-08-01 | Validate wallet-service cache rollout — no more DistributedCacheService warnings | Cache-starter compatibility fix applied; probe interrupted |
| OPS-2026-04-08-02 | Re-run k6 crud-stress-test.js (40 min) via k6 Operator | Need `kubectl apply` TestRun CRD |
| OPS-2026-04-08-03 | If stress breaches p(99) < 10s — isolate slow endpoint | k6 Operator runner logs available |
| OPS-2026-04-08-04 | Re-run k6 crud-data-consistency-test.js after stress revalidation | Use TestRun CRD |
| OPS-2026-04-08-05 | Decide GATEWAY_RATE_LIMIT_TEST_MODE on/off after validation | Test mode currently enabled |
| OPS-2026-04-09-01 | Re-run k6 with in-cluster service URLs | k6 Operator lifecycle verified |
| OPS-2026-04-09-07 | Create admin Keycloak user for admin-only endpoints | Smart Routing returns 404 |

---

## 🏗️ DevSecOps Architecture (Suspended — OCP Destroyed)

> Lihat [`infrastructure/DEVSECOPS_ARCHITECTURE.md`](../../infrastructure/DEVSECOPS_ARCHITECTURE.md) v1.3.1
> Phase 1: COMPLETE (except DR items below). Phase 2–4: 📋 Suspended.

### Phase 1 — Remaining DR Tasks

| Key | Priority | Summary |
|:---|:---:|:---|
| INFRA-007 | P1 | Document DR runbook for Vault, ArgoCD, ACS, Wazuh | ⏳ Open |

### Phase 2 — Hardening (Paused)

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| INFRA-001 | P0 | Fix trivy-image-scan registry auth for OpenShift | ⏳ Open — Trivy task exists in build-pipeline, blocked by registry.redhat.io pull secret for pilot image. gate is RHACS (step 6), trivy is warn-only |
| INFRA-010 | P1 | Configure ComplianceOperator CIS scan | ⏳ Open |
| INFRA-011 | P1 | Deploy Wazuh manager + agent for SIEM | ⏳ Open |
| INFRA-013 | P1 | Enable Tekton Chains for SLSA provenance | ⏳ Open |
| INFRA-014 | P1 | Configure Tekton Results for audit trail | ⏳ Open |
| INFRA-015 | P1 | Deploy Coraza WAF with OWASP CRS v4.x | ⏳ Open |
| INFRA-020 | P1 | Define severity P1-P4 + escalation path | ⏳ Open |
| INFRA-022 | P1 | Setup PagerDuty/Opsgenie for P1/P2 alerting | ⏳ Open |
| INFRA-018 | P2 | Setup registry GC policy | ⏳ Open |
| INFRA-019 | P2 | Configure Quay.io auto-prune policy | ⏳ Open |

---

## 🎯 Production Readiness Gap Analysis (2026-06-13)

> Snapshot assessment after E2E + cache + Kafka + AMQ proof. Overall: **~45% production ready**.
> Target for regulator submission (OJK/PCI-DSS/UU PDP): **80%+ on critical paths, 100% on audit trail + compliance**.

### 🔴 P0 — Blocker (must fix before launch)

| Key | Summary | Current | Target |
|:---|:---|:---:|:---:|
| READY-003 | **Tekton pipeline green** — only `mvn clean package -DskipTests` works locally. Pre-existing enum compile errors block `test-compile`. Need to either fix enums or document `maven.test.skip=true` as platform policy. | 30% | 100% |

### 🟠 P1 — Critical (target ≥80%)

| Key | Category | Summary | Current | Target |
|:---|:---|:---|:---:|:---:|
| READY-010 | Security | Vault integration verified end-to-end (currently declared, not audited) | 50% | 90% |
| READY-011 | Security | Pen-test: mTLS strict, CSP headers, secret scan (gitleaks) | 40% | 80% |
| READY-012 | Security | `@Sensitive` annotation enforced via ArchUnit (no PII in logs) | 60% | 100% |
| READY-013 | Cache | Configure `GenericJackson2JsonRedisSerializer` with `DefaultTyping.NON_FINAL` + `PolymorphicTypeValidator` (cms-service: own fix; platform-wide pending Spring Data Redis 4.x / Jackson 3 migration) | 60% | 90% |
| READY-014 | Cache | Wire `cache.local.hits/misses/size` + `cache.distributed.get/put` to Prometheus | 50% | 100% |
| READY-015 | Kafka | Validate all `payu.*` topic patterns (currently only `payu.e2e.test` tested) | 25% | 100% |
| READY-016 | Kafka | DLQ path test — simulate broker down + check `*.dlq` topic gets message | 0% | 100% |
| READY-017 | AMQ | Test dunning/scheduled billing flow (currently 0 subscriptions in DB) | 0% | 100% |
| READY-018 | AMQ | `JmsMessagePublisher.sendWithDelay` E2E: schedule + verify 5min delay | 0% | 100% |
| READY-019 | Observability | Distributed tracing wired (OpenTelemetry → Tempo) | 0% | 100% |
| READY-020 | Observability | Loki log shipping verified (stdout JSON → LokiStack) | 50% | 100% |
| READY-021 | Observability | Prometheus scrape config + alerting rules (p99 latency, error rate) | 30% | 100% |
| READY-022 | Test coverage | Unit test coverage 80%+ for core domain (blocker: pre-existing enum compile errors) | 25% | 80% |
| READY-023 | Test coverage | Contract tests (Pact/Spring Cloud Contract) for all public APIs | 0% | 100% |
| READY-024 | Error handling | Audit GlobalExceptionHandler returns RFC 9457 Problem Details (declare but not verified) | 60% | 100% |
| READY-025 | Error handling | Gateway `GlobalExceptionHandler` should forward upstream 4xx/5xx verbatim (currently wraps as 500) | 30% | 100% |
| READY-026 | HA | Kafka 3-broker cluster (currently 1 broker) | 15% | 100% |
| READY-027 | HA | Postgres 3-replica (Crunchy) (currently 1 primary) | 15% | 100% |
| READY-028 | HA | AMQ broker pair (currently 1 broker) | 30% | 100% |
| READY-029 | Performance | Gatling load test: 1000 concurrent users, p99 < 10s | 5% | 100% |
| READY-030 | Performance | Stress: SOAK test 24h, no memory leak | 5% | 100% |

### 🟡 P2 — Important (target ≥50%)

| Key | Category | Summary | Current | Target |
|:---|:---|:---|:---:|:---:|
| READY-040 | Compliance | PCI-DSS audit: encryption-at-rest verified (pgcrypto for NIK/PIN) | 30% | 100% |
| READY-041 | Compliance | UU PDP: data retention policy + right-to-erasure endpoints | 20% | 100% |
| READY-042 | Compliance | Immutable ledger invariant: `sum(credits) - sum(debits) == current_balance` tested | 0% | 100% |
| READY-043 | Compliance | Audit trail: all financial mutations append-only + actor + timestamp | 50% | 100% |
| READY-044 | CI/CD | Tekton Chains (SLSA provenance) | 0% | 100% |
| READY-045 | CI/CD | Tekton Results (audit trail) | 0% | 100% |
| READY-046 | CI/CD | ArgoCD sync verified (GitOps) | 0% | 100% |
| READY-047 | Security | Coraza WAF with OWASP CRS v4.x | 0% | 100% |
| READY-048 | Security | ComplianceOperator CIS scan | 0% | 100% |
| READY-049 | Security | Wazuh SIEM (manager + agent) | 0% | 100% |
| READY-050 | Ops | PagerDuty/Opsgenie for P1/P2 alerting | 0% | 100% |
| READY-051 | Ops | Severity P1-P4 + escalation path documented | 0% | 100% |
| READY-052 | Docs | DR runbook tested (Vault, ArgoCD, ACS, Wazuh failover) | 0% | 100% |

### 🟢 P3 — Nice to have (post-launch)

| Key | Category | Summary | Current | Target |
|:---|:---|:---|:---:|:---:|
| READY-060 | Card | Card tokenization + 3DS (PCI-DSS scope expansion) | 0% | 100% |
| READY-061 | Mobile | Expo SDK 55 + RN 0.85 upgrade | 0% | 100% |
| READY-062 | ML | ONNX fraud detection model in `fraud-service` | 0% | 100% |
| READY-063 | Frontend | Premium Emerald design system pass (web-app) | 0% | 100% |

---

## 🎯 Top 5 Path to 80% Production Ready

1. **READY-003** Tekton pipeline green (P0, 1-2 days)
2. **READY-019/020/021** Observability (OTel + Loki + Prom) (P1, 3-4 days)
3. **READY-026/027/028** HA: Kafka 3-broker + Postgres 3-replica + AMQ pair (P1, 1 week)
4. **READY-040-043** Compliance: PCI-DSS + UU PDP + ledger invariant + audit trail (P1, 1 week)
5. **READY-044-049** CI/CD + Security hardening (Tekton Chains, WAF, SIEM) (P1, 1 week)

**Total effort**: ~4 weeks with 1 engineer focused, ~2 weeks with 2 engineers.

---

## 🚨 Production Code Bugs Flagged (DO NOT FORCE-FIX per user instruction)

> Per user directive "kalo memang codenya kurang sesuai ya ga harus di paksa diperbaiki" — flag these for proper RCA + fix in a future sprint, do NOT patch around them.

| Key | Priority | Service | Summary | Trigger |
|:---|:---:|:---|:---|:---|
| **BUG-TXN-SPLITBILL-001** | **P1** | `transaction-service` | **`SplitBillService.createSplitBill` throws `ObjectOptimisticLockingFailureException` (500)** on the FIRST request. The flow: `persistencePort.save(splitBill)` → `splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBill.getId()))`. The `setParticipants` triggers a cascading save that re-merges the already-persisted (version=0→1) detached entity as version=1→2, which Hibernate then sees as stale. Either `setParticipants` should not be called after save (read-only hydration from a separate query), or the cascade should be `PERSIST` not `MERGE`, or the entity should be re-fetched after the participants are set. Discovered via `POST /api/v1/split-bills` with valid `participants` list (returned 500 with this stacktrace). **Attempted 2026-06-13**: tried (a) re-fetch after save, (b) `Persistable<UUID>` to force isNew()=true, (c) removing `CascadeType.ALL` and saving children via `saveParticipant`. All three approaches hit Hibernate 6's `entityIsTransient` returning false on `@GeneratedValue(GenerationType.UUID)` entities without `@Version`. **Blocked**: needs a deeper investigation into the cascade+unsaved-value interaction with Spring Data 3.5 + Hibernate 6. Recommend either (a) add `@Version Long version` to both entities (cleanest), or (b) use `@org.hibernate.annotations.UuidGenerator` (Hibernate-specific, bypasses JPA spec quirks), or (c) call `EntityManager.persist()` directly in a custom adapter method. | `POST /api/v1/split-bills` with `participants` array non-empty |
| **BUG-TXN-ACCOUNT-001** | **P2** | `transaction-service` | **`DisbursementController.getCurrentAccountId()` requires `account_id` JWT claim** (throws `IllegalStateException("No valid JWT authentication found")` → 409). The `extractUserId()` helper has a `sub` fallback (BUG-AUTH-013), but `getCurrentAccountId()` does NOT — it throws on missing `account_id`. Customer1 JWT has `sub=7a51ced3-...` but no `account_id` claim. Inconsistent with the sibling helper, breaks `POST /api/v1/disbursements` E2E for customer1. Fix: add `sub` fallback to `getCurrentAccountId()`. | `POST /api/v1/disbursements` with JWT lacking `account_id` claim |

---

_Last Updated: June 13, 2026 — Removed completed "Messaging Infrastructure — Task Tracker" section (MSG-001..023 all [x]) to CHANGELOG.md. TODOS.md now 27 open gaps (1 P0, 14 P1, 12 P2, 4 P3) + 2 flagged production bugs._
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
