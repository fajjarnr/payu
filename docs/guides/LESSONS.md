# PayU Platform - Lessons Learned & Troubleshooting Guide

## 🐳 Containerization & Podman Compose

### 1. Podman-Compose Compatibility

* **Volume Syntax**: Current versions of `podman-compose` may fail with advanced Docker Compose volume types (like `type: persistent`). Use standard bind-mount or named volume syntax:

    ```yaml
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ```

* **Short-name Resolution**: Podman requires fully qualified image names to avoid interactive prompts. Always prepend `docker.io/library/` or `docker.io/` for official/public images.
* **Local Image Tagging**: Always provide an `image:` tag (e.g., `localhost/payu-service`) when using the `build` directive. This prevents Podman from using random hex IDs which makes referencing images easier.

### 2. Monorepo Build Contexts

* **The Shared Library Trap**: In a monorepo (like `backend/`), setting the build `context` to the service subfolder prevents access to shared siblings (e.g., `backend/shared/`).
* **The Fix**:
    1. Set `context` to the parent directory (e.g., `../../backend`).
    2. Set `dockerfile` to the relative path (e.g., `service-name/Dockerfile`).
    3. Update the `Dockerfile` to `COPY . .` from the root context.
    4. **Crucial**: Use Maven project selection flags `-pl :service-name -am` to build only the target service and its local dependencies.
    5. Update `COPY --from=build` paths to point into the service-specific `target` folder: `COPY --from=build /build/service-name/target/app.jar ...`

### 3. Permissions and Package Installation

* **UBI User Switching**: Red Hat UBI images often default to a non-root user (like `jboss` or `node`).
* **The Fix**: Always switch back to `USER root` before running `microdnf` or `dnf` to install packages (like `curl`), then switch back to the application user (e.g., `USER 185` or `USER 1001`).

    ```dockerfile
    USER root
    RUN microdnf install -y curl && microdnf clean all
    USER 185
    ```

### 4. Environment Variable Precision

* **Explicit over Implicit**: Even if `application.yml` has defaults, explicitly define `DB_URL`, `KAFKA_BROKERS`, and `REDIS_HOST` in `podman-compose.yml`.
* **Profile Activation**: Always set `SPRING_PROFILES_ACTIVE: container` to ensure container-specific configurations are loaded.
* **UBI9 Minimal & Curl**: The standard `curl` package conflicts with `curl-minimal` in UBI9 minimal images.
  * **The Fix**: Use `microdnf install -y curl-minimal` instead. If you must use full curl, you might need `--allowerasing` (though `curl-minimal` is usually sufficient for healthchecks).

### 5. Memory Limits & OOM Kills (Exit Code 137)

* **The Problem**: Java applications (especially Quarkus/Spring Boot) in containers may be killed by the OOM Killer (Exit Code 137) if the container memory limit is too tight compared to the JVM heap requirements.
* **The Symptom**: Container starts, runs for a few seconds/minutes, then exits silently or with "Killed". `podman ps` shows "Exited (137)".
* **The Fix**: Increase the `mem_limit` or `deploy.resources.limits.memory`.
  * **Example**: Updating `dukcapil-simulator` from `256M` to `512M` resolved startup crashes.
  * **Note**: JVM `MAX_RAM_PERCENTAGE` automatically adjusts heap size based on container limits, but overhead (metaspace, thread stacks, native memory) must also fit within the limit.

### 6. Port Standardization Strategy

* **The Problem**: Using different internal ports for every service (8001, 8002, 8003...) creates maintenance hell in Dockerfiles (EXPOSE instructions) and Healthchecks.
* **The Fix**: Standardize ALL backend services to use **8080** internally.
  * **Dockerfile**: Always `EXPOSE 8080`.
  * **Docker Compose**: Map unique external ports to standard internal port: `"8001:8080"`, `"8002:8080"`.
  * **Healthcheck**: Always check `http://localhost:8080/...`.

### 7. Environment vs. Persistence Mismatches

* **The Problem**: Changing a password in `.env` (e.g., `POSTGRES_PASSWORD`) does **not** update the password of an existing, persistent database volume. The container starts, but applications fail to connect with "Password authentication failed".
* **The Fix**:
  1. **Reset**: Delete the volume (`podman volume rm ...`) to let it recreate with the new password (DATA LOSS WARNING).
  2. **Sync**: Update `.env` to match the *actual* password currently used by the database (Safe).
  3. **SQL**: Manually change the password via `ALTER USER` inside the database.

## 🛠️ Build & Dependency Management

### 1. Multi-Module Project Dependencies

* **GroupId Consistency**: In a multi-module Maven project where submodules are grouped (e.g., `backend/shared/`), ensure dependency references use the correct `groupId`.
  * **Example**: `id.payu:api-commons` vs `id.payu.shared:api-commons`. An incorrect GroupId leads to build failures finding the artifact, even if the ArtifactId is correct.

### 2. Monorepo Scripting

* **Context Path Traps**: When writing support scripts (Python/Bash) for a monorepo, do not rely solely on the `build context` path from `compose.yml` to check for file existence (like `pom.xml`).
  * **Better Approach**: Resolve paths based on the `Dockerfile` location or explicitly handle the subdirectory structure.

## ☕ Java & Spring Boot

### 1. Naming Consistency (Entity vs Repo vs Test)

* **The Issue**: Discrepancies between `userId` and `customerId` often lead to `cannot find symbol` or `BeanCreationException` during Flyway/JPA initialization.
* **Lesson**: Standardize on `customerId` for all external-facing IDs across the platform.

### 2. Custom Annotations & Enums

* **Inner Class Resolution**: When using custom annotations with inner enums (like `@Audited(level = AuditLevel.INFO)`), Java may fail to resolve the enum if not fully qualified or correctly imported.
* **Correction**: Use `Audited.AuditLevel.INFO` to guarantee resolution.

### 4. Ambiguous Enum References (Swagger vs Security Starter)

* **The Problem**: Importing `id.payu.security.annotation.Audited.Operation` can conflict with `io.swagger.v3.oas.annotations.Operation`, leading to `reference to Operation is ambiguous` compilation errors.
* **The Fix**: Use semi-qualified names in annotations: `@Audited(operation = Audited.Operation.CREATE, ...)` instead of importing the inner enum directly.

### 5. Abstract Exception Instantiation in Tests

* **The Problem**: Making a base domain exception `abstract` prevents direct instantiation in unit tests, leading to compilation errors.
* **The Fix**: Either make the base exception concrete with a generic error code (e.g., `COMPLIANCE_GENERIC_ERROR`) or ensure tests always use a concrete subclass.

### 3. JPA Entity Architecture (Pragmatic Hexagonal)

* **The Problem**: In a Hexagonal Architecture, repositories were extending `JpaRepository` using standard Domain Models (`ScheduledTransfer`, `Transaction`) that lacked `@Entity` annotations.
* **The Symptom**: `UnsatisfiedDependencyException`: Not a managed type.
* **The Fix**: Annotate the Domain Model class with `@Entity`, `@Table`, and `@Id`.
* **Best Practice**: Ensure ALL classes used in `JpaRepository<T, ID>` are properly annotated entities.

### 4. Value Object Mapping

* **The Problem**: `Money` Value Object (containing `amount` and `currency`) cannot be persisted directly without `@Embedded` or `AttributeConverter`.
* **The Legacy Fix**: Using deprecated `amountValue` and `currencyCode` fields mapped with `@Column`, while marking the main `Money` object as `@Transient`.

### 5. JPA Boolean Naming

* **The Issue**: Derived Query Methods (like `findByActiveTrue`) expect a field named `active`. If the field is `isActive`, the method must be `findByIsActiveTrue`.

## 🗄️ Database Management

### 1. Initialization Order

* **Postgres Healthchecks**: A healthy Postgres container doesn't mean the databases in `init-db.sql` are ready.
* **The Fix**: Update healthchecks to check a specific database: `pg_isready -U payu -d payu_account`.

### 2. Partitioning Limitations

* **Hash Partitioning Defaults**: PostgreSQL (as of v16) does **not** support a `DEFAULT` partition for `HASH` partitioning strategies. Attempting to create one causes a migration failure.
  * **The Fix**: Do not create a default partition for HASH strategies. Ensure the modulus/remainder coverage is complete (which it naturally is).
* **Unique Constraints**: A unique constraint on a partitioned table **must include** all partitioning columns. Attempting to create a unique index on just the ID when partitioned by `account_id` will fail.
  * **The Fix**: Add the partition key to the unique index definition: `CREATE UNIQUE INDEX ... ON table (id, partition_key)`.

### 3. Index Predicates & Immutability

* **Mutable Functions in Indexes**: You cannot use `CURRENT_DATE`, `NOW()`, or `CURRENT_TIMESTAMP` in a `WHERE` clause of an index (partial index) because these functions are not IMMUTABLE.
  * **The Fix**: Remove time-based filtering from the index definition or use a mechanism that doesn't rely on dynamic dates.

### 4. Podman Build Caching

* **Stale Maven Layers**: Podman's layer caching is aggressive. If you update source code but the `mvn package` step is cached, old logic persists.
  * **The Fix**: Use `podman build --no-cache` when debugging cryptic logic errors.
* **Context Contamination**: Without a `.dockerignore` file, `COPY . .` copies `target/` directories from the host. If the host has stale compiled classes, they can contaminate the build.
  * **The Fix**: Create `.dockerignore` excluding `**/target`. Clean host target (`rm -rf backend/*/target`) before critical builds.
* **Compose Service Naming**: `podman-compose` can sometimes fail to map service names correctly or reuse existing containers.
  * **Fallback**: Use `podman run` with explicit environment variables (`-e`) for reliable debugging.

### 5. Flyway Development

* **Checksum Mismatches**: Changing a migration script after it has run locally causes checksum errors.
* **The Strategy**: In dev/local environment, it is often faster to `DROP DATABASE` and let Flyway recreate it from scratch than to manually patch the `flyway_schema_history` table.

## 🛡️ Security & Configuration

### 1. Spring Bean Instantiation

* **No-Args Constructor**: Beans instantiated by Spring (especially Filters or Interceptors that might be proxied) **must** have a no-args constructor available, even if they have final fields.
  * **The Fix**: Remove `final` from fields and provide a protected/public no-args constructor to avoid `BeanInstantiationException`.

### 2. OAuth2 Configuration

* **Silent Failures**: Missing `JwtDecoder` beans often manifest as `UnsatisfiedDependencyException` deep in the security chain.
  * **The Fix**: Ensure `issuer-uri` or `jwk-set-uri` is explicitly defined in `application.yml` or a `JwtDecoder` bean is manually supplied.

### 3. Quarkus Startup Validation

* **Mandatory Properties**: Quarkus performs strict validation on `@ConfigProperty`. If a property is defined but resolved to an empty string (e.g., via `${ENV:}`), it may fail with `NoSuchElementException`.
  * **The Fix**: Always provide a non-empty fallback in `podman-compose.yml` for mandatory secrets or config keys:

      ```yaml
      WEBHOOK_PARTNER_1_SECRET: ${WEBHOOK_PARTNER_1_SECRET:-dummy_secret}
      ```

## 🏗️ Monorepo Infrastructure

### 1. Shared Library Env Var Mapping

* **Custom Starters**: When using custom Spring Boot starters (like `cache-starter`), they often use specific property prefixes (e.g., `payu.cache.*`). Standard environment variables like `REDIS_HOST` might not be enough if the starter doesn't map them explicitly.
  * **The Fix**: Double-check the `@ConfigurationProperties` prefix in the starter and provide matching env vars in `podman-compose.yml`:

      ```yaml
      PAYU_CACHE_REDIS_HOST: redis
      ```

### 2. Selective Maven Builds (Resource Optimization)

* **The Problem**: Attempting to build the entire monorepo root in every service Dockerfile leads to "Too many open files" and extreme memory usage.
  * **The Fix**: Use selective builds and project selection:

      ```dockerfile
      RUN mvn package -DskipTests -pl :service-name -am
      ```

### 4. Healthcheck Authentication (401 Unauthorized)

* **The Problem**: Health endpoints (`/q/health` for Quarkus, `/actuator/health` for Spring Boot) may return `401 Unauthorized` if global security filters are too aggressive.
* **The Fix (Quarkus)**: Ensure `quarkus.health.security.enabled=false` or explicitly permit the health path in your security configuration/filter.
* **The Fix (Spring Boot)**: Ensure `management.endpoints.web.exposure.include=health` and that the security filter chain permits `/actuator/**`.
* **Liveness Probes & Context Paths**:
  * **Probes missing**: By default, Spring Boot does not expose `/actuator/health/liveness` unless `management.endpoint.health.probes.enabled=true`.
  * **Context Path**: If `server.servlet.context-path` is set (e.g., `/compliance-service`), the healthcheck URL in `podman-compose.yml` MUST include it: `http://localhost:8087/compliance-service/actuator/health/liveness`.
  * **401 in Spring Boot**: If `/actuator/health/liveness` returns 401 even if `/actuator/health` is permitted, ensure the `requestMatchers` use wildcards (`/actuator/**`) to cover sub-paths.

### 5. Misconfigured Service Labels (Spring Boot vs Quarkus)

* **The Problem**: A service built with Spring Boot but configured in `docker-compose.yml` using Quarkus environment variables (e.g., `QUARKUS_DATASOURCE_JDBC_URL`) and healthchecks (`/q/health`) will fail to start or report as unhealthy.
* **The Fix**: Ensure the configuration matches the framework:
  * **Spring**: `SPRING_DATASOURCE_URL`, `actuator/health/liveness`.
  * **Quarkus**: `QUARKUS_DATASOURCE_JDBC_URL`, `q/health`.

### 6. Vault Dev Mode Healthcheck

* **The Problem**: `vault status` inside a container defaults to HTTPS, causing 401/error when Vault is running in `-dev` mode (HTTP).
* **The Fix**: Explicitly set `VAULT_ADDR` in the healthcheck command:

  ```yaml
  healthcheck:
    test: ["CMD-SHELL", "VAULT_ADDR=http://127.0.0.1:8200 vault status || exit 1"]
  ```

## 🧪 Systematic Debugging

### 6. Spring Boot 3.4 Security & Public Endpoints (Feb 2026)

* **The Problem**: Spring Security OAuth2 resource server configuration can intercept requests before permitAll() rules are evaluated, causing 401 errors even on public endpoints like `/actuator/health` and `/api/v1/accounts/register`.
* **Root Cause**: When using `oauth2ResourceServer().jwt()`, Spring creates a filter chain that validates JWT tokens BEFORE the authorization rules (`permitAll()`) are checked.
* **The Fix**: Use `WebSecurityCustomizer` bean to completely bypass Spring Security for specific paths:

  ```java
  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
      return (web) -> web.ignoring()
              .requestMatchers("/actuator/**")
              .requestMatchers("/api/v1/accounts/register")
              .requestMatchers("/api/v1/auth/login");
  }
  ```

* **Note**: Spring will warn "This is not recommended" but this is necessary when OAuth2 resource server is enabled globally.
* **Alternative**: Disable OAuth2 for specific paths using `securityMatcher()`.

### 7. Gateway Service URL Configuration (Feb 2026)

* **The Problem**: Gateway proxying fails with "Connection refused: localhost/127.0.0.1:8081" even though service is running.
* **Root Cause**: Default service URLs in `application.yaml` use `localhost:PORT` which doesn't resolve in container networks.
* **The Fix**: Update default URLs to use service names from container network:

  ```yaml
  services:
    account-service:
      url: ${ACCOUNT_SERVICE_URL:http://account-service:8001}  # NOT localhost:8081
  ```

* **Environment Variable Mismatch**: podman-compose.yml may set different variable names (e.g., `ROUTES_ACCOUNT_URL` vs `ACCOUNT_SERVICE_URL`). Ensure ENV variable names match config property names.

### 8. API Key vs JWT Authorization Layering (Feb 2026)

* **The Problem**: Requests return "MISSING_API_KEY" even after JWT token is provided.
* **Root Cause**: Multiple security filters are chained (ApiKeyValidationFilter → AuthorizationFilter). If API key validation is enabled, it blocks requests before JWT validation.
* **The Fix**: Either:
  1. Disable API key validation for dev: `gateway.api-keys.enabled=false`
  2. Add public endpoints to API key bypass paths: `gateway.api-keys.bypass-paths=/api/v1/accounts/register`

## 🎨 Frontend & Design System

### 1. Cultural vs. Professional Aesthetics

* **Observation**: Attempting to force cultural themes (e.g., "Wayang", "Javanese Philosophy") into a Fintech UI can clash with user expectations for "Premium" and "Trust".
* **Lesson**: Users prefer standard international banking aesthetics (Clean, White, Sans-serif, Glassmorphism) for financial products. Use cultural elements very subtly or not at all if the goal is "Premium Global Standard".

### 2. Responsive Card Design (The "Golden Ratio" Fix)

* **The Problem**: Fixed pixel widths (e.g., `w-[350px]`) for Credit Card components break on small mobile screens (iPhone SE) or look tiny on large desktops.
* **The Fix**: Use `vw` (viewport width) units combined with `aspect-ratio` to maintain the ISO/IEC 7810 ID-1 standard.
  * **Snippet**: `w-[85vw] max-w-[340px] aspect-[1.586]` ensures the card scales perfectly while maintaining the correct physical ratio. Update text sizes to be relative (`text-[3vw]`) to scale with the card.

### 3. Mobile Layout Stacking

* **The Problem**: "Zig-zag" or staggered grid layouts that look dynamic on Desktop often break flow on Mobile, leading to overlapping or confusing content.
* **The Fix**: Switch to `flex-col` to stack elements vertically on mobile. Crucially, add significant vertical padding (`py-16` or `py-20`) to containers to prevent content from being occluded by fixed headers or bottom navigation bars.
