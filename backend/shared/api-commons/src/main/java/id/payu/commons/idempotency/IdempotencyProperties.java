package id.payu.commons.idempotency;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration properties for Idempotency support.
 * <p>
 * Properties are prefixed with <code>payu.idempotency</code>.
 *
 * @see IdempotencyAutoConfiguration
 */
@Data
@ConfigurationProperties(prefix = "payu.idempotency")
public class IdempotencyProperties {

    /**
     * Whether idempotency support is enabled.
     */
    private boolean enabled = true;

    /**
     * Default TTL for idempotency entries in hours.
     */
    private int ttlHours = 24;

    /**
     * Header name for the idempotency key.
     */
    private String headerName = "Idempotency-Key";

    /**
     * Redis configuration.
     */
    private RedisProperties redis = new RedisProperties();

    /**
     * Interceptor configuration.
     */
    private InterceptorProperties interceptor = new InterceptorProperties();

    /**
     * Redis-specific properties.
     */
    @Data
    public static class RedisProperties {
        /**
         * Key prefix for Redis storage.
         */
        private String keyPrefix = "idempotency";

        /**
         * Whether to use Redis transactions for atomic operations.
         */
        private boolean useTransactions = true;
    }

    /**
     * Interceptor-specific properties.
     */
    @Data
    public static class InterceptorProperties {
        /**
         * Path pattern for interceptor registration.
         */
        private String pathPattern = "/api/**";

        /**
         * HTTP methods to apply idempotency to.
         */
        private List<String> methods = Arrays.asList("POST", "PUT", "PATCH");

        /**
         * Whether to cache error responses.
         */
        private boolean cacheErrors = true;
    }
}
