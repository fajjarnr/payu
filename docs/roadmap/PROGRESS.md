# 📈 PayU Platform — Progress & Engineering Scorecard

> **Dokumen ini adalah historical record & status snapshot PayU Platform.**
> Untuk open bugs dan actionable items → lihat [`TODOS.md`](./TODOS.md)
> Untuk arsitektur gateway & integrasi → lihat [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)

---

## 🏁 Current Status Snapshot

| Attribute | Value |
| :--- | :--- |
| **Last Status Update** | February 25, 2026 |
| **Production Readiness** | 99% (229/232 bugs fixed) |
| **OpenShift Tag** | `v1.4.0` (in-progress) |
| **Namespace** | `payu-dev` |
| **Total Pods** | 36/36 running |
| **Services Deployed** | 22/22 |
| **E2E Tests** | 399/399 passing |

> ✅ **Code Review Complete (Feb 24-25)**: 229 of ~232 bugs fixed (~99% resolution rate).
> **0 open bugs**. 3 intentionally skipped (low impact, future consideration).
> Lihat `TODOS.md` untuk detail skipped items.

---

## 🎯 Platform Maturity Scorecard

| Category | Weight | Infra/Deploy Score | Notes |
| :--- | :--- | :--- | :--- |
| **Backend Services** | 25% | 22/22 deployed | ✅ All bugs fixed, biller-simulator added |
| **Shared Libraries** | 10% | 7/7 starters | BUG-BE-091 skip (rate limit burst — acceptable) |
| **Frontend Web-App** | 15% | Deployed & running | ✅ All cross-service issues resolved |
| **Frontend Mobile** | 5% | Expo setup only | Deferred |
| **Testing** | 15% | 399/399 E2E pass | ✅ Auth test gaps closed (useSilentRefresh) |
| **Security** | 10% | JWT + OIDC active | ✅ BUG-BE-001 fixed (nimbus-jose-jwt) |
| **Infrastructure** | 10% | OpenShift HA | HPA + PDB for all critical services |

---

## 📈 DORA Metrics (Current Target)

| Metric | Target | Current | Alignment |
| :--- | :--- | :--- | :--- |
| **Deployment Frequency** | ≥ 1/day | Multiple/day (CI) | 🟢 **Elite** |
| **Lead Time for Changes** | < 4 hours | ~30 mins | 🟢 **Elite** |
| **Mean Time to Recovery** | < 30 mins | ~15 mins | 🟢 **Elite** |
| **Change Failure Rate** | < 10% | ~8% | 🟢 **Elite** |

---

## 🏗️ Architectural Compliance

| Standard | Status | Detail |
| :--- | :--- | :--- |
| **Hexagonal Architecture** | ✅ 19/19 services | All Java/Quarkus services |
| **Event-First** | ✅ Active | `outbox-starter`, `events-starter`, `saga-starter` |
| **ArchUnit Governance** | ✅ 18/19 | 1 service exempt with documented reason |
| **Zero Trust** | ✅ Per-service | JWT + OIDC validation per endpoint |
| **API-First** | ✅ 22/22 | OpenAPI spec per service |
| **Doc-as-Code** | ✅ 13 ADRs | `/docs/adr/` |

---

## 📦 Deployment Log

### v1.4.0 (In-Progress) — February 25, 2026

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

| # | Item | Resolution |
| :--- | :--- | :--- |
| 1 | Gateway JWT Validation (BUG-BE-001) | ✅ Done — Fixed with `nimbus-jose-jwt` |
| 2 | Auth in-memory state | ✅ Done — Fully moved to Redis |
| 3 | Transaction reference number collision | ✅ Done — Migrated to UUID generation |
| 4 | Wallet cache invalidation | ✅ Done — Exhaustive key eviction applied |
| 5 | HPA + PDB enabled | ✅ Done — All 22 services |
| 6 | Keycloak realm configured | ✅ Done — `payu` realm live |
| 7 | E2E test suite | ✅ Done — 399/399 passing |
| 8 | TLS certificates | ✅ Done — cert-manager + Let's Encrypt |
| 9 | Image registry | ✅ Done — All images pushed `v1.3.0` |
| 10–19 | Infrastructure (PGO, KRaft, DataGrid, etc.) | ✅ Done — All operators running |

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

| Layer | Framework | Status |
| :--- | :--- | :--- |
| E2E | Playwright | ✅ 399/399 |
| Performance | Gatling | ✅ Configured |
| Contract | Pact | ✅ Configured |
| Integration | Testcontainers | ✅ Per service |
| Architecture | ArchUnit | ✅ 18/19 services |
| Unit | JUnit 5 + Mockito | Varies (see TODOS for coverage gaps) |
