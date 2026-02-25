package id.payu.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.outbox.entity.OutboxEvent;
import id.payu.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Service for creating and managing outbox events.
 * <p>
 * This service provides the API for domain services to create outbox events
 * within their transactions. Events created through this service will be
 * picked up by the {@link id.payu.outbox.publisher.OutboxPublisher} and
 * published to Kafka.
 * <p>
 * Usage example:
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class WalletService {
 *     private final WalletRepository walletRepository;
 *     private final OutboxService outboxService;
 *
 *     @Transactional
 *     public void creditWallet(UUID walletId, BigDecimal amount) {
 *         // Update wallet balance
 *         Wallet wallet = walletRepository.findById(walletId).orElseThrow();
 *         wallet.credit(amount);
 *         walletRepository.save(wallet);
 *
 *         // Create outbox event (same transaction)
 *         outboxService.createEvent(
 *             "Wallet",
 *             walletId.toString(),
 *             "WalletCredited",
 *             Map.of(
 *                 "walletId", walletId,
 *                 "amount", amount,
 *                 "newBalance", wallet.getBalance()
 *             )
 *         );
 *     }
 * }
 * }</pre>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new outbox event.
     * <p>
     * The event is persisted to the database within the current transaction.
     * It will be published to Kafka by the OutboxPublisher in a separate process.
     *
     * @param aggregateType the type of aggregate that generated the event (e.g., "Wallet")
     * @param aggregateId the ID of the aggregate instance
     * @param eventType the type of event (e.g., "WalletCredited")
     * @param payload the event payload data
     * @return the created OutboxEvent
     */
    @Transactional
    public OutboxEvent createEvent(
            @NotBlank String aggregateType,
            @NotBlank String aggregateId,
            @NotBlank String eventType,
            @NotNull Map<String, Object> payload) {

        return createEvent(aggregateType, aggregateId, eventType, payload, null, null);
    }

    /**
     * Creates a new outbox event with custom headers.
     *
     * @param aggregateType the type of aggregate that generated the event
     * @param aggregateId the ID of the aggregate instance
     * @param eventType the type of event
     * @param payload the event payload data
     * @param headers custom headers for the event
     * @return the created OutboxEvent
     */
    @Transactional
    public OutboxEvent createEvent(
            @NotBlank String aggregateType,
            @NotBlank String aggregateId,
            @NotBlank String eventType,
            @NotNull Map<String, Object> payload,
            Map<String, Object> headers) {

        return createEvent(aggregateType, aggregateId, eventType, payload, headers, null);
    }

    /**
     * Creates a new outbox event with custom headers and destination topic.
     *
     * @param aggregateType the type of aggregate that generated the event
     * @param aggregateId the ID of the aggregate instance
     * @param eventType the type of event
     * @param payload the event payload data
     * @param headers custom headers for the event (can be null)
     * @param destinationTopic the destination Kafka topic (can be null for default)
     * @return the created OutboxEvent
     */
    @Transactional
    public OutboxEvent createEvent(
            @NotBlank String aggregateType,
            @NotBlank String aggregateId,
            @NotBlank String eventType,
            @NotNull Map<String, Object> payload,
            Map<String, Object> headers,
            String destinationTopic) {

        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(payload, "payload must not be null");

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .headers(headers)
                .destinationTopic(destinationTopic)
                .createdAt(Instant.now())
                .retryCount(0)
                .build();

        OutboxEvent savedEvent = outboxRepository.save(event);

        log.debug("Created outbox event: id={}, aggregateType={}, aggregateId={}, eventType={}",
                savedEvent.getId(), aggregateType, aggregateId, eventType);

        return savedEvent;
    }

    /**
     * Creates an outbox event from a domain event object.
     * <p>
     * The domain event object is serialized to a Map for storage.
     *
     * @param aggregateType the type of aggregate
     * @param aggregateId the ID of the aggregate instance
     * @param eventType the type of event
     * @param domainEvent the domain event object to serialize
     * @return the created OutboxEvent
     */
    @Transactional
    public OutboxEvent createEventFromObject(
            @NotBlank String aggregateType,
            @NotBlank String aggregateId,
            @NotBlank String eventType,
            @NotNull Object domainEvent) {

        return createEventFromObject(aggregateType, aggregateId, eventType, domainEvent, null, null);
    }

    /**
     * Creates an outbox event from a domain event object with custom headers and topic.
     *
     * @param aggregateType the type of aggregate
     * @param aggregateId the ID of the aggregate instance
     * @param eventType the type of event
     * @param domainEvent the domain event object to serialize
     * @param headers custom headers (can be null)
     * @param destinationTopic the destination topic (can be null)
     * @return the created OutboxEvent
     */
    @Transactional
    public OutboxEvent createEventFromObject(
            @NotBlank String aggregateType,
            @NotBlank String aggregateId,
            @NotBlank String eventType,
            @NotNull Object domainEvent,
            Map<String, Object> headers,
            String destinationTopic) {

        try {
            // Convert domain event object to Map
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(domainEvent, Map.class);

            return createEvent(aggregateType, aggregateId, eventType, payload, headers, destinationTopic);
        } catch (Exception e) {
            log.error("Failed to serialize domain event for aggregate {}: {}", aggregateId, e.getMessage());
            throw new OutboxEventCreationException("Failed to serialize domain event", e);
        }
    }

    /**
     * Gets the count of pending (unpublished) events.
     *
     * @return the number of unpublished events
     */
    public long getPendingEventCount() {
        return outboxRepository.countUnpublishedEvents();
    }

    /**
     * Gets the count of failed events (exceeded max retries).
     *
     * @param maxRetries the maximum retry count threshold
     * @return the number of failed events
     */
    public long getFailedEventCount(int maxRetries) {
        return outboxRepository.countFailedEvents(maxRetries);
    }

    /**
     * Exception thrown when event creation fails.
     */
    public static class OutboxEventCreationException extends RuntimeException {
        public OutboxEventCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
