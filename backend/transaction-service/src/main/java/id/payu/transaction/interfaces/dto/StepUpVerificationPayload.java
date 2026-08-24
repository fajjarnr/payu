package id.payu.transaction.interfaces.dto;

import java.math.BigDecimal;

/**
 * ADR-0028 dynamic linking payload structure per task contract.
 *
 * <p>Fields: amount, currency, payeeAccountId (recipientAccountNumber),
 * referenceId, stepUpToken (challengeId). The PIN is carried separately
 * and never logged (masked via @Sensitive).
 *
 * <p>Canonical digest computed by adapter: SHA-256(sender|payee|amount.toPlainString| currency).
 */
public record StepUpVerificationPayload(
        BigDecimal amount,
        String currency,
        String payeeAccountId,
        String referenceId,
        String stepUpToken
) {}
