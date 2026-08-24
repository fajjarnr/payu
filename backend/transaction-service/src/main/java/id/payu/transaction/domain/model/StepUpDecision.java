package id.payu.transaction.domain.model;

/**
 * ADR-0028 decision matrix for step-up requirement.
 * Top-level enum per task contract.
 */
public enum StepUpDecision {
    /** No step-up required — low risk and below amount threshold */
    BYPASS,
    /** Step-up required — risk 40-70 or amount > threshold */
    REQUIRED,
    /** Step-up verified successfully */
    VERIFIED
}
