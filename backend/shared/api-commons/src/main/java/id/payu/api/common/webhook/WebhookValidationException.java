package id.payu.api.common.webhook;

/**
 * Exception thrown when webhook payload validation fails.
 *
 * <p>This exception indicates a non-retryable error. The webhook payload
 * is invalid and should not be retried. The sender should fix the payload
 * before resending.
 *
 * @author PayU Platform Engineering
 * @see WebhookHandler
 * @see WebhookProcessor
 * @since 1.0.0
 */
public class WebhookValidationException extends RuntimeException {

    private final String webhookId;

    /**
     * Constructs a new webhook validation exception.
     *
     * @param message the validation error message
     */
    public WebhookValidationException(String message) {
        super(message);
        this.webhookId = null;
    }

    /**
     * Constructs a new webhook validation exception with webhook ID.
     *
     * @param webhookId the unique identifier of the webhook that failed validation
     * @param message the validation error message
     */
    public WebhookValidationException(String webhookId, String message) {
        super(message);
        this.webhookId = webhookId;
    }

    /**
     * Constructs a new webhook validation exception with a cause.
     *
     * @param webhookId the unique identifier of the webhook that failed validation
     * @param message the validation error message
     * @param cause the underlying cause of the validation failure
     */
    public WebhookValidationException(String webhookId, String message, Throwable cause) {
        super(message, cause);
        this.webhookId = webhookId;
    }

    /**
     * Gets the webhook ID associated with this exception.
     *
     * @return the webhook ID, or null if not set
     */
    public String getWebhookId() {
        return webhookId;
    }
}
