package id.payu.promotion.interfaces.dto;

import java.math.BigDecimal;

public record RewardSummaryResponse(
    BigDecimal totalCashback,
    Integer totalPoints,
    int transactionCount
) {}
