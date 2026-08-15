package id.payu.wallet.adapter.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.wallet.interfaces.dto.RefundRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundRequestedConsumer {

    private final ObjectMapper objectMapper;
    private final RefundReversalExecutor executor;

    @KafkaListener(
            topics = "${payu.refund-reversal.topic:payu.dispute.refund-requested.v1}",
            groupId = "${payu.refund-reversal.group-id:wallet-service-refund-reversal}",
            // outbox publishes JSON strings; the global JacksonJsonDeserializer
            // cannot read them without type headers (ARCH-TOPIC-003)
            properties = "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode data = root.has("data") ? root.get("data") : root;
            executor.execute(objectMapper.treeToValue(data, RefundRequestedEvent.class));
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid RefundRequested event", exception);
        }
    }
}
