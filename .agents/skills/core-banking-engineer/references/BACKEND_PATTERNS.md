# Core Banking & Backend Engineering Patterns

## 🏗️ Hexagonal Architecture Implementation
Standard package structure for PayU services:
```
id.payu.{service}/
├── adapter/
│   ├── web/            # REST Controllers & DTOs
│   ├── persistence/    # JPA Repositories & Entities
│   └── messaging/      # Kafka Producers/Consumers
├── domain/
│   ├── model/          # Pure domain objects (no framework annotations)
│   └── port/           # Inbound (UseCases) and Outbound (Port) interfaces
├── application/
│   └── service/        # UseCase implementations (Domain logic)
└── infrastructure/
    └── config/         # Spring/Quarkus beans & security config
```
*   **ArchUnit Enforcement**: Use `archunit-starter` to ensure layering. Avoid older `.or()`/.and()` syntax; use `.orShould()`/.andShould()` for chain continuation.

## ☕ Spring Boot & JPA Best Practices
*   **Financial Precision**: Always use `isEqualByComparingTo()` for `BigDecimal` comparisons in tests. Standard `isEqualTo()` fails on scale differences (100.0 vs 100.00).
*   **Value Objects**: Use `@Embedded` for `Money` (amount + currency).
*   **Optimistic Locking**: Always add a `version` field with `@Version` (JPA) on core entities (Account, Wallet) to prevent race conditions during concurrent credits/debits.
*   **Bean Lifecycle**: Ensure proxied beans have a no-args constructor (remove `final` if necessary) to avoid `BeanInstantiationException`.
*   **Enum Safety**: Use fully qualified or semi-qualified names for inner enums in annotations (e.g., `Audited.AuditLevel.INFO`) to avoid ambiguity with Swagger or Security starters.
*   **Open-in-View**: Always set `spring.jpa.open-in-view: false` explicitly. The Spring Boot default (`true`) keeps DB sessions open during HTTP response rendering — causes lazy-loading surprises and connection pool exhaustion. (L-008)

## ⚠️ Code Health Anti-Patterns in Multi-Pod Microservices (L-008)

**1. In-Memory State (ConcurrentHashMap) in Stateless Services**
`WalletServiceAdapter` used a `ConcurrentHashMap<String, ReservationInfo>` for reservation data between `reserveBalance()` and `commitBalance()`. This fails in multi-pod deployments because the commit may hit a different pod. **Fix**: Pass `reservationId` through method signatures; the saga context (persisted in DB as JSONB) already carries this field.

**2. Spring Boot Config Namespace Gotcha**
`transaction-service/application.yml` had a top-level `kafka:` block. Spring Boot silently ignores this — the correct path is `spring.kafka.*`. No error, no warning, just silent misconfiguration. **Rule**: Always verify config properties are under the correct Spring namespace. Use `@ConfigurationProperties` binding validation.

**3. `.gitignore` Matching `port/out/` Directories (L-011)**
A root `.gitignore` entry `out/` matched any path containing `/out/`, including Hexagonal Architecture paths like `domain/port/out/AccountServicePort.java`. This silently excluded **26 port interface files** across 10 services. Build passes locally (files on disk) but fails on fresh clone.
```gitignore
out/
# Negation: preserve Hexagonal Architecture port directories
!**/port/out/
!**/port/output/
```
**Rule**: Use `/out/` (root-only) instead of `out/` (recursive) when targeting build output directories. After adding gitignore rules, verify with `git ls-files --others --ignored --exclude-standard | grep port`.

## 💳 Payment Gateway — Webhook & VA Patterns (L-009)

**1. VA Simulator Architecture**
*   External bank simulators should be **deterministic** — same VA number + amount = same response
*   Fixed prefixes per bank (BCA: 12345, BNI: 67890) for easy testing
*   Quarkus Native ideal for simulators: sub-second startup, low memory footprint

**2. Payment Link Webhook Reliability**
*   **HMAC-SHA256 signing** mandatory for webhook payload integrity
*   **Exponential backoff retry** (3x) with jitter for failed deliveries
*   Store webhook delivery attempts in DB for audit trail

**3. Scheduler-Based Expiry Pattern**
*   Single centralized scheduler (`PaymentExpiryScheduler`) instead of multiple per payment type
*   **Optimistic locking** on status updates to prevent race conditions
*   Release reserved balance **before** publishing Kafka event for consistency

## 💰 Settlement & Revenue Share — Financial Engine Patterns (L-010)

**1. Rate Card Engine**
*   Support **3 fee types**: FLAT (fixed amount), PERCENTAGE (of transaction), TIERED (volume-based brackets)
*   **Min/max caps** essential for percentage fees (prevent excessive fees)
*   Link: Partner → Rate Card (1:1 for simplicity, 1:N for complex pricing)

**2. Settlement State Machine**
*   PENDING → PROCESSING → COMPLETED/FAILED/OVERRIDDEN
*   **Never delete** settlement batches — soft delete with status for audit
*   Manual override with **dual-authorization** for amount > threshold

**3. Revenue Split Calculation**
*   **Priority-based stakeholder ordering** — primary stakeholder gets payout first
*   Handle **remaining amount** (rounding errors) — assign to platform or distribute proportionally
*   **Monthly royalty statements** auto-generated with breakdown per transaction

**4. Multi-Currency Settlement**
*   **FX rate locking window** (15 minutes) — prevent rate fluctuation during settlement processing
*   Partner currency preference per settlement batch
*   Auto-conversion only at settlement time, not at transaction time

## 🔑 Gateway Idempotency — `@Idempotent` Annotation Architecture (L-018)

**Architecture (5 layers)**:
1.  `@Idempotent(required = true)` annotation on mutation endpoints — returns 400 if `X-Idempotency-Key` header missing
2.  `IdempotencyInterceptor` (Spring MVC `HandlerInterceptor`) — auto-registered via `IdempotencyAutoConfiguration`
3.  `IdempotencyService` — SHA-256 fingerprints request body, detects key reuse with different payloads (`ConflictException: IDEMPOTENCY_KEY_REUSE`)
4.  `RedisIdempotencyRepository` — atomic Lua script for concurrent duplicate detection (`SETEX if not EXISTS`)
5.  State machine: `IN_PROGRESS` → `COMPLETED` / `FAILED` with 24-hour TTL

*   **Key Design**: The fingerprint check catches accidental key reuse (different requests with same key), not just exact duplicates.
*   **Known Gap**: `ContentCachingResponseWrapper` in `storeSuccessfulResponse()` is a placeholder with no-op `getContentAsByteArray()`. Successful responses are never cached — idempotency only works for error paths. MUST be replaced with Spring's actual `ContentCachingResponseWrapper`.
*   **Rule**: Use `@Idempotent(required = true)` on ALL payment/transfer/mutation POST endpoints. Key must be UUID v4. Use Redis Lua scripts for atomic lock acquisition.

## 🗄️ Database Engineering (PostgreSQL)
*   **Partitioning**: PostgreSQL `HASH` partitioning does NOT support a `DEFAULT` partition. Ensure modulus/remainder coverage is complete.
*   **Unique Constraints**: Unique indexes on partitioned tables MUST include the partitioning key.
*   **Partial Indexes**: Do NOT use mutable functions like `NOW()` or `CURRENT_DATE` in index `WHERE` clauses. These prevent index usage and cause migration failures.
*   **Flyway Verification**: In dev environments, `DROP DATABASE` and let Flyway recreate it if checksum mismatches occur in older migrations.

## 🧪 Testing Patterns (Backend)
*   **Mockito State Trap**: For mutating objects across multiple `save()` calls, use `thenAnswer` to capture state at the moment of invocation. `ArgumentCaptor` only shows the final state.
*   **Slice Test Isolation**: Keep JPA annotations (`@EntityScan`, `@EnableJpaRepositories`) in a separate `JpaConfig.java` to prevent `@WebMvcTest` from failing due to missing `DataSource`.
*   **Validation Regex**: Use `^[a-zA-Z0-9 #().]*$` for transaction descriptions to allow common special characters.
