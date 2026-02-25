package id.payu.simulator.biller.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for biller simulator behavior.
 */
@ConfigMapping(prefix = "simulator")
public interface SimulatorConfig {

    Latency latency();

    @WithName("failure-rate")
    @WithDefault("3")
    int failureRate();

    interface Latency {
        @WithDefault("100")
        int min();

        @WithDefault("600")
        int max();
    }
}
