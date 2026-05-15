package id.payu.promotion.dto;

import id.payu.promotion.adapter.persistence.entity.LoyaltyPointsEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.promotion.domain.TransactionType;

public record LoyaltyPointsResponse(
    UUID id,
    String accountId,
    String transactionId,
    TransactionType transactionType,
    Integer points,
    Integer balanceAfter,
    LocalDateTime expiryDate,
    LocalDateTime redeemedAt,
    LocalDateTime createdAt
) {
    public static LoyaltyPointsResponse from(LoyaltyPointsEntity loyaltyPoints) {
        return new LoyaltyPointsResponse(
            loyaltyPoints.getId(),
            loyaltyPoints.getAccountId(),
            loyaltyPoints.getTransactionId(),
            loyaltyPoints.getTransactionType(),
            loyaltyPoints.getPoints(),
            loyaltyPoints.getBalanceAfter(),
            loyaltyPoints.getExpiryDate(),
            loyaltyPoints.getRedeemedAt(),
            loyaltyPoints.getCreatedAt()
        );
    }
}
