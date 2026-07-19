package id.payu.cache.service;

import id.payu.cache.model.CacheInvalidationEvent;
import id.payu.cache.properties.CacheProperties;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


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

    private final io.micrometer.core.instrument.Counter processedCounter = Metrics.counter("cache.invalidation.processed");
    private final io.micrometer.core.instrument.Counter failedCounter = Metrics.counter("cache.invalidation.failed");

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

            processedCounter.increment();
        } catch (Exception e) {
            log.error("Error processing cache invalidation event: {}", e.getMessage(), e);
            failedCounter.increment();
        }
    }

    /**
     * Invalidate a single cache key.
     */
    private void invalidateKey(CacheInvalidationEvent event) {
        String key = buildCacheKey(event.getCacheName(), event.getKey());
        cacheService.invalidate(key);
        log.debug("Invalidated cache key: {}", key);
    }

    /**
     * Invalidate cache keys matching a pattern.
     */
    private void invalidatePattern(CacheInvalidationEvent event) {
        String pattern = buildCacheKey(event.getCacheName(), event.getKey());

        cacheService.getDistributedCache().evictMatching(pattern);
        log.debug("Invalidated keys matching pattern: {}", pattern);
    }

    /**
     * Invalidate all keys in a cache.
     */
    private void invalidateAll(CacheInvalidationEvent event) {
        String pattern = buildCacheKey(event.getCacheName(), "*");

        cacheService.getDistributedCache().evictMatching(pattern);
        log.debug("Invalidated all keys in cache: {}", event.getCacheName());
    }

    /**
     * Build a full cache key with cache name prefix.
     */
    private String buildCacheKey(String cacheName, String key) {
        return cacheName + "::" + key;
    }
}
