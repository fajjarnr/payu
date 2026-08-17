package id.payu.promotion.interfaces.dto;

import java.math.BigDecimal;

public record ClaimPromotionRequest(
    String accountId,
    String transactionId,
    BigDecimal transactionAmount,
    String merchantCode,
    String categoryCode
) {}
