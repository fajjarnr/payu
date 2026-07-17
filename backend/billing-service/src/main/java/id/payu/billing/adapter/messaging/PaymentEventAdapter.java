package id.payu.billing.adapter.messaging;

import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.port.out.PaymentEventPort;
import id.payu.outbox.service.OutboxService;
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

    @Override
    public void publishPaymentEvent(BillPayment payment) {
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
                payment.getStatus() == PaymentStatus.COMPLETED
                        ? "PaymentCompleted" : "PaymentFailed",
                payload
        );

        log.info("Published payment event: id={}, status={}", payment.getId(), payment.getStatus());
    }
}
