package id.payu.partner.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for webhook delivery log entries.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Webhook delivery attempt record")
public class WebhookDeliveryDTO {

    @Schema(description = "Delivery ID", example = "42")
    private Long id;

    @Schema(description = "Unique event identifier", example = "evt_abc123")
    private String eventId;

    @Schema(description = "Event type", example = "payment.completed")
    private String eventType;

    @Schema(description = "Delivery status", example = "DELIVERED")
    private String status;

    @Schema(description = "Number of delivery attempts", example = "1")
    private int attemptCount;

    @Schema(description = "Maximum delivery attempts", example = "5")
    private int maxAttempts;

    @Schema(description = "HTTP response status code", example = "200")
    private Integer responseCode;

    @Schema(description = "Error message if delivery failed")
    private String errorMessage;

    @Schema(description = "When delivery was created")
    private String createdAt;

    @Schema(description = "When last delivery attempt was made")
    private String lastAttemptAt;

    @Schema(description = "When next retry is scheduled")
    private String nextRetryAt;

    @Schema(description = "When delivery was successfully completed")
    private String deliveredAt;

    public WebhookDeliveryDTO() {}

    // --- Getters/Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(String lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public String getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(String nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public String getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(String deliveredAt) { this.deliveredAt = deliveredAt; }
}
