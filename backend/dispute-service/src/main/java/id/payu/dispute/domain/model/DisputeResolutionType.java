package id.payu.dispute.domain.model;

/**
 * Enum representing the resolution type of a dispute.
 */
public enum DisputeResolutionType {
    /**
     * Customer is refunded in full.
     */
    REFUND_CUSTOMER,

    /**
     * Customer claim is rejected, no refund.
     */
    REJECT_CLAIM,

    /**
     * Partial refund is issued to customer.
     */
    PARTIAL_REFUND
}
