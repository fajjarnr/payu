package id.payu.dispute.domain.port.out;

import id.payu.dispute.domain.model.Refund;

/**
 * Publishes durable refund commands for downstream payment and ledger execution.
 */
public interface RefundEventPublisherPort {

    void publishRefundRequested(Refund refund);
}
