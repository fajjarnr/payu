package id.payu.sdk.config;

import id.payu.sdk.PayUEnvironment;

/**
 * Immutable configuration for the PayU Java SDK.
 */
public class PayUConfig {

    private final String apiKey;
    private final String apiSecret;
    private final PayUEnvironment environment;
    private final String baseUrl;
    private final long timeout;
    private final boolean enableRetries;
    private final int maxRetries;
    private final boolean debug;

    private PayUConfig(Builder builder) {
        this.apiKey = builder.apiKey;
        this.apiSecret = builder.apiSecret;
        this.environment = builder.environment;
        this.baseUrl = builder.baseUrl;
        this.timeout = builder.timeout;
        this.enableRetries = builder.enableRetries;
        this.maxRetries = builder.maxRetries;
        this.debug = builder.debug;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public PayUEnvironment getEnvironment() {
        return environment;
    }

    public String getBaseUrl() {
        return baseUrl != null ? baseUrl : environment.getBaseUrl();
    }

    public long getTimeout() {
        return timeout;
    }

    public boolean isEnableRetries() {
        return enableRetries;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public boolean isDebug() {
        return debug;
    }

    public static class Builder {
        private String apiKey;
        private String apiSecret;
        private PayUEnvironment environment = PayUEnvironment.SANDBOX;
        private String baseUrl;
        private long timeout = 30000;
        private boolean enableRetries = true;
        private int maxRetries = 3;
        private boolean debug = false;

        Builder() {
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder apiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
            return this;
        }

        public Builder environment(PayUEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder enableRetries(boolean enableRetries) {
            this.enableRetries = enableRetries;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public PayUConfig build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("apiKey is required");
            }
            if (apiSecret == null || apiSecret.isEmpty()) {
                throw new IllegalArgumentException("apiSecret is required");
            }
            return new PayUConfig(this);
        }
    }
}
