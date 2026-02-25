package id.payu.logging.kafka;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka ProducerInterceptor that propagates MDC correlation_id
 * into Kafka record headers for downstream consumers.
 *
 * <p>Register via:</p>
 * <pre>
 * spring.kafka.producer.properties.interceptor.classes=id.payu.logging.kafka.MdcKafkaProducerInterceptor
 * </pre>
 */
public class MdcKafkaProducerInterceptor implements ProducerInterceptor<Object, Object> {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlation_id";

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            record.headers().add(CORRELATION_ID_HEADER,
                    correlationId.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
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
