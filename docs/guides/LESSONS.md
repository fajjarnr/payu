# PayU Platform - Lessons Learned & Troubleshooting Guide

## 🐳 Containerization & Podman Compose

### 1. Podman-Compose Compatibility
*   **Volume Syntax**: Current versions of `podman-compose` may fail with advanced Docker Compose volume types (like `type: persistent`). Use standard bind-mount or named volume syntax:
    ```yaml
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ```
*   **Short-name Resolution**: Podman requires fully qualified image names to avoid interactive prompts. Always prepend `docker.io/library/` or `docker.io/` for official/public images.
*   **Local Image Tagging**: Always provide an `image:` tag (e.g., `localhost/payu-service`) when using the `build` directive. This prevents Podman from using random hex IDs which makes referencing images easier.

### 2. Monorepo Build Contexts
*   **The Shared Library Trap**: In a monorepo (like `backend/`), setting the build `context` to the service subfolder prevents access to shared siblings (e.g., `backend/shared/`).
*   **The Fix**:
    1.  Set `context` to the parent directory (e.g., `../../backend`).
    2.  Set `dockerfile` to the relative path (e.g., `service-name/Dockerfile`).
    3.  Update the `Dockerfile` to `COPY . .` from the root context.
    4.  **Crucial**: Use Maven project selection flags `-pl :service-name -am` to build only the target service and its local dependencies.
    5.  Update `COPY --from=build` paths to point into the service-specific `target` folder: `COPY --from=build /build/service-name/target/app.jar ...`

### 3. Permissions and Package Installation
*   **UBI User Switching**: Red Hat UBI images often default to a non-root user (like `jboss` or `node`).
*   **The Fix**: Always switch back to `USER root` before running `microdnf` or `dnf` to install packages (like `curl`), then switch back to the application user (e.g., `USER 185` or `USER 1001`).
    ```dockerfile
    USER root
    RUN microdnf install -y curl && microdnf clean all
    USER 185
    ```

### 4. Environment Variable Precision
*   **Explicit over Implicit**: Even if `application.yml` has defaults, explicitly define `DB_URL`, `KAFKA_BROKERS`, and `REDIS_HOST` in `podman-compose.yml`.
*   **Profile Activation**: Always set `SPRING_PROFILES_ACTIVE: container` to ensure container-specific configurations are loaded.
*   **UBI9 Minimal & Curl**: The standard `curl` package conflicts with `curl-minimal` in UBI9 minimal images.
    *   **The Fix**: Use `microdnf install -y curl-minimal` instead. If you must use full curl, you might need `--allowerasing` (though `curl-minimal` is usually sufficient for healthchecks).

## 🛠️ Build & Dependency Management

### 1. Multi-Module Project Dependencies
*   **GroupId Consistency**: In a multi-module Maven project where submodules are grouped (e.g., `backend/shared/`), ensure dependency references use the correct `groupId`.
    *   **Example**: `id.payu:api-commons` vs `id.payu.shared:api-commons`. An incorrect GroupId leads to build failures finding the artifact, even if the ArtifactId is correct.

### 2. Monorepo Scripting
*   **Context Path Traps**: When writing support scripts (Python/Bash) for a monorepo, do not rely solely on the `build context` path from `compose.yml` to check for file existence (like `pom.xml`).
    *   **Better Approach**: Resolve paths based on the `Dockerfile` location or explicitly handle the subdirectory structure.

## ☕ Java & Spring Boot

### 1. Naming Consistency (Entity vs Repo vs Test)
*   **The Issue**: Discrepancies between `userId` and `customerId` often lead to `cannot find symbol` or `BeanCreationException` during Flyway/JPA initialization.
*   **Lesson**: Standardize on `customerId` for all external-facing IDs across the platform.

### 2. Custom Annotations & Enums
*   **Inner Class Resolution**: When using custom annotations with inner enums (like `@Audited(level = AuditLevel.INFO)`), Java may fail to resolve the enum if not fully qualified or correctly imported.
*   **Correction**: Use `Audited.AuditLevel.INFO` to guarantee resolution.

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

## 🧪 Systematic Debugging
*   **Root Cause First**: Never guess with `podman-compose restart`. Use `podman logs <container>` to find the specific Java exception (e.g., `FlywayException` usually means missing `DB_URL` or name mismatch).
*   **Vault Port Conflict**: In Podman dev environments, ensure port 8200 isn't bound by a host process or another container's conflicting configuration. If `vault-config.json` is used, ensure it doesn't try to bind to an address already handled by the container runtime.
