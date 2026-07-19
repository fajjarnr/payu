package id.payu.commons.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.cache.service.DistributedAtomicCache;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Native distributed-cache implementation of {@link IdempotencyRepository}.
 */
@Slf4j
@RequiredArgsConstructor
public class DistributedCacheIdempotencyRepository implements IdempotencyRepository {

    private final DistributedAtomicCache distributedCache;
    private final ObjectMapper objectMapper;
    private final IdempotencyProperties properties;

    @Override
    public Optional<IdempotencyEntry> findByKey(IdempotencyKey key) {
        try {
            String json = distributedCache.getString(buildCacheKey(key));
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, IdempotencyEntry.class));
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("Failed to read idempotency entry for key '{}': {}", key.value(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean save(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
        try {
            distributedCache.putString(buildCacheKey(key), objectMapper.writeValueAsString(entry), ttl(ttlSeconds));
            return true;
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("Failed to save idempotency entry for key '{}': {}", key.value(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean saveIfAbsent(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
        try {
            return distributedCache.putStringIfAbsent(
                    buildCacheKey(key), objectMapper.writeValueAsString(entry), ttl(ttlSeconds));
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("Failed to atomically save idempotency entry for key '{}': {}", key.value(), e.getMessage());
            return false;
        }
    }

    @Override
    public void update(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
        try {
            String cacheKey = buildCacheKey(key);
            String json = objectMapper.writeValueAsString(entry);
            if (!distributedCache.replaceString(cacheKey, json, ttl(ttlSeconds))) {
                distributedCache.putString(cacheKey, json, ttl(ttlSeconds));
            }
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("Failed to update idempotency entry for key '{}': {}", key.value(), e.getMessage());
        }
    }

    @Override
    public void delete(IdempotencyKey key) {
        try {
            distributedCache.evict(buildCacheKey(key));
        } catch (RuntimeException e) {
            log.error("Failed to delete idempotency entry for key '{}': {}", key.value(), e.getMessage());
        }
    }

    @Override
    public boolean exists(IdempotencyKey key) {
        try {
            return distributedCache.getString(buildCacheKey(key)) != null;
        } catch (RuntimeException e) {
            log.error("Failed to check idempotency entry for key '{}': {}", key.value(), e.getMessage());
            return false;
        }
    }

    @Override
    public long getTtl(IdempotencyKey key) {
        try {
            return distributedCache.getRemainingTtlSeconds(buildCacheKey(key));
        } catch (RuntimeException e) {
            log.error("Failed to read idempotency TTL for key '{}': {}", key.value(), e.getMessage());
            return -1L;
        }
    }

    private Duration ttl(long ttlSeconds) {
        return Duration.ofSeconds(ttlSeconds);
    }

    private String buildCacheKey(IdempotencyKey key) {
        return properties.getRedis().getKeyPrefix() + ":" + key.value();
    }
}
