package id.payu.promotion.dto;

import java.math.BigDecimal;

public record RewardSummaryResponse(
    BigDecimal totalCashback,
    Integer totalPoints,
    int transactionCount
) {}
