package id.payu.commons.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of {@link IdempotencyRepository}.
 * <p>
 * This repository uses Redis for storing idempotency entries with the following features:
 * <ul>
 *   <li>JSON serialization for entry storage</li>
 *   <li>TTL-based automatic expiration</li>
 *   <li>Atomic check-and-set operations using Lua scripts</li>
 *   <li>Thread-safe operations via Redis single-threaded nature</li>
 * </ul>
 * <p>
 * Redis Key Structure:
 * <pre>
 * idempotency:&lt;key&gt;           - Main entry storage (JSON)
 * idempotency:&lt;key&gt;:fingerprint - Request fingerprint for validation
 * </pre>
 *
 * @see IdempotencyRepository
 * @see IdempotencyEntry
 */
@Slf4j
@RequiredArgsConstructor
public class RedisIdempotencyRepository implements IdempotencyRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final IdempotencyProperties properties;

    // Lua script for atomic check-and-set
    private static final String SAVE_IF_ABSENT_SCRIPT =
            "if redis.call('exists', KEYS[1]) == 0 then " +
            "    redis.call('setex', KEYS[1], ARGV[2], ARGV[1]) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    private final RedisScript<Long> saveIfAbsentScript = new DefaultRedisScript<>(
            SAVE_IF_ABSENT_SCRIPT, Long.class
    );

    @Override
    public Optional<IdempotencyEntry> findByKey(IdempotencyKey key) {
        try {
            String cacheKey = buildCacheKey(key);
            String json = redisTemplate.opsForValue().get(cacheKey);

            if (json == null) {
                return Optional.empty();
            }

            IdempotencyEntry entry = objectMapper.readValue(json, IdempotencyEntry.class);
            return Optional.of(entry);

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize idempotency entry for key '{}': {}",
                    key.value(), e.getMessage());
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Redis error while finding idempotency entry for key '{}': {}",
                    key.value(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean save(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
        try {
            String cacheKey = buildCacheKey(key);
            String json = objectMapper.writeValueAsString(entry);

            redisTemplate.opsForValue().set(cacheKey, json, ttlSeconds, TimeUnit.SECONDS);

            log.debug("Saved idempotency entry for key '{}', ttl: {}s", key.value(), ttlSeconds);
            return true;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency entry for key '{}': {}",
                    key.value(), e.getMessage());
            return false;
        } catch (DataAccessException e) {
            log.error("Redis error while saving idempotency entry for key '{}': {}",
                    key.value(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean saveIfAbsent(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
        try {
            String cacheKey = buildCacheKey(key);
            String json = objectMapper.writeValueAsString(entry);

            // Use Lua script for atomic check-and-set
            Long result = redisTemplate.execute(
                    saveIfAbsentScript,
                    Collections.singletonList(cacheKey),
                    json,
                    String.valueOf(ttlSeconds)
            );

            boolean saved = result != null && result == 1;

            if (saved) {
                log.debug("Atomically saved idempotency entry for key '{}', ttl: {}s",
                        key.value(), ttlSeconds);
            } else {
                log.debug("Idempotency entry already exists for key '{}'", key.value());
            }

            return saved;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency entry for key '{}': {}",
                    key.value(), e.getMessage());
            return false;
        } catch (DataAccessException e) {
            log.error("Redis error while saving idempotency entry for key '{}': {}",
                    key.value(), e.getMessage());
            return false;
        }
    }

    @Override
    public void update(IdempotencyKey key, IdempotencyEntry entry, long ttlSeconds) {
        try {
            String cacheKey = buildCacheKey(key);
            String json = objectMapper.writeValueAsString(entry);

            // Use SET with XX option to only update if exists, preserving TTL if possible
            Boolean updated = redisTemplate.opsForValue().setIfPresent(cacheKey, json);

            if (Boolean.TRUE.equals(updated)) {
                // Restore TTL after update
                redisTemplate.expire(cacheKey, ttlSeconds, TimeUnit.SECONDS);
                log.debug("Updated idempotency entry for key '{}', ttl: {}s", key.value(), ttlSeconds);
            } else {
                // Entry doesn't exist, create it
                redisTemplate.opsForValue().set(cacheKey, json, ttlSeconds, TimeUnit.SECONDS);
                log.debug("Created idempotency entry during update for key '{}'", key.value());
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency entry for key '{}': {}",
                    key.value(), e.getMessage());
        } catch (DataAccessException e) {
            log.error("Redis error while updating idempotency entry for key '{}': {}",
                    key.value(), e.getMessage());
        }
    }

    @Override
    public void delete(IdempotencyKey key) {
        try {
            String cacheKey = buildCacheKey(key);
            redisTemplate.delete(cacheKey);
            log.debug("Deleted idempotency entry for key '{}'", key.value());

        } catch (DataAccessException e) {
            log.error("Redis error while deleting idempotency entry for key '{}': {}",
                    key.value(), e.getMessage());
        }
    }

    @Override
    public boolean exists(IdempotencyKey key) {
        try {
            String cacheKey = buildCacheKey(key);
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));

        } catch (DataAccessException e) {
            log.error("Redis error while checking existence for key '{}': {}",
                    key.value(), e.getMessage());
            return false;
        }
    }

    @Override
    public long getTtl(IdempotencyKey key) {
        try {
            String cacheKey = buildCacheKey(key);
            Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;

        } catch (DataAccessException e) {
            log.error("Redis error while getting TTL for key '{}': {}",
                    key.value(), e.getMessage());
            return -1;
        }
    }

    /**
     * Builds the Redis cache key for an idempotency key.
     *
     * @param key the idempotency key
     * @return the Redis cache key
     */
    private String buildCacheKey(IdempotencyKey key) {
        return properties.getRedis().getKeyPrefix() + ":" + key.value();
    }
}
