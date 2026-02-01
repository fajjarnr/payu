package id.payu.promotion.dto;

import id.payu.promotion.domain.Reward;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RewardResponse(
    UUID id,
    String accountId,
    String transactionId,
    String promotionCode,
    Reward.RewardType type,
    BigDecimal amount,
    Integer pointsEarned,
    BigDecimal transactionAmount,
    Reward.Status status,
    LocalDateTime expiryDate,
    LocalDateTime createdAt
) {
    public static RewardResponse from(Reward reward) {
        return new RewardResponse(
            reward.getId(),
            reward.getAccountId(),
            reward.getTransactionId(),
            reward.getPromotionCode(),
            reward.getType(),
            reward.getAmount(),
            reward.getPointsEarned(),
            reward.getTransactionAmount(),
            reward.getStatus(),
            reward.getExpiryDate(),
            reward.getCreatedAt()
        );
    }
}
