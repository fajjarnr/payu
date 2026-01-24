package id.payu.gateway.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /**
     * Tenant configuration for multitenancy support.
     */
    @WithName("tenant")
    TenantConfig tenant();

    /**
     * API versioning configuration.
     */
    VersioningConfig versioning();

    /**
     * Request validation configuration.
     */
    ValidationConfig validation();

    /**
     * Response compression configuration.
     */
    CompressionConfig compression();

    /**
     * API analytics configuration.
     */
    AnalyticsConfig analytics();

    /**
     * Enhanced rate limiting configuration (bucket4j).
     */
    @WithName("rate-limit-v2")
    RateLimitV2Config rateLimitV2();

    /**
     * API key management configuration.
     */
    @WithName("api-keys")
    ApiKeyConfig apiKeys();

    /**
     * Request signing configuration.
     */
    @WithName("request-signing")
    RequestSigningConfig requestSigning();

    /**
     * IP whitelist configuration.
     */
    @WithName("ip-whitelist")
    IpWhitelistConfig ipWhitelist();

    /**
     * Retry policies configuration.
     */
    RetryConfig retry();

    /**
     * Timeout policies configuration.
     */
    TimeoutConfig timeout();

    /**
     * Idempotency configuration.
     */
    IdempotencyConfig idempotency();

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

    interface TenantConfig {
        @WithDefault("true")
        boolean enabled();

        @WithName("default-tenant-id")
        @WithDefault("default")
        String defaultTenantId();

        @WithDefault("false")
        @WithName("strict-mode")
        boolean strictMode();

        @WithName("header-name")
        @WithDefault("X-Tenant-Id")
        String headerName();
    }

    interface VersioningConfig {
        @WithDefault("true")
        boolean enabled();

        @WithName("default-version")
        @WithDefault("v1")
        String defaultVersion();

        @WithName("supported-versions")
        @WithDefault("v1,v2")
        List<String> supportedVersions();

        @WithName("header-name")
        @WithDefault("X-API-Version")
        String headerName();

        @WithName("deprecated-versions")
        List<String> deprecatedVersions();
    }

    interface ValidationConfig {
        @WithDefault("true")
        boolean enabled();

        @WithName("schema-validation")
        @WithDefault("true")
        boolean schemaValidation();

        @WithName("max-request-size")
        @WithDefault("10485760")
        long maxRequestSize();

        @WithDefault("false")
        @WithName("strict-mode")
        boolean strictMode();
    }

    interface CompressionConfig {
        @WithDefault("true")
        boolean enabled();

        @WithName("min-size")
        @WithDefault("1024")
        int minSize();

        @WithDefault("gzip,br")
        List<String> algorithms();

        @WithDefault("6")
        int level();

        @WithName("mime-types")
        @WithDefault("application/json,application/xml,text/html,text/plain,text/css,text/javascript")
        List<String> mimeTypes();
    }

    interface AnalyticsConfig {
        @WithDefault("true")
        boolean enabled();

        @WithName("retention-days")
        @WithDefault("90")
        int retentionDays();

        @WithName("batch-size")
        @WithDefault("100")
        int batchSize();

        @WithName("flush-interval")
        @WithDefault("60s")
        Duration flushInterval();

        @WithDefault("request-count,response-time,error-rate,status-codes,user-agents,endpoints")
        List<String> track();
    }

    interface RateLimitV2Config {
        @WithDefault("true")
        boolean enabled();

        @WithDefault("token-bucket")
        String algorithm();

        @WithName("default")
        TokenBucketConfig defaultRule();

        @WithName("per-user")
        Optional<TokenBucketConfig> perUser();

        @WithName("per-ip")
        Optional<TokenBucketConfig> perIp();

        Map<String, TokenBucketConfig> endpoints();
    }

    interface TokenBucketConfig {
        @WithDefault("100")
        int capacity();

        @WithName("refill-tokens")
        @WithDefault("60")
        int refillTokens();

        @WithName("refill-duration")
        @WithDefault("60s")
        Duration refillDuration();
    }

    interface ApiKeyConfig {
        @WithDefault("true")
        boolean enabled();

        RotationConfig rotation();

        @WithName("header-name")
        @WithDefault("X-API-Key")
        String headerName();

        @WithName("bypass-paths")
        @WithDefault("/health,/q/")
        List<String> bypassPaths();

        interface RotationConfig {
            @WithDefault("true")
            boolean enabled();

            @WithName("auto-rotate-days")
            @WithDefault("90")
            int autoRotateDays();

            @WithName("warning-days")
            @WithDefault("7")
            int warningDays();

            @WithName("grace-period-days")
            @WithDefault("30")
            int gracePeriodDays();
        }
    }

    interface RequestSigningConfig {
        @WithDefault("true")
        boolean enabled();

        @WithDefault("HmacSHA256")
        String algorithm();

        @WithName("header-name")
        @WithDefault("X-Signature")
        String headerName();

        @WithName("timestamp-header")
        @WithDefault("X-Timestamp")
        String timestampHeader();

        @WithName("tolerance-seconds")
        @WithDefault("300")
        long toleranceSeconds();

        @WithName("required-paths")
        @WithDefault("/v1/partner/*,/api/v1/partners/*")
        List<String> requiredPaths();

        @WithName("partner-keys")
        Map<String, String> partnerKeys();
    }

    interface IpWhitelistConfig {
        @WithDefault("true")
        boolean enabled();

        @WithDefault("allow")
        String mode();

        List<IpWhitelistPathConfig> paths();

        @WithName("bypass-headers")
        @WithDefault("X-Bypass-IP-Check")
        List<String> bypassHeaders();

        interface IpWhitelistPathConfig {
            String pattern();

            List<String> ips();
        }
    }

    interface RetryConfig {
        @WithDefault("true")
        boolean enabled();

        @WithName("default")
        RetryPolicyConfig defaultPolicy();

        @WithName("per-service")
        Map<String, RetryPolicyConfig> perService();

        interface RetryPolicyConfig {
            @WithName("max-retries")
            @WithDefault("3")
            int maxRetries();

            @WithName("initial-interval")
            @WithDefault("1s")
            Duration initialInterval();

            @WithName("max-interval")
            @WithDefault("10s")
            Duration maxInterval();

            @WithDefault("2.0")
            double multiplier();

            @WithDefault("0.2")
            double jitter();
        }
    }

    interface TimeoutConfig {
        @WithDefault("true")
        boolean enabled();

        @WithDefault("30s")
        Duration defaultValue();

        @WithName("per-service")
        Map<String, Duration> perService();
    }

    interface IdempotencyConfig {
        @WithDefault("true")
        boolean enabled();

        @WithName("header-name")
        @WithDefault("X-Idempotency-Key")
        String headerName();

        @WithDefault("24h")
        Duration ttl();

        @WithName("applicable-methods")
        @WithDefault("POST,PUT,PATCH,DELETE")
        List<String> applicableMethods();
    }
}
