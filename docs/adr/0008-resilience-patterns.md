# ADR-0008: Resilience Patterns (Circuit Breaker & Retry)

**Status**: Accepted
**Date**: 2026-01-30
**Last Updated**: 2026-03-11
**Deciders**: Architecture Team

## Context

Microservices are distributed by nature and failures are inevitable. We need a standard way to handle faults (timeouts, unavailability) to prevent cascading failures across the system.

## Decision

Use **Resilience4j** via a shared library (`resilience-starter`) to implement resilience patterns across all services.

### Patterns Adopted

1. **Circuit Breaker**:
    - Stop requests to failing services to allow recovery.
    - Configuration: Open after 50% failure rate, wait 30s before half-open, volume threshold 10.
2. **Retry**:
    - Automatically retry transient failures (network blips).
    - Configuration: Max 3 retries with 500ms exponential backoff.
3. **Bulkhead**:
    - Limit concurrent requests to isolate downstream failures.
    - Configuration: 20 concurrent calls, 500ms max wait.
4. **Fallback**:
    - Provide default behavior or helpful error messages when operations fail.
    - Returns 503 Service Unavailable with `Retry-After` header (RFC 7231).

## Implementation

- All core banking services MUST import `resilience-starter`.
- Use the custom annotation `@FinancialOperation` which wraps common resilience config.
- Configure via `application.yml` properties `resilience4j.circuitbreaker`.

### Gateway Circuit Breaker (IMP-003, IMP-067)

- Per-service circuit breakers via `ConcurrentHashMap` in `CircuitBreakerService`.
- `Retry-After` header on 503 when circuit is OPEN.
- Health endpoints:
  - `GET /health/circuits` — all services summary
  - `GET /health/circuits/{serviceName}` — per-service detail
  - `POST /health/circuits/{serviceName}/reset` — admin reset
- Health status degrades to `DEGRADED` when any circuit is OPEN.

### Bug Fixes Applied

- **BUG-BE-093**: Fixed broken Spring property placeholders in `@FinancialOperation` — replaced with hardcoded `"financial"` literal names.
- **BUG-BE-097**: Set `matchIfMissing=true` so `resilience-starter` auto-enables.
- **BUG-BE-098**: Removed duplicate `TimeoutException.class` in `@ExceptionHandler`.
- **BUG-BE-099**: Dynamic CB registration via `onEntryAdded()` handler.
- **BUG-BE-106**: Added `Throwable.class.isAssignableFrom()` validation before unchecked cast.

## Consequences

- **Positive**: Increased system availability, graceful integration degradation, observable circuit states via health endpoints.
- **Negative**: Complexity in tuning timeouts and thresholds.
