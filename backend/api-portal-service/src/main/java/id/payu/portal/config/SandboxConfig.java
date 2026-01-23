package id.payu.portal.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "sandbox")
public interface SandboxConfig {

    LatencyConfig latency();

    interface LatencyConfig {
        long min();
        long max();
        boolean enabled();
    }
}
