package id.payu.promotion.dto;

import id.payu.promotion.adapter.persistence.entity.ReferralEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.promotion.domain.ReferralRewardType;
import id.payu.promotion.domain.ReferralStatus;

public record ReferralResponse(
    UUID id,
    String referrerAccountId,
    String refereeAccountId,
    String referralCode,
    BigDecimal referrerReward,
    BigDecimal refereeReward,
    ReferralRewardType rewardType,
    ReferralStatus status,
    LocalDateTime completedAt,
    LocalDateTime expiryDate,
    LocalDateTime createdAt
) {
    public static ReferralResponse from(ReferralEntity referral) {
        return new ReferralResponse(
            referral.getId(),
            referral.getReferrerAccountId(),
            referral.getRefereeAccountId(),
            referral.getReferralCode(),
            referral.getReferrerReward(),
            referral.getRefereeReward(),
            referral.getRewardType(),
            referral.getStatus(),
            referral.getCompletedAt(),
            referral.getExpiryDate(),
            referral.getCreatedAt()
        );
    }
}
