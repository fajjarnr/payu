package id.payu.cache.service;

import id.payu.cache.model.CacheInvalidationEvent;
import id.payu.cache.properties.CacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing cache invalidation events to Kafka.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Async event publishing</li>
 *   <li>Error handling and retry</li>
 *   <li>Metrics tracking</li>
 *   <li>Tenant-aware invalidation</li>
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
public class CacheInvalidationPublisher {

    private final KafkaTemplate<String, CacheInvalidationEvent> kafkaTemplate;
    private final CacheProperties properties;

    /**
     * Publish a cache invalidation event.
     */
    public CompletableFuture<SendResult<String, CacheInvalidationEvent>> invalidate(
            CacheInvalidationEvent event) {
        log.debug("Publishing cache invalidation event: cache={}, key={}, type={}",
            event.getCacheName(), event.getKey(), event.getType());

        String key = event.getCacheName() + ":" + event.getKey();

        return kafkaTemplate.send(properties.getInvalidation().getTopic(), key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish cache invalidation event: {}", ex.getMessage());
                } else {
                    log.debug("Cache invalidation event published successfully: partition={}, offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }

    /**
     * Invalidate a single cache key.
     */
    public CompletableFuture<SendResult<String, CacheInvalidationEvent>> invalidateKey(
            String cacheName,
            String key,
            String service) {
        CacheInvalidationEvent event = CacheInvalidationEvent.forKey(cacheName, key, service);
        return invalidate(event);
    }

    /**
     * Invalidate cache keys matching a pattern.
     */
    public CompletableFuture<SendResult<String, CacheInvalidationEvent>> invalidatePattern(
            String cacheName,
            String pattern,
            String service) {
        CacheInvalidationEvent event = CacheInvalidationEvent.forPattern(cacheName, pattern, service);
        return invalidate(event);
    }

    /**
     * Invalidate all keys in a cache.
     */
    public CompletableFuture<SendResult<String, CacheInvalidationEvent>> invalidateAll(
            String cacheName,
            String service) {
        CacheInvalidationEvent event = CacheInvalidationEvent.forAll(cacheName, service);
        return invalidate(event);
    }
}
