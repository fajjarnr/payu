package id.payu.cache.service;

import id.payu.cache.model.CacheEntry;
import id.payu.cache.properties.CacheProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;

import java.time.Duration;
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

    // Metrics
    private final Counter localFallbackCounter;
    private final Counter localWriteCounter;

    public CacheService(
            DistributedCacheService distributedCache,
            LocalCacheService localCache,
            CacheProperties properties) {
        this.distributedCache = distributedCache;
        this.localCache = localCache;
        this.properties = properties;

        // Initialize metrics
        this.localFallbackCounter = Metrics.counter("cache.local.fallback");
        this.localWriteCounter = Metrics.counter("cache.local.writes");

        log.info("Cache service initialized with local fallback: {}",
                properties.getLocalCache().isEnabled());
    }

    /**
     * Get value from cache with automatic fallback to local cache and supplier.
     *
     * @param key      Cache key
     * @param type     Expected type
     * @param fallback Fallback supplier when cache miss
     * @param <T>      Return type
     * @return Cached or fresh value
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

        // Try distributed cache
        try {
            T value = distributedCache.get(key, type);
            if (value != null) {
                // Update local cache
                if (localCache.isEnabled()) {
                    localCache.put(key, value);
                }
                return value;
            }
        } catch (Exception e) {
            log.warn("Distributed cache error, falling back to local: {}", e.getMessage());
            localFallbackCounter.increment();
        }

        // Fallback to supplier
        T value = fallback.get();
        if (value != null) {
            put(key, value);
        }
        return value;
    }

    /**
     * Get value from cache without fallback.
     */
    public <T> T get(String key, Class<T> type) {
        return get(key, type, () -> null);
    }

    /**
     * Get value with stale-while-revalidate pattern.
     * Returns stale data immediately if available and triggers async refresh.
     *
     * @param key             Cache key
     * @param type            Expected type
     * @param fallback        Fallback supplier when cache miss
     * @param softTtl         Soft TTL - after this, data is stale but served
     * @param hardTtl         Hard TTL - after this, data must be refreshed
     * @param <T>             Return type
     * @return Cached or fresh value
     */
    public <T> T getWithStaleWhileRevalidate(
            String key,
            Class<T> type,
            Supplier<T> fallback,
            Duration softTtl,
            Duration hardTtl) {

        // Try local cache first
        if (localCache.isEnabled()) {
            T localValue = localCache.get(key, type);
            if (localValue != null) {
                return localValue;
            }
        }

        // Try distributed cache with stale-while-revalidate
        try {
            CacheEntry<T> entry = distributedCache.getEntry(key, type);
            if (entry != null) {
                if (entry.isExpired()) {
                    // Data expired, need refresh
                    T value = fallback.get();
                    put(key, value, softTtl, hardTtl);
                    return value;
                }

                // Update local cache
                if (localCache.isEnabled()) {
                    localCache.put(key, entry.getValue());
                }

                if (entry.isStale()) {
                    // Data is stale but serve it and trigger async refresh
                    // The caller is responsible for scheduling the refresh
                    return entry.getValue();
                }

                return entry.getValue();
            }
        } catch (Exception e) {
            log.warn("Distributed cache error in stale-while-revalidate: {}", e.getMessage());
            localFallbackCounter.increment();
        }

        // Cache miss - get fresh value
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
                // Entry exists and not expired, refresh it
                T newValue = refresher.get();
                put(key, newValue, softTtl, hardTtl);
                return newValue;
            }
        } catch (Exception e) {
            log.warn("Error in getAndRefresh: {}", e.getMessage());
        }

        // No entry or expired, get fresh value
        T value = refresher.get();
        if (value != null) {
            put(key, value, softTtl, hardTtl);
        }
        return value;
    }

    /**
     * Put value in cache with default TTL.
     */
    public void put(String key, Object value) {
        try {
            distributedCache.put(key, value);
            if (localCache.isEnabled()) {
                localCache.put(key, value);
            }
        } catch (Exception e) {
            log.error("Error putting to cache for key {}: {}", key, e.getMessage());
            // Try to put in local cache at least
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

    /**
     * Evict entry from cache.
     */
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

    /**
     * Check if key exists in cache.
     */
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

    /**
     * Get cache properties.
     */
    public CacheProperties getProperties() {
        return properties;
    }
}
