package id.payu.transaction.adapter.messaging;

import id.payu.outbox.service.OutboxService;
import id.payu.transaction.adapter.persistence.entity.SplitBillEntity;
import id.payu.transaction.adapter.persistence.entity.SplitBillParticipantEntity;
import id.payu.transaction.domain.port.out.SplitBillEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Outbox-backed adapter for publishing split-bill events.
 */
@Component
public class SplitBillEventPublisherAdapter implements SplitBillEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(SplitBillEventPublisherAdapter.class);
    private final OutboxService outboxService;

    private static final String AGGREGATE_TYPE = "SplitBillEntity";
    private static final String TOPIC_SPLIT_BILLS = "payu.split-bills";

    public SplitBillEventPublisherAdapter(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Override
    public void publishSplitBillCreated(SplitBillEntity splitBill) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "split-bill-created");
        payload.put("splitBillId", splitBill.getId().toString());
        payload.put("referenceNumber", splitBill.getReferenceNumber());
        payload.put("creatorAccountId", splitBill.getCreatorAccountId().toString());
        payload.put("totalAmount", splitBill.getTotalAmount());
        payload.put("currency", splitBill.getCurrency());
        payload.put("title", splitBill.getTitle());
        payload.put("status", splitBill.getStatus().name());
        payload.put("participantCount", splitBill.getParticipants() != null ? splitBill.getParticipants().size() : 0);
        payload.put("timestamp", splitBill.getCreatedAt());

        outboxService.createEvent(AGGREGATE_TYPE, splitBill.getId().toString(),
                "SplitBillCreated", payload, null, TOPIC_SPLIT_BILLS + ".created.v1");
        log.info("Created outbox event for split-bill-created: {}", splitBill.getId());
    }

    @Override
    public void publishSplitBillActivated(SplitBillEntity splitBill) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "split-bill-activated");
        payload.put("splitBillId", splitBill.getId().toString());
        payload.put("referenceNumber", splitBill.getReferenceNumber());
        payload.put("creatorAccountId", splitBill.getCreatorAccountId().toString());
        payload.put("status", splitBill.getStatus().name());
        payload.put("timestamp", splitBill.getUpdatedAt());

        outboxService.createEvent(AGGREGATE_TYPE, splitBill.getId().toString(),
                "SplitBillActivated", payload, null, TOPIC_SPLIT_BILLS + ".activated.v1");
        log.info("Created outbox event for split-bill-activated: {}", splitBill.getId());
    }

    @Override
    public void publishSplitBillCancelled(SplitBillEntity splitBill) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "split-bill-cancelled");
        payload.put("splitBillId", splitBill.getId().toString());
        payload.put("referenceNumber", splitBill.getReferenceNumber());
        payload.put("creatorAccountId", splitBill.getCreatorAccountId().toString());
        payload.put("status", splitBill.getStatus().name());
        payload.put("timestamp", splitBill.getUpdatedAt());

        outboxService.createEvent(AGGREGATE_TYPE, splitBill.getId().toString(),
                "SplitBillCancelled", payload, null, TOPIC_SPLIT_BILLS + ".cancelled.v1");
        log.info("Created outbox event for split-bill-cancelled: {}", splitBill.getId());
    }

    @Override
    public void publishParticipantAdded(SplitBillEntity splitBill, SplitBillParticipantEntity participant) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "participant-added");
        payload.put("splitBillId", splitBill.getId().toString());
        payload.put("referenceNumber", splitBill.getReferenceNumber());
        payload.put("participantId", participant.getId().toString());
        payload.put("accountId", participant.getAccountId().toString());
        payload.put("accountNumber", participant.getAccountNumber());
        payload.put("accountName", participant.getAccountName());
        payload.put("amountOwed", participant.getAmountOwed());
        payload.put("status", participant.getStatus().name());
        payload.put("timestamp", participant.getCreatedAt());

        outboxService.createEvent(AGGREGATE_TYPE, splitBill.getId().toString(),
                "ParticipantAdded", payload, null, TOPIC_SPLIT_BILLS + ".participant.added.v1");
        log.info("Created outbox event for participant-added: splitBillId={}, participantId={}",
                splitBill.getId(), participant.getId());
    }

    @Override
    public void publishPaymentMade(SplitBillEntity splitBill, SplitBillParticipantEntity participant, BigDecimal amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "payment-made");
        payload.put("splitBillId", splitBill.getId().toString());
        payload.put("referenceNumber", splitBill.getReferenceNumber());
        payload.put("participantId", participant.getId().toString());
        payload.put("accountId", participant.getAccountId().toString());
        payload.put("accountNumber", participant.getAccountNumber());
        payload.put("accountName", participant.getAccountName());
        payload.put("paymentAmount", amount);
        payload.put("totalPaid", participant.getAmountPaid());
        payload.put("amountOwed", participant.getAmountOwed());
        payload.put("status", participant.getStatus().name());
        payload.put("timestamp", participant.getUpdatedAt());

        outboxService.createEvent(AGGREGATE_TYPE, splitBill.getId().toString(),
                "PaymentMade", payload, null, TOPIC_SPLIT_BILLS + ".payment.made.v1");
        log.info("Created outbox event for payment-made: splitBillId={}, participantId={}, amount={}",
                splitBill.getId(), participant.getId(), amount);
    }

    @Override
    public void publishSplitBillCompleted(SplitBillEntity splitBill) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "split-bill-completed");
        payload.put("splitBillId", splitBill.getId().toString());
        payload.put("referenceNumber", splitBill.getReferenceNumber());
        payload.put("creatorAccountId", splitBill.getCreatorAccountId().toString());
        payload.put("totalAmount", splitBill.getTotalAmount());
        payload.put("totalPaid", splitBill.getTotalPaid());
        payload.put("status", splitBill.getStatus().name());
        payload.put("completedAt", splitBill.getCompletedAt());
        payload.put("timestamp", splitBill.getUpdatedAt());

        outboxService.createEvent(AGGREGATE_TYPE, splitBill.getId().toString(),
                "SplitBillCompleted", payload, null, TOPIC_SPLIT_BILLS + ".completed.v1");
        log.info("Created outbox event for split-bill-completed: {}", splitBill.getId());
    }

    @Override
    public void publishSplitBillPaymentReminder(SplitBillEntity splitBill, SplitBillParticipantEntity participant) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "payment-reminder");
        payload.put("splitBillId", splitBill.getId().toString());
        payload.put("referenceNumber", splitBill.getReferenceNumber());
        payload.put("creatorAccountId", splitBill.getCreatorAccountId().toString());
        payload.put("participantId", participant.getId().toString());
        payload.put("accountId", participant.getAccountId().toString());
        payload.put("accountNumber", participant.getAccountNumber());
        payload.put("accountName", participant.getAccountName());
        payload.put("amountOwed", participant.getAmountOwed());
        payload.put("amountPaid", participant.getAmountPaid());
        payload.put("remainingAmount", participant.getRemainingAmount());
        payload.put("dueDate", splitBill.getDueDate());
        payload.put("timestamp", java.time.Instant.now());

        outboxService.createEvent(AGGREGATE_TYPE, splitBill.getId().toString(),
                "PaymentReminder", payload, null, TOPIC_SPLIT_BILLS + ".reminder.v1");
        log.info("Created outbox event for payment-reminder: splitBillId={}, participantId={}",
                splitBill.getId(), participant.getId());
    }
}
