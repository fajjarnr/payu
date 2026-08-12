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
 *   <li>payu.split-bills.*.v1 → split-bill.created/completed/payment.made</li>
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
                    "payu.split-bills.created.v1",
                    "payu.split-bills.activated.v1",
                    "payu.split-bills.cancelled.v1",
                    "payu.split-bills.payment.made.v1",
                    "payu.split-bills.completed.v1",
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
        } catch (RuntimeException e) {
            // PARTNER-PROD-004: never swallow a processing exception — rethrow so
            // the Kafka error handler retries (3x) and forwards the record to
            // <topic>.dlq instead of committing the offset and losing the event.
            log.error("Failed to process financial event: topic={}, eventType={}, error={}",
                    topic, eventType, e.getMessage(), e);
            throw e;
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
            case "payu.split-bills.created.v1" -> "split-bill.created";
            case "payu.split-bills.activated.v1" -> "split-bill.activated";
            case "payu.split-bills.cancelled.v1" -> "split-bill.cancelled";
            case "payu.split-bills.payment.made.v1" -> "split-bill.payment.made";
            case "payu.split-bills.completed.v1" -> "split-bill.completed";
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
     * Handles CloudEvent envelopes and plain JSON payloads.
     * <p>PARTNER-PROD-004: invalid JSON is NOT silently wrapped — it propagates
     * so the record is retried and moved to the DLQ instead of being dispatched
     * as an opaque rawPayload blob.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String value) {
        Map<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(value, Map.class);
        } catch (Exception e) {
            // PARTNER-PROD-004: malformed payloads must fail the record (retry + DLQ),
            // not be dispatched as an opaque rawPayload blob.
            throw new IllegalArgumentException("Invalid event payload JSON", e);
        }

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
