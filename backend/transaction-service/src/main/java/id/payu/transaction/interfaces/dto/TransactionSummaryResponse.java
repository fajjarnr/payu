package id.payu.transaction.interfaces.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate summary of an account's transactions (GRPC-008).
 * Consumed by lending-service enhanced credit scoring.
 */
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
