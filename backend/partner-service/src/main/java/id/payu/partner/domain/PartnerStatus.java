package id.payu.partner.domain;

/**
 * Status values for PartnerEntity entities.
 * ADR-0035: PENDING_APPROVAL and REJECTED for dual-control; PENDING_VERIFICATION deprecated.
 */
public enum PartnerStatus {
    PENDING_VERIFICATION,
    PENDING_APPROVAL,
    ACTIVE,
    REJECTED,
    SUSPENDED,
    TERMINATED
}
