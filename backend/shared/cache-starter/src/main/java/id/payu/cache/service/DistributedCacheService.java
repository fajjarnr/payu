package id.payu.cache.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.cache.model.CacheEntry;
import id.payu.cache.properties.CacheProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed cache service using Redis/Red Hat Data Grid with stale-while-revalidate support.
 *
 * <p>Compatible with both Redis and Red Hat Data Grid (Infinispan) in RESP protocol mode.
 * Uses Lettuce client with JSON serialization for portable, cross-platform caching.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Redis/Data Grid distributed caching via RESP protocol</li>
 *   <li>Stale-while-revalidate pattern</li>
 *   <li>Type-safe deserialization via ObjectMapper.convertValue()</li>
 *   <li>Metrics tracking with Micrometer</li>
 *   <li>Automatic JSON serialization (GenericJackson2JsonRedisSerializer)</li>
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

    /**
     * Creates DistributedCacheService with a pre-configured RedisTemplate.
     * The template should use GenericJackson2JsonRedisSerializer for DataGrid/Redis compatibility.
     *
     * @param redisTemplate pre-configured template (from RedisCacheConfig)
     * @param properties    cache configuration properties
     */
    public DistributedCacheService(
            RedisTemplate<String, Object> redisTemplate,
            CacheProperties properties) {
        this.properties = properties;

        // ObjectMapper for type-safe deserialization (BUG-BE-074 fix)
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModules(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // Use the pre-configured RedisTemplate with JSON serializers
        // This ensures consistency with RedisCacheConfig and DataGrid compatibility
        this.redisTemplate = redisTemplate;
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
     * BUG-BE-074 FIX: Uses ObjectMapper.convertValue() for type-safe deserialization
     * instead of unsafe casts. GenericJackson2JsonRedisSerializer deserializes to
     * LinkedHashMap — convertValue properly converts to target type.
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

            // BUG-BE-074: Type-safe deserialization via ObjectMapper
            // GenericJackson2JsonRedisSerializer may return LinkedHashMap instead of CacheEntry
            CacheEntry<T> entry = convertToCacheEntry(value, type);

            if (entry != null) {
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

            // Direct value without CacheEntry wrapper — convert safely
            hitCounter.increment();
            log.debug("Cache hit for key: {}", key);
            return convertToType(value, type);

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

            // BUG-BE-074: Type-safe conversion
            CacheEntry<T> entry = convertToCacheEntry(value, type);

            if (entry != null) {
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
            return convertToType(value, type);

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
            // BUG-BE-074: Type-safe conversion from deserialized JSON
            return convertToCacheEntry(value, type);
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

    // --- BUG-BE-074: Type-safe conversion helpers ---

    /**
     * Safely convert a deserialized value to CacheEntry.
     * GenericJackson2JsonRedisSerializer may deserialize CacheEntry as LinkedHashMap.
     * This method handles both cases for Redis and Red Hat Data Grid compatibility.
     *
     * @param value the raw deserialized value from Redis/DataGrid
     * @param innerType the expected type of CacheEntry.value
     * @return CacheEntry if convertible, null otherwise
     */
    @SuppressWarnings("unchecked")
    private <T> CacheEntry<T> convertToCacheEntry(Object value, Class<T> innerType) {
        if (value == null) {
            return null;
        }

        // Case 1: Already a CacheEntry (JDK serialization or properly typed)
        if (value instanceof CacheEntry) {
            CacheEntry<T> entry = (CacheEntry<T>) value;
            // Ensure inner value is properly typed
            Object innerValue = entry.getValue();
            if (innerValue != null && !innerType.isInstance(innerValue)) {
                T converted = convertToType(innerValue, innerType);
                return CacheEntry.create(converted, entry.getSoftTtl(), entry.getHardTtl(), entry.getCreatedAt());
            }
            return entry;
        }

        // Case 2: LinkedHashMap from GenericJackson2JsonRedisSerializer
        if (value instanceof Map) {
            try {
                Map<String, Object> map = (Map<String, Object>) value;
                // Check if map looks like a CacheEntry (has value, createdAt, softTtl, hardTtl)
                if (map.containsKey("value") && map.containsKey("createdAt")) {
                    // Convert the entire map to CacheEntry using ObjectMapper
                    JavaType cacheEntryType = objectMapper.getTypeFactory()
                            .constructParametricType(CacheEntry.class, innerType);
                    return objectMapper.convertValue(value, cacheEntryType);
                }
            } catch (Exception e) {
                log.debug("Value is not a CacheEntry map for type {}: {}", innerType.getSimpleName(), e.getMessage());
            }
        }

        return null;
    }

    /**
     * Safely convert a deserialized value to the target type.
     * Handles LinkedHashMap from JSON serialization and direct type matches.
     *
     * @param value the raw deserialized value
     * @param type  the target type
     * @return converted value
     */
    private <T> T convertToType(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }

        // Direct type match — no conversion needed
        if (type.isInstance(value)) {
            return type.cast(value);
        }

        // Convert via ObjectMapper (handles LinkedHashMap → POJO, number conversions, etc.)
        try {
            return objectMapper.convertValue(value, type);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to convert cached value to type {}: {}", type.getSimpleName(), e.getMessage());
            return null;
        }
    }
}
