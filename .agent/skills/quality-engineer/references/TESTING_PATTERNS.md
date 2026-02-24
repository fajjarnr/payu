# Testing Patterns & Quality Engineering

## 🚀 Load Testing (K6)
*   **CRUD Load Testing**: Don't just ping health checks. Test real-world writes/reads.
    *   **Modular Library**: Use reusable handlers in `lib/` (e.g., `lib/auth.js`, `lib/wallet.js`).
    *   **Consistency Checks**: Implement read-after-write and atomicity validation under load.
*   **Baseline Testing**: Every service needs a baseline test.
    *   **SLAs**: 
        *   Core (auth, wallet): p95 < 300ms
        *   Financial: p95 < 500ms
        *   Analytics: p95 < 800ms
    *   **Error Rate**: Must be < 0.1% for production readiness.
*   **Weighted Operations**: Realistic load should be ~40% Read, ~25% Create, ~20% Transfer, ~15% Other.

## 🧪 Integration & Contract Testing
*   **Integration Tests**: Minimum requirement for financial services (lending, fx). Use `@Testcontainers` with PostgreSQL/Kafka.
*   **Contract Testing (Spring Cloud Contract/Pact)**:
    *   Critical pairs: `transaction`↔`wallet`, `transaction`↔`account`.
    *   Use Groovy DSL (readable) or Pact JSON.
    *   Convention: `tests/contract/<provider-service>/`.
*   **AutoConfiguration Testing**: Use `ApplicationContextRunner` for isolated starter tests. Boots in ~200ms (5s faster than `@SpringBootTest`).

## 🎭 E2E Testing (Playwright)
*   **Authentication Fixtures**: Use `test.extend` to automatically set `accessToken` and `payu_session` cookies. Avoid logging in for every test.
*   **Network Idle**: Always `await page.waitForLoadState('networkidle')` after navigation to handle hydration.
*   **Port Configuration**: Point `baseURL` to the containerized app (e.g., `localhost:3001`). Disable Playwright's handled `webServer` in CI if containers are pre-started.
*   **Accessibility (Axe)**: Run automated scans for contrast and semantic HTML.
*   **Stable Selectors**: Favor `data-testid` over text content or fragile CSS paths.

## 🛡️ Security & Mutation Testing
*   **DAST (OWASP ZAP)**: Run automated scans via Podman. Baseline (passive) in CI, Full (active) in staging.
*   **Mutation Testing (PITest)**: Set thresholds (60% mutation, 70% coverage). Exclude config and application bootstrap classes.
*   **False Positive Detection**: Always `grep` the codebase before fixing a roadmap issue. Stale items in `TODOS.md` are common.
