package id.payu.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event model for cache invalidation via Kafka.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Cache key and pattern support</li>
 *   <li>Service and tenant isolation</li>
 *   <li>Timestamp tracking</li>
 *   <li>Invalidation type (single key, pattern, all)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidationEvent {

    /**
     * Cache key to invalidate.
     */
    private String key;

    /**
     * Cache name/region.
     */
    private String cacheName;

    /**
     * Service that initiated the invalidation.
     */
    private String service;

    /**
     * Tenant ID for multi-tenant applications.
     */
    private String tenantId;

    /**
     * Invalidation type: KEY, PATTERN, ALL.
     */
    private InvalidationType type;

    /**
     * Event timestamp.
     */
    private Instant timestamp;

    /**
     * Optional correlation ID for tracing.
     */
    private String correlationId;

    /**
     * Invalidation types.
     */
    public enum InvalidationType {
        /**
         * Invalidate a single cache key.
         */
        KEY,

        /**
         * Invalidate keys matching a pattern.
         */
        PATTERN,

        /**
         * Invalidate all keys in a cache.
         */
        ALL
    }

    /**
     * Create a single key invalidation event.
     */
    public static CacheInvalidationEvent forKey(String cacheName, String key, String service) {
        return CacheInvalidationEvent.builder()
            .cacheName(cacheName)
            .key(key)
            .service(service)
            .type(InvalidationType.KEY)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create a pattern-based invalidation event.
     */
    public static CacheInvalidationEvent forPattern(String cacheName, String pattern, String service) {
        return CacheInvalidationEvent.builder()
            .cacheName(cacheName)
            .key(pattern)
            .service(service)
            .type(InvalidationType.PATTERN)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create an invalidate-all event.
     */
    public static CacheInvalidationEvent forAll(String cacheName, String service) {
        return CacheInvalidationEvent.builder()
            .cacheName(cacheName)
            .service(service)
            .type(InvalidationType.ALL)
            .timestamp(Instant.now())
            .build();
    }
}
