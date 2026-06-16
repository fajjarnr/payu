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

## L-041: Jackson 3 ↔ Jackson 2 Annotation Version Mismatch in SB 4.1.0 (CORRECTED) (2026-06-15)

**Date**: 2026-06-15 (corrected)
**Domain**: Java / Jackson / Spring Boot
**Context**: Spring Boot 4.1.0 defaults to **Jackson 3** (`tools.jackson.databind.*` package). The SB 4.0 release notes state: "Jackson 3 is the recommended and default choice" and "Jackson 2 support ships in a deprecated form for facilitating migration to Jackson 3." At runtime, Jackson 3's `JsonMapper.Builder.<clinit>` calls `JacksonAnnotationIntrospector.<clinit>` which transitively requires `com.fasterxml.jackson.annotation.JsonSerializeAs`.

**THE BUG (original misdiagnosis)**: Initial analysis claimed "`JsonSerializeAs` was REMOVED in Jackson 2.18". **This was wrong**.

**THE ACTUAL ROOT CAUSE**: `JsonSerializeAs` was **ADDED in Jackson 2.21** specifically to support Jackson 3's annotation introspection. Verification (`unzip -l jackson-annotations-2.{17,18,21}.jar | grep JsonSerializeAs`):
- 2.17.x: NOT present
- 2.18.x: NOT present
- 2.21+: PRESENT

The Jackson 3.1.4 BOM explicitly pins `<jackson.version.annotations>2.21</jackson.version.annotations>` (comment: "latest 2.x at time of 3.x minor version is released"). Our parent pom had `<jackson.version>2.18.6</jackson.version>` which overrode the BOM-managed annotation jar to an older version that lacked the class Jackson 3 needs.

**THE FIX (1 line + cleanup)**: Remove the entire `<jackson.version>` property + the explicit Jackson dependency management block from parent pom. Let Spring Boot 4.1.0's `jackson-2-bom:2.21.4` (auto-imported via `spring-boot-dependencies`) manage all Jackson 2 artifact versions. Result:
- jackson-core → 2.21.4 (from SB BOM)
- jackson-databind → 2.21.4 (from SB BOM)
- **jackson-annotations → 2.21** (from SB BOM, has `JsonSerializeAs`)
- jackson-datatype-jsr310 → 2.21.4 (from SB BOM)

**Verification**: `mvn test` on saga-starter went from 23 errors (all `NoClassDefFoundError: JsonSerializeAs` at context refresh) to 146/146 PASS. Same for outbox-starter (83/83 PASS). Cascade unblocked 20+ downstream service tests.

**Lesson**:
1. **Never assume "class removed" without verifying the artifact directly**. The fix was actually "class added in newer version, you need to upgrade". Use `unzip -l` or `javap` against the actual jar in `~/.m2/repository` to confirm class presence before forming a hypothesis.
2. **SB 4.1.0 ships a `jackson-2-bom` import** that pins all Jackson 2 artifacts correctly for Jackson 3 compat. **Never override `jackson.version` in a parent pom unless you also bump `jackson-annotations` to a compatible version**. The two artifacts have asymmetric versioning (annotations releases independently as `2.x`, core releases as `2.x.y`).
3. **`spring-boot-autoconfigure-classic` is for AUTOCONFIG fallback, not Jackson version pinning**. It provides Jackson 2-style autoconfig (e.g., `ObjectMapper` bean if `spring-boot-jackson2` is also added) but doesn't fix annotation version mismatches.
4. **Stakeholder decision is moot if root cause is wrong**: The original "Option A (force Jackson 2) vs Option B (full Jackson 3 migration)" framing in READY-036 became unnecessary once the actual root cause (annotation version) was identified. The fix is neither — it's removing an incorrect override.

## L-043: Resilience4j 2.4.0 + Spring Boot 4.1.0 — `resilience4j-spring-boot4` Module Required + Transitive Cascade (2026-06-15)

**Date**: 2026-06-15
**Domain**: Java / Resilience4j / Spring Boot
**Context**: Per L-035 / L-038, Spring Cloud BOM is tightly coupled to Spring Boot major. Spring Cloud 2025.1.2 (for SB 4.1.0) pulls `spring-cloud-circuitbreaker-dependencies:5.0.2` which pins `<resilience4j.version>2.3.0</resilience4j.version>` and imports `resilience4j-bom:2.3.0`. PayU's parent pom sets `<resilience4j.version>2.4.0</resilience4j.version>` AND uses `resilience4j-spring-boot3` artifact. Two distinct issues surface at SB 4.1.0:

1. **`resilience4j-spring-boot3` is for SB 3.x only**. SB 4.x requires `resilience4j-spring-boot4` (published since 2.4.0, March 2026). Failure to switch causes class loading failures during `@ConditionalOnMissingBean` introspection of fallback decorators.

2. **`resilience4j-spring-boot4:2.4.0` depends on `resilience4j-spring6:2.4.0`** (compile scope). But Maven dep mediation prefers the older `resilience4j-spring6:2.2.0` brought in transitively by `resilience4j-bom:2.3.0` (from Spring Cloud). The 2.2.0 spring6 jar contains `RxJava3FallbackDecorator` but references `io.reactivex.rxjava3.*` packages directly. Without an explicit dep-mgmt pin, Maven serves the wrong (older) jar and `@ConditionalOnMissingBean` fails with `NoSuchMethodError: io.github.resilience4j.retry.annotation.Retry.configuration()` (2.4.0 spring6 expects 2.4.0 annotations, but mediation gives 2.2.0 annotations).

3. **`resilience4j-bom` does NOT manage `resilience4j-spring-boot4`** (only spring-boot3 + spring6 + core artifacts). Manual pin required.

**Pattern (verified migration recipe)**:
```xml
<!-- Parent pom dependencyManagement: pin ALL Resilience4j artifacts explicitly + import BOM -->
<dependencyManagement>
    <dependencies>
        <!-- BOM (manages core/circuitbreaker/retry/bulkhead/timelimiter/spring6/spring-boot3) -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-bom</artifactId>
            <version>${resilience4j.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- spring-boot4 (NOT in BOM) -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot4</artifactId>
            <version>${resilience4j.version}</version>
        </dependency>

        <!-- Override Spring Cloud BOM's older r4j-spring6 + annotations + core + consumer + framework-common + circularbuffer + ratelimiter pins -->
        <dependency><groupId>io.github.resilience4j</groupId><artifactId>resilience4j-spring6</artifactId><version>${resilience4j.version}</version></dependency>
        <dependency><groupId>io.github.resilience4j</groupId><artifactId>resilience4j-annotations</artifactId><version>${resilience4j.version}</version></dependency>
        <dependency><groupId>io.github.resilience4j</groupId><artifactId>resilience4j-core</artifactId><version>${resilience4j.version}</version></dependency>
        <dependency><groupId>io.github.resilience4j</groupId><artifactId>resilience4j-consumer</artifactId><version>${resilience4j.version}</version></dependency>
        <dependency><groupId>io.github.resilience4j</groupId><artifactId>resilience4j-framework-common</artifactId><version>${resilience4j.version}</version></dependency>
        <dependency><groupId>io.github.resilience4j</groupId><artifactId>resilience4j-circularbuffer</artifactId><version>${resilience4j.version}</version></dependency>
        <dependency><groupId>io.github.resilience4j</groupId><artifactId>resilience4j-ratelimiter</artifactId><version>${resilience4j.version}</version></dependency>
    </dependencies>
</dependencyManagement>
```

```xml
<!-- All service/shared poms using r4j: switch to spring-boot4 artifact -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot4</artifactId>  <!-- was: resilience4j-spring-boot3 -->
</dependency>
```

**Additional runtime dep required**: `RxJava3FallbackDecorator` in spring6:2.4.0 imports `io.reactivex.rxjava3.*` directly. Spring's `@ConditionalOnMissingBean` type-deduction forces class introspection BEFORE the `@Conditional` gate fires, so RxJava3 MUST be on classpath even though the application doesn't use RxJava3. Add to `resilience-starter/pom.xml`:
```xml
<dependency>
    <groupId>io.reactivex.rxjava3</groupId>
    <artifactId>rxjava</artifactId>
    <scope>runtime</scope>
</dependency>
```
Version managed by SB 4.1.0 BOM (3.1.12).

**Lesson**:
1. **Spring Cloud BOM pins transitive r4j artifacts to older versions** that don't match our intended r4j.version. Maven dep mediation picks the BOM-managed version (depth 2) over the desired version (declared transitively at depth 3). The fix is explicit dep-mgmt pins for EVERY artifact in the r4j family, not just the entry-point starter.
2. **Use `mvn dependency:tree -Dverbose -Dincludes='io.github.resilience4j:*'`** to detect version cascading bugs. Look for `(version managed from X.Y)` and `(omitted for conflict)` lines.
3. **r4j-bom is incomplete** — doesn't include `spring-boot4`. Track Resilience4j team to add it. Until then, manual pin.
4. **Spring's type-deduction in `@ConditionalOnMissingBean` is eager** — it forces class introspection BEFORE the bean's `@Conditional` annotations are evaluated. If the bean class references optional runtime libs (like RxJava3 here), those libs MUST be on classpath even when the bean is never instantiated. Workaround: include the optional libs as `runtime` scope deps.

## L-044: Spring Cloud Vault 5.0.x Requires Spring Boot 4.0+ + Service-Local SC Version Overrides Trap (2026-06-15)

**Date**: 2026-06-15
**Domain**: Java / Spring Cloud / Spring Boot
**Context**: PayU services had per-service `<spring-cloud.version>2025.0.2</spring-cloud.version>` overrides plus local `<dependencyManagement>` imports of `spring-cloud-dependencies:2025.0.2`. When parent pom was bumped to SB 4.1.0 (which requires Spring Cloud 2025.1.2), the service-local overrides won (Maven dep-mgmt nearest-wins), pinning spring-cloud-* artifacts to 4.3.2 (the SB 3.x compat version). Result: services pulled `spring-cloud-vault-config:4.3.2` which references `org.springframework.boot.autoconfigure.web.ServerProperties` (SB 3.x package path) and `spring-cloud-commons:4.3.2` which references `org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties` (also SB 3.x). Both classes were moved/removed in SB 4.0. Symptom: `NoClassDefFoundError: org/springframework/boot/autoconfigure/web/ServerProperties` at context refresh.

**Pattern**: Per-service Spring Cloud version overrides are a foot-gun during major SB migrations. They silently break the parent's intent.

**Lesson**:
1. **Audit per-service `<spring-cloud.version>` overrides + local dep-mgmt imports BEFORE bumping parent SB version**. 14 PayU services had this pattern (account, auth, transaction, lending, fx, dispute, wallet, support, backoffice, billing, investment, partner, compliance, promotion). Bulk sed: `s|<spring-cloud.version>2025.0.2</spring-cloud.version>|<spring-cloud.version>2025.1.2</spring-cloud.version>|g; s|<version>2025.0.2</version>|<version>2025.1.2</version>|g`.
2. **Springdoc-openapi version is also coupled to SB major**. 2.x is SB 3.x compat; 3.0+ is SB 4.x compat. Bumping SB without bumping springdoc causes `NoClassDefFoundError: org/springframework/boot/autoconfigure/web/servlet/WebMvcProperties` at first SwaggerConfig load. Bump from 2.8.x → 3.0.3 (April 2026).
3. **The "DRY parent pom" pattern fails when services override**. Consider a CI/ArchUnit check that fails the build if any service pom has `<spring-cloud.version>` property set OR imports `spring-cloud-dependencies` locally (forcing all services to inherit parent's version).

## L-045: SB 4.1.0 Drops Default Jackson 2 `ObjectMapper` Bean — Add `spring-boot-jackson2` for Idempotency / Cache Use Cases (2026-06-15)

**Date**: 2026-06-15
**Domain**: Java / Spring Boot / Jackson
**Context**: PayU's `IdempotencyAutoConfiguration` (in `shared/api-commons`) `@Autowired`s a `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2) to serialize cached idempotency responses. In SB 3.x, the default `JacksonAutoConfiguration` created this bean automatically. In SB 4.1.0, the default is `tools.jackson.databind.json.JsonMapper` (Jackson 3) — no Jackson 2 `ObjectMapper` bean is created. Result: `NoSuchBeanDefinitionException: No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper'` when any service using `@Idempotent` loads its Spring context.

**Pattern**: For services that need Jackson 2 `ObjectMapper` (because they use Jackson 2 API directly — e.g., cache wire format, idempotency response cache), explicitly add `spring-boot-jackson2`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-jackson2</artifactId>
</dependency>
```
This provides `Jackson2AutoConfiguration` which creates a Jackson 2 `ObjectMapper` bean alongside the Jackson 3 `JsonMapper`. Both can coexist.

**Lesson**:
1. **Identify all places using Jackson 2 `ObjectMapper` directly** (search `@Autowired ObjectMapper`, `@Bean ObjectMapper`, `new ObjectMapper()`). These all need `spring-boot-jackson2` on classpath.
2. **The fix is library-level, not service-level**. Add the dep to the shared starter that consumes Jackson 2 (in our case, `api-commons`) so all downstream services inherit it transitively.
3. **Plan a Jackson 3 migration as separate ticket**. Long-term, migrate `IdempotencyService` (and other consumers) to `tools.jackson.databind.json.JsonMapper`. Until then, `spring-boot-jackson2` is the bridge.

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

## L-048: 100% Test Green ≠ 100% Runtime Healthy — Always Verify Cluster Deploy (2026-06-15)

**Date**: 2026-06-15
**Domain**: Process / Testing / Deployment
**Context**: After 6 iterations achieving **41/41 modules SUCCESS** in `mvn test`, iteration 7 rebuilt + deployed 26 images at `:1.8.21`. Result: 22 services UP, **3 services CrashLoopBackOff** with runtime production bugs that tests had never exposed:
- **auth-service**: SB 4.1 reactive autoconfig stopped registering `WebClient.Builder` bean. KeycloakService `@Autowired` failed. Tests passed because `KeycloakService` was mocked via `@MockitoBean` in unit tests + the failing context was never loaded.
- **wallet-service**: `org.springframework.grpc.client.AbstractGrpcClientRegistrar` class not found. spring-grpc 0.2.0 → 1.0.3 package rename. Tests passed because gRPC autoconfig was excluded in test slices.
- **product-catalog-service**: 3-chain bug: Hypersistence `JsonType` (READY-037 family), cache-starter `@ConditionalOnClass(KafkaTemplate)` should be `@ConditionalOnBean`, payu.cache.invalidation.enabled=true requires Kafka that doesn't exist. All 3 surface only during full Spring context refresh in production env, not test slice.

**Pattern**: Test isolation (mocks, autoconfig excludes, `@Disabled` for infra issues) hides framework integration bugs that only surface when:
1. Full production context refreshes (no test mocks)
2. Real classpath has full transitive dep tree (no test excludes)
3. Real env vars + configmaps (no test profile defaults)
4. Real network deps available/unavailable (e.g., Kafka broker, Postgres, Redis)

**Lesson**:
1. **`mvn test` SUCCESS is a NECESSARY but not SUFFICIENT condition for production readiness**. Always include a real deploy step in the verification pipeline.
2. **Multi-dimensional readiness metric**:
   - **Compile-time**: source compiles (cheapest, fastest signal)
   - **Test-time runtime**: full test suite passes (catches unit-level bugs)
   - **Container build**: image builds + has correct entrypoint (catches packaging bugs)
   - **Cluster deploy**: pod starts + readiness probe passes (catches autoconfig + classpath + env bugs)
   - **E2E**: real user flow via real network (catches integration + auth chain bugs)
3. **Deploy verification is cheap (5-10 min total)**: build 4 fresh JARs + 4 podman images + 4 oc set image + 4 health endpoint curls. Always do this before claiming "production ready".
4. **Iteration 8 fix pattern**: when test-green code fails to deploy, the fix is usually at the Spring autoconfig boundary (bean missing, condition wrong, classpath leak). Reach for `@Bean` explicit registration, `@ConditionalOnBean` vs `@ConditionalOnClass`, or yml/env property override.
5. **Cluster infra issues are often pre-existing**: during iteration 2 deploy, 14+ services had been crashlooping 24h with `28P01 password authentication failed` because `db-secrets.DB_PASSWORD` random string didn't match Postgres `payu-postgres-credentials.password=payu-dev-password`. Patched secret → rollout restart → 0 CrashLoopBackOff. **Always inspect existing cluster state before assuming your code change is the root cause.**

## L-049: Cluster Infrastructure Cleanup During Major Migration (2026-06-15)

**Date**: 2026-06-15
**Domain**: OpenShift / Operations
**Context**: During SB 4.1.0 migration deploy iterations, OpenShift `payu-dev` cluster had legacy infrastructure constraints that blocked rollouts:

1. **HPA + PDB battles**: `auth-service-hpa min=2/max=5` overrode manual `oc scale --replicas=4`. HPA scaled back to 5. PDB `min-available=1` blocked pod evictions during rollout. Per user directive: deleted all 13 HPA + 18 PDB resources from namespace.

2. **Topology spread constraints**: deployments had `topologySpreadConstraints: maxSkew:1, whenUnsatisfiable:DoNotSchedule`. With 4 workers + 5 replicas, the 5th pod always pending (`FailedScheduling: 4 node(s) didn't match pod topology spread constraints`). Scaled all deployments to `replicas=1` to bypass.

3. **Container name mismatches**: Spring Boot service deployments have container name `app`. Quarkus simulators have container name matching the deployment name (e.g., `bi-fast-simulator`). The `oc set image deployment/X app=...` command fails on simulators with `error: unable to find container named "app"`. Need conditional script: `oc set image deployment/$svc $cname=$image:$tag` where `$cname` is detected via `oc get deployment $svc -o jsonpath='{.spec.template.spec.containers[0].name}'`.

4. **Secret sync drift**: `db-secrets.DB_PASSWORD` value drifted from `payu-postgres-credentials.password` over time (cluster was rebuilt but secrets not re-synced). Result: `28P01 password authentication failed` for 14+ services. Patch via `oc patch secret db-secrets --type=json -p='[{"op":"replace","path":"/data/DB_PASSWORD","value":"<base64>"}]'`.

5. **Memory limits insufficient for new framework deps**: wallet-service `:1.8.22` (Resilience4j 2.4 + spring-grpc 1.0.3 + new dep tree) OOMKilled at 512Mi limit. Bumped to 1024Mi. Pattern: framework upgrades typically need 1.5-2x baseline memory for the first iteration.

**Lesson**:
1. **Cluster maintenance is NOT free during major migrations**. Budget 30-60 min per deploy iteration for: secret sync verification, replica count adjustment, container name validation, memory limit tuning. These are NOT code bugs — they're infrastructure drift.
2. **Topology spread + replica count is a footgun**. If you set `whenUnsatisfiable: DoNotSchedule` and your replicas exceed worker count, the extra pods will Pending forever. Either: (a) bump worker count, (b) reduce replicas, (c) change to `whenUnsatisfiable: ScheduleAnyway`.
3. **Always check the container name before `oc set image`**. Use `oc get deployment $svc -o jsonpath='{.spec.template.spec.containers[*].name}'`. Spring Boot scaffolds often use `app`, Quarkus often uses service name.
4. **Secret rotation is a separate operational concern from code migration**. Don't conflate "service crashed after my deploy" with "my code is broken" — verify infrastructure state first.
5. **Memory limits ARE part of the deploy contract**. After major framework upgrade (Boot 3→4, Resilience4j 2.3→2.4, Jackson 2→3), expect ~25-50% memory increase. Re-baseline limits in the same PR as the framework upgrade.

## L-050: 3scale Backend-Listener Stale In-Memory Cache — Restart Fixes "service_id_invalid" (2026-06-15)

**Date**: 2026-06-15
**Domain**: 3scale / API Management / Backend
**Context**: After deploying SB 4.1.0 cascade, attempted to verify E2E via 3scale APIcast (`payu-product-payu-apicast-production.apps.payu.ocp.fajjjar.my.id`). APIcast returned 403 "Authentication failed" for valid user_key `04dc03f2e2a776bffcb9b16eb9f93796`. Investigation revealed:

1. **3scale System state was CORRECT**: Admin API confirmed Application ID 7 with valid user_key, plan="Unlimited Plan", state=live, enabled=true, bound to service 3.
2. **Backend Redis was POPULATED**: `payu-cache:6379/0` had 298 keys including `service/id:3/provider_key=95ebe8814cdbaad764b4c62615c4bc39`, `service/id:3/state=active`, `application/service_id:3/key:04dc03f2e2a776bffcb9b16eb9f93796/id=d3a5040b`.
3. **APIcast proxy config was CORRECT**: HTTP fetch from system-master returned valid proxy config v2 with correct hosts, auth_user_key, credentials_location.
4. **But backend-listener `/transactions/authrep.xml` STILL returned `service_id_invalid` for ALL service IDs (1, 2, 3)** — even with correct provider_key. Same error from external route + internal port.

**Root cause**: backend-listener pods maintain an in-memory LRU cache of service registrations. When the cache is stale (e.g., from a previous deploy where services hadn't been synced yet), `authrep` validation rejects requests even though Redis has the data. The cache doesn't auto-refresh from Redis on each request — it relies on sidekiq worker sync events that may have been missed.

**Fix (1 command, instant)**:
```bash
oc -n payu-api-management rollout restart deployment backend-listener
oc -n payu-api-management rollout restart deployment backend-worker
# Wait ~60s for pods to come up
# Verify:
curl "https://backend-payu.apps.payu.ocp.fajjjar.my.id/transactions/authrep.xml?provider_key=<KEY>&service_id=3&user_key=<USER_KEY>&usage[hits]=1"
# Should return: <status><authorized>true</authorized><plan>...</plan></status>
```

**Verification (after restart)**:
- Backend authrep: `<authorized>true</authorized><plan>Unlimited Plan</plan>` ✓
- APIcast → gateway → wallet → Postgres E2E cards CRUD: T1-T5 all HTTP 200/201 ✓

**Lesson**:
1. **3scale backend has 3 cache layers**: (a) APIcast proxy config cache (TTL 300s), (b) backend-listener in-memory service cache (refreshed on sidekiq event), (c) backend Redis (source of truth). When debugging "Authentication failed", check ALL THREE before assuming config is broken.
2. **Order of investigation**: (1) Verify Application + Plan in Admin API. (2) Verify keys in backend Redis. (3) Verify proxy config via system-master endpoint. (4) Verify authrep XML response from backend route. (5) If 4 fails despite 1-3 being correct → **restart backend-listener**.
3. **Symptom hint**: if `authrep` returns `service_id_invalid` for EVERY service ID (not just the one being tested), it's the in-memory cache. If only specific service fails, it's likely a registration issue.
4. **Don't recreate Application CR or run ProxyConfigPromote unnecessarily**. These add new versions but don't fix backend cache. The error `Required parameter missing: to` + `version: has already been taken` for ProxyConfigPromote indicates the version is already promoted — restart is the actual fix.
5. **Pre-flight check before declaring 3scale "broken"**: run `oc -n payu-api-management exec backend-worker-* -- bundle exec ruby -e 'require "redis"; r=Redis.new(url:ENV["CONFIG_REDIS_PROXY"]); puts r.get("service/id:3/state")'`. If returns "active" → cache mismatch, restart fixes it.

## L-051: Gateway `@Path("/{path: .*}")` vs `@Path("/foo")` — Quarkus RESTeasy Reactive Drops the Literal (2026-06-15)

**Date**: 2026-06-15
**Domain**: Java / Quarkus / RESTeasy Reactive / Gateway
**Context**: During READY-064 fix, the gateway `ApiGatewayResource` had a mix of literal `@Path("/payments/va/{vaId}")` and catch-all `@Path("/{path: .*}")` methods. After refactoring to a single catch-all dispatcher, routes like `/api/v1/payments/methods` (from `PaymentMethodResource`) returned 404 "Unable to find matching target resource method" — even though the endpoint existed.

**Root cause**: `PaymentMethodResource` declared `@Path("/api/v1/payments")` at the **class level**. RESTeasy Reactive matches a path to the most specific resource class first. For `/api/v1/payments/methods`, it picked `PaymentMethodResource` (class-level match) over the catch-all in `ApiGatewayResource`. The class had `@GET @Path("/methods")` — a literal match for `/payments/methods` — but for `/payments/va`, the class had no method matching `/va`, so RESTeasy returned 404 from the resource class, not from the gateway.

**Pattern (Quarkus RESTeasy Reactive route resolution precedence)**:
1. RESTeasy scans class-level `@Path` and picks the **most specific** resource class.
2. Within the class, it picks the **most specific** method (literal > `{var}` > `{path: .*}`).
3. If no method matches → throws `jakarta.ws.rs.NotFoundException` → gateway maps to 404.

**Production-ready fix (two-part)**:
1. **Change class-level `@Path` to the FULL path**: `PaymentMethodResource @Path("/api/v1/payments")` → `@Path("/api/v1/payments/methods")`. Remove method-level `@Path("/methods")` since it's now redundant.
2. **Use one catch-all per HTTP verb** in the gateway's catch-all resource. Avoid mixing exact and catch-all `@Path` in the same class — RESTeasy's match algorithm is brittle.

**Lesson**:
1. **Quarkus RESTeasy Reactive has a "most specific class wins" precedence that drops exact `@Path` methods when the class also has a catch-all.** This is the exact-vs-greedy `@Path` conflict that drops literal endpoints.
2. **Always use FULL paths in class-level `@Path`** — never `/api/v1/foo` + method-level `/bar` if the class might shadow a sibling path. Use `/api/v1/foo/bar` as class-level, then methods inherit the full path.
3. **Test with sibling paths** to catch shadow bugs. If you have `/api/v1/payments/va` and `/api/v1/payments/methods`, verify BOTH resolve correctly. A test that only hits one path will miss the shadow bug.
4. **Use Quarkus OpenAPI spec** (`/q/openapi`) as a sanity check after every resource change. If an endpoint disappears from the spec, it's shadowed.

## L-052: `@GeneratedValue(UUID)` + Manual ID = Spring Data JPA "Detached Entity" Trap (2026-06-15)

**Date**: 2026-06-15
**Domain**: Java / JPA / Spring Data JPA / Entity Design
**Context**: During READY-063 fix, `DisbursementEntity.id` had `@GeneratedValue(strategy = GenerationType.UUID)` AND the service code set `disbursement.id = UUID.randomUUID()` manually before save. Result: `StaleObjectStateException: Row was already updated or deleted by another transaction for entity [DisbursementEntity with id '...']` on every INSERT.

**Root cause (per context7/spring-projects/spring-data-jpa documentation)**: Spring Data JPA's `JpaMetamodelEntityInformation` uses this detection strategy for `isNew()`:
- If `@Version` field exists and is null → `isNew = true` → `EntityManager.persist()` (INSERT)
- If no `@Version` AND `@Id` is null → `isNew = true` → persist
- If `@Id` is non-null AND no `@Version` → `isNew = false` → `EntityManager.merge()` (SELECT + INSERT/UPDATE)

The third case is the trap. `@GeneratedValue` with a manual ID set looks identical to a previously-persisted entity to Spring Data JPA. The fix path (`save()` → `merge()`) then calls `SELECT WHERE id = ?` which returns 0 rows, and Hibernate throws `StaleObjectStateException` because the in-memory entity is "stale" relative to no DB row.

**Production-ready fix options (in order of preference)**:
1. **Use the `Persistable<ID>` interface** (Spring Data JPA best practice per context7):
   ```java
   @Entity
   public class DisbursementEntity implements Persistable<UUID> {
       @Id private UUID id;
       @Transient private boolean isNew = true;
       @Override public boolean isNew() { return isNew; }
       @PostPersist @PostLoad void markNotNew() { this.isNew = false; }
   }
   ```
   Manually manage `isNew` flag. `save()` then correctly calls `persist()` for new entities.
2. **Remove `@GeneratedValue` for application-assigned IDs**: just `@Id private UUID id;` with no generator. Then set `id = UUID.randomUUID()` in factory method. Spring Data JPA still sees non-null ID but `merge()` is acceptable because the entity was never previously persisted.
3. **Add `@Version` field**: `@Version Long version;` with `version = 0L` set in factory. Then `isNew()` returns `true` → `persist()`.
4. **Custom `JpaRepository` fragment with `persistNew()`**: use `EntityManager.persist()` + `flush()` directly via `@PersistenceContext`. Bypasses Spring Data JPA's `isNew()` entirely.

**Pattern (chosen for READY-063 + READY-072)**: Option 2 (remove `@GeneratedValue`) + Option 4 (custom fragment). Combined approach: entity has no `@GeneratedValue` (clean), repository has a custom `persistNew()` that uses `EntityManager.persist()` directly (no `isNew()` checks).

**Lesson**:
1. **`@GeneratedValue` + manual ID = footgun**. The annotation is meant for cases where the DB generates the value. If your code sets the ID manually, REMOVE `@GeneratedValue`. There's no benefit to having it.
2. **Hibernate 6.2+ has stricter merge() behavior** — `StaleObjectStateException` is now thrown eagerly for new rows. The "merging a transient entity" trick that worked in older versions no longer works.
3. **For audit-trail entities (disbursement, scheduled-transfer, escrow, settlement)** that need stable cross-service IDs, prefer **application-assigned UUIDs + Persistable interface** over DB-generated sequences. This is also the recommended pattern for event-sourced systems where the ID is the event ID.
4. **Generic solution for the platform**: create a shared `abstract class PayuPersistableEntity<ID>` that implements `Persistable<ID>` + manages `isNew` flag. All entities that need manual IDs extend it. This eliminates the bug class for all current + future entities.

## L-053: Yaml Routes Override RouteRegistry Defaults — Add ALL Routes to YAML, Not Defaults (2026-06-15)

**Date**: 2026-06-15
**Domain**: Java / Quarkus / Gateway / Configuration
**Context**: After gateway refactor, escrow + settlements routes returned 404 "No route found for path: /api/v1/escrow" despite being added to `RouteRegistry.loadDefaultRoutes()`. Investigation revealed: `loadRoutes()` checks if `configRoutes` (from yaml) is non-empty; if so, it skips `loadDefaultRoutes()` entirely. The yaml had many routes (accounts, wallets, etc.) so defaults were never loaded.

**Root cause**: `RouteRegistry.loadRoutes()` has a **fallback semantics**, not a **merge semantics**. Either YAML has the route OR defaults do, not both. This is a common "first source wins" pattern in config loaders, but it has a footgun: if you add a new route in code (e.g., for a new service) AND the yaml has any other routes, your default is silently ignored.

**Pattern (the right way)**:
- **All gateway routes MUST be in `application.yaml`** (the single source of truth).
- `loadDefaultRoutes()` should be a **fallback for development only** (when no yaml is present), not a "production" route source.
- Add a CI check: `grep -c '  [a-z].*:' application.yaml` and assert >= expected route count.

**Production-ready fix applied**: Added escrow + settlements routes to `application.yaml`:
```yaml
escrow:
  service: "wallet-service"
  target-prefix: "/api/v1/escrow"
  methods: ["GET", "POST", "PUT", "DELETE"]
settlements:
  service: "wallet-service"
  target-prefix: "/api/v1/settlements"
  methods: ["GET", "POST", "PUT", "DELETE"]
```

**Lesson**:
1. **"Defaults are fallback, not supplements"** — if a config loader has a fallback path, never rely on it coexisting with primary config. Either populate primary config fully, or implement a proper merge.
2. **Config loaders should log which source provided each route/entry**. `RouteRegistry.loadRoutes()` should log "Loaded 45 routes from YAML + 0 from defaults" or "Loaded 0 routes from YAML, using 12 defaults". This makes it obvious when defaults are bypassed.
3. **Add startup assertion**: fail fast if critical routes are missing. E.g., `RouteRegistry.verifyCriticalRoutes()` throws if `/api/v1/payments`, `/api/v1/accounts`, etc. are not registered at startup. Catches config drift in CI before users see 404s in production.
4. **The "fallback defaults" pattern is a common anti-pattern in config loaders** — Spring `@ConditionalOnMissingBean`, Quarkus `@UnlessBuildProperty`, and 12-factor config all share this footgun. Always check whether the fallback fires at runtime, not just in unit tests.

## L-054: HttpRequestMethodNotSupportedException → Always Map to 405, Not the Generic 500 Handler (2026-06-15)

**Date**: 2026-06-15
**Domain**: Java / Spring Boot / Exception Handling
**Context**: During READY-073 fix, `wallet-service` local `GlobalExceptionHandler` didn't handle `HttpRequestMethodNotSupportedException`. When client POSTs to an endpoint that only has GET (e.g., `POST /api/v1/wallets` when controller has only `POST /api/v1/wallets/{accountId}/reserve`), Spring throws the exception but it falls through to the generic `@ExceptionHandler(Exception.class)` which returns 500 `INTERNAL_ERROR`. The user sees a misleading "internal error" for what is actually a client bug.

**Root cause (per context7/spring-projects/spring-boot docs)**: When a `@RestControllerAdvice` bean is present in the context, Spring's default `ResponseEntityExceptionHandler` is NOT auto-applied. The default `ResponseEntityExceptionHandler` does handle `HttpRequestMethodNotSupportedException` → 405. But as soon as you add your own `@RestControllerAdvice`, you must explicitly add the handlers for all standard Spring MVC exceptions, OR extend `ResponseEntityExceptionHandler`.

**Pattern (production-ready fix)**:
```java
@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
        HttpRequestMethodNotSupportedException ex,
        HttpServletRequest request) {
    String supportedMethods = ex.getSupportedHttpMethods() != null
            ? ex.getSupportedHttpMethods().stream()
                    .map(Object::toString).collect(Collectors.joining(", "))
            : "unknown";
    log.info("Method not allowed for {}: requested={} allowed={}",
            request.getRequestURI(), ex.getMethod(), supportedMethods);
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .header("Allow", supportedMethods)
            .body(ApiResponse.error("METHOD_NOT_ALLOWED",
                    "Method " + ex.getMethod() + " not allowed. Supported: " + supportedMethods));
}
```

**Lesson**:
1. **Always extend `ResponseEntityExceptionHandler`** (Spring's base) instead of writing a `@RestControllerAdvice` from scratch. You get all standard Spring MVC exception → HTTP status mappings (404, 405, 415, 422, etc.) for free + can override specific ones.
2. **If you must write a custom advice**, audit each Spring MVC exception type: `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException`, `HttpMessageNotReadableException`, `MethodArgumentNotValidException`, `MissingServletRequestParameterException`, `NoHandlerFoundException`, `NoResourceFoundException`, `ConversionFailedException`, `TypeMismatchException`, etc.
3. **Add the handler to BOTH shared `api-commons` AND local service `GlobalExceptionHandler`** — they may not share the same advice (services often define their own local advice for PII masking or different error formats).
4. **The 405 response MUST include the `Allow` header** per RFC 7231 §6.5.5. Clients use this to determine which methods to retry with. The body should also include `supportedMethods` field for machine-readable parsing.
5. **Test with curl `-X POST` on a GET-only endpoint** to catch the bug. A test that only uses correct methods will never expose a missing 405 handler.

---

*Last Updated: June 15, 2026 — L-041 corrected (Jackson 2.21 ADD, not 2.18 removal). L-043 (Resilience4j 2.4 + SB 4.1 cascade), L-044 (Spring Cloud 5.0 + service-local overrides), L-045 (spring-boot-jackson2), L-046 (Jackson 3 SerializationFeature enum binding), L-047 (Camel 4.20 SB 4.1 compat), L-048 (test green ≠ runtime healthy), L-049 (cluster infra cleanup during migration), L-050 (3scale backend cache restart) added.*
