package id.payu.promotion.dto;

import id.payu.promotion.domain.Promotion;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreatePromotionRequest(
    String code,
    String name,
    String description,
    Promotion.PromotionType promotionType,
    Promotion.RewardType rewardType,
    BigDecimal rewardValue,
    Integer maxRedemptions,
    BigDecimal minTransactionAmount,
    LocalDateTime startDate,
    LocalDateTime endDate
) {}
