# Implementation Plan - P0 Fixes & E2E Stabilization

## Goal Description
Fix remaining P0 (Production Readiness) issues identified during E2E testing on February 1, 2026. This includes fixing accessibility violations in the Web App and unblocking the full-stack backend build for comprehensive E2E testing.

## Proposed Changes

### 1. Web App Accessibility Fixes (Priority 1)
- [ ] **Login Page**:
    - Add proper `<title>` and `meta` description for SEO and Accessibility.
    - Fix color contrast issues on the submit button and secondary links.
    - Add ARIA landmarks (header, main, footer).
- [ ] **Onboarding Page**:
    - Add proper `<title>`.
    - Fix color contrast on progress indicators.
    - Add ARIA labels to form inputs.
- [ ] **Layout**:
    - Ensure all pages have a `main` landmark.
    - Standardize page titles using a common component or hook.

### 2. Backend Build Context Fixes (Priority 2)
- [ ] **Containerfiles Optimization**:
    - Update Containerfiles to support building from the `backend/` root context.
    - Ensure `shared` libraries and parent `pom.xml` are correctly copied.
- [ ] **Docker Compose Update**:
    - Update `docker-compose.test.yml` to use `backend/` as the build context for all services.
    - Point to the relative `Dockerfile` (Containerfile) path.

### 3. E2E Test Stabilization
- [ ] **Timeout Adjustments**:
    - Increase Playwright timeout from 30s to 60s for slow UBI9 environment.
- [ ] **API Mocking (Partial)**:
    - Add MSW or similar to mock critical failure points if backend is still flaky.

## Verification Plan

### Automated Tests
- [ ] **Playwright Accessibility Audit**:
    - Run `npx playwright test a11y-audit.spec.ts` and verify it passes.
- [ ] **Backend Image Build**:
    - Run `podman build -f backend/wallet-service/Containerfile ./backend` to verify fix.
- [ ] **Full-Stack Run**:
    - Run `podman-compose -f docker-compose.test.yml up -d` and check health logs.

### Manual Checks
- [ ] Open Login page in browser and verify title in tab.
- [ ] Inspect Login page with DevTools and verify ARIA landmarks.
