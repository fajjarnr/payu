package id.payu.partner.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for webhook subscription registration and updates.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Webhook subscription request")
public class WebhookSubscriptionDTO {

    @Schema(description = "Subscription ID (read-only)", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Webhook URL is required")
    @Pattern(regexp = "^https://.*", message = "Webhook URL must use HTTPS")
    @Schema(description = "HTTPS URL to receive webhook notifications",
            example = "https://api.tokobapak.com/webhooks/payu")
    private String url;

    @NotBlank(message = "Events are required")
    @Schema(description = "Comma-separated event types to subscribe to (or '*' for all)",
            example = "payment.completed,payment.failed,payment.refunded")
    private String events;

    @Schema(description = "Human-readable description", example = "TokoBapak payment notifications")
    private String description;

    @Schema(description = "Whether subscription is active", example = "true")
    private Boolean active;

    @Schema(description = "Maximum delivery retries (1-10)", example = "5")
    private Integer maxRetries;

    @Schema(description = "HMAC secret for signature verification (read-only, shown once at creation)",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String secret;

    @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private String createdAt;

    @Schema(description = "Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private String updatedAt;

    public WebhookSubscriptionDTO() {}

    // --- Getters/Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getEvents() { return events; }
    public void setEvents(String events) { this.events = events; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
