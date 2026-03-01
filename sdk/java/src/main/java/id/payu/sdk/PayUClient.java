package id.payu.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.payu.sdk.auth.AuthInterceptor;
import id.payu.sdk.config.PayUConfig;
import id.payu.sdk.error.PayUError;
import id.payu.sdk.error.PayUException;
import id.payu.sdk.interceptor.RetryInterceptor;
import id.payu.sdk.resource.PaymentsResource;
import id.payu.sdk.resource.TransfersResource;
import id.payu.sdk.resource.WalletsResource;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Main client for PayU Payment Gateway API.
 *
 * <p>Provides authenticated access to all PayU endpoints with
 * automatic retry logic and comprehensive error handling.
 *
 * <p>Example usage:
 * <pre>{@code
 * PayUClient client = PayUClient.builder()
 *     .apiKey("your-api-key")
 *     .apiSecret("your-api-secret")
 *     .environment(PayUEnvironment.SANDBOX)
 *     .build();
 *
 * PaymentResponse payment = client.payments().create(
 *     CreatePaymentRequest.builder()
 *         .amount(100000)
 *         .currency("IDR")
 *         .description("Test payment")
 *         .build()
 * );
 * }</pre>
 */
public class PayUClient {

    private static final Logger log = LoggerFactory.getLogger(PayUClient.class);

    private final PayUConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Lazy-loaded resource clients
    private PaymentsResource payments;
    private TransfersResource transfers;
    private WalletsResource wallets;

    private PayUClient(Builder builder) {
        this.config = PayUConfig.builder()
                .apiKey(builder.apiKey)
                .apiSecret(builder.apiSecret)
                .environment(builder.environment != null ? builder.environment : PayUEnvironment.SANDBOX)
                .baseUrl(builder.baseUrl)
                .timeout(builder.timeout != null ? builder.timeout : 30000)
                .enableRetries(builder.enableRetries != null ? builder.enableRetries : true)
                .maxRetries(builder.maxRetries != null ? builder.maxRetries : 3)
                .build();

        this.objectMapper = createObjectMapper();
        this.httpClient = createHttpClient();
    }

    /**
     * Create a new builder for PayUClient.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    private OkHttpClient createHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getTimeout(), TimeUnit.MILLISECONDS);

        // Add auth interceptor
        builder.addInterceptor(new AuthInterceptor(config));

        // Add retry interceptor
        if (config.isEnableRetries()) {
            builder.addInterceptor(new RetryInterceptor(config.getMaxRetries()));
        }

        // Add logging interceptor in debug mode
        if (config.isDebug()) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(log::debug);
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }

        return builder.build();
    }

    /**
     * Execute a request and parse the response.
     *
     * @param request the HTTP request
     * @param responseType the expected response type
     * @param <T> the response type
     * @return the parsed response
     * @throws PayUException if the request fails
     */
    public <T> T execute(Request request, Class<T> responseType) throws PayUException {
        try (Response response = httpClient.newCall(request).execute()) {
            return handleResponse(response, responseType);
        } catch (IOException e) {
            throw new PayUException("Request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a request without parsing the response body.
     *
     * @param request the HTTP request
     * @throws PayUException if the request fails
     */
    public void execute(Request request) throws PayUException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw PayUError.fromResponse(response);
            }
        } catch (IOException e) {
            throw new PayUException("Request failed: " + e.getMessage(), e);
        }
    }

    private <T> T handleResponse(Response response, Class<T> responseType) throws PayUException {
        if (!response.isSuccessful()) {
            throw PayUError.fromResponse(response);
        }

        if (responseType == Void.class) {
            return null;
        }

        try {
            String body = response.body() != null ? response.body().string() : null;
            if (body == null || body.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(body, responseType);
        } catch (IOException e) {
            throw new PayUException("Failed to parse response: " + e.getMessage(), e);
        }
    }

    // Resource accessors

    /**
     * Access the Payments API.
     *
     * @return payments resource client
     */
    public PaymentsResource payments() {
        if (payments == null) {
            payments = new PaymentsResource(this, config);
        }
        return payments;
    }

    /**
     * Access the Transfers API.
     *
     * @return transfers resource client
     */
    public TransfersResource transfers() {
        if (transfers == null) {
            transfers = new TransfersResource(this, config);
        }
        return transfers;
    }

    /**
     * Access the Wallets API.
     *
     * @return wallets resource client
     */
    public WalletsResource wallets() {
        if (wallets == null) {
            wallets = new WalletsResource(this, config);
        }
        return wallets;
    }

    /**
     * Get the ObjectMapper for JSON serialization.
     *
     * @return the object mapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Get the HTTP client for advanced usage.
     *
     * @return the HTTP client
     */
    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Get the client configuration.
     *
     * @return the configuration
     */
    public PayUConfig getConfig() {
        return config;
    }

    /**
     * Builder for PayUClient.
     */
    public static class Builder {
        private String apiKey;
        private String apiSecret;
        private PayUEnvironment environment;
        private String baseUrl;
        private Integer timeout;
        private Boolean enableRetries;
        private Integer maxRetries;

        private Builder() {
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

        public Builder timeout(int timeout) {
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

        public PayUClient build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("API key is required");
            }
            if (apiSecret == null || apiSecret.isEmpty()) {
                throw new IllegalArgumentException("API secret is required");
            }
            return new PayUClient(this);
        }
    }
}
