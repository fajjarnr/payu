package id.payu.promotion.dto;

import id.payu.promotion.adapter.persistence.entity.RewardEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.promotion.domain.RewardStatus;
import id.payu.promotion.domain.RewardType;

public record RewardResponse(
    UUID id,
    String accountId,
    String transactionId,
    String promotionCode,
    RewardType type,
    BigDecimal amount,
    Integer pointsEarned,
    BigDecimal transactionAmount,
    RewardStatus status,
    LocalDateTime expiryDate,
    LocalDateTime createdAt
) {
    public static RewardResponse from(RewardEntity reward) {
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
