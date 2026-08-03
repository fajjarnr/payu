package id.payu.dispute.domain.port.out;

import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.model.TransactionDetails;

/**
 * Publishes durable refund commands for downstream payment and ledger execution.
 */
public interface RefundEventPublisherPort {

    void publishRefundRequested(Refund refund, TransactionDetails transaction);
}
