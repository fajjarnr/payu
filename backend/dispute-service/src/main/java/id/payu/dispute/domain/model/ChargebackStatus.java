package id.payu.dispute.domain.model;

/**
 * Chargeback lifecycle (ADR-0054 gap 054C).
 * Chargeback via card scheme — separate from internal Dispute REFUND.
 * State machine: OPEN -> SUBMITTED -> UNDER_REVIEW -> ACCEPTED/REJECTED -> REVERSED -> CLOSED
 */
public enum ChargebackStatus {
    OPEN,
    SUBMITTED,
    UNDER_REVIEW,
    ACCEPTED,
    REJECTED,
    REVERSED,
    CLOSED
}
