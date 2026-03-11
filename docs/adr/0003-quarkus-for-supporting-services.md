# ADR-0003: Quarkus Native for Supporting Services

**Status**: Accepted
**Date**: 2026-01-30
**Last Updated**: 2026-03-11
**Deciders**: Architecture Team, Engineering Leads

## Context

Supporting services (gateway, billing, notification) require high performance and low resource consumption. These services are:

- High-throughput (gateway: routing, rate limiting)
- IO-bound (billing: external API calls, notification: SMS/email)
- Stateless or minimally stateful
- Can benefit from native compilation

## Decision Drivers

- **Performance**: Low latency and high throughput
- **Resource Efficiency**: Lower memory footprint
- **Startup Time**: Fast scaling for stateless services
- **Native Compilation**: Quarkus Native for production
- **Developer Experience**: Live reload during development

## Considered Options

### Option 1: Quarkus 3.x Native

- **Pros**:
  - Native compilation with GraalVM
  - Fast startup time (< 100ms native)
  - Low memory footprint (< 64MB native)
  - Live reload during development
  - Excellent for cloud-native workloads
- **Cons**:
  - Smaller ecosystem than Spring Boot
  - Different programming model
- **Complexity**: Medium
- **Rationale**: Best fit for high-throughput supporting services

### Option 2: Spring Boot 3.4

- **Pros**:
  - Consistent with core banking services
  - Mature ecosystem
- **Cons**:
  - Higher memory footprint
  - Slower startup time
  - Not optimized for native compilation
- **Complexity**: Medium
- **Rationale**: Overkill for simple supporting services

### Option 3: Node.js/TypeScript

- **Pros**:
  - Fast development
  - Good for IO-bound workloads
- **Cons**:
  - Runtime overhead
  - No native compilation
  - Different tech stack
- **Complexity**: Low
- **Rationale**: Not suitable for enterprise consistency

## Decision

**Choose Quarkus 3.x Native** for Supporting services:

- gateway-service
- notification-service
- api-portal-service

**Quarkus Simulators** (test infrastructure):

- biller-simulator _(added Feb 2026)_
- va-simulator _(added Feb 2026)_
- qris-simulator
- bi-fast-simulator
- dukcapil-simulator

> **Amendment (Mar 11, 2026)**: `billing-service` was originally planned as Quarkus but was implemented using **Spring Boot 3.4** due to deeper integration needs with shared starters (`security-starter`, `resilience-starter`, `cache-starter`) and JPA/Flyway patterns consistent with other core services. Moved to ADR-0002 scope. Simulator services added as Quarkus services for lightweight external mocking.

## Rationale

1. **Native Compilation**: Sub-100ms startup time, < 64MB memory
2. **Live Reload**: Instant feedback during development
3. **Resource Efficiency**: Lower cloud costs
4. **Performance**: Excellent for IO-bound workloads
5. **Reactive**: Built-in Mutiny (reactive programming)

## Consequences

**Positive**:

- Fast startup and scaling
- Low memory footprint
- Lower cloud costs
- Excellent for stateless services

**Negative**:

- Different framework from core services
- Smaller ecosystem
- Learning curve for Spring developers

**Trade-offs Accepted**:

- Accept framework diversity for resource efficiency
- Accept learning curve for performance benefits

## Implementation Notes

### Standard Quarkus Configuration

```xml
<quarkus.platform.version>3.17.5</quarkus.platform.version>
```

### Required Dependencies

- quarkus-resteasy-reactive
- quarkus-rest-client
- quarkus-hibernate-validator
- quarkus-smallrye-openapi
- quarkus-micrometer
- quarkus-arc (dependency injection)

### Build for Native

```bash
./mvnw package -Dnative -DskipTests
```

### Dockerfile for Native

```dockerfile
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.4
WORKDIR /work/
COPY target/*-runner /work/application
chmod 775 /work/application
EXPOSE 8080
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
```

---

_Created via @principal-architect_
