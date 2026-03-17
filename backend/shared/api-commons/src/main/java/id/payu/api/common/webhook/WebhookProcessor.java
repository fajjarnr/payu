package id.payu.api.common.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Processor for handling webhook idempotency and asynchronous processing.
 *
 * <p>This component implements the "Quick ACK" pattern:
 * <ol>
 *   <li>Check idempotency (sync)</li>
 *   <li>Acknowledge immediately with 202 Accepted (sync)</li>
 *   <li>Process asynchronously via Kafka/Outbox (async)</li>
 * </ol>
 *
 * <p><strong>Idempotency Key Design:</strong>
 * <ul>
 *   <li>{@code webhook:{id}} - Tracks processed webhooks (TTL: 24 hours)</li>
 *   <li>{@code webhook:{id}:processing} - Tracks in-flight webhooks (TTL: 5 minutes)</li>
 *   <li>{@code webhook:{id}:error} - Tracks failed webhooks (TTL: 7 days)</li>
 * </ul>
 *
 * @author PayU Platform Engineering
 * @see WebhookHandler
 * @see WebhookVerifier
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookProcessor implements DisposableBean {

    private static final String KEY_PREFIX = "webhook:";
    private static final String PROCESSING_SUFFIX = ":processing";
    private static final String ERROR_SUFFIX = ":error";

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, WebhookEvent> kafkaTemplate;
    private final WebhookConfig config;

    // BUG-BE-092: Dedicated scheduler for non-blocking retry delays
    private static final ScheduledExecutorService RETRY_SCHEDULER = Executors.newScheduledThreadPool(2,
            r -> {
                Thread t = new Thread(r, "webhook-retry");
                t.setDaemon(true);
                return t;
            }
    );

    /**
     * Checks if a webhook has already been processed.
     *
     * <p>This method uses Redis to check for duplicate webhook IDs.
     * If the webhook was processed within the TTL window, it returns true.
     *
     * @param webhookId the unique webhook identifier
     * @return true if the webhook has already been processed
     */
    public boolean isProcessed(String webhookId) {
        String key = buildKey(webhookId);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Checks if a webhook is currently being processed.
     *
     * <p>This helps detect concurrent processing of the same webhook.
     *
     * @param webhookId the unique webhook identifier
     * @return true if the webhook is currently being processed
     */
    public boolean isProcessing(String webhookId) {
        String key = buildKey(webhookId) + PROCESSING_SUFFIX;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Acknowledges a webhook by marking it as received.
     *
     * <p>This method should be called immediately before returning 202 Accepted.
     * It sets both the processed flag (for idempotency) and processing flag
     * (for in-flight detection).
     *
     * @param webhookId the unique webhook identifier
     */
    public void acknowledge(String webhookId) {
        String processedKey = buildKey(webhookId);
        String processingKey = processedKey + PROCESSING_SUFFIX;

        Duration processedTtl = Duration.ofHours(config.getIdempotencyTtlHours());
        Duration processingTtl = Duration.ofMinutes(5);

        redisTemplate.opsForValue().set(processedKey, "acknowledged", processedTtl);
        redisTemplate.opsForValue().set(processingKey, "true", processingTtl);

        log.debug("Webhook acknowledged: id={}", webhookId);
    }

    /**
     * Marks a webhook as successfully processed.
     *
     * @param webhookId the unique webhook identifier
     */
    public void markProcessed(String webhookId) {
        String key = buildKey(webhookId);
        String processingKey = key + PROCESSING_SUFFIX;

        Duration ttl = Duration.ofHours(config.getIdempotencyTtlHours());
        redisTemplate.opsForValue().set(key, "processed", ttl);
        redisTemplate.delete(processingKey);

        log.debug("Webhook marked as processed: id={}", webhookId);
    }

    /**
     * Marks a webhook as failed.
     *
     * @param webhookId the unique webhook identifier
     * @param errorMessage the error message
     */
    public void markFailed(String webhookId, String errorMessage) {
        String key = buildKey(webhookId);
        String processingKey = key + PROCESSING_SUFFIX;
        String errorKey = key + ERROR_SUFFIX;

        Duration ttl = Duration.ofHours(config.getIdempotencyTtlHours());
        Duration errorTtl = Duration.ofDays(7);

        redisTemplate.opsForValue().set(key, "failed:" + errorMessage, ttl);
        redisTemplate.delete(processingKey);
        redisTemplate.opsForValue().set(errorKey, errorMessage, errorTtl);

        log.warn("Webhook marked as failed: id={}, error={}", webhookId, errorMessage);
    }

    /**
     * Processes a webhook asynchronously.
     *
     * <p>This method publishes the webhook to Kafka for async processing.
     * The handler will be invoked by a consumer.
     *
     * @param webhookId the unique webhook identifier
     * @param payload the raw webhook payload
     */
    @Async("webhookTaskExecutor")
    public void processAsync(String webhookId, String payload) {
        WebhookEvent event = new WebhookEvent(webhookId, payload, System.currentTimeMillis());

        try {
            kafkaTemplate.send(config.getKafkaTopic(), webhookId, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish webhook to Kafka: id={}", webhookId, ex);
                            markFailed(webhookId, "Kafka publish failed: " + ex.getMessage());
                        } else {
                            log.debug("Webhook published to Kafka: id={}, offset={}",
                                    webhookId, result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to publish webhook to Kafka: id={}", webhookId, e);
            markFailed(webhookId, "Kafka publish failed: " + e.getMessage());
            throw new WebhookProcessingException(webhookId, "Failed to publish to Kafka", e);
        }
    }

    /**
     * Processes a webhook asynchronously with a specific handler.
     *
     * <p>This method is used when direct processing is needed instead of Kafka.
     *
     * @param webhookId the unique webhook identifier
     * @param payload the raw webhook payload
     * @param handler the handler to process the webhook
     */
    @Async("webhookTaskExecutor")
    public void processAsync(String webhookId, String payload, WebhookHandler handler) {
        processWithRetry(webhookId, payload, handler, 0);
    }

    /**
     * Processes a webhook with retry logic.
     *
     * <p>Implements exponential backoff for retryable failures.
     *
     * @param webhookId the unique webhook identifier
     * @param payload the raw webhook payload
     * @param handler the handler to process the webhook
     * @param attempt the current attempt number (0-based)
     */
    private void processWithRetry(String webhookId, String payload, WebhookHandler handler, int attempt) {
        try {
            log.info("Processing webhook: id={}, attempt={}", webhookId, attempt + 1);

            // Validate payload before processing
            if (!handler.validatePayload(webhookId, payload)) {
                log.warn("Invalid webhook payload: id={}", webhookId);
                markFailed(webhookId, "Payload validation failed");
                handler.onError(webhookId, new WebhookValidationException("Payload validation failed"));
                return;
            }

            // Process the webhook
            handler.processWebhook(webhookId, payload);

            // Mark as processed
            markProcessed(webhookId);
            handler.onSuccess(webhookId, null);

            log.info("Webhook processed successfully: id={}", webhookId);

        } catch (WebhookValidationException e) {
            // Non-retryable error
            log.warn("Webhook validation failed (non-retryable): id={}", webhookId, e);
            markFailed(webhookId, "Validation failed: " + e.getMessage());
            handler.onError(webhookId, e);

        } catch (Exception e) {
            // Retryable error
            if (attempt < config.getRetryMaxAttempts() - 1) {
                long delayMs = calculateBackoffDelay(attempt);
                log.warn("Webhook processing failed, will retry: id={}, attempt={}, delay={}ms",
                        webhookId, attempt + 1, delayMs, e);

                // BUG-BE-092: Non-blocking retry delay using ScheduledExecutorService
                // instead of Thread.sleep which would block the @Async thread pool
                final int nextAttempt = attempt + 1;
                RETRY_SCHEDULER.schedule(
                        () -> processWithRetry(webhookId, payload, handler, nextAttempt),
                        delayMs,
                        TimeUnit.MILLISECONDS
                );
            } else {
                log.error("Webhook processing failed after all retries: id={}", webhookId, e);
                markFailed(webhookId, "Max retries exceeded: " + e.getMessage());
                handler.onError(webhookId, e);
                throw new WebhookProcessingException(webhookId, "Max retries exceeded", e);
            }
        }
    }

    /**
     * Calculates the delay for the next retry attempt using exponential backoff.
     *
     * <p>Formula: delay = baseDelay * (2 ^ attempt) + jitter
     *
     * @param attempt the current attempt number (0-based)
     * @return the delay in milliseconds
     */
    private long calculateBackoffDelay(int attempt) {
        long baseDelay = config.getRetryInitialDelayMs();
        long exponentialDelay = baseDelay * (1L << attempt); // 2^attempt

        // Add jitter (±20%) to prevent thundering herd
        double jitter = 0.8 + (Math.random() * 0.4);
        return (long) (exponentialDelay * jitter);
    }

    /**
     * Gets the error message for a failed webhook.
     *
     * @param webhookId the unique webhook identifier
     * @return the error message, or null if not found
     */
    public String getErrorMessage(String webhookId) {
        String errorKey = buildKey(webhookId) + ERROR_SUFFIX;
        return redisTemplate.opsForValue().get(errorKey);
    }

    /**
     * Clears the idempotency record for a webhook.
     *
     * <p>Use with caution - this allows duplicate processing.
     *
     * @param webhookId the unique webhook identifier
     */
    public void clearIdempotency(String webhookId) {
        String key = buildKey(webhookId);
        redisTemplate.delete(key);
        redisTemplate.delete(key + PROCESSING_SUFFIX);
        redisTemplate.delete(key + ERROR_SUFFIX);

        log.info("Cleared idempotency record for webhook: id={}", webhookId);
    }

    private String buildKey(String webhookId) {
        return KEY_PREFIX + webhookId;
    }

    /**
     * Shuts down the static retry scheduler on application context close
     * to prevent thread leaks.
     */
    @Override
    public void destroy() throws Exception {
        log.info("Shutting down webhook retry scheduler");
        RETRY_SCHEDULER.shutdown();
        try {
            if (!RETRY_SCHEDULER.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Webhook retry scheduler did not terminate gracefully, forcing shutdown");
                RETRY_SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            RETRY_SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Event object for Kafka messaging.
     */
    public record WebhookEvent(String webhookId, String payload, long timestamp) {
    }
}
