package id.payu.outbox.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing an outbox event for the Transactional Outbox Pattern.
 * <p>
 * This entity stores domain events that need to be published to the message broker.
 * Events are written to the database within the same transaction as the business data,
 * ensuring atomicity. A separate publisher process then reads unpublished events
 * and publishes them to Kafka.
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id"),
        @Index(name = "idx_outbox_published", columnList = "published_at"),
        @Index(name = "idx_outbox_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    /**
     * Unique identifier for the outbox event.
     */
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The type of aggregate that generated this event (e.g., "Wallet", "Transaction").
     * Used for routing and filtering events.
     */
    @Column(name = "aggregate_type", length = 100, nullable = false)
    private String aggregateType;

    /**
     * The unique identifier of the aggregate instance that generated this event.
     * Used for event ordering and idempotency.
     */
    @Column(name = "aggregate_id", length = 100, nullable = false)
    private String aggregateId;

    /**
     * The type of event that occurred (e.g., "WalletCredited", "TransactionCompleted").
     * Used by consumers to determine how to process the event.
     */
    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    /**
     * The event payload stored as JSONB.
     * Contains all the data necessary for consumers to process the event.
     */
    @Type(JsonType.class)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    /**
     * Optional headers for the event, stored as JSONB.
     * Can include metadata like correlation IDs, trace IDs, etc.
     */
    @Type(JsonType.class)
    @Column(name = "headers", columnDefinition = "jsonb")
    private Map<String, Object> headers;

    /**
     * The destination topic for this event.
     * If not specified, the default topic from configuration will be used.
     */
    @Column(name = "destination_topic", length = 255)
    private String destinationTopic;

    /**
     * Timestamp when the event was created (when it was written to the outbox).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the event was published to the message broker.
     * Null if the event has not been published yet.
     */
    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Monotonically increasing sequence number for strict ordering.
     * Used to ensure events are processed in the order they were created.
     */
    @Column(name = "sequence_num", insertable = false, updatable = false)
    private Long sequenceNum;

    /**
     * Number of retry attempts for publishing this event.
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Error message from the last failed publish attempt.
     */
    @Column(name = "last_error", length = 1000)
    private String lastError;

    /**
     * JPA pre-persist callback to set the created timestamp.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
    }

    /**
     * Marks this event as published.
     *
     * @return this event instance for method chaining
     */
    public OutboxEvent markAsPublished() {
        this.publishedAt = Instant.now();
        return this;
    }

    /**
     * Increments the retry count and records the error message.
     *
     * @param errorMessage the error message from the failed attempt
     * @return this event instance for method chaining
     */
    public OutboxEvent incrementRetry(String errorMessage) {
        this.retryCount++;
        this.lastError = errorMessage;
        return this;
    }

    /**
     * Checks if this event has been published.
     *
     * @return true if the event has been published, false otherwise
     */
    public boolean isPublished() {
        return this.publishedAt != null;
    }

    /**
     * Checks if this event should be retried based on the maximum retry count.
     *
     * @param maxRetries the maximum number of retry attempts allowed
     * @return true if the event should be retried, false otherwise
     */
    public boolean shouldRetry(int maxRetries) {
        return !isPublished() && this.retryCount < maxRetries;
    }
}
