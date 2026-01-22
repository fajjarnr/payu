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
            promotion.id,
            promotion.code,
            promotion.name,
            promotion.description,
            promotion.promotionType,
            promotion.rewardType,
            promotion.rewardValue,
            promotion.maxRedemptions,
            promotion.redemptionCount,
            promotion.minTransactionAmount,
            promotion.status,
            promotion.startDate,
            promotion.endDate,
            promotion.createdAt
        );
    }
}
