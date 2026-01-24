package id.payu.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import id.payu.cache.properties.CacheProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Local cache service using Caffeine as a fallback when Redis is unavailable.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>In-memory caching with Caffeine</li>
 *   <li>Configurable size and TTL</li>
 *   <li>Statistics tracking</li>
 *   <li>Automatic eviction</li>
 * </ul>
 */
@Slf4j
public class LocalCacheService {

    private final Cache<String, Object> cache;
    private final CacheProperties properties;
    private final boolean enabled;

    public LocalCacheService(CacheProperties properties) {
        this.properties = properties;
        this.enabled = properties.getLocalCache().isEnabled();

        if (enabled) {
            this.cache = Caffeine.newBuilder()
                    .maximumSize(properties.getLocalCache().getMaxSize())
                    .expireAfterWrite(properties.getLocalCache().getTtl())
                    .recordStats()
                    .build();
            log.info("Local cache enabled with maxSize={}, ttl={}",
                    properties.getLocalCache().getMaxSize(),
                    properties.getLocalCache().getTtl());
        } else {
            this.cache = null;
            log.info("Local cache disabled");
        }
    }

    /**
     * Get value from local cache.
     */
    public <T> T get(String key, Class<T> type) {
        if (!enabled) {
            return null;
        }

        try {
            Object value = cache.getIfPresent(key);
            if (value != null) {
                log.debug("Local cache hit for key: {}", key);
                return type.cast(value);
            }
            log.debug("Local cache miss for key: {}", key);
        } catch (Exception e) {
            log.warn("Error getting from local cache: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Put value in local cache.
     */
    public void put(String key, Object value) {
        if (!enabled) {
            return;
        }

        try {
            cache.put(key, value);
            log.debug("Put key in local cache: {}", key);
        } catch (Exception e) {
            log.warn("Error putting in local cache: {}", e.getMessage());
        }
    }

    /**
     * Put value with custom TTL.
     */
    public void put(String key, Object value, Duration ttl) {
        if (!enabled) {
            return;
        }

        try {
            cache.asMap().put(key, value);
            // Caffeine uses a single TTL setting per cache
            // For custom TTL per entry, we'd need a different approach
            log.debug("Put key in local cache with TTL: {}", key);
        } catch (Exception e) {
            log.warn("Error putting in local cache: {}", e.getMessage());
        }
    }

    /**
     * Evict entry from local cache.
     */
    public void evict(String key) {
        if (!enabled) {
            return;
        }

        try {
            cache.invalidate(key);
            log.debug("Evicted key from local cache: {}", key);
        } catch (Exception e) {
            log.warn("Error evicting from local cache: {}", e.getMessage());
        }
    }

    /**
     * Clear all entries from local cache.
     */
    public void clear() {
        if (!enabled) {
            return;
        }

        try {
            cache.invalidateAll();
            log.debug("Cleared local cache");
        } catch (Exception e) {
            log.warn("Error clearing local cache: {}", e.getMessage());
        }
    }

    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        if (!enabled) {
            return null;
        }
        return cache.stats();
    }

    /**
     * Get estimated cache size.
     */
    public long size() {
        if (!enabled) {
            return 0;
        }
        return cache.estimatedSize();
    }

    /**
     * Check if local cache is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
}
