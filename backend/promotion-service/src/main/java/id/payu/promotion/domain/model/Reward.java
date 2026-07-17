package id.payu.promotion.domain.model;

import id.payu.promotion.domain.RewardStatus;
import id.payu.promotion.domain.RewardType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record Reward(
    UUID id,
    String accountId,
    String transactionId,
    String promotionCode,
    RewardType type,
    BigDecimal amount,
    Integer pointsEarned,
    BigDecimal transactionAmount,
    String merchantCode,
    String categoryCode,
    RewardStatus status,
    LocalDateTime expiryDate,
    LocalDateTime createdAt,
    Long version
) {
}
