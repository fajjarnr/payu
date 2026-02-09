---
description: |
  Workflow untuk menjalankan remediasi P0/P1 dari hasil P19 Full Platform Audit.
  Gunakan workflow ini untuk menyelesaikan 5 P0 blockers dan membawa production readiness dari 48% ke 80%.
updated: 2026-02-09
---

# 🔧 P19 Remediation Workflow

Workflow ini adalah panduan langkah-demi-langkah untuk memperbaiki semua temuan audit P19.
**Target: 48% → 80% production readiness** dalam 84 Story Points.

## Pre-requisites

- Baca `.agent/context/P19-AUDIT-STATUS.md` untuk status terkini
- Baca `docs/guides/REMEDIATION_PLAYBOOK.md` untuk detail implementasi setiap remedy
- Baca `docs/guides/LESSONS.md` untuk pattern yang benar

---

## Phase 1: P0 Blockers (25 SP, Sprint 1)

### Step 1: R-001 — JWT → httpOnly Cookies via BFF (8 SP)

**Agents**: `@frontend-architect` + `@cybersecurity-architect`

1. Create `frontend/web-app/app/api/auth/login/route.ts` (BFF proxy)
2. Create `frontend/web-app/app/api/auth/refresh/route.ts` (BFF refresh)
3. Create `frontend/web-app/app/api/proxy/[...path]/route.ts` (BFF generic proxy)
4. Update `frontend/web-app/src/lib/api.ts` — remove ALL `localStorage` token calls
5. Update `frontend/web-app/src/stores/authStore.ts` — use cookie-based auth
6. Add `credentials: 'include'` to all fetch calls
7. **Verify**: `grep -r "localStorage" frontend/web-app/src/` returns 0 results

### Step 2: R-002 — Integrate Outbox Starter (5 SP)

**Agents**: `@integration-architect` + `@core-banking-engineer`

1. Add `outbox-starter` dependency to `transaction-service/pom.xml`
2. Add `outbox-starter` dependency to `wallet-service/pom.xml`
3. Add Flyway migration `V*__add_outbox_events_table.sql` to each service
4. Replace direct `kafkaTemplate.send()` with `outboxRepository.save()`
5. Configure Debezium CDC connector for each database
6. **Verify**: `grep -r "kafkaTemplate.send" backend/transaction-service/` returns 0 results

### Step 3: R-003 — Vault + Env Var for All Secrets (3 SP)

**Agents**: `@cybersecurity-architect` + `@platform-engineer`

1. Replace all hardcoded passwords in `infrastructure/keycloak/payu-realm-export.json` with `${KEYCLOAK_*}` vars
2. Replace all hardcoded passwords in `docker-compose.yml` with `${POSTGRES_PASSWORD}` etc.
3. Create `.env.example` with placeholder values
4. Add `.env` to `.gitignore`
5. Remove hardcoded IP `13.212.248.122` — use `${CORS_ALLOWED_ORIGINS}`
6. **Verify**: `grep -rn "P@ssw0rd\|payu_secret\|admin.*admin" infrastructure/ docker-compose.yml` returns 0 results

### Step 4: R-004 — Integration Tests for Critical Services (5 SP)

**Agents**: `@tester` (primary)

1. Write `OutboxStarterIntegrationTest.java` in `backend/shared/outbox-starter/`
2. Write `SagaStarterIntegrationTest.java` in `backend/shared/saga-starter/`
3. Write `LoanDisbursementIntegrationTest.java` in `backend/lending-service/`
4. Write `ExchangeRateIntegrationTest.java` in `backend/fx-service/`
5. **Verify**: `mvn test -pl shared/outbox-starter,shared/saga-starter,lending-service,fx-service`

### Step 5: R-005 — Fix Port Conflict (1 SP)

**Agents**: `@platform-engineer`

1. Change `api-portal-service` port mapping in `docker-compose.yml` from `8099:8080` to `8100:8080`
2. Update any references to port 8099 for api-portal
3. **Verify**: `docker-compose config | grep "8099"` shows only keycloak

### Step 6: R-006 — Integrate Shared Starters into Underserved Services (3 SP)

**Agents**: `@core-banking-engineer`

For each of: `cms-service`, `ab-testing-service`, `statement-service`:
1. Add `security-starter` dependency to `pom.xml`
2. Add `resilience-starter` dependency to `pom.xml`
3. Add `cache-starter` dependency to `pom.xml`
4. Configure `application.yml` with Keycloak properties
5. Add `@PreAuthorize` to all sensitive endpoints
6. **Verify**: `mvn compile` succeeds for each service

---

## Phase 2: P1 Architecture & Testing (37 SP, Sprint 2-3)

### R-007: Quarkus Security (8 SP)
**Agents**: `@core-banking-engineer` + `@cybersecurity-architect`
- Option A: Create Quarkus-compatible security extension
- Option B: Migrate gateway, notification, api-portal to Spring Boot

### R-008: Hexagonal Refactor for 8 Services (13 SP)
**Agents**: `@core-banking-engineer` + `@scaffolding-expert`
- Services: support, promotion, backoffice, billing, partner, investment, lending, fx
- Follow checklist in `core-banking-engineer` SKILL.md § "Hexagonal Refactoring Checklist"

### R-009: Fix E2E Playwright Tests (8 SP)
**Agents**: `@tester` + `@frontend-architect`
- Create proper auth fixture
- Skip unimplemented feature tests
- Fix selector mismatches
- Target: >70% pass rate

### R-010: Restrict Remote Image Domains (2 SP)
**Agents**: `@frontend-architect`
- Whitelist CDN domains in `next.config.ts`

### R-011: Mobile Feature Completion (6 SP)
**Agents**: `@mobile-architect`
- Implement biometric auth
- Complete placeholder screens

---

## Phase 3: P2 Hardening (22 SP, Sprint 4)

- R-012: Structured logging (5 SP) — `@platform-engineer`
- R-013: Gatling load tests (8 SP) — `@tester`
- R-014: Pact contract tests (5 SP) — `@tester`
- R-015: OpenShift readiness probes (2 SP) — `@platform-engineer`
- R-016: Documentation cleanup (2 SP) — `@principal-architect`

---

## Verification Protocol

After EACH remedy is implemented:

1. Run the verify command listed in the remedy step
2. Update the service score in `.agent/context/P19-AUDIT-STATUS.md`
3. Mark the remedy as complete in `docs/roadmap/TODOS.md`
4. Update production readiness percentage
5. Commit with conventional commit: `fix(service): R-XXX description`

## Completion Criteria

Production readiness ≥ 80% verified by:
- [ ] All P0 blockers resolved (0 Critical findings)
- [ ] All services use security-starter (or equivalent)
- [ ] Outbox-starter integrated in transaction + wallet services
- [ ] Integration tests exist for all financial services
- [ ] E2E pass rate > 70%
- [ ] No hardcoded credentials in VCS
- [ ] No localStorage token storage
