# 🧠 PayU Lessons Learned & Implementation Patterns

This document serves as a high-level index for the "Lessons Learned" and historical implementation patterns discovered during the PayU platform development.

To ensure maintenance and specialized access, all detailed patterns have been migrated to the **AI Agent Skill Ecosystem** in `.agent/skills/`.

## 📂 Pattern Directory

| Domain                    | Reference Document                                                                                        | Primary Skill             |
| :------------------------ | :-------------------------------------------------------------------------------------------------------- | :------------------------ |
| **Infrastructure & Ops**  | [INFRASTRUCTURE_PATTERNS.md](../../.agent/skills/platform-engineer/references/INFRASTRUCTURE_PATTERNS.md) | `platform-engineer`       |
| **Deployment & Release**  | [DEPLOYMENT_PATTERNS.md](../../.agent/skills/platform-engineer/references/DEPLOYMENT_PATTERNS.md)         | `platform-engineer`       |
| **Backend & JPA**         | [BACKEND_PATTERNS.md](../../.agent/skills/core-banking-engineer/references/BACKEND_PATTERNS.md)           | `core-banking-engineer`   |
| **Security & IAM**        | [SECURITY_PATTERNS.md](../../.agent/skills/cybersecurity-architect/references/SECURITY_PATTERNS.md)       | `cybersecurity-architect` |
| **API Standards**         | [API_STANDARDS.md](../../.agent/skills/api-architect/references/API_STANDARDS.md)                         | `api-architect`           |
| **Integration & Events**  | [EVENT_DRIVEN_PATTERNS.md](../../.agent/skills/integration-architect/references/EVENT_DRIVEN_PATTERNS.md) | `integration-architect`   |
| **Frontend Architecture** | [FRONTEND_PATTERNS.md](../../.agent/skills/frontend-architect/references/FRONTEND_PATTERNS.md)            | `frontend-architect`      |
| **Design System**         | [DESIGN_SYSTEM_PATTERNS.md](../../.agent/skills/product-designer/references/DESIGN_SYSTEM_PATTERNS.md)    | `product-designer`        |
| **Testing & Quality**     | [TESTING_PATTERNS.md](../../.agent/skills/quality-engineer/references/TESTING_PATTERNS.md)                | `quality-engineer`        |

## 🧩 Lessons Learned (Session Log)

### L-001: Python ML/AI Services — Stay on Debian Slim, Not UBI9

**Date**: February 26, 2026 | **Severity**: High | **Domain**: Platform

UBI9 `python-312` has known compatibility issues with native ML/AI dependencies:

- PaddleOCR, OpenCV, PyTorch — prebuilt wheels expect Debian/glibc paths
- Missing shared libraries (`libGL`, `libglib`, `libgomp`) require different package names on RHEL
- `site-packages` path differs (`/opt/app-root/lib/` vs `/usr/local/lib/`)

**Decision**: Keep `python:3.12-slim` for `kyc-service` and `analytics-service`. All Java (UBI9 OpenJDK 21) and Node.js (UBI9 Node 20) services use UBI9.

**Rule**: Do not migrate Python ML services to UBI9 without full dependency compatibility testing first.

---

### L-002: Domain Routing Strategy — Gateway API + Istio Ingress (Updated)

**Date**: February 26, 2026 (Updated) | **Severity**: Critical | **Domain**: Infrastructure

**Dual-ingress architecture** separating application traffic from platform traffic:

| Traffic Type      | Ingress Controller               | Domain Pattern                                         | Example                                                        |
| :---------------- | :------------------------------- | :----------------------------------------------------- | :------------------------------------------------------------- |
| **App (Prod)**    | Istio Ingress Gateway            | `payu.fajjjar.my.id` + `*.payu.fajjjar.my.id`          | `api.payu.fajjjar.my.id`, `sso.payu.fajjjar.my.id`             |
| **App (Dev)**     | Istio Ingress Gateway            | `*.dev.payu.fajjjar.my.id`                             | `api.dev.payu.fajjjar.my.id`, `gateway.dev.payu.fajjjar.my.id` |
| **App (Staging)** | Istio Ingress Gateway            | `*.staging.payu.fajjjar.my.id`                         | `api.staging.payu.fajjjar.my.id`                               |
| **App (SIT/UAT)** | Istio Ingress Gateway            | `*.sit.payu.fajjjar.my.id`, `*.uat.payu.fajjjar.my.id` | `api.sit.payu.fajjjar.my.id`                                   |
| **OCP Platform**  | OCP Ingress Controller (HAProxy) | `*.apps.payu.ocp.fajjjar.my.id`                        | `console-openshift-console.apps.payu.ocp.fajjjar.my.id`        |
| **OCP API**       | Kubernetes API                   | `api.payu.ocp.fajjjar.my.id`                           | —                                                              |

**Rule**: ALL environments use Istio Ingress Gateway for application traffic. `*.apps.payu.ocp.*` is exclusively for OCP platform components (console, image registry, ArgoCD, Grafana).

**Gotcha**: Beware of `apps.cluster.payu` vs `apps.payu.ocp` inconsistency — standardize early. Always replace most-specific patterns first during migration.

---

### L-003: Domain Migration — Scope & Safe Replacement

**Date**: February 26, 2026 | **Severity**: High | **Domain**: DevOps

When doing bulk domain replacement across a monorepo (156 files, ~400 matches):

1. **Order matters**: Replace most-specific patterns first (`staging-api.payu.id` before `payu.id`)
2. **Preserve intentionally different domains**: `payu.local` (mesh trust), `payu.internal` (internal DNS), `payu.test` (test data), Java packages (`id.payu.*`)
3. **Java code is mostly unaffected**: Domain references in Java are OpenAPI metadata and CORS — both overridden by OpenShift configmaps at deploy time
4. **Always verify with negative grep**: After replacement, confirm zero stray references remain

**Regex used**: `sed 's/payu\.id/payu.fajjjar.my.id/g'` — safe because `id.payu` (Java packages) doesn't match `payu.id`

---

### L-004: Container Image Pinning

**Date**: February 26, 2026 | **Severity**: Medium | **Domain**: Platform

Never use `:latest` in compose files or Quadlet containers. Pin to specific versions:

| Image       | Before         | After          |
| :---------- | :------------- | :------------- |
| Keycloak    | `:latest`      | `:26.1`        |
| kafka-ui    | `:latest`      | `:v0.7.2`      |
| timescaledb | `:latest-pg16` | `:2.17.2-pg16` |
| rustfs      | `:latest`      | `:0.3.0`       |

**Rule**: Every image reference must have an explicit version tag for reproducibility.

---

### L-005: Backstage catalog-info.yaml — Single Root File

**Date**: February 26, 2026 | **Severity**: Medium | **Domain**: Developer Hub

For a monorepo with 22+ services, use a single root `catalog-info.yaml` with YAML multi-document (`---`) separators rather than per-service files. Benefits:

- Single import point in Backstage/RHDH
- Easier to maintain dependency graph (`dependsOn`, `providesApis`)
- System-level view of all components in one place

Include: Components (services, libraries, websites), Resources (databases, message brokers, caches), and System definition.

---

### L-006: OSS Version Compatibility Matrix

**Date**: February 26, 2026 | **Severity**: Medium | **Domain**: Architecture

Maintain a compatibility matrix between Red Hat products and OSS equivalents. Key validated mappings:

| Red Hat Product  | OSS Equivalent | PayU Version | Compatible |
| :--------------- | :------------- | :----------- | :--------- |
| Red Hat Runtimes | Spring Boot    | 3.4.1        | ✅         |
| RHBQ             | Quarkus        | 3.17.5       | ✅         |
| Crunchy PGO      | PostgreSQL     | 16           | ✅         |
| AMQ Streams      | Apache Kafka   | 3.5 (CP 7.5) | ✅         |
| RHBK             | Keycloak       | 26.1         | ✅         |
| Data Grid (RESP) | Redis          | 7.x          | ✅         |
| RHDH             | Backstage.io   | 1.25+        | ✅         |

**Rule**: Verify wire compatibility when client/broker versions differ (e.g., Kafka client 3.8 ↔ broker 3.5 is safe).

---

### L-007: Istio Ingress Gateway — Router Node Placement & Dual LoadBalancer VIP

**Date**: February 26, 2026 | **Severity**: High | **Domain**: Infrastructure / Service Mesh

When running both OCP Ingress Controller and Istio Ingress Gateway on the same cluster with dedicated router nodes:

**Architecture**:

- 3 router nodes with taint `node-role.kubernetes.io/router:NoSchedule`
- OCP Ingress Controller pods (HAProxy) → already scheduled on router nodes by OpenShift
- Istio Ingress Gateway pods → must explicitly opt-in with `nodeSelector` + `tolerations`

**Configuration**:

```yaml
nodeSelector:
  node-role.kubernetes.io/router: ""
tolerations:
  - key: node-role.kubernetes.io/router
    operator: Exists
    effect: NoSchedule
podAntiAffinity:
  requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        matchLabels:
          app: istio-ingressgateway
      topologyKey: kubernetes.io/hostname
```

**Dual LoadBalancer VIP separation**:
| Component | Ports | DNS Target |
| :--------------------------- | :-------- | :------------------------------ |
| OCP Ingress Controller (HAProxy) | 80, 443 | `*.apps.payu.ocp.fajjjar.my.id` |
| Istio Ingress Gateway | 8080, 8443 | `*.payu.fajjjar.my.id` + env wildcards |

**Rule**: Use separate LB VIPs with different ports (80/443 vs 8080/8443). Both can coexist on the same router nodes because they bind different ports. Set `replicas: 3` and HPA `minReplicas: 3` (one per router node).

---

### L-008: Code Health Anti-Patterns in Multi-Pod Microservices

**Date**: February 26, 2026 | **Severity**: High | **Domain**: Backend / Architecture

Three critical anti-patterns discovered during E-20 Code Health & Tech Hygiene epic:

**1. In-Memory State (ConcurrentHashMap) in Stateless Services**
`WalletServiceAdapter` used a `ConcurrentHashMap<String, ReservationInfo>` to store reservation data between `reserveBalance()` and `commitBalance()` calls. This fails catastrophically in multi-pod deployments because the commit call may hit a different pod than the reserve call. **Fix**: Pass `reservationId` through method signatures; the saga context (persisted in DB as JSONB) already had this field.

**2. Spring Boot Config Namespace Gotcha**
`transaction-service/application.yml` had a top-level `kafka:` block. Spring Boot silently ignores this — the correct path is `spring.kafka.*`. No error, no warning, just silent misconfiguration. **Rule**: Always verify config properties are under the correct Spring namespace. Use `@ConfigurationProperties` binding validation.

**3. `.gitignore` Pattern Matching `port/out/` Directories**
A root `.gitignore` entry `out/` matched any path containing `out/`, including valid hexagonal architecture paths like `domain/port/out/AccountServicePort.java`. Required `git add -f` to force-add. **Rule**: Use more specific patterns like `/out/` (root only) instead of `out/` (recursive match).

**Bonus**: `spring.jpa.open-in-view` defaults to `true` in Spring Boot, which keeps DB sessions open during HTTP response rendering — an anti-pattern that causes lazy-loading surprises and connection pool exhaustion. Always set `spring.jpa.open-in-view: false` explicitly.

---

## 🚀 How to use these patterns

1. **AI Agents**: Should read the `SKILL.md` of their respective domain. The reference documents are explicitly linked in the "Reference Implementation Patterns" section of the skill.
2. **Human Developers**: Can access the patterns directly via the links above or by navigating the `.agent/skills/` directory.
3. **Session Lessons** (L-001+): Captured from live development sessions. Review before starting related work.

---

### L-009: Payment Gateway Implementation — Webhook Delivery Patterns

**Date**: February 28, 2026 | **Severity**: High | **Domain**: Backend / Gateway

Lessons from implementing E-15 Payment Gateway Features (7 stories, 25 SP):

**1. VA Simulator Architecture**

- External bank simulators should be **deterministic** — same VA number + amount = same response
- Use **fixed prefixes per bank** (BCA: 12345, BNI: 67890) untuk memudahkan testing
- Quarkus Native ideal untuk simulators: sub-second startup, low memory footprint

**2. Payment Link Webhook Reliability**

- **HMAC-SHA256 signing** wajib untuk webhook payload integrity
- Implement **exponential backoff retry** (3x) dengan jitter untuk failed deliveries
- Store webhook delivery attempts di DB untuk audit trail

**3. Scheduler-Based Expiry Pattern**

- Gunakan **single centralized scheduler** (`PaymentExpiryScheduler`) daripada multiple schedulers per payment type
- Implement **optimistic locking** pada status updates untuk prevent race conditions
- Release reserved balance **sebelum** publish Kafka event untuk maintain consistency

**4. Mobile Deeplink Security**

- **Signed URLs dengan expiry** — jangan trust client-side params
- Support **universal links** (iOS) dan **app links** (Android) sebagai fallback
- Expo Linking + React Native Hooks pattern untuk clean separation

---

### L-010: Settlement & Revenue Share — Financial Engine Patterns

**Date**: February 28, 2026 | **Severity**: High | **Domain**: Backend / FinOps

Lessons from implementing E-12 Settlement & Financial Operations:

**1. Rate Card Engine Design**

- Support **3 fee types**: FLAT (fixed amount), PERCENTAGE (of transaction), TIERED (volume-based brackets)
- **Min/max caps** essential untuk percentage fees (prevent Rp100 juta fee on Rp1M transaction)
- Link: Partner → Rate Card (1:1 untuk simplicity, 1:N jika complex pricing tiers)

**2. Settlement State Machine**

- PENDING → PROCESSING → COMPLETED/FAILED/OVERRIDDEN
- **Never delete** settlement batches — soft delete dengan status untuk audit
- Manual override capability dengan **dual-authorization** untuk amount > threshold

**3. Revenue Split Calculation**

- **Priority-based stakeholder ordering** — primary stakeholder dapat payout pertama
- Handle **remaining amount** (rounding errors) — assign ke platform atau distribute proportional
- **Monthly royalty statements** auto-generated dengan breakdown per transaction

**4. Multi-Currency Settlement**

- **FX rate locking window** (15 menit) — prevent rate fluctuation during settlement processing
- Partner currency preference per settlement batch
- Auto-conversion hanya pada settlement time, bukan saat transaction

---

### L-011: .gitignore `out/` vs Hexagonal `port/out/` — 26 Missing Port Interfaces

**Date**: March 14, 2026 | **Severity**: Critical | **Domain**: Backend / Architecture

A root `.gitignore` entry `out/` (intended for Next.js build output) silently matched all `port/out/` directories in the Hexagonal Architecture, preventing 26 port interface files across 10 services from being committed to git.

**Symptoms**:

- Build passes locally (files exist on disk) but fails on fresh clone
- `mvn clean package` fails with "cannot find symbol" errors on port interfaces
- `git status` shows nothing to commit despite files existing

**Root Cause**: Git's `out/` pattern matches _any_ path containing `/out/` — including `domain/port/out/AccountServicePort.java`.

**Affected Services** (26 files total):

- account-service (1), billing-service (6), dispute-service (2), fx-service (1), integration-service (1), lending-service (1), product-catalog-service (1), promotion-service (6), statement-service (1), transaction-service (2), wallet-service (4)

**Fix Applied**:

```gitignore
out/
# Negation: preserve Hexagonal Architecture port directories
!**/port/out/
!**/port/output/
```

**Rules**:

1. Use `/out/` (root-only) instead of `out/` (recursive) when targeting build output directories
2. After adding gitignore rules, always verify with `git ls-files --others --ignored --exclude-standard | grep port` that no source files are excluded
3. When build fails with missing port interfaces on CI but works locally, check `.gitignore` first

---

### L-012: Kafka Deserialization — Class Mismatch in Microservices

**Date**: March 15, 2026 | **Severity**: High | **Domain**: Integration / Messaging

When sharing events via Kafka between microservices with different package structures (e.g., `fx-service` vs `wallet-service`), the default Jackson deserializer will fail due to `ClassNotFoundException` because the fully qualified class name in the message header doesn't exist in the consumer.

**Symptoms**:
- `RecordDeserializationException` in Kafka consumer logs
- `ClassNotFoundException` for the event class

**Fix**: Use `spring.json.type.mapping` in `application.yml` to map the producer's class string to the consumer's local class.

```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.json.type.mapping: 'id.payu.fx.adapter.messaging.FxRatesUpdatedEvent:id.payu.wallet.adapter.messaging.fx.FxRatesUpdatedEvent'
```

**Rule**: Always use explicit type mapping or shared event libraries with identical package names for cross-service Kafka events.

---

### L-013: Saga Starter Infrastructure — Missing `saga_instances` Table

**Date**: March 15, 2026 | **Severity**: Critical | **Domain**: Backend / Saga

Services including the `saga-starter` module automatically initialize `SagaRecoveryService`, which requires a local `saga_instances` table for state persistence. If this table is missing from a service's database, the application will fail to start or crash during recovery cycles.

**Symptoms**:
- Hibernate error: `Relation "saga_instances" does not exist`
- Service health check failing or 503 errors on dependent endpoints

**Fix**: Ensure every microservice using `saga-starter` has a Flyway migration creating the `saga_instances` table.

**Rule**: The `saga_instances` table schema must remain consistent across all services to ensure compatibility with the shared `saga-starter` entity mappings.

---

### L-014: Podman Local Infrastructure — Storage Management

**Date**: March 15, 2026 | **Severity**: Medium | **Domain**: Platform / DevOps

Local development with 22+ microservices, Postgres, Kafka, and large ML-based images (OCR, Analytics) rapidly consumes disk space, leading to "No space left on device" errors in both the host and container volumes (Postgres stats, Maven repo).

**Symptoms**:
- `mvn` build failures during artifact download
- Postgres failing to write temp files
- `podman-compose up` failing to pull or build images

**Recommended Cleanup Ritual**:
1. `podman system prune -f` (removes unused containers/networks)
2. `podman builder prune -f` (cleans build cache)
3. `rm -rf ~/.m2/repository` (if repo is corrupted or too large)
4. `rm -rf /tmp/*` (cleans temporary build artifacts)

**Rule**: Monitor disk space with `df -h` and keep at least 10GB free for stable local multi-service orchestration.

---

### L-015: IDOR Vulnerability Pattern — Duplicated `extractUserId()` Across Controllers

**Date**: March 16, 2026 | **Severity**: Critical | **Domain**: Backend / Security

Phase 3 closed 3 P0 IDOR bugs (BUG-BE-148, BUG-BE-149, BUG-BE-150) that all shared the same root cause: controllers accessing user-scoped resources without verifying ownership via the JWT subject claim.

**The Pattern**: Every affected controller needed an `extractUserId()` method:

```java
private String extractUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
        throw new IllegalStateException("No valid JWT authentication found");
    }
    Jwt jwt = (Jwt) auth.getPrincipal();
    return jwt.getSubject();
}
```

This was copy-pasted into `ScheduledTransferController`, `SplitBillController`, `WalletController`, `TransactionController`, and `PaymentController`. Each has slightly different error handling (some throw, some return null), creating a consistency hazard.

**Three Sub-Patterns Emerged**:
1. **Direct comparison**: `if (!accountId.equals(userId)) throw AccessDeniedException`
2. **Fetch-then-compare**: Load resource, check `response.getSenderAccountId().equals(userId)`
3. **Dedicated security service**: `SplitBillSecurityService.isOwner(id, userId)` — best pattern for complex ownership rules

**Rule**: Every controller endpoint accessing user-scoped resources MUST verify ownership via JWT subject BEFORE any data retrieval or mutation. Extract a shared `SecurityContextUtils.extractAuthenticatedUserId()` into `security-starter` to prevent copy-paste divergence. The method must throw `AccessDeniedException`, never return null.

---

### L-016: BFF Path Whitelist — Silent 400 on New Backend Routes

**Date**: March 16, 2026 | **Severity**: High | **Domain**: Frontend / BFF

BUG-FE-047 revealed that the BFF proxy (`frontend/web-app/src/app/api/v1/[...path]/route.ts`) uses an explicit `ALLOWED_PATH_PREFIXES` array. When backend services add new API paths, the BFF silently returns 400 because the new prefix isn't whitelisted.

**Two-Layer SSRF Defense**:
1. **Path sanitization**: Per-segment validation rejects `..`, control chars, encoded traversals
2. **Prefix whitelist**: `fullPath.startsWith(prefix + '/')` — the trailing `/` prevents `/api/v1/accountsEvil` from matching `/api/v1/accounts`

**Gotcha**: The whitelist must be manually updated whenever a new service path is added. Phase 3 added 6 missing prefixes (`cards`, `pockets`, `payments`, `topup`, `billers`, `biometric`).

**Rule**: When adding a new backend API path prefix, ALWAYS add the corresponding prefix to `ALLOWED_PATH_PREFIXES` in the BFF route handler. Treat the BFF whitelist as a mandatory checklist item when onboarding a new service or domain. Validate using `startsWith(prefix + '/')` with trailing slash to prevent prefix overlap attacks.

---

### L-017: i18n Middleware — Locale Detection, Route Guarding, and Single Source of Truth

**Date**: March 16, 2026 | **Severity**: High | **Domain**: Frontend / i18n

Phase 3 closed 8 i18n bugs. Three key architectural decisions:

**1. Config-driven locale pattern** — Build regex dynamically from config:
```typescript
const localePattern = new RegExp(`^/(${locales.join('|')})`);
```
Source of truth: `i18n/config.ts` (single file, imported everywhere).

**2. Disable automatic locale detection** — `localeDetection: false` in middleware. Auto-detection via `Accept-Language` was causing Indonesian banking app users with English browser locale to be redirected to `/en/dashboard` unexpectedly.

**3. Segment-boundary route matching** — The original `publicRoutes.includes(path)` allowed `/login-debug` to bypass auth because it started with `/login`. Fixed to:
```typescript
pathWithoutLocale === route || pathWithoutLocale.startsWith(route + '/')
```

**4. Locale-aware navigation** — All route navigation must use `Link`, `useRouter`, `redirect` from `@/lib/navigation` (which wraps `next-intl/navigation`), never raw `next/navigation`. BUG-I18N-002 through BUG-I18N-007 were all unprefixed routes.

**Rule**: (a) Define `locales` and `defaultLocale` in exactly one file and import everywhere. (b) Disable `localeDetection` when the default locale is contextually obvious. (c) Always use segment-boundary matching (`=== route` or `startsWith(route + '/')`) for route access control. (d) Import navigation primitives exclusively from `@/lib/navigation`, never from `next/navigation` directly.

---

### L-018: Gateway Idempotency — Shared `@Idempotent` Annotation with Redis Lua Locking

**Date**: March 16, 2026 | **Severity**: High | **Domain**: Backend / Gateway

Phase 2 implemented GAP-006 (global idempotency) as a shared starter in `api-commons`.

**Architecture (5 layers)**:
1. `@Idempotent(required = true)` annotation on mutation endpoints — returns 400 if `X-Idempotency-Key` header missing
2. `IdempotencyInterceptor` (Spring MVC `HandlerInterceptor`) — auto-registered via `IdempotencyAutoConfiguration`
3. `IdempotencyService` — SHA-256 fingerprints request body, detects key reuse with different payloads (`ConflictException: IDEMPOTENCY_KEY_REUSE`)
4. `RedisIdempotencyRepository` — atomic Lua script for concurrent duplicate detection (`SETEX if not EXISTS`)
5. State machine: `IN_PROGRESS` → `COMPLETED` / `FAILED` with 24-hour TTL

**Key Design**: The fingerprint check catches accidental key reuse (different requests with same key), not just exact duplicates.

**Known Gap**: The `ContentCachingResponseWrapper` in `storeSuccessfulResponse()` is a placeholder with a no-op `getContentAsByteArray()`. Successful responses are never actually cached — idempotency only works for error paths currently. This needs to be replaced with Spring's actual `ContentCachingResponseWrapper`.

**Rule**: Use `@Idempotent(required = true)` on ALL payment/transfer/mutation POST endpoints. The key must be UUID v4. Use Redis Lua scripts for atomic lock acquisition. CRITICAL: Replace the placeholder response wrapper to cache successful responses, otherwise duplicate POSTs get processed twice.

---

### L-019: E2E Test Resilience — Separating Infrastructure Failures from Business Logic Failures

**Date**: March 16, 2026 | **Severity**: High | **Domain**: Testing / Quality

Phase 1 achieved 100% E2E pass rate (703 tests). Key patterns:

**Pytest (Backend E2E)**:
- `X-E2E-Test: true` header enables rate limit bypass server-side
- Rate-limited responses (429/503) trigger `pytest.skip()`, not assertion failures
- Login fixtures skip when auth service returns 401/502/503

**Playwright (Frontend E2E)**:
- Mock auth cookies (`accessToken`, `payu_session`) injected via `BrowserContext.addCookies()` to bypass middleware
- `safeClick()` utility retries DOM interactions up to 3 times with 500ms backoff
- `waitForPageStable()` helper prevents assertions against loading states

**Critical Gotcha**: Accepting 500 alongside 200 in the same assertion (`in [200, 201, 400, 429, 500, 503]`) hides real bugs. A test that passes when the service returns 500 provides false confidence. The correct approach:
- **Skip** on infra issues (429, 502, 503, 504) — service isn't available for testing
- **Fail** on business logic errors (500, unexpected 400) — something is genuinely broken
- **Pass** on expected statuses (200, 201, 204)

**Rule**: (a) Send `X-E2E-Test: true` to bypass rate limiting; the gateway MUST reject this header in production. (b) Use `pytest.skip()` for infra unavailability (429/503), not assertion tolerance. (c) Never lump 500 and 200 in the same assertion — separate "infra down" from "logic broken". (d) Frontend E2E should use dedicated auth fixtures aligned with middleware cookie names.

---

### L-020: SilentRefreshProvider — Every Authenticated Route Needs Token Refresh

**Date**: March 16, 2026 | **Severity**: High | **Domain**: Frontend / Auth

BUG-FE-053 revealed that `SilentRefreshProvider` was only mounted in `dashboard/layout.tsx`. Users navigating directly to `/transfer` or `/settings` (not through dashboard) had no silent refresh running, causing mid-session 401 errors.

**The Fix**: Wrap every authenticated route layout in `<SilentRefreshProvider>`:
- `app/[locale]/dashboard/layout.tsx` (original)
- `app/[locale]/transfer/layout.tsx` (Phase 3)
- `app/[locale]/settings/layout.tsx` (Phase 3)
- `app/[locale]/exchange/layout.tsx` (Phase 3)

**Four Defensive Measures in `useSilentRefresh()`**:
1. **Concurrency lock** (`isRefreshingRef`) — prevents parallel refresh calls
2. **Ref mirror for reactive state** — `setTimeout` captures stale closure values; `useRef` always reads current `isAuthenticated`
3. **Immediate refresh on mount** — if `tokenExpiresAt === null`, refresh immediately rather than waiting
4. **Exponential backoff** — 2s, 4s, 8s, 16s, 32s, max 5 retries on failure

**Stale Closure Gotcha**: `setTimeout` captures `isAuthenticated` from the render when the timer was set. If the user logs out before the callback fires, the captured value still says `true`. The `useRef` mirror solves this:
```typescript
const isAuthenticatedRef = useRef(isAuthenticated);
useEffect(() => { isAuthenticatedRef.current = isAuthenticated; }, [isAuthenticated]);
```

**Rule**: (a) Wrap EVERY authenticated route layout in `<SilentRefreshProvider>`. (b) In hooks using `setTimeout`/`setInterval`, store reactive state in a `useRef` mirror — never read Zustand/React state directly inside timer callbacks. (c) Implement a concurrency lock to prevent parallel refresh calls. (d) Use exponential backoff with a retry cap (5 max) for refresh failures.

---

### L-021: Backlog Hygiene — Bug Count Integrity and Document Routing

**Date**: March 16, 2026 | **Severity**: Medium | **Domain**: Process / Documentation

Phase 4 backlog hygiene uncovered a data integrity issue: the bug scorecard said "12 open" but the actual bug table had **19** entries. Frontend Logic was undercounted (4 vs actual 5 bugs: BUG-FE-060 through BUG-FE-064).

**Root Cause**: The scorecard was written from memory/estimation rather than counted from the actual table data. Multiple doc locations (scorecard, metrics, footer, CHANGELOG, PROGRESS) each had independent counts that diverged.

**Document Routing Rules** (from AGENTS.md, reinforced by this experience):
| Content | Target File |
| :--- | :--- |
| Bug backlog, open items, actionable todos | `docs/roadmap/TODOS.md` |
| Deployment status, completed milestones | `docs/roadmap/PROGRESS.md` |
| Architecture decisions, gap analysis | `docs/roadmap/GATEWAY_ARCH.md` |
| Version changelog, archived closed items | `CHANGELOG.md` |
| Implementation patterns, lessons learned | `docs/guides/LESSONS.md` |

**TODOS.md Convention**: Only contains items NOT yet done. Completed items get archived to `CHANGELOG.md`. Won't Do items get archived with rationale.

**Rule**: (a) Always count bug entries from the actual table data, never from memory. (b) When a count appears in multiple locations (scorecard, metrics, footer), update ALL of them atomically. (c) Completed/closed bugs must be moved out of `TODOS.md` into `CHANGELOG.md` — the backlog should only show open work.

---

### L-022: Frontend Type Drift — Strict TypeScript Catches Service/Page Mismatches

**Date**: March 17, 2026 | **Severity**: High | **Domain**: Frontend / TypeScript

During Phase 7 bulk bug fixing, fixing one TypeScript error (removing `monthlyLimit` from `cards/page.tsx`) exposed **27+ additional errors** across 8 files. The root cause: frontend service types and page-level usage had drifted apart over months of parallel development.

**Common drift patterns found**:
- Property names differ (`cardholderName` vs `cardHolderName`, `approvedAmount` vs `maxAmount`)
- Backend returns flat array but frontend expects paginated `.content` wrapper
- Service method signatures changed (`getStatement` → `listStatements`) but callers weren't updated
- ID types inconsistent (`string` vs `number` for agent IDs, `string | undefined` vs `string`)
- Extended type fields (`target`, `type` on Pocket) never added to the TypeScript interface

**Rule**: (a) Run `next build` (or `tsc --noEmit`) after **every** service type change — never assume callers are compatible. (b) Define a single source-of-truth type per backend DTO and re-export it; never duplicate type shapes. (c) When fixing one type error, budget time for cascading fixes — they are the norm, not the exception.

---

### L-023: Bulk Audit Approach — Verify Before Fixing

**Date**: March 17, 2026 | **Severity**: Medium | **Domain**: Process / Audit

Of 32 backend P0 financial bugs (Batch 1), only **2 needed genuine code changes**. The other 30 were already correctly implemented — the audit descriptions were based on code snapshots that had since been fixed in earlier phases. Blindly "fixing" already-correct code would have introduced regressions.

**Verification protocol**: For each bug, (1) read the current code at the exact location described, (2) check if the described vulnerability still exists, (3) only modify if the bug is confirmed present. This cut Batch 1 effort from ~32 fixes to 2.

**Rule**: (a) Never batch-apply fixes from an audit report without first verifying each finding against the current codebase state. (b) Mark findings as "Already Fixed" with evidence (file:line, pattern found) rather than silently skipping them. (c) When an audit is stale, the verification pass IS the primary deliverable.

---

### L-024: Auth Parameter Changes Break Unit Tests

**Date**: March 17, 2026 | **Severity**: High | **Domain**: Backend / Testing

Adding `@AuthenticationPrincipal Jwt jwt` to controller methods for security fixes (Batch 2) broke unit tests that called those methods without the new parameter. Both `CardControllerTest.java` and `LendingApplicationServiceTest.java` failed to compile.

**Root Cause**: Controller unit tests call methods directly (not via MockMvc), so adding a parameter changes the method signature. Integration tests using MockMvc are unaffected because they go through the full Spring Security filter chain.

**Rule**: (a) After adding `@AuthenticationPrincipal` to any controller method, immediately grep for direct method calls in test files: `grep -rn "methodName(" backend/*/src/test/`. (b) For unit tests, create a mock `Jwt` object: `Jwt.withTokenValue("test").header("alg","RS256").claim("account_id","test-id").build()`. (c) Prefer MockMvc-based tests for controllers with security annotations — they test the full stack including auth resolution.

---

### L-025: Constructor Signature Changes Cascade to All Subclasses and Tests

**Date**: March 17, 2026 | **Severity**: High | **Domain**: Backend / Shared Libraries

Adding `PlatformTransactionManager` to `SagaOrchestrator`'s constructor (BUG-SHARED-007 fix) caused a **cascading build failure** across every class that extends it: `TransferSagaOrchestrator`, `CashbackSagaOrchestrator`, and all their test files. The Maven build broke in 4+ locations.

**Root Cause**: Java constructors are not inherited — every subclass that calls `super(...)` must be updated when the parent constructor signature changes. Test files that instantiate these subclasses also break.

**Rule**: (a) Before changing a shared library constructor, run `grep -rn "extends ClassName" backend/ --include="*.java"` to find all subclasses. (b) For each subclass, also check its test files: `grep -rn "new SubclassName(" backend/ --include="*.java"`. (c) Update all locations in the same commit — never push a partial constructor change. (d) After fixing, run `mvn clean package -DskipTests` to verify zero compilation errors before proceeding.

---

_Last Updated: March 17, 2026_
