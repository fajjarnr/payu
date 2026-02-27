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
 * <p>This sets MDC from the <b>first record</b> in the polled batch.
 * For per-record MDC in listener methods, wrap processing with
 * {@link MdcKafkaListenerHelper#withMdc(org.apache.kafka.clients.consumer.ConsumerRecord, Runnable)}.</p>
 *
 * <p>Configuration keys (passed via Kafka consumer properties map):</p>
 * <ul>
 *   <li>{@code payu.mdc.header-name} — Kafka header to read (default: {@code X-Correlation-Id})</li>
 *   <li>{@code payu.mdc.mdc-key} — MDC key to set (default: {@code correlation_id})</li>
 * </ul>
 *
 * <p>Register via:</p>
 * <pre>
 * spring.kafka.consumer.properties.interceptor.classes=id.payu.logging.kafka.MdcKafkaConsumerInterceptor
 * </pre>
 */
public class MdcKafkaConsumerInterceptor implements ConsumerInterceptor<Object, Object> {

    static final String CONFIG_HEADER_NAME = "payu.mdc.header-name";
    static final String CONFIG_MDC_KEY = "payu.mdc.mdc-key";
    static final String DEFAULT_HEADER_NAME = "X-Correlation-Id";
    static final String DEFAULT_MDC_KEY = "correlation_id";

    private String headerName = DEFAULT_HEADER_NAME;
    private String mdcKey = DEFAULT_MDC_KEY;

    @Override
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        if (records.isEmpty()) {
            return records;
        }
        // Extract correlation_id from the first record only (batch-level MDC)
        var firstRecord = records.iterator().next();
        Header header = firstRecord.headers().lastHeader(headerName);
        if (header != null && header.value() != null) {
            String correlationId = new String(header.value(), StandardCharsets.UTF_8);
            MDC.put(mdcKey, correlationId);
        }
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // Clean up MDC after commit to avoid leaking between poll cycles
        MDC.remove(mdcKey);
    }

    @Override
    public void close() {
        MDC.remove(mdcKey);
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
