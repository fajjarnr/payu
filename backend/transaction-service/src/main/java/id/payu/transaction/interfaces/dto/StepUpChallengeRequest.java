package id.payu.transaction.interfaces.dto;

/**
 * DTO for initiating a step-up challenge (prepare phase) — not wired in handler
 * yet but provided for controller completeness per ADR-0028.
 */
public record StepUpChallengeRequest(
        String userId,
        String payloadDigest
) {}
