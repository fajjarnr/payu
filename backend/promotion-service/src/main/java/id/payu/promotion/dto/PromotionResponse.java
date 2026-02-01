package id.payu.promotion.dto;

import id.payu.promotion.domain.Promotion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PromotionResponse(
    UUID id,
    String code,
    String name,
    String description,
    Promotion.PromotionType promotionType,
    Promotion.RewardType rewardType,
    BigDecimal rewardValue,
    Integer maxRedemptions,
    Integer redemptionCount,
    BigDecimal minTransactionAmount,
    Promotion.Status status,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime createdAt
) {
    public static PromotionResponse from(Promotion promotion) {
        return new PromotionResponse(
            promotion.getId(),
            promotion.getCode(),
            promotion.getName(),
            promotion.getDescription(),
            promotion.getPromotionType(),
            promotion.getRewardType(),
            promotion.getRewardValue(),
            promotion.getMaxRedemptions(),
            promotion.getRedemptionCount(),
            promotion.getMinTransactionAmount(),
            promotion.getStatus(),
            promotion.getStartDate(),
            promotion.getEndDate(),
            promotion.getCreatedAt()
        );
    }
}
