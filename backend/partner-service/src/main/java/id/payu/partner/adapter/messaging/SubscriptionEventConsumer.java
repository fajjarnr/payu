package id.payu.partner.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.events.cloudevents.CloudEventEnvelope;
import id.payu.partner.application.service.WebhookDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

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
     *
     * @param event the CloudEvent envelope
     * @param eventType the event type from header
     * @param partnerId the partner ID from header
     */
    @KafkaListener(
            topics = SUBSCRIPTION_EVENTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id:partner-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSubscriptionEvent(
            @Payload CloudEventEnvelope<?> event,
            @Header(value = "X-Event-Type", required = false) String eventType,
            @Header(value = "X-Partner-Id", required = false) String partnerId) {

        if (event == null || event.getType() == null) {
            log.warn("Received null or invalid subscription event");
            return;
        }

        String type = eventType != null ? eventType : event.getType();
        String pid = partnerId != null ? partnerId : extractPartnerId(event);

        log.info("Consuming subscription event: type={}, partnerId={}, subject={}",
                type, pid, event.getSubject());

        try {
            // Convert CloudEvent data to Map for webhook dispatch
            Map<String, Object> payload = convertToMap(event.getData());

            // Add metadata for partner reference
            payload.put("eventId", event.getId().toString());
            payload.put("eventTime", event.getTime() != null ? event.getTime().toString() : null);
            payload.put("partnerId", pid);

            // Dispatch to all matching webhook subscriptions
            webhookDispatcher.dispatch(type, payload);

            log.info("Dispatched subscription webhook: type={}, partnerId={}", type, pid);
        } catch (Exception e) {
            log.error("Failed to process subscription event: type={}, error={}", type, e.getMessage(), e);
            // Don't rethrow - let Kafka offset commit to prevent infinite retry
        }
    }

    private String extractPartnerId(CloudEventEnvelope<?> event) {
        if (event.getData() instanceof Map) {
            Map<?, ?> data = (Map<?, ?>) event.getData();
            Object pid = data.get("partnerId");
            return pid != null ? pid.toString() : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object data) {
        if (data == null) {
            return Map.of();
        }
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        // Convert POJO to Map using ObjectMapper
        return objectMapper.convertValue(data, Map.class);
    }
}
