# ADR-0004: Hexagonal Architecture for Domain Services

**Status**: Accepted
**Date**: 2026-01-30
**Deciders**: Architecture Team, Engineering Leads

## Context

PayU platform needs to maintain clean separation between business logic and technical concerns. As the system grows, we need an architecture that:

- Isolates domain logic from infrastructure
- Enables easy testing of business rules
- Supports technology changes without affecting domain
- Follows DDD principles

## Decision Drivers

- **Testability**: Business logic should be testable without frameworks
- **Flexibility**: Easy to swap databases, APIs, or messaging
- **Maintainability**: Clear separation of concerns
- **Domain Focus**: Business logic not coupled to technical details

## Considered Options

### Option 1: Hexagonal (Ports and Adapters) Architecture
- **Pros**:
  - Domain logic completely isolated
  - Easy to test (no framework dependencies)
  - Supports technology changes
  - Follows DDD principles
- **Cons**:
  - More initial boilerplate
  - Learning curve for developers
- **Complexity**: Medium
- **Rationale**: Best for complex domain logic (core banking)

### Option 2: Layered Architecture
- **Pros**:
  - Simple and familiar
  - Less boilerplate
- **Cons**:
  - Domain coupled to infrastructure
  - Harder to test in isolation
  - Technology changes affect domain
- **Complexity**: Low
- **Rationale**: Suitable for simple CRUD, not banking

### Option 3: Onion Architecture
- **Pros**:
  - Similar benefits to Hexagonal
  - Domain at center
- **Cons**:
  - More complex structure
  - Overkill for microservices
- **Complexity**: High
- **Rationale**: Too complex for microservice architecture

## Decision

**Choose Hexagonal Architecture** for all 19 Java/Quarkus microservices.

> **Amendment (Feb 10, 2026)**: Originally scoped to 6 core services only. After TD-ARCH-004 refactoring (Batches 1-3), all 19 services now use hexagonal architecture — 100% compliance. The simpler services (notification, cms, ab-testing, etc.) also benefited from clear port/adapter separation for testability.

## Rationale

1. **Domain Isolation**: Business logic independent of Spring/Quarkus
2. **Testability**: Pure unit tests without mocks
3. **Flexibility**: Easy to swap databases, APIs
4. **DDD Alignment**: Clear bounded contexts

## Consequences

**Positive**:
- Clean domain logic
- Easy to test
- Technology flexibility
- Better code organization

**Negative**:
- More boilerplate initially
- Learning curve for new developers
- More interfaces and abstractions

**Trade-offs Accepted**:
- Accept more boilerplate for cleaner architecture
- Accept learning curve for long-term maintainability

## Implementation Notes

### Package Structure

```
src/main/java/id/payu/{service}/
├── domain/
│   ├── model/          # Entities, Value Objects
│   ├── repository/     # Repository interfaces (Ports)
│   ├── service/         # Domain services
│   └── event/          # Domain events
├── application/
│   ├── command/        # Command handlers
│   ├── query/          # Query handlers
│   └── dto/            # Input/Output DTOs
├── infrastructure/
│   ├── persistence/    # JPA repositories (Adapters)
│   ├── messaging/      # Kafka producers/consumers (Adapters)
│   └── external/       # External API clients (Adapters)
└── api/
    └── rest/           # REST controllers
```

### Example Repository Interface (Port)

```java
package id.payu.wallet.domain.repository;

import id.payu.wallet.domain.model.Wallet;

public interface WalletRepository {
    Wallet save(Wallet wallet);
    Optional<Wallet> findById(String id);
    Optional<Wallet> findByAccountId(String accountId);
}
```

### Example JPA Implementation (Adapter)

```java
package id.payu.wallet.infrastructure.persistence;

import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.repository.WalletRepository;
import org.springframework.stereotype.Repository;

@Repository
public class WalletRepositoryJpa implements WalletRepository {
    // JPA implementation
}
```

---

*Created via @principal-architect*
