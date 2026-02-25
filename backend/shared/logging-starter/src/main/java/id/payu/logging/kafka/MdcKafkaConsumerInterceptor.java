package id.payu.logging.kafka;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka ConsumerInterceptor that extracts correlation_id from Kafka headers
 * and sets it in MDC for log correlation.
 *
 * <p>Note: This sets MDC for the consumer poll thread. For per-record
 * correlation in listener methods, use {@link MdcKafkaListenerHelper} instead.</p>
 *
 * <p>Register via:</p>
 * <pre>
 * spring.kafka.consumer.properties.interceptor.classes=id.payu.logging.kafka.MdcKafkaConsumerInterceptor
 * </pre>
 */
public class MdcKafkaConsumerInterceptor implements ConsumerInterceptor<Object, Object> {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlation_id";

    @Override
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        // Extract correlation_id from the first record (batch may contain many)
        records.forEach(record -> {
            Header header = record.headers().lastHeader(CORRELATION_ID_HEADER);
            if (header != null) {
                String correlationId = new String(header.value(), StandardCharsets.UTF_8);
                MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            }
        });
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // no-op
    }
}
