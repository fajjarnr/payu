package id.payu.grpc.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for gRPC starter.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "payu.grpc")
public class GrpcStarterProperties {

    /**
     * Server configuration
     */
    private Server server = new Server();

    /**
     * Client configurations per service
     */
    private Map<String, ClientConfig> clients = new HashMap<>();

    /**
     * Interceptor configuration
     */
    private Interceptor interceptors = new Interceptor();

    @Data
    public static class Server {
        /**
         * Whether to enable gRPC server
         */
        private boolean enabled = true;

        /**
         * gRPC server port
         */
        private int port = 9090;

        /**
         * Whether to enable reflection (disable in production)
         */
        private boolean reflectionEnabled = false;

        /**
         * Maximum message size in bytes
         */
        private int maxMessageSize = 4194304; // 4MB

        /**
         * Security configuration
         */
        private Security security = new Security();

        @Data
        public static class Security {
            /**
             * Whether TLS is enabled
             * (typically disabled when using Istio for TLS termination)
             */
            private boolean enabled = false;

            /**
             * Path to certificate file (if TLS enabled)
             */
            private String certificatePath;

            /**
             * Path to private key file (if TLS enabled)
             */
            private String privateKeyPath;
        }
    }

    @Data
    public static class ClientConfig {
        /**
         * Service address (e.g., "static://localhost:9090")
         */
        private String address;

        /**
         * Negotiation type (PLAINTEXT or TLS)
         */
        private String negotiationType = "PLAINTEXT";

        /**
         * Whether to enable retry
         */
        private boolean retryEnabled = true;

        /**
         * Maximum retry attempts
         */
        private int maxRetryAttempts = 3;

        /**
         * Initial backoff in milliseconds
         */
        private long initialBackoffMs = 100;

        /**
         * Maximum backoff in milliseconds
         */
        private long maxBackoffMs = 5000;

        /**
         * Connection timeout in seconds
         */
        private int connectionTimeoutSeconds = 10;

        /**
         * Deadline timeout in seconds
         */
        private int deadlineSeconds = 30;
    }

    @Data
    public static class Interceptor {
        /**
         * Tracing interceptor configuration
         */
        private Tracing tracing = new Tracing();

        /**
         * Auth interceptor configuration
         */
        private Auth auth = new Auth();

        /**
         * Error handling interceptor configuration
         */
        private ErrorHandling errorHandling = new ErrorHandling();

        @Data
        public static class Tracing {
            private boolean enabled = true;
        }

        @Data
        public static class Auth {
            private boolean enabled = true;

            /**
             * GRPC-014: when true, gRPC calls without a valid Bearer token are
             * rejected with UNAUTHENTICATED. Default false for compatibility —
             * service-to-service calls do not carry tokens yet (mesh mTLS is the
             * live control). Turn on per-server once clients send tokens.
             */
            private boolean requireToken = false;
        }

        @Data
        public static class ErrorHandling {
            private boolean enabled = true;
        }
    }
}
