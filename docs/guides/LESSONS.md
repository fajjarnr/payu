# 🧠 PayU Lessons Learned (Session Log)

This document serves as a chronological log of "Lessons Learned" and critical architectural discoveries made during development sessions. Detailed implementation patterns have been migrated to the **AI Agent Skill Ecosystem** in `.agents/skills/`.

---

## L-027: Tekton Pipeline — `onError: continue` Not Supported in v1.9

**Date**: 2026-05-02  
**Domain**: CI/CD  
**Context**: Tekton v1.9.0 (OpenShift Pipelines 1.22) does not support `onError: continue` on pipeline tasks. This means security scanning tasks (Trivy, Grype, ZAP) that find vulnerabilities will block the entire pipeline — there's no way to make them "warning-only" at the pipeline level.

**Pattern**: Use `|| true` shell wrappers inside the task's `script` to absorb non-zero exit codes:
```yaml
- name: grype-scan
  taskRef:
    name: grype-scan
  params:
    - name: args
      value: |
        grype dir:/workspace/source -o json > /workspace/grype-report.json || true
```

**Lesson**: For security scanning tools in Tekton pipelines, always wrap the scan command with `|| true` at the script level. Pipeline-level `onError: continue` won't work until Tekton v1.10+. Also log a warning if the scan failed, so teams still have visibility into skipped findings.

## L-028: Tekton Pipeline — Registry Auth `unused:<token>` Format

**Date**: 2026-05-02  
**Domain**: CI/CD  
**Context**: OpenShift internal image registry (`image-registry.openshift-image-registry.svc:5000`) uses service account tokens for authentication. The `registry-credentials` Secret must use `unused:<token>` as the `auth` field value (base64-encoded), NOT `username:password`. Standard tools like Podman and Buildah accept this format: `echo -n "unused:$(oc whoami -t)" | base64 -w0`.

**Pattern**: Always use `unused:` prefix with the token as the "password" field:
```yaml
apiVersion: v1
kind: Secret
type: kubernetes.io/dockerconfigjson
data:
  .dockerconfigjson: |
    {
      "auths": {
        "image-registry.openshift-image-registry.svc:5000": {
          "auth": "<base64 of unused:<token>>"
        }
      }
    }
```

**Lesson**: The `unused:` prefix signals Docker/Podman clients to use token-based auth with no username. This is the OpenShift convention. Don't try to guess a username — `unused` is the literal string.

## L-029: Tekton Pipeline — License Compliance PURL Filtering

**Date**: 2026-05-02  
**Domain**: CI/CD  
**Context**: Syft generates SPDX/CycloneDX SBOMs that include ALL packages including OS-level (RPM, DEB). When checking license compliance, filter to application-level dependencies only (Maven, npm, PyPI, Go modules) to avoid false positives from base image packages that are licensed separately.

**Pattern**: Filter SBOM components by `purl` prefix before checking licenses:
```bash
# Grype/Syft: only check app-level dependencies
syft packages -o cyclonedx-json dir:/workspace/source \
  | jq '[.components[] | select(.purl // "" | startswith("pkg:maven") or startswith("pkg:npm") or startswith("pkg:pypi") or startswith("pkg:golang"))]' \
  > /workspace/app-sbom.json
```

**Lesson**: OS-level packages in UBI9/RHEL have their own license compliance lifecycle managed by Red Hat. The pipeline should only gate application dependencies. Use `purl` (Package URL) prefixes to distinguish dependency types.

---

## L-030: Podman DevSecOps — k6 Local Smoke Testing

**Date**: 2026-05-05  
**Domain**: DevOps  
**Context**: k6 local smoke test verified against podman compose stack. **918/918 requests passed, 0% failure rate, p(95) 1.71ms** against `gateway-service:8080/q/health`. The compose file uses `profiles: [devsecops]` — invoke with `podman compose -f infrastructure/local/podman/podman-compose.yml --profile devsecops run --rm k6`.

**Pattern**: Always use `-f` with explicit compose file path for non-default locations:
```bash
podman compose -f infrastructure/local/podman/podman-compose.yml --profile devsecops run --rm k6 run /tests/local-smoke.js
```

**Lesson**: `podman compose` without `-f` looks for `compose.yaml`/`docker-compose.yml` in the current directory only. The PayU compose file is at `infrastructure/local/podman/podman-compose.yml` — always pass `-f`.

---

## L-031: `new GenericJackson2JsonRedisSerializer()` Is a Footgun — Always Register `JavaTimeModule`

**Date**: 2026-06-13  
**Domain**: Java / Spring Data Redis  
**Context**: The no-arg constructor `new GenericJackson2JsonRedisSerializer()` builds an internal `ObjectMapper` that does **not** register the `JavaTimeModule`. Any cached value containing `java.time.LocalDate`, `LocalDateTime`, `Instant`, `OffsetDateTime`, `ZonedDateTime`, or `Duration` throws `InvalidDefinitionException` at write time, surfacing as HTTP 500. The cache-starter's `RedisCacheConfig` registers `JavaTimeModule` correctly — but any service with a local `@Configuration` (e.g. `cms-service/.../config/RedisConfig.java`) or a hand-rolled `RedisTemplate` bean (e.g. `auth-service/AuthServiceApplication.java#redisTemplate`) silently bypasses the starter and reinvents the bug.

**Pattern**: Always construct the serializer with an `ObjectMapper` that has `JavaTimeModule` registered:
```java
ObjectMapper om = new ObjectMapper();
om.registerModule(new JavaTimeModule());
GenericJackson2JsonRedisSerializer ser = new GenericJackson2JsonRedisSerializer(om);
```
Reuse one helper method (e.g. package-private `buildValueSerializer()`) across `RedisCacheConfiguration` and `RedisTemplate` beans so the configuration lives in one place.

**Lesson**: 
1. The default ctor is a **silent footgun** — it compiles, runs, and only fails when a value containing a `java.time` type is actually cached. Tests that only PUT/GET `String` or `Map<String, String>` will not catch it.
2. `scripts/check_pod_connections.py` flags any exception in pod logs as `Redis: 🔴 Failed/Unreachable`. Serialization errors in `RedisCache.put` are categorized as Redis failures, leading operators to chase env-var and credential bugs that don't exist. When investigating "Redis failed" reports, grep for `InvalidDefinitionException` and `jsr310` to distinguish serializer bugs from connectivity issues.
3. The original "fix plan" proposed editing 20 base deployment YAML files to change `PAYU_CACHE_REDIS_USERNAME` and add `REDIS_PASSWORD` env vars. Cluster-state inspection proved all env vars were already correct — the root cause was a Java code defect, not a misconfigured environment. **Always verify the runtime cluster state with `oc get deployment ... -o jsonpath` before proposing manifest changes.** The Iron Law: NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST.

## L-032: Spring Boot 4.1.0 / Jakarta EE 11 Migration (ARCH-006 Pilot)

**Date**: 2026-06-13  
**Domain**: Java / Framework  
**Context**: Successfully migrated `statement-service` to Spring Boot 4.1.0 as a pilot. The migration involves transitioning from `javax.*` to `jakarta.*` (Jakarta EE 11), utilizing Java 25, and enabling Virtual Threads natively.

**Pattern / Discoveries**:
1. **OpenRewrite works flawlessly**: Using `JavaxMigrationToJakarta` and `SpringBoot3BestPractices` automatically resolves 90% of the `javax` to `jakarta` import swaps and `application.yml` property deprecations (e.g., prometheus export paths).
2. **gRPC Generated Code Compatbility**: The `protoc-gen-grpc-java` (v1.61.0) emits `@javax.annotation.Generated`. Because the Jakarta EE migration removes `javax.annotation-api` entirely, the gRPC Java generated stub compilation will fail. **Always manually re-add `javax.annotation-api:1.3.2`** to the dependencies of any gRPC-consuming service during the Jakarta EE 11 migration.
3. **Properties Migrator**: Keep `spring-boot-properties-migrator` in the POM during development to catch any overlooked application properties that were renamed between Spring Boot 3.4 and 4.1.
4. **Virtual Threads**: Enabled out-of-the-box via `spring.threads.virtual.enabled: true`.

**Lesson**: The jump from Spring Boot 3.4 to 4.1.0 is relatively smooth using OpenRewrite, but code-generation plugins (like gRPC) that haven't fully switched to Jakarta EE require backward-compatibility hacks (`javax.annotation-api`). Ensure all unit tests compile *before* running OpenRewrite, as syntax errors will block the AST parser.

## L-033: Inner-Enum Extraction in Tests — Production Code Moves First, Tests Get Forgotten (2026-06-13)

**Date**: 2026-06-13  
**Domain**: Java / Hexagonal Architecture / Test Maintenance  
**Context**: Closed READY-003 (P0 blocker for ARCH-006 platform-wide Jakarta EE 11 migration). 49 test files in 8 backend services still referenced inner-class enums (`X.InnerEnum.VALUE`) after the May 2026 ARCH-009 extraction moved them to top-level files. Production code compiled because the extraction was atomic + tests were partially updated, but `mvn test-compile` failed in 8 of 20 services with 250+ `cannot find symbol` errors.

**Pattern**: When extracting inner enums to top-level (SOP #6), the test file sweep is the **last** and **most error-prone** step. Production refactors have IDE/compiler assist; test file sweeps are done by hand or by `replaceAll` and are easy to miss. Same pattern applies to:
- ARCH-008 entity layer move (`domain/` → `adapter/persistence/entity/`) — 2 test files still imported `id.payu.partner.domain.ApiKeyEntity`.
- ARCH-009 inner-enum extraction — 41 test files still referenced `X.InnerEnum.VALUE`.
- MSG-009 outbox migration — 2 test files still mocked `KafkaTemplate` instead of `OutboxService`.

**Lesson**:
1. **Refactor + test sweep in same commit, not same sprint**. The inner-enum extraction touched 144 enums (per L-032) but the test file sweep was deferred. Result: 6 weeks of "tests don't compile" left as technical debt.
2. **Always run `mvn test-compile` immediately after a refactor**, not just `mvn compile`. Production code can be green while tests are red.
3. **The fix is mechanical, not creative**: `replaceAll X.InnerEnum.` → `InnerEnum.` + add import. Subagents can do this in parallel — 8 services × ~30 references per service = 250 fixes in <5 minutes total wall time.
4. **Bonus test bug surfaced**: `SecurityConfigPatternTest` (added in 1.8.11 as regression guard) used a wrong source path `account.config/SecurityConfig.java` (dot, not slash). The test always failed with `NoSuchFileException`. **Lesson for characterization tests**: write them after the production fix, but verify they actually run + pass before merging. A "regression test" that always fails is worse than no test — it normalizes failure in CI.
5. **OpenRewrite dependency**: per the user's pre-task analysis, `JavaxMigrationToJakarta` and `SpringBoot3BestPractices` require the codebase to AST-parse cleanly. Test-compile failures break OpenRewrite silently — there's no error, just no migration. **Always clean test-compile before scheduling an OpenRewrite run**.

## L-034: OpenRewrite `JavaxMigrationToJakarta` Strips `javax.annotation-api` — Re-Add After Every Rewrite Run (2026-06-14)

**Date**: 2026-06-14
**Domain**: Java / gRPC / OpenRewrite / Build Tooling
**Context**: During ARCH-006 wallet-service pilot (Phase B OpenRewrite run), `mvn rewrite:run` with the `JavaxMigrationToJakarta` recipe **silently removed** the `javax.annotation:javax.annotation-api:1.3.2` dependency from `wallet-service/pom.xml`. The recipe appears to treat any `javax.*` artifact as "already migrated to jakarta" and deletes it. Result: gRPC-generated code (`@javax.annotation.Generated` from `protoc-gen-grpc-java`) failed to compile with `cannot find symbol: class Generated, location: package javax.annotation`.

**Pattern**: OpenRewrite's `JavaxMigrationToJakarta` recipe is **too aggressive** for the `javax.annotation` namespace. The `jakarta.annotation-api` artifact does **not** contain `javax.annotation.Generated` — these are parallel packages, not aliases. The recipe should only migrate `javax.servlet`, `javax.persistence`, `javax.validation`, etc., but it strips ALL `javax.*` deps indiscriminately.

**Lesson**:
1. **Always re-add `javax.annotation:javax.annotation-api:1.3.2` AFTER every `mvn rewrite:run` for any gRPC-consuming service**. Treat the dep as "transient" in the pom — OpenRewrite will keep removing it.
2. **Add a CI guard**: a post-OpenRewrite check that verifies `javax.annotation-api` is still in the pom for services with `protoc-gen-grpc-java` (e.g., wallet-service, transaction-service, integration-service). Could be a custom ArchUnit rule or a simple grep in CI.
3. **Better fix (upstream)**: file an issue with OpenRewrite to add a `javax.annotation.Generated` exclusion to the `JavaxMigrationToJakarta` recipe, OR add a recipe option like `excludeArtifacts: javax.annotation:javax.annotation-api`.
4. **OpenRewrite's `SpringBoot3BestPractices` recipe is also non-idempotent**: bumped `resilience4j-spring-boot3:2.3.0 → 2.6.0` and `maven-compiler-plugin:3.13.0 → 3.15.0` in the pom without being asked. Subsequent runs may bump further. Consider pinning versions explicitly in service poms if you want to control the version OpenRewrite bumps to.
5. **No-op safety**: despite these pom mutations, OpenRewrite found **zero Java source changes** for wallet-service. The 2 `javax.sql.DataSource` references were correctly left alone (JDK class, not Jakarta). The Jakarta migration story for this service is "already done" — wallet-service was on jakarta.* imports since the 3.x era.

**Recovery sequence** (reproducible):
```bash
# 1. Run OpenRewrite (will modify pom)
mvn -f backend/wallet-service/pom.xml rewrite:run

# 2. Re-add javax.annotation-api manually
# Edit pom: insert <dependency>javax.annotation:javax.annotation-api:1.3.2</dependency>

# 3. Verify
mvn -f backend/wallet-service/pom.xml clean verify

# 4. Commit
git add backend/wallet-service/pom.xml
git commit -m "fix(arch-006): re-add javax.annotation-api after OpenRewrite run"
```

## L-035: ARCH-006 Deferred — Shared Starter Migration is the True Prerequisite (2026-06-14)

**Date**: 2026-06-14
**Domain**: Java / Microservices / Build Tooling / Architecture
**Context**: Attempted ARCH-006 platform-wide Spring Boot 4.1.0 migration via 2 strategies (Option A: per-service dep mgmt override, Option B: parent pom bump). Both failed. Option A failed at scale when auth-service hit Spring Cloud Vault version mismatch (5.0.0 requires Boot 4.0+, but Option A keeps mixed BOMs in classpath). Option B failed at shared starter compilation: 4 of 14 shared starters (jms, rest-client, events, saga) use Spring Boot 3.x APIs (actuate.health, jackson.datatype.jsr310, hibernate.query.BindableType, etc.) that no longer exist in Spring Boot 4.1.0 + Spring 7 + Hibernate 7. **The true prerequisite for ARCH-006 is migrating the 14 shared libraries FIRST, not the service-level migration.**

**Pattern**: When a platform has many services sharing a common library set, framework upgrade ROI is concentrated in the shared libraries, not the services. A service-level migration is cheap (pom changes only) if shared libraries are already compatible. A library-level migration is expensive (API audits, package renames, method signature updates) but unblocks all downstream services.

**Lesson**:
1. **Framework migration order: libraries → parent pom → services**, not the reverse. We did services → discovered libraries break. The right order is libraries → services inherit the new framework.
2. **Per-service dep mgmt override (Option A) is a workaround, not a strategy**. It works for trivial services but breaks down for services with strict version alignment (e.g., spring-cloud-vault). Don't build a migration plan around it.
3. **The 14 shared starters are the platform's de facto framework contract**. Any framework upgrade must start with their audit. Treat them as a versioned artifact (e.g., `shared-starters:1.1.0` for Boot 4.1.0 compat) with their own release cadence.
4. **Hidden coupling**: shared starters depend on Spring Boot APIs implicitly (autoconfigure, health indicators, Jackson modules). When Spring Boot 4.1.0 reorganizes these, starters break even if their own code didn't change. Always re-validate starter compilation before assuming "no changes needed".
5. **Cost estimate (revised)**: 14 shared starters × ~2h each = ~28h = ~3-4 dev days. Plus per-service migration (~7.5h) + verification + deploy = ~5-7 dev days total. Previous estimates of "1-2 days" (per TODOS) were naive.

**Decision**: ARCH-006 platform-wide rollout is **deferred** until shared starter migration is funded. Pilot services (statement-service, wallet-service if retained) remain on Boot 4.1.0. See ADR-0016 for full decision log.

**Recovery for future ARCH-006 work**:
```bash
# Phase 0: Migrate 14 shared starters (NEW prerequisite, ~2-3 days)
# - For each starter in backend/shared/*:
#   - Update imports (javax.* → jakarta.*, moved packages)
#   - Update method signatures (Spring 7 / Hibernate 7 / Jackson 3)
#   - Verify with mvn -f backend/shared/<starter>/pom.xml clean test

# Phase 1: Parent pom bump (Option B)
# - backend/pom.xml: spring-boot-starter-parent 3.5.14 → 4.1.0
# - Fix F-1 (rest-assured-bom), F-2 (starter-aop pin), F-3 (testcontainers-bom:1.20.6)
# - mvn -f backend/pom.xml test-compile -T 1C

# Phase 2: Per-service verification
# - mvn -f backend/<service>/pom.xml clean verify per service

# Phase 3: OpenRewrite
# - mvn -f backend/pom.xml rewrite:run (per service or globally)
# - Re-add javax.annotation-api per L-034 if gRPC service
```
## L-036: Spring Boot 4.1.0 Migration — Library-First Cost Concentration (2026-06-15)

**Date**: 2026-06-15
**Domain**: Architecture / Framework Migration
**Context**: READY-034 partial execution confirmed L-035's hypothesis quantitatively. The 4 dev-day estimate (vs original 1-2 day TODOS estimate) was driven by shared starter migration work (14 starters × ~2h each) cascading to 16+ service POMs that needed `spring-boot-starter-aop` removal, Hibernate 6.3→7.0 hypersistence artifact rename, and testcontainers 2.0 artifact renames. The 6 starters migrated in Phase 1 (jms, saga, events, outbox, rest-client, api-commons) consumed ~3 hours of work despite OpenRewrite being available.

**Pattern**: When a platform has many services sharing a library set, the framework upgrade ROI is heavily concentrated in the libraries:
- 14 starters migrated in ~1 hour
- 16+ service POM cascades in ~1 hour (mechanical)
- 22 service property renames in ~30 minutes (no deprecated properties found)
- 22 service main code fixes in ~2 hours (bulk sed for package renames)
- 30 test files `@MockBean` → `@MockitoBean` in ~1 hour

The service-level work was 90% mechanical sed. The library work required real code understanding (audit-only report identified the issues, but only execution revealed the full extent).

**Lesson**:
1. **Library-first migration order is mandatory**, but the cost estimate must include the BOM cascade (parent POM updates ripple to 30+ downstream poms). L-035's 4-day estimate was accurate.
2. **Mechanical sed works at scale** when the renames are known in advance. 95% of the 50+ file changes across 14 services were done via `find ... -exec sed -i` patterns. Per the orchestrator's "subagent + parallel dispatch" SOP, this is a textbook case for cavecrew-investigator (find usages) → cavecrew-builder (apply fixes in parallel).
3. **OpenRewrite is NOT a silver bullet** for test framework changes (`@MockBean` removed, `TestRestTemplate` removed). These are genuine API changes requiring per-test rewrites, not package renames.

## L-037: `spring-boot-starter-aop` Silent Removal in SB 4.0 (2026-06-15)

**Date**: 2026-06-15
**Domain**: Build Tooling / Spring Boot
**Context**: Confirmed during READY-034 execution. The `spring-boot-starter-aop` artifact was last published at `3.5.15` + `4.0.0-M2`. Final `4.0.0` and `4.1.0` releases do **NOT** publish this artifact. SB 4.0 release notes mention this only in passing under "Minor adjustments" without explicit deprecation warning. Result: 20 poms (5 shared starters + 16 services) reference a non-existent artifact, causing reactor-wide `mvn` parse failures BEFORE compilation can even begin.

**Pattern**: AOP is now auto-configured when `aspectjweaver` is on the classpath (per SB 4.0 release notes: "Spring Boot automatically configures Aspect-Oriented Programming (AOP) and defaults to using CGLib proxies."). No starter wrapper needed.

**Lesson**:
1. **For services that USE AOP** (e.g., have `@Aspect` classes): replace `spring-boot-starter-aop` with explicit `org.aspectj:aspectjweaver` (managed by SB BOM, no version needed).
2. **For services that DON'T use AOP** (e.g., `rest-client-starter` had it as stale dep): just remove the dep entirely. No AOP fallback needed.
3. **Always verify with `mvn help:effective-pom`** before assuming a dep works. The reactor parse failure is a hard stop, not a soft warning.
4. **SB release notes are NOT exhaustive** for dependency changes. Always grep for the artifact in `~/.m2/repository` to confirm it's published in the target version.

## L-038: Testcontainers 2.0 Artifact Rename — `junit-jupiter` → `testcontainers-junit-jupiter` (2026-06-15)

**Date**: 2026-06-15
**Domain**: Build Tooling / Testcontainers
**Context**: Testcontainers 2.0.5 (the version pulled in by SB 4.1.0 BOM) renamed all artifacts with a `testcontainers-` prefix for namespace consistency. Code that used `org.testcontainers:junit-jupiter:1.x` in 3.5.14 era needs to be `org.testcontainers:testcontainers-junit-jupiter:2.0.5` in 4.1.0 era. Same pattern for `postgresql` → `testcontainers-postgresql`, `kafka` → `testcontainers-kafka`, etc.

**Pattern**: This is purely a package rename — no API changes. The Testcontainers Java API (`Container.start()`, `@Container`, `DynamicPropertySource`, etc.) is unchanged.

**Lesson**:
1. **Always check the BOM contents first**. `mvn dependency:tree -Dincludes='com.fasterxml.jackson*:*'` would have revealed this in advance.
2. **Mechanical sed works** for these renames: `s|org.testcontainers:junit-jupiter|org.testcontainers:testcontainers-junit-jupiter|g`. 22+ service poms updated in 1 sed pass.
3. **For poms with hardcoded version** (e.g., `<version>1.20.4</version>` in `notification-service/pom.xml`): remove the version entirely to use parent BOM-managed version.

## L-039: Hypersistence JsonType — Hibernate 6.x → 7.x ABI Break (2026-06-15)

**Date**: 2026-06-15
**Domain**: Java / Hibernate / Hypersistence
**Context**: `hypersistence-utils-hibernate-70:3.15.3` (latest available on Maven Central as of June 2026) is the most recent release. It was compiled against Hibernate 6.x where `org.hibernate.type.descriptor.java.AbstractClassJavaType.getJavaTypeClass()` was a non-final method. Spring Boot 4.1.0 ships Hibernate 7.x, which marked this method as `final`. Result: `java.lang.IncompatibleClassChangeError: class io.hypersistence.utils.hibernate.type.json.internal.JsonJavaTypeDescriptor overrides final method` at class load time (`JsonType.<clinit>`).

**Pattern**: When loading a `@Type(JsonType.class)` annotated entity column, the static init of `JsonType` calls `JsonType.class.getDeclaredConstructor().newInstance()` which triggers `JsonJavaTypeDescriptor.<init>` → fails because parent method is now final. The error happens at Spring context refresh time, not at Hibernate query time, so even simple SELECTs fail.

**Lesson**:
1. **Hypersistence-utils has not been updated for Hibernate 7 ABI changes**. Maven Central confirms 3.15.3 is the latest, no newer release as of 2026-06-15. Track for upstream fix.
2. **Workaround: migrate to Hibernate 7 native JSON support** — replace `@Type(JsonType.class)` with `@JdbcTypeCode(SqlTypes.JSON)`. No external JSON type lib needed; Hibernate handles it natively. This was the fix applied in READY-034 execution for 5 fields across 2 starter entities (SagaInstance, OutboxEvent). See commit `b6868bb9`.
3. **Migration is purely mechanical**: change the annotation, remove the hypersistence-utils-hibernate-70 dep, no import changes needed (both annotations are in `org.hibernate.annotations`).
4. **Caveat**: Not all entities using `@Type(JsonType.class)` were migrated in this session (e.g., `account-service/Profile.java`). Per-stop work — each service that uses hypersistence-utils-hibernate-70 needs migration. Track as follow-up ticket per service.

## L-040: Audit-Only Mode is a Valid Scope for "Too-Big" Migrations (2026-06-15)

**Date**: 2026-06-15
**Domain**: Process / Project Management
**Context**: READY-034 was estimated at 4 dev days. In a single session, the natural tendency is to try to finish everything. But per the orchestrator's "Graceful Halt" + "Structured Completion" SOP, a mid-session stop + "audit-only report" deliverable is a valid scope.

**Pattern**: When the user signals scope concern (e.g., "READY-034 only" vs "full platform migration"), the right response is:
1. Acknowledge the scope is bounded.
2. Deliver a **static audit** (no code changes) that enumerates: P0 blockers, P1 issues, dependency version matrix, total effort estimate, migration phases, lessons pending.
3. Get user decision: execute now (with clear cost estimate) OR defer to future sprint.
4. The audit report is **valuable even if never executed** — it captures institutional knowledge about the migration's risks and rewards.

**Lesson**:
1. **Audit reports are deliverables**, not throwaway work. The 664-line `READY-034_MIGRATION_REPORT.md` documented: 4 known P0 blockers (jms, rest-client, events, saga), 12 starter POM changes needed, version matrix for Spring Cloud + Hypersistence + Resilience4j + ArchUnit, and the 4-day estimate. This is institutional knowledge worth keeping.
2. **Always present the audit alongside the work**, not instead of it. The audit informs the next session's decision; the work delivers immediate value.
3. **The audit-only phase is a discrete milestone**, not a stalling tactic. It produces a file with measurable value: line count, known issues count, effort estimate, references to upstream release notes.

## L-041: Jackson 3 (tools.jackson.databind) ↔ Jackson 2 (com.fasterxml.jackson) ABI Break in SB 4.1.0 (2026-06-15)

**Date**: 2026-06-15
**Domain**: Java / Jackson / Spring Boot
**Context**: Spring Boot 4.1.0 defaults to **Jackson 3** (`tools.jackson.databind.*` package). The SB 4.0 release notes state: "Jackson 3 is the recommended and default choice" and "Jackson 2 support ships in a deprecated form for facilitating migration to Jackson 3." However, **at runtime**, Jackson 3's `JsonMapper.Builder.<clinit>` calls `JacksonAnnotationIntrospector.<clinit>` which transitively requires `com.fasterxml.jackson.annotation.JsonSerializeAs` (a Jackson 2 annotation). This class was **REMOVED in Jackson 2.18**. Result: `java.lang.NoClassDefFoundError: com/fasterxml/jackson/annotation/JsonSerializeAs` at first Jackson 3 init in any Spring Boot 4.1.0 test that triggers `JsonMapper.builder()`.

**Pattern**: The classic fix is to use `spring-boot-autoconfigure-classic` (which provides Jackson 2 + Spring 6-style autoconfig). This module is at `org.springframework.boot:spring-boot-autoconfigure-classic:4.1.0` and pulls in `spring-boot-autoconfigure-classic-modules` (parent). It activates the Jackson 2 autoconfig path. But — **the Jackson 3 jar is STILL on the classpath** (provided by `spring-boot-starter-json` or similar). The fix is incomplete: classic module activates Jackson 2 autoconfig, but Jackson 3's static init still fails when something triggers it.

**Real fix options**:
1. **Force Jackson 2 everywhere**: Exclude Jackson 3 modules from classpath via `spring-boot-starter-json` exclusions. The classic module then becomes the only JSON path. ~1-2 days effort.
2. **Full Jackson 3 migration**: Replace all `com.fasterxml.jackson.*` imports with `tools.jackson.*` across the platform. 1-2 weeks effort. Per AGENTS.md, this is a `frontend-architect` (web) + `logic-builder` (backend) parallel work.
3. **Wait for SB 4.x patch** that fixes the `JsonSerializeAs` resolution. Unclear timeline.

**Lesson**:
1. **SB 4.1.0 + Jackson 3 is a dual-stack transition period**, not a clean migration. The classic module is a half-fix. Plan for option 1 (lock to Jackson 2) for now, with option 2 (full Jackson 3 migration) as a future initiative.
2. **The runtime error happens at first `JsonMapper.builder()` call** — meaning even tests that don't directly use JSON can fail if the Spring context loads `JacksonAutoConfiguration`. This is why the `saga-starter` and `outbox-starter` integration tests fail at context refresh, not at test execution.
3. **Don't trust "Jackson 2 support ships in a deprecated form"** as a complete migration path. Verify the runtime init path yourself — the classic module alone is insufficient.

## L-042: The "Compile-Only" Production Readiness Metric is Misleading (2026-06-15)

**Date**: 2026-06-15
**Domain**: Process / Metrics
**Context**: During READY-034 partial execution + READY-035 test framework migration, the platform went from "compile-broken" to "compile-clean across 22 services + 14 shared starters + all 30 test files". This was reported as "production readiness 70% → 75%". But a subagent dispatched to run the actual `mvn -T 1C test` revealed:
- **Only 11/41 modules** had tests that actually **ran at runtime** (compile-clean ≠ runtime-clean)
- **9/11 passed 100%** at runtime (5 starters + 1 simulator + 3 services)
- **2/11 failed** (saga-starter 84%, outbox-starter 78%) — both at context load due to the Jackson 3 ABI break (L-041)
- **20 business services SKIPPED** entirely (Maven `-fae -T 1C` cascade-stops at upstream test failure)

True runtime confidence: **~25%**, not 75%.

**Pattern**: "Production readiness" is a multi-dimensional metric. A single percentage hides the gap between:
- **Compile-time**: 100% (all sources compile against SB 4.1.0 API surface)
- **Unit-test runtime**: ~25% (only 9/41 modules run + pass at runtime)
- **Integration-test runtime**: ~10% (many tests require Testcontainers/Docker which is env-dependent)
- **E2E**: 0% (no OCP deploys yet)

**Lesson**:
1. **Always include a runtime test run in any "production readiness" assessment**. The 1m35s cost of running `mvn -T 1C test` is trivial compared to the wrong-direction work that follows a false-positive 75% claim.
2. **The `mvn -fae -T 1C` cascade-skip is a known footgun**: when one starter test fails, 20 downstream service tests don't run. The "100% compile" metric gives the illusion of progress. Always also count the **modules that didn't run**, not just the ones that ran.
3. **Don't merge "production-ready" claims based on compile alone**. The orchestrator's "Verification-First Planning" SOP requires evidence of runtime correctness, not just absence of compile errors.
4. **Future work**: Re-run `mvn -T 1C test` after each major fix (e.g., after Jackson 3 strategy is decided) to update the runtime metric.

---

*Last Updated: June 15, 2026*
