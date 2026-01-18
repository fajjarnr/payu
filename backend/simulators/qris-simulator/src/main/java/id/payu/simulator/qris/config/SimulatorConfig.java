package id.payu.simulator.qris.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for QRIS simulator behavior.
 */
@ConfigMapping(prefix = "simulator")
public interface SimulatorConfig {

    Latency latency();

    @WithName("failure-rate")
    @WithDefault("2")
    int failureRate();

    @WithName("qr")
    QrConfig qr();

    Webhook webhook();

    interface Latency {
        @WithDefault("50")
        int min();

        @WithDefault("300")
        int max();
    }

    interface QrConfig {
        @WithName("expiry-seconds")
        @WithDefault("300")
        int expirySeconds();

        @WithName("image-size")
        @WithDefault("300")
        int imageSize();

        @WithDefault("PNG")
        String format();
    }

    interface Webhook {
        @WithDefault("true")
        boolean enabled();

        @WithName("delay-ms")
        @WithDefault("1000")
        int delayMs();

        @WithName("retry-count")
        @WithDefault("3")
        int retryCount();
    }
}
