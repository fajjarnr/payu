package id.payu.promotion.dto;

import id.payu.promotion.domain.LoyaltyPoints;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoyaltyPointsResponse(
    UUID id,
    String accountId,
    String transactionId,
    LoyaltyPoints.TransactionType transactionType,
    Integer points,
    Integer balanceAfter,
    LocalDateTime expiryDate,
    LocalDateTime redeemedAt,
    LocalDateTime createdAt
) {
    public static LoyaltyPointsResponse from(LoyaltyPoints loyaltyPoints) {
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
