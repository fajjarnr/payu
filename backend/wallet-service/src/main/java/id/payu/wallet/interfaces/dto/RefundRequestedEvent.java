package id.payu.wallet.interfaces.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequestedEvent(
        UUID refundId,
        UUID transactionId,
        BigDecimal amount,
        String currency,
        String reason,
        String senderAccountId,
        String recipientAccountId) {
}
