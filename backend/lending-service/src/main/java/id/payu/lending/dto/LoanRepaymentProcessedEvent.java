package id.payu.lending.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanRepaymentProcessedEvent(
        UUID repaymentId,
        UUID repaymentScheduleId,
        UUID loanId,
        UUID userId,
        BigDecimal amount,
        BigDecimal principalApplied,
        BigDecimal interestApplied,
        String currency,
        String idempotencyKey,
        String walletTransactionId,
        Instant processedAt
) {}
