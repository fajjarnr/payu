package id.payu.transaction.domain.model;

/**
 * Enumeration of possible statuses for a disbursement (payout) transaction.
 *
 * <p>The disbursement lifecycle follows this state machine:</p>
 * <pre>
 * PENDING → PROCESSING → COMPLETED
n *                    ↘ FAILED
 * </pre>
 *
 * <p>State descriptions:</p>
 * <ul>
 *   <li><b>PENDING</b> - Disbursement created, awaiting processing</li>
 *   <li><b>PROCESSING</b> - Funds debited from source, BI-FAST transfer initiated</li>
 *   <li><b>COMPLETED</b> - BI-FAST confirmed successful transfer</li>
 *   <li><b>FAILED</b> - Transfer failed, funds will be refunded</li>
 * </ul>
 *
 * @see Disbursement
 */
public enum DisbursementStatus {
    /**
     * Disbursement has been created but not yet processed.
     * Initial state after creation.
     */
    PENDING,

    /**
     * Disbursement is being processed.
     * Funds have been debited from source account and BI-FAST transfer initiated.
     */
    PROCESSING,

    /**
     * Disbursement completed successfully.
     * BI-FAST has confirmed the transfer to beneficiary account.
     */
    COMPLETED,

    /**
     * Disbursement failed.
     * Transfer could not be completed, funds will be refunded to source account.
     */
    FAILED
}
