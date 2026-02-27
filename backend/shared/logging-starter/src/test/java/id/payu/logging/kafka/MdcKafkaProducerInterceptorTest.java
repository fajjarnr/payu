package id.payu.logging.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MdcKafkaProducerInterceptorTest {

    private MdcKafkaProducerInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new MdcKafkaProducerInterceptor();
        interceptor.configure(Map.of());
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("onSend")
    class OnSend {

        @Test
        @DisplayName("should add correlation_id header from MDC")
        void shouldAddCorrelationHeader() {
            MDC.put("correlation_id", "produce-test-id");
            ProducerRecord<Object, Object> record = new ProducerRecord<>("test-topic", "key", "value");

            ProducerRecord<Object, Object> result = interceptor.onSend(record);

            assertThat(result.headers().lastHeader("X-Correlation-Id")).isNotNull();
            String headerValue = new String(result.headers().lastHeader("X-Correlation-Id").value(),
                    StandardCharsets.UTF_8);
            assertThat(headerValue).isEqualTo("produce-test-id");
        }

        @Test
        @DisplayName("should not add header when MDC is empty")
        void shouldNotAddHeaderWhenMdcEmpty() {
            ProducerRecord<Object, Object> record = new ProducerRecord<>("test-topic", "key", "value");

            ProducerRecord<Object, Object> result = interceptor.onSend(record);

            assertThat(result.headers().lastHeader("X-Correlation-Id")).isNull();
        }

        @Test
        @DisplayName("should not add header when MDC value is blank")
        void shouldNotAddHeaderWhenMdcBlank() {
            MDC.put("correlation_id", "   ");
            ProducerRecord<Object, Object> record = new ProducerRecord<>("test-topic", "key", "value");

            ProducerRecord<Object, Object> result = interceptor.onSend(record);

            assertThat(result.headers().lastHeader("X-Correlation-Id")).isNull();
        }
    }

    @Nested
    @DisplayName("configure")
    class Configure {

        @Test
        @DisplayName("should use custom header name and MDC key")
        void shouldUseCustomConfig() {
            interceptor.configure(Map.of(
                    "payu.mdc.header-name", "X-Trace-Id",
                    "payu.mdc.mdc-key", "trace_id"
            ));
            MDC.put("trace_id", "custom-trace-123");

            ProducerRecord<Object, Object> record = new ProducerRecord<>("test-topic", "key", "value");
            ProducerRecord<Object, Object> result = interceptor.onSend(record);

            assertThat(result.headers().lastHeader("X-Trace-Id")).isNotNull();
            String headerValue = new String(result.headers().lastHeader("X-Trace-Id").value(),
                    StandardCharsets.UTF_8);
            assertThat(headerValue).isEqualTo("custom-trace-123");
            assertThat(result.headers().lastHeader("X-Correlation-Id")).isNull();
        }

        @Test
        @DisplayName("should ignore blank config values")
        void shouldIgnoreBlankConfig() {
            interceptor.configure(Map.of(
                    "payu.mdc.header-name", "",
                    "payu.mdc.mdc-key", "  "
            ));
            MDC.put("correlation_id", "default-key-test");

            ProducerRecord<Object, Object> record = new ProducerRecord<>("test-topic", "key", "value");
            ProducerRecord<Object, Object> result = interceptor.onSend(record);

            // Should use defaults
            assertThat(result.headers().lastHeader("X-Correlation-Id")).isNotNull();
        }
    }
}
