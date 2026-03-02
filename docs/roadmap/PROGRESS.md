# 📈 PayU Platform — Progress & Engineering Scorecard

> **Dokumen ini adalah historical record & status snapshot PayU Platform.**
> Untuk open bugs dan actionable items → lihat [`TODOS.md`](./TODOS.md)
> Untuk arsitektur gateway & integrasi → lihat [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)

---

## 🏁 Current Status Snapshot

| Attribute                | Value                    |
| :----------------------- | :----------------------- |
| **Last Status Update**   | March 2, 2026            |
| **Production Readiness** | 99% (229/232 bugs fixed) |
| **OpenShift Tag**        | `v1.6.0` (in-progress)   |
| **Namespace**            | `payu-dev`               |
| **Total Pods**           | 36/36 running            |
| **Services Deployed**    | 22/22                    |
| **E2E Tests**            | 399/399 passing          |
| **Maven Build**          | 38/38 modules SUCCESS    |
| **Kafka Mode**           | KRaft (no Zookeeper)     |

> ✅ **Code Review Complete (Feb 24-25)**: 229 of ~232 bugs fixed (~99% resolution rate).
> **0 open bugs**. 3 intentionally skipped (low impact, future consideration).
> Lihat `TODOS.md` untuk detail skipped items.

---

## 🎯 Platform Maturity Scorecard

| Category             | Weight | Infra/Deploy Score | Notes                                           |
| :------------------- | :----- | :----------------- | :---------------------------------------------- |
| **Backend Services** | 25%    | 22/22 deployed     | ✅ All bugs fixed, biller-simulator added       |
| **Shared Libraries** | 10%    | 7/7 starters       | BUG-BE-091 skip (rate limit burst — acceptable) |
| **Frontend Web-App** | 15%    | Deployed & running | ✅ All cross-service issues resolved            |
| **Frontend Mobile**  | 5%     | Expo setup only    | Deferred                                        |
| **Testing**          | 15%    | 399/399 E2E pass   | ✅ Auth test gaps closed (useSilentRefresh)     |
| **Security**         | 10%    | JWT + OIDC active  | ✅ BUG-BE-001 fixed (nimbus-jose-jwt)           |
| **Infrastructure**   | 10%    | OpenShift HA       | HPA + PDB for all critical services             |

---

## 📈 DORA Metrics (Current Target)

| Metric                    | Target    | Current           | Alignment    |
| :------------------------ | :-------- | :---------------- | :----------- |
| **Deployment Frequency**  | ≥ 1/day   | Multiple/day (CI) | 🟢 **Elite** |
| **Lead Time for Changes** | < 4 hours | ~30 mins          | 🟢 **Elite** |
| **Mean Time to Recovery** | < 30 mins | ~15 mins          | 🟢 **Elite** |
| **Change Failure Rate**   | < 10%     | ~8%               | 🟢 **Elite** |

---

## 🏗️ Architectural Compliance

| Standard                   | Status            | Detail                                             |
| :------------------------- | :---------------- | :------------------------------------------------- |
| **Hexagonal Architecture** | ✅ 19/19 services | All Java/Quarkus services                          |
| **Event-First**            | ✅ Active         | `outbox-starter`, `events-starter`, `saga-starter` |
| **ArchUnit Governance**    | ✅ 18/19          | 1 service exempt with documented reason            |
| **Zero Trust**             | ✅ Per-service    | JWT + OIDC validation per endpoint                 |
| **API-First**              | ✅ 22/22          | OpenAPI spec per service                           |
| **Doc-as-Code**            | ✅ 13 ADRs        | `/docs/adr/`                                       |

---

## 📦 Deployment Log

### v1.6.0 (In Progress) — March 2, 2026

**Build Stabilization & Infrastructure Alignment:**

- ✅ **38/38 Maven Modules Compile** — Resolved all compilation errors across entire backend reactor build (`mvn clean package -DskipTests -T 1C`). 138 files changed.
- ✅ **partner-service** — Created `Refund`/`Dispute` domain models with lifecycle state machines, added `WebhookDispatcherService`/`KafkaTemplate` mocks to test constructors, fixed UUID type mismatches.
- ✅ **integration-service** — Removed non-existent `camel-cxf:4.4.0` dependency, fixed illegal regex escape characters in `SwiftTransformer`/`SwiftValidator`, added missing `MessageDirection` import.
- ✅ **promotion-service** — Fixed ArchUnit test API calls (replaced non-existent methods), CashbackSagaOrchestrator constructor args, WalletCreditException import.
- ✅ **transaction-service** — Lombok→manual conversion for domain models and DTOs, fixed `DisbursementServiceTest` checked exception handling.
- ✅ **fx-service** — Added `WalletServicePort` mock to `FxConversionServiceTest`.
- ✅ **support-service** — Converted Quarkus test annotations to Spring Boot.
- ✅ **billing-service** — Fixed port interfaces and pom dependencies.
- ✅ **product-catalog-service** — Fixed ArchTest, DTO validations, SecurityConfig.
- ✅ **gateway-service** — Fixed Redis/analytics/rate-limit service signatures.
- ✅ **statement-service** — Fixed ReceiptService and TestContainersConfig.
- ✅ **shared starters** — Fixed cache/saga/archunit test compilation.

**Infrastructure — Kafka KRaft Migration:**

- ✅ **Kafka Zookeeper → KRaft** — Migrated local Podman dev environment from `cp-kafka:7.5.0` + Zookeeper to `cp-kafka:7.7.1` KRaft mode. Aligned with AMQ Streams operator on OpenShift.
- ✅ **Removed Zookeeper** — Deleted `zookeeper.container`, `zookeeper.target` quadlet files. Updated `podman-compose.yml`, `podman-compose.test.yml`, `kafka.container`, `kafka.target`, `podman-payu.service`.
- ✅ **KRaft Config** — Combined broker+controller mode (`KAFKA_PROCESS_ROLES=broker,controller`), Raft consensus voters, static CLUSTER_ID.

### v1.5.0 (Completed) — February 28, 2026

**E-15 — Payment Gateway Features COMPLETED (Feb 28):**

All 7 stories finished (IMP-040 to IMP-046, 25 SP total):
- ✅ **IMP-040** — Payment Link webhooks (`payment_link.paid`, `payment_link.expired`) dengan HMAC-SHA256 signing
- ✅ **IMP-042** — VA Simulator service (Quarkus Native) dengan deterministic behavior untuk testing
- ✅ **IMP-044** — Payment expiry completion: balance release + Kafka events + scheduler job
- ✅ **IMP-045** — Dynamic QR settlement flow ke merchant wallet via `MerchantService`
- ✅ **IMP-046** — Mobile deeplink handler (`useDeeplinkHandler.ts`) dengan Expo Linking

**E-12 — Settlement & FinOps COMPLETED (Feb 28):**

4 stories finished (GAP-003, GAP-004, GAP-010, GAP-013, 16 SP total):
- ✅ **GAP-003** — Settlement batch job dengan reconciliation & discrepancy detection
- ✅ **GAP-004** — Rate card engine (flat, percentage, tiered pricing) per partner
- ✅ **GAP-010** — Multi-currency settlement dengan 15m FX rate locking
- ✅ **GAP-013** — Revenue share / royalty engine dengan stakeholder splits

**E-14 — Consumer Banking Experience COMPLETED (Feb 28):**

6 stories finished (IMP-034 to IMP-039, 12 SP total):
- ✅ **IMP-034** — Transaction memo & tags (JSONB storage)
- ✅ **IMP-035** — Beneficiary management (max 50/user)
- ✅ **IMP-036** — P2P transfer via phone lookup
- ✅ **IMP-037** — QR Pay P2P dengan checksum verification
- ✅ **IMP-038, IMP-039** — Savings goals dengan progress tracking

**E-03 — Frontend Quality COMPLETED (Feb 28):**

5 stories finished (IMP-004, IMP-010, IMP-011, IMP-014, IMP-015, 7 SP total):
- ✅ **IMP-004** — 429 rate limit handling dengan exponential backoff + toast notification
- ✅ **IMP-010** — FxService double-prefix bug fix
- ✅ **IMP-011** — Pocket type consolidation
- ✅ **IMP-014** — Duplicate type definitions removed
- ✅ **IMP-015** — Financial data moved dari URL query ke request body

**E-04 — API Management COMPLETED (Feb 28):**

3 stories finished (IMP-016, IMP-017, IMP-018, 9 SP total):
- ✅ **IMP-016** — Persistent analytics dengan Redis (90d retention)
- ✅ **IMP-017** — Rate plans per partner dengan per-endpoint overrides
- ✅ **IMP-018** — Request/response transformation filters

---

### v1.4.0 (Completed) — February 25-27, 2026

**E-15 — Payment Gateway Features (Feb 27):**

- ✅ **IMP-040 — Payment Link / Invoice** (3 SP) — `PaymentLink` entity with slug-based URLs, partner-scoped CRUD, public payer endpoint, auto-expire scheduler. 24 unit tests.
- ✅ **IMP-041 — Payment Method Selection API** (3 SP) — Catalog of 6 payment methods (wallet, VA, QRIS, bank transfer, credit card, PayLater) with eligibility, fees, settlement time.
- ✅ **IMP-042 — Virtual Account (VA) Payment** (5 SP) — VA lifecycle (PENDING→PAID/EXPIRED) with bank-prefixed number generation (BCA/BNI/Mandiri/Permata), bank callback, auto-expiry. 10 unit tests.
- ✅ **IMP-043 — Hosted Checkout Page** (5 SP) — Snap-style checkout with token generation, server-rendered HTML page, session cleanup scheduler.
- ✅ **IMP-044 — Payment Expiry & Auto-Cancel** (2 SP) — Centralized `PaymentExpiryScheduler` for transactions + VAs, `expiresAt` field on `Transaction` entity.
- ✅ **IMP-045 — Dynamic QR for Merchants** (5 SP) — Merchant onboarding + dynamic QRIS generation with payment confirmation flow. 10+ unit tests.
- ✅ **IMP-046 — Checkout Deeplink** (2 SP) — HMAC-SHA256 signed deeplinks (`payu://pay|topup|transfer`) with universal link fallback.
- 🔧 **E-15 Code Quality Fixes** — Fixed @Audited enum misuse, @Transactional(readOnly) write bug, duplicate scheduler, hardcoded HMAC secret, missing @Audited/@Idempotent on financial endpoints, auth on payer endpoint, redundant indexes.

**Epic Implementation (Feb 26):**

- ✅ **E-01 — Core Banking Ledger** (3 stories, 13 SP) — True double-entry ledger with `JournalEntry`/`LedgerEntry` domain models, Chart of Accounts (18 PSAK-based categories, 22 seed accounts), GL Engine with balance sheet, income statement, and daily settlement endpoints. 51 unit tests.
- ✅ **E-02 — Gateway Hardening** (5 stories, 11 SP) — Circuit breaker/retry with Resilience4j (`@CircuitBreaker`, `@Retry`, `@Bulkhead`), Redis-based sliding-window rate limiting, dynamic routing via config, request validation filter with body-size/SQL-injection/XSS checks, response PII masking filter for card/account/phone numbers.
- ✅ **E-20 — Code Health & Tech Hygiene** (8 stories, 10 SP) — Gateway query-param forwarding via `UriInfo`, Kafka config namespace fix, `open-in-view: false` across 12 services, removed in-memory `ConcurrentHashMap` reservation map (multi-pod unsafe), removed dead `CloudEventPublisher`, deduplicated `InsufficientFundsException`, `WalletEntity.tenantId` builder fix, `archunit-starter` added to reactor + 6 service POMs.
- ✅ **E-21 — Security Hardening** (2 stories, 5 SP) — `SecurityAutoConfiguration` fail-closed defaults: `masking-enabled` and `audit-enabled` now default to `true` (`matchIfMissing=true`), `encryption-enabled` stays opt-in. `AuditAspect.extractUserId()` now reads `SecurityContextHolder` (JWT) first, fallback to `X-User-Id` header, then `"anonymous"`. Removed `@Component` from `AuditAspect`/`AuditLogPublisher` (bean creation via auto-config only). Added SLF4J fallback when Kafka unavailable. 39 tests passing.

**Code Review Remediation:**

- ✅ **Production Readiness 99%** — Fixed 229 of ~232 bugs across all 22 microservices + frontend. 0 open, 3 intentionally skipped.
- ✅ **Core Financial Ledger** — Stabilized `wallet-service` and `investment-service` by handling data type parsing exceptions (`UUID` vs `String`) and enforcing saga compensations (`Try-Catch Rollbacks`) to maintain idempotent data flow.
- ✅ **Concurrency Resilience** — Replaced asynchronous Reactor `Mono.block()` with fully synchronous `RestTemplate` components targeting Tomcat thread starvation in auth processing. Hardened `ScheduledTransferScheduler` clearing runs with Redis distributed locks.
- ✅ **Data Integrity & Consistency** — Stopped lost point updates using optimistic locking & atomic sums in `promotion-service`. Status updates explicitly handle synchronous business validations (e.g. KYC approval/rejection constraint).
- ✅ **Biller Simulator** — Created `biller-simulator` (Quarkus 3.17.5) with 14 seeded test accounts (PLN, PDAM, Telco, E-wallet). Integrated via `BillerPort`/`BillerAdapter` hexagonal pattern in `billing-service`.
- ✅ **SMS Sender Refactor** — `SmsSender.java` refactored with configurable provider mode (`LOG`/`TWILIO`/`VONAGE`/`ZENZIVA`). LOG mode prints full OTP/message content to console for lab use.
- ✅ **Statement Historical Balance** — Fixed `statement-service` to compute historical balances by reversing post-period transactions from current balance.
- ✅ **API Contract Alignment** — Fixed `ScheduledTransferController` and `SplitBillController` response types (void→response object) and BFF whitelist routing.
- ✅ **Auth Test Coverage** — Added 9 comprehensive vitest tests for `useSilentRefresh` hook.
- ✅ **Containerfile Standardization** — Unified 27 Containerfiles, deleted 25 Dockerfiles. Fixed wrong ports (8001-8092 → 8080), added HeapDump, removed redundant HEALTHCHECK/VOLUME/curl. 86 files changed, -1764/+418 lines.
- ✅ **Logging-Starter Overhaul** — CRITICAL: added `container` profile to logback (fixes silent log loss on OpenShift). Added reactive WebFlux filters, Kafka MDC interceptors, configurable TraceIdFilter. 8 files changed, +305 lines.
- ✅ **RHSSO → RHBK Migration** — Upgraded to Red Hat Build of Keycloak v26.4.9. Realm imported, OIDC tokens verified.

### v1.3.0 — February 23, 2026

**Infrastructure:**

- ✅ **22/22 Services Running** — All services deployed and healthy on OpenShift
- ✅ **Auth Refresh Fixed** — Resolved 500 errors in refresh token endpoint (delegated to Keycloak)
- ✅ **OIDC Config & JPA Fixed** — Updated `OIDC_ISSUER` across core services, fixed auto-commit DB issues in `wallet-service`
- ✅ **High Availability** — 2 replicas + HPA + PDB for all critical services
- ✅ **HPA** — 12 HorizontalPodAutoscalers (CPU 70% target, min 1-2, max 3-5)
- ✅ **PDB** — 22 PodDisruptionBudgets (minAvailable: 1) for zero-downtime maintenance
- ✅ **4 Failed Services Recovered** — `billing-service`, `investment-service`, `promotion-service`, `statement-service`
- ✅ **Rate Limiting Enhanced** — Best practices: auth 30/min, burst 50
- ✅ **Keycloak User Seeder** — `scripts/keycloak-seeder.sh` with test users (customer1, customer2, admin)
- ✅ **Image Registry** — defaultRoute enabled, all images tagged `1.3.0` pushed
- ✅ **Login Fixed** — Invalid credentials resolved, `payu-backend` client configured

### v1.2.0 — February 20, 2026

**Initial OpenShift Deployment:**

- ✅ **OpenShift Deployed** — 22 services + web-app on OCP 4.20+ (`payu-dev` namespace)
- ✅ **Infrastructure via Operators** — Crunchy PGO, AMQ Streams (KRaft), DataGrid, RHBK, Vault, cert-manager
- ✅ **Kustomize IaC** — Complete manifests (`operators/` + `infra/` + `overlays/`) for reproducible deployments
- ✅ **TLS** — Let's Encrypt certs via cert-manager DNS01/Route53
- ✅ **Images Built** — All 22 services via Podman, pushed to OCP internal registry (`tag 1.3.0` for web-app)
- ✅ **NetworkPolicies Simplified** — Removed 7 custom policies, kept only Kafka operator policies
- ✅ **Keycloak Realm Imported** — `payu` realm: 4 clients, 5 roles, 4 users, E2E login verified
- ✅ **PostgreSQL Connection Fix** — Workaround for connection exhaustion (scale down/up pattern)
- ✅ **Web-App v1.3.0** — TypeScript errors fixed, `sonner`/`radix-ui` deps added, Transaction types aligned
- 🟢 **Status** — Running: 36/36 pods, 22 services + infra

---

## ✅ Major Completed Tech Debt Items (19/19 Closed)

> Previously tracked as P0-P3 blockers, all resolved prior to Feb 20 deployment.

| #     | Item                                        | Resolution                                |
| :---- | :------------------------------------------ | :---------------------------------------- |
| 1     | Gateway JWT Validation (BUG-BE-001)         | ✅ Done — Fixed with `nimbus-jose-jwt`    |
| 2     | Auth in-memory state                        | ✅ Done — Fully moved to Redis            |
| 3     | Transaction reference number collision      | ✅ Done — Migrated to UUID generation     |
| 4     | Wallet cache invalidation                   | ✅ Done — Exhaustive key eviction applied |
| 5     | HPA + PDB enabled                           | ✅ Done — All 22 services                 |
| 6     | Keycloak realm configured                   | ✅ Done — `payu` realm live               |
| 7     | E2E test suite                              | ✅ Done — 399/399 passing                 |
| 8     | TLS certificates                            | ✅ Done — cert-manager + Let's Encrypt    |
| 9     | Image registry                              | ✅ Done — All images pushed `v1.3.0`      |
| 10–19 | Infrastructure (PGO, KRaft, DataGrid, etc.) | ✅ Done — All operators running           |

> Items 1-4 were marked complete but code review (Feb 24) found underlying issues still present.
> They have been re-opened and documented in `TODOS.md`.

---

## 🌐 Infrastructure Topology

```
Internet → Route → NGINX Ingress → OpenShift Service → gateway-service (Quarkus)
                                                      → auth-service (Spring Boot)
                                                      → [22 microservices]

Data Layer:
  PostgreSQL (Crunchy PGO): 22 databases (1 per service)
  Redis (DataGrid RESP): cache + session + rate-limit
  Kafka (AMQ Streams KRaft): event streaming
  Keycloak (RHBK): identity & access management
  Vault: secret management
```

---

## 📊 Test Coverage Summary

| Layer        | Framework         | Status                               |
| :----------- | :---------------- | :----------------------------------- |
| E2E          | Playwright        | ✅ 399/399                           |
| Performance  | Gatling           | ✅ Configured                        |
| Contract     | Pact              | ✅ Configured                        |
| Integration  | Testcontainers    | ✅ Per service                       |
| Architecture | ArchUnit          | ✅ 18/19 services                    |
| Unit         | JUnit 5 + Mockito | Varies (see TODOS for coverage gaps) |
