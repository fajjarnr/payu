package id.payu.integration.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain entity representing a message processed by the integration layer.
 * Tracks the lifecycle of messages exchanged with legacy systems.
 */
@Entity
@Table(name = "integration_messages", indexes = {
    @Index(name = "idx_message_status", columnList = "status"),
    @Index(name = "idx_message_type", columnList = "type"),
    @Index(name = "idx_message_created_at", columnList = "createdAt"),
    @Index(name = "idx_message_source_target", columnList = "sourceSystem,targetSystem")
})
// BUG-ARCH-005 FIX: Replaced @Data with @Getter @Setter to avoid Lombok-generated equals/hashCode on JPA entities
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationMessage {

    @Id
    @Column(name = "message_id", length = 36)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MessageType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private MessageDirection direction;

    @Column(name = "source_system", length = 100)
    private String sourceSystem;

    @Column(name = "target_system", length = 100)
    private String targetSystem;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "business_reference", length = 100)
    private String businessReference;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "transformed_payload", columnDefinition = "TEXT")
    private String transformedPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    public void prePersist() {
        if (messageId == null) {
            messageId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = MessageStatus.RECEIVED;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (maxRetries == null) {
            maxRetries = 3;
        }
    }

    /**
     * Mark message as being validated.
     */
    public void markValidating() {
        this.status = MessageStatus.VALIDATING;
    }

    /**
     * Mark message as being transformed.
     */
    public void markTransforming() {
        this.status = MessageStatus.TRANSFORMING;
    }

    /**
     * Mark message as transformed with the transformed payload.
     */
    public void markTransformed(String transformedPayload) {
        this.transformedPayload = transformedPayload;
        this.status = MessageStatus.TRANSFORMED;
    }

    /**
     * Mark message as being sent.
     */
    public void markSending() {
        this.status = MessageStatus.SENDING;
    }

    /**
     * Mark message as successfully sent.
     */
    public void markSent() {
        this.status = MessageStatus.SENT;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark message as failed with error message.
     */
    public void markFailed(String errorMessage) {
        this.status = MessageStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark message for retry.
     */
    public void markRetrying() {
        this.status = MessageStatus.RETRYING;
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
    }

    /**
     * Check if message can be retried.
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * Cancel message processing.
     */
    public void cancel() {
        this.status = MessageStatus.CANCELLED;
        this.processedAt = LocalDateTime.now();
    }
}
