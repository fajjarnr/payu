package id.payu.transaction.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ADR-0028: Step-up verification client port. Transaction-service calls
 * auth-service internal step-up verify endpoint with dynamic linking payload.
 *
 * <p>Payload fields per ADR dynamic linking: amount, currency, payee (recipientAccountNumber),
 * sender, and step-up proof (challengeId + pin). The adapter computes
 * payload_digest = SHA-256(sender|recipient|amount|currency) and forwards it.
 */
public interface StepUpVerificationPort {

    /**
     * Verify step-up proof with dynamic linking. Throws on failure.
     *
     * @param userId                 authenticated user id (JWT sub/account_id)
     * @param challengeId            X-StepUp-Challenge-Id header value (stepUpToken)
     * @param pin                    6-digit transaction PIN
     * @param senderAccountId        sender account UUID
     * @param recipientAccountNumber payee account number (dynamic linking)
     * @param amount                 plain amount (dynamic linking — tampering guard)
     * @param currency               ISO 4217 currency
     */
    void verify(String userId, String challengeId, String pin,
                UUID senderAccountId, String recipientAccountNumber,
                BigDecimal amount, String currency);
}
