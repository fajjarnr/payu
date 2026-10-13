package id.payu.wallet.interfaces.dto;

import java.math.BigDecimal;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RefundRequestedEvent(
        UUID refundId,
        UUID transactionId,
        BigDecimal amount,
        String currency,
        String reason,
        String senderAccountId,
        String recipientAccountId,
        String signature) {
    /** Backwards-compatible constructor for callers that do not sign yet. */
    public RefundRequestedEvent(
            UUID refundId, UUID transactionId, BigDecimal amount, String currency,
            String reason, String senderAccountId, String recipientAccountId) {
        this(refundId, transactionId, amount, currency, reason,
                senderAccountId, recipientAccountId, null);
    }
}
