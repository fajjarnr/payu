package id.payu.outbox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for the Transactional Outbox Pattern.
 * <p>
 * These properties can be configured in application.yml or application.properties
 * under the prefix {@code payu.outbox}.
 *
 * <pre>{@code
 * payu:
 *   outbox:
 *     enabled: true
 *     publisher:
 *       batch-size: 100
 *       poll-interval-ms: 1000
 *       max-retries: 3
 *       default-topic: outbox.events
 *     cleanup:
 *       enabled: true
 *       retention-days: 30
 *       cron: "0 0 2 * * *"  # Daily at 2 AM
 * }</pre>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "payu.outbox")
public class OutboxProperties {

    /**
     * Whether the outbox pattern is enabled.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Publisher-specific configuration.
     */
    @NotNull
    private PublisherProperties publisher = new PublisherProperties();

    /**
     * Cleanup-specific configuration.
     */
    @NotNull
    private CleanupProperties cleanup = new CleanupProperties();

    /**
     * Publisher configuration properties.
     */
    @Data
    @Validated
    public static class PublisherProperties {

        /**
         * Number of events to process in a single batch.
         * Default: 100
         */
        @Min(1)
        @Max(1000)
        private int batchSize = 100;

        /**
         * Interval in milliseconds between polling for new events.
         * Default: 1000 (1 second)
         */
        @Min(100)
        private long pollIntervalMs = 1000;

        /**
         * Maximum number of retry attempts for failed events.
         * Default: 3
         */
        @Min(0)
        @Max(10)
        private int maxRetries = 3;

        /**
         * Default Kafka topic for publishing events.
         * Default: "outbox.events"
         */
        @NotBlank
        private String defaultTopic = "outbox.events";

        /**
         * Lock timeout in milliseconds for pessimistic locking.
         * Default: 10000 (10 seconds)
         */
        @Min(1000)
        private long lockTimeoutMs = 10000;

        /**
         * Whether the publisher is enabled.
         * Default: true
         */
        private boolean enabled = true;
    }

    /**
     * Cleanup configuration properties.
     */
    @Data
    @Validated
    public static class CleanupProperties {

        /**
         * Whether cleanup of old events is enabled.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Number of days to retain published events before deletion.
         * Default: 30
         */
        @Min(1)
        private int retentionDays = 30;

        /**
         * Number of days to retain failed events (exceeded max retries) before deletion.
         * Default: 7
         */
        @Min(1)
        private int failedRetentionDays = 7;

        /**
         * Cron expression for cleanup schedule.
         * Default: "0 0 2 * * *" (daily at 2 AM)
         */
        @NotBlank
        private String cron = "0 0 2 * * *";
    }
}
