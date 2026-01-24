package id.payu.cache.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Wrapper for cache entries with metadata for stale-while-revalidate.
 *
 * <p>Each cache entry contains:</p>
 * <ul>
 *   <li>value - The actual cached data</li>
 *   <li>createdAt - When the entry was created</li>
 *   <li>softTtl - When the entry becomes stale (soft TTL)</li>
 *   <li>hardTtl - When the entry must be refreshed (hard TTL)</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheEntry<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The cached value.
     */
    private T value;

    /**
     * Timestamp when the entry was created.
     */
    private Instant createdAt;

    /**
     * Soft TTL timestamp - after this, entry is considered stale
     * but still served while async refresh happens.
     */
    private Instant softTtl;

    /**
     * Hard TTL timestamp - after this, entry must be refreshed
     * before serving.
     */
    private Instant hardTtl;

    /**
     * Version for optimistic locking.
     */
    private long version;

    @JsonCreator
    public CacheEntry(
            @JsonProperty("value") T value,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("softTtl") Instant softTtl,
            @JsonProperty("hardTtl") Instant hardTtl,
            @JsonProperty("version") long version) {
        this.value = value;
        this.createdAt = createdAt;
        this.softTtl = softTtl;
        this.hardTtl = hardTtl;
        this.version = version;
    }

    /**
     * Check if the entry is stale (past soft TTL).
     */
    public boolean isStale() {
        return Instant.now().isAfter(softTtl);
    }

    /**
     * Check if the entry is expired (past hard TTL).
     */
    public boolean isExpired() {
        return Instant.now().isAfter(hardTtl);
    }

    /**
     * Check if the entry is fresh (not stale).
     */
    public boolean isFresh() {
        return !isStale();
    }

    /**
     * Create a new cache entry with the given TTL settings.
     */
    public static <T> CacheEntry<T> create(T value, long softTtlSeconds, long hardTtlSeconds) {
        Instant now = Instant.now();
        return new CacheEntry<>(
                value,
                now,
                now.plusSeconds(softTtlSeconds),
                now.plusSeconds(hardTtlSeconds),
                0
        );
    }

    /**
     * Create a simple cache entry without stale-while-revalidate.
     */
    public static <T> CacheEntry<T> create(T value, long ttlSeconds) {
        return create(value, ttlSeconds, ttlSeconds);
    }
}
