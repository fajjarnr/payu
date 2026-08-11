package id.payu.partner.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.partner.application.service.WebhookDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka consumer for subscription lifecycle events.
 * Consumes from subscription.events topic and dispatches webhooks to registered partners.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventConsumer {

    private final WebhookDispatcherService webhookDispatcher;
    private final ObjectMapper objectMapper;

    public static final String SUBSCRIPTION_EVENTS_TOPIC = "subscription.events";

    /**
     * Consume subscription events and dispatch webhooks.
     * Accepts raw String records for compatibility with StringDeserializer.
     */
    @KafkaListener(
            topics = SUBSCRIPTION_EVENTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id:partner-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSubscriptionEvent(ConsumerRecord<String, String> record) {
        String value = record.value();
        if (value == null || value.isBlank()) {
            log.warn("Received null or invalid subscription event");
            return;
        }

        try {
            Map<String, Object> parsed = parseCloudEvent(value);

            String type = extractHeader(record, "X-Event-Type");
            if (type == null) type = (String) parsed.getOrDefault("type", "subscription.updated");

            Map<String, Object> payload = extractPayload(parsed);

            String pid = extractHeader(record, "X-PartnerEntity-Id");
            if (pid == null) pid = (String) payload.get("partnerId");
            if (pid == null) pid = (String) parsed.get("partnerId");
            payload.put("partnerId", pid);

            log.info("Consuming subscription event: type={}, partnerId={}", type, pid);

            webhookDispatcher.dispatch(type, payload);

            log.info("Dispatched subscription webhook: type={}, partnerId={}", type, pid);
        } catch (RuntimeException e) {
            // PARTNER-PROD-004: never swallow a processing exception — rethrow so
            // the Kafka error handler retries and forwards the record to <topic>.dlq
            // instead of committing the offset and losing the event.
            log.error("Failed to process subscription event: error={}", e.getMessage(), e);
            throw e;
        }
    }

    private String extractHeader(ConsumerRecord<String, String> record, String headerName) {
        if (record.headers() != null) {
            var header = record.headers().lastHeader(headerName);
            if (header != null) {
                return new String(header.value());
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseCloudEvent(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            // PARTNER-PROD-004: malformed payloads must fail the record (retry + DLQ).
            throw new IllegalArgumentException("Invalid subscription event payload JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayload(Map<String, Object> parsed) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // If CloudEvent envelope, extract data + metadata
        if (parsed.containsKey("specversion") && parsed.containsKey("data")) {
            payload.put("eventId", parsed.get("id"));
            payload.put("eventTime", parsed.get("time"));
            Object data = parsed.get("data");
            if (data instanceof Map) {
                payload.putAll((Map<String, Object>) data);
            } else {
                payload.put("data", data);
            }
        } else {
            // Plain JSON payload
            payload.putAll(parsed);
        }
        return payload;
    }
}
