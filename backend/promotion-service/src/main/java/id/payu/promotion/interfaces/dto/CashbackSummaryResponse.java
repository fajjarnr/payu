package id.payu.promotion.interfaces.dto;

import java.math.BigDecimal;

public record CashbackSummaryResponse(
    BigDecimal totalCashback,
    BigDecimal pendingCashback,
    BigDecimal creditedCashback,
    int transactionCount
) {}
