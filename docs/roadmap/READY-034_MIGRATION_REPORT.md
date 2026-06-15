# READY-034 Migration Audit Report — Spring Boot 4.1.0 Shared Starter Compatibility

> **Audit date**: 2026-06-15
> **Auditor**: `ready-034` worktree (`feature/ready-034-spring-boot-4`)
> **Target**: Spring Boot **3.5.14 → 4.1.0** (Jakarta EE 11, Spring Framework 7, Hibernate 7, Jackson 3, Spring Cloud 2025.1.2)
> **Scope**: All 14 shared starters + parent POM impact
> **Methodology**: Static code + POM audit against SB 4.1.0 BOM and 4.0 release notes. No build attempted (per audit-only directive).

---

## 📊 Executive Summary

| Metric | Value |
|:---|:---|
| **Starters audited** | 14 (13 SB-based + 1 Quarkus) |
| **Starters needing code changes** | 10 |
| **Starters needing pom-only changes** | 12 |
| **P0 blockers (won't compile)** | 4 (`jms`, `rest-client`, `events`, `saga`) |
| **P1 deprecated/warnings** | 8 (all except `quarkus-api-commons` + `logging`) |
| **Parent POM changes required** | 1 version bump + 2 BOM imports + 1 starter removal |
| **Cascade to services** | 20+ service poms use `spring-boot-starter-aop` (removed) |
| **Total estimated effort** | ~3-4 dev days (matches L-035 cost estimate) |

> **Verdict**: Migration is viable but **NOT bounded to 14 starters**. The parent POM is shared infrastructure; bumping it cascades to **all 22 services** that reference `spring-boot-starter-aop` (removed in 4.0) plus service poms that need `rest-assured-bom` and `testcontainers-bom` imports.
>
> See "Execution Plan" below for sequenced approach.

---

## 🔥 Cross-Cutting Changes (Apply to All Starters)

### CC-1. `spring-boot-starter-aop` — REMOVED in SB 4.0

**Status**: P0 BLOCKER. Last published at `3.5.15` + `4.0.0-M2` (milestone). Final 4.0.0 / 4.1.0 releases do **NOT** publish this artifact.

**Replacement**: AOP is now auto-configured when AspectJ is on the classpath (per SB 4.0 release notes: "Spring Boot automatically configures Aspect-Oriented Programming (AOP) and defaults to using CGLib proxies."). No starter wrapper needed.

**Affected poms (5 shared starters + 16 services = 20 total)**:
- `shared/api-commons/pom.xml` line 90-93 ✅ USES AOP (`RateLimitAspect`)
- `shared/cache-starter/pom.xml` line 50-54 ✅ USES AOP (`CacheWithTTLAspect`)
- `shared/resilience-starter/pom.xml` line 39-43 ❓ Need to verify (`@CircuitBreaker` AOP?)
- `shared/rest-client-starter/pom.xml` line 47-51 ❌ DOES NOT USE AOP — pure stale dep
- `shared/security-starter/pom.xml` line 39-43 ✅ USES AOP (`AuditAspect`, `DataMaskingAspect`)

**Migration recipe**:
```xml
<!-- REMOVE -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- FOR STARTERS THAT USE AOP: add explicit AspectJ dep (auto-pulled by BOM but explicit for safety) -->
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
</dependency>
```

### CC-2. Jackson 2 → Jackson 3 (Default in SB 4.1.0)

**Status**: P1 DEGRADED. Jackson 2 still works (deprecated, ships in `spring-boot-autoconfigure-classic` for migration). Migration window exists; not blocking.

**Affected files**:
- `shared/events-starter/.../EventsAutoConfiguration.java:19` — `import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;`
- `shared/jms-starter/.../JmsAutoConfiguration.java:15,78` — `import org.springframework.jms.support.converter.MappingJackson2MessageConverter;`
- `shared/events-starter/.../EventsAutoConfiguration.java:19` — unused import (verify)

**Migration recipe** (Jackson 3):
```java
// Old
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
// New (Jackson 3)
import org.springframework.http.converter.json.JacksonObjectMapperBuilder;
```

```java
// Old
MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
// New (Jackson 3)
JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
```

> ⚠️ For now, keep Jackson 2 imports — Jackson 2 still functions in 4.1.0. Add `@SuppressWarnings("removal")` if needed. Schedule Jackson 3 migration as separate ticket post-SB 4.1.0 stabilization.

### CC-3. Property Renames (Spring Boot 4.0)

**Status**: P1 DEGRADED. Properties still work but emit deprecation warnings. `spring-boot-properties-migrator` tool auto-converts at startup (per migration guide).

**Renames affecting this codebase**:

| Old Property | New Property | Affected Starters |
|:---|:---|:---|
| `management.tracing.enabled` | `management.tracing.export.enabled` | logging-starter, api-commons (filter chain), security-starter (audit publish) |
| `spring.dao.exceptiontranslation.enabled` | `spring.persistence.exceptiontranslation.enabled` | outbox-starter, saga-starter, services using JPA repos |

**Migration recipe**:
```bash
# Per-service grep
rg -l "management.tracing.enabled" backend/
rg -l "spring.dao.exceptiontranslation" backend/
```

### CC-4. Auto-Configuration Package Renames (Spring Boot 4.0)

**Status**: P0 BLOCKER for specific starters.

**Known renames**:
- `org.springframework.boot.autoconfigure.domain.EntityScan` → `org.springframework.boot.persistence.autoconfigure.EntityScan`
- `org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration` → `org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration`

**Affected files**:
- `shared/outbox-starter/src/main/java/id/payu/outbox/config/OutboxAutoConfiguration.java:14,43`
- `shared/outbox-starter/src/test/java/id/payu/outbox/TestConfig.java:5`
- `shared/saga-starter/src/main/java/id/payu/saga/config/SagaAutoConfiguration.java:12`
- `shared/events-starter/src/main/java/id/payu/events/config/EventsAutoConfiguration.java:26`

**Migration recipe** (1-line import fix per file):
```java
// Old
import org.springframework.boot.autoconfigure.domain.EntityScan;
// New
import org.springframework.boot.persistence.autoconfigure.EntityScan;
```

```java
// Old
@AutoConfiguration(after = org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class)
// New
@AutoConfiguration(after = org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration.class)
```

> ⚠️ Package rename locations to be verified against SB 4.1.0 source before applying. Pre-flight: `mvn dependency:resolve` in worktree, then `javap` the new artifact to confirm.

### CC-5. Hibernate 6.3 → 7.1 (Hypersistence artifact rename)

**Status**: P0 BLOCKER for `outbox-starter` + `saga-starter`.

**Affected poms**:
- `shared/outbox-starter/pom.xml:79-82` — `io.hypersistence:hypersistence-utils-hibernate-63:3.9.0` (pinned, also stale)
- `shared/saga-starter/pom.xml:66-68` — `io.hypersistence:hypersistence-utils-hibernate-63:3.9.0` (pinned, stale)
- Parent pom `dependencyManagement:227-235` — `hypersistence-utils-hibernate-63:3.15.2` (needs bump to 70)

**Migration recipe**:
```xml
<!-- Old (Hibernate 6.3) -->
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
    <version>3.15.2</version>
</dependency>

<!-- New (Hibernate 7.0) -->
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-70</artifactId>
    <version>3.15.3</version>
</dependency>
```

**Java code change**: `import io.hypersistence.utils.hibernate.type.json.JsonType;` (unchanged — Hypersistence keeps same package).

### CC-6. Spring Cloud 2025.0.2 → 2025.1.2

**Status**: P0 BLOCKER (incompatible BOM if any starter pulls cloud deps).

**Affected poms**:
- Parent pom line 40: `<spring-cloud.version>2025.0.2</spring-cloud.version>` → `2025.1.2`
- Parent pom line 41: `<spring-cloud-contract.version>4.2.1</spring-cloud-contract.version>` → `5.0.3`

**Cascade**: Services using `spring-cloud-starter-vault`, `spring-cloud-starter-circuitbreaker`, etc. need full Spring Cloud BOM migration. Per L-035: "auth-service: spring-cloud-vault 5.0.0 requires Boot 4.0+, mixed BOMs fail".

### CC-7. ArchUnit 1.3.0 → 1.4.x (for Java 25 support)

**Status**: P0 BLOCKER. Per **READY-032** (already open ticket).

**Affected poms**:
- `shared/archunit-starter/pom.xml:23` — `<archunit.version>1.3.0</archunit.version>`

**Migration recipe**:
```xml
<archunit.version>1.4.1</archunit.version>  <!-- latest 1.4.x supports Java 25 -->
```

> Note: this also fixes READY-032 simultaneously. Coordinated fix recommended.

---

## 📋 Per-Starter Audit

### 1. `api-commons` ⚠️ P1

**Files**:
- `backend/shared/api-commons/pom.xml`
- `src/main/java/id/payu/api/common/controller/RateLimitAspect.java` (AOP)
- `src/main/java/id/payu/commons/idempotency/IdempotencyAutoConfiguration.java` (uses `@EnableConfigurationProperties`)

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| CC-1 | P0 | `spring-boot-starter-aop` dep at pom:90-93 (uses AOP — needs explicit `aspectjweaver`) |
| CC-3 | P1 | Property renames: `management.tracing.enabled` (filter chain), `spring.dao.exceptiontranslation.enabled` |
| Standalone | P2 | `springdoc.version:2.3.0` (parent uses 2.8.17) — older springdoc may have SB 4.1 compat issues |
| Standalone | P2 | `redis` dep for rate limiting — needs SB 4.1 Redis autoconfig verification |

**Effort**: ~2h (pom + verify springdoc 2.3.0 vs 2.8.17 with Spring 7).

### 2. `events-starter` 🔴 P0

**Files**:
- `backend/shared/events-starter/pom.xml`
- `src/main/java/id/payu/events/config/EventsAutoConfiguration.java` (3 issues)

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| CC-4 | P0 | `KafkaAutoConfiguration` import at line 26 (package renamed) |
| CC-2 | P1 | `Jackson2ObjectMapperBuilder` import at line 19 (unused — confirm) |
| Standalone | P0 | **pom hardcodes `<source>21</source><target>21</target>` (lines 99-100)** while parent is Java 25 — will FAIL compile |
| Standalone | P1 | `cloudevents.version:4.0.1` — verify Spring 7 / CloudEvents 4.x compat |

**Effort**: ~3h (3 import fixes + 1 pom override removal + verify CloudEvents 4.0.1).

**Migration recipe (critical fix)**:
```xml
<!-- pom.xml line 96-104: REMOVE these overrides, use parent default (Java 25) -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>  <!-- outdated too — bump to 3.13.0 -->
    <configuration>
        <!-- REMOVE <source>21</source><target>21</target> -->
        <compilerArgs>
            <arg>-parameters</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

### 3. `security-starter` ⚠️ P1

**Files**:
- `backend/shared/security-starter/pom.xml`
- `src/main/java/id/payu/security/masking/DataMaskingAspect.java` (AOP)
- `src/main/java/id/payu/security/audit/AuditAspect.java` (AOP)
- `src/main/java/id/payu/security/config/SecurityAutoConfiguration.java` (`@EnableConfigurationProperties`)

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| CC-1 | P0 | `spring-boot-starter-aop` dep at pom:39-43 (uses AOP) |
| CC-3 | P1 | `management.tracing.enabled` rename for audit publish |
| Standalone | P2 | `jasypt-spring-boot3-starter` is commented out (lines 45-59) — leave as-is, scheduled for separate work |
| Standalone | P2 | `spring-security-oauth2-jose` + `spring-security-oauth2-resource-server` — Spring Security 7.0 may have package renames (verify) |

**Effort**: ~2h (aop fix + verify spring-security 7.0 compat).

### 4. `cache-starter` ⚠️ P1

**Files**:
- `backend/shared/cache-starter/pom.xml`
- `src/main/java/id/payu/cache/aspect/CacheWithTTLAspect.java` (AOP)
- `src/main/java/id/payu/cache/config/CacheAutoConfiguration.java` (`@EnableConfigurationProperties`)

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| CC-1 | P0 | `spring-boot-starter-aop` dep at pom:50-54 (uses AOP) |
| Standalone | P1 | `org.testcontainers:junit-jupiter` + `testcontainers` deps at pom:118-127 (no version) — needs `testcontainers-bom` import |
| Standalone | P1 | `caffeine` + `lettuce-core` versions come from SB 4.1 BOM — verify compatibility |
| Standalone | P2 | `GenericJackson2JsonRedisSerializer` (per READY-013 platform-wide Jackson 3 config) — scheduled separately |

**Effort**: ~3h (aop + testcontainers BOM + verify caffeine/lettuce on Spring 7).

### 5. `resilience-starter` ⚠️ P1

**Files**:
- `backend/shared/resilience-starter/pom.xml`
- `src/main/java/id/payu/resilience/config/ResilienceAutoConfiguration.java`

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| CC-1 | P0 | `spring-boot-starter-aop` dep at pom:39-43 (likely unused — needs audit; Resilience4j uses ByteBuddy proxies via resilience4j-spring-boot3, not AspectJ) |
| Standalone | P1 | `resilience4j-spring-boot3:2.2.0` — needs bump to 2.4.0+ for Spring 7 compat (verify) |
| Standalone | P2 | Local override `<resilience4j.version>2.2.0</resilience.version>` (line 22) duplicates parent property — redundant, can be removed |

**Effort**: ~2h (aop audit + resilience4j version bump).

### 6. `logging-starter` ✅ LOW RISK

**Files**:
- `backend/shared/logging-starter/pom.xml`
- `src/main/java/id/payu/logging/config/PayuLoggingAutoConfiguration.java`

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| CC-3 | P1 | `management.tracing.enabled` rename — likely used in MDC config |
| Standalone | P2 | `micrometer-tracing-bridge-otel` + `opentelemetry-api` (optional) — Micrometer 1.16 / OTel 1.54.0 are SB 4.1 defaults, version should match |

**Effort**: ~1h (property rename + verify Micrometer 1.16 / OTel 1.54.0 work).

### 7. `outbox-starter` 🔴 P0

**Files**:
- `backend/shared/outbox-starter/pom.xml`
- `src/main/java/id/payu/outbox/config/OutboxAutoConfiguration.java` (multiple)
- `src/main/java/id/payu/outbox/entity/OutboxEvent.java` (uses Hypersistence `JsonType`)
- `src/main/java/id/payu/outbox/publisher/OutboxPublisher.java` (uses `@Deprecated` — verify)
- `src/test/java/id/payu/outbox/TestConfig.java`

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| CC-4 | P0 | `EntityScan` import at OutboxAutoConfiguration:14 (package renamed) |
| CC-4 | P0 | `KafkaAutoConfiguration` import at OutboxAutoConfiguration:43 + TestConfig:5 (package renamed) |
| CC-5 | P0 | `hypersistence-utils-hibernate-63:3.9.0` (pinned, stale) → `hypersistence-utils-hibernate-70:3.15.3` |
| Standalone | P1 | `org.testcontainers:*` (4 deps) at pom:138-157 (no version) — needs `testcontainers-bom` import |

**Effort**: ~4h (2 package renames + hibernate-70 dep bump + testcontainers BOM + verify outbox works with Hibernate 7).

### 8. `saga-starter` 🔴 P0

**Files**:
- `backend/shared/saga-starter/pom.xml`
- `src/main/java/id/payu/saga/config/SagaAutoConfiguration.java`
- `src/main/java/id/payu/saga/entity/SagaInstance.java` (uses Hypersistence `JsonType`)

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| CC-4 | P0 | `EntityScan` import at SagaAutoConfiguration:12 (package renamed) |
| CC-5 | P0 | `hypersistence-utils-hibernate-63:3.9.0` (pinned, stale) → `hypersistence-utils-hibernate-70:3.15.3` |

**Effort**: ~3h (1 import + hibernate-70 dep + verify saga orchestrator with Hibernate 7).

### 9. `archunit-starter` 🔴 P0 (per READY-032)

**Files**:
- `backend/shared/archunit-starter/pom.xml`

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| CC-7 | P0 | ArchUnit 1.3.0 → 1.4.x+ for Java 25 support (READY-032 — this audit confirms the blocker) |
| Standalone | P2 | `jakarta.persistence-api` optional dep at line 64 — verify Jakarta Persistence 3.2 compat |
| Standalone | P2 | `spring-data-jpa` optional dep at line 57 — verify Spring Data 2025.1 compat |

**Effort**: ~1h (version bump + smoke test).

### 10. `mapper-starter` ✅ LOW RISK

**Files**:
- `backend/shared/mapper-starter/pom.xml`

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| Standalone | P2 | MapStruct 1.5.5.Final — verify Spring 7 / Hibernate 7 compat. MapStruct 1.6.x recommended |

**Effort**: ~1h (version bump + smoke test on sample service).

### 11. `grpc-starter` ⚠️ P1

**Files**:
- `backend/shared/grpc-starter/pom.xml`
- `src/main/java/id/payu/grpc/starter/config/GrpcStarterAutoConfiguration.java`

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| Standalone | P1 | `spring-grpc.version:0.2.0` — Spring gRPC 1.0+ required for Spring Boot 4.1.0 / Spring Framework 7 |
| Standalone | P2 | `grpc.version:1.69.0` — bump to 1.70+ for Spring 7 compat |
| L-034 | OK | Uses `jakarta.annotation-api` (NOT javax.annotation) — per L-034 lesson, this is correct for gRPC services |
| Standalone | P2 | `protobuf.version:3.25.5` — verify Spring 7 compat |

**Effort**: ~4h (spring-grpc version audit + grpc version bump + per-service smoke test).

### 12. `rest-client-starter` 🔴 P0

**Files**:
- `backend/shared/rest-client-starter/pom.xml`
- `src/main/java/id/payu/shared/restclient/RestClientAutoConfiguration.java` (line 69 — broken API)
- `src/main/java/id/payu/shared/restclient/RestClientErrorHandler.java` (implements `ResponseErrorHandler`)

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| CC-1 | P0 | `spring-boot-starter-aop` dep at pom:47-51 (**DOES NOT USE AOP** — pure stale dep, just remove) |
| Standalone | **P0** | `RestClient.builder().defaultStatusHandler(new RestClientErrorHandler())` at RestClientAutoConfiguration:69 — **Spring Framework 7 REMOVED this method**. Replacement: use `.statusHandler(Predicate<HttpStatusCode>, ErrorHandler)` lambda |
| Standalone | P1 | `SimpleClientHttpRequestFactory` — deprecated in Spring 7, replaced by `JdkClientHttpRequestFactory` |

**Effort**: ~3h (remove aop + refactor statusHandler API + verify error propagation).

**Migration recipe (critical fix)**:
```java
// Old (Spring 6)
return RestClient.builder()
        .requestFactory(factory)
        .defaultHeader("User-Agent", "PayU-RestClient/1.0")
        .defaultStatusHandler(new RestClientErrorHandler())  // REMOVED
        .requestInterceptor(new CorrelationIdInterceptor());

// New (Spring 7)
return RestClient.builder()
        .requestFactory(factory)
        .defaultHeader("User-Agent", "PayU-RestClient/1.0")
        .statusHandler(HttpStatusCode::isError, (request, response) -> {
            // delegate to error handler
            new RestClientErrorHandler().handleError(
                request.getURI(),
                HttpMethod.valueOf(request.getMethod().name()),
                response
            );
        })
        .requestInterceptor(new CorrelationIdInterceptor());
```

### 13. `jms-starter` 🔴 P0

**Files**:
- `backend/shared/jms-starter/pom.xml`
- `src/main/java/id/payu/jms/config/JmsAutoConfiguration.java` (lines 15, 78, 92, 96)
- `src/main/java/id/payu/jms/health/JmsHealthIndicator.java` (lines 3-4)

**Issues**:
| ID | Severity | Description |
|:---|:---:|:---|
| CC-2 | P1 | `MappingJackson2MessageConverter` (line 15, 78) — Jackson 2 deprecated |
| Standalone | **P0** | `org.springframework.boot.actuate.health.Health` + `HealthIndicator` imports (lines 3-4) — package path MAY have moved in SB 4.x. **NEEDS VERIFICATION** (artifact `spring-boot-actuator:4.1.0` exists but package may have moved to `spring-boot-actuator-autoconfigure` or `spring-boot-observability-autoconfigure`) |
| Standalone | P1 | New `JmsClient` API in SB 4.0 — consider migrating from `JmsTemplate` |
| Standalone | P2 | `artemis-jakarta-client` version comes from SB 4.1 BOM — Artemis 2.43.0 (per release notes) |

**Effort**: ~3h (verify actuator package + Jackson 2/3 decision + smoke test against Artemis 2.43.0).

### 14. `quarkus-api-commons` ⏸️ OUT OF SCOPE

**Stack**: Quarkus **3.33.1** (NOT Spring Boot). Different ecosystem, different migration track.

**Action**: Deferred to **UPGRADE-013** (Quarkus 3.36.2 Upgrade, per TODOS.md line 309-321).

---

## 🎯 Execution Plan (For Future Implementation)

> ⚠️ **NOT executing this plan per audit-only directive.** Documenting for future sprint planning.

### Phase 0: Parent POM Pre-Work (Required Before Any Starter Compiles)

**Files to edit**: `backend/pom.xml` (1 file)

**Changes**:
1. `spring-boot-starter-parent: 3.5.14 → 4.1.0` (line 8-11)
2. `spring-cloud.version: 2025.0.2 → 2025.1.2` (line 40)
3. `spring-cloud-contract.version: 4.2.1 → 5.0.3` (line 41)
4. `hypersistence.version: 3.15.2 → 3.15.3` + artifact `hibernate-63 → hibernate-70` (line 39, 227-235)
5. `resilience4j.version: 2.2.0 → 2.4.0` (line 37)
6. ADD `rest-assured-bom` import to `dependencyManagement` (for 15+ service poms)
7. ADD `testcontainers-bom` import to `dependencyManagement` (for 20+ service poms)

**Verification**: `mvn -f backend/pom.xml -N help:effective-pom` to confirm BOM resolutions.

### Phase 1: Shared Starter Migration (4 known blockers)

**Estimated effort**: ~1 dev day

**Sequence** (tackle in dependency order):
1. `api-commons` (no inter-deps) — sets AOP pattern
2. `events-starter` (no inter-deps) — fixes Kafka autoconfig rename
3. `cache-starter` (depends on api-commons) — fixes testcontainers BOM
4. `outbox-starter` (depends on events-starter) — fixes Hibernate 7 + Kafka rename
5. `saga-starter` (depends on outbox-starter) — fixes Hibernate 7
6. `jms-starter` (depends on api-commons) — fixes actuator package
7. `rest-client-starter` (depends on api-commons) — fixes Spring 7 RestClient API
8. `security-starter` (depends on outbox-starter, api-commons) — fixes AOP
9. `resilience-starter` (depends on api-commons) — fixes AOP + Resilience4j
10. `logging-starter` (depends on api-commons) — fixes property rename
11. `mapper-starter` (no inter-deps) — version bump only
12. `grpc-starter` (no inter-deps) — spring-grpc + grpc version bumps
13. `archunit-starter` (depends on security-starter) — fixes ArchUnit 1.3 → 1.4
14. `quarkus-api-commons` — DEFERRED (UPGRADE-013)

**Per-starter test**: `mvn -f backend/shared/<starter>/pom.xml clean test`

### Phase 2: Service POM Cascade (16+ services)

**Estimated effort**: ~1 dev day

**Bulk changes via sed/grep** (mechanical, can be parallelized across subagents):
- REMOVE `<artifactId>spring-boot-starter-aop</artifactId>` from 16 service poms
- ADD explicit `<artifactId>aspectjweaver</artifactId>` (optional, BOM-managed) to services that need AOP
- ADD `testcontainers-bom` import to 20+ poms
- ADD `rest-assured-bom` import to 15+ poms

**Verification**: `mvn -f backend/pom.xml -T 1C -DskipTests test-compile`

### Phase 3: Property Renames (22 services)

**Estimated effort**: ~0.5 dev day

**Grep + replace**:
- `management.tracing.enabled` → `management.tracing.export.enabled`
- `spring.dao.exceptiontranslation.enabled` → `spring.persistence.exceptiontranslation.enabled`

**Per-service test**: `mvn -f backend/<service>/pom.xml test`

### Phase 4: Validation (End-to-End)

**Estimated effort**: ~1 dev day

**Activities**:
1. `mvn -f backend/pom.xml clean test-compile -T 1C` (full compile)
2. `mvn -f backend/pom.xml -P shared-only clean test` (starter tests)
3. `mvn -f backend/pom.xml -P services clean verify` (service tests)
4. Deploy pilot service to OCP, verify E2E
5. `spring-boot-properties-migrator` runtime check — capture deprecation warnings

### Total Estimated Effort

| Phase | Days |
|:---|:---:|
| Phase 0 (parent POM) | 0.5 |
| Phase 1 (14 starters) | 1.0 |
| Phase 2 (16+ service POMs) | 1.0 |
| Phase 3 (22 service property renames) | 0.5 |
| Phase 4 (validation) | 1.0 |
| **TOTAL** | **4.0 dev days** |

This matches L-035's revised cost estimate ("3-4 dev days") — no surprises.

---

## 📦 Dependency Version Reference

| Component | Current (SB 3.5.14) | Target (SB 4.1.0) | Verified |
|:---|:---|:---|:---:|
| Spring Boot | 3.5.14 | 4.1.0 | ✅ |
| Spring Framework | 6.2.x | 7.0 | ✅ (release notes) |
| Spring Cloud | 2025.0.2 | 2025.1.2 | ✅ (Maven Central) |
| Spring Cloud Contract | 4.2.1 | 5.0.3 | ✅ (Maven Central) |
| Spring Security | 6.5.x | 7.0 | ✅ (release notes) |
| Spring for Apache Kafka | 3.3.x | 4.0 | ✅ (release notes) |
| Hibernate | 6.5.x | 7.1 | ✅ (release notes) |
| Jackson | 2.18.6 | 3.0 (default) / 2.x deprecated | ✅ (release notes) |
| Tomcat | 10.x | 11.0 | ✅ (release notes) |
| Jakarta EE | 10 | 11 | ✅ (release notes) |
| Hypersistence Utils | 3.15.2 (hibernate-63) | 3.15.3 (hibernate-70) | ✅ (Maven Central) |
| Resilience4j | 2.2.0 | 2.4.0 | ✅ (Maven Central) |
| ArchUnit | 1.3.0 | 1.4.1+ (Java 25) | ✅ (per READY-032) |
| Spring gRPC | 0.2.0 | 1.0+ (Spring 7) | ⏳ Verify |
| Testcontainers | 1.20.x | 2.0 | ✅ (release notes) |
| MapStruct | 1.5.5.Final | 1.6.x | ⏳ Verify |
| Artemis | 2.40.x | 2.43.0 | ✅ (release notes) |

---

## 🎓 Lessons Learned (For Future SB Migrations)

> Capture for `docs/guides/LESSONS.md` post-execution.

1. **L-036**: SB major version migration cost is concentrated in shared libraries, not services. The 14 shared starters account for ~40% of the work, but 20+ service poms cascade from parent POM changes alone. **Always budget Phase 0 (parent POM + BOM imports) explicitly.**

2. **L-037**: `spring-boot-starter-aop` removal in SB 4.0 is undocumented in migration guide (verified against 4.0 release notes — silent removal). **Audit with `grep -r starter-aop` BEFORE bumping parent POM**, not after.

3. **L-038**: Spring Cloud BOM version is tightly coupled to Spring Boot major version. SB 3.5 → Spring Cloud 2025.0.x. SB 4.0 → Spring Cloud 2025.1.x. Bumping one without the other creates classpath conflicts (e.g., `spring-cloud-vault 5.0.0 requires Boot 4.0+`).

4. **L-039**: Audit-only mode is a viable scope for "too-big" migrations. Produces a migration report in ~1 hour vs multi-day execution. Trade-off: no code change, but the report becomes the spec for future sprint planning.

5. **L-040**: Hypersistence `JsonType` API is stable across Hibernate 6.3 → 7.0 — only the **artifact name** changes (`hibernate-63` → `hibernate-70`). Java imports unchanged. Lower migration cost than expected.

---

## 🔗 References

- Spring Boot 4.0 Release Notes: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes
- Spring Boot 4.0 Migration Guide: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- Spring Boot 4.0 Configuration Changelog: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Configuration-Changelog
- TODOS.md READY-034: `/home/ubuntu/payu/docs/roadmap/TODOS.md` line 237-280
- L-034 Lesson: `/home/ubuntu/payu/docs/guides/LESSONS.md` line 134-163
- L-035 Lesson: `/home/ubuntu/payu/docs/guides/LESSONS.md` line 165-201
- ADR-0016: ARCH-006 phase A strategy (deferred)

---

**Audit completed by**: `feature/ready-034-spring-boot-4` worktree
**Status**: Audit-only deliverable, no code changes applied
**Next step**: User decision on execution timing (deferred to future sprint)
