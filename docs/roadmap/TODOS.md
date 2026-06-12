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
| **Open P0s** | 0 (all resolved) |
| **Open P1s** | 11 |
| **Open P2s** | 12 |
| **Last Audit** | June 10, 2026 — All 12 production readiness audit items resolved (2 batches) |
| **Production Score** | 99/100 |

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

## ✉️ Messaging Infrastructure — Task Tracker

### Category B: Outbox Migrations (P1 - Security/Atomicity)
- [ ] MSG-007: Update `account-service` `KafkaUserEventPublisherAdapter`
- [ ] MSG-008: Update `promotion-service` notification adapter + 4 services
- [ ] MSG-009: Update `partner-service` `PaymentLinkService` & `MerchantService`
- [ ] MSG-010: Update `cms-service` `ContentEventPublisher`
- [ ] MSG-011: Update `investment-service` `KafkaInvestmentEventPublisherAdapter`
- [ ] MSG-012: Update `fx-service` `FxRateEventPublisher`
- [ ] MSG-013: Update `statement-service` `StatementEventPublisher`
- [ ] MSG-014: Update `billing-service` `BillingEventPublisher`
- [ ] MSG-015: Update `transaction-service` `PaymentExpiryScheduler`
- [ ] MSG-016: Update `integration-service` `BIFastTransferService` & `SnapTransferService` and `security-starter` `AuditLogPublisher` → `outbox-starter`
- [ ] MSG-017: `integration-service` `MessagePublisherAdapter` → `outbox-starter`

### Category A: Artemis Infrastructure (P2)
- [ ] MSG-001: Create `shared/jms-starter`
- [ ] MSG-002: Setup Artemis in Podman Compose
- [ ] MSG-003: Migrate `integration-service` → Artemis
- [ ] MSG-004: Implement Artemis in `notification-service`
- [ ] MSG-005: Implement Artemis delayed delivery in `billing-service`
- [ ] MSG-006: Implement Artemis command queue in `kyc-service`

### Category C: Topic Naming (P2)
- [ ] MSG-018: Standardize all topic names

### Category D: DLQ Strategy (P2)
- [ ] MSG-019: Implement DLQ in `events-starter`/`outbox-starter`
- [ ] MSG-020: Configure DLQ per service consumer

### Category E: CloudEvents Format (P2)
- [ ] MSG-021: Enforce CloudEvents in `outbox-starter`
- [ ] MSG-022: Migrate consumers to CloudEvents

### Category F: Consumer Hardening (P3)
- [ ] MSG-023: Refactor `notification-service` `EventConsumer`

### Build & Test
- [ ] Build all shared starters
- [ ] Build all affected services
- [ ] Run tests

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
| ~~OPS-2026-04-09-06~~ | ✅ Fix transaction-service Redis/DataGrid RESP connection (port 11222) | Resolved — RateLimitAspect & RateLimitInterceptor now catch DataAccessException, gracefully allow requests when Redis/DataGrid unreachable |
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



---

_Last Updated: June 9, 2026 — Removed HCP-001 s/d HCP-013. Only open/deferred items remain._
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
