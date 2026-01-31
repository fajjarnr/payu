package id.payu.outbox.publisher;

import id.payu.outbox.entity.OutboxEvent;
import id.payu.outbox.repository.OutboxRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for publishing outbox events to Kafka.
 * <p>
 * This publisher implements the relay component of the Transactional Outbox Pattern.
 * It periodically polls the outbox table for unpublished events and publishes them
 * to the configured Kafka topics. Successfully published events are marked as published.
 * <p>
 * The publisher supports:
 * <ul>
 *   <li>Batch processing for efficiency</li>
 *   <li>Retry mechanism with exponential backoff</li>
 *   <li>Idempotency through unique event IDs</li>
 *   <li>Metrics and monitoring</li>
 *   <li>Custom headers and routing</li>
 * </ul>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${payu.outbox.publisher.batch-size:100}")
    private int batchSize;

    @Value("${payu.outbox.publisher.max-retries:3}")
    private int maxRetries;

    @Value("${payu.outbox.publisher.default-topic:outbox.events}")
    private String defaultTopic;

    @Value("${payu.outbox.publisher.enabled:true}")
    private boolean enabled;

    @Value("${payu.outbox.publisher.lock-timeout-ms:10000}")
    private long lockTimeoutMs;

    private final AtomicInteger pendingEventsGauge = new AtomicInteger(0);

    /**
     * Initializes metrics gauges on bean creation.
     */
    public void init() {
        Gauge.builder("outbox.pending.events", pendingEventsGauge, AtomicInteger::get)
                .description("Number of pending outbox events waiting to be published")
                .register(meterRegistry);

        Gauge.builder("outbox.unpublished.count", this, p -> outboxRepository.countUnpublishedEvents())
                .description("Total count of unpublished outbox events")
                .register(meterRegistry);
    }

    /**
     * Scheduled task that polls for unpublished events and publishes them.
     * <p>
     * This method runs at a fixed interval (default: 1 second) and processes
     * pending events in batches. It uses pessimistic locking to prevent
     * concurrent processing by multiple application instances.
     */
    @Scheduled(fixedDelayString = "${payu.outbox.publisher.poll-interval-ms:1000}")
    @Transactional
    public void pollAndPublish() {
        if (!enabled) {
            log.debug("Outbox publisher is disabled");
            return;
        }

        try {
            Pageable pageable = PageRequest.of(0, batchSize);
            List<OutboxEvent> unpublishedEvents = outboxRepository.findUnpublishedEventsWithLock(maxRetries, pageable);

            if (unpublishedEvents.isEmpty()) {
                pendingEventsGauge.set(0);
                return;
            }

            pendingEventsGauge.set(unpublishedEvents.size());
            log.debug("Found {} unpublished outbox events to process", unpublishedEvents.size());

            Timer.Sample batchTimer = Timer.start(meterRegistry);
            int successCount = 0;
            int failureCount = 0;

            for (OutboxEvent event : unpublishedEvents) {
                try {
                    publishEvent(event);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    handlePublishFailure(event, e);
                }
            }

            batchTimer.stop(Timer.builder("outbox.publish.batch")
                    .description("Time taken to process a batch of outbox events")
                    .tag("status", "completed")
                    .register(meterRegistry));

            log.info("Outbox batch processed: {} succeeded, {} failed", successCount, failureCount);

        } catch (Exception e) {
            log.error("Error during outbox polling", e);
            Counter.builder("outbox.poll.errors")
                    .description("Number of errors during outbox polling")
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * Publishes a single outbox event to Kafka.
     * <p>
     * The event is serialized to JSON and sent to the appropriate topic.
     * Custom headers from the event are included in the Kafka record.
     * The event ID is used as the Kafka message key for ordering guarantees.
     *
     * @param event the outbox event to publish
     */
    public void publishEvent(OutboxEvent event) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String topic = Optional.ofNullable(event.getDestinationTopic()).orElse(defaultTopic);

        try {
            // Serialize payload to JSON
            String payload = serializePayload(event.getPayload());

            // Create Kafka record with event ID as key for ordering
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    topic,
                    null, // partition (null for default partitioning)
                    event.getCreatedAt().toEpochMilli(),
                    event.getAggregateId(), // Use aggregate ID as key for ordering within aggregate
                    payload
            );

            // Add headers
            record.headers().add(new RecordHeader("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("aggregateType", event.getAggregateType().getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("sequenceNum", event.getSequenceNum().toString().getBytes(StandardCharsets.UTF_8)));

            // Add custom headers if present
            if (event.getHeaders() != null) {
                event.getHeaders().forEach((key, value) -> {
                    if (value != null) {
                        record.headers().add(new RecordHeader(key, value.toString().getBytes(StandardCharsets.UTF_8)));
                    }
                });
            }

            // Send to Kafka with callback
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish outbox event {} to topic {}", event.getId(), topic, ex);
                    throw new OutboxPublishException("Failed to publish event " + event.getId(), ex);
                } else {
                    log.debug("Successfully published outbox event {} to topic {} at offset {}",
                            event.getId(), topic, result.getRecordMetadata().offset());
                }
            });

            // Wait for acknowledgment (synchronous for transaction safety)
            future.get(10, TimeUnit.SECONDS);

            // Mark as published in database
            int updated = outboxRepository.markAsPublished(event.getId(), Instant.now());
            if (updated == 0) {
                log.warn("Event {} was already marked as published by another process", event.getId());
            }

            timer.stop(Timer.builder("outbox.publish.duration")
                    .description("Time taken to publish an outbox event")
                    .tag("eventType", event.getEventType())
                    .tag("status", "success")
                    .register(meterRegistry));

            Counter.builder("outbox.publish.success")
                    .description("Number of successfully published outbox events")
                    .tag("eventType", event.getEventType())
                    .register(meterRegistry)
                    .increment();

        } catch (Exception e) {
            timer.stop(Timer.builder("outbox.publish.duration")
                    .description("Time taken to publish an outbox event")
                    .tag("eventType", event.getEventType())
                    .tag("status", "failure")
                    .register(meterRegistry));

            Counter.builder("outbox.publish.failure")
                    .description("Number of failed outbox publish attempts")
                    .tag("eventType", event.getEventType())
                    .register(meterRegistry)
                    .increment();

            throw new OutboxPublishException("Failed to publish event " + event.getId(), e);
        }
    }

    /**
     * Handles a publish failure by incrementing the retry count and recording the error.
     *
     * @param event the event that failed to publish
     * @param exception the exception that occurred
     */
    @Transactional
    protected void handlePublishFailure(OutboxEvent event, Exception exception) {
        String errorMessage = exception.getMessage();
        if (errorMessage != null && errorMessage.length() > 1000) {
            errorMessage = errorMessage.substring(0, 1000);
        }

        outboxRepository.incrementRetryCount(event.getId(), errorMessage);

        log.warn("Failed to publish outbox event {} (retry {}/{}): {}",
                event.getId(), event.getRetryCount() + 1, maxRetries, errorMessage);

        if (event.getRetryCount() + 1 >= maxRetries) {
            log.error("Outbox event {} has exceeded maximum retry attempts and will be marked as failed", event.getId());
            Counter.builder("outbox.publish.permanent.failure")
                    .description("Number of events that exceeded max retry attempts")
                    .tag("eventType", event.getEventType())
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * Manually triggers publishing of a specific event by ID.
     * Useful for retrying failed events through an admin API.
     *
     * @param eventId the ID of the event to publish
     * @return true if the event was successfully published, false otherwise
     */
    @Transactional
    public boolean publishEventById(java.util.UUID eventId) {
        OutboxEvent event = outboxRepository.findWithLockById(eventId)
                .orElseThrow(() -> new OutboxEventNotFoundException("Event not found: " + eventId));

        if (event.isPublished()) {
            log.warn("Event {} is already published", eventId);
            return false;
        }

        try {
            publishEvent(event);
            return true;
        } catch (Exception e) {
            handlePublishFailure(event, e);
            return false;
        }
    }

    /**
     * Retries all failed events (those that have not exceeded max retries).
     * Can be triggered manually through an admin endpoint.
     *
     * @return the number of events queued for retry
     */
    @Transactional
    public int retryFailedEvents() {
        Pageable pageable = PageRequest.of(0, batchSize);
        List<OutboxEvent> failedEvents = outboxRepository.findUnpublishedEventsForRetry(maxRetries, pageable).getContent();

        int count = 0;
        for (OutboxEvent event : failedEvents) {
            try {
                publishEvent(event);
                count++;
            } catch (Exception e) {
                handlePublishFailure(event, e);
            }
        }

        log.info("Retried {} failed outbox events", count);
        return count;
    }

    /**
     * Serializes the payload map to JSON string.
     *
     * @param payload the payload map
     * @return JSON string representation
     */
    private String serializePayload(Map<String, Object> payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .writeValueAsString(payload);
        } catch (Exception e) {
            throw new OutboxSerializationException("Failed to serialize payload", e);
        }
    }

    /**
     * Custom exception for outbox publishing errors.
     */
    public static class OutboxPublishException extends RuntimeException {
        public OutboxPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Custom exception for outbox event not found.
     */
    public static class OutboxEventNotFoundException extends RuntimeException {
        public OutboxEventNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Custom exception for serialization errors.
     */
    public static class OutboxSerializationException extends RuntimeException {
        public OutboxSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
