package id.payu.integration.domain.model;

/**
 * Processing status of an integration message.
 * Tracks the lifecycle from receipt to completion.
 */
public enum MessageStatus {
    /**
     * Message received, awaiting processing
     */
    RECEIVED,

    /**
     * Message is being validated
     */
    VALIDATING,

    /**
     * Message is being transformed to/from internal format
     */
    TRANSFORMING,

    /**
     * Message transformation completed
     */
    TRANSFORMED,

    /**
     * Message is being sent to target system
     */
    SENDING,

    /**
     * Message successfully sent/received and processed
     */
    SENT,

    /**
     * Message processing failed
     */
    FAILED,

    /**
     * Message is queued for retry
     */
    RETRYING,

    /**
     * Message processing cancelled
     */
    CANCELLED
}
