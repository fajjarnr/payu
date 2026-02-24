# Core Banking & Backend Engineering Patterns

## 🏗️ Hexagonal Architecture Implementation
Standard package structure for PayU services:
```
com.payu.{service}/
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

## 🗄️ Database Engineering (PostgreSQL)
*   **Partitioning**: PostgreSQL `HASH` partitioning does NOT support a `DEFAULT` partition. Ensure modulus/remainder coverage is complete.
*   **Unique Constraints**: Unique indexes on partitioned tables MUST include the partitioning key.
*   **Partial Indexes**: Do NOT use mutable functions like `NOW()` or `CURRENT_DATE` in index `WHERE` clauses. These prevent index usage and cause migration failures.
*   **Flyway Verification**: In dev environments, `DROP DATABASE` and let Flyway recreate it if checksum mismatches occur in older migrations.

## 🧪 Testing Patterns (Backend)
*   **Mockito State Trap**: For mutating objects across multiple `save()` calls, use `thenAnswer` to capture state at the moment of invocation. `ArgumentCaptor` only shows the final state.
*   **Slice Test Isolation**: Keep JPA annotations (`@EntityScan`, `@EnableJpaRepositories`) in a separate `JpaConfig.java` to prevent `@WebMvcTest` from failing due to missing `DataSource`.
*   **Validation Regex**: Use `^[a-zA-Z0-9 #().]*$` for transaction descriptions to allow common special characters.
