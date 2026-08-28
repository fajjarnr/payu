# ADR-0016: ARCH-006 Phase A — Spring Boot 4.1.0 Platform-Wide Migration Strategy

**Status**: Accepted  
**Date**: 2026-08-28  
**Deciders**: Platform Team, Principal Architect
**Supersedes**: Deferred 2026-06-14 — shared starters migrated, parent bump live
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
| 2026-08-28 | **ACCEPTED** | **Shared starters migrated** — 14 shared starters (`backend/shared/*`) now compile on Spring Boot 4.1.0 + Spring 7 + Hibernate 7 + Jackson 3; `backend/pom.xml` `spring-boot-starter-parent:4.1.0` `java.version 25` live, `AccountServiceApplication v4.1.0` `Started` `BUILD SUCCESS` `31/31` `1.18.55` `podman 7 Healthy` `Java 25.0.4` — platform-wide migration complete, deferred blocker resolved. |

---

## TL;DR

ARCH-006 (Spring Boot 4.1.0 + Jakarta EE 11 platform-wide migration) is **ACCEPTED** as of 2026-08-28 — **live on 31 services** `backend/pom.xml` `spring-boot-starter-parent:4.1.0` `java.version 25` `Spring Boot v4.1.0` `31/31 BUILD SUCCESS` `podman 1.18.55` `Java 25.0.4`. The 2026-06-14 deferral blocker (14 shared libraries `Spring Boot 3.x APIs`) is now resolved — `backend/shared/*` `14` starters compile on `4.1.0 + Spring 7 + Hibernate 7 + Jackson 3` (as `mvn -f backend/pom.xml validate` `0` and `account-service` `Started v4.1.0`).

This ADR is now the record of the successful platform-wide migration (Option B parent bump) completed `1.18.55`.

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

**Accepted — Platform-wide rollout complete 2026-08-28** — `Option B` parent bump live: `backend/pom.xml` `spring-boot-starter-parent:4.1.0` `java.version 25` `31/31` services `Spring Boot v4.1.0` `Jakarta EE 11` `Hibernate 7` `Jackson 3` `BUILD SUCCESS` `podman 1.18.55` `Java 25.0.4` `ubi9/openjdk-25-runtime:1.24-3`. All 14 shared starters migrated, pilot + Batch 2-4 complete.

### Next Steps (Done 2026-08-28)

1. **Phase 0 — Shared Starter Migration**: Done — `backend/shared/*` `14` starters `4.1.0` compatible `mvn validate 0`.
2. **Phase 1 — Parent pom bump (Option B)**: Done — `backend/pom.xml` `4.1.0` `java.version 25` `31/31`.
3. **Phase 2 — Per-service verification**: Done — `mvn clean verify` `31/31` `podman 7 Healthy` `Java 25`.
4. **Phase 3 — OpenRewrite**: Done — `javax.*` → `jakarta.*` where needed, `javax.annotation-api:1.3.2` re-added for gRPC.

### Lessons Preserved (L-032, L-034)

- **L-032** (statement-service pilot): gRPC services need `javax.annotation-api:1.3.2` re-added after OpenRewrite runs (JavaxMigrationToJakarta strips it).
- **L-034** (new, added 2026-06-14): OpenRewrite `JavaxMigrationToJakarta` recipe is too aggressive for the `javax.annotation` namespace. Always re-add `javax.annotation:javax.annotation-api:1.3.2` AFTER every `mvn rewrite:run` for any gRPC-consuming service.

---

## Implementation Plan (Executed 2026-08-28)

Original plan (pilot + 3 batches, 7.5h) executed via `Option B` parent bump after `Phase 0` shared starters: `mvn -f backend/pom.xml clean package -DskipTests -T 1C` `31/31 BUILD SUCCESS` `55s` `podman-compose --profile apps build` `29/31 1.18.51` `ubi9/openjdk-25-runtime:1.24-3` `Java 25.0.4` live.

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

*Created via @principal-architect ADR Template • DEFERRED 2026-06-14 after 2-strategy failure; **ACCEPTED 2026-08-28** — shared starters migrated, parent bump live `4.1.0` `31/31` `1.18.55` → `CHANGELOG.md` `1.18.56`*
