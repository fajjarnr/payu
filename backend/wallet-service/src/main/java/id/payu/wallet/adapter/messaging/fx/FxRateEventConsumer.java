package id.payu.wallet.adapter.messaging.fx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes FX rate updates from Kafka.
 * Used to break circular dependency between wallet-service and fx-service.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FxRateEventConsumer {

    private final FxRateCache fxRateCache;
    private final ObjectMapper objectMapper;

    /**
     * Consume FX rates updated event.
     * <p>
     * ARCH-TOPIC-003: listens on the outbox-published topic
     * {@code payu.fx.rates-updated.v1} and unwraps the CloudEvents envelope
     * ({@code data} holds the FxRatesUpdatedEvent), like RefundRequestedConsumer.
     *
     * @param record the FX rates updated event record
     */
    @KafkaListener(
            topics = "payu.fx.rates-updated.v1",
            groupId = "${spring.kafka.consumer.group-id:wallet-service-group}",
            // outbox publishes JSON strings; the global JacksonJsonDeserializer
            // cannot read them without type headers (ARCH-TOPIC-003)
            properties = "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer")
    public void onFxRatesUpdated(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode data = root.has("data") ? root.get("data") : root;
            FxRatesUpdatedEvent event = objectMapper.treeToValue(data, FxRatesUpdatedEvent.class);

            log.debug("Received FX rates updated event: {} with {} rates",
                    event.getEventId(), event.getRates() != null ? event.getRates().size() : 0);

            fxRateCache.updateRates(event);

            log.info("Processed FX rates updated event, cache size: {}", fxRateCache.size());
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid FxRatesUpdated event", exception);
        }
    }
}
