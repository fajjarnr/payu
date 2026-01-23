package id.payu.promotion.dto;

import java.math.BigDecimal;

public record ProcessTransactionRequest(
    String accountId,
    String transactionId,
    BigDecimal amount,
    String merchantCode,
    String categoryCode
) {
}
