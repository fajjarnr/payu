package id.payu.billing.adapter.messaging;

import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.port.out.PaymentEventPort;
import id.payu.outbox.service.OutboxService;
import id.payu.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import id.payu.billing.domain.model.PaymentStatus;

/**
 * Kafka outbox adapter implementing the payment event port.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventAdapter implements PaymentEventPort {

    private final OutboxService outboxService;
    private final OutboxRepository outboxRepository;

    @Override
    public void publishPaymentEvent(BillPayment payment) {
        String eventType = payment.getStatus() == PaymentStatus.COMPLETED
                ? "PaymentCompleted" : "PaymentFailed";
        if (outboxRepository.findFirstByAggregateTypeAndAggregateIdAndEventType(
                "BillPaymentEntity", payment.getId().toString(), eventType).isPresent()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "paymentId", payment.getId().toString(),
                "referenceNumber", payment.getReferenceNumber(),
                "accountId", payment.getAccountId(),
                "billerCode", payment.getBillerType().getCode(),
                "amount", payment.getTotalAmount(),
                "status", payment.getStatus().name()
        );

        outboxService.createEvent(
                "BillPaymentEntity",
                payment.getId().toString(),
                eventType,
                payload
        );

        log.info("Published payment event: id={}, status={}", payment.getId(), payment.getStatus());
    }
}
