package id.payu.transaction.adapter.messaging;

import id.payu.outbox.service.OutboxService;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Outbox-backed adapter for publishing transaction events.
 * <p>
 * Events are written to the outbox_events table within the same DB transaction
 * as the business operation, guaranteeing at-least-once delivery to Kafka.
 * The OutboxPublisher polls and publishes them asynchronously.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventPublisherAdapter implements TransactionEventPublisherPort {

    private final OutboxService outboxService;

    private static final String AGGREGATE_TYPE = "Transaction";
    private static final String TOPIC_TRANSACTIONS = "payu.transactions";

    @Override
    public void publishTransactionInitiated(Transaction transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "transaction-initiated");
        payload.put("transactionId", transaction.getId().toString());
        payload.put("referenceNumber", transaction.getReferenceNumber());
        payload.put("senderAccountId", transaction.getSenderAccountId().toString());
        payload.put("amount", transaction.getAmount().getAmount());
        payload.put("currency", transaction.getAmount().getCurrency().getCurrencyCode());
        payload.put("type", transaction.getType().name());
        payload.put("status", transaction.getStatus().name());
        payload.put("timestamp", transaction.getCreatedAt());

        outboxService.createEvent(
                AGGREGATE_TYPE,
                transaction.getId().toString(),
                "TransactionInitiated",
                payload,
                null,
                TOPIC_TRANSACTIONS + ".initiated"
        );
        log.info("Created outbox event for transaction-initiated: {}", transaction.getId());
    }

    @Override
    public void publishTransactionValidated(Transaction transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "transaction-validated");
        payload.put("transactionId", transaction.getId().toString());
        payload.put("referenceNumber", transaction.getReferenceNumber());
        payload.put("status", transaction.getStatus().name());
        payload.put("timestamp", transaction.getUpdatedAt());

        outboxService.createEvent(
                AGGREGATE_TYPE,
                transaction.getId().toString(),
                "TransactionValidated",
                payload,
                null,
                TOPIC_TRANSACTIONS + ".validated"
        );
        log.info("Created outbox event for transaction-validated: {}", transaction.getId());
    }

    @Override
    public void publishTransactionCompleted(Transaction transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "transaction-completed");
        payload.put("transactionId", transaction.getId().toString());
        payload.put("referenceNumber", transaction.getReferenceNumber());
        payload.put("amount", transaction.getAmount().getAmount());
        payload.put("currency", transaction.getAmount().getCurrency().getCurrencyCode());
        payload.put("type", transaction.getType().name());
        payload.put("status", transaction.getStatus().name());
        payload.put("completedAt", transaction.getCompletedAt());
        payload.put("timestamp", transaction.getUpdatedAt());

        outboxService.createEvent(
                AGGREGATE_TYPE,
                transaction.getId().toString(),
                "TransactionCompleted",
                payload,
                null,
                TOPIC_TRANSACTIONS + ".completed"
        );
        log.info("Created outbox event for transaction-completed: {}", transaction.getId());
    }

    @Override
    public void publishTransactionFailed(Transaction transaction, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "transaction-failed");
        payload.put("transactionId", transaction.getId().toString());
        payload.put("referenceNumber", transaction.getReferenceNumber());
        payload.put("amount", transaction.getAmount().getAmount());
        payload.put("currency", transaction.getAmount().getCurrency().getCurrencyCode());
        payload.put("type", transaction.getType().name());
        payload.put("status", transaction.getStatus().name());
        payload.put("failureReason", reason);
        payload.put("timestamp", transaction.getUpdatedAt());

        outboxService.createEvent(
                AGGREGATE_TYPE,
                transaction.getId().toString(),
                "TransactionFailed",
                payload,
                null,
                TOPIC_TRANSACTIONS + ".failed"
        );
        log.info("Created outbox event for transaction-failed: {} - Reason: {}", transaction.getId(), reason);
    }
}
