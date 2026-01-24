package id.payu.abtesting.infrastructure.redis.cache;

import id.payu.abtesting.domain.service.ExperimentService.VariantAssignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis cache service for variant assignments
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExperimentCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String VARIANT_ASSIGNMENT_KEY_PREFIX = "ab:variant:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    /**
     * Cache variant assignment for user
     */
    public void cacheVariantAssignment(String experimentKey, UUID userId, VariantAssignment assignment) {
        String key = buildVariantKey(experimentKey, userId);
        try {
            redisTemplate.opsForValue().set(key, assignment, CACHE_TTL);
            log.debug("Cached variant assignment: {}", key);
        } catch (Exception e) {
            log.error("Failed to cache variant assignment", e);
        }
    }

    /**
     * Get cached variant assignment
     */
    public VariantAssignment getVariantAssignment(String experimentKey, UUID userId) {
        String key = buildVariantKey(experimentKey, userId);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof VariantAssignment) {
                log.debug("Cache hit for variant assignment: {}", key);
                return (VariantAssignment) cached;
            }
        } catch (Exception e) {
            log.error("Failed to get variant assignment from cache", e);
        }
        return null;
    }

    /**
     * Clear variant assignment cache
     */
    public void clearVariantAssignment(String experimentKey, UUID userId) {
        String key = buildVariantKey(experimentKey, userId);
        try {
            redisTemplate.delete(key);
            log.debug("Cleared variant assignment: {}", key);
        } catch (Exception e) {
            log.error("Failed to clear variant assignment", e);
        }
    }

    /**
     * Clear all variant assignments for an experiment
     */
    public void clearExperimentAssignments(String experimentKey) {
        try {
            var pattern = VARIANT_ASSIGNMENT_KEY_PREFIX + experimentKey + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Cleared {} assignments for experiment: {}", keys.size(), experimentKey);
            }
        } catch (Exception e) {
            log.error("Failed to clear experiment assignments", e);
        }
    }

    private String buildVariantKey(String experimentKey, UUID userId) {
        return VARIANT_ASSIGNMENT_KEY_PREFIX + experimentKey + ":" + userId;
    }
}
