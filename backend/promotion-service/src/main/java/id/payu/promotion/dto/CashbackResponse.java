package id.payu.promotion.dto;

import id.payu.promotion.domain.Cashback;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CashbackResponse(
    UUID id,
    String accountId,
    String transactionId,
    BigDecimal cashbackAmount,
    BigDecimal transactionAmount,
    BigDecimal percentage,
    String merchantCode,
    String categoryCode,
    String cashbackCode,
    Cashback.Status status,
    LocalDateTime creditedAt,
    LocalDateTime expiryDate,
    LocalDateTime createdAt
) {
    public static CashbackResponse from(Cashback cashback) {
        return new CashbackResponse(
            cashback.id,
            cashback.accountId,
            cashback.transactionId,
            cashback.cashbackAmount,
            cashback.transactionAmount,
            cashback.percentage,
            cashback.merchantCode,
            cashback.categoryCode,
            cashback.cashbackCode,
            cashback.status,
            cashback.creditedAt,
            cashback.expiryDate,
            cashback.createdAt
        );
    }
}
