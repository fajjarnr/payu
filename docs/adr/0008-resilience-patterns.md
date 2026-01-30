# ADR-0008: Resilience Patterns (Circuit Breaker & Retry)

**Status**: Accepted
**Date**: 2026-01-30
**Deciders**: Architecture Team

## Context

Microservices are distributed by nature and failures are inevitable. We need a standard way to handle faults (timeouts, unavailability) to prevent cascading failures across the system.

## Decision

Use **Resilience4j** via a shared library (`resilience-starter`) to implement resilience patterns across all services.

### Patterns Adopted

1.  **Circuit Breaker**:
    - Stop requests to failing services to allow recovery.
    - Configuration: Open after 50% failure rate, wait 30s before half-open.
2.  **Retry**:
    - Automatically retry transient failures (network blips).
    - Configuration: Max 3 retries with exponential backoff.
3.  **Bulkhead**:
    - Limit concurrent requests to isolate downstream failures.
4.  **Fallback**:
    - Provide default behavior or helpful error messages when operations fail.

## Implementation

- All core banking services MUST import `resilience-starter`.
- Use the custom annotation `@FinancialOperation` which wraps common resilience config.
- Configure via `application.yml` properties `resilience4j.circuitbreaker`.

## Consequences

- **Positive**: Increased system availability, graceful integration degradation.
- **Negative**: Complexity in tuning timeouts and thresholds.
