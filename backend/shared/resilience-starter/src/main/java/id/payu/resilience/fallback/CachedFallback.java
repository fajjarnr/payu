package id.payu.resilience.fallback;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A fallback implementation that returns cached/stale data when the primary
 * service is unavailable. Useful for read-heavy operations where stale data
 * is acceptable.
 *
 * <p>The cache supports TTL (time-to-live) and automatic refresh.
 *
 * <p>Usage example:
 * <pre>
 * CachedFallback<UserProfile> cachedFallback = CachedFallback.builder()
 *     .withSupplier(() -> userService.getProfile(userId))
 *     .withTtl(Duration.ofMinutes(5))
 *     .build();
 *
 * @CircuitBreaker(name = "user-service", fallbackMethod = "getProfileFallback")
 * public UserProfile getProfile(String userId) {
 *     return userService.getProfile(userId);
 * }
 *
 * public UserProfile getProfileFallback(String userId, Exception ex) {
 *     return cachedFallback.provide(ex);
 * }
 * </pre>
 *
 * @param <T> the type of the cached value
 */
@Slf4j
public class CachedFallback<T> implements FallbackProvider<T> {

    private final AtomicReference<CacheEntry<T>> cache;
    private final Supplier<T> supplier;
    private final Duration ttl;
    private final T defaultValue;

    private CachedFallback(Builder<T> builder) {
        this.supplier = builder.supplier;
        this.ttl = builder.ttl;
        this.defaultValue = builder.defaultValue;
        this.cache = new AtomicReference<>();
    }

    /**
     * Get the cached value or default if cache is empty/expired.
     *
     * @param exception the exception that triggered the fallback (ignored)
     * @return the cached value or default
     */
    @Override
    public T provide(Exception exception) {
        CacheEntry<T> entry = cache.get();

        if (entry != null && !entry.isExpired()) {
            log.debug("Returning cached fallback value, age: {}ms",
                    Duration.between(entry.timestamp, Instant.now()).toMillis());
            return entry.value;
        }

        if (defaultValue != null) {
            log.debug("Cache miss or expired, returning default value");
            return defaultValue;
        }

        log.warn("Cache miss and no default value configured");
        throw new IllegalStateException("No cached value available and no default configured", exception);
    }

    /**
     * Refresh the cache with a new value from the supplier.
     * Should be called periodically to keep the cache fresh.
     *
     * @return the new cached value
     */
    public T refresh() {
        log.debug("Refreshing cached fallback value");
        // BUG-BE-104: Wrap supplier.get() in try-catch to retain stale value on refresh failure
        try {
            T value = supplier.get();
            cache.set(new CacheEntry<>(value, Instant.now(), ttl));
            return value;
        } catch (Exception e) {
            log.warn("Failed to refresh cached fallback value, retaining stale data", e);
            CacheEntry<T> entry = cache.get();
            if (entry != null) {
                return entry.value;
            }
            throw new IllegalStateException("Cache refresh failed and no stale value available", e);
        }
    }

    /**
     * Check if the cache has a valid (non-expired) entry.
     *
     * @return true if cache is valid
     */
    public boolean isValid() {
        CacheEntry<T> entry = cache.get();
        return entry != null && !entry.isExpired();
    }

    /**
     * Invalidate the current cache entry.
     */
    public void invalidate() {
        cache.set(null);
        log.debug("Cache invalidated");
    }

    /**
     * Get the current cached value without checking expiration.
     *
     * @return the cached value or null
     */
    public T getCachedValue() {
        CacheEntry<T> entry = cache.get();
        return entry != null ? entry.value : null;
    }

    /**
     * Create a new builder for CachedFallback.
     *
     * @param <T> the type of the cached value
     * @return a new builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builder for CachedFallback.
     *
     * @param <T> the type of the cached value
     */
    public static class Builder<T> {
        private Supplier<T> supplier;
        private Duration ttl = Duration.ofMinutes(5);
        private T defaultValue;

        /**
         * Set the supplier for refreshing the cache.
         *
         * @param supplier the supplier
         * @return this builder
         */
        public Builder<T> withSupplier(Supplier<T> supplier) {
            this.supplier = supplier;
            return this;
        }

        /**
         * Set the time-to-live for cache entries.
         *
         * @param ttl the TTL duration
         * @return this builder
         */
        public Builder<T> withTtl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        /**
         * Set the default value to return when cache is empty.
         *
         * @param defaultValue the default value
         * @return this builder
         */
        public Builder<T> withDefault(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Build the CachedFallback instance.
         *
         * @return the configured CachedFallback
         */
        public CachedFallback<T> build() {
            if (supplier == null && defaultValue == null) {
                throw new IllegalStateException("Either supplier or defaultValue must be provided");
            }
            return new CachedFallback<>(this);
        }
    }

    /**
     * Internal cache entry with timestamp.
     * BUG-BE-103: Made static to prevent implicit outer class reference causing memory leak.
     */
    private static class CacheEntry<V> {
        final V value;
        final Instant timestamp;
        final Duration entryTtl;

        CacheEntry(V value, Instant timestamp, Duration ttl) {
            this.value = value;
            this.timestamp = timestamp;
            this.entryTtl = ttl;
        }

        boolean isExpired() {
            return Duration.between(timestamp, Instant.now()).compareTo(entryTtl) > 0;
        }
    }
}
