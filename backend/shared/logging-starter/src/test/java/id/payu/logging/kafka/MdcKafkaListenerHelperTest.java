package id.payu.logging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MdcKafkaListenerHelperTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("withMdc (Runnable)")
    class WithMdcRunnable {

        @Test
        @DisplayName("should set MDC from record header during execution")
        void shouldSetMdcDuringExecution() {
            ConsumerRecord<String, String> record = createRecord("X-Correlation-Id", "listener-id-1");

            MdcKafkaListenerHelper.withMdc(record, () -> {
                assertThat(MDC.get("correlation_id")).isEqualTo("listener-id-1");
            });
        }

        @Test
        @DisplayName("should restore previous MDC value after execution")
        void shouldRestorePreviousMdc() {
            MDC.put("correlation_id", "previous-value");

            ConsumerRecord<String, String> record = createRecord("X-Correlation-Id", "temp-id");
            MdcKafkaListenerHelper.withMdc(record, () -> {
                assertThat(MDC.get("correlation_id")).isEqualTo("temp-id");
            });

            assertThat(MDC.get("correlation_id")).isEqualTo("previous-value");
        }

        @Test
        @DisplayName("should remove MDC when no previous value existed")
        void shouldRemoveMdcWhenNoPrevious() {
            ConsumerRecord<String, String> record = createRecord("X-Correlation-Id", "temp-id");

            MdcKafkaListenerHelper.withMdc(record, () -> {});

            assertThat(MDC.get("correlation_id")).isNull();
        }

        @Test
        @DisplayName("should handle records without correlation header")
        void shouldHandleMissingHeader() {
            RecordHeaders headers = new RecordHeaders();
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "topic", 0, 0, 0L, TimestampType.CREATE_TIME,
                    0, 0, "key", "value", headers, Optional.empty());

            MdcKafkaListenerHelper.withMdc(record, () -> {
                assertThat(MDC.get("correlation_id")).isNull();
            });
        }

        @Test
        @DisplayName("should restore MDC even when action throws")
        void shouldRestoreOnException() {
            MDC.put("correlation_id", "safe-value");
            ConsumerRecord<String, String> record = createRecord("X-Correlation-Id", "throwing");

            try {
                MdcKafkaListenerHelper.withMdc(record, () -> {
                    throw new RuntimeException("boom");
                });
            } catch (RuntimeException ignored) {
            }

            assertThat(MDC.get("correlation_id")).isEqualTo("safe-value");
        }
    }

    @Nested
    @DisplayName("withMdc (Supplier)")
    class WithMdcSupplier {

        @Test
        @DisplayName("should return supplier result with MDC set")
        void shouldReturnSupplierResult() {
            ConsumerRecord<String, String> record = createRecord("X-Correlation-Id", "supply-id");

            String result = MdcKafkaListenerHelper.withMdc(record, () -> {
                return "processed-" + MDC.get("correlation_id");
            });

            assertThat(result).isEqualTo("processed-supply-id");
        }
    }

    @Nested
    @DisplayName("Custom header/key")
    class CustomConfig {

        @Test
        @DisplayName("should use custom header name and MDC key")
        void shouldUseCustomHeaderAndKey() {
            ConsumerRecord<String, String> record = createRecord("X-Trace-Id", "trace-abc");

            MdcKafkaListenerHelper.withMdc(record, "X-Trace-Id", "trace_id", () -> {
                assertThat(MDC.get("trace_id")).isEqualTo("trace-abc");
                assertThat(MDC.get("correlation_id")).isNull();
            });
        }
    }

    private ConsumerRecord<String, String> createRecord(String headerName, String headerValue) {
        RecordHeaders headers = new RecordHeaders();
        headers.add(headerName, headerValue.getBytes(StandardCharsets.UTF_8));
        return new ConsumerRecord<>("topic", 0, 0, 0L, TimestampType.CREATE_TIME,
                0, 0, "key", "value", headers, Optional.empty());
    }
}
