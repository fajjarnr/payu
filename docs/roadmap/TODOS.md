# 📂 PayU Project Roadmap & Engineering Scorecard

> **Platform Maturity**: 🟡 **62%** | **Production Readiness**: 🔴 **48%** (Honest Assessment - Critical Gaps in Security, Testing, and Feature Completeness)
> **Strategic Objective**: Standardize a stand-alone digital banking infrastructure on Red Hat OpenShift 4.20+.
> **Last Synchronized**: February 9, 2026 (P19 - Full Platform Audit Complete)

---

## 🏢 Strategic Platform Overview

### 📈 DORA Metrics Alignment (Elite Targets)

Metrics derived from the latest E2E and CI/CD audit logs.

| Metric                    | Target    | Current Status       | Alignment    |
| :------------------------ | :-------- | :------------------- | :----------- |
| **Deployment Frequency**  | ≥ 1/day   | Multiple/day (CI)    | 🟢 **Elite** |
| **Lead Time for Changes** | < 4 hours | ~30 mins             | 🟢 **Elite** |
| **Mean Time to Recovery** | < 30 mins | ~15 mins             | 🟢 **Elite** |
| **Change Failure Rate**   | < 10%     | ~15% (E2E Tests Fixed - Auth Issues Resolved) | 🟡 **Improving** |

### 🏗️ Architectural Principle Compliance

Audit against the *14 Immutable Laws of PayU*.

- **Hexagonal Architecture**: ⚠️ **55% compliance** — Only 7/19 Java services use hexagonal. 8 services use flat packages. 3 Quarkus services incompatible.
- **Event-First**: ⚠️ **Partial** — Kafka used directly in transaction/wallet but `events-starter`, `outbox-starter`, `saga-starter` are **DEAD CODE** (built but 0 services consume them).
- **Zero Trust**: ⚠️ **Partial** — `security-starter` excellent but 4 services don't use it (cms, ab-testing, notification, gateway, api-portal).
- **API-First**: ✅ Centralized OpenAPI Portal (22 services) active.
- **Doc-as-Code**: ✅ 13 ADRs versioned in `/docs/adr`.

---

## 🚀 Active Mission: P19 - Full Platform Audit & Production Readiness (Feb 2026)

**Mission Goal**: Honest assessment of true production readiness. Identify all critical gaps blocking OpenShift deployment.

### 🎯 Mission Status: 🔴 AUDIT COMPLETE — Production NOT Ready

**Honest Production Readiness Score: 48/100**

| Category | Weight | Score | Weighted |
| :--- | :--- | :--- | :--- |
| **Backend Services (Avg)** | 25% | 72/100 | 18.0 |
| **Shared Libraries** | 10% | 83/100 | 8.3 |
| **Frontend Web-App** | 15% | 72/100 | 10.8 |
| **Frontend Mobile** | 5% | 58/100 | 2.9 |
| **Testing (Unit+Integration)** | 15% | 55/100 | 8.3 |
| **E2E Tests (Passing)** | 10% | 15/100 | 1.5 |
| **Security & Compliance** | 10% | 40/100 | 4.0 |
| **Infrastructure (OpenShift)** | 10% | 70/100 | 7.0 |
| **TOTAL** | 100% | — | **60.8 → 48%*** |

> *Adjusted to 48% karena ada 3 P0 blockers yang belum terselesaikan (Security Token Storage, Empty Shared Starters, No Load Tests) yang men-diskualifikasi deployment ke production.

---

## 🔴 P0 — PRODUCTION BLOCKERS (Must Fix Before Deploy)

### ~~P0-SEC-001: JWT Token Stored in localStorage (XSS Vulnerability)~~ ✅ FIXED (Feb 9, 2026)

**Severity**: ~~🔴 CRITICAL~~ → ✅ RESOLVED

- Implemented BFF (Backend-for-Frontend) pattern with httpOnly cookies
- Created `/api/auth/login`, `/api/auth/logout`, `/api/auth/refresh` server-side routes
- Created `/api/v1/[...path]` catch-all proxy that converts cookie → Bearer header
- Rewrote `src/lib/api.ts` — removed ALL localStorage token operations (was 6 refs)
- Updated login page to use BFF route instead of direct API call
- Updated `AuthService.ts` to use fetch→BFF, removed static `api` import
- Updated `useAuth.ts` logout to call BFF `/api/auth/logout`
- Removed `next.config.ts` gateway rewrite (proxy handles forwarding)
- Verified: `grep -r "localStorage.*token" src/` returns 0 matches
- PCI-DSS 8.2.4 compliant: tokens never accessible to client JavaScript

### ~~P0-ARCH-001: Shared Starters Are Dead Code (0 Consumers)~~ ✅ FIXED (Feb 9, 2026)

**Severity**: ~~🔴 CRITICAL~~ → ✅ RESOLVED

**Changes Applied** (commit `320f686`):
- Integrated `outbox-starter` into 4 financial services: transaction-service, wallet-service, lending-service, billing-service
- Added Maven dependency `outbox-starter` to all 4 service POMs
- Created Flyway migration `outbox_events` table for each service (V8, V100, V4, V2 respectively)
- Refactored 5 Kafka publisher adapters from direct `KafkaTemplate.send()` to `OutboxService.createEvent()`:
  - `TransactionEventPublisherAdapter` (4 event methods) → outbox with topic `payu.transactions.*`
  - `SplitBillEventPublisherAdapter` (7 event methods) → outbox with topic `payu.split-bills.*`
  - `WalletEventPublisherAdapter` (5 event methods) → outbox with topic `payu.wallets.*`
  - `KafkaLoanEventPublisherAdapter` (2 event methods) → outbox with topics `loan.approved/rejected`
  - `PaymentService.publishPaymentEvent()` → outbox with topic `payment-events`
- Added `payu.outbox` config section to all 4 service `application.yml` files
- Updated billing-service test mocks from `KafkaTemplate` to `OutboxService`
- Port interfaces unchanged (hexagonal architecture preserved)
- Financial events now written to DB within same transaction → at-least-once delivery guaranteed

**Remaining lower-priority items** (P2):
- `events-starter` (CloudEvents) still unused — consider integration or removal
- `saga-starter` still unused — consider integration for distributed transaction flows

### ~~P0-SEC-002: Hardcoded Credentials in Version Control~~ ✅ FIXED (Feb 9, 2026)

**Severity**: ~~🔴 CRITICAL~~ → ✅ RESOLVED

**Changes Applied**:
- `infrastructure/keycloak/payu-realm-export.json`: All user passwords and client secrets now use Keycloak `$(env.VAR)` syntax
- `infrastructure/containers/init-vault.sh`: All hardcoded secrets replaced with `${VAR:?ERROR}` (fail-fast)
- `infrastructure/local-podman/containers/manage-podman.sh`: Template uses `CHANGE_ME_*` placeholders
- `backend/investment-service/application.yaml`: Raw `payu_password` replaced with `${DB_PASSWORD}`
- `backend/gateway-service/application.yaml`: JWT secret, OIDC secret, webhook secret defaults removed
- `backend/partner-service/application.yml`: JWT secret default removed
- `backend/auth-service/application.yaml`: Keycloak client-secret default removed
- `backend/kyc-service/config.py` & `backend/analytics-service/config.py`: Empty default + startup validation
- `scripts/seed-data.sh`: Uses `${KEYCLOAK_ADMIN_PASSWORD}` and `${KEYCLOAK_TEST_USER_PASSWORD}` env vars
- `backend/docs/archive/deprecated-docker/docker-compose.yml`: `P@ssw0rd123`, `payu_secret`, `13.212.248.122` all replaced
- `.env.template`: All defaults removed, fields marked REQUIRED with generation instructions

**Remaining lower-priority items** (P2): Application config files with `${ENV:-postgres}` or `${ENV:-payu}` defaults — acceptable for local dev but should use Vault in production

### P0-TEST-001: Zero Tests on Critical Financial Components

**Severity**: 🔴 CRITICAL — Financial Risk

- `outbox-starter`: 0 tests — handles transactional event publishing
- `saga-starter`: 0 tests — handles distributed transaction compensation
- `lending-service`: 0 integration tests — financial lending!
- `fx-service`: 0 integration tests — currency exchange rates!
- `load-tests/src/`: Empty scaffold — no Gatling simulations (real sims in `performance/` separate folder)
- **Remediation**: Write integration tests for all starters. Write loan/FX integration tests. Move Gatling simulations to load-tests/ or consolidate.

### ~~P0-INFRA-001: Port Conflict in Docker Compose~~ ✅ FIXED (Feb 9, 2026)

**Severity**: ~~🔴 CRITICAL~~ → ✅ RESOLVED

- `api-portal-service` changed from `8099` to `8021:8021`
- `keycloak` keeps `8099:8080`
- `backoffice-service/Containerfile` EXPOSE fixed from `8099` to `8080`
- `backoffice-service/OpenApiConfiguration` dev URL fixed to `localhost:8011`
- Deprecated `docker-compose.yml` also updated

---

## 🟠 P1 — HIGH PRIORITY (Fix Before Staging)

### P1-ARCH-001: Quarkus Services Cannot Use Shared Starters

- 3 Quarkus services (notification, gateway, api-portal) are standalone POMs
- Cannot use security-starter, resilience-starter, cache-starter (Spring Boot only)
- **Creates security gap** — no JWT validation, no circuit breakers, no caching in gateway
- **Remediation**: Create Quarkus-compatible equivalents OR migrate to Spring Boot

### P1-ARCH-002: cms-service Uses ZERO Shared Starters

- Despite being Spring Boot with parent POM, cms-service imports NO shared starters
- No security-starter = no JWT auth = **unauthenticated CMS endpoints**
- No resilience-starter = no circuit breakers
- Only 2 test files, 0 integration tests
- **Remediation**: Add all 4 shared starter dependencies

### P1-ARCH-003: ab-testing-service Missing 3/4 Starters

- Only uses `api-commons` — missing security, resilience, cache starters
- 0 integration tests
- **Remediation**: Add missing starters, write integration tests

### P1-ARCH-004: statement-service Critically Thin

- Only 13 main files, 2 test files, 0 integration tests
- Missing security-starter (handles sensitive financial statements!)
- Missing resilience-starter (calls transaction-service and wallet-service)
- **Remediation**: Add starters, write proper tests, implement resilience patterns

### P1-TEST-001: E2E Tests Passing Rate < 15%

- 12 Playwright spec files, ~424 tests
- Most tests failing due to: auth middleware redirects, missing UI features, selector mismatches
- Investment module: tests written but features not implemented (TDD without implementation)
- Lending, KYC, Bill Pay: major implementation gaps
- **Remediation**: Align tests with actual implementation, skip unimplemented feature tests

### P1-SEC-001: next.config.ts Allows All Remote Image Sources

- `remotePatterns: [{ protocol: 'http', hostname: '**' }, { protocol: 'https', hostname: '**' }]`
- Potential SSRF/abuse vector — allows loading images from any domain
- **Remediation**: Whitelist specific CDN/API domains only

### P1-SEC-002: security-starter Key Derivation Uses SHA-256

- `EncryptionService` uses `MessageDigest.getInstance("SHA-256")` for key derivation
- Should use PBKDF2, bcrypt, or Argon2 for proper key stretching
- No key rotation mechanism
- **Remediation**: Implement PBKDF2WithHmacSHA256 with salt and iterations

### P1-FE-001: Missing Frontend-Backend Service Integrations

- No frontend service class for: Notification, Investment, Compliance, Analytics, KYC, Support, Billing
- 7 of 22 backend services have no frontend integration
- **Remediation**: Create service classes and integrate into UI

### P1-FE-002: Missing .env.example File

- README references "Create .env.local" but no template exists
- New developers have no reference for required environment variables
- **Remediation**: Create `.env.example` with all required vars documented

### P1-INFRA-001: Helm Charts Directory Empty

- `infrastructure/helm/` contains only `.gitkeep`
- No actual Helm charts exist despite being listed as infrastructure component
- **Remediation**: Create Helm charts OR remove from architecture docs

### P1-INFRA-002: No NetworkPolicies in OpenShift Base

- All pods can communicate freely — no network segmentation
- **Remediation**: Add NetworkPolicy per namespace/service

### P1-INFRA-003: No TLS/Certificate Management

- No cert-manager or Route TLS configs in base manifests
- **Remediation**: Add cert-manager or OpenShift Route TLS configuration

### P1-BUILD-001: Makefile build-test-deps Incomplete

- Only builds 4/9 shared modules (api-commons, cache, resilience, security)
- Missing: events-starter, outbox-starter, saga-starter, archunit-starter, flyway
- **Remediation**: Add all 9 modules to build-test-deps target

---

## 🟡 P2 — MEDIUM PRIORITY (Fix Before Production)

### P2-ARCH-001: 8/19 Java Services Lack Hexagonal Architecture

Non-compliant services (using flat package structure):
- auth-service, statement-service, backoffice-service, partner-service
- promotion-service, support-service, billing-service, ab-testing-service
- **Remediation**: Refactor to hexagonal ports/adapters pattern per architectural standard

### P2-ARCH-002: Dual Config Files in Multiple Services

Services with both `.yaml` AND `.yml` (Spring Boot loads both, last wins — unpredictable):
- investment-service, lending-service, compliance-service, cms-service, ab-testing-service
- **Remediation**: Standardize to single `application.yml` per service

### P2-TEST-001: Shared Libraries Severely Under-Tested

| Module | Source Files | Test Files | Risk |
| :--- | :--- | :--- | :--- |
| cache-starter | 17 | 1 | 🟠 High |
| resilience-starter | 10 | 1 | 🟠 High |
| events-starter | 4 | 0 | 🔴 Critical |
| outbox-starter | 10 | 0 | 🔴 Critical |
| saga-starter | 20 | 0 | 🔴 Critical |

### P2-TEST-002: Low Test Coverage Services

| Service | Main Files | Test Files | Test Ratio | Risk |
| :--- | :--- | :--- | :--- | :--- |
| lending-service | 65 | 4 | 6% | 🔴 Financial |
| investment-service | 39 | 4 | 10% | 🟠 Financial |
| fx-service | 20 | 3 | 15% | 🟠 Financial |
| cms-service | 17 | 2 | 12% | 🟡 |
| ab-testing-service | 15 | 3 | 20% | 🟡 |
| statement-service | 13 | 2 | 15% | 🟡 |

### P2-TEST-003: No Contract Tests (Pact/Spring Cloud Contract)

- 22 microservices communicating without contract testing
- API changes can break consumers silently
- **Remediation**: Implement Pact or Spring Cloud Contract for critical service pairs

### P2-TEST-004: Security Tests Are Static Only

- `tests/security/` only verifies config files and report existence
- No OWASP ZAP, no HTTP-based auth bypass, no SQL injection testing
- **Remediation**: Implement DAST with OWASP ZAP in CI pipeline

### P2-TEST-005: load-tests/ Empty Scaffold

- `tests/load-tests/` has pom.xml and config but NO Gatling simulations
- Real simulations exist in `tests/performance/` — confusing structure
- **Remediation**: Consolidate into single directory

### P2-FE-001: Only 2 Zustand Stores for Banking App

- Only `authStore` and `uiStore` — missing wallet, transaction, notification stores
- **Remediation**: Add stores for critical state or confirm TanStack Query handles it

### P2-FE-002: Dual Test Runners (Vitest + Jest)

- Both `vitest.config.ts` and `jest.config.js` exist
- Confusing for contributors — which to use?
- **Remediation**: Standardize on one test runner

### P2-FE-003: Mobile App Feature Parity Gap

- Web has 22 routes, mobile has ~10
- Missing: investments, lending, analytics, settings, exchange, backoffice
- 11 backend services have no mobile integration
- **Remediation**: Implement missing mobile screens for core flows

### P2-INFRA-001: OpenShift Uses image:latest

- OpenShift base manifests use `image: <service>:latest`
- No pinned versions or image registry prefix
- **Remediation**: Use image digests or semver tags with registry prefix

### P2-INFRA-002: Traefik Dashboard Insecure

- `--api.insecure=true` in docker-compose — exposes dashboard without auth
- **Remediation**: Remove in production or add basic auth

### P2-INFRA-003: Kafka Uses Legacy Zookeeper

- Docker Compose still uses Zookeeper for Kafka
- **Remediation**: Migrate to KRaft mode for production

### P2-INFRA-004: Tekton Pipeline Tasks Sparse

- Only `security-scan-task.yaml` exists
- No build, test, or deploy tasks (pipelines reference missing tasks)
- **Remediation**: Create all Tekton tasks or switch to alternative CI

---

## 🟢 P3 — LOW PRIORITY (Nice to Have)

### P3-ARCH-001: No GlobalExceptionHandler in api-commons

- All services inherit exception handling from `resilience-starter`'s FallbackHandler
- If a service removes api-commons, stack traces leak to clients
- **Remediation**: Add explicit GlobalExceptionHandler to api-commons

### P3-SEC-001: No HSM/Key Rotation in security-starter

- Encryption keys are static — no rotation mechanism
- No HSM integration for production key management
- **Remediation**: Implement key rotation with Vault Transit backend

### P3-FE-001: Developer Docs Missing API Reference

- No auto-generated OpenAPI documentation for partner portal
- Only 3 guides (bifast, partner, qris) — missing auth, webhooks, investments, lending
- **Remediation**: Generate OpenAPI docs, add missing guides

### P3-FE-002: No Search in Developer Docs

- Developer portal has no search functionality
- **Remediation**: Add Algolia DocSearch or similar

### P3-TEST-001: No Mutation Testing

- No PIT or similar mutation testing configured
- Cannot verify test quality (tests may pass with wrong assertions)
- **Remediation**: Add PITest for Java services

### P3-PERF-001: LCP Optimization Still at 9.3s

- Lighthouse LCP is 9.3s — target is <2.5s
- **Remediation**: Implement code splitting, lazy loading, server components

---

## 📊 Honest Per-Service Production Readiness

| Service | Score | Blockers | Verdict |
| :--- | :--- | :--- | :--- |
| **transaction-service** | 90% | Not using saga-starter despite saga tests | 🟢 Near Ready |
| **wallet-service** | 88% | — | 🟢 Near Ready |
| **account-service** | 85% | Not using events-starter | 🟡 Ready w/ caveats |
| **api-commons** (shared) | 92% | Missing GlobalExceptionHandler | 🟢 Ready |
| **outbox-starter** (shared) | 90% | **0 tests, 0 consumers** | 🔴 NOT Ready |
| **cache-starter** (shared) | 88% | 1 test for 17 files | 🟡 |
| **archunit-starter** (shared) | 88% | Meta-tests only | 🟡 |
| **saga-starter** (shared) | 85% | **0 tests, 0 consumers** | 🔴 NOT Ready |
| **resilience-starter** (shared) | 85% | Alert publishing is TODO | 🟡 |
| **promotion-service** | 82% | No hexagonal | 🟡 |
| **partner-service** | 82% | No hexagonal, 1 migration | 🟡 |
| **analytics-service** | 82% | Java-style migration path | 🟡 Ready |
| **security-starter** (shared) | 82% | SHA-256 key derivation | 🟡 |
| **kyc-service** | 80% | No explicit DB migrations | 🟡 |
| **compliance-service** | 80% | Dual config files | 🟡 |
| **backoffice-service** | 78% | No hexagonal | 🟡 |
| **auth-service** | 78% | Flat packages, 1 migration | 🟡 |
| **billing-service** | 72% | 1 repo for 3 controllers | 🟡 |
| **support-service** | 72% | No README, limited scope | 🟡 |
| **investment-service** | 72% | Missing starters, low tests | 🟠 Risky |
| **events-starter** (shared) | 70% | **Interface only, 0 tests** | 🔴 NOT Ready |
| **lending-service** | 70% | **0 integration tests** (financial!) | 🔴 NOT Ready |
| **fx-service** | 68% | **0 integration tests**, no resilience | 🔴 NOT Ready |
| **gateway-service** | 65% | Quarkus, no shared security | 🟠 Risky |
| **ab-testing-service** | 62% | Missing 3/4 starters | 🟠 Risky |
| **notification-service** | 60% | Quarkus, no shared security | 🟠 Risky |
| **cms-service** | 58% | **0 starters, 2 tests** | 🔴 NOT Ready |
| **api-portal-service** | 55% | No security, no ArchUnit | 🔴 NOT Ready |
| **statement-service** | 52% | Thin impl, no security | 🔴 NOT Ready |

---

## 📉 Production Readiness Scorecard (Honest Assessment)

### Overall: 🔴 48/100 — NOT Production Ready

| Dimension | Score | Justification |
| :--- | :--- | :--- |
| **Code Quality** | 70/100 | Good architecture in core services, but inconsistent across platform |
| **Security** | 35/100 | localStorage tokens (P0), no key rotation, 4 services without auth |
| **Testing** | 40/100 | E2E <15% pass, 0 tests on critical starters, 0 contract tests, 0 load tests |
| **Observability** | 80/100 | Prometheus, Grafana, Jaeger, LokiStack configured |
| **Infrastructure** | 65/100 | OpenShift manifests exist but no Helm, no TLS, no NetworkPolicy |
| **Feature Completeness** | 55/100 | Core banking works, but Investment/Lending/KYC UI incomplete |
| **Documentation** | 75/100 | Good ADRs and READMEs, but gaps in developer docs |
| **Operational Readiness** | 45/100 | No runbook testing, no chaos engineering, no DR drill results |

### What "Production Ready" Actually Means for a Banking Platform:

1. ✅ All financial transactions must be idempotent → **Partially done**
2. ❌ All PII must be encrypted at rest and in transit → **localStorage tokens violate this**
3. ❌ All services must pass integration tests → **6 services have 0 integration tests**
4. ❌ Load testing must prove capacity → **No load test results**
5. ❌ Security penetration testing must pass → **Only static checks, no DAST**
6. ❌ Disaster recovery must be tested → **DR plan exists but untested**
7. ✅ Health endpoints must work → **Done (21/22 services)**
8. ⚠️ Audit trail must be complete → **security-starter works but not all services use it**
9. ❌ Compliance (PCI-DSS, PDP) must be verified → **Not formally audited**
10. ⚠️ Zero-downtime deployment must work → **ArgoCD configured but untested**

---

## 🏁 Previous Mission: P18 - Accessibility & A11y Compliance (Feb 2026)

**Mission Status**: 🟡 89% COMPLETE - Public Pages A11y Compliant

- [x] Axe Configuration fixed, Color Contrast fixed, Login/Onboarding pages compliant
- [x] 16/18 Axe tests passing (89% pass rate)
- [ ] Protected Pages: Dashboard & Investments - Chart SVG accessibility (separate issue)

### 🧩 P18 Implementation Details (Feb 6, 2026)

**Problem Identified**: E2E tests showing color contrast violations and Axe configuration error.

**Root Cause**:
- `muted-foreground` color at 45% lightness provided only ~3.8:1 contrast ratio (needs 4.5:1).
- `keyboard` rule in Axe configuration does not exist in `@axe-core/playwright@4.11.0`.
- Zinc color scale (zinc-400, zinc-500) on dark backgrounds failed contrast requirements.
- Primary button color `emerald-500` (#10b981) gave only 2.53:1 contrast with white text.
- Login links using `emerald-800` (#006045) on light background gave only 2.62:1 contrast.

**Solution Implemented**:
1. ✅ Fixed `globals.css` - Updated `--muted-foreground` from 45% to 35% (light mode) and 60% to 70% (dark mode).
2. ✅ Fixed `globals.css` - Updated `--primary` from `hsl(160 84.3% 39.4%)` to `hsl(160 84% 26%)` (#0a6b48).
3. ✅ Fixed `a11y-audit.spec.ts` - Replaced invalid `keyboard` rule with valid alternatives:
   - `focus-order-semantics`, `tabindex`, `region`, `aria-hidden-focus`, `scrollable-region-focusable`.
4. ✅ Fixed `login/page.tsx` - Updated link colors from emerald-800 to emerald-600.
5. ✅ Fixed `onboarding/page.tsx` - Updated zinc-400 to zinc-300 for better contrast.
6. ✅ Fixed `button.tsx` - Updated `emerald` variant from emerald-500 to emerald-800.
7. ✅ Fixed `stepper.tsx` - Changed `text-muted-foreground` to `text-foreground/60` for inactive steps.
8. ✅ Fixed `calendar.tsx` - Updated selected state colors from emerald-500 to emerald-700.

**Files Modified**:
- `/frontend/web-app/e2e/a11y-audit.spec.ts` - Fixed Axe config (removed invalid `keyboard` rule).
- `/frontend/web-app/src/app/globals.css` - Fixed color tokens for WCAG AA compliance.
- `/frontend/web-app/src/app/[locale]/login/page.tsx` - Fixed link color contrast (emerald-800 → emerald-600).
- `/frontend/web-app/src/app/[locale]/onboarding/page.tsx` - Fixed text contrast on dark sidebar.
- `/frontend/web-app/src/components/ui/button.tsx` - Fixed emerald variant contrast.
- `/frontend/web-app/src/components/ui/stepper.tsx` - Fixed inactive step text contrast.
- `/frontend/web-app/src/components/ui/calendar.tsx` - Fixed selected state contrast.

**Verification**:
- Color contrast ratios now meet WCAG 2.1 AA standards:
  - Normal text: 4.5:1 minimum
  - Large text: 3:1 minimum
- Test Results: 16/18 tests passing (89% pass rate).
- Login Page: ✅ All color contrast issues resolved.
- Onboarding Page: ✅ All color contrast issues resolved.
- Remaining issues: Dashboard & Investments (Chart SVG accessibility - separate concern).

---

## 🏁 Previous Mission: P17 - Production Readiness (Feb 2026)

**Mission Goal**: Stabilize the isolated Podman Compose environment and achieve 95%+ E2E pass rate.

### 🎯 Mission Status: 🟡 COMPLETE - E2E Tests Fixed

- [x] **Environment**: Podman Compose (`docker-compose.test.yml`) stabilized.
- [x] **Base Images**: Migrated to UBI9 (OpenJDK 21, Node.js 20).
- [x] **Frontend**: E2E Image and Playwright framework configured.
- [x] **Backend Build**: All 22 microservices successfully built and containerized.
- [x] **Quality Gates**:
  - [x] 100% OpenAPI Coverage (154/154 endpoints documented).
  - [x] **E2E Test Infrastructure** - ✅ **FIXED** (Auth fixtures implemented, tests passing ~70%).
  - [x] **Accessibility Audit** - ✅ **FIXED** (P18 completed, WCAG 2.1 AA compliant).
- [x] **UI/UX**: Emerald v4.0 System implementation with Radix UI Primitives.
- [x] **Features**: FX (`/exchange`) and Statement (`/settings`) integrations complete.

### 🧩 P17-C22: Playwright E2E Test Fixes (Feb 6, 2026)

**Problem Identified**: Tests failing due to authentication middleware blocking protected routes.

**Root Cause**:
- `middleware.ts` requires `accessToken` or `payu_session` cookies for protected routes
- Tests were navigating directly to `/investments`, `/dashboard`, etc. without authentication
- This caused redirects to `/login`, making all assertions fail

**Solution Implemented**:
1. ✅ Created `/e2e/fixtures/index.ts` - Extended test fixtures with auth support
2. ✅ Created `/e2e/fixtures/auth.ts` - Authentication utilities
3. ✅ Updated all 12 test files to use `authPage` fixture for protected routes
4. ✅ Updated `playwright.config.ts`:
   - Changed baseURL to `http://localhost:3001` (containerized app)
   - Disabled webServer (using existing containerized app)
   - Set default locale to 'id'
5. ✅ Added `waitForLoadState('networkidle')` after navigation for stability

**Test Results After Fix**:
- **Before**: <10% pass rate (132 failures from 290 tests)
- **After**: ~70% pass rate (many tests now passing)

**Remaining Issues**:
- Color contrast accessibility violations (WCAG 2.1 AA)
- Some timeout issues on slower interactions
- A few tests need selector updates for changed UI

### 🧩 Detailed P17 Execution Breakdown

- **P17-C11: Infrastructure Stabilization**
  - [x] Fixed `api-commons` dependency mismatches.
  - [x] Standardized all services to internal port **8080**.
  - [x] Resolved Redis connectivity in `cache-starter` (REDIS_HOST env fixes).
  - [x] Healthcheck paths aligned (`/actuator/health/liveness`).
- **P17-C20: Security & Health Check Hardening**
  - [x] Fixed 401 Unauthorized on health endpoints via `WebSecurityCustomizer`.
  - [x] Standardized all system passwords to `P@ssw0rd123`.
  - [x] Fixed Public Endpoint access (`/api/v1/accounts/register`) in Gateway.
- **P17-C21: Python/ML Services**
  - [x] Migrated `kyc-service` and `analytics-service` to `python:3.12-slim`.
  - [x] Implemented `uv` for 10x faster builds.

---

## 🐛 ACTIVE BUG REPORT & REMEDIATION (Feb 6, 2026)

### Status: 🟡 IMPROVING - Playwright E2E Tests Fixed

> **Test Environment**: Container (Podman Compose) - Frontend at `http://localhost:3001`
> **Test Files**: 12 spec files | **Total Tests**: 424 tests
> **Execution**: Headless Chromium mode
> **Status**: 🟡 **FIXED** - Authentication issues resolved, ~70% tests passing

| Priority | Bug ID | Description | Affected Services | Status |
| :--- | :--- | :--- | :--- | :--- |
| 🔴 **P0** | #P0-1 | Health Endpoints return 503 (Redis/Port/Seed issues) | auth, account, trans, wallet | ✅ FIXED |
| 🟡 **P1** | #P1-1 | Keycloak Admin Password Sync Issue | Keycloak | ✅ Workaround Docs |
| 🟢 **P2** | #P2-1 | Gateway Actuator 404 | Gateway | ⚪ Low Priority |
| 🔴 **P0** | #P0-2 | Service Down - Connection Refused | partner, compliance, ab-testing | ✅ FIXED |
| 🔴 **P0** | #P0-2a | Service Up but Redis DOWN | support | ✅ FIXED |
| 🔴 **P0** | #P0-3 | Redis Connection Failure | statement-service | ✅ FIXED |
| 🔴 **P0** | #P0-4 | Container Not Running | fx-service | ✅ FIXED |
| 🔴 **P0** | #P0-5 | Redis Connection Failure | billing-service | ✅ FIXED |
| 🟡 **P1** | #P1-2 | Port Conflict - AB Testing vs Lending | ab-testing-service | ✅ NO ISSUE |
| 🟡 **P1** | #P1-3 | CMS Content Endpoint 404 | cms-service | 🔴 OPEN |
| 🟡 **P1** | #P1-E2E-1 | **Playwright E2E Auth Issues** | `web-app` | ✅ **FIXED** |

---

## 🧪 Playwright E2E Test Audit Report (Feb 6, 2026)

### Executive Summary

**✅ ISSUE RESOLVED**: Playwright E2E tests were failing due to **authentication middleware** blocking protected routes. The tests expected authenticated sessions but were navigating directly to protected pages without auth cookies.

**Root Cause**: `middleware.ts` redirects unauthenticated users from `/investments`, `/dashboard`, etc. to `/login`. Tests were not setting session cookies before navigation.

**Solution**: Created extended test fixtures with automatic authentication support.

### Test Execution Details

| Metric | Value |
| :--- | :--- |
| **Test Framework** | Playwright with Chromium |
| **Test Files** | 12 `.spec.ts` files |
| **Total Tests** | ~1965 (including retries) |
| **Environment** | Container (Podman Compose) |
| **Base URL** | `http://localhost:3001` |
| **Execution Mode** | Headless |

### Error Summary Statistics

| Error Type | Count | Percentage |
| :--- | :--- | :--- |
| **Element(s) not found** | 128 | ~40% |
| **toBeVisible() failed** | 120 | ~37% |
| **page.click Timeout** | 92 | ~28% |
| **locator.click Timeout** | 12 | ~4% |
| **toHaveTitle() failed** | 10 | ~3% |
| **toHaveCount() failed** | 10 | ~3% |
| **toBeGreaterThanOrEqual() failed** | 6 | ~2% |
| **toBeEnabled() failed** | 4 | ~1% |
| **Axe keyboard rule error** | 2 | ~1% |
| **toEqual() deep equality failed** | 2 | ~1% |

### Critical Issues Identified

#### 1. 🔴 Port Conflict - Grafana Hijacking (P0)

**Problem**: Tests expecting "PayU" title receive "Grafana" instead

```
Expected pattern: /PayU/
Received string:  "Grafana"
```

**Affected Tests**:
- `bill-pay-flow.spec.ts` - Bill payment page
- `investment-flow.spec.ts` - Investment page
- Multiple page navigation tests

**Root Cause**: Port 3001 atau port terkait mungkin sedang digunakan oleh Grafana container, menyebabkan request diarahkan ke Grafana bukan PayU web-app.

**Remediation**:
- [ ] Verify port mapping in `docker-compose.yml`
- [ ] Ensure web-app container menggunakan port yang benar
- [ ] Check for port conflicts between services

---

#### 2. 🔴 Missing UI Elements - Feature Not Implemented (P0)

**Problem**: Test mencari elemen yang tidak ada di aplikasi

**Top Missing Elements**:

| Element | Test File | Expected Feature |
| :--- | :--- | :--- |
| `Optimasi Portofolio` button | `investment-flow.spec.ts` | Portfolio optimization feature |
| `Tinjau Strategi` button | `investment-flow.spec.ts` | Strategy review feature |
| `Manajemen Kekayaan` text | `investment-flow.spec.ts` | Wealth management section |
| `.bg-success-light` growth badge | `investment-flow.spec.ts` | Growth indicators styling |
| `Pulsa` biller category | `bill-pay-flow.spec.ts` | Mobile credit top-up |
| `Listrik (PLN)` biller | `bill-pay-flow.spec.ts` | Electricity bill payment |
| `Penyelesaian Real-time 24/7` | `bill-pay-flow.spec.ts` | Real-time processing badge |
| `Katalog Produk Terpilih` | `investment-flow.spec.ts` | Featured products catalog |
| `Suku Bunga Tetap Plus` | `investment-flow.spec.ts` | Fixed rate product display |
| `Risiko Rendah` label | `investment-flow.spec.ts` | Risk level indicators |
| `input[placeholder="username123"]` | `check_ui.spec.ts` | Login form placeholder |

**Root Cause**:
- Features memang belum diimplementasi di frontend
- Test ditulis sebelum fitur selesai (TDD yang belum diimplementasi)
- Perubahan UI/UX yang belum di-update di test

---

#### 3. 🔴 Accessibility Configuration Error (P1)

**Problem**: Axe accessibility test gagal karena rule yang tidak dikenal

```
Error: unknown rule `keyboard` in options.runOnly
```

**Affected File**: `a11y-audit.spec.ts`

**Root Cause**: Konfigurasi Axe menggunakan rule `keyboard` yang mungkin tidak tersedia di versi `@axe-core/playwright` yang digunakan.

**Remediation**:
- [ ] Update Axe configuration to use valid rules
- [ ] Check `@axe-core/playwright` version compatibility

---

#### 4. 🔴 Investment Flow - Major Feature Gaps (P0)

**Test File**: `investment-flow.spec.ts` (16KB, 400+ lines)

**Failed Scenarios**:
- Portfolio optimization button not found
- Strategy review button not found
- Wealth management section not visible
- Growth indicators styling missing
- Product cards not rendering (expected 3, got 0)
- Fixed rate product details not found
- Equity fund details not found
- Gold product details not found

**Assessment**: Investment module memiliki **MAJOR IMPLEMENTATION GAPS** - test sudah lengkap tapi fitur belum diimplementasi.

---

#### 5. 🔴 Lending/PayLater Flow - Feature Incomplete (P0)

**Test File**: `lending-flow.spec.ts` (17KB, largest test file)

**Failed Scenarios**:
- Lending page title mismatch (Grafana issue)
- Loan and PayLater tabs not found
- Credit score display not found
- PayLater limit not found
- Transaction list not rendering

---

#### 6. 🔴 KYC Flow - Implementation Missing (P0)

**Test File**: `kyc-flow.spec.ts`

**Failed Scenarios**:
- Step navigation not working
- Camera upload area not found
- Form validation not implemented
- Success state not reachable

---

### Test Files Status

| Test File | Tests Count | Status | Notes |
| :--- | :--- | :--- | :--- |
| `a11y-audit.spec.ts` | 9+ | 🔴 FAIL | Axe config error + element issues |
| `bill-pay-flow.spec.ts` | 12+ | 🔴 FAIL | Port conflict + missing billers |
| `check_ui.spec.ts` | 4+ | 🔴 FAIL | Login elements not found |
| `investment-flow.spec.ts` | 50+ | 🔴 FAIL | Major feature gaps |
| `kyc-flow.spec.ts` | 30+ | 🔴 FAIL | Implementation incomplete |
| `lending-flow.spec.ts` | 60+ | 🔴 FAIL | Largest file, many failures |
| `login-flow.spec.ts` | 25+ | 🔴 FAIL | Form elements mismatch |
| `onboarding-flow.spec.ts` | 60+ | 🔴 FAIL | Navigation issues |
| `qris-flow.spec.ts` | 30+ | 🔴 FAIL | Payment flow broken |
| `registration-flow.spec.ts` | 30+ | 🔴 FAIL | Form validation missing |
| `settings-flow.spec.ts` | 50+ | 🔴 FAIL | Profile settings incomplete |
| `transfer-flow.spec.ts` | 40+ | 🔴 FAIL | Transfer form issues |

**Overall Pass Rate**: < 10% (estimated from error count)

---

### Root Cause Analysis

```
┌─────────────────────────────────────────────────────────────────┐
│  PLAYWRIGHT E2E TEST FAILURE - ROOT CAUSE ANALYSIS             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. INFRASTRUCTURE (Port Conflicts)                             │
│     └── Grafana menggunakan port yang sama dengan web-app      │
│                                                                 │
│  2. FEATURE IMPLEMENTATION GAPS                                 │
│     ├── Test ditulis lengkap berdasarkan PRD/requirements      │
│     ├── Implementation belum selesai/sempurna                   │
│     └── Gap antara expectation test dan actual UI              │
│                                                                 │
│  3. UI/UX CHANGES WITHOUT TEST UPDATES                          │
│     ├── Component styling berubah                              │
│     └── Text/labels berbeda dengan test expectation            │
│                                                                 │
│  4. ACCESSIBILITY CONFIG                                        │
│     └── Axe rule configuration outdated                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

### Recommended Actions

#### Immediate (P0)
- [ ] **Fix Port Conflict**: Pastikan web-app container tidak bentrok dengan Grafana
- [ ] **Update TODOS.md**: Hapus status "E2E 95% Pass Rate" sampai test benar-benar passing
- [ ] **Review Test Scope**: Diskusikan dengan tim mana fitur yang memang belum diimplementasi

#### Short Term (P1-P2)
- [ ] **Align Test dengan Implementation**: Update test untuk mencocokkan UI yang sudah ada
- [ ] **Mark Skipped Tests**: Tandai test untuk fitur yang memang belum ready dengan `.skip()`
- [ ] **Fix Axe Configuration**: Perbaiki accessibility test configuration
- [ ] **Prioritize Features**: Fokus implementasi fitur core (login, transfer, payment) dulu

#### Long Term (P3)
- [ ] **TDD Workflow**: Implementasi fitur baru harus barengan dengan test yang passing
- [ ] **Visual Regression**: Setup screenshot comparison untuk mendeteksi UI changes
- [ ] **Component Testing**: Tambahkan unit test untuk component sebelum E2E

---

### Evidence - Sample Error Logs

```
# Port Conflict - Wrong Title
Error: expect(page).toHaveTitle(expected) failed
Expected pattern: /PayU/
Received string:  "Grafana"

# Missing Elements
Error: expect(locator).toBeVisible() failed
Locator: locator('button:has-text("Optimasi Portofolio")')
Expected: visible
Timeout: 10000ms
Error: element(s) not found

# Count Mismatch
Error: expect(locator).toHaveCount(expected) failed
Locator:  locator('.bg-card.p-8.rounded-xl')
Expected: 3
Received: 0

# Accessibility Config
Error: frame.evaluate: Error: unknown rule `keyboard` in options.runOnly
```

---

### Verification Status (Revised Feb 9, 2026)

| Previous Claim | Audit Result | Verdict |
| :--- | :--- | :--- |
| "95%+ E2E Pass Rate" | **< 15% Pass Rate** | 🔴 **FALSE** |
| "Features Complete" | **Major Gaps: Investment, Lending, KYC, Bill Pay UI** | 🔴 **FALSE** |
| "Production Ready" | **48/100 — Blocked by Security, Testing, Feature gaps** | 🔴 **NOT READY** |
| "Hexagonal 100%" | **55% — Only 7/19 Java services compliant** | 🔴 **FALSE** |
| "Event-First" | **Starters built but 0 services use them** | 🔴 **FALSE** |
| "OpenShift Ready 91%" | **58% — No Helm, No TLS, No NetworkPolicy, No load tests** | 🔴 **OVERSTATED** |

---

| Priority | Bug ID | Description | Affected Services | Status |
| :--- | :--- | :--- | :--- | :--- |
| 🔴 **P0** | #P0-1 | Health Endpoints return 503 (Redis/Port/Seed issues) | auth, account, trans, wallet | ✅ FIXED |
| 🟡 **P1** | #P1-1 | Keycloak Admin Password Sync Issue | Keycloak | ✅ Workaround Docs |
| 🟢 **P2** | #P2-1 | Gateway Actuator 404 | Gateway | ⚪ Low Priority |
| 🔴 **P0** | #P0-2 | Service Down - Connection Refused | partner, compliance, ab-testing | ✅ FIXED |
| 🔴 **P0** | #P0-2a | Service Up but Redis DOWN | support | ✅ FIXED |
| 🔴 **P0** | #P0-3 | Redis Connection Failure | statement-service | ✅ FIXED |
| 🔴 **P0** | #P0-4 | Container Not Running | fx-service | ✅ FIXED |
| 🔴 **P0** | #P0-5 | Redis Connection Failure | billing-service | ✅ FIXED |
| 🟡 **P1** | #P1-2 | Port Conflict - AB Testing vs Lending | ab-testing-service | ✅ NO ISSUE |
| 🟡 **P1** | #P1-3 | CMS Content Endpoint 404 | cms-service | 🔴 OPEN |

---

## 📊 Reliability Audit Summary (Revised Feb 9, 2026)

| Category | Status | Pass Rate | Notes |
| :--- | :--- | :--- | :--- |
| **Infrastructure** | ✅ | 95% | 21/22 containers healthy |
| **API Endpoints** | ✅ | 95% | 20/21 services responding |
| **E2E Core Flows** | 🔴 | <15% | Auth issues partially fixed, feature gaps remain |
| **Health Probes** | ✅ | 95% | 20/21 services UP |
| **Unit Tests** | 🟡 | ~70% | Varies widely by service (6%-100%) |
| **Integration Tests** | 🔴 | ~55% | 6 services have 0 integration tests |
| **Load Tests** | 🔴 | 0% | No load test results available |
| **Security Tests** | 🔴 | N/A | Static only, no dynamic testing |
| **Contract Tests** | 🔴 | 0% | Not implemented |

---

## 🚢 OpenShift Deployment Readiness Audit (Feb 6, 2026)

### ✅ Compliant Areas

| Category | Status | Details |
| :--- | :--- | :--- |
| **Base Images** | ✅ | All services use `registry.access.redhat.com/ubi9/*` |
| **Non-root User** | ✅ | All Dockerfiles use `USER 185` (jboss) or `USER 1001` |
| **Health Endpoints** | ✅ | All services expose `/actuator/health/liveness` |
| **Resource Limits** | ✅ | All 26 OpenShift manifests have `resources:` defined |
| **OpenShift Manifests** | ✅ | Kustomize overlays ready (base, dev, staging, prod) |
| **Prometheus Metrics** | ✅ | 19 services have Micrometer configured |
| **Distributed Tracing** | ✅ | OTEL_ENDPOINT configured for Jaeger |
| **Routes** | ✅ | Gateway and Web-App Routes configured |
| **ConfigMaps/Secrets** | ✅ | 49 definitions in infrastructure/openshift/ |
| **NetworkPolicy** | ✅ | Service Mesh and Multi-region configs available |
| **PodDisruptionBudget** | ✅ | Defined in ArgoCD and Service Mesh configs |

### 🔴 Gaps to Address Before Production

| ID | Gap | Priority | Affected Services | Remediation |
| :--- | :--- | :--- | :--- | :--- |
| **OCP-001** | Hardcoded passwords in application.yml | **P0** | billing, partner, promotion, notification | ✅ Use `${DB_PASSWORD}` env var |
| **OCP-002** | Missing `.dockerignore` files | **P2** | ab-testing, backoffice, cms, fx-service | Copy from `backend/.dockerignore` template |
| **OCP-003** | Hardcoded localhost defaults without container profile | **P1** | 10+ services | Ensure `application-container.yml` overrides |
| **OCP-004** | Hardcoded JWT secret in partner-service | **P0** | partner-service | ✅ Move to Vault or OpenShift Secret |
| **OCP-005** | Python services missing OpenShift manifests | **P2** | kyc-service, analytics-service | Verify base/*.yaml exists |
| **OCP-006** | HPA (Horizontal Pod Autoscaler) not configured | **P2** | All services | Add HPA manifests with CPU/memory triggers |
| **OCP-007** | Service Mesh mTLS not enforced per-service | **P3** | All services | Add PeerAuthentication per namespace |
| **OCP-008** | Missing Liveness/Readiness separation | **P2** | Some services | Ensure separate probe endpoints |
| **OCP-009** | auth-service port mismatch (8002 in Dockerfile) | **P1** | auth-service | ✅ Standardize to 8080 |
| **OCP-010** | Missing API versioning headers | **P3** | All services | Add `X-API-Version` header support |

### 📊 OpenShift Readiness Score (Revised Feb 9, 2026)

| Component | Ready | Total | Percentage | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Backend Services** | 22 | 22 | ✅ 100% | All build & containerize |
| **Frontend Apps** | 1 | 1 | 100% | Container exists |
| **Infrastructure** | 20 | 28 | 71% | No Helm, No TLS, No NetworkPolicy |
| **Security** | 5 | 10 | 50% | ~~localStorage tokens~~✅, missing starters, no DAST |
| **Testing** | 4 | 10 | 40% | No load tests, E2E <15%, 0 contract tests |
| **Overall** | - | - | **🔴 58%** |

---

## 🛠️ Technical Debt Ledger

| ID | Description | Priority | Status |
| :--- | :--- | :--- | :--- |
| **TD-SEC-001** | JWT tokens in localStorage (XSS vuln) | **P0** | ✅ FIXED |
| **TD-ARCH-001** | events/outbox/saga starters dead code | **P0** | 🔴 OPEN |
| **TD-SEC-002** | Hardcoded credentials in VCS | **P0** | ✅ FIXED |
| **TD-TEST-001** | 0 tests on outbox-starter, saga-starter | **P0** | 🔴 OPEN |
| **TD-ARCH-002** | cms-service uses 0 shared starters | **P1** | 🔴 OPEN |
| **TD-ARCH-003** | Quarkus services can't use shared starters | **P1** | 🟡 OPEN |
| **TD-SEC-003** | SHA-256 key derivation (needs PBKDF2) | **P1** | 🟡 OPEN |
| **TD-TEST-002** | lending-service 0 integration tests | **P1** | 🔴 OPEN |
| **TD-TEST-003** | fx-service 0 integration tests | **P1** | 🔴 OPEN |
| **TD-FE-001** | 7 backend services have no frontend | **P1** | 🟡 OPEN |
| **TD-INFRA-001** | Helm charts directory empty | **P1** | 🔴 OPEN |
| **TD-INFRA-002** | No NetworkPolicies in OpenShift | **P1** | 🔴 OPEN |
| **TD-WEB-001** | LCP Optimization (9.3s → <2.5s) | **P2** | Backlog |
| **TD-ARCH-004** | 8 services lack hexagonal architecture | **P2** | 🟡 OPEN |
| **TD-TEST-004** | No contract tests (Pact/SCC) | **P2** | 🟡 OPEN |
| **TD-TEST-005** | Security tests static only (no DAST) | **P2** | 🟡 OPEN |
| **TD-MOB-001** | Duplicate State Management (Zustand/RQ) | **P2** | ✅ COMPLETE |
| **TD-CORE-001** | Replace Lombok with Manual Code | **P1** | ✅ COMPLETE |
| **TD-ARCH-005** | Protobuf/gRPC for Internal Comms | **P4** | Proposed |

### TD-MOB-001 Implementation Details

**Problem**: Duplicate state management between Zustand and TanStack Query (React Query) causing state synchronization issues and unnecessary complexity in the mobile app. Both stores were managing the same server state (user, cards, transactions) leading to inconsistent UI and hard-to-debug state issues.

**Solution**: Implemented clear separation of concerns:
- **TanStack Query**: Server state (API data, caching, synchronization)
- **Zustand**: UI state only (theme, language, selections, view preferences)
- **SecureStore**: Token storage (encrypted, never in state)

**Changes Made**:

#### 1. Store Refactoring
- **`store/authStore.ts`**: Deprecated for auth state, now only UI preferences (`lastLoginAttempt`, `biometricPromptEnabled`)
- **`store/cardStore.ts`**: Renamed to `cardUIStore.ts`, now only UI state (`selectedCardId`, `cardViewMode`, `showCardDetails`)
- **`store/uiStore.ts`**: Unchanged - already correct (theme, language, UI preferences)
- **`store/index.ts`**: Created centralized exports with clear documentation

#### 2. Unified Hooks
- **`hooks/useAuth.ts`**: Refactored to use TanStack Query for auth state, Zustand for UI preferences
- **`hooks/useCards.ts`**: Refactored to use TanStack Query for card data, Zustand for selection state
- **`hooks/index.ts`**: Created unified exports combining TanStack Query and custom hooks

#### 3. Context Updates
- **`context/AuthContext.tsx`**: Updated to use `useAuthState` and `useInitializeAuth` from TanStack Query

#### 4. Test Updates
- **`store/__tests__/authStore.test.ts`**: Updated to test only UI state
- **`store/__tests__/cardUIStore.test.ts`**: Created for new card UI store
- **`store/__tests__/cardStore.test.ts`**: Removed (old combined store)

#### 5. Documentation
- **`docs/STATE_MANAGEMENT.md`**: Created comprehensive architecture documentation

**Files Modified**:
| File | Change |
|------|--------|
| `store/authStore.ts` | Deprecated auth state, kept UI preferences only |
| `store/cardStore.ts` | Renamed to `cardUIStore.ts`, UI state only |
| `store/cardUIStore.ts` | New file with expanded UI state |
| `store/index.ts` | New centralized exports |
| `hooks/useAuth.ts` | Refactored to use TanStack Query |
| `hooks/useCards.ts` | Refactored to use TanStack Query |
| `hooks/index.ts` | New unified exports |
| `context/AuthContext.tsx` | Updated to use TanStack Query |
| `store/__tests__/authStore.test.ts` | Updated for UI-only state |
| `store/__tests__/cardUIStore.test.ts` | New test file |
| `store/__tests__/cardStore.test.ts` | Deleted |
| `docs/STATE_MANAGEMENT.md` | New documentation |

**Architecture Pattern**:
```
Server State (API Data)
    ↓
TanStack Query (caching, synchronization)
    ↓
Custom Hooks (unified interface)
    ↓
Components

UI State (Preferences)
    ↓
Zustand (persistence)
    ↓
Components
```

**Verification**:
- TypeScript compilation passes
- No duplicate state management
- Clear separation of concerns
- Security maintained (tokens never in state)
- Backward compatibility preserved through unified hooks

**Migration Path for Developers**:
```typescript
// OLD (deprecated)
import { useAuthStore } from '@/store/authStore';
const { user, login } = useAuthStore();

// NEW (recommended)
import { useAuth } from '@/hooks/useAuth';
const { user, login } = useAuth();
```

---

## 📜 Milestone Lifecycle Archive

| Phase | Milestone Name | Progress | Status | Completion Highlights |
| :--- | :--- | :--- | :--- | :--- |
| **P16** | Web App UX Standardization | 100% | ✅ | Emerald Design System, Shadcn. |
| **P14** | Persistence Hardening | 100% | ✅ | Flyway verified across 22 services. |
| **P9** | Event-Driven Architecture | 100% | ✅ | Sagas, Outbox pattern. |
| **P7** | Docker → Podman Migration | 100% | ✅ | Quadlet files created. |
| **P3** | Backend API Documentation | 100% | ✅ | OpenAPI 3.0 implementation. |
| **P0** | Web App Prod Readiness | 100% | ✅ | TS 0 errors, Security hardened. |

---

## 📋 Platform Inventory

### ☕ Backend Microservices (22)
- **Core**: `account`, `auth`, `wallet`, `transaction`
- **Financial**: `investment`, `lending`, `fx`, `statement`
- **Ops**: `billing`, `notification`, `compliance`, `backoffice`
- **AI/ML**: `kyc`, `analytics` (Python 3.12)
- **Platform**: `gateway`, `api-portal`, `cms`, `ab-testing`, `partner`, `promotion`, `support`

### 📱 Client Applications
- **Digital Banking Web**: Next.js 15+, Tailwind, TanStack Query.
- **Digital Banking Mobile**: Expo SDK 52, React Native.
- **Partner Portal**: Next.js, Developer Docs.

---
_Last Updated: February 9, 2026 | PayU Engineering Team — Full Platform Audit by AI Agent_
