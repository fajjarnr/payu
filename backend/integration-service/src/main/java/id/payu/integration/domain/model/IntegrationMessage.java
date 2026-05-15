package id.payu.integration.domain.model;

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
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationMessage {

    private String messageId;

    private MessageType type;

    private MessageDirection direction;

    private String sourceSystem;

    private String targetSystem;

    private String correlationId;

    private String businessReference;

    private String rawPayload;

    private String transformedPayload;

    private MessageStatus status;

    private String errorMessage;

    @Builder.Default
    private Integer retryCount = 0;

    @Builder.Default
    private Integer maxRetries = 3;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private LocalDateTime lastRetryAt;

    private Long version;

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
