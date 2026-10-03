package id.payu.resilience.fallback;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A fallback implementation that returns static/predefined responses.
 * Useful for providing consistent error responses or default values
 * when services are unavailable.
 *
 * <p>Supports typed responses and exception-based response selection.
 *
 * <p>Usage example:
 * <pre>
 * StaticFallback<AccountBalance> fallback = StaticFallback.of(
 *     AccountBalance.builder()
 *         .amount(BigDecimal.ZERO)
 *         .currency("IDR")
 *         .status("UNAVAILABLE")
 *         .build()
 * );
 *
 * @CircuitBreaker(name = "balance-service", fallbackMethod = "getBalanceFallback")
 * public AccountBalance getBalance(String accountId) {
 *     return balanceService.getBalance(accountId);
 * }
 *
 * public AccountBalance getBalanceFallback(String accountId, Exception ex) {
 *     return fallback.provide(ex);
 * }
 * </pre>
 *
 * @param <T> the type of the static value
 */
@Slf4j
public class StaticFallback<T> implements FallbackProvider<T> {

    private final T defaultValue;
    private final Map<Class<? extends Exception>, T> exceptionMappings;
    private final Function<Exception, T> exceptionMapper;

    private StaticFallback(Builder<T> builder) {
        this.defaultValue = builder.defaultValue;
        this.exceptionMappings = new HashMap<>(builder.exceptionMappings);
        this.exceptionMapper = builder.exceptionMapper;
    }

    /**
     * Create a simple static fallback with a single value.
     *
     * @param value the static value to return
     * @param <T>   the type of the value
     * @return a StaticFallback that always returns the given value
     */
    public static <T> StaticFallback<T> of(T value) {
        return new Builder<T>().withDefault(value).build();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public T provide(Exception exception) {
        log.debug("Providing static fallback for exception: {}", exception.getClass().getSimpleName());

        if (exceptionMappings != null) {
            for (Map.Entry<Class<? extends Exception>, T> entry : exceptionMappings.entrySet()) {
                if (entry.getKey().isInstance(exception)) {
                    log.debug("Found specific fallback for exception type: {}", entry.getKey().getSimpleName());
                    return entry.getValue();
                }
            }
        }

        if (exceptionMapper != null) {
            try {
                return exceptionMapper.apply(exception);
            } catch (Exception e) {
                log.warn("Exception mapper failed, using default value", e);
            }
        }

        if (defaultValue != null) {
            return defaultValue;
        }

        throw new IllegalStateException("No fallback value configured for exception: " + exception.getMessage(), exception);
    }

    public static class Builder<T> {
        private T defaultValue;
        private final Map<Class<? extends Exception>, T> exceptionMappings = new HashMap<>();
        private Function<Exception, T> exceptionMapper;

        public Builder<T> withDefault(T value) {
            this.defaultValue = value;
            return this;
        }

        public <E extends Exception> Builder<T> mapException(Class<E> exceptionClass, T value) {
            this.exceptionMappings.put(exceptionClass, value);
            return this;
        }

        public Builder<T> withExceptionMapper(Function<Exception, T> mapper) {
            this.exceptionMapper = mapper;
            return this;
        }

        public StaticFallback<T> build() {
            return new StaticFallback<>(this);
        }
    }

    /**
     * Common static fallback values for financial operations.
     */
    public static class Financial {

        private Financial() {
        }

        public static Map<String, Object> serviceUnavailable(String serviceName) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "SERVICE_UNAVAILABLE");
            response.put("errorCode", "FALLBACK_001");
            response.put("service", serviceName);
            response.put("message", serviceName + " is temporarily unavailable. Please try again later.");
            response.put("retryAfter", "30s");
            return response;
        }

        public static Map<String, Object> degradedResponse(String operation) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "DEGRADED_MODE");
            response.put("errorCode", "FALLBACK_002");
            response.put("operation", operation);
            response.put("message", operation + " is operating in degraded mode. Some features may be limited.");
            return response;
        }

        @SuppressWarnings("unchecked")
        public static <T> java.util.List<T> emptyList() {
            return java.util.Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        public static <K, V> Map<K, V> emptyMap() {
            return java.util.Collections.emptyMap();
        }
    }
}
