package id.payu.promotion.dto;

import java.math.BigDecimal;

public record ClaimPromotionRequest(
    String accountId,
    String transactionId,
    BigDecimal transactionAmount,
    String merchantCode,
    String categoryCode
) {}
