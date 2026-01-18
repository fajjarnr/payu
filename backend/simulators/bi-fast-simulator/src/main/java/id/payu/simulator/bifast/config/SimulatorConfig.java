package id.payu.simulator.bifast.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for simulator behavior.
 */
@ConfigMapping(prefix = "simulator")
public interface SimulatorConfig {

    /**
     * Latency simulation configuration.
     */
    Latency latency();

    /**
     * Failure rate in percentage (0-100).
     */
    @WithName("failure-rate")
    @WithDefault("5")
    int failureRate();

    /**
     * Webhook configuration.
     */
    Webhook webhook();

    interface Latency {
        /**
         * Minimum latency in milliseconds.
         */
        @WithDefault("50")
        int min();

        /**
         * Maximum latency in milliseconds.
         */
        @WithDefault("500")
        int max();
    }

    interface Webhook {
        /**
         * Enable webhook callbacks.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Delay before sending webhook (ms).
         */
        @WithName("delay-ms")
        @WithDefault("2000")
        int delayMs();

        /**
         * Number of retry attempts.
         */
        @WithName("retry-count")
        @WithDefault("3")
        int retryCount();

        /**
         * Delay between retries (ms).
         */
        @WithName("retry-delay-ms")
        @WithDefault("1000")
        int retryDelayMs();
    }
}
