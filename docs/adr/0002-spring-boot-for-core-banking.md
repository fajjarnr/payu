# ADR-0002: Spring Boot for Core Banking Services

**Status**: Accepted
**Date**: 2026-01-30
**Deciders**: Architecture Team, Engineering Leads

## Context

PayU Digital Banking Platform requires a robust, enterprise-grade framework for core banking services (account, transaction, wallet, etc.). These services handle financial transactions and require:

- Strong typing and compile-time safety
- Mature ecosystem with banking-specific libraries
- Excellent testing support (JUnit, Mockito)
- Enterprise support and long-term stability
- Compatibility with Java EE/Jakarta EE standards

## Decision Drivers

- **Safety**: Financial transactions require compile-time type safety
- **Ecosystem**: Access to Spring Security, Spring Data, Spring Kafka
- **Talent**: Large pool of Spring Boot developers in Indonesia
- **Support**: Enterprise-grade support from VMware/Broadcom
- **Maturity**: Proven in production at major banks globally

## Considered Options

### Option 1: Spring Boot 3.4
- **Pros**:
  - Mature ecosystem with banking-specific libraries
  - Excellent testing support (JUnit 5, Mockito, Testcontainers)
  - Spring Security for OAuth2/OIDC
  - Spring Data JPA for database access
  - Spring Kafka for event streaming
  - Enterprise support available
- **Cons**:
  - Heavier memory footprint
  - Slower startup time compared to Quarkus
- **Complexity**: Medium
- **Rationale**: Best fit for complex business logic with strong typing

### Option 2: Quarkus 3.x
- **Pros**:
  - Fast startup time
  - Lower memory footprint
  - Native compilation support
- **Cons**:
  - Smaller ecosystem
  - Less proven in banking
  - Different programming model
- **Complexity**: Medium
- **Rationale**: Better suited for supporting services, not core banking

### Option 3: Micronaut
- **Pros**:
  - Fast startup time
  - Cloud-native design
- **Cons**:
  - Smaller ecosystem
  - Less mature than Spring Boot
  - Fewer developers with experience
- **Complexity**: Medium
- **Rationale**: Not enough banking-specific libraries

### Option 4: Node.js/TypeScript
- **Pros**:
  - Large talent pool
  - Fast development
- **Cons**:
  - No compile-time type safety
  - Weak typing for financial calculations
  - Not suitable for financial transactions
- **Complexity**: Low
- **Rationale**: Not acceptable for core banking due to type safety

## Decision

**Choose Spring Boot 3.4** for all Core Banking services:
- account-service
- auth-service
- transaction-service
- wallet-service
- investment-service
- lending-service
- fx-service
- statement-service
- backoffice-service
- partner-service
- promotion-service
- support-service
- compliance-service
- cms-service
- ab-testing-service

## Rationale

1. **Type Safety**: Java's strong typing is critical for financial calculations
2. **Ecosystem**: Spring Security, Spring Data, Spring Kafka are industry standards
3. **Testing**: JUnit 5 + Mockito + Testcontainers provide excellent testing
4. **Talent**: Large pool of Spring Boot developers in Indonesia
5. **Support**: Enterprise support available from VMware/Broadcom
6. **Maturity**: Proven in production at major banks globally

## Consequences

**Positive**:
- Industry-standard framework for banking
- Strong typing for financial calculations
- Excellent testing support
- Large ecosystem of libraries
- Enterprise support available

**Negative**:
- Higher memory footprint
- Slower startup time
- More complex configuration

**Trade-offs Accepted**:
- Accept higher memory usage for type safety and ecosystem
- Accept slower startup for maturity and support

## Implementation Notes

### Standard Spring Boot Configuration

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.1</version>
</parent>
```

### Required Dependencies

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-validation
- spring-boot-starter-actuator
- spring-kafka
- spring-boot-starter-test

### Code Structure

Follow Hexagonal Architecture:
- `domain/` - Business logic (entities, value objects, domain services)
- `application/` - Use cases, ports (interfaces)
- `infrastructure/` - Adapters (repositories, external services)
- `api/` - REST controllers

---

*Created via @principal-architect*
