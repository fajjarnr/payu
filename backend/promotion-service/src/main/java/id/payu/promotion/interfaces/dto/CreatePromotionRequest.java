package id.payu.promotion.interfaces.dto;

import id.payu.promotion.adapter.persistence.entity.PromotionEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import id.payu.promotion.domain.PromotionRewardType;
import id.payu.promotion.domain.PromotionType;

public record CreatePromotionRequest(
    String code,
    String name,
    String description,
    PromotionType promotionType,
    PromotionRewardType rewardType,
    BigDecimal rewardValue,
    Integer maxRedemptions,
    BigDecimal minTransactionAmount,
    LocalDateTime startDate,
    LocalDateTime endDate
) {}
