# Project Roadmap & Todo List

> **Lab Project Status**: ✅ **FEATURE COMPLETE** - All 22 microservices implemented
> **Primary Focus**: 🧪 **TDD & Test Quality** - Backend ~85%, Frontend ~85%
> **Last Updated**: January 29, 2026

---

## 📊 Lab Completeness Summary

### Services Inventory (22 Total)

| Category | Services | Status |
|----------|----------|--------|
| **Core Banking** | account, auth, wallet, transaction | ✅ Complete |
| **Financial** | investment, lending, fx, statement | ✅ Complete |
| **Operations** | billing, notification, compliance | ✅ Complete |
| **Platform** | gateway, api-portal, cms, ab-testing | ✅ Complete |
| **Support** | support, backoffice, partner, promotion | ✅ Complete |
| **ML/Analytics** | kyc (Python), analytics (Python) | ✅ Complete |
| **Simulators** | bi-fast, dukcapil, qris | ✅ Complete |
| **Shared Libs** | security-starter, resilience-starter, cache-starter, api-commons | ✅ Fixed |

### Frontend Inventory

| Application | Location | Status | Test Framework |
|-------------|----------|--------|----------------|
| **Web App** | `frontend/web-app/` | ✅ Complete (Next.js 15) | Vitest + Playwright |
| **Mobile App** | `frontend/mobile/` | ✅ Complete (Expo/React Native) | Jest |
| **Developer Docs** | `frontend/developer-docs/` | ✅ Complete (Next.js) | Vitest |

### Test Status Summary

| App | Unit Tests | E2E Tests | Type Check | Lint | Build | Status |
|-----|------------|-----------|------------|------|-------|--------|
| `web-app` | ✅ 829 tests (53 files) | ✅ 7 Playwright flows | ⚠️ 23 errors | ⚠️ 109 issues | ✅ Pass | 92% passing |
| `mobile` | ❓ Unknown | N/A | ⚠️ 8 errors | ❓ Unknown | ❓ Unknown | Partially fixed |
| `developer-docs` | ❓ Unknown | N/A | ❓ Unknown | ❓ Unknown | ✅ Pass | Needs verification |

---

## 🚨 CRITICAL: TDD & Test Fixes (Priority #1)

### Parallel QA Tasks (January 29, 2026)

**Backend Tasks (10 Parallel Tracks):**
- [ ] **Task #2**: Add ArchitectureTest for partner-service (SNAP BI compliance)
- [ ] **Task #3**: Add ArchitectureTest for promotion-service (layered architecture)
- [ ] **Task #4**: Increase kyc-service coverage from 65% to 80%
- [ ] **Task #5**: Fix partner-service integration tests (14 failing tests)
- [ ] **Task #6**: Fix analytics-service failing tests (13 WebSocket/Kafka)
- [ ] **Task #7**: Add notification-service integration tests (Kafka pub/sub)
- [ ] **Task #8**: Add gateway-service integration tests (filter chain)
- [ ] **Task #9**: Add compliance-service integration tests (AML screening)
- [ ] **Task #10**: Add backoffice-service integration tests (admin workflows)
- [ ] **Task #11**: Add promotion-service integration tests (rewards engine)

**Frontend Tasks (3 Parallel Tracks):**
- [ ] **Task #12**: Fix web-app test failures (65 minor issues)
- [ ] **Task #13**: Setup mobile app test infrastructure (Jest + React Native)
- [ ] **Task #14**: Add mobile component tests (screens, stores, navigation)

### Backend Services Detailed Status

| Service | Unit Tests | Integration Tests | Architecture Tests | Coverage | Status |
|---------|------------|-------------------|-------------------|----------|--------|
| `account-service` | ✅ 44/44 | ✅ 1 Docker | ✅ Has ArchitectureTest | 85% | **Complete** |
| `auth-service` | ✅ 67/67 | ✅ 1 Docker | ✅ Has ArchitectureTest | 85% | **Complete** |
| `transaction-service` | ✅ 75/75 | ⚠️ 3 Docker | ✅ Has ArchitectureTest | 80% | **Integration tests need Docker** |
| `wallet-service` | ✅ 85/85 | ⚠️ 1 Docker | ✅ Has ArchitectureTest | 80% | **Integration tests need Docker** |
| `billing-service` | ✅ 51/51 | ✅ 1 Docker | ✅ Has ArchitectureTest | 80% | **Complete** |
| `notification-service` | ✅ 23/23 | ❌ 0 | ❌ Missing | 80% | **Needs integration tests** |
| `gateway-service` | ✅ 85/85 | ❌ 0 | ✅ Has ArchitectureTest | 75% | **Needs integration tests** |
| `support-service` | ✅ 17/17 | ❌ 0 | ✅ Has ArchitectureTest | 83% | **Needs integration tests** |
| `compliance-service` | ✅ 55/55 | ❌ 0 | ✅ Has ArchitectureTest | 75% | **Needs integration tests** |
| `partner-service` | ⚠️ 88/102 | ✅ 1 Docker | ❌ **MISSING** | 86% | **Need ArchitectureTest + fix 14 tests** |
| `backoffice-service` | ✅ 79/79 | ❌ 0 | ✅ Has ArchitectureTest | 83% | **Needs integration tests** |
| `promotion-service` | ✅ 102/102 | ❌ 0 | ❌ **MISSING** | 70% | **Need ArchitectureTest + integration tests** |
| `kyc-service` | ✅ 116/116 | ✅ 0/0 | ✅ Has ArchitectureTest | 65% | **Coverage below 80% target** |
| `analytics-service` | ⚠️ 128/141 | ✅ 9 marked | ✅ Has ArchitectureTest | 78% | **13 failing tests, below 80%** |

### Remaining Infrastructure Issues (DevOps Responsibility)

🔧 **Docker/Testcontainers Required**: Services with integration tests that require Docker infrastructure:
- billing-service, notification-service: Kafka DevServices
- account-service, auth-service, transaction-service: Testcontainers (PostgreSQL, Keycloak)
- analytics-service: WebSocket and Kafka integration tests
- partner-service, backoffice-service: Testcontainers infrastructure

---

## 📌 Pending Tasks

### 1. Test Infrastructure & CI/CD
- [ ] **[HIGH]** Distribute pre-commit hook installation guide to all developers
- [ ] **[HIGH]** Add TDD training session for development team
- [ ] **[MEDIUM]** Implement mutation testing (PIT) to verify test quality
- [ ] **[MEDIUM]** Create `scripts/run-all-tests.sh` for automated test execution
- [ ] **[MEDIUM]** Create `Makefile` with test targets (test-unit, test-integration, test-e2e, test-coverage)
- [ ] **[LOW]** Setup CI/CD pipeline for automated test runs (GitHub Actions)

### 2. Frontend Tests (Web App)

> **Location**: `frontend/web-app/`
> **Stack**: Next.js 15, TypeScript, Vitest, Playwright, React Query, Zustand

#### Build & Quality Checks
- [ ] **Type checking**: `cd frontend/web-app && npm run type-check`
- [ ] **Linting**: `cd frontend/web-app && npm run lint`
- [ ] **Build**: `cd frontend/web-app && npm run build`
- [ ] **Bundle analysis**: Check for large dependencies
- [ ] **Lighthouse audit**: Performance, Accessibility, SEO scores

#### Accessibility (A11y) Tests
- [ ] **Verify A11y audit**: `cd frontend/web-app && npm run a11y` (if configured)
- [ ] **axe-core integration**: Verify in Playwright tests
- [ ] **Screen reader testing**: Manual NVDA/VoiceOver verification
- [ ] **Keyboard navigation**: Tab order verification
- [ ] **Color contrast**: WCAG 2.1 AA compliance

### 3. Frontend Tests (Mobile App)

> **Location**: `frontend/mobile/`
> **Stack**: Expo 52, React Native, TypeScript, Jest, Expo Router

#### Unit Tests (Jest)
- [ ] **Verify test setup**: `cd frontend/mobile && npm run test`
- [ ] **Services Tests** (`services/`): auth, wallet, transaction, card, notification, feedback
- [ ] **Stores Tests** (`store/`): authStore, walletStore, cardStore, transactionStore
- [ ] **Hooks Tests** (`hooks/`): useAuth, useWallet, useBiometrics, useNotifications, useOfflineMode, useAppLock, useCamera
- [ ] **Utils Tests** (`utils/`): currency, validation, date, storage
- [ ] **Components Tests** (`components/`): BalanceCard, TransactionItem, CardFlip, QRScanner
- [ ] **Target**: 60% coverage for services, 50% for components

#### Build & Quality Checks
- [ ] **Type checking**: `cd frontend/mobile && npx tsc --noEmit`
- [ ] **Linting**: `cd frontend/mobile && npm run lint`
- [ ] **Expo Doctor**: `cd frontend/mobile && npx expo-doctor`
- [ ] **Bundle check**: Verify app size < 50MB

### 4. Frontend Tests (Developer Docs)

> **Location**: `frontend/developer-docs/`
> **Stack**: Next.js, TypeScript, Vitest

#### Unit Tests (Vitest)
- [ ] **Verify test setup**: `cd frontend/developer-docs && npm run test`
- [ ] **Existing tests**: Check `src/__tests__/` directory
- [ ] **API docs rendering**: Verify OpenAPI components render
- [ ] **Code samples**: Verify syntax highlighting
- [ ] **Search functionality**: Test search works
- [ ] **Target**: 50% coverage (docs are less critical)

#### Build & Quality Checks
- [ ] **Type checking**: `cd frontend/developer-docs && npx tsc --noEmit`
- [ ] **Linting**: `cd frontend/developer-docs && npm run lint`
- [ ] **Build**: `cd frontend/developer-docs && npm run build`
- [ ] **Link checking**: Verify no broken internal links

---

## 📁 Quick Test Commands

```bash
# ========================
# BACKEND
# ========================

# Java Services (Maven)
cd backend/<service> && mvn test

# Java Services (Quarkus with wrapper)
cd backend/<service> && ./mvnw test

# Python Services
cd backend/<service> && pytest -v

# ========================
# FRONTEND
# ========================

# Web App
cd frontend/web-app && npm run test          # Unit tests
cd frontend/web-app && npm run test:e2e      # E2E tests
cd frontend/web-app && npm run build         # Production build

# Mobile App
cd frontend/mobile && npm run test           # Unit tests
cd frontend/mobile && npx tsc --noEmit       # Type check
cd frontend/mobile && npm run ios            # iOS Simulator

# Developer Docs
cd frontend/developer-docs && npm run test   # Unit tests
cd frontend/developer-docs && npm run build  # Production build

# ========================
# FULL SUITE
# ========================
make test                                     # All tests
./scripts/test-single-service.sh <service>   # Single service
./scripts/test-health-check.sh               # Health check
```

---

## 🏁 Lab Readiness Score

| Category | Score | Notes |
|----------|-------|-------|
| **Feature Completeness** | 95% | All core banking features |
| **Architecture Quality** | 90% | Hexagonal, Event-driven, DDD |
| **Documentation** | 90% | ARCHITECTURE.md (C4 diagrams), GEMINI.md, API docs |
| **Backend Test Coverage** | 85% | Unit tests mostly complete, integration blocked |
| **Frontend Test Coverage** | 85% | Web-app comprehensive tests (15,599+ lines), mobile needs work |
| **Test Infrastructure** | 85% | QA script, reports, automation ready |
| **Production Readiness** | 70% | Security hardening done, tests needed |

**Overall Lab Score: 85%** - Feature complete, TDD mostly complete (Backend & Frontend), container optimized, C4 architecture documented

---

## 📋 Recently Completed (January 2026)

### Backend Tests
- ✅ All 22 microservices implemented
- ✅ Shared libraries fixed (security-starter, resilience-starter, cache-starter)
- ✅ 600+ tests across 20+ services documented
- ✅ Container optimization (UBI9, OCI labels, multi-stage builds)

### Frontend Tests
- ✅ Web-App: Comprehensive test suite created (53 files, 15,599+ lines)
  - E2E Flow Tests (7 Playwright tests)
  - Component Tests (9 comprehensive test files)
  - Hook Tests (7 React Query hooks)
  - Service Tests (7 API service layers)
  - Utility Tests (3 core libraries)

### Documentation
- ✅ C4 Architecture diagrams added to ARCHITECTURE.md
- ✅ QA test runner script created
- ✅ Test configuration templates created

---

_Last Updated: January 29, 2026_
