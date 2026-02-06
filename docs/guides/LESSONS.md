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

### 6. Port Standardization (Feb 2026 Mass Update)

*   **The Problem**: Managing 22 different internal ports (8001-8099) caused constant "unhealthy" statuses and broken gateways because of mismatches between `application.yml`, Dockerfiles, and `docker-compose` healthchecks.
*   **The Standard**: All 22 microservices (Java, Python, Quarkus) MUST listen on internal port **8080**.
*   **Why?**:
    *   **Convention over Configuration**: DNS-based service discovery (e.g., `http://service-name:8080`) is more reliable than remembering unique ports.
    *   **Cloud-Native Compliance**: Standard port for non-root containers in OpenShift/K8s.
    *   **Unified Monitoring**: Simple, consistent healthcheck and Prometheus scrape configs.
*   **The Implementation**:
    *   **Dockerfile**: Universal `EXPOSE 8080`.
    *   **Application**: Enforce `server.port=8080` or use `PORT` env var default.
    *   **Compose**: Use unique host ports (e.g., `8001:8080`) but always point healthcheck to `localhost:8080`.
    *   **Gateway**: Standardize all backend URLs to port 8080.

### 7. Environment vs. Persistence Mismatches

*   **The Problem**: Changing a password in `.env` (e.g., `POSTGRES_PASSWORD`) does **not** update the password of an existing, persistent database volume. The container starts, but applications fail to connect with "Password authentication failed".
*   **The Fix**:
    1.  **Reset**: Delete the volume (`podman volume rm ...`) to let it recreate with the new password (DATA LOSS WARNING).
    2.  **Sync**: Update `.env` to match the *actual* password currently used by the database (Safe).
    3.  **SQL**: Manually change the password via `ALTER USER` inside the database.

### 8. Python ML Containerization Strategy (Feb 4, 2026)
*   **The Problem**: Red Hat UBI9 Minimal images are excellent for security but lack system libraries required for ML/CV tasks (like OpenCV's dependency on `libGL.so.1` and `libgomp.so.1`).
*   **The Fix**: For services requiring heavy C-extensions (OpenCV, PyTorch, PaddleOCR), use `python:3.12-slim` (Debian-based) instead of UBI9. It simplifies installing system dependencies:
    ```dockerfile
    RUN apt-get update && apt-get install -y libgl1 libglib2.0-0 libgomp1 curl
    ```
*   **Performance Boost**: Switch from `pip` to `uv` (Astral) for package installation. Reduces build time for heavy ML libraries (PyTorch, Pandas) from 10m to 1.5m.
    ```dockerfile
    COPY --from=ghcr.io/astral-sh/uv:latest /uv /uv
    RUN /uv pip install --system --no-cache -r requirements.txt
    ```

### 9. Spring Boot Monorepo Build Pattern (Feb 4, 2026)
*   **The Problem**: Docker builds for services relying on local shared modules (`backend/shared/`) fail because the build context is often restricted to the service directory.
*   **The Fix**: "Decoupled Build" strategy.
    1.  **Build Artifacts on Host** (using root POM): `mvn -pl :service-name -am package`
    2.  **Copy Artifacts to Context**: `cp target/app.jar backend/service/target/`
    3.  **Simple Dockerfile**: `COPY target/app.jar /deployments/`
    This avoids complex Docker context juggling and leverages local Maven cache.

### 10. Pydantic Model Field Conflicts (Feb 4, 2026)
*   **The Problem**: Defining a class method named `success()` on a Pydantic model that has a field named `success` causes `AttributeError` at runtime. Pydantic v2 internals conflict with the method name.
*   **The Fix**: Rename factory methods to avoid colliding with field names. Use `create_success()` or `build_success()` instead of just `success()`.

### 11. ML Service Memory Limits (Feb 5, 2026)
*   **The Problem**: ML Services (KYC, Analytics) using PyTorch/PaddleOCR crash with "Killed" or Exit 137 immediately upon loading models if memory limit is too low (e.g., 512MB).
*   **The Fix**: Increase memory limits for ML containers.
    ```yaml
    resources:
      limits:
        memory: 2G  # Increased from 512M
      reservations:
        memory: 1G
    ```


## 🛠️ Build & Dependency Management

### 1. Multi-Module Project Dependencies

*   **GroupId Consistency**: In a multi-module Maven project where submodules are grouped (e.g., `backend/shared/`), ensure dependency references use the correct `groupId`.
    *   **Example**: `id.payu:api-commons` vs `id.payu.shared:api-commons`. An incorrect GroupId leads to build failures finding the artifact, even if the ArtifactId is correct.

### 2. Monorepo Scripting

*   **Context Path Traps**: When writing support scripts (Python/Bash) for a monorepo, do not rely solely on the `build context` path from `compose.yml` to check for file existence (like `pom.xml`).
    *   **Better Approach**: Resolve paths based on the `Dockerfile` location or explicitly handle the subdirectory structure.

### 3. Pact CLI Installation
*   **Correct Package Name**: Use `@pact-foundation/pact-cli` instead of the legacy `@subosito/pact-js-cli` to avoid "Package not found" errors during setup.

### 4. GPG Keyring Practices (Ubuntu 24.04+)
*   **Avoid `apt-key`**: The `apt-key` command is deprecated. Use `/etc/apt/keyrings` and `gpg --dearmor` for better security and compatibility.
*   **Example (Trivy/k6)**:
    ```bash
    wget -qO - https://.../public.key | sudo gpg --dearmor -o /etc/apt/keyrings/tool.gpg
    echo "deb [signed-by=/etc/apt/keyrings/tool.gpg] https://..." | sudo tee /etc/apt/sources.list.d/tool.list
    ```

## ☕ Java & Spring Boot

### 1. Naming Consistency (Entity vs Repo vs Test)

*   **The Issue**: Discrepancies between `userId` and `customerId` often lead to `cannot find symbol` or `BeanCreationException` during Flyway/JPA initialization.
*   **Lesson**: Standardize on `customerId` for all external-facing IDs across the platform.

### 2. Custom Annotations & Enums

*   **Inner Class Resolution**: When using custom annotations with inner enums (like `@Audited(level = AuditLevel.INFO)`), Java may fail to resolve the enum if not fully qualified or correctly imported.
*   **Correction**: Use `Audited.AuditLevel.INFO` to guarantee resolution.

### 4. Ambiguous Enum References (Swagger vs Security Starter)

*   **The Problem**: Importing `id.payu.security.annotation.Audited.Operation` can conflict with `io.swagger.v3.oas.annotations.Operation`, leading to `reference to Operation is ambiguous` compilation errors.
*   **The Fix**: Use semi-qualified names in annotations: `@Audited(operation = Audited.Operation.CREATE, ...)` instead of importing the inner enum directly.

### 5. Abstract Exception Instantiation in Tests

*   **The Problem**: Making a base domain exception `abstract` prevents direct instantiation in unit tests, leading to compilation errors.
*   **The Fix**: Either make the base exception concrete with a generic error code (e.g., `COMPLIANCE_GENERIC_ERROR`) or ensure tests always use a concrete subclass.

### 3. JPA Entity Architecture (Pragmatic Hexagonal)

*   **The Problem**: In a Hexagonal Architecture, repositories were extending `JpaRepository` using standard Domain Models (`ScheduledTransfer`, `Transaction`) that lacked `@Entity` annotations.
*   **The Symptom**: `UnsatisfiedDependencyException`: Not a managed type.
*   **The Fix**: Annotate the Domain Model class with `@Entity`, `@Table`, and `@Id`.
*   **Best Practice**: Ensure ALL classes used in `JpaRepository<T, ID>` are properly annotated entities.

### 4. Value Object Mapping

*   **The Problem**: `Money` Value Object (containing `amount` and `currency`) cannot be persisted directly without `@Embedded` or `AttributeConverter`.
*   **The Legacy Fix**: Using deprecated `amountValue` and `currencyCode` fields mapped with `@Column`, while marking the main `Money` object as `@Transient`.

### 5. JPA Boolean Naming

*   **The Issue**: Derived Query Methods (like `findByActiveTrue`) expect a field named `active`. If the field is `isActive`, the method must be `findByIsActiveTrue`.

## 🔄 CQRS & Architectural Refactoring (Feb 2026)

### 1. Mockito Mutation Trap (Capture-by-Reference)

*   **The Problem**: When testing services that mutate the same object across multiple repository `save()` calls (common in Saga flows), `verify(...).save(argThat(...))` or `ArgumentCaptor` will only show the **final state** of the object for all invocations.
*   **The Symptom**: A test checking if a transaction was saved as `PENDING` then `VALIDATING` fails because Mockito reports it was `VALIDATING` both times.
*   **The Fix**: Use a custom `Answer` to collect the object's state at the exact moment of invocation.

    ```java
    List<Status> capturedStatuses = new ArrayList<>();
    when(repository.save(any())).thenAnswer(inv -> {
        Transaction t = inv.getArgument(0);
        capturedStatuses.add(t.getStatus()); // Hand-copy state here
        return t;
    });
    // ... execution ...
    assertThat(capturedStatuses).containsExactly(Status.PENDING, Status.VALIDATING);
    ```

### 2. Controller Slice Test Isolation (JPA Interference)

*   **The Problem**: `@WebMvcTest` (slice test) attempts to load the full `@SpringBootApplication` context. If JPA annotations (`@EnableJpaRepositories`, `@EntityScan`) are on the main application class, the slice test will fail because it lacks `DataSource` and `EntityManager` beans.
*   **The Fix**: Move JPA-related annotations to a separate `@Configuration` class (e.g., `JpaConfig.java`). This allows `@WebMvcTest` to ignore JPA infra while still scanning your controller.
*   **Alternative**: Use `excludeAutoConfiguration` in the test annotation, but separating config is cleaner for monorepos.

### 3. Financial Precision in Assertions

*   **The Issue**: `BigDecimal` assertions with `isEqualTo()` fail if the scale is different (e.g., `100.0` vs `100.00`), even if the value is numerically identical.
*   **The Fix**: Always use `isEqualByComparingTo()` for `BigDecimal` comparisons in tests, especially when testing `Money` value objects.

### 4. Validation Regex & Special Characters

*   **The Issue**: Strict regex patterns for transaction descriptions (e.g., `^[a-zA-Z0-9 ]*$`) often block valid banking use cases like reference numbers containing `#` or `()`.
*   **Correction**: Update DTO validation patterns to include common symbols: `^[a-zA-Z0-9 #().]*$`.

## 🗄️ Database Management

### 1. Initialization Order

*   **Postgres Healthchecks**: A healthy Postgres container doesn't mean the databases in `init-db.sql` are ready.
*   **The Fix**: Update healthchecks to check a specific database: `pg_isready -U payu -d payu_account`.

### 2. Partitioning Limitations

*   **Hash Partitioning Defaults**: PostgreSQL (as of v16) does **not** support a `DEFAULT` partition for `HASH` partitioning strategies. Attempting to create one causes a migration failure.
    *   **The Fix**: Do not create a default partition for HASH strategies. Ensure the modulus/remainder coverage is complete (which it naturally is).
*   **Unique Constraints**: A unique constraint on a partitioned table **must include** all partitioning columns. Attempting to create a unique index on just the ID when partitioned by `account_id` will fail.
    *   **The Fix**: Add the partition key to the unique index definition: `CREATE UNIQUE INDEX ... ON table (id, partition_key)`.

### 3. Index Predicates & Immutability

*   **Mutable Functions in Indexes**: You cannot use `CURRENT_DATE`, `NOW()`, or `CURRENT_TIMESTAMP` in a `WHERE` clause of an index (partial index) because these functions are not IMMUTABLE.
    *   **The Fix**: Remove time-based filtering from the index definition or use a mechanism that doesn't rely on dynamic dates.

### 4. Podman Build Caching

*   **Stale Maven Layers**: Podman's layer caching is aggressive. If you update source code but the `mvn package` step is cached, old logic persists.
    *   **The Fix**: Use `podman build --no-cache` when debugging cryptic logic errors.
*   **Context Contamination**: Without a `.dockerignore` file, `COPY . .` copies `target/` directories from the host. If the host has stale compiled classes, they can contaminate the build.
    *   **The Fix**: Create `.dockerignore` excluding `**/target`. Clean host target (`rm -rf backend/*/target`) before critical builds.
*   **Compose Service Naming**: `podman-compose` can sometimes fail to map service names correctly or reuse existing containers.
    *   **Fallback**: Use `podman run` with explicit environment variables (`-e`) for reliable debugging.

### 5. Flyway Development

*   **Checksum Mismatches**: Changing a migration script after it has run locally causes checksum errors.
*   **The Strategy**: In dev/local environment, it is often faster to `DROP DATABASE` and let Flyway recreate it from scratch than to manually patch the `flyway_schema_history` table.

## 🛡️ Security & Configuration

### 1. Spring Bean Instantiation

*   **No-Args Constructor**: Beans instantiated by Spring (especially Filters or Interceptors that might be proxied) **must** have a no-args constructor available, even if they have final fields.
    *   **The Fix**: Remove `final` from fields and provide a protected/public no-args constructor to avoid `BeanInstantiationException`.

### 2. OAuth2 Configuration

*   **Silent Failures**: Missing `JwtDecoder` beans often manifest as `UnsatisfiedDependencyException` deep in the security chain.
    *   **The Fix**: Ensure `issuer-uri` or `jwk-set-uri` is explicitly defined in `application.yml` or a `JwtDecoder` bean is manually supplied.

### 3. Quarkus Startup Validation

*   **Mandatory Properties**: Quarkus performs strict validation on `@ConfigProperty`. If a property is defined but resolved to an empty string (e.g., via `${ENV:}`), it may fail with `NoSuchElementException`.
    *   **The Fix**: Always provide a non-empty fallback in `podman-compose.yml` for mandatory secrets or config keys:

        ```yaml
        WEBHOOK_PARTNER_1_SECRET: ${WEBHOOK_PARTNER_1_SECRET:-dummy_secret}
        ```

## 🏗️ Monorepo Infrastructure

### 1. Shared Library Env Var Mapping

*   **Custom Starters**: When using custom Spring Boot starters (like `cache-starter`), they often use specific property prefixes (e.g., `payu.cache.*`). Standard environment variables like `REDIS_HOST` might not be enough if the starter doesn't map them explicitly.
    *   **The Fix**: Double-check the `@ConfigurationProperties` prefix in the starter and provide matching env vars in `podman-compose.yml`:

        ```yaml
        PAYU_CACHE_REDIS_HOST: redis
        ```

### 2. Selective Maven Builds (Resource Optimization)

*   **The Problem**: Attempting to build the entire monorepo root in every service Dockerfile leads to "Too many open files" and extreme memory usage.
    *   **The Fix**: Use selective builds and project selection:

        ```dockerfile
        RUN mvn package -DskipTests -pl :service-name -am
        ```

### 4. Healthcheck Authentication (401 Unauthorized)

*   **The Problem**: Health endpoints (`/q/health` for Quarkus, `/actuator/health` for Spring Boot) may return `401 Unauthorized` if global security filters are too aggressive.
*   **The Fix (Quarkus)**: Ensure `quarkus.health.security.enabled=false` or explicitly permit the health path in your security configuration/filter.
*   **The Fix (Spring Boot)**: Ensure `management.endpoints.web.exposure.include=health` and that the security filter chain permits `/actuator/**`.
*   **Liveness Probes & Context Paths**:
    *   **Probes missing**: By default, Spring Boot does not expose `/actuator/health/liveness` unless `management.endpoint.health.probes.enabled=true`.
    *   **Context Path**: If `server.servlet.context-path` is set (e.g., `/compliance-service`), the healthcheck URL in `podman-compose.yml` MUST include it: `http://localhost:8087/compliance-service/actuator/health/liveness`.
    *   **401 in Spring Boot**: If `/actuator/health/liveness` returns 401 even if `/actuator/health` is permitted, ensure the `requestMatchers` use wildcards (`/actuator/**`) to cover sub-paths.

### 5. Misconfigured Service Labels (Spring Boot vs Quarkus)

*   **The Problem**: A service built with Spring Boot but configured in `docker-compose.yml` using Quarkus environment variables (e.g., `QUARKUS_DATASOURCE_JDBC_URL`) and healthchecks (`/q/health`) will fail to start or report as unhealthy.
*   **The Fix**: Ensure the configuration matches the framework:
    *   **Spring**: `SPRING_DATASOURCE_URL`, `actuator/health/liveness`.
    *   **Quarkus**: `QUARKUS_DATASOURCE_JDBC_URL`, `q/health`.

### 6. Vault Dev Mode Healthcheck

*   **The Problem**: `vault status` inside a container defaults to HTTPS, causing 401/error when Vault is running in `-dev` mode (HTTP).
*   **The Fix**: Explicitly set `VAULT_ADDR` in the healthcheck command:

    ```yaml
    healthcheck:
      test: ["CMD-SHELL", "VAULT_ADDR=http://127.0.0.1:8200 vault status || exit 1"]
    ```

### 7. Quarkus Uber-JAR Augmentation
*   **The Problem**: Duplicate files in dependencies (e.g., `META-INF/beans.xml` or custom resource files) can cause Quarkus build failures during the `buildUberJar` step.
*   **The Fix**: Exclude problematic duplicates or check for dependency conflicts. In most cases, ensuring the project structure follows standard Maven naming prevents resource collisions.

### 8. ArchUnit DSL Modernization
*   **The Problem**: Older ArchUnit syntax like `.or()` or `.and()` in `ClassesShould` chains may result in `cannot find symbol` errors in newer versions.
*   **The Fix**: Use the more explicit `.orShould()` and `.andShould()` methods to properly continue the rule chain. Use `shouldNot().dependOnClassesThat()` instead of `should().notDependOnClassesThat()`.

### 9. Financial Integrity & Optimistic Locking
*   **The Problem**: Concurrent financial operations (credits/debits) can lead to race conditions without proper locking.
*   **The Fix**: Add a `version` field to core domain entities (like `Account`) and use `@Version` (JPA) or manual checks in domain logic to enforce optimistic locking, as verified by P0 integrity tests.

## 🧪 Systematic Debugging

### 6. Spring Boot 3.4 Security & Public Endpoints (Feb 2026)

*   **The Problem**: Spring Security OAuth2 resource server configuration can intercept requests before permitAll() rules are evaluated, causing 401 errors even on public endpoints like `/actuator/health` and `/api/v1/accounts/register`.
*   **Root Cause**: When using `oauth2ResourceServer().jwt()`, Spring creates a filter chain that validates JWT tokens BEFORE the authorization rules (`permitAll()`) are checked.
*   **The Fix**: Use `WebSecurityCustomizer` bean to completely bypass Spring Security for specific paths:

    ```java
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/actuator/**")
                .requestMatchers("/api/v1/accounts/register")
                .requestMatchers("/api/v1/auth/login");
    }
    ```

*   **Note**: Spring will warn "This is not recommended" but this is necessary when OAuth2 resource server is enabled globally.
*   **Alternative**: Disable OAuth2 for specific paths using `securityMatcher()`.

### 7. Gateway Service URL Configuration (Feb 2026)

*   **The Problem**: Gateway proxying fails with "Connection refused: localhost/127.0.0.1:8081" even though service is running.
*   **Root Cause**: Default service URLs in `application.yaml` use `localhost:PORT` which doesn't resolve in container networks.
*   **The Fix**: Update default URLs to use service names from container network:

    ```yaml
    services:
      account-service:
        url: ${ACCOUNT_SERVICE_URL:http://account-service:8001}  # NOT localhost:8081
    ```

*   **Environment Variable Mismatch**: podman-compose.yml may set different variable names (e.g., `ROUTES_ACCOUNT_URL` vs `ACCOUNT_SERVICE_URL`). Ensure ENV variable names match config property names.

### 8. API Key vs JWT Authorization Layering (Feb 2026)

*   **The Problem**: Requests return "MISSING_API_KEY" even after JWT token is provided.
*   **Root Cause**: Multiple security filters are chained (ApiKeyValidationFilter → AuthorizationFilter). If API key validation is enabled, it blocks requests before JWT validation.
*   **The Fix**: Either:
    1.  Disable API key validation for dev: `gateway.api-keys.enabled=false`
    2.  Add public endpoints to API key bypass paths: `gateway.api-keys.bypass-paths=/api/v1/accounts/register`

## 🎨 Frontend & Design System

### 1. Cultural vs. Professional Aesthetics

*   **Observation**: Attempting to force cultural themes (e.g., "Wayang", "Javanese Philosophy") into a Fintech UI can clash with user expectations for "Premium" and "Trust".
*   **Lesson**: Users prefer standard international banking aesthetics (Clean, White, Sans-serif, Glassmorphism) for financial products. Use cultural elements very subtly or not at all if the goal is "Premium Global Standard".

### 2. Responsive Card Design (The "Golden Ratio" Fix)

*   **The Problem**: Fixed pixel widths (e.g., `w-[350px]`) for Credit Card components break on small mobile screens (iPhone SE) or look tiny on large desktops.
*   **The Fix**: Use `vw` (viewport width) units combined with `aspect-ratio` to maintain the ISO/IEC 7810 ID-1 standard.
    *   **Snippet**: `w-[85vw] max-w-[340px] aspect-[1.586]` ensures the card scales perfectly while maintaining the correct physical ratio. Update text sizes to be relative (`text-[3vw]`) to scale with the card.

### 3. Mobile Layout Stacking

*   **The Problem**: "Zig-zag" or staggered grid layouts that look dynamic on Desktop often break flow on Mobile, leading to overlapping or confusing content.
*   **The Fix**: Switch to `flex-col` to stack elements vertically on mobile. Crucially, add significant vertical padding (`py-16` or `py-20`) to containers to prevent content from being occluded by fixed headers or bottom navigation bars.

## 🐳 Containerization & Environment Setup (Feb 2026 Updates)

### 4. Podman Registry Configuration (Feb 4, 2026)

*   **The Problem**: Podman cannot pull images from Docker Hub, showing errors like "short-name 'postgres:16-alpine' did not resolve to an alias and no unqualified-search registries are defined".
*   **Root Cause**: `/etc/containers/registries.conf` has all registry configurations commented out by default for security reasons.
*   **The Fix**: Add Docker Hub to unqualified search registries:

    ```bash
    sudo bash -c 'echo "unqualified-search-registries = [\"docker.io\"]" >> /etc/containers/registries.conf.d/short-name.conf'
    ```

*   **Validation**: Run `podman pull postgres:16-alpine` to confirm images can now be pulled.

### 5. Maven JAR Build Before Container Image (Feb 4, 2026)


  # Step 2: Build container image
  podman build -f backend/account-service/Dockerfile -t payu_account-service backend/account-service
  ```

* **Note**: This two-step approach is more reliable than trying to run Maven inside the container build, especially for monorepo setups with shared dependencies.

### 6. Local Image Tagging for Podman Compose (Feb 4, 2026)

* **The Problem**: `podman-compose up` fails with "no such file or directory" even though images exist locally.
* **Root Cause**: Images are built as `localhost/payu_service:latest` but `podman-compose` references them as `payu_service:latest` (without the `localhost/` prefix).
* **The Fix**: Tag the local image to match the compose reference:

  ```bash
  podman tag localhost/payu_account-service:latest payu_account-service:latest
  podman compose up -d account-service
  ```

* **Best Practice**: When using `build:` directive in docker-compose.yml, always specify an explicit `image:` tag to avoid naming mismatches between `localhost/` prefixed images and compose references.

## ☕ Java & Spring Boot (Feb 2026 Updates)

### 7. Spring Security Wildcard Matchers for Public Endpoints (Feb 4, 2026)

* **The Problem**: Spring Security OAuth2 resource server configuration returns 401 for public endpoints even with `permitAll()` configuration and `WebSecurityCustomizer`.
* **Root Cause**: Exact path matchers in `securityMatcher()` may not match due to path normalization issues. Using `/api/v1/accounts/register` might not match while `/api/v1/accounts/**` will.
* **The Fix**: Use wildcard matchers for public filter chains with `@Order(1)`:

  ```java
  @Bean
  @Order(1)
  public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
      http
          .securityMatcher("/api/v1/accounts/**", "/api/v1/auth/**")  // Use wildcards!
          .csrf(csrf -> csrf.disable())
          .cors(cors -> cors.configurationSource(corsConfigurationSource()))
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .oauth2ResourceServer(oauth2 -> oauth2.disable());  // Explicitly disable for public endpoints
      return http.build();
  }
  ```

* **Why This Works**: Wildcard matchers ensure all subpaths are covered, and explicitly disabling OAuth2 resource server prevents JWT validation on public endpoints.
* **Note**: The JWT filter chain should have `@Order(2)` and should only match paths that require authentication.

### 8. Quarkus Redis Connection Format (Feb 4, 2026)

* **The Problem**: Quarkus gateway service fails to start with `NullPointerException: Cannot invoke "String.length()" because "ip" is null` when connecting to Redis.
* **Root Cause**: Vert.x Redis client (used by Quarkus) expects a specific URI format. Using `redis:6379` or `redis://redis:6379` can cause parsing issues.
* **The Fix**: Use the `redis://host:port` format in environment variables:

  ```yaml
  # docker-compose.yml
  environment:
    QUARKUS_REDIS_HOSTS: redis://redis:6379  # Include redis:// prefix
  ```

* **Why This Works**: The Vert.x Redis client URI parser expects the `redis://` scheme to properly parse the connection string. Without it, the client attempts to parse the string incorrectly and fails with NPE.

### 9. Gateway Authorization Configuration Mapping (Feb 4, 2026)

* **The Problem**: Quarkus configuration validation fails with "does not map to any root" error for `gateway.authorization.jwt-secret` even though the property is defined in `application.yaml`.
* **Root Cause**: SmallRye Config (used by Quarkus) requires all configuration properties to be mapped to a root interface in `@ConfigMapping` classes. The `AuthorizationFilter` was using `@ConfigProperty` directly, but the config mapping was rejecting unmapped properties.
* **The Fix**: Add the `AuthorizationConfig` interface to `GatewayConfig.java`:

  ```java
  @ConfigMapping(prefix = "gateway")
  public interface GatewayConfig {
      // ... other configs

      @WithName("authorization")
      AuthorizationConfig authorization();

      interface AuthorizationConfig {
          @WithDefault("true")
          boolean enabled();

          @WithName("jwt-secret")
          @WithDefault("dGVzdC1qd3Qtc2VjcmV0...")
          String jwtSecret();
      }
  }
  ```

* **Why This Works**: Adding the interface to the config mapping tells SmallRye Config that these properties are valid and expected, preventing validation failures.

## 🧪 E2E Testing (Feb 2026 Updates)

### 10. Playwright Installation for E2E Tests (Feb 4, 2026)

* **The Problem**: Running `npx playwright test` fails with "Cannot find module '@playwright/test'" even though Playwright is listed in `package.json`.
* **Root Cause**: The `@playwright/test` package needs to be installed locally in the project, and browsers need to be downloaded separately.
* **The Fix**: Install dependencies and browsers before running tests:

  ```bash
  # Install all npm dependencies including @playwright/test
  npm ci

  # Install Playwright browsers with system dependencies
  npx playwright install --with-deps
  ```

* **Note**: The `--with-deps` flag installs system-level dependencies (like libraries for Chromium, Firefox, WebKit) which are required for headless browser operation in Linux environments.

### 11. Standalone Quarkus Service Dockerfile Pattern (Feb 4, 2026)

* **The Problem**: Quarkus services built with `-pl :service-name -am` flag fail with "no such file or directory" when the service is a standalone module (not part of a multi-module parent POM structure).
* **Root Cause**: The `-pl` flag is designed for multi-module Maven projects where you need to specify which module to build. Standalone services should build the current directory without the `-pl` flag.
* **The Fix**: Remove the `-pl :service-name -am` flags and fix COPY paths:

  ```dockerfile
  # WRONG (for standalone services):
  RUN mvn package -DskipTests -Dquarkus.package.jar.type=fast-jar -pl :api-portal-service -am
  COPY --from=build --chown=185 /build/api-portal-service/target/quarkus-app/lib/ /deployments/lib/
  
  # CORRECT:
  RUN mvn package -DskipTests -Dquarkus.package.jar.type=fast-jar
  COPY --from=build --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
  ```

* **Services Affected**: api-portal-service, gateway-service

### 12. Spring Boot Service with Pre-built JAR Pattern (Feb 4, 2026)

* **The Problem**: Multi-module Maven build inside Docker fails when the parent POM is not accessible from the build context (subdirectory build).
* **Root Cause**: Dockerfile with `context: ./backend/service-name` cannot access `../pom.xml` for multi-module builds.
* **The Fix**: Build the JAR locally first, then use a simplified Dockerfile:

  ```bash
  # Step 1: Build JAR locally
  mvn -f backend/service-name/pom.xml clean package -DskipTests
  cp target/service-name-*.jar target/app.jar
  
  # Step 2: Use simplified Dockerfile
  FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2
  COPY target/app.jar /deployments/app.jar
  ```

* **Services Affected**: lending-service (and any service with complex multi-module dependencies)

### 13. PostgreSQL Password in Container Environment (Feb 4, 2026)

* **The Problem**: Spring Boot services fail with "FATAL: password authentication failed for user 'payu'" when connecting to PostgreSQL, even though the correct password is configured.
* **Root Cause**: The running PostgreSQL container was created with a different password than what's configured in `docker-compose.yml`. The environment variable `POSTGRES_PASSWORD` was set when the container was first created, and changing it in `docker-compose.yml` doesn't affect running containers.
* **The Fix**: Either:
  1. Recreate the PostgreSQL container with the correct password, OR
  2. Use the actual password from the running container when connecting services
  3. Check the actual password: `podman inspect payu-postgres | grep POSTGRES_PASSWORD`

* **Lesson**: PostgreSQL password is set at container creation time. Changing `docker-compose.yml` doesn't update running containers.

### 14. Context Path in Healthcheck URLs (Feb 4, 2026)

* **The Problem**: Healthcheck fails with 404 even though the service is running correctly.
* **Root Cause**: Services with `server.servlet.context-path` (like `/compliance-service`) require the context path in healthcheck URLs.
* **The Fix**: Include the context path in healthcheck configuration:

  ```yaml
  # compliance-service has context-path: /compliance-service
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/compliance-service/actuator/health/liveness"]
  
  # partner-service has no context-path (uses root)
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  ```

* **Services Affected**: compliance-service, any service with custom context-path


## 🐳 Container Orchestration & Environment Setup (Feb 2026 - Final)

### 15. Port 8080 Standardization Implementation (Feb 4, 2026)

* **Observation**: Different services were mapped to different host ports (8001, 8002, 8003, etc.) while all services internally listen on port 8080.
* **Implementation**: 
  - All services configured with `EXPOSE 8080` internally
  - Gateway service exposed on host port 8080 (standard API gateway port)
  - Other services mapped to unique host ports for development (8001-8014)
  - In production OpenShift, services use ClusterIP/Route - no port mapping needed
* **Benefit**: Standard internal port simplifies service discovery and configuration
* **Note**: Host port variation is development-only for local testing

### 16. E2E Test Execution in Container Environment (Feb 4, 2026)

* **Challenge**: Running full Playwright E2E suite takes 45-50 minutes in containerized environment
* **Root Cause**: Browser automation, container resource constraints, and parallel test execution
* **Optimization Strategies**:
  1. Use `--workers` flag to control parallel execution
  2. Run specific test suites instead of full suite during development
  3. Use `--project` flag to target specific browsers
  4. Consider using headless mode for faster execution
* **Test Result**: 238 test folders created before termination
* **Recommendation**: For CI/CD, use smoke tests for quick validation and full suite overnight

### 17. Image Tagging for Podman Compose (Feb 4, 2026)

* **The Problem**: `podman-compose` cannot find local images that were built with `localhost/` prefix
* **Root Cause**: Images are built as `localhost/payu_service:latest` but compose references `payu_service:latest`
* **The Fix**: Always tag local images to match compose reference:

  ```bash
  # Build creates localhost/payu_service:latest
  podman build -f service/Dockerfile -t payu_service service
  
  # Tag to match compose reference
  podman tag localhost/payu_service:latest payu_service:latest
  
  # Now compose can find it
  podman compose up -d service
  ```

* **Alternative**: Use explicit `image:` tag in docker-compose.yml to avoid naming conflicts

### 11. Port Collision Management (Feb 5, 2026)
* **The Problem**: Multiple services (Lending, Partner, KYC) were competing for host port 8010, causing container creation to fail silently or with "port already in use" errors during `podman compose up`.
* **The Fix**: Audited all services and aligned them strictly with the `.env` configuration template. Standardized host port mapping to avoid any overlap.
* **The Lesson**: In complex microservice environments, rely on central `.env` templates rather than hardcoded ports in `docker-compose.yml`.

### 12. Quarkus Fast-JAR Dockerfile Pattern (Feb 5, 2026)
* **The Problem**: Simulators and Notification services were failing with "no main manifest attribute" or failing to find `app.jar` because the Dockerfile was trying to run a standard JAR instead of the Quarkus specialized `quarkus-run.jar`.
* **The Fix**: Updated Dockerfiles to use multi-stage builds, copying the entire `target/quarkus-app/` directory and setting the entry point to `-jar /deployments/quarkus-run.jar`.
* **The Lesson**: Quarkus `fast-jar` (default) requires copying the entire `quarkus-app` structure, not just a single JAR.

### 13. Spring Boot OIDC Configuration (Feb 5, 2026)
* **The Problem**: Services like `support-service` and `backoffice-service` failed to start with `JwtDecoder` bean errors (`BeanCreationException`).
* **The Fix**: Explicitly added Keycloak OIDC issuer URLs to the `environment` section in `docker-compose.yml` to resolve JWT validation beans at startup.

### 14. Flyway Migration Synchronization (Feb 5, 2026)
* **The Problem**: `promotion-service` crashed because the `customer_segments` table was missing, even though the entity existed in the code.
* **The Fix**: Created the missing `V3__add_customer_segments.sql` migration script to reconcile the database schema with the JPA domain model.

### 15. Gateway Service Resource Limits (Feb 5, 2026)
* **The Problem**: `gateway-service` (Quarkus) experienced OOM (Exit 137) during high load or complex routing initialization with default 256MB limit.
* **The Fix**: Increased memory limits to 768MB (and 256MB reservation) to provide enough headroom for the Vert.x reactive stack.

### 16. Redis Configuration for Spring Services (Feb 5, 2026)
* **The Problem**: Spring Boot services using `cache-starter` fail to connect to Redis in container environments, showing "Connection refused: localhost:6379" errors even though Redis is running.
* **Root Cause**: Services using `payu.cache.redis.host` property don't automatically map standard `REDIS_HOST` environment variable. The custom cache-starter uses `PAYU_CACHE_REDIS_HOST` prefix.
* **The Fix**: Add `PAYU_CACHE_REDIS_HOST: redis` (or service DNS name) to docker-compose.yml for services using cache-starter:
  ```yaml
  lending-service:
    environment:
      PAYU_CACHE_REDIS_HOST: redis  # Maps to payu.cache.redis.host
  ```
* **Note**: Some services also need `spring.data.redis.host` or `REDIS_HOST` depending on configuration pattern.

### 17. Port Standardization Enforcement (Feb 5, 2026)
* **The Problem**: Services hardcoded to non-standard ports (e.g., `server.port=${PORT:8089}`) break standardization and cause healthcheck failures.
* **Root Cause**: Historical port assignments weren't cleaned up when standardizing to port 8080 across all services.
* **The Fix**: Audit `application.yml` for all services and ensure:
  ```yaml
  server:
    port: ${PORT:8080}  # ALL services must default to 8080
  ```
* **Impact**: Non-standard ports cause gateway routing failures and healthcheck mismatches.

### 18. Healthcheck Path Alignment (Feb 5, 2026)
* **The Problem**: Healthchecks in docker-compose.yml pointing to wrong ports (8089, 8090) fail even when services are healthy.
* **Root Cause**: Healthcheck URLs weren't updated when port standardization changed service ports.
* **The Fix**: After fixing source port in `application.yml`, update all healthcheck URLs to match:
  ```yaml
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health/liveness"]
  ```
* **Best Practice**: Healthcheck should always check `localhost:8080` for internal container port, regardless of external host port mapping.

### 19. Quarkus Parent POM Build Context (Feb 5, 2026)
* **The Problem**: Quarkus simulators in monorepo fail with "Parent POM not found" when building from service subdirectory context.
* **Root Cause**: Docker build context is service directory, but parent POM is at backend root.
* **The Fix**: Use backend root as build context and update COPY paths:
  ```dockerfile
  # Build from backend root to access parent POM
  COPY backend/pom.xml .
  COPY backend/simulators/qris-simulator/pom.xml simulators/qris-simulator/
  RUN mvn package -f simulators/qris-simulator/pom.xml -DskipTests
  ```
* **Alternative**: Pre-build JAR locally and use simplified Dockerfile (see Lesson 9).

## 🎨 Product Design Protocol (Feb 2026)

### 20. Jackson Deserialization with Keycloak Responses (Feb 5, 2026)
* **The Problem**: Auth-service login fails with `IllegalArgumentException` when deserializing Keycloak token response.
* **Root Cause**: Keycloak returns extra fields (`not-before-policy`, `refresh_expires_in`, `session_state`, `scope`) that aren't mapped in the `LoginResponse` record. Jackson fails on unknown properties in records by default.
* **The Fix**: Add `@JsonIgnoreProperties(ignoreUnknown = true)` to DTOs that deserialize external OAuth2/OIDC responses:
  ```java
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record LoginResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("refresh_token") String refreshToken,
      @JsonProperty("expires_in") long expiresIn,
      @JsonProperty("token_type") String tokenType
  ) {}
  ```
* **Note**: This is especially important for DTOs that map to third-party API responses (Keycloak, OAuth2 providers) where you don't control the response schema.

### 21. Environment Variable Naming Consistency (Feb 5, 2026)
* **The Problem**: Auth-service login fails with `IllegalArgumentException: Not enough variable values available to expand 'KEYCLOAK_URL'`.
* **Root Cause**: `application.yaml` referenced `${KEYCLOAK_URL}` but `docker-compose.yml` set `KEYCLOAK_SERVER_URL`. The WebClient received the literal string `${KEYCLOAK_URL}` and tried to expand it as a URI template.
* **The Fix**: Align environment variable names between `application.yaml` and `docker-compose.yml`:
  ```yaml
  # application.yaml
  keycloak:
    server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
  ```
* **Best Practice**: Use consistent variable naming across all configuration files. Prefer specific names like `KEYCLOAK_SERVER_URL` over generic `KEYCLOAK_URL`.

### 22. The "Startup Protocol" for Design (Feb 2026)
* **The Shift**: Moving from ad-hoc design improvements to a strict "Steve Jobs" persona protocol.
* **The Rule**: No design opinions are valid without first auditing: 1) Existing Design System, 2) PRD, and 3) Live App Responsiveness.
* **The Impact**: Prevents "design drift" where new features don't match the established "Premium Emerald" aesthetic.
* **Key Check**: "If an element can be removed without losing meaning, it must be removed."

### 23. Keycloak Admin Password Persistence (Feb 6, 2026)
* **The Problem**: Changing `KEYCLOAK_ADMIN_PASSWORD` in `docker-compose.yml` doesn't update existing Keycloak admin password.
* **Root Cause**: Keycloak stores admin credentials in PostgreSQL database. The `KEYCLOAK_ADMIN_PASSWORD` environment variable only sets the initial password on first startup. Once the database exists, changing the env var has no effect.
* **The Fix**: To reset admin password on existing installation:
  1. Access Keycloak Admin Console at http://localhost:8099
  2. Navigate to Users → admin → Credentials → Set password
  3. OR use kc.sh CLI: `podman exec payu-keycloak /opt/keycloak/bin/kc.sh import users` (requires restart)
* **Best Practice**: Document admin passwords securely and consider using external secret management (Vault, Sealed Secrets) for production.
* **Note**: For fresh installations, set `KEYCLOAK_ADMIN_PASSWORD` in docker-compose.yml before first startup.

### 24. Redis Environment Variables for Spring Boot Services (Feb 6, 2026)
* **The Problem**: Spring Boot services showing `DOWN` status in health checks despite all containers running healthy.
* **Symptoms**:
  - Health endpoint returns: `{"status":"DOWN","components":{"redis":{"status":"DOWN","details":{"error":"RedisConnectionFailureException"}}}}`
  - DeepHealthIndicator logs: "Redis health check failed: Unable to connect to Redis"
  - Redis container is healthy and responding to PING
* **Root Cause**: Services were missing `REDIS_HOST` and `PAYU_CACHE_REDIS_HOST` environment variables in `docker-compose.yml`. Without these, Spring Data Redis defaults to `localhost:6379` instead of the container network hostname `redis:6379`.
* **The Fix**: Add missing Redis environment variables to docker-compose.yml:
  ```yaml
  environment:
    REDIS_HOST: redis
    REDIS_PORT: 6379
    PAYU_CACHE_REDIS_HOST: redis
  ```
* **Verification**: After restarting services:
  ```bash
  curl -s http://localhost:8001/actuator/health/deepHealth | jq '.details.redis'
  # Returns: {"latency": "1ms", "response": "PONG"}
  ```
* **Best Practice**: Always explicitly define Redis connection parameters in container environments, even if application.yml has defaults. The `localhost` default only works for local development, not container networking.

### 25. Reset Keycloak Passwords Directly in Database (Feb 6, 2026)
* **The Problem**: Need to reset Keycloak admin or user passwords but don't have access to the Admin Console or current password is unknown.
* **Root Cause**: Keycloak stores passwords as PBKDF2-SHA256 hashes in the `credential` table. The `KEYCLOAK_ADMIN_PASSWORD` env var only works on first startup.
* **The Solution**: Generate PBKDF2-SHA256 hash and update database directly.
* **Python Script to Generate Hash**:
  ```python
  import hashlib
  import binascii
  import json

  password = "P@ssw0rd123"
  salt = "payusaltkey2024".encode('utf-8')
  iterations = 27500

  hashed = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
  hash_b64 = binascii.b2a_base64(hashed).decode('utf-8').strip()
  salt_b64 = binascii.b2a_base64(salt).decode('utf-8').strip()

  secret_data = json.dumps({"value": hash_b64, "salt": salt_b64, "additionalParameters": {}})
  credential_data = json.dumps({"hashIterations": iterations, "algorithm": "pbkdf2-sha256", "additionalParameters": {}})
  ```
* **SQL Update Command**:
  ```sql
  UPDATE credential
  SET SECRET_DATA = '<secret_data_json>',
      CREDENTIAL_DATA = '<credential_data_json>',
      TYPE = 'password'
  WHERE user_id = (SELECT id FROM user_entity WHERE username = 'admin');
  ```
* **Important**: After updating the database, restart Keycloak container to apply changes.
* **Best Practice**: Store the generated passwords securely and document the salt used for future reference.

### 26. OpenAPI Documentation Coverage Gap (Feb 6, 2026)
* **The Problem**: API documentation at `/api-docs` was incomplete, with only 15.6% of endpoints having `@Operation` annotations.
* **Root Cause**: Developers implemented REST endpoints without adding OpenAPI annotations, causing a gap between implemented and documented APIs.
* **Discovery Method**: Created `scripts/validate-openapi.py` to scan all controllers and compare `@RequestMapping` derivatives with `@Operation` annotations.
* **Findings**:
  - Total endpoints: 154 across 13 services
  - Documented: 24 (15.6%)
  - Undocumented: 130 (84.4%)
  - Services with 0% documentation: auth-service, fx-service, partner-service, account-service
  - Only billing-service had 100% coverage
* **The Fix**: Add `@Operation` annotations to all undocumented endpoints:
  ```java
  @Operation(
      summary = "Transfer funds between accounts",
      description = "Executes a transfer from source to destination account with idempotency support",
      tags = {"Transactions"},
      responses = {
          @ApiResponse(responseCode = "200", description = "Transfer successful"),
          @ApiResponse(responseCode = "400", description = "Invalid request"),
          @ApiResponse(responseCode = "409", description = "Insufficient funds")
      }
  )
  @PostMapping("/transfer")
  public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
      // ...
  }
  ```
* **Validation Script Usage**:
  ```bash
  # Run full validation
  ./scripts/validate-openapi.py

  # Validate single service
  ./scripts/validate-openapi.py --service transaction-service

  # Generate JSON report for CI/CD
  ./scripts/validate-openapi.py --json
  ```
* **CI/CD Integration**: Add validation to build pipeline to enforce documentation coverage threshold (e.g., minimum 80%).
* **Best Practice**: Require `@Operation` annotation in code review checklist for all new REST endpoints.
* **Achievement**: After fixing detection script and adding missing annotations, reached 100% coverage (154/154 endpoints).

### 27. Java Annotation Order Matters for OpenAPI Detection (Feb 6, 2026)
* **The Problem**: Validation script initially reported 15.6% coverage, but actual coverage was much higher.
* **Root Cause**: Java annotation order can vary between codebases. Two patterns exist:
  1. **Standard**: `@Operation` before `@GetMapping` (operation annotation first)
  2. **Reverse**: `@GetMapping` before `@Operation` (mapping annotation first)
* **The Fix**: Updated validation script to check both patterns - look back 15 lines AND look forward 20 lines for `@Operation` annotation.
* **Detection Logic**:
  ```python
  # Pattern 1: Look back (standard annotation order)
  for i in range(max(0, start_idx - 15), start_idx):
      if '@Operation' in lines[i]:
          return True, summary, tags

  # Pattern 2: Look forward (reverse annotation order)
  for i in range(start_idx, min(len(lines), start_idx + 20)):
      if '@Operation' in lines[i]:
          return True, summary, tags

  # Stop at method declaration
  if line.strip().startswith('public'):
      break
  ```
* **Result**: After fixing detection, coverage jumped from 15.6% to 96.1%. Only needed to add annotations to `ScheduledTransferController` (6 endpoints) to reach 100%.
* **Best Practice**: When writing validation scripts, account for different code style patterns within the same language. Always verify false positives by manual inspection.

### 28. Seed Data Alignment for Development (Feb 6, 2026)
* **The Problem**: Tests and documentation referenced test users (customer1, admin) but no automated seed data existed to create these users.
* **Root Cause**:
  - Keycloak realm was created manually, not via export/import
  - Database migrations only created schemas, not seed data
  - Tests used Faker to generate random users instead of deterministic fixtures
* **The Fix**: Created comprehensive seed data infrastructure:
  1. **Keycloak Realm Export** (`infrastructure/keycloak/payu-realm-export.json`):
     - Defines realm: `payu`
     - Roles: USER, ADMIN, BACKOFFICE, KYC_VERIFIED, PREMIUM
     - Users: customer1, customer2, admin, backoffice (all with password `P@ssw0rd123`)
     - Clients: payu-web-app, payu-backend, payu-mobile
  2. **Database Seed Migrations** (`V99__seed_test_data.sql`):
     - account-service: Users, profiles, accounts with initial balances
     - wallet-service: Wallets and ledger entries for test accounts
  3. **Seed Data Script** (`scripts/seed-data.sh`):
     - Initializes Keycloak realm via admin API
     - Runs database seed migrations
     - Verifies seed data was created
  4. **Idempotency Validation Test**:
     - Tests idempotency key reuse detection
     - Tests in-progress request detection
     - Tests fingerprint consistency
* **Test Credentials**:
  ```bash
  # Customer 1 (KYC verified, premium user)
  Username: customer1
  Password: P@ssw0rd123
  Email: customer1@payu.id
  NIK: 3201234567890001
  Initial Balance: Rp 10,000,000

  # Customer 2 (Basic user)
  Username: customer2
  Password: P@ssw0rd123

  # Admin
  Username: admin
  Password: P@ssw0rd123
  ```
* **Usage**:
  ```bash
  # Initialize all seed data
  ./scripts/seed-data.sh

  # Initialize only Keycloak
  ./scripts/seed-data.sh --keycloak

  # Initialize only database
  ./scripts/seed-data.sh --db

  # Verify seed data
  ./scripts/seed-data.sh --verify
  ```
* **Best Practice**: Store seed data in version control alongside migrations. Use V99 or similar high version number to ensure seed data runs after all schema migrations.
