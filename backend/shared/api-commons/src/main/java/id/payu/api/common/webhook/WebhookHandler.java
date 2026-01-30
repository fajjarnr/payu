package id.payu.api.common.webhook;

/**
 * Interface for webhook handlers implementing the "Verify-ACK-Process" pattern.
 *
 * <p>This interface defines the contract for processing webhooks with:
 * <ul>
 *   <li>HMAC signature verification</li>
 *   <li>Idempotency checking</li>
 *   <li>Quick ACK (202 Accepted)</li>
 *   <li>Asynchronous processing</li>
 *   <li>Error handling with retry logic</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @Component
 * public class PaymentWebhookHandler implements WebhookHandler {
 *
 *     @Override
 *     public void processWebhook(String webhookId, String payload) {
 *         PaymentEvent event = parseEvent(payload);
 *         // Process the event
 *     }
 *
 *     @Override
 *     public void onError(String webhookId, Throwable error) {
 *         log.error("Failed to process webhook: {}", webhookId, error);
 *     }
 * }
 * }</pre>
 *
 * @author PayU Platform Engineering
 * @see WebhookVerifier
 * @see WebhookProcessor
 * @since 1.0.0
 */
public interface WebhookHandler {

    /**
     * Processes the webhook payload after successful verification and idempotency check.
     *
     * <p>This method is called asynchronously by the {@link WebhookProcessor} after
     * the webhook has been acknowledged (202 Accepted) to the sender.
     *
     * <p>Implementations should:
     * <ul>
     *   <li>Parse the payload into domain objects</li>
     *   <li>Perform business logic processing</li>
     *   <li>Throw {@link WebhookProcessingException} for retryable errors</li>
     *   <li>Throw {@link WebhookValidationException} for non-retryable errors</li>
     * </ul>
     *
     * @param webhookId the unique identifier of the webhook (from X-Webhook-Id header)
     * @param payload the raw JSON payload received from the webhook sender
     * @throws WebhookProcessingException if processing fails and retry is needed
     * @throws WebhookValidationException if payload is invalid and should not be retried
     */
    void processWebhook(String webhookId, String payload);

    /**
     * Called when webhook processing fails after all retry attempts.
     *
     * <p>This callback allows handlers to perform cleanup, alerting, or DLQ operations
     * when a webhook cannot be processed successfully.
     *
     * @param webhookId the unique identifier of the failed webhook
     * @param error the error that caused the final failure
     */
    default void onError(String webhookId, Throwable error) {
        // Default no-op implementation
    }

    /**
     * Called when a webhook is successfully processed.
     *
     * <p>This callback can be used for metrics, logging, or triggering downstream processes.
     *
     * @param webhookId the unique identifier of the successfully processed webhook
     * @param result optional result object from the processing
     */
    default void onSuccess(String webhookId, Object result) {
        // Default no-op implementation
    }

    /**
     * Returns the event types this handler can process.
     *
     * <p>Optional method for handlers that filter by event type.
     * Return empty array to accept all event types.
     *
     * @return array of supported event type strings
     */
    default String[] supportedEventTypes() {
        return new String[0];
    }

    /**
     * Validates the webhook payload before processing.
     *
     * <p>This method is called synchronously before {@link #processWebhook}.
     * Use it for lightweight validation that doesn't require external calls.
     *
     * @param webhookId the unique identifier of the webhook
     * @param payload the raw JSON payload
     * @return true if payload is valid, false otherwise
     */
    default boolean validatePayload(String webhookId, String payload) {
        return true;
    }
}
