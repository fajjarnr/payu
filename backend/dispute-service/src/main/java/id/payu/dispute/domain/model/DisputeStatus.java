package id.payu.dispute.domain.model;

/**
 * Enum representing the status of a dispute.
 *
 * <p>State Machine:
 * <pre>
 * OPEN -> INVESTIGATING -> RESOLVED
 *                      -> ESCALATED
 *       -> REJECTED (from OPEN or INVESTIGATING)
 * </pre></p>
 */
public enum DisputeStatus {
    /**
     * Dispute has been opened but investigation has not started.
     */
    OPEN,

    /**
     * Dispute is under investigation.
     */
    INVESTIGATING,

    /**
     * Dispute has been resolved.
     */
    RESOLVED,

    /**
     * Dispute claim was rejected.
     */
    REJECTED,

    /**
     * Dispute has been escalated for senior review.
     */
    ESCALATED
}
