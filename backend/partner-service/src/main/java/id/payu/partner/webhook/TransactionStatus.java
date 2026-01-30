package id.payu.partner.webhook;

/**
 * Enumeration of possible transaction statuses for payment webhooks.
 *
 * @author PayU Platform Engineering
 * @since 1.0.0
 */
public enum TransactionStatus {

    /**
     * Payment is pending and awaiting completion.
     */
    PENDING,

    /**
     * Payment has been successfully completed.
     */
    COMPLETED,

    /**
     * Payment has failed.
     */
    FAILED,

    /**
     * Payment has been refunded.
     */
    REFUNDED,

    /**
     * Payment has been cancelled.
     */
    CANCELLED,

    /**
     * Payment has expired.
     */
    EXPIRED
}
