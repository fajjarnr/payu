package id.payu.cache.service;

import id.payu.cache.model.CacheInvalidationEvent;
import id.payu.cache.properties.CacheProperties;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumer for cache invalidation events from Kafka.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Kafka-based event consumption</li>
 *   <li>Pattern-based invalidation support</li>
 *   <li>Service-level isolation</li>
 *   <li>Metrics tracking</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "payu.cache.invalidation",
    name = "enabled",
    havingValue = "true"
)
public class CacheInvalidationConsumer {

    private final CacheService cacheService;
    private final CacheProperties properties;

    private final AtomicLong processedCounter = Metrics.counter("cache.invalidation.processed");
    private final AtomicLong failedCounter = Metrics.counter("cache.invalidation.failed");

    /**
     * Listen for cache invalidation events.
     */
    @KafkaListener(
        topics = "${payu.cache.invalidation.topic:cache-invalidation}",
        groupId = "${payu.cache.invalidation.consumer-group:cache-invalidation-group}",
        properties = {
            "spring.json.value.default.type=id.payu.cache.model.CacheInvalidationEvent"
        }
    )
    public void handleInvalidation(CacheInvalidationEvent event) {
        log.debug("Received cache invalidation event: cache={}, key={}, type={}, service={}",
            event.getCacheName(), event.getKey(), event.getType(), event.getService());

        try {
            // Skip events from the same service (optional, for cross-service invalidation)
            // Uncomment if you want to skip self-originated events
            // if (event.getService().equals(properties.getServiceName())) {
            //     log.debug("Skipping self-originated invalidation event");
            //     return;
            // }

            switch (event.getType()) {
                case KEY:
                    invalidateKey(event);
                    break;
                case PATTERN:
                    invalidatePattern(event);
                    break;
                case ALL:
                    invalidateAll(event);
                    break;
            }

            processedCounter.incrementAndGet();
        } catch (Exception e) {
            log.error("Error processing cache invalidation event: {}", e.getMessage(), e);
            failedCounter.incrementAndGet();
        }
    }

    /**
     * Invalidate a single cache key.
     */
    private void invalidateKey(CacheInvalidationEvent event) {
        String key = buildCacheKey(event.getCacheName(), event.getKey());
        cacheService.evict(key);
        log.debug("Invalidated cache key: {}", key);
    }

    /**
     * Invalidate cache keys matching a pattern.
     */
    private void invalidatePattern(CacheInvalidationEvent event) {
        String pattern = buildCacheKey(event.getCacheName(), event.getKey());

        // Use Redis SCAN to find matching keys
        var redisTemplate = cacheService.getDistributedCache().getRedisTemplate();
        Set<String> keys = redisTemplate.keys(pattern.replace("*", ".*"));

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Invalidated {} keys matching pattern: {}", keys.size(), pattern);
        }
    }

    /**
     * Invalidate all keys in a cache.
     */
    private void invalidateAll(CacheInvalidationEvent event) {
        String pattern = buildCacheKey(event.getCacheName(), "*");

        var redisTemplate = cacheService.getDistributedCache().getRedisTemplate();
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Invalidated {} keys in cache: {}", keys.size(), event.getCacheName());
        }
    }

    /**
     * Build a full cache key with cache name prefix.
     */
    private String buildCacheKey(String cacheName, String key) {
        return cacheName + "::" + key;
    }
}
