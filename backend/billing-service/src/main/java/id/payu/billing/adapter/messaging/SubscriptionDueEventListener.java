package id.payu.billing.adapter.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.billing.domain.port.in.SubscriptionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * ARCH-BILL-001: Kafka consumer for the outbox-published subscription-due
 * trigger. Replaces the Artemis scheduled queue (payu.billing.scheduled).
 * The service re-checks due-ness, so an early event is a no-op.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionDueEventListener {

    private static final String TOPIC = "payu.billing.subscription-due.v1";

    private final SubscriptionUseCase subscriptionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC, groupId = "billing-subscription-due")
    public void onSubscriptionDue(String payload) {
        log.info("Received subscription-due event");
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode idNode = node.has("data") && node.get("data").isObject()
                    ? node.get("data").get("subscriptionId") : node.get("subscriptionId");
            if (idNode == null || idNode.isNull() || idNode.asText().isBlank()) {
                throw new IllegalArgumentException("subscriptionId missing in subscription-due event");
            }
            UUID subscriptionId = UUID.fromString(idNode.asText());
            subscriptionService.processScheduledCharge(subscriptionId);
        } catch (Exception e) {
            log.error("Failed to process subscription-due event: {}", e.getMessage(), e);
            throw new RuntimeException("Scheduled billing execution failed, rollback to DLQ", e);
        }
    }
}
