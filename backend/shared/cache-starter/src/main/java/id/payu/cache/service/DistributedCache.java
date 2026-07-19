package id.payu.cache.service;

import id.payu.cache.model.CacheEntry;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Port for distributed cache operations used by application services.
 */
public interface DistributedCache {

    <T> T get(String key, Class<T> type);

    <T> T get(String key, Class<T> type, Supplier<T> fallback);

    <T> T getWithStaleWhileRevalidate(
            String key, Class<T> type, Supplier<T> fallback, long softTtlSeconds, long hardTtlSeconds);

    <T> CacheEntry<T> getEntry(String key, Class<T> type);

    void put(String key, Object value);

    void put(String key, Object value, Duration ttl);

    void put(String key, Object value, long softTtlSeconds, long hardTtlSeconds);

    void evict(String key);

    boolean exists(String key);

    void evictMatching(String pattern);
}
