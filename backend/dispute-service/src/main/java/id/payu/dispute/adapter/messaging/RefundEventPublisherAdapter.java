package id.payu.dispute.adapter.messaging;

import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.model.TransactionDetails;
import id.payu.dispute.domain.port.out.RefundEventPublisherPort;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
    public void publishRefundRequested(Refund refund, TransactionDetails transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("refundId", refund.getId().toString());
        payload.put("transactionId", refund.getTransactionId().toString());
        payload.put("amount", refund.getAmount().toPlainString());
        payload.put("currency", refund.getCurrency());
        payload.put("reason", refund.getReason());
        payload.put("ledgerOperation", "REVERSAL");
        payload.put("senderAccountId", transaction.senderAccountId());
        payload.put("recipientAccountId", transaction.recipientAccountId());
        outboxService.createEvent(
                "Refund",
                refund.getId().toString(),
                "RefundRequested",
                payload,
                null,
                TOPIC_REFUND_REQUESTED
        );
    }
}
