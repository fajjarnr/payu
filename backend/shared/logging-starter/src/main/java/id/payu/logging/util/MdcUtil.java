package id.payu.logging.util;

import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Utility class for programmatic MDC manipulation.
 * Provides methods for setting/retrieving MDC values with proper cleanup.
 */
public class MdcUtil {

    private static final String CORRELATION_ID_KEY = "correlation_id";

    /**
     * Execute a runnable with a new correlation ID, cleaning up after execution.
     */
    public void withCorrelationId(Runnable runnable) {
        withCorrelationId(generateCorrelationId(), runnable);
    }

    /**
     * Execute a runnable with a specific correlation ID, cleaning up after execution.
     */
    public void withCorrelationId(String correlationId, Runnable runnable) {
        String previous = MDC.get(CORRELATION_ID_KEY);
        try {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            runnable.run();
        } finally {
            if (previous != null) {
                MDC.put(CORRELATION_ID_KEY, previous);
            } else {
                MDC.remove(CORRELATION_ID_KEY);
            }
        }
    }

    /**
     * Execute a supplier with a new correlation ID, cleaning up after execution.
     */
    public <T> T withCorrelationId(Supplier<T> supplier) {
        return withCorrelationId(generateCorrelationId(), supplier);
    }

    /**
     * Execute a supplier with a specific correlation ID, cleaning up after execution.
     */
    public <T> T withCorrelationId(String correlationId, Supplier<T> supplier) {
        String previous = MDC.get(CORRELATION_ID_KEY);
        try {
            MDC.put(CORRELATION_ID_KEY, correlationId);
            return supplier.get();
        } finally {
            if (previous != null) {
                MDC.put(CORRELATION_ID_KEY, previous);
            } else {
                MDC.remove(CORRELATION_ID_KEY);
            }
        }
    }

    /**
     * Get the current correlation ID from MDC.
     */
    public String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    /**
     * Set a value in MDC.
     */
    public void put(String key, String value) {
        MDC.put(key, value);
    }

    /**
     * Get a value from MDC.
     */
    public String get(String key) {
        return MDC.get(key);
    }

    /**
     * Remove a value from MDC.
     */
    public void remove(String key) {
        MDC.remove(key);
    }

    /**
     * Clear all MDC values.
     */
    public void clear() {
        MDC.clear();
    }

    /**
     * Get a copy of the current MDC context map.
     */
    public Map<String, String> getCopyOfContextMap() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * Set the MDC context map.
     */
    public void setContextMap(Map<String, String> contextMap) {
        MDC.setContextMap(contextMap);
    }

    /**
     * Generate a new correlation ID.
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
