# ADR-0009: Caching Strategy (L1/L2)

**Status**: Accepted
**Date**: 2026-01-30
**Last Updated**: 2026-03-11
**Deciders**: Architecture Team

## Context

High-traffic endpoints (e.g., wallet balance, product catalogs) require low latency. Database lookups for every request are inefficient and not scalable.

## Decision

Implement a **Multi-Layer Caching Strategy** using `cache-starter`.

### Layers

1. **L1 Cache (Local)**: Caffeine
    - **Usage**: High-frequency, low-change data (e.g., config, dictionaries).
    - **Pros**: Microsecond access (in-memory).
    - **TTL**: Short (1-5 min) or specific invalidation.
2. **L2 Cache (Distributed)**: Redis / Red Hat Data Grid (RESP mode)
    - **Usage**: Shared state, user sessions, temporary data.
    - **Pros**: Consistent across instances, survives restart.
    - **TTL**: Medium (10-60 min).

## Implementation

- Use Spring Cache abstraction (`@Cacheable`).
- `cache-starter` auto-configures Redis and Caffeine managers.
- Keys must be namespaced: `service:domain:id`.

### Advanced Features (Implemented)

- **Stale-While-Revalidate**: `CacheService` serves stale data while refreshing in background, preventing cache stampede.
- **Spring-Managed Thread Pools** (IMP-068): `@Bean(name = "cacheRefreshExecutor")` with `ThreadPoolTaskExecutor` for background refresh. Micrometer metrics via `ExecutorServiceMetrics.monitor()`. Graceful shutdown with `waitForTasksToCompleteOnShutdown=true`.
- **Type-Safe Deserialization** (BUG-BE-074): `DistributedCacheService` uses `ObjectMapper.convertValue()` for safe JSON→Java conversion from Redis/Data Grid.
- **Red Hat Data Grid Compatibility**: `CacheProperties` and `RedisCacheConfig` Javadoc with RESP mode config examples for DataGrid deployment.

### Bug Fixes Applied

- **BUG-BE-103**: `CacheEntry<V>` made `static` inner class to prevent memory leak.
- **BUG-BE-104**: `refresh()` wrapped in try-catch, retains stale value on failure.
- **BUG-BE-074**: Type-safe deserialization with `convertToCacheEntry()` and `convertToType()` helpers.
- **IMP-068**: Replaced static unmanaged `Executors.newCachedThreadPool()` with Spring-managed thread pools.

### Cache Key Invalidation Pattern (wallet-service)

- All mutation methods (balance, reserve, commit, release, credit) invalidate 4 cache keys per wallet (BUG-BE-004).

## Consequences

- **Positive**: Reduced DB load, sub-millisecond read times, resilient to cache refresh failures.
- **Negative**: Cache invalidation complexity, consistency lag.
