package id.payu.dispute.adapter.messaging;

import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.port.out.RefundEventPublisherPort;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Transactional outbox adapter for refund commands.
 */
@Component
@RequiredArgsConstructor
public class RefundEventPublisherAdapter implements RefundEventPublisherPort {

    private static final String TOPIC_REFUND_REQUESTED = "payu.dispute.refund-requested.v1";

    private final OutboxService outboxService;

    @Override
    public void publishRefundRequested(Refund refund) {
        outboxService.createEvent(
                "Refund",
                refund.getId().toString(),
                "RefundRequested",
                Map.of(
                        "refundId", refund.getId().toString(),
                        "transactionId", refund.getTransactionId().toString(),
                        "amount", refund.getAmount().toPlainString(),
                        "currency", refund.getCurrency(),
                        "reason", refund.getReason(),
                        "ledgerOperation", "REVERSAL"
                ),
                null,
                TOPIC_REFUND_REQUESTED
        );
    }
}
