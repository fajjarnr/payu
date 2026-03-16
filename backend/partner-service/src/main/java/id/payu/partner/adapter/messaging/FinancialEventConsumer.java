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
 * Multi-topic Kafka consumer for financial events that trigger outbound webhooks.
 * <p>
 * Listens to all financial event topics across the platform and routes them
 * to the WebhookDispatcherService, which delivers to partner webhook subscriptions
 * matching the event type.
 * <p>
 * This closes GAP-001 (Outbound Webhooks) by ensuring ALL financial events —
 * not just subscription.events — reach the webhook delivery engine.
 * <p>
 * Event type mapping:
 * <ul>
 *   <li>payu.transactions.* → transaction.initiated/validated/completed/failed</li>
 *   <li>payment-events → payment.* (completed/failed/pending/refunded)</li>
 *   <li>payment.expired → payment.expired</li>
 *   <li>wallet.balance.changed → wallet.balance.changed</li>
 *   <li>investment-events → investment.* (purchased/redeemed/matured)</li>
 *   <li>payu.split-bills.* → split-bill.created/completed/payment.made</li>
 *   <li>merchant.settlements → settlement.completed</li>
 *   <li>payment.link.events → payment-link.created/completed</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialEventConsumer {

    private final WebhookDispatcherService webhookDispatcher;
    private final ObjectMapper objectMapper;

    /**
     * Consume financial events from all relevant topics and dispatch to webhook subscribers.
     * <p>
     * Uses a single @KafkaListener with multiple topics for efficiency.
     * The event type is derived from the Kafka topic name or message headers.
     */
    @KafkaListener(
            topics = {
                    "payu.transactions.initiated",
                    "payu.transactions.validated",
                    "payu.transactions.completed",
                    "payu.transactions.failed",
                    "payment-events",
                    "payment.expired",
                    "wallet.balance.changed",
                    "investment-events",
                    "payu.split-bills.created",
                    "payu.split-bills.activated",
                    "payu.split-bills.cancelled",
                    "payu.split-bills.payment.made",
                    "payu.split-bills.completed",
                    "merchant.settlements",
                    "payment.link.events",
                    "escrow.held",
                    "escrow.released",
                    "escrow.settled",
                    "escrow.refunded",
                    "escrow.expired"
            },
            groupId = "partner-service-webhooks",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeFinancialEvent(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String value = record.value();

        if (value == null || value.isBlank()) {
            log.warn("Received null/empty message on topic: {}", topic);
            return;
        }

        String eventType = deriveEventType(topic, record);

        log.info("Consuming financial event: topic={}, eventType={}, key={}",
                topic, eventType, record.key());

        try {
            Map<String, Object> payload = parsePayload(value);

            // Extract event ID from payload if present, otherwise generate one
            String eventId = extractEventId(payload);

            // Add source metadata
            payload.put("_source_topic", topic);

            webhookDispatcher.dispatch(eventType, eventId, payload);

            log.info("Dispatched webhook for financial event: type={}, eventId={}",
                    eventType, eventId);
        } catch (Exception e) {
            log.error("Failed to process financial event: topic={}, eventType={}, error={}",
                    topic, eventType, e.getMessage(), e);
            // Don't rethrow — allow Kafka offset commit to prevent infinite retry.
            // Failed events are logged for manual investigation.
        }
    }

    /**
     * Derive a webhook event type from the Kafka topic name.
     * Maps internal topic names to partner-facing event types.
     */
    String deriveEventType(String topic, ConsumerRecord<String, String> record) {
        // Check for explicit event type in Kafka headers
        if (record.headers() != null) {
            var header = record.headers().lastHeader("X-Event-Type");
            if (header != null) {
                return new String(header.value());
            }
            header = record.headers().lastHeader("ce_type");
            if (header != null) {
                return new String(header.value());
            }
        }

        // Map topic to event type
        return switch (topic) {
            case "payu.transactions.initiated" -> "transaction.initiated";
            case "payu.transactions.validated" -> "transaction.validated";
            case "payu.transactions.completed" -> "transaction.completed";
            case "payu.transactions.failed" -> "transaction.failed";
            case "payment-events" -> "payment.updated";
            case "payment.expired" -> "payment.expired";
            case "wallet.balance.changed" -> "wallet.balance.changed";
            case "investment-events" -> "investment.updated";
            case "payu.split-bills.created" -> "split-bill.created";
            case "payu.split-bills.activated" -> "split-bill.activated";
            case "payu.split-bills.cancelled" -> "split-bill.cancelled";
            case "payu.split-bills.payment.made" -> "split-bill.payment.made";
            case "payu.split-bills.completed" -> "split-bill.completed";
            case "merchant.settlements" -> "settlement.completed";
            case "payment.link.events" -> "payment-link.updated";
            case "escrow.held" -> "escrow.held";
            case "escrow.released" -> "escrow.released";
            case "escrow.settled" -> "escrow.settled";
            case "escrow.refunded" -> "escrow.refunded";
            case "escrow.expired" -> "escrow.expired";
            default -> "event." + topic.replace(".", "_");
        };
    }

    /**
     * Parse Kafka message value as JSON map.
     * Handles both CloudEvent envelopes and plain JSON payloads.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String value) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(value, Map.class);

            // If this is a CloudEvent envelope, unwrap the data field
            if (parsed.containsKey("specversion") && parsed.containsKey("data")) {
                Map<String, Object> unwrapped = new LinkedHashMap<>();
                // Preserve CloudEvent metadata
                unwrapped.put("eventId", parsed.get("id"));
                unwrapped.put("source", parsed.get("source"));
                unwrapped.put("eventTime", parsed.get("time"));
                unwrapped.put("subject", parsed.get("subject"));

                // Merge data payload
                Object data = parsed.get("data");
                if (data instanceof Map) {
                    unwrapped.putAll((Map<String, Object>) data);
                } else {
                    unwrapped.put("data", data);
                }
                return unwrapped;
            }

            return parsed;
        } catch (Exception e) {
            // If not valid JSON, wrap the raw string
            log.warn("Could not parse event payload as JSON, wrapping raw value");
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("rawPayload", value);
            return fallback;
        }
    }

    /**
     * Extract event ID from payload for idempotency.
     * Checks common fields: eventId, id, transactionId.
     */
    private String extractEventId(Map<String, Object> payload) {
        Object eventId = payload.get("eventId");
        if (eventId != null) return eventId.toString();

        Object id = payload.get("id");
        if (id != null) return "evt_" + id.toString();

        Object txnId = payload.get("transactionId");
        if (txnId != null) return "evt_txn_" + txnId.toString();

        return "evt_" + java.util.UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16);
    }
}
