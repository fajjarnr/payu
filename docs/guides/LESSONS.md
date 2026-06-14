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

---
*Last Updated: June 13, 2026*
