package id.payu.api.common.webhook;

/**
 * Exception thrown when webhook processing fails.
 *
 * <p>This exception indicates a retryable error during webhook processing.
 * The {@link WebhookProcessor} will retry the operation according to the
 * configured retry policy.
 *
 * @author PayU Platform Engineering
 * @see WebhookHandler
 * @see WebhookProcessor
 * @since 1.0.0
 */
public class WebhookProcessingException extends RuntimeException {

    private final String webhookId;
    private final boolean retryable;

    /**
     * Constructs a new webhook processing exception.
     *
     * @param webhookId the unique identifier of the webhook that failed
     * @param message the error message
     */
    public WebhookProcessingException(String webhookId, String message) {
        super(message);
        this.webhookId = webhookId;
        this.retryable = true;
    }

    /**
     * Constructs a new webhook processing exception with a cause.
     *
     * @param webhookId the unique identifier of the webhook that failed
     * @param message the error message
     * @param cause the underlying cause of the exception
     */
    public WebhookProcessingException(String webhookId, String message, Throwable cause) {
        super(message, cause);
        this.webhookId = webhookId;
        this.retryable = true;
    }

    /**
     * Constructs a new webhook processing exception with retry flag.
     *
     * @param webhookId the unique identifier of the webhook that failed
     * @param message the error message
     * @param cause the underlying cause of the exception
     * @param retryable whether this error is retryable
     */
    public WebhookProcessingException(String webhookId, String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.webhookId = webhookId;
        this.retryable = retryable;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
