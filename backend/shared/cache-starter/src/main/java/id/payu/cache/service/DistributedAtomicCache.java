package id.payu.cache.service;

import java.time.Duration;

/**
 * Atomic distributed-cache primitives for idempotency and rate limiting.
 */
public interface DistributedAtomicCache {

    String getString(String key);

    void putString(String key, String value, Duration ttl);

    boolean putStringIfAbsent(String key, String value, Duration ttl);

    boolean replaceString(String key, String value, Duration ttl);

    long increment(String key, Duration ttl);

    long getRemainingTtlSeconds(String key);

    void evict(String key);
}
