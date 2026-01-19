package id.payu.transaction.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventPublisherAdapter implements TransactionEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_TRANSACTIONS = "payu.transactions";

    @Override
    public void publishTransactionInitiated(Transaction transaction) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "transaction-initiated");
        event.put("transactionId", transaction.getId().toString());
        event.put("referenceNumber", transaction.getReferenceNumber());
        event.put("senderAccountId", transaction.getSenderAccountId().toString());
        event.put("amount", transaction.getAmount());
        event.put("currency", transaction.getCurrency());
        event.put("type", transaction.getType().name());
        event.put("status", transaction.getStatus().name());
        event.put("timestamp", transaction.getCreatedAt());

        kafkaTemplate.send(TOPIC_TRANSACTIONS + ".initiated", transaction.getId().toString(), event);
        log.info("Published transaction-initiated event: {}", transaction.getId());
    }

    @Override
    public void publishTransactionValidated(Transaction transaction) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "transaction-validated");
        event.put("transactionId", transaction.getId().toString());
        event.put("referenceNumber", transaction.getReferenceNumber());
        event.put("status", transaction.getStatus().name());
        event.put("timestamp", transaction.getUpdatedAt());

        kafkaTemplate.send(TOPIC_TRANSACTIONS + ".validated", transaction.getId().toString(), event);
        log.info("Published transaction-validated event: {}", transaction.getId());
    }

    @Override
    public void publishTransactionCompleted(Transaction transaction) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "transaction-completed");
        event.put("transactionId", transaction.getId().toString());
        event.put("referenceNumber", transaction.getReferenceNumber());
        event.put("amount", transaction.getAmount());
        event.put("currency", transaction.getCurrency());
        event.put("type", transaction.getType().name());
        event.put("status", transaction.getStatus().name());
        event.put("completedAt", transaction.getCompletedAt());
        event.put("timestamp", transaction.getUpdatedAt());

        kafkaTemplate.send(TOPIC_TRANSACTIONS + ".completed", transaction.getId().toString(), event);
        log.info("Published transaction-completed event: {}", transaction.getId());
    }

    @Override
    public void publishTransactionFailed(Transaction transaction, String reason) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "transaction-failed");
        event.put("transactionId", transaction.getId().toString());
        event.put("referenceNumber", transaction.getReferenceNumber());
        event.put("amount", transaction.getAmount());
        event.put("currency", transaction.getCurrency());
        event.put("type", transaction.getType().name());
        event.put("status", transaction.getStatus().name());
        event.put("failureReason", reason);
        event.put("timestamp", transaction.getUpdatedAt());

        kafkaTemplate.send(TOPIC_TRANSACTIONS + ".failed", transaction.getId().toString(), event);
        log.info("Published transaction-failed event: {} - Reason: {}", transaction.getId(), reason);
    }
}
