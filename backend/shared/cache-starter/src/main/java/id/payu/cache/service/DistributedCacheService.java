package id.payu.cache.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.cache.model.CacheEntry;
import id.payu.cache.properties.CacheProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed cache service using Redis with stale-while-revalidate support.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Redis-based distributed caching</li>
 *   <li>Stale-while-revalidate pattern</li>
 *   <li>Metrics tracking</li>
 *   <li>Automatic JSON serialization</li>
 *   <li>Connection failure handling</li>
 * </ul>
 */
@Slf4j
public class DistributedCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ValueOperations<String, Object> valueOps;
    private final CacheProperties properties;
    private final ObjectMapper objectMapper;

    // Metrics
    private final Counter hitCounter;
    private final Counter missCounter;
    private final Counter staleCounter;
    private final Counter errorCounter;
    private final Timer getTimer;
    private final Timer putTimer;

    public DistributedCacheService(
            RedisConnectionFactory connectionFactory,
            CacheProperties properties) {
        this.properties = properties;

        // Create ObjectMapper for JSON serialization
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModules(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // Create RedisTemplate
        this.redisTemplate = new RedisTemplate<>();
        this.redisTemplate.setConnectionFactory(connectionFactory);
        this.redisTemplate.afterPropertiesSet();
        this.valueOps = redisTemplate.opsForValue();

        // Initialize metrics
        String prefix = "cache.distributed";
        this.hitCounter = Metrics.counter(prefix + ".hits");
        this.missCounter = Metrics.counter(prefix + ".misses");
        this.staleCounter = Metrics.counter(prefix + ".stale");
        this.errorCounter = Metrics.counter(prefix + ".errors");
        this.getTimer = Metrics.timer(prefix + ".get");
        this.putTimer = Metrics.timer(prefix + ".put");

        log.info("Distributed cache service initialized");
    }

    /**
     * Get value from cache.
     */
    public <T> T get(String key, Class<T> type) {
        return get(key, type, null);
    }

    /**
     * Get value from cache with fallback supplier.
     */
    public <T> T get(String key, Class<T> type, Supplier<T> fallback) {
        Timer.Sample sample = Timer.start();

        try {
            Object value = valueOps.get(key);

            if (value == null) {
                missCounter.increment();
                log.debug("Cache miss for key: {}", key);

                if (fallback != null) {
                    T fallbackValue = fallback.get();
                    if (fallbackValue != null) {
                        put(key, fallbackValue);
                    }
                    return fallbackValue;
                }
                return null;
            }

            // Check if it's a CacheEntry (with TTL metadata)
            if (value instanceof CacheEntry) {
                CacheEntry<T> entry = (CacheEntry<T>) value;

                if (entry.isExpired()) {
                    missCounter.increment();
                    log.debug("Cache entry expired for key: {}", key);

                    if (fallback != null) {
                        T fallbackValue = fallback.get();
                        put(key, fallbackValue);
                        return fallbackValue;
                    }
                    return null;
                }

                if (entry.isStale()) {
                    staleCounter.increment();
                    log.debug("Cache entry stale for key: {}", key);
                } else {
                    hitCounter.increment();
                    log.debug("Cache hit for key: {}", key);
                }

                return entry.getValue();
            }

            // Direct value without CacheEntry wrapper
            hitCounter.increment();
            log.debug("Cache hit for key: {}", key);
            return type.cast(value);

        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error getting from cache for key {}: {}", key, e.getMessage());
            return fallback != null ? fallback.get() : null;
        } finally {
            sample.stop(getTimer);
        }
    }

    /**
     * Get value with stale-while-revalidate pattern.
     * Returns stale data immediately if available and schedules refresh.
     */
    public <T> T getWithStaleWhileRevalidate(
            String key,
            Class<T> type,
            Supplier<T> fallback,
            long softTtlSeconds,
            long hardTtlSeconds) {

        try {
            Object value = valueOps.get(key);

            if (value == null) {
                missCounter.increment();
                log.debug("Cache miss for key: {}", key);
                T fallbackValue = fallback.get();
                if (fallbackValue != null) {
                    put(key, fallbackValue, softTtlSeconds, hardTtlSeconds);
                }
                return fallbackValue;
            }

            if (value instanceof CacheEntry) {
                CacheEntry<T> entry = (CacheEntry<T>) value;

                if (entry.isExpired()) {
                    missCounter.increment();
                    log.debug("Cache entry expired for key: {}", key);
                    T fallbackValue = fallback.get();
                    put(key, fallbackValue, softTtlSeconds, hardTtlSeconds);
                    return fallbackValue;
                }

                if (entry.isStale()) {
                    staleCounter.increment();
                    log.debug("Cache entry stale for key: {}, serving stale data", key);
                    // Return stale data, caller should schedule refresh
                    return entry.getValue();
                }

                hitCounter.increment();
                log.debug("Cache hit for key: {}", key);
                return entry.getValue();
            }

            hitCounter.increment();
            return type.cast(value);

        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error in stale-while-revalidate for key {}: {}", key, e.getMessage());
            return fallback.get();
        }
    }

    /**
     * Get raw CacheEntry for advanced use cases.
     */
    public <T> CacheEntry<T> getEntry(String key, Class<T> type) {
        try {
            Object value = valueOps.get(key);
            if (value instanceof CacheEntry) {
                return (CacheEntry<T>) value;
            }
        } catch (Exception e) {
            log.error("Error getting cache entry for key {}: {}", key, e.getMessage());
        }
        return null;
    }

    /**
     * Put value in cache with default TTL.
     */
    public void put(String key, Object value) {
        Timer.Sample sample = Timer.start();

        try {
            Duration ttl = properties.getDefaultTtl();
            CacheEntry<Object> entry = CacheEntry.create(value, ttl.getSeconds());
            valueOps.set(key, entry, ttl);
            log.debug("Put key in cache: {} with TTL: {}", key, ttl);
        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error putting to cache for key {}: {}", key, e.getMessage());
        } finally {
            sample.stop(putTimer);
        }
    }

    /**
     * Put value in cache with custom TTL (hard TTL only).
     */
    public void put(String key, Object value, Duration ttl) {
        put(key, value, ttl.getSeconds(), ttl.getSeconds());
    }

    /**
     * Put value in cache with soft and hard TTL (stale-while-revalidate).
     */
    public void put(String key, Object value, long softTtlSeconds, long hardTtlSeconds) {
        Timer.Sample sample = Timer.start();

        try {
            CacheEntry<Object> entry = CacheEntry.create(value, softTtlSeconds, hardTtlSeconds);
            valueOps.set(key, entry, Duration.ofSeconds(hardTtlSeconds));
            log.debug("Put key in cache: {} with softTTL: {}s, hardTTL: {}s",
                    key, softTtlSeconds, hardTtlSeconds);
        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error putting to cache for key {}: {}", key, e.getMessage());
        } finally {
            sample.stop(putTimer);
        }
    }

    /**
     * Evict entry from cache.
     */
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Evicted key from cache: {}", key);
        } catch (Exception e) {
            log.error("Error evicting from cache for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Check if key exists in cache.
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking cache for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Get Redis template for advanced operations.
     */
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }
}
