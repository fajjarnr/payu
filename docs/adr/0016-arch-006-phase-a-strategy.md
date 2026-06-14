# ADR-0016: ARCH-006 Phase A — Spring Boot 4.1.0 Platform-Wide Migration Strategy

**Status**: Deferred (2026-06-14) — see [Decision Log](#decision-log)
**Date**: 2026-06-14
**Deciders**: Platform Team, Principal Architect
**Supersedes**: None
**Related**: [ARCH-006 in TODOS.md](../roadmap/TODOS.md), [L-032 in LESSONS.md](../guides/LESSONS.md), [statement-service pilot](https://github.com/fajjarnr/payu/commit/526e480b)

---

## Decision Log

| Date | Status | Notes |
|:--|:--|:---|
| 2026-06-14 | Proposed | Initial 3-option analysis (A: per-service, B: parent bump, C: profile) |
| 2026-06-14 | Accepted A | User approved Option A (per-service dep mgmt override, pilot pattern) |
| 2026-06-14 | Pilot executed | wallet-service migrated; mvn clean verify green with READY-033 (ThemeResolver) workaround |
| 2026-06-14 | Pilot extended | Batch 2 (5 services) attempted; auth-service revealed fundamental Option A limitation: `spring-cloud-vault-config:5.0.0` from spring-cloud 2025.1.0 requires Spring Boot 4.0+ classes, but Option A keeps parent at Boot 3.5.14 (mixed BOMs in service classpath) |
| 2026-06-14 | Switched to B | User chose to switch to Option B (parent pom bump) |
| 2026-06-14 | B blocked | Option B revealed 4 shared starters (jms, rest-client, events, saga) using Spring Boot 3.x APIs (package locations, method overrides) that no longer exist in Spring Boot 4.1.0 + Spring 7 + Hibernate 7. **Shared libraries MUST be migrated FIRST** before any service can use Boot 4.1.0 |
| 2026-06-14 | DEFERRED | User chose to revert + defer ARCH-006 until shared starter migration is completed (separate workstream, ~2-3 days effort). L-032/L-034 lessons preserved. |

---

## TL;DR

ARCH-006 (Spring Boot 4.1.0 + Jakarta EE 11 platform-wide migration) is **deferred** as of 2026-06-14. The pilot and 2 attempted rollout strategies both revealed that the migration's true blocker is the **14 PayU shared libraries** (cache-starter, security-starter, outbox-starter, jms-starter, rest-client-starter, events-starter, saga-starter, etc.) — they all use Spring Boot 3.x APIs and would need a separate ~2-3 day effort to migrate before the parent pom can be bumped.

This ADR is preserved as a record of the strategic options explored. Future ARCH-006 work should start with shared starter migration, then re-evaluate Option B.

---

## Why Both Option A and Option B Failed

### Option A (per-service dep mgmt override) — failed at scale
- **What works**: For services WITHOUT spring-cloud-vault/spring-cloud-config dependencies. The 3.5.14 BOM (inherited from parent) provides the missing artifacts (rest-assured, starter-aop, etc.) alongside the 4.1.0 BOM override.
- **What fails**: For services using `spring-cloud-vault-config` (auth-service and likely 8+ others), the spring-cloud 2025.1.0 (Oakwood) brings `spring-cloud-vault-config:5.0.0` which has hardcoded `requires spring-boot:4.0+`. With mixed BOMs in classpath, the 3.5.14 spring-boot artifacts win and the vault config class signatures mismatch.

### Option B (parent pom bump) — failed at shared starters
- **What works**: Centralized migration, all services get 4.1.0 in one place.
- **What fails**: 4 of 14 shared starters (jms, rest-client, events, saga) immediately break with:
  - `package org.springframework.boot.actuate.health does not exist` (jms-starter)
  - `RestClientErrorHandler method override mismatch` (rest-client-starter, Spring 7 RestClient changed signature)
  - `package com.fasterxml.jackson.datatype.jsr310 does not exist` (events-starter)
  - `package org.springframework.boot.autoconfigure.domain does not exist` (saga-starter)
  - `cannot access org.hibernate.query.BindableType` (saga-starter, Hibernate 7 moved class)
- **Fix**: Each shared starter needs its own Spring Boot 4.1.0 + Spring 7 + Hibernate 7 + Jackson 3 API audit. ~2-3 days effort, orthogonal to service-level migration.

---

## Decision (Final)

**Defer ARCH-006 platform-wide rollout** until shared starter migration is completed. Pilot services (statement-service, wallet-service if retained) remain on Boot 4.1.0. Other 10 services stay on Boot 3.5.14.

### Next Steps (for when ARCH-006 resumes)

1. **Phase 0 — Shared Starter Migration (NEW prerequisite)**: Migrate all 14 shared starters in `backend/shared/` to be Spring Boot 4.1.0 + Spring 7 + Hibernate 7 + Jackson 3 compatible. This is a separate workstream.
2. **Phase 1 — Parent pom bump (Option B)**: Once shared starters compile on 4.1.0, apply Option B (single parent pom change, no per-service overrides needed).
3. **Phase 2 — Per-service verification**: Run `mvn -f backend/pom.xml test-compile -T 1C` and `mvn clean verify` per service.
4. **Phase 3 — OpenRewrite (B platform-wide)**: Run OpenRewrite recipes to migrate any remaining `javax.*` → `jakarta.*` and deprecated Boot 3.x APIs.

### Lessons Preserved (L-032, L-034)

- **L-032** (statement-service pilot): gRPC services need `javax.annotation-api:1.3.2` re-added after OpenRewrite runs (JavaxMigrationToJakarta strips it).
- **L-034** (new, added 2026-06-14): OpenRewrite `JavaxMigrationToJakarta` recipe is too aggressive for the `javax.annotation` namespace. Always re-add `javax.annotation:javax.annotation-api:1.3.2` AFTER every `mvn rewrite:run` for any gRPC-consuming service.

---

## Implementation Plan (Cancelled)

The original implementation plan (1 service pilot + 5 in Batch 2 + 4 in Batch 3 + 2 in Batch 4, ~7.5 hours total) is cancelled. When ARCH-006 resumes, the plan should be rebuilt starting with the shared starter migration phase.

---

## Open Questions (Closed)

| # | Question | Resolution |
|:--|:---------|:-----------|
| Q1 | Option A, B, atau C? | Tried A → failed at scale; tried B → failed at shared starters; deferred |
| Q2 | First service to pilot | wallet-service (gRPC-tested) — completed (commit reverted) |
| Q3 | F-2 fix strategy | Replace `spring-boot-starter-aop` with `aspectjweaver` per-service (pilot); pin to 3.5.14 in parent dep mgmt (Option B) |
| Q4 | ARCH-006 closed after 12/12 migrated? | N/A — deferred |
| Q5 | Image tag policy | N/A — deferred |
| Q6 | Worktree branch | Force-reverted to main HEAD on 2026-06-14 |

---

*Created via @principal-architect ADR Template • DEFERRED 2026-06-14 after 2-strategy failure revealing shared starter prerequisite*
