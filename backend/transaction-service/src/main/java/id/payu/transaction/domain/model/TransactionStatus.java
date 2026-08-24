package id.payu.transaction.domain.model;

public enum TransactionStatus {
        PENDING,
        VALIDATING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        // ADR-0030: risk score 71-85 (HIGH_RISK) — held for AML compliance review before funds move
        PENDING_COMPLIANCE_REVIEW,
        // ADR-0028: step-up required but not yet verified
        PENDING_STEP_UP
    }
