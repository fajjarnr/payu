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
            reward.id,
            reward.accountId,
            reward.transactionId,
            reward.promotionCode,
            reward.type,
            reward.amount,
            reward.pointsEarned,
            reward.transactionAmount,
            reward.status,
            reward.expiryDate,
            reward.createdAt
        );
    }
}
