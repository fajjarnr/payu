package id.payu.lending.interfaces.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionSummaryResponse(
        UUID accountId,
        long totalTransactions,
        BigDecimal totalAmount,
        BigDecimal totalSent,
        BigDecimal totalReceived,
        long successfulTransactions,
        long failedTransactions,
        Instant oldestTransactionDate,
        Instant latestTransactionDate
) {
}
