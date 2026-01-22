package id.payu.promotion.dto;

import java.math.BigDecimal;

public record CreateCashbackRequest(
    String accountId,
    String transactionId,
    BigDecimal transactionAmount,
    String merchantCode,
    String categoryCode,
    String cashbackCode
) {}
