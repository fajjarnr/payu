package id.payu.lending.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionSummaryResponse(
        UUID userId,
        Integer totalTransactions,
        BigDecimal totalAmount,
        BigDecimal totalSent,
        BigDecimal totalReceived,
        Integer successfulTransactions,
        Integer failedTransactions,
        Instant oldestTransactionDate,
        Instant latestTransactionDate
) {
}
