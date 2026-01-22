package id.payu.lending.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String referenceNumber,
        UUID senderAccountId,
        UUID recipientAccountId,
        String type,
        BigDecimal amount,
        String currency,
        String description,
        String status,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {
}
