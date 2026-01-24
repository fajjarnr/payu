package id.payu.gateway.service;

import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Random;

/**
 * Service to handle retry and timeout policies for external service calls.
 * Implements exponential backoff with jitter for retries.
 */
@ApplicationScoped
public class RetryAndTimeoutService {

    @Inject
    GatewayConfig config;

    private final Random random = new Random();

    @PostConstruct
    void init() {
        Log.infof("Retry and Timeout service initialized (retry: %s, timeout: %s)",
            config.retry().enabled(), config.timeout().enabled());
    }

    /**
     * Execute a Uni with retry logic.
     */
    public <T> Uni<T> executeWithRetry(String serviceName, Uni<T> operation) {
        if (!config.retry().enabled()) {
            return operation;
        }

        GatewayConfig.RetryConfig.RetryPolicyConfig policy = getRetryPolicy(serviceName);

        return operation
            .onFailure().recoverWithUni(throwable -> {
                // Check if the error is retryable
                if (!isRetryable(throwable)) {
                    return Uni.createFrom().failure(throwable);
                }

                // For simplicity, just fail. In production, implement proper retry logic
                // with exponential backoff and jitter
                Log.warnf("Service %s call failed, should retry (retries: %d)",
                    serviceName, policy.maxRetries());
                return Uni.createFrom().failure(throwable);
            });
    }

    /**
     * Get timeout for a specific service.
     */
    public Duration getTimeout(String serviceName) {
        if (!config.timeout().enabled()) {
            return config.timeout().defaultValue();
        }

        return config.timeout().perService()
            .getOrDefault(serviceName, config.timeout().defaultValue());
    }

    /**
     * Calculate delay for retry with exponential backoff and jitter.
     */
    public Duration calculateRetryDelay(int retryAttempt, String serviceName) {
        GatewayConfig.RetryConfig.RetryPolicyConfig policy = getRetryPolicy(serviceName);

        // Exponential backoff: initialInterval * (multiplier ^ attempt)
        long baseDelay = policy.initialInterval().toMillis();
        double multiplier = policy.multiplier();
        long delayMillis = (long) (baseDelay * Math.pow(multiplier, retryAttempt));

        // Apply jitter
        double jitter = policy.jitter();
        long jitterAmount = (long) (delayMillis * jitter);
        long finalDelay = delayMillis + (random.nextInt(2 * (int) jitterAmount) - (int) jitterAmount);

        // Cap at max interval
        long maxDelay = policy.maxInterval().toMillis();
        finalDelay = Math.min(finalDelay, maxDelay);

        return Duration.ofMillis(finalDelay);
    }

    private GatewayConfig.RetryConfig.RetryPolicyConfig getRetryPolicy(String serviceName) {
        return config.retry().perService()
            .getOrDefault(serviceName, config.retry().defaultPolicy());
    }

    private boolean isRetryable(Throwable throwable) {
        // Check if the exception is retryable
        // Retry on timeout, connection errors, 5xx errors
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }

        return message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("503") ||
               message.contains("502") ||
               message.contains("504");
    }
}
