# ADR-0017: Full Migration to Native Infinispan Hot Rod Protocol & ProtoStream

**Status**: Accepted  
**Date**: 2026-07-17  
**Deciders**: Principal Architect, Core Banking Engineering Team, Infrastructure Team  

## Context

Red Hat Data Grid (Infinispan) was initially accessed in PayU microservices via Redis Emulation Protocol (RESP) mode (`payu.cache.provider=redis`). 
Under production workloads (`INFRA-025`), RESP mode caused client-side cursor iterator leak warnings (`ISPN005061`), lacked topology-aware routing, had no native client-side near-cache push invalidations, and relied on text/JSON strings rather than binary serialization.

To eliminate RESP protocol overhead and unlock Data Grid's enterprise capabilities, PayU requires a complete, platform-wide migration to native **Hot Rod Binary Protocol** (`payu.cache.provider=hotrod`).

## Decision Drivers

- **Performance & Wire Efficiency**: Binary ProtoStream serialization vs heavy JSON strings.
- **Topology Awareness**: Client routing requests directly to the Infinispan cluster node holding the primary key owner (`HASH_DISTRIBUTION_AWARE`).
- **L1 Near Caching**: Server-pushed invalidation (`NearCacheMode.INVALIDATION`) eliminating Kafka invalidation overhead for read-heavy workloads.
- **Reliability & Bug Remediation**: Eliminating RESP iterator leaks (`ISPN005061`) and Netty negotiation warnings.

## Considered Options

### Option 1: Standard Spring Cache Abstraction (`SpringRemoteCacheManager`) Only
- **Pros**: Zero custom code; standard Spring annotations (`@Cacheable`, `@CacheEvict`).
- **Cons**: Cannot support custom Stale-While-Revalidate (SWR), soft-TTL metadata, stampede protection, or explicit async refresh.

### Option 2: Strategy Pattern with `HotRodDistributedCacheServiceImpl` + `SpringRemoteCacheManager` + ProtoStream (CHOSEN)
- **Pros**:
  1. `SpringRemoteCacheManager` configured as standard `@Bean` for declarative Spring caching.
  2. `HotRodDistributedCacheServiceImpl` wrapping `RemoteCacheManager` for programmatically managed SWR, soft TTLs, stampede locking, and async revalidation.
  3. ProtoStream binary serialization for high throughput & compact heap/wire footprints.
  4. Native `NearCacheMode.INVALIDATION` with bounded LFU/LRU capacity (`10,000` entries) for microsecond L1 reads.
- **Cons**: Requires registering `SerializationContextInitializer` ProtoStream schemas for cached domain models.

## Decision

We will perform a **Full Migration to Native Infinispan Hot Rod Protocol**:
1. Deprecate and remove RESP / Redis provider configurations across all PayU microservices in favor of `payu.cache.provider=hotrod`.
2. Provide dual-tier integration in `cache-starter`:
   - `SpringRemoteCacheManager` for declarative `@Cacheable` Spring annotations.
   - `HotRodDistributedCacheServiceImpl` implementing `CacheService` / `DistributedCacheService` for explicit SWR operations.
3. Adopt **ProtoStream** binary marshalling with generated `.proto` schemas (`@ProtoSchema` / `SerializationContextInitializer`) registered via `ConfigurationBuilder.addContextInitializer()` for cached domain objects.
4. Enable **Near Caching Invalidation Mode** (`nearCacheMode(NearCacheMode.INVALIDATION).nearCacheMaxEntries(10000)`) on `RemoteCacheManager` configurations for microsecond local reads with automatic server push invalidation.
5. Store SWR metadata (`createdTimestamp`, `softTtlMs`) inside a `@ProtoSchema`-annotated `CacheEntry<T>` wrapper object stored with Infinispan hard `lifespan`.

## Rationale

- **Best Practice Alignment**: Combining `SpringRemoteCacheManager` and custom `HotRodDistributedCacheServiceImpl` provides maximum flexibility (declarative Spring annotations + high-performance SWR programmatic caching).
- **Near Cache Bounds**: 10,000 entries bound guarantees sub-millisecond local L1 cache hits while preventing OOM crashes in memory-constrained OpenShift pods.
- **Total Migration**: Migrating all services cleanly to Hot Rod avoids dual-stack maintenance, reduces operational complexity, and eliminates RESP protocol bugs (`INFRA-025`).

## Consequences

**Positive**:
- Eliminates `ISPN005061` RESP cursor leaks and Netty SSL negotiation warnings.
- Sub-millisecond read latency via server-assisted near-cache push invalidation.
- Reduced network payload size and JVM GC pressure due to ProtoStream binary encoding.
- Automatic pod-level topology routing to key owners in the Data Grid cluster.

**Negative**:
- Cached DTOs require `@ProtoDoc` / `@ProtoField` annotations or ProtoStream schema initializers.
- Hot Rod port (`11222`) must be strictly exposed and secured via TLS/SASL across OpenShift namespaces.

## Implementation Plan

1. **`cache-starter` Core Update**:
   - Create `HotRodDistributedCacheServiceImpl` implementing `DistributedCacheService`.
   - Configure `SpringRemoteCacheManager` bean alongside `RemoteCacheManager`.
   - Implement ProtoStream marshaller context with default `CacheEntry` schema.
   - Configure near-cache invalidation mode with `nearCacheMaxEntries(10000)`.
2. **Microservice Rollout**:
   - Update `application.yml` in core services (`account-service`, `wallet-service`, `transaction-service`, `product-catalog-service`) to set `payu.cache.provider=hotrod`.
   - Remove legacy Redis/RESP dependencies and `RedisCacheConfig`.
3. **Verification & Testing**:
   - Run unit & integration tests (`HotRodCacheConfigTest`, `VerifyNikCacheRoundTripTest`).
   - Validate near-cache invalidation across multi-node pod deployments.

---
*Created via `grill-with-docs` session (ARCH-007 / INFRA-025)*
