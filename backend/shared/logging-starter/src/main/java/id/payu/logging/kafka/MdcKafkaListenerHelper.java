package id.payu.logging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * Helper for per-record MDC propagation in {@code @KafkaListener} methods.
 *
 * <p>The {@link MdcKafkaConsumerInterceptor} sets MDC at batch level (first record).
 * When processing individual records that may have different correlation IDs,
 * wrap your listener logic with this helper:</p>
 *
 * <pre>{@code
 * @KafkaListener(topics = "events")
 * public void handle(ConsumerRecord<String, String> record) {
 *     MdcKafkaListenerHelper.withMdc(record, () -> {
 *         log.info("Processing event");  // logs with per-record correlation_id
 *     });
 * }
 * }</pre>
 */
public final class MdcKafkaListenerHelper {

    private static final String DEFAULT_HEADER_NAME = "X-Correlation-Id";
    private static final String DEFAULT_MDC_KEY = "correlation_id";

    private MdcKafkaListenerHelper() {
        // utility class
    }

    /**
     * Execute a runnable with MDC set from the record's correlation header.
     * Restores the previous MDC value after execution.
     */
    public static void withMdc(ConsumerRecord<?, ?> record, Runnable action) {
        withMdc(record, DEFAULT_HEADER_NAME, DEFAULT_MDC_KEY, action);
    }

    /**
     * Execute a runnable with MDC set from a custom header/key.
     */
    public static void withMdc(ConsumerRecord<?, ?> record, String headerName,
                               String mdcKey, Runnable action) {
        String previous = MDC.get(mdcKey);
        try {
            String correlationId = extractHeader(record, headerName);
            if (correlationId != null) {
                MDC.put(mdcKey, correlationId);
            }
            action.run();
        } finally {
            if (previous != null) {
                MDC.put(mdcKey, previous);
            } else {
                MDC.remove(mdcKey);
            }
        }
    }

    /**
     * Execute a supplier with MDC set from the record's correlation header.
     * Restores the previous MDC value after execution.
     */
    public static <T> T withMdc(ConsumerRecord<?, ?> record, Supplier<T> supplier) {
        return withMdc(record, DEFAULT_HEADER_NAME, DEFAULT_MDC_KEY, supplier);
    }

    /**
     * Execute a supplier with MDC set from a custom header/key.
     */
    public static <T> T withMdc(ConsumerRecord<?, ?> record, String headerName,
                                String mdcKey, Supplier<T> supplier) {
        String previous = MDC.get(mdcKey);
        try {
            String correlationId = extractHeader(record, headerName);
            if (correlationId != null) {
                MDC.put(mdcKey, correlationId);
            }
            return supplier.get();
        } finally {
            if (previous != null) {
                MDC.put(mdcKey, previous);
            } else {
                MDC.remove(mdcKey);
            }
        }
    }

    private static String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return null;
    }
}
