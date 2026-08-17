package id.payu.promotion.interfaces.dto;

import id.payu.promotion.domain.model.Reward;
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
    public static RewardResponse from(Reward reward) {
        return new RewardResponse(
            reward.id(),
            reward.accountId(),
            reward.transactionId(),
            reward.promotionCode(),
            reward.type(),
            reward.amount(),
            reward.pointsEarned(),
            reward.transactionAmount(),
            reward.status(),
            reward.expiryDate(),
            reward.createdAt()
        );
    }

}
