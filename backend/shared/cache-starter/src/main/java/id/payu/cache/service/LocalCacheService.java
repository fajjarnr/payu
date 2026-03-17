package id.payu.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import id.payu.cache.properties.CacheProperties;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Local cache service using Caffeine as a fallback when Redis is unavailable.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>In-memory caching with Caffeine</li>
 *   <li>Configurable size and TTL</li>
 *   <li>Statistics tracking with Micrometer</li>
 *   <li>Automatic eviction</li>
 *   <li>Failure detection and circuit breaker pattern</li>
 * </ul>
 */
@Slf4j
public class LocalCacheService {

    private final Cache<String, Object> cache;
    private final CacheProperties properties;
    private final boolean enabled;
    private final boolean recordStats;

    // Per-entry TTL tracking for variable expiration support
    private final ConcurrentHashMap<String, Long> customTtlNanos = new ConcurrentHashMap<>();
    private final long defaultTtlNanos;

    // Failure tracking for circuit breaker pattern
    private volatile boolean redisAvailable = true;
    private volatile long lastRedisFailureTime = 0;
    private static final long REDIS_FAILURE_COOLDOWN_MS = 30000; // 30 seconds

    // Metrics
    private final io.micrometer.core.instrument.Counter hitCounter;
    private final io.micrometer.core.instrument.Counter missCounter;
    private final io.micrometer.core.instrument.Counter evictionCounter;
    private final io.micrometer.core.instrument.Gauge sizeGauge;

    public LocalCacheService(CacheProperties properties) {
        this.properties = properties;
        this.enabled = properties.getLocalCache().isEnabled();
        this.recordStats = properties.getLocalCache().isRecordStats();
        this.defaultTtlNanos = properties.getLocalCache().getTtl().toNanos();

        if (enabled) {
            Caffeine<String, Object> builder = Caffeine.<String, Object>newBuilder()
                    .maximumSize(properties.getLocalCache().getMaxSize())
                    .expireAfter(new Expiry<String, Object>() {
                        @Override
                        public long expireAfterCreate(String key, Object value, long currentTime) {
                            Long customTtl = customTtlNanos.remove(key);
                            return customTtl != null ? customTtl : defaultTtlNanos;
                        }

                        @Override
                        public long expireAfterUpdate(String key, Object value, long currentTime, long currentDuration) {
                            Long customTtl = customTtlNanos.remove(key);
                            return customTtl != null ? customTtl : currentDuration;
                        }

                        @Override
                        public long expireAfterRead(String key, Object value, long currentTime, long currentDuration) {
                            return currentDuration;
                        }
                    });

            if (recordStats) {
                builder.recordStats();
            }

            this.cache = builder.build();

            // Initialize metrics
            String prefix = properties.getMetrics().getPrefix() + ".local";
            this.hitCounter = Metrics.counter(prefix + ".hits");
            this.missCounter = Metrics.counter(prefix + ".misses");
            this.evictionCounter = Metrics.counter(prefix + ".evictions");
            this.sizeGauge = io.micrometer.core.instrument.Gauge.builder(prefix + ".size", this, LocalCacheService::getCurrentSize).register(Metrics.globalRegistry);

            log.info("Local cache enabled with maxSize={}, ttl={}, stats={}",
                    properties.getLocalCache().getMaxSize(),
                    properties.getLocalCache().getTtl(),
                    recordStats);
        } else {
            this.cache = null;
            this.hitCounter = null;
            this.missCounter = null;
            this.evictionCounter = null;
            this.sizeGauge = null;
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
                if (recordStats) {
                    hitCounter.increment();
                }
                log.debug("Local cache hit for key: {}", key);
                return type.cast(value);
            }
            if (recordStats) {
                missCounter.increment();
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
            // Store custom TTL so Expiry callback picks it up on create/update
            customTtlNanos.put(key, ttl.toNanos());
            cache.put(key, value);
            log.debug("Put key in local cache with custom TTL {}: {}", ttl, key);
        } catch (Exception e) {
            customTtlNanos.remove(key);
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
        if (!enabled || !recordStats) {
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
     * Get current size for metrics gauge.
     */
    private double getCurrentSize() {
        return enabled ? cache.estimatedSize() : 0;
    }

    /**
     * Check if local cache is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Mark Redis as unavailable (circuit breaker pattern).
     */
    public void markRedisUnavailable() {
        this.redisAvailable = false;
        this.lastRedisFailureTime = System.currentTimeMillis();
        log.warn("Redis marked as unavailable, using local cache");
    }

    /**
     * Mark Redis as available (circuit breaker recovery).
     */
    public void markRedisAvailable() {
        this.redisAvailable = true;
        log.info("Redis marked as available");
    }

    /**
     * Check if Redis is available (with cooldown).
     */
    public boolean isRedisAvailable() {
        if (!redisAvailable) {
            long timeSinceFailure = System.currentTimeMillis() - lastRedisFailureTime;
            if (timeSinceFailure > REDIS_FAILURE_COOLDOWN_MS) {
                // Attempt recovery
                log.info("Attempting Redis recovery after cooldown");
                markRedisAvailable();
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Get cache health status.
     */
    public CacheHealth getHealth() {
        if (!enabled) {
            return new CacheHealth(false, 0, 0.0, 0L);
        }

        CacheStats stats = recordStats ? cache.stats() : null;
        return new CacheHealth(
            true,
            cache.estimatedSize(),
            stats != null ? stats.hitRate() : 0.0,
            stats != null ? stats.evictionCount() : 0L
        );
    }

    /**
     * Cache health status.
     */
    public record CacheHealth(
        boolean enabled,
        long size,
        double hitRate,
        long evictionCount
    ) {}
}
