package id.payu.transaction.domain.model;

/**
 * Enumeration of possible statuses for a batch disbursement.
 *
 * <p>The batch disbursement lifecycle follows this state machine:
 * <pre>
 * PENDING → PROCESSING → [COMPLETED | PARTIAL | FAILED]
 * </pre>
 *
 * <p>State descriptions:
 * <ul>
 *   <li><b>PENDING</b> - Batch created, items added, awaiting processing</li>
 *   <li><b>PROCESSING</b> - Batch processing started, items being processed</li>
 *   <li><b>COMPLETED</b> - All items in batch completed successfully</li>
 *   <li><b>PARTIAL</b> - Some items completed, some failed</li>
 *   <li><b>FAILED</b> - All items in batch failed</li>
 * </ul>
 *
 * @see BatchDisbursement
 */
public enum BatchDisbursementStatus {
    /**
     * Batch has been created but processing has not started.
     * Initial state after creation while items are being added.
     */
    PENDING,

    /**
     * Batch processing is in progress.
     * Items are being processed sequentially.
     */
    PROCESSING,

    /**
     * All items in the batch completed successfully.
     * Terminal state - no further processing needed.
     */
    COMPLETED,

    /**
     * Batch completed with mixed results.
     * Some items succeeded, some failed.
     * Terminal state - manual review may be needed for failed items.
     */
    PARTIAL,

    /**
     * All items in the batch failed.
     * Terminal state - entire batch failed.
     */
    FAILED
}
