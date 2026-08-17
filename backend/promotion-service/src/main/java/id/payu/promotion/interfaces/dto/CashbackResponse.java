package id.payu.promotion.interfaces.dto;

import id.payu.promotion.domain.model.Cashback;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.promotion.domain.CashbackStatus;

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
    CashbackStatus status,
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
