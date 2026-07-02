package id.payu.cache.service;

import id.payu.cache.model.CacheInvalidationEvent;
import id.payu.cache.properties.CacheProperties;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for publishing cache invalidation events to Kafka via Outbox.
 *
 * <p>AUDIT-050: Fix CacheInvalidationPublisher to route invalidation events via Outbox
 * to enforce transactional reliability and avoid direct KafkaTemplate dependency.</p>
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

    private final OutboxService outboxService;
    private final CacheProperties properties;

    /**
     * Publish a cache invalidation event.
     */
    public void invalidate(CacheInvalidationEvent event) {
        log.debug("Publishing cache invalidation event: cache={}, key={}, type={}",
            event.getCacheName(), event.getKey(), event.getType());

        Map<String, Object> payload = new HashMap<>();
        payload.put("key", event.getKey());
        payload.put("cacheName", event.getCacheName());
        payload.put("service", event.getService());
        payload.put("tenantId", event.getTenantId());
        payload.put("type", event.getType() != null ? event.getType().name() : null);
        payload.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : null);
        payload.put("correlationId", event.getCorrelationId());

        String key = event.getCacheName() + ":" + (event.getKey() != null ? event.getKey() : "all");

        outboxService.createEvent(
            "CacheInvalidation",
            key,
            "CacheInvalidated",
            payload,
            null,
            properties.getInvalidation().getTopic()
        );
    }

    /**
     * Invalidate a single cache key.
     */
    public void invalidateKey(
            String cacheName,
            String key,
            String service) {
        CacheInvalidationEvent event = CacheInvalidationEvent.forKey(cacheName, key, service);
        invalidate(event);
    }

    /**
     * Invalidate cache keys matching a pattern.
     */
    public void invalidatePattern(
            String cacheName,
            String pattern,
            String service) {
        CacheInvalidationEvent event = CacheInvalidationEvent.forPattern(cacheName, pattern, service);
        invalidate(event);
    }

    /**
     * Invalidate all keys in a cache.
     */
    public void invalidateAll(
            String cacheName,
            String service) {
        CacheInvalidationEvent event = CacheInvalidationEvent.forAll(cacheName, service);
        invalidate(event);
    }
}
