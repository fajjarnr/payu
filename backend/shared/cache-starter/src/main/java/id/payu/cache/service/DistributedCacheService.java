package id.payu.cache.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.cache.model.CacheEntry;
import id.payu.cache.properties.CacheProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.commons.util.CloseableIterator;

import java.time.Duration;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Distributed cache service with stale-while-revalidate support.
 *
 * <p>Uses native Infinispan Hot Rod.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Data Grid distributed caching via Hot Rod protocol</li>
 *   <li>Stale-while-revalidate pattern</li>
 *   <li>Type-safe deserialization via ObjectMapper.convertValue()</li>
 *   <li>Metrics tracking with Micrometer</li>
 *   <li>Automatic JSON serialization</li>
 *   <li>Connection failure handling</li>
 * </ul>
 */
@Slf4j
public class DistributedCacheService implements DistributedCache, DistributedAtomicCache {

    private static final Set<String> SERIALIZER_METADATA_KEYS = Set.of(
        "@class",
        "_class",
        "@type",
        "_type",
        "javaClass"
    );

    private static final Set<String> SIMPLE_VALUE_WRAPPER_KEYS = Set.of(
        "value",
        "@class",
        "_class",
        "@type",
        "_type",
        "javaClass"
    );

    private final Supplier<RemoteCache<String, Object>> hotRodCacheSupplier;
    private final CacheProperties properties;
    private final ObjectMapper objectMapper;

    // Metrics
    private Counter hitCounter;
    private Counter missCounter;
    private Counter staleCounter;
    private Counter errorCounter;
    private Timer getTimer;
    private Timer putTimer;

    /**
     * Creates a native Hot Rod cache service.
     *
     * @param hotRodCache configured named remote cache
     * @param properties cache configuration properties
     */
    protected DistributedCacheService(
            Supplier<RemoteCache<String, Object>> hotRodCacheSupplier,
            CacheProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModules(
                new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule(),
                new com.fasterxml.jackson.datatype.jdk8.Jdk8Module());
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.hotRodCacheSupplier = hotRodCacheSupplier;

        initializeMetrics();
        log.info("Distributed cache service initialized with native Hot Rod backend");
    }

    private void initializeMetrics() {
        String prefix = "cache.distributed";
        this.hitCounter = Metrics.counter(prefix + ".hits");
        this.missCounter = Metrics.counter(prefix + ".misses");
        this.staleCounter = Metrics.counter(prefix + ".stale");
        this.errorCounter = Metrics.counter(prefix + ".errors");
        this.getTimer = Metrics.timer(prefix + ".get");
        this.putTimer = Metrics.timer(prefix + ".put");

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
            Object value = getRaw(key);

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
            Object value = getRaw(key);

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
            Object value = getRaw(key);
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
            putRaw(key, entry, ttl);
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
            putRaw(key, entry, Duration.ofSeconds(hardTtlSeconds));
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
            hotRodCache().remove(key);
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
            return hotRodCache().containsKey(key);
        } catch (Exception e) {
            log.error("Error checking cache for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Evict keys matching a cache invalidation glob.
     */
    public void evictMatching(String pattern) {
        Pattern matcher = Pattern.compile(Pattern.quote(pattern).replace("*", "\\E.*\\Q"));
        RemoteCache<String, Object> cache = hotRodCache();
        try (CloseableIterator<String> keys = cache.keySet().iterator()) {
            while (keys.hasNext()) {
                String key = keys.next();
                if (matcher.matcher(key).matches()) {
                    cache.remove(key);
                }
            }
        }
    }

    @Override
    public String getString(String key) {
        Object value = hotRodCache().get(key);
        return value == null ? null : value.toString();
    }

    @Override
    public void putString(String key, String value, Duration ttl) {
        hotRodCache().put(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean putStringIfAbsent(String key, String value, Duration ttl) {
        return hotRodCache().putIfAbsent(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS) == null;
    }

    @Override
    public boolean replaceString(String key, String value, Duration ttl) {
        MetadataValue<Object> current = hotRodCache().getWithMetadata(key);
        return current != null && hotRodCache().replaceWithVersion(
                key, value, current.getVersion(), ttl.toMillis(), TimeUnit.MILLISECONDS, -1, TimeUnit.MILLISECONDS);
    }

    @Override
    public long increment(String key, Duration ttl) {
        RemoteCache<String, Object> cache = hotRodCache();
        for (int attempt = 0; attempt < 8; attempt++) {
            MetadataValue<Object> current = cache.getWithMetadata(key);
            if (current == null) {
                if (cache.putIfAbsent(key, "1", ttl.toMillis(), TimeUnit.MILLISECONDS) == null) {
                    return 1L;
                }
                continue;
            }

            long next = Long.parseLong(current.getValue().toString()) + 1;
            if (cache.replaceWithVersion(key, Long.toString(next), current.getVersion())) {
                return next;
            }
        }
        throw new IllegalStateException("Could not atomically increment cache key: " + key);
    }

    @Override
    public long getRemainingTtlSeconds(String key) {
        MetadataValue<Object> metadata = hotRodCache().getWithMetadata(key);
        if (metadata == null || metadata.getLifespan() <= 0) {
            return -1L;
        }
        long expiresAtMillis = metadata.getCreated() + TimeUnit.SECONDS.toMillis(metadata.getLifespan());
        return Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(expiresAtMillis - System.currentTimeMillis()));
    }

    private Object getRaw(String key) throws IOException {
        Object value = hotRodCache().get(key);
        return value instanceof String json
                ? objectMapper.readValue(json, Object.class)
                : value;
    }

    private void putRaw(String key, Object value, Duration ttl) throws com.fasterxml.jackson.core.JsonProcessingException {
        hotRodCache().put(key, objectMapper.writeValueAsString(value), ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    private RemoteCache<String, Object> hotRodCache() {
        return hotRodCacheSupplier.get();
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
                Map<String, Object> map = sanitizeMap((Map<?, ?>) value);
                // Check if map looks like a CacheEntry (has value, createdAt, softTtl, hardTtl)
                if (map.containsKey("value") && map.containsKey("createdAt")) {
                    // Convert the entire map to CacheEntry using ObjectMapper
                    JavaType cacheEntryType = objectMapper.getTypeFactory()
                            .constructParametricType(CacheEntry.class, innerType);
                    CacheEntry<T> entry = objectMapper.convertValue(map, cacheEntryType);
                    Object innerValue = entry.getValue();
                    if (innerValue != null && !innerType.isInstance(innerValue)) {
                        T converted = convertToType(innerValue, innerType);
                        return CacheEntry.create(converted, entry.getSoftTtl(), entry.getHardTtl(), entry.getCreatedAt());
                    }
                    return entry;
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

        Object sanitizedValue = sanitizeSerializedValue(value);
        Object unwrappedValue = unwrapSimpleValueWrapper(sanitizedValue);

        if (type.isInstance(unwrappedValue)) {
            return type.cast(unwrappedValue);
        }

        // Convert via ObjectMapper (handles LinkedHashMap → POJO, number conversions, etc.)
        try {
            return objectMapper.convertValue(unwrappedValue, type);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to convert cached value to type {}: {}", type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private Object sanitizeSerializedValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            return sanitizeMap(mapValue);
        }
        return value;
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> rawMap) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String key) || SERIALIZER_METADATA_KEYS.contains(key)) {
                continue;
            }
            sanitized.put(key, sanitizeSerializedValue(entry.getValue()));
        }
        return sanitized;
    }

    private Object unwrapSimpleValueWrapper(Object value) {
        if (!(value instanceof Map<?, ?> rawMap) || !rawMap.containsKey("value")) {
            return value;
        }

        boolean cacheEntryLike = rawMap.containsKey("createdAt")
                || rawMap.containsKey("softTtl")
                || rawMap.containsKey("hardTtl")
                || rawMap.containsKey("version");
        if (cacheEntryLike) {
            return value;
        }

        for (Object rawKey : rawMap.keySet()) {
            if (!(rawKey instanceof String key) || !SIMPLE_VALUE_WRAPPER_KEYS.contains(key)) {
                return value;
            }
        }

        return sanitizeSerializedValue(rawMap.get("value"));
    }
}
