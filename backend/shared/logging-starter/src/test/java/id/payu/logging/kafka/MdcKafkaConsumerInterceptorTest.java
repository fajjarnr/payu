package id.payu.logging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class MdcKafkaConsumerInterceptorTest {

    private MdcKafkaConsumerInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new MdcKafkaConsumerInterceptor();
        interceptor.configure(Map.of());
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("onConsume")
    class OnConsume {

        @Test
        @DisplayName("should extract correlation_id from first record header")
        void shouldExtractFromFirstRecord() {
            ConsumerRecords<Object, Object> records = createRecords(
                    "first-id", "second-id", "third-id"
            );

            interceptor.onConsume(records);

            assertThat(MDC.get("correlation_id")).isEqualTo("first-id");
        }

        @Test
        @DisplayName("should not set MDC when records are empty")
        void shouldHandleEmptyRecords() {
            ConsumerRecords<Object, Object> records = ConsumerRecords.empty();

            interceptor.onConsume(records);

            assertThat(MDC.get("correlation_id")).isNull();
        }

        @Test
        @DisplayName("should not set MDC when header is missing")
        void shouldHandleMissingHeader() {
            ConsumerRecords<Object, Object> records = createRecordsWithoutHeader();

            interceptor.onConsume(records);

            assertThat(MDC.get("correlation_id")).isNull();
        }
    }

    @Nested
    @DisplayName("onCommit")
    class OnCommit {

        @Test
        @DisplayName("should clean up MDC on commit")
        void shouldCleanUpOnCommit() {
            MDC.put("correlation_id", "test-id");

            interceptor.onCommit(Map.of());

            assertThat(MDC.get("correlation_id")).isNull();
        }
    }

    @Nested
    @DisplayName("configure")
    class Configure {

        @Test
        @DisplayName("should use custom header name from config")
        void shouldUseCustomHeaderName() {
            interceptor.configure(Map.of(
                    "payu.mdc.header-name", "X-Request-Id",
                    "payu.mdc.mdc-key", "request_id"
            ));

            ConsumerRecords<Object, Object> records = createRecordsWithCustomHeader(
                    "X-Request-Id", "custom-123"
            );

            interceptor.onConsume(records);

            assertThat(MDC.get("request_id")).isEqualTo("custom-123");
            assertThat(MDC.get("correlation_id")).isNull();
        }

        @Test
        @DisplayName("should fall back to defaults when config is blank")
        void shouldFallBackToDefaults() {
            interceptor.configure(Map.of(
                    "payu.mdc.header-name", "   ",
                    "payu.mdc.mdc-key", ""
            ));

            ConsumerRecords<Object, Object> records = createRecords("fallback-id");
            interceptor.onConsume(records);

            assertThat(MDC.get("correlation_id")).isEqualTo("fallback-id");
        }
    }

    // --- Helper methods ---

    private ConsumerRecords<Object, Object> createRecords(String... correlationIds) {
        TopicPartition tp = new TopicPartition("test-topic", 0);
        List<ConsumerRecord<Object, Object>> list = new ArrayList<>();
        for (int i = 0; i < correlationIds.length; i++) {
            RecordHeaders headers = new RecordHeaders();
            headers.add("X-Correlation-Id", correlationIds[i].getBytes(StandardCharsets.UTF_8));
            list.add(new ConsumerRecord<>("test-topic", 0, i, 0L,
                    TimestampType.CREATE_TIME, 0, 0, "key", "value", headers, Optional.empty()));
        }
        return new ConsumerRecords<>(Map.of(tp, list));
    }

    private ConsumerRecords<Object, Object> createRecordsWithoutHeader() {
        TopicPartition tp = new TopicPartition("test-topic", 0);
        RecordHeaders headers = new RecordHeaders();
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("test-topic", 0, 0, 0L,
                TimestampType.CREATE_TIME, 0, 0, "key", "value", headers, Optional.empty());
        return new ConsumerRecords<>(Map.of(tp, List.of(record)));
    }

    private ConsumerRecords<Object, Object> createRecordsWithCustomHeader(String headerName, String value) {
        TopicPartition tp = new TopicPartition("test-topic", 0);
        RecordHeaders headers = new RecordHeaders();
        headers.add(headerName, value.getBytes(StandardCharsets.UTF_8));
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("test-topic", 0, 0, 0L,
                TimestampType.CREATE_TIME, 0, 0, "key", "value", headers, Optional.empty());
        return new ConsumerRecords<>(Map.of(tp, List.of(record)));
    }
}
