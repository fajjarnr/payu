package id.payu.gateway.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.time.Duration;
import java.util.Map;

/**
 * Gateway configuration mapping.
 */
@ConfigMapping(prefix = "gateway")
public interface GatewayConfig {

    /**
     * Backend service configurations.
     */
    Map<String, ServiceConfig> services();

    /**
     * Simulator configurations.
     */
    Map<String, ServiceConfig> simulators();

    /**
     * Rate limiting configuration.
     */
    @WithName("rate-limit")
    RateLimitConfig rateLimit();

    /**
     * Circuit breaker configuration.
     */
    @WithName("circuit-breaker")
    CircuitBreakerConfig circuitBreaker();

    /**
     * CORS configuration.
     */
    CorsConfig cors();

    /**
     * Request/Response logging configuration.
     */
    LoggingConfig logging();

    interface ServiceConfig {
        String url();

        @WithDefault("30s")
        Duration timeout();
    }

    interface RateLimitConfig {
        @WithDefault("true")
        boolean enabled();

        @WithName("default")
        RateLimitRule defaultRule();

        Map<String, RateLimitRule> endpoints();
    }

    interface RateLimitRule {
        @WithName("requests-per-minute")
        @WithDefault("60")
        int requestsPerMinute();

        @WithDefault("100")
        int burst();
    }

    interface CircuitBreakerConfig {
        @WithDefault("true")
        boolean enabled();

        @WithName("failure-ratio")
        @WithDefault("0.5")
        double failureRatio();

        @WithDefault("30s")
        Duration delay();

        @WithName("success-threshold")
        @WithDefault("3")
        int successThreshold();
    }

    interface LoggingConfig {
        @WithDefault("true")
        boolean enabled();

        @WithName("log-body")
        @WithDefault("false")
        boolean logBody();

        @WithName("max-body-size")
        @WithDefault("1024")
        int maxBodySize();
    }

    interface CorsConfig {
        @WithDefault("true")
        boolean enabled();

        @WithDefault("")
        String allowedOrigins();

        @WithDefault("")
        String allowedMethods();

        @WithDefault("")
        String allowedHeaders();

        @WithDefault("")
        String exposedHeaders();

        @WithDefault("false")
        boolean allowCredentials();

        @WithName("max-age")
        @WithDefault("3600")
        int maxAge();
    }
}
