# ADR-0009: Caching Strategy (L1/L2)

**Status**: Accepted
**Date**: 2026-01-30
**Deciders**: Architecture Team

## Context

High-traffic endpoints (e.g., wallet balance, product catalogs) require low latency. Database lookups for every request are inefficient and not scalable.

## Decision

Implement a **Multi-Layer Caching Strategy** using `cache-starter`.

### Layers

1.  **L1 Cache (Local)**: Caffeine
    - **Usage**: High-frequency, low-change data (e.g., config, dictionaries).
    - **Pros**: Microsecond access (in-memory).
    - **TTL**: Short (1-5 min) or specific invalidation.
2.  **L2 Cache (Distributed)**: Redis
    - **Usage**: Shared state, user sessions, temporary data.
    - **Pros**: Consistent across instances, survives restart.
    - **TTL**: Medium (10-60 min).

## Implementation

- Use Spring Cache abstraction (`@Cacheable`).
- `cache-starter` auto-configures Redis and Caffeine managers.
- Keys must benamespaced: `service:domain:id`.

## Consequences

- **Positive**: Reduced DB load, sub-millisecond read times.
- **Negative**: Cache invalidation complexity, consistency lag.
