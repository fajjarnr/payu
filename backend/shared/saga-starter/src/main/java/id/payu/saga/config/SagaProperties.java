package id.payu.saga.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for saga pattern support.
 */
@Data
@ConfigurationProperties(prefix = "payu.saga")
public class SagaProperties {

    /**
     * Enable saga pattern support.
     */
    private boolean enabled = true;

    /**
     * Enable saga persistence to database.
     */
    private boolean persistent = true;

    /**
     * Enable automatic compensation on failure.
     */
    private boolean compensationEnabled = true;

    /**
     * Default timeout for saga execution.
     */
    private Duration defaultTimeout = Duration.ofMinutes(5);

    /**
     * Default timeout for individual steps.
     */
    private Duration defaultStepTimeout = Duration.ofSeconds(30);

    /**
     * Default number of retries for failed steps.
     */
    private int defaultMaxRetries = 3;

    /**
     * Default delay between retries.
     */
    private Duration defaultRetryDelay = Duration.ofSeconds(1);

    /**
     * Enable saga monitoring and metrics.
     */
    private boolean monitoringEnabled = true;

    /**
     * Enable publishing of saga lifecycle events.
     */
    private boolean eventsEnabled = true;

    /**
     * Topic for saga lifecycle events.
     */
    private String eventTopic = "saga.events";

    /**
     * Enable cleanup of old completed sagas.
     */
    private boolean cleanupEnabled = true;

    /**
     * Retention period for completed sagas before cleanup.
     */
    private Duration retentionPeriod = Duration.ofDays(30);

    /**
     * Schedule for cleanup job (cron expression).
     */
    private String cleanupSchedule = "0 0 2 * * ?"; // Daily at 2 AM

    /**
     * Enable optimistic locking for saga instances.
     */
    private boolean optimisticLocking = true;

    /**
     * Maximum number of sagas to process in parallel.
     */
    private int maxParallelSagas = 100;

    /**
     * Thread pool size for saga execution.
     */
    private int threadPoolSize = 10;

    /**
     * Enable reactive saga execution.
     */
    private boolean reactiveEnabled = true;
}
