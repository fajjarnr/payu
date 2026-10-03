package id.payu.cache.service;

import id.payu.cache.model.CacheEntry;
import id.payu.cache.properties.CacheProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.QueryTimeoutException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Primary cache service combining distributed (Redis) and local (Caffeine) caching.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Multi-layer caching: Redis (L1) + Local (L2 fallback)</li>
 *   <li>Automatic fallback when Redis is unavailable</li>
 *   <li>Stale-while-revalidate pattern</li>
 *   <li>Unified API for all cache operations</li>
 *   <li>Metrics and observability</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@literal @Autowired}
 * private CacheService cacheService;
 *
 * // Simple get with fallback
 * Account account = cacheService.get(
 *     "account:123",
 *     Account.class,
 *     () -> accountRepository.findById("123")
 * );
 *
 * // With custom TTL
 * cacheService.put("account:123", account, Duration.ofMinutes(10));
 *
 * // With stale-while-revalidate
 * Balance balance = cacheService.getWithStaleWhileRevalidate(
 *     "balance:123",
 *     Balance.class,
 *     () -> balanceRepository.findByAccountId("123"),
 *     Duration.ofSeconds(15),  // soft TTL
 *     Duration.ofSeconds(30)   // hard TTL
 * );
 * </pre>
 */
@Slf4j
public class CacheService {

    private final DistributedCacheService distributedCache;
    private final LocalCacheService localCache;
    private final CacheProperties properties;
    private final Executor cacheRefreshExecutor;

    private final Counter localFallbackCounter;
    private final Counter localWriteCounter;

    /**
     * Constructor with Spring-managed executor.
     */
    public CacheService(
            DistributedCacheService distributedCache,
            LocalCacheService localCache,
            CacheProperties properties,
            @Qualifier("cacheRefreshExecutor") Executor cacheRefreshExecutor) {
        this.distributedCache = distributedCache;
        this.localCache = localCache;
        this.properties = properties;
        this.cacheRefreshExecutor = cacheRefreshExecutor;

        this.localFallbackCounter = Metrics.counter("cache.local.fallback");
        this.localWriteCounter = Metrics.counter("cache.local.writes");

        log.info("Cache service initialized with local fallback: {}",
                properties.getLocalCache().isEnabled());
    }

    /**
     * Get value from cache with automatic fallback to local cache and supplier.
     */
    public <T> T get(String key, Class<T> type, Supplier<T> fallback) {
        // Try local cache first (fastest)
        if (localCache.isEnabled()) {
            T localValue = localCache.get(key, type);
            if (localValue != null) {
                log.debug("Local cache hit for key: {}", key);
                return localValue;
            }
        }

        try {
            T value = distributedCache.get(key, type);
            if (value != null) {
                if (localCache.isEnabled()) {
                    localCache.put(key, value);
                }
                return value;
            }
        } catch (Exception e) {
            log.warn("Distributed cache error, falling back to local: {}", e.getMessage());
            localFallbackCounter.increment();
        }

        T value = fallback.get();
        if (value != null) {
            put(key, value);
        }
        return value;
    }

    public <T> T get(String key, Class<T> type) {
        return get(key, type, () -> null);
    }

    /**
     * Get value with stale-while-revalidate pattern.
     * Returns stale data immediately if available and triggers async refresh.
     *
     * @param fallback        Fallback supplier when cache miss
     * @param softTtl         Soft TTL - after this, data is stale but served
     * @param hardTtl         Hard TTL - after this, data must be refreshed
     */
    public <T> T getWithStaleWhileRevalidate(
            String key,
            Class<T> type,
            Supplier<T> fallback,
            Duration softTtl,
            Duration hardTtl) {

        if (localCache.isEnabled()) {
            T localValue = localCache.get(key, type);
            if (localValue != null) {
                return localValue;
            }
        }

        try {
            CacheEntry<T> entry = distributedCache.getEntry(key, type);
            if (entry != null) {
                if (entry.isExpired()) {
                    T value = fallback.get();
                    put(key, value, softTtl, hardTtl);
                    return value;
                }

                if (localCache.isEnabled()) {
                    localCache.put(key, entry.getValue());
                }

                if (entry.isStale()) {
                    // IMP-068: Trigger async background refresh using Spring-managed executor
                    final Duration sTtl = softTtl;
                    final Duration hTtl = hardTtl;
                    CompletableFuture.runAsync(() -> {
                        try {
                            T refreshed = fallback.get();
                            if (refreshed != null) {
                                put(key, refreshed, sTtl, hTtl);
                            }
                        } catch (Exception ex) {
                            log.warn("Async cache refresh failed for key '{}': {}", key, ex.getMessage());
                        }
                    }, cacheRefreshExecutor);
                    return entry.getValue();
                }

                return entry.getValue();
            }
        } catch (Exception e) {
            log.warn("Distributed cache error in stale-while-revalidate: {}", e.getMessage());
            localFallbackCounter.increment();
        }

        T value = fallback.get();
        if (value != null) {
            put(key, value, softTtl, hardTtl);
        }
        return value;
    }

    /**
     * Get and refresh cache entry atomically.
     * Useful for manual stale-while-revalidate implementation.
     */
    public <T> T getAndRefresh(
            String key,
            Class<T> type,
            Supplier<T> refresher,
            Duration softTtl,
            Duration hardTtl) {

        try {
            CacheEntry<T> entry = distributedCache.getEntry(key, type);
            if (entry != null && !entry.isExpired()) {
                T newValue = refresher.get();
                put(key, newValue, softTtl, hardTtl);
                return newValue;
            }
        } catch (Exception e) {
            log.warn("Error in getAndRefresh: {}", e.getMessage());
        }

        T value = refresher.get();
        if (value != null) {
            put(key, value, softTtl, hardTtl);
        }
        return value;
    }

    public void put(String key, Object value) {
        try {
            distributedCache.put(key, value);
            if (localCache.isEnabled()) {
                localCache.put(key, value);
            }
        } catch (Exception e) {
            log.error("Error putting to cache for key {}: {}", key, e.getMessage());
            if (localCache.isEnabled()) {
                localCache.put(key, value);
                localWriteCounter.increment();
            }
        }
    }

    /**
     * Put value in cache with custom TTL (hard TTL only).
     */
    public void put(String key, Object value, Duration ttl) {
        put(key, value, ttl, ttl);
    }

    /**
     * Put value in cache with soft and hard TTL (stale-while-revalidate).
     */
    public void put(String key, Object value, Duration softTtl, Duration hardTtl) {
        try {
            distributedCache.put(key, value, softTtl.getSeconds(), hardTtl.getSeconds());
            if (localCache.isEnabled()) {
                localCache.put(key, value);
            }
        } catch (Exception e) {
            log.error("Error putting to cache for key {}: {}", key, e.getMessage());
            if (localCache.isEnabled()) {
                localCache.put(key, value);
                localWriteCounter.increment();
            }
        }
    }

    public void invalidate(String key) {
        try {
            distributedCache.evict(key);
        } catch (Exception e) {
            log.error("Error invalidating cache for key {}: {}", key, e.getMessage());
        }
        if (localCache.isEnabled()) {
            localCache.evict(key);
        }
    }

    public boolean exists(String key) {
        try {
            return distributedCache.exists(key);
        } catch (Exception e) {
            log.error("Error checking cache existence for key {}: {}", key, e.getMessage());
            return localCache.isEnabled() && localCache.get(key, Object.class) != null;
        }
    }

    /**
     * Get distributed cache service for advanced operations.
     */
    public DistributedCacheService getDistributedCache() {
        return distributedCache;
    }

    /**
     * Get local cache service for direct access.
     */
    public LocalCacheService getLocalCache() {
        return localCache;
    }

    public CacheProperties getProperties() {
        return properties;
    }
}
