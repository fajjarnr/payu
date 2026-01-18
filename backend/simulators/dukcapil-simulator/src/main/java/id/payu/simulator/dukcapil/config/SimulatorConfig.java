package id.payu.simulator.dukcapil.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for Dukcapil simulator behavior.
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
    @WithDefault("3")
    int failureRate();

    /**
     * Face matching configuration.
     */
    @WithName("face-match")
    FaceMatch faceMatch();

    interface Latency {
        @WithDefault("100")
        int min();

        @WithDefault("800")
        int max();
    }

    interface FaceMatch {
        /**
         * Threshold for successful face match (0-100).
         */
        @WithDefault("75")
        int threshold();

        /**
         * Random variance added to match scores.
         */
        @WithDefault("10")
        int variance();
    }
}
