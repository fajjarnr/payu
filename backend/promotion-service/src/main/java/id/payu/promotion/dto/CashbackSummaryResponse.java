package id.payu.promotion.dto;

import java.math.BigDecimal;

public record CashbackSummaryResponse(
    BigDecimal totalCashback,
    BigDecimal pendingCashback,
    BigDecimal creditedCashback,
    int transactionCount
) {}
