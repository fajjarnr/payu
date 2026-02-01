package id.payu.promotion.dto;

import id.payu.promotion.domain.Referral;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReferralResponse(
    UUID id,
    String referrerAccountId,
    String refereeAccountId,
    String referralCode,
    BigDecimal referrerReward,
    BigDecimal refereeReward,
    Referral.RewardType rewardType,
    Referral.Status status,
    LocalDateTime completedAt,
    LocalDateTime expiryDate,
    LocalDateTime createdAt
) {
    public static ReferralResponse from(Referral referral) {
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
