package id.payu.api.common.webhook;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for webhook handling.
 *
 * <p>This configuration class provides settings for:
 * <ul>
 *   <li>Security (secret keys, tolerance)</li>
 *   <li>Idempotency (TTL settings)</li>
 *   <li>Retry logic (max attempts, delays)</li>
 *   <li>Kafka integration (topic name)</li>
 * </ul>
 *
 * <p><strong>Configuration Example (application.yml):</strong>
 * <pre>{@code
 * webhook:
 *   security:
 *     secret: ${WEBHOOK_SECRET:changeme}
 *     tolerance-seconds: 300
 *   idempotency:
 *     ttl-hours: 24
 *   retry:
 *     max-attempts: 3
 *     initial-delay-ms: 1000
 *   kafka:
 *     topic: webhook-events
 * }</pre>
 *
 * @author PayU Platform Engineering
 * @see WebhookVerifier
 * @see WebhookProcessor
 * @since 1.0.0
 */
@Slf4j
@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "webhook")
public class WebhookConfig {

    /**
     * Security configuration for webhook verification.
     */
    @NotNull
    private SecurityConfig security = new SecurityConfig();

    /**
     * Idempotency configuration.
     */
    @NotNull
    private IdempotencyConfig idempotency = new IdempotencyConfig();

    /**
     * Retry configuration.
     */
    @NotNull
    private RetryConfig retry = new RetryConfig();

    /**
     * Kafka configuration.
     */
    @NotNull
    private KafkaConfig kafka = new KafkaConfig();

    private static final String DEV_DEFAULT_SECRET = "payu-webhook-dev-secret-CHANGE-ME-IN-PRODUCTION-2026";  // pragma: allowlist secret

    @PostConstruct
    void validateSecret() {
        if (DEV_DEFAULT_SECRET.equals(security.getSecret())) {
            log.warn("***** SECURITY WARNING: Webhook secret is using the default dev value. "
                    + "Set 'webhook.security.secret' to a strong, unique value in production! *****");
            String active = System.getenv("SPRING_PROFILES_ACTIVE");
            if (active == null) active = System.getProperty("spring.profiles.active", "");
            boolean isProdLike = active != null && java.util.Set.of("prod", "container", "staging", "production").stream().anyMatch(active::contains);
            // ponytail: fail-closed in prod-like profiles — dev/container local remains warn-only without breaking local compose
            if (isProdLike) {
                throw new IllegalStateException("webhook.security.secret default dev value forbidden in profile '" + active + "' — set WEBHOOK_SECRET via Vault");
            }
        }
    }

    /**
     * Gets the webhook secret from security configuration.
     *
     * @return the configured webhook secret
     */
    public String getSecret() {
        return security.getSecret();
    }

    /**
     * Gets the timestamp tolerance in seconds.
     *
     * @return tolerance in seconds
     */
    public int getToleranceSeconds() {
        return security.getToleranceSeconds();
    }

    /**
     * Gets the idempotency TTL in hours.
     *
     * @return TTL in hours
     */
    public int getIdempotencyTtlHours() {
        return idempotency.getTtlHours();
    }

    /**
     * Gets the maximum retry attempts.
     *
     * @return max retry attempts
     */
    public int getRetryMaxAttempts() {
        return retry.getMaxAttempts();
    }

    /**
     * Gets the initial retry delay in milliseconds.
     *
     * @return initial delay in milliseconds
     */
    public long getRetryInitialDelayMs() {
        return retry.getInitialDelayMs();
    }

    /**
     * Gets the Kafka topic for webhook events.
     *
     * @return Kafka topic name
     */
    public String getKafkaTopic() {
        return kafka.getTopic();
    }

    /**
     * Security configuration properties.
     */
    @Getter
    @Setter
    public static class SecurityConfig {

        /**
         * Webhook secret key for HMAC signature verification.
         *
         * <p><strong>Security Warning:</strong> This should be stored securely
         * (e.g., in HashiCorp Vault or OpenShift Secrets) and never committed to source control.
         */
        @NotBlank(message = "Webhook secret must be configured")
        private String secret = "payu-webhook-dev-secret-CHANGE-ME-IN-PRODUCTION-2026";  // pragma: allowlist secret

        /**
         * Timestamp tolerance in seconds for replay attack prevention.
         *
         * <p>Default: 300 seconds (5 minutes)
         */
        @Min(value = 30, message = "Tolerance must be at least 30 seconds")
        @Max(value = 3600, message = "Tolerance must not exceed 1 hour")
        private int toleranceSeconds = 300;
    }

    /**
     * Idempotency configuration properties.
     */
    @Getter
    @Setter
    public static class IdempotencyConfig {

        /**
         * Time-to-live for idempotency records in hours.
         *
         * <p>After this period, duplicate webhooks with the same ID may be processed again.
         * Default: 24 hours
         */
        @Min(value = 1, message = "TTL must be at least 1 hour")
        @Max(value = 168, message = "TTL must not exceed 1 week")
        private int ttlHours = 24;
    }

    /**
     * Retry configuration properties.
     */
    @Getter
    @Setter
    public static class RetryConfig {

        /**
         * Maximum number of retry attempts for failed webhooks.
         *
         * <p>Default: 3 attempts
         */
        @Min(value = 0, message = "Max attempts must be non-negative")
        @Max(value = 10, message = "Max attempts must not exceed 10")
        private int maxAttempts = 3;

        /**
         * Initial delay before the first retry in milliseconds.
         *
         * <p>Subsequent retries use exponential backoff.
         * Default: 1000ms (1 second)
         */
        @Min(value = 100, message = "Initial delay must be at least 100ms")
        @Max(value = 60000, message = "Initial delay must not exceed 60 seconds")
        private long initialDelayMs = 1000;
    }

    /**
     * Kafka configuration properties.
     */
    @Getter
    @Setter
    public static class KafkaConfig {

        /**
         * Kafka topic for webhook events.
         *
         * <p>Default: webhook-events
         */
        @NotBlank(message = "Kafka topic must be configured")
        private String topic = "payu.webhook.events.v1";
    }
}
