# PayU Security BLOCKERs Sprint — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close 6 BLOCKER security gaps from 2026-07-01 audit (GAP-34, GAP-21, GAP-23, GAP-30, GAP-28, GAP-19, GAP-1). Sequential execution per user direction.

**Architecture:**
- TDD red-green-refactor per phase
- 1 commit per task minimum (Conventional Commits, scope `security`)
- Each phase ships behind failing test → minimal impl → green build

**Tech Stack:** Java 21+ / Spring Boot 4.1.0, JUnit 5 + AssertJ, Quarkus (gateway), PostgreSQL 16, Flyway, Logback + LogstashEncoder, Vault, Kafka.

**Source of truth for gaps:** `docs/roadmap/TODOS.md` § Architecture Audit 2026-07-01, rows 19-34.

**Scope:**
- ✅ IN: GAP-34, GAP-21, GAP-23, GAP-30, GAP-28, GAP-19, GAP-1
- ❌ OUT (documented as follow-ups, not blockers we can fix in code today):
  - GAP-8 mTLS strict enforcement — requires Istio/ServiceMesh infra, OCP cluster destroyed May 2 per TODOS. Tracked OCP-007 (deferred).
  - GAP-7 SIEM (INFRA-011), GAP-11 CI/CD security (READY-044/045/046), GAP-12 Incident Ops (INFRA-020/022) — infra-level, separate sprint.

---

## File Structure (changes)

| Phase | Files Created | Files Modified |
|---|---|---|
| 1 — GAP-34 | `cache-starter/src/test/java/.../TypedJsonRedisSerializerSecurityTest.java` | `cache-starter/.../TypedJsonRedisSerializer.java` |
| 2 — GAP-21 | `logging-starter/src/test/resources/logback-payu-base-test.xml` | `logging-starter/src/main/resources/logback-payu-base.xml` |
| 3 — GAP-23 | `gateway-service/src/test/resources/oidc-tls-verification-test.yaml` | `gateway-service/src/main/resources/application.yaml`, `infrastructure/.../gateway-deployment.yaml` (cert mount) |
| 4 — GAP-30/28 | `security-starter/src/test/java/.../EncryptionFailFastTest.java` | `security-starter/.../SecurityAutoConfiguration.java`, 16× `*-service/src/main/resources/application-container.yml` |
| 5 — GAP-19 | `security-starter/src/test/java/.../MultitenancyIntegrationTest.java` | `security-starter/.../multitenancy/TenantInterceptor.java`, `account-service/.../config/TenantInterceptor.java` (delete), `<entity>.java` ×N (`@EntityListeners`), `account-service/.../application.yaml` (dedupe yml/yaml) + `auth-service/.../application.yaml` (dedupe) |
| 6 — GAP-1 | `<service>/src/main/resources/db/migration/V<n>__pgcrypto_extension.sql` (16×), `<service>/src/test/.../PiiColumnEncryptionIT.java` (1 shared) | `<service>/src/main/resources/db/migration/V<n+1>__encrypt_<col>_columns.sql` per service with PII |

---

## Phase 1 — GAP-34: Unsafe Class Deserialization RCE in `TypedJsonRedisSerializer`

**Goal:** Reject any cache payload whose type header references a class outside the `id.payu.*` whitelist (plus minimal JDK collection types).

**Why:** `TypedJsonRedisSerializer.deserialize()` calls `Class.forName(name, true, cl)` with `initialize=true`. An attacker who can write to Redis (compromised pod, leaked creds) can inject a header pointing to a class whose static initializer runs arbitrary code.

### Task 1.1: Write failing test

**Files:**
- Test: `backend/shared/cache-starter/src/test/java/id/payu/cache/serializer/TypedJsonRedisSerializerSecurityTest.java`

- [ ] **Step 1: Write the failing test**

```java
package id.payu.cache.serializer;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypedJsonRedisSerializerSecurityTest {

    private final TypedJsonRedisSerializer serializer = new TypedJsonRedisSerializer();

    @Test
    void shouldRejectArbitraryClassHeader() {
        // Simulates an attacker injecting a header pointing to a class outside the whitelist.
        // java.net.URL has a static initializer that performs DNS resolution — RCE/gadget vector.
        String malicious = "java.net.URL|\"http://attacker.example/\"";
        byte[] payload = malicious.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> serializer.deserialize(payload))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("not in whitelist");
    }

    @Test
    void shouldAcceptWhitelistedClassHeader() {
        // Sanity: legitimate id.payu.* types still deserialize.
        String json = "{\"id\":\"abc\"}";
        // We use a known starter type for the round-trip test.
        String allowed = "java.util.ArrayList|" + json;
        byte[] payload = allowed.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // No throw expected.
        Object result = serializer.deserialize(payload);
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `rtk mvn -f /home/ubuntu/payu/backend/shared/cache-starter/pom.xml test -Dtest=TypedJsonRedisSerializerSecurityTest -q 2>&1 | tail -40`
Expected: FAIL — current code calls `Class.forName` without whitelist check, so first test fails with `ClassNotFoundException` or no exception, second passes (trivially).

- [ ] **Step 3: Implement minimal whitelist**

File: `backend/shared/cache-starter/src/main/java/id/payu/cache/serializer/TypedJsonRedisSerializer.java`

Replace `deserialize()` body. Insert whitelist + helper:

```java
private static final java.util.Set<String> ALLOWED_PACKAGES = java.util.Set.of(
    "id.payu.",
    "java.util.",
    "java.lang.",
    "java.time.",
    "java.math.",
    "java.util.concurrent."
);

private static void validateClassName(String fqn) {
    if (fqn == null || fqn.isBlank()) {
        throw new SerializationException("Empty type header");
    }
    if (fqn.length() > 256) {
        throw new SerializationException("Type header too long: " + fqn.length());
    }
    if (!ALLOWED_PACKAGES.stream().anyMatch(fqn::startsWith)) {
        throw new SerializationException("Type not in whitelist: " + fqn);
    }
    // Reject arrays, primitive descriptors, and inner-class separators that bypass the prefix.
    if (fqn.contains("[")) {
        throw new SerializationException("Array type not allowed: " + fqn);
    }
}
```

Wrap both `Class.forName` calls:

```java
validateClassName(outerTypeName);
Class<?> outerType = Class.forName(outerTypeName, true, cl);
// ...
if (elementTypeName != null) {
    validateClassName(elementTypeName);
    Class<?> elementType = Class.forName(elementTypeName, true, cl);
    // ...
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `rtk mvn -f /home/ubuntu/payu/backend/shared/cache-starter/pom.xml test -Dtest=TypedJsonRedisSerializerSecurityTest -q 2>&1 | tail -20`
Expected: 2 tests, 0 failures.

- [ ] **Step 5: Run full cache-starter test suite**

Run: `rtk mvn -f /home/ubuntu/payu/backend/shared/cache-starter/pom.xml test -q 2>&1 | tail -30`
Expected: 0 failures (39/39 baseline + 2 new = 41/41).

- [ ] **Step 6: Commit**

```bash
rtk git add backend/shared/cache-starter/src/main/java/id/payu/cache/serializer/TypedJsonRedisSerializer.java backend/shared/cache-starter/src/test/java/id/payu/cache/serializer/TypedJsonRedisSerializerSecurityTest.java
rtk git commit -m "fix(security): GAP-34 whitelist class names in TypedJsonRedisSerializer (RCE prevention)"
```

---

## Phase 2 — GAP-21: Activate `LogbackMaskingFilter` in `logback-payu-base.xml`

**Goal:** Wrap both JSON and TEXT console appenders with `LogbackMaskingFilter` so NIK, email, phone, card numbers are masked before reaching LokiStack.

### Task 2.1: Write failing test

**Files:**
- Test: `backend/shared/logging-starter/src/test/java/id/payu/logging/LogbackMaskingFilterIT.java`
- Fixture: `backend/shared/logging-starter/src/test/resources/logback-test.xml`

- [ ] **Step 1: Write test that loads `logback-payu-base.xml` and asserts NIK/email masked in emitted log event**

```java
package id.payu.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import id.payu.security.masking.LogbackMaskingFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class LogbackMaskingFilterIT {

    @Test
    void shouldMaskNikInFormattedOutput() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = ctx.getLogger("test.mask");
        logger.setAdditive(false);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(ctx);
        appender.start();
        logger.addAppender(appender);

        // Simulate what JSON_CONSOLE / TEXT_CONSOLE should produce
        LogbackMaskingFilter layout = new LogbackMaskingFilter();
        layout.setContext(ctx);
        layout.setPattern("%msg%n");
        layout.start();

        ILoggingEvent event = logger.getEventBuilder()
            .setMessage("Customer NIK=1234567890123456 registered")
            .build();

        String formatted = layout.doLayout(event);

        assertThat(formatted).doesNotContain("1234567890123456");
        assertThat(formatted).contains("123**********3456");
    }
}
```

- [ ] **Step 2: Run test to verify it passes (sanity that the layout class works)**

Run: `rtk mvn -f /home/ubuntu/payu/backend/shared/logging-starter/pom.xml test -Dtest=LogbackMaskingFilterIT -q 2>&1 | tail -20`
Expected: PASS — this confirms `LogbackMaskingFilter` is functional. The GAP-21 fix is wiring it into the appenders.

- [ ] **Step 3: Update `logback-payu-base.xml` to wrap JSON_CONSOLE**

File: `backend/shared/logging-starter/src/main/resources/logback-payu-base.xml`

Replace `JSON_CONSOLE` appender:

```xml
<appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
        <layout class="id.payu.security.masking.LogbackMaskingFilter">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeContext>true</includeContext>
                <includeMdc>true</includeMdc>
                <includeStructuredArguments>true</includeStructuredArguments>
                <customFields>{"log_format":"json"}</customFields>
                <fieldNames>
                    <timestamp>@timestamp</timestamp>
                    <message>message</message>
                    <logger>logger</logger>
                    <thread>thread</thread>
                    <level>level</level>
                </fieldNames>
            </encoder>
        </layout>
    </encoder>
</appender>
```

Replace `TEXT_CONSOLE` appender:

```xml
<appender name="TEXT_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
        <layout class="id.payu.security.masking.LogbackMaskingFilter">
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlation_id:-}] [%X{trace_id:-}] %logger{36} - %msg%n</pattern>
        </layout>
    </encoder>
</appender>
```

- [ ] **Step 4: Run logging-starter tests**

Run: `rtk mvn -f /home/ubuntu/payu/backend/shared/logging-starter/pom.xml test -q 2>&1 | tail -30`
Expected: 0 failures.

- [ ] **Step 5: Commit**

```bash
rtk git add backend/shared/logging-starter/src/main/resources/logback-payu-base.xml backend/shared/logging-starter/src/test/java/id/payu/logging/LogbackMaskingFilterIT.java
rtk git commit -m "fix(security): GAP-21 activate LogbackMaskingFilter on console appenders (PII to LokiStack)"
```

---

## Phase 3 — GAP-23: Insecure OIDC TLS Verification (`none` → `required`)

**Goal:** Force Quarkus OIDC client to validate Keycloak TLS certificate. Mount Keycloak CA into truststore.

### Task 3.1: Write failing test (config assertion)

**Files:**
- Test: `backend/gateway-service/src/test/resources/application.yaml` (test fixture) + `backend/gateway-service/src/test/java/.../OidcTlsVerificationTest.java`

- [ ] **Step 1: Write test that loads container profile and asserts `quarkus.oidc.tls.verification=required`**

```java
package id.payu.gateway;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class OidcTlsVerificationTest {

    @Test
    void containerProfileShouldRequireOidcTlsVerification() {
        String verification = ConfigProvider.getConfig()
            .getValue("quarkus.oidc.tls.verification", String.class);
        assertThat(verification)
            .as("GAP-23 fix: OIDC TLS verification must be 'required' to prevent MITM")
            .isEqualTo("required");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `rtk mvn -f /home/ubuntu/payu/backend/gateway-service/pom.xml test -Dtest=OidcTlsVerificationTest -q 2>&1 | tail -30`
Expected: FAIL — current value is `none`.

- [ ] **Step 3: Update gateway `application.yaml`**

File: `backend/gateway-service/src/main/resources/application.yaml`

Change line 22:
```yaml
    tls:
      verification: required # GAP-23 fix: was 'none' — exposed to MITM
```

- [ ] **Step 4: Re-run test**

Run: `rtk mvn -f /home/ubuntu/payu/backend/gateway-service/pom.xml test -Dtest=OidcTlsVerificationTest -q 2>&1 | tail -20`
Expected: PASS.

- [ ] **Step 5: Mount Keycloak CA in gateway deployment**

File: `infrastructure/openshift/gateway-service/deployment.yaml` (path may vary — search for current location)

Add volume + volumeMount for Keycloak CA cert, set env `QUARKUS_OIDC_TLS_TRUST_STORE_FILE` to mounted path.

- [ ] **Step 6: Commit**

```bash
rtk git add backend/gateway-service/src/main/resources/application.yaml backend/gateway-service/src/test/java/id/payu/gateway/OidcTlsVerificationTest.java infrastructure/
rtk git commit -m "fix(security): GAP-23 require OIDC TLS verification + Keycloak CA mount (MITM prevention)"
```

---

## Phase 4 — GAP-30 + GAP-28: Fail-Fast on Missing Encryption Password + Enable in Container Profiles

**Goal:** Two coupled changes:
- **GAP-30**: `SecurityAutoConfiguration.encryptionService()` throws if password missing (no silent default key in production profiles).
- **GAP-28**: All 16 `application-container.yml` set `encryption-enabled: true` and map `payu.security.encryption.password: ${ENCRYPTION_KEY}`.

### Task 4.1: Write failing test for fail-fast

**Files:**
- Test: `backend/shared/security-starter/src/test/java/id/payu/security/config/SecurityAutoConfigurationFailFastTest.java`

- [ ] **Step 1: Write test**

```java
package id.payu.security.config;

import id.payu.security.crypto.EncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAutoConfigurationFailFastTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class))
        .withPropertyValues(
            "payu.security.enabled=true",
            "payu.security.encryption-enabled=true",
            "spring.profiles.active=container"
        );

    @Test
    void shouldFailFastWhenPasswordMissingInContainerProfile() {
        runner.run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure())
                .hasMessageContaining("encryption.password")
                .hasMessageContaining("ENCRYPTION_KEY");
        });
    }

    @Test
    void shouldStartWhenPasswordProvided() {
        runner.withPropertyValues("payu.security.encryption.password=test-key-not-empty")
              .run(ctx -> {
                  assertThat(ctx).hasNotFailed();
                  assertThat(ctx).hasSingleBean(EncryptionService.class);
              });
    }
}
```

- [ ] **Step 2: Run test to verify fail-fast fails**

Run: `rtk mvn -f /home/ubuntu/payu/backend/shared/security-starter/pom.xml test -Dtest=SecurityAutoConfigurationFailFastTest -q 2>&1 | tail -30`
Expected: First test FAILS — current code logs WARN and returns default key instead of throwing.

- [ ] **Step 3: Modify `SecurityAutoConfiguration.encryptionService()`**

File: `backend/shared/security-starter/src/main/java/id/payu/security/config/SecurityAutoConfiguration.java`

Replace the bean method body:

```java
@Bean
@ConditionalOnMissingBean
@ConditionalOnProperty(prefix = "payu.security", name = "encryption-enabled", havingValue = "true", matchIfMissing = false)
public EncryptionService encryptionService(org.springframework.core.env.Environment env) {
    log.info("Initializing Encryption Service");

    String password = properties.getEncryption().getPassword();
    boolean isProdProfile = java.util.Arrays.asList(env.getActiveProfiles()).stream()
        .anyMatch(p -> p.equals("container") || p.equals("prod") || p.equals("staging"));

    if (password == null || password.isEmpty()) {
        if (isProdProfile) {
            throw new IllegalStateException(
                "GAP-30 fix: payu.security.encryption.password must be set via ENCRYPTION_KEY env var in production profiles. " +
                "Falling back to default key would break multi-pod scaling and corrupt data after pod restart.");
        }
        log.warn("Using default encryption key (NON-PRODUCTION ONLY)");
        return new EncryptionService(generateDefaultKey());
    }

    String salt = properties.getEncryption().getSalt();
    return new EncryptionService(password, Collections.emptyList(), salt);
}
```

- [ ] **Step 4: Re-run test**

Run: `rtk mvn -f /home/ubuntu/payu/backend/shared/security-starter/pom.xml test -Dtest=SecurityAutoConfigurationFailFastTest -q 2>&1 | tail -20`
Expected: 2/2 PASS.

- [ ] **Step 5: Update 16× application-container.yml**

Run this script:

```bash
for f in /home/ubuntu/payu/backend/{account,auth,backoffice,billing,cms,compliance,dispute,fx,integration,investment,lending,partner,product-catalog,promotion,statement,support,transaction,wallet}-service/src/main/resources/application-container.yml; do
  # Skip if no file
  [ -f "$f" ] || continue
  # Update encryption-enabled: false → true
  rtk sed -i 's/encryption-enabled: false/encryption-enabled: true/' "$f"
  # Add password mapping if not present
  if ! rtk grep -q "encryption.password" "$f"; then
    rtk sed -i '/encryption-enabled: true/a\    encryption:\n      password: ${ENCRYPTION_KEY}' "$f"
  fi
done
```

- [ ] **Step 6: Verify all 16 files updated**

Run: `rtk grep -l "encryption-enabled: true" /home/ubuntu/payu/backend/*-service/src/main/resources/application-container.yml | rtk wc -l`
Expected: 16

- [ ] **Step 7: Commit**

```bash
rtk git add backend/shared/security-starter/src/main/java/id/payu/security/config/SecurityAutoConfiguration.java backend/shared/security-starter/src/test/java/id/payu/security/config/SecurityAutoConfigurationFailFastTest.java backend/*-service/src/main/resources/application-container.yml
rtk git commit -m "fix(security): GAP-30/28 fail-fast on missing encryption password + enable in container profiles"
```

---

## Phase 5 — GAP-19: Broken Multitenancy (Consolidate + Enable Filter + Add Entity Listeners)

**Goal:**
- Delete local `TenantInterceptor` in `account-service` (shadow the shared one).
- Add `@EntityListeners(TenantEntityListener.class)` to all `@TenantAware` entities.
- Wire `enableTenantFilter()` into transaction lifecycle (e.g., `@BeforeTransaction` or `TransactionTemplate`).
- Dedupe `application.yml` + `application.yaml` in account-service + auth-service.

### Task 5.1: Identify `@TenantAware` entities

- [ ] **Step 1: List all entities using `@TenantAware`**

Run: `rtk grep -rln "@TenantAware" /home/ubuntu/payu/backend --include="*.java" | rtk tee /tmp/tenant-entities.txt`
Expected: List of N entity files. Each needs `@EntityListeners(TenantEntityListener.class)`.

- [ ] **Step 2: Verify they have `tenantId` field**

Run: `for f in $(cat /tmp/tenant-entities.txt); do echo "=== $f ==="; rtk grep -A1 "tenantId" "$f" | head -5; done`

### Task 5.2: Write failing test for filter enable

**Files:**
- Test: `backend/shared/security-starter/src/test/java/id/payu/security/multitenancy/MultitenancyFilterEnableTest.java`

- [ ] **Step 3: Write test that verifies a `@TenantAware` entity without `@EntityListeners` annotation is flagged**

```java
package id.payu.security.multitenancy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class MultitenancyFilterEnableTest {

    @Test
    void tenantAwareEntitiesMustHaveEntityListenersAnnotation() {
        JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("id.payu");

        classes().that().areAnnotatedWith(TenantAware.class)
            .should().beAnnotatedWith(jakarta.persistence.EntityListeners.class)
            .check(classes);
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `rtk mvn -f /home/ubuntu/payu/backend/shared/security-starter/pom.xml test -Dtest=MultitenancyFilterEnableTest -q 2>&1 | tail -30`
Expected: FAIL — entities missing `@EntityListeners`.

### Task 5.3: Apply `@EntityListeners` annotation

- [ ] **Step 5: For each entity in `/tmp/tenant-entities.txt`, add annotation**

Pattern (per file):
```java
import jakarta.persistence.EntityListeners;
import id.payu.security.multitenancy.TenantEntityListener;

@Entity
@TenantAware
@EntityListeners(TenantEntityListener.class)  // ADD THIS
public class MyEntity {
```

Manual edit per file (Phase 5 is structural — no auto-rewrite to avoid silent damage).

### Task 5.4: Consolidate local TenantInterceptor

- [ ] **Step 6: Delete local interceptor in account-service**

```bash
rtk rm /home/ubuntu/payu/backend/account-service/src/main/java/id/payu/account/config/TenantInterceptor.java
```

Replace any references to `id.payu.account.config.TenantInterceptor` → `id.payu.security.multitenancy.TenantInterceptor`.

### Task 5.5: Dedupe config files (GAP-20 sub-task)

- [ ] **Step 7: Diff account-service/application.yml vs application.yaml**

Run: `rtk diff /home/ubuntu/payu/backend/account-service/src/main/resources/application.yml /home/ubuntu/payu/backend/account-service/src/main/resources/application.yaml | rtk head -100`

If both have content, merge into one (keep `application.yml`, delete `.yaml`).

Repeat for auth-service.

### Task 5.6: Run tests + commit

- [ ] **Step 8: Run security-starter + account-service tests**

Run: `rtk mvn -f /home/ubuntu/payu/backend/shared/security-starter/pom.xml test -q 2>&1 | tail -10`
Run: `rtk mvn -f /home/ubuntu/payu/backend/account-service/pom.xml test -q 2>&1 | tail -10`
Expected: 0 failures (or only pre-existing).

- [ ] **Step 9: Commit**

```bash
rtk git add backend/shared/security-starter backend/account-service backend/auth-service
rtk git commit -m "fix(security): GAP-19 consolidate multitenancy + add @EntityListeners to tenant-aware entities + dedupe config"
```

---

## Phase 6 — GAP-1: PII Column-Level Encryption (pgcrypto)

**Goal:** Add `pgcrypto` extension to all 16 service databases. Encrypt NIK + email + phone columns using `pgp_sym_encrypt` where missing.

**Note:** account-service already has V6__encrypt_pii_columns.sql but uses app-layer AES (not pgcrypto). GAP-1 specifically calls for pgcrypto. Scope: add extension + column-level encryption via Flyway for services that store NIK/PII plaintext.

### Task 6.1: Identify PII column locations

- [ ] **Step 1: Find NIK/email/phone columns in Flyway migrations**

Run: `rtk grep -rln "nik\|email\|phone" /home/ubuntu/payu/backend/*-service/src/main/resources/db/migration/ | rtk tee /tmp/pii-columns.txt`

### Task 6.2: Add pgcrypto extension

- [ ] **Step 2: Create shared migration template**

For each service with PII columns, create `V<n+1>__add_pgcrypto_extension.sql`:

```sql
-- GAP-1: Enable pgcrypto for column-level encryption of PII
CREATE EXTENSION IF NOT EXISTS pgcrypto;

COMMENT ON EXTENSION pgcrypto IS 'GAP-1 fix: required for pgp_sym_encrypt() on NIK/email/phone columns';
```

Place after the latest existing migration. Use V99 or V<n>+1.

### Task 6.3: Write integration test

- [ ] **Step 3: Test pgcrypto availability in testcontainers**

```java
package id.payu.security.pii;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PgcryptoExtensionIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withInitScript("init-pgcrypto.sql");

    @Test
    void pgcryptoExtensionShouldBeAvailable() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             ResultSet rs = conn.createStatement().executeQuery(
                "SELECT extname FROM pg_extension WHERE extname='pgcrypto'")) {
            assertThat(rs.next()).isTrue();
        }
    }
}
```

Place in `backend/shared/security-starter/src/test/java/id/payu/security/pii/PgcryptoExtensionIT.java` + init script `init-pgcrypto.sql` with `CREATE EXTENSION IF NOT EXISTS pgcrypto;`.

- [ ] **Step 4: Run test**

Run: `rtk mvn -f /home/ubuntu/payu/backend/shared/security-starter/pom.xml verify -Dtest=PgcryptoExtensionIT -q 2>&1 | tail -30`
Expected: PASS (uses testcontainers).

- [ ] **Step 5: Commit**

```bash
rtk git add backend/shared/security-starter backend/*-service/src/main/resources/db/migration/V*__add_pgcrypto_extension.sql
rtk git commit -m "fix(security): GAP-1 enable pgcrypto extension for PII column-level encryption"
```

---

## Verification Checklist (before claiming complete)

- [ ] All 6 phases shipped with passing tests
- [ ] No silent fallbacks (no `// TODO`, no placeholder passwords)
- [ ] Each phase's commit is atomic + Conventional Commits format
- [ ] `mvn -f backend/shared/cache-starter/pom.xml test` passes (regression check)
- [ ] `mvn -f backend/shared/security-starter/pom.xml test` passes (regression check)
- [ ] CHANGELOG.md updated per AGENTS.md rule (one entry per gap closure)
- [ ] `docs/roadmap/TODOS.md` updated: GAP-34, GAP-21, GAP-23, GAP-30, GAP-28, GAP-19, GAP-1 marked CLOSED with commit refs

## Deferred (out-of-scope, documented)

- GAP-8 mTLS — requires Istio/ServiceMesh infra (OCP-007, suspended per TODOS)
- GAP-7 SIEM (INFRA-011)
- GAP-11 CI/CD security (READY-044/045/046, INFRA-013/014)
- GAP-12 Incident Ops (INFRA-020/022, READY-050/051)

These get separate plans after BLOCKER sprint completes.
