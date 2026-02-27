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
 * <p>Configuration keys (passed via Kafka producer properties map):</p>
 * <ul>
 *   <li>{@code payu.mdc.header-name} — Kafka header to write (default: {@code X-Correlation-Id})</li>
 *   <li>{@code payu.mdc.mdc-key} — MDC key to read (default: {@code correlation_id})</li>
 * </ul>
 *
 * <p>Register via:</p>
 * <pre>
 * spring.kafka.producer.properties.interceptor.classes=id.payu.logging.kafka.MdcKafkaProducerInterceptor
 * </pre>
 */
public class MdcKafkaProducerInterceptor implements ProducerInterceptor<Object, Object> {

    static final String CONFIG_HEADER_NAME = "payu.mdc.header-name";
    static final String CONFIG_MDC_KEY = "payu.mdc.mdc-key";
    static final String DEFAULT_HEADER_NAME = "X-Correlation-Id";
    static final String DEFAULT_MDC_KEY = "correlation_id";

    private String headerName = DEFAULT_HEADER_NAME;
    private String mdcKey = DEFAULT_MDC_KEY;

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        String correlationId = MDC.get(mdcKey);
        if (correlationId != null && !correlationId.isBlank()) {
            record.headers().add(headerName,
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
        Object headerCfg = configs.get(CONFIG_HEADER_NAME);
        if (headerCfg instanceof String s && !s.isBlank()) {
            this.headerName = s;
        }
        Object mdcCfg = configs.get(CONFIG_MDC_KEY);
        if (mdcCfg instanceof String s && !s.isBlank()) {
            this.mdcKey = s;
        }
    }
}
