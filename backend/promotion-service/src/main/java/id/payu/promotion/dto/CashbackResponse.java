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
            cashback.getId(),
            cashback.getAccountId(),
            cashback.getTransactionId(),
            cashback.getCashbackAmount(),
            cashback.getTransactionAmount(),
            cashback.getPercentage(),
            cashback.getMerchantCode(),
            cashback.getCategoryCode(),
            cashback.getCashbackCode(),
            cashback.getStatus(),
            cashback.getCreditedAt(),
            cashback.getExpiryDate(),
            cashback.getCreatedAt()
        );
    }
}
