package id.payu.partner.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks individual webhook delivery attempts.
 * Records status, response, and retry scheduling for audit and debugging.
 */
@Entity
@Table(name = "webhook_deliveries",
       indexes = {
           @Index(name = "idx_delivery_subscription", columnList = "subscription_id"),
           @Index(name = "idx_delivery_status", columnList = "status"),
           @Index(name = "idx_delivery_next_retry", columnList = "next_retry_at"),
           @Index(name = "idx_delivery_event_id", columnList = "event_id")
       })
public class WebhookDelivery {

    public enum Status {
        PENDING,
        DELIVERING,
        DELIVERED,
        FAILED,
        EXHAUSTED  // max retries exceeded
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private WebhookSubscription subscription;

    /**
     * Unique identifier for this event (for idempotency on receiver side).
     */
    @NotBlank
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @NotBlank
    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    /**
     * JSON payload sent to the partner.
     */
    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * HTTP response status code from the partner's webhook endpoint.
     */
    @Column(name = "response_code")
    private Integer responseCode;

    /**
     * Truncated response body from the partner (max 2KB for debugging).
     */
    @Column(name = "response_body", length = 2048)
    private String responseBody;

    /**
     * Error message if delivery failed (connection error, timeout, etc).
     */
    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    public WebhookDelivery() {}

    public WebhookDelivery(WebhookSubscription subscription, String eventId,
                           String eventType, String payload) {
        this.subscription = subscription;
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = Status.PENDING;
        this.maxAttempts = subscription.getMaxRetries();
    }

    // --- Domain Methods ---

    /**
     * Record a successful delivery.
     */
    public void markDelivered(int httpStatus, String body) {
        this.status = Status.DELIVERED;
        this.responseCode = httpStatus;
        this.responseBody = truncate(body, 2048);
        this.deliveredAt = LocalDateTime.now();
        this.lastAttemptAt = LocalDateTime.now();
        this.attemptCount++;
    }

    /**
     * Record a failed delivery attempt and schedule retry.
     */
    public void markFailed(Integer httpStatus, String body, String error) {
        this.attemptCount++;
        this.lastAttemptAt = LocalDateTime.now();
        this.responseCode = httpStatus;
        this.responseBody = truncate(body, 2048);
        this.errorMessage = truncate(error, 1024);

        if (this.attemptCount >= this.maxAttempts) {
            this.status = Status.EXHAUSTED;
            this.nextRetryAt = null;
        } else {
            this.status = Status.FAILED;
            // Exponential backoff: 30s, 2m, 8m, 32m, 2h
            long delaySeconds = (long) (30 * Math.pow(4, this.attemptCount - 1));
            // Cap at 2 hours
            delaySeconds = Math.min(delaySeconds, 7200);
            this.nextRetryAt = LocalDateTime.now().plusSeconds(delaySeconds);
        }
    }

    /**
     * Mark as currently being delivered (prevents concurrent delivery).
     */
    public void markDelivering() {
        this.status = Status.DELIVERING;
    }

    public boolean canRetry() {
        return status == Status.FAILED && attemptCount < maxAttempts;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    // --- Getters/Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WebhookSubscription getSubscription() { return subscription; }
    public void setSubscription(WebhookSubscription subscription) { this.subscription = subscription; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
}
