package id.payu.dispute.domain.model;

/**
 * Enum representing the status of a refund.
 *
 * <p>State Machine:
 * <pre>
 * PENDING -> PROCESSING -> COMPLETED
 *                     \> FAILED
 *         -> CANCELLED (from PENDING only)
 * </pre></p>
 */
public enum RefundStatus {
    /**
     * Refund request has been created but not yet processed.
     */
    PENDING,

    /**
     * Refund is being processed by the payment provider.
     */
    PROCESSING,

    /**
     * Refund has been successfully completed.
     */
    COMPLETED,

    /**
     * Refund processing failed.
     */
    FAILED,

    /**
     * Refund was cancelled before processing.
     */
    CANCELLED
}
