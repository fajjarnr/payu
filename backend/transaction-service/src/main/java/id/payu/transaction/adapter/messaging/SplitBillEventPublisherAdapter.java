package id.payu.transaction.adapter.messaging;

import id.payu.transaction.domain.model.SplitBill;
import id.payu.transaction.domain.model.SplitBillParticipant;
import id.payu.transaction.domain.port.out.SplitBillEventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SplitBillEventPublisherAdapter implements SplitBillEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_SPLIT_BILLS = "payu.split-bills";

    @Override
    public void publishSplitBillCreated(SplitBill splitBill) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "split-bill-created");
        event.put("splitBillId", splitBill.getId().toString());
        event.put("referenceNumber", splitBill.getReferenceNumber());
        event.put("creatorAccountId", splitBill.getCreatorAccountId().toString());
        event.put("totalAmount", splitBill.getTotalAmount());
        event.put("currency", splitBill.getCurrency());
        event.put("title", splitBill.getTitle());
        event.put("status", splitBill.getStatus().name());
        event.put("participantCount", splitBill.getParticipants() != null ? splitBill.getParticipants().size() : 0);
        event.put("timestamp", splitBill.getCreatedAt());

        kafkaTemplate.send(TOPIC_SPLIT_BILLS + ".created", splitBill.getId().toString(), event);
        log.info("Published split-bill-created event: {}", splitBill.getId());
    }

    @Override
    public void publishSplitBillActivated(SplitBill splitBill) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "split-bill-activated");
        event.put("splitBillId", splitBill.getId().toString());
        event.put("referenceNumber", splitBill.getReferenceNumber());
        event.put("creatorAccountId", splitBill.getCreatorAccountId().toString());
        event.put("status", splitBill.getStatus().name());
        event.put("timestamp", splitBill.getUpdatedAt());

        kafkaTemplate.send(TOPIC_SPLIT_BILLS + ".activated", splitBill.getId().toString(), event);
        log.info("Published split-bill-activated event: {}", splitBill.getId());
    }

    @Override
    public void publishSplitBillCancelled(SplitBill splitBill) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "split-bill-cancelled");
        event.put("splitBillId", splitBill.getId().toString());
        event.put("referenceNumber", splitBill.getReferenceNumber());
        event.put("creatorAccountId", splitBill.getCreatorAccountId().toString());
        event.put("status", splitBill.getStatus().name());
        event.put("timestamp", splitBill.getUpdatedAt());

        kafkaTemplate.send(TOPIC_SPLIT_BILLS + ".cancelled", splitBill.getId().toString(), event);
        log.info("Published split-bill-cancelled event: {}", splitBill.getId());
    }

    @Override
    public void publishParticipantAdded(SplitBill splitBill, SplitBillParticipant participant) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "participant-added");
        event.put("splitBillId", splitBill.getId().toString());
        event.put("referenceNumber", splitBill.getReferenceNumber());
        event.put("participantId", participant.getId().toString());
        event.put("accountId", participant.getAccountId().toString());
        event.put("accountNumber", participant.getAccountNumber());
        event.put("accountName", participant.getAccountName());
        event.put("amountOwed", participant.getAmountOwed());
        event.put("status", participant.getStatus().name());
        event.put("timestamp", participant.getCreatedAt());

        kafkaTemplate.send(TOPIC_SPLIT_BILLS + ".participant.added", splitBill.getId().toString(), event);
        log.info("Published participant-added event: splitBillId={}, participantId={}", 
                splitBill.getId(), participant.getId());
    }

    @Override
    public void publishPaymentMade(SplitBill splitBill, SplitBillParticipant participant, BigDecimal amount) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "payment-made");
        event.put("splitBillId", splitBill.getId().toString());
        event.put("referenceNumber", splitBill.getReferenceNumber());
        event.put("participantId", participant.getId().toString());
        event.put("accountId", participant.getAccountId().toString());
        event.put("accountNumber", participant.getAccountNumber());
        event.put("accountName", participant.getAccountName());
        event.put("paymentAmount", amount);
        event.put("totalPaid", participant.getAmountPaid());
        event.put("amountOwed", participant.getAmountOwed());
        event.put("status", participant.getStatus().name());
        event.put("timestamp", participant.getUpdatedAt());

        kafkaTemplate.send(TOPIC_SPLIT_BILLS + ".payment.made", splitBill.getId().toString(), event);
        log.info("Published payment-made event: splitBillId={}, participantId={}, amount={}", 
                splitBill.getId(), participant.getId(), amount);
    }

    @Override
    public void publishSplitBillCompleted(SplitBill splitBill) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "split-bill-completed");
        event.put("splitBillId", splitBill.getId().toString());
        event.put("referenceNumber", splitBill.getReferenceNumber());
        event.put("creatorAccountId", splitBill.getCreatorAccountId().toString());
        event.put("totalAmount", splitBill.getTotalAmount());
        event.put("totalPaid", splitBill.getTotalPaid());
        event.put("status", splitBill.getStatus().name());
        event.put("completedAt", splitBill.getCompletedAt());
        event.put("timestamp", splitBill.getUpdatedAt());

        kafkaTemplate.send(TOPIC_SPLIT_BILLS + ".completed", splitBill.getId().toString(), event);
        log.info("Published split-bill-completed event: {}", splitBill.getId());
    }

    @Override
    public void publishSplitBillPaymentReminder(SplitBill splitBill, SplitBillParticipant participant) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "payment-reminder");
        event.put("splitBillId", splitBill.getId().toString());
        event.put("referenceNumber", splitBill.getReferenceNumber());
        event.put("creatorAccountId", splitBill.getCreatorAccountId().toString());
        event.put("participantId", participant.getId().toString());
        event.put("accountId", participant.getAccountId().toString());
        event.put("accountNumber", participant.getAccountNumber());
        event.put("accountName", participant.getAccountName());
        event.put("amountOwed", participant.getAmountOwed());
        event.put("amountPaid", participant.getAmountPaid());
        event.put("remainingAmount", participant.getRemainingAmount());
        event.put("dueDate", splitBill.getDueDate());
        event.put("timestamp", java.time.Instant.now());

        kafkaTemplate.send(TOPIC_SPLIT_BILLS + ".reminder", splitBill.getId().toString(), event);
        log.info("Published payment-reminder event: splitBillId={}, participantId={}", 
                splitBill.getId(), participant.getId());
    }
}
