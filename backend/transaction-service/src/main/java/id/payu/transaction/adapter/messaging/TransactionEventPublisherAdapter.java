package id.payu.transaction.adapter.messaging;

import id.payu.events.cloudevents.CloudEventBuilder;
import id.payu.events.cloudevents.CloudEventEnvelope;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.application.service.DeferredOutboxService;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Outbox-backed adapter for publishing transaction events using CloudEvents 1.0 envelopes.
 * <p>
 * TXN-HARDEN-003: publish outbox outside the business TX via DeferredOutboxService (afterCommit + REQUIRES_NEW).
 * The OutboxPublisher polls and publishes them asynchronously.
 * <p>
 * All events conform to CloudEvents 1.0 spec via the events-starter CloudEventEnvelope.
 */
@Component
public class TransactionEventPublisherAdapter implements TransactionEventPublisherPort {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransactionEventPublisherAdapter.class);



    private final DeferredOutboxService deferredOutboxService;

    public TransactionEventPublisherAdapter(DeferredOutboxService deferredOutboxService) {
        this.deferredOutboxService = deferredOutboxService;
    }

    private static final String AGGREGATE_TYPE = "TransactionEntity";
    private static final String TOPIC_TRANSACTION = "payu.transaction";
    private static final String SERVICE_NAME = "transaction-service";

    @Override
    public void publishTransactionInitiated(TransactionEntity transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", transaction.getId().toString());
        payload.put("referenceNumber", transaction.getReferenceNumber());
        payload.put("senderAccountId", transaction.getSenderAccountId().toString());
        payload.put("amount", transaction.getAmount().getAmount());
        payload.put("currency", transaction.getAmount().getCurrency().getCurrencyCode());
        payload.put("type", transaction.getType().name());
        payload.put("status", transaction.getStatus().name());
        payload.put("timestamp", transaction.getCreatedAt());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.transaction.initiated")
                .subject(transaction.getId().toString())
                .data(payload)
                .build();

        deferredOutboxService.publishAfterCommit(
                AGGREGATE_TYPE,
                transaction.getId().toString(),
                "TransactionInitiated",
                envelopeToMap(envelope),
                TOPIC_TRANSACTION + ".initiated.v1"
        );
        log.info("Created CloudEvent outbox event for transaction-initiated: {}", transaction.getId());
    }

    @Override
    public void publishTransactionValidated(TransactionEntity transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", transaction.getId().toString());
        payload.put("referenceNumber", transaction.getReferenceNumber());
        payload.put("status", transaction.getStatus().name());
        payload.put("timestamp", transaction.getUpdatedAt());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.transaction.validated")
                .subject(transaction.getId().toString())
                .data(payload)
                .build();

        deferredOutboxService.publishAfterCommit(
                AGGREGATE_TYPE,
                transaction.getId().toString(),
                "TransactionValidated",
                envelopeToMap(envelope),
                TOPIC_TRANSACTION + ".validated.v1"
        );
        log.info("Created CloudEvent outbox event for transaction-validated: {}", transaction.getId());
    }

    @Override
    public void publishTransactionCompleted(TransactionEntity transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", transaction.getId().toString());
        payload.put("referenceNumber", transaction.getReferenceNumber());
        payload.put("amount", transaction.getAmount().getAmount());
        payload.put("currency", transaction.getAmount().getCurrency().getCurrencyCode());
        payload.put("type", transaction.getType().name());
        payload.put("status", transaction.getStatus().name());
        payload.put("completedAt", transaction.getCompletedAt());
        payload.put("timestamp", transaction.getUpdatedAt());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.transaction.completed")
                .subject(transaction.getId().toString())
                .data(payload)
                .build();

        deferredOutboxService.publishAfterCommit(
                AGGREGATE_TYPE,
                transaction.getId().toString(),
                "TransactionCompleted",
                envelopeToMap(envelope),
                TOPIC_TRANSACTION + ".completed.v1"
        );
        log.info("Created CloudEvent outbox event for transaction-completed: {}", transaction.getId());
    }

    @Override
    public void publishTransactionFailed(TransactionEntity transaction, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionId", transaction.getId().toString());
        payload.put("referenceNumber", transaction.getReferenceNumber());
        payload.put("amount", transaction.getAmount().getAmount());
        payload.put("currency", transaction.getAmount().getCurrency().getCurrencyCode());
        payload.put("type", transaction.getType().name());
        payload.put("status", transaction.getStatus().name());
        payload.put("failureReason", reason);
        payload.put("timestamp", transaction.getUpdatedAt());

        CloudEventEnvelope<Map<String, Object>> envelope = CloudEventBuilder
                .<Map<String, Object>>forService(SERVICE_NAME)
                .type("id.payu.transaction.failed")
                .subject(transaction.getId().toString())
                .data(payload)
                .build();

        deferredOutboxService.publishAfterCommit(
                AGGREGATE_TYPE,
                transaction.getId().toString(),
                "TransactionFailed",
                envelopeToMap(envelope),
                TOPIC_TRANSACTION + ".failed.v1"
        );
        log.info("Created CloudEvent outbox event for transaction-failed: {} - Reason: {}", transaction.getId(), reason);
    }

    /**
     * Convert a CloudEventEnvelope to a Map suitable for outbox JSON serialization.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> envelopeToMap(CloudEventEnvelope<Map<String, Object>> envelope) {
        Map<String, Object> ceMap = new HashMap<>();
        ceMap.put("specversion", envelope.getSpecVersion());
        ceMap.put("id", envelope.getId().toString());
        ceMap.put("source", envelope.getSource().toString());
        ceMap.put("type", envelope.getType());
        ceMap.put("datacontenttype", envelope.getDataContentType());
        ceMap.put("time", envelope.getTime().toString());
        if (envelope.getSubject() != null) ceMap.put("subject", envelope.getSubject());
        if (envelope.getData() != null) ceMap.put("data", envelope.getData());
        if (envelope.getPayuCorrelationId() != null) ceMap.put("payucorrelationid", envelope.getPayuCorrelationId());
        return ceMap;
    }
}
