package id.payu.promotion.dto;

import id.payu.promotion.domain.Referral;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateReferralRequest(
    String referrerAccountId,
    BigDecimal referrerReward,
    BigDecimal refereeReward,
    Referral.RewardType rewardType,
    LocalDateTime expiryDate
) {}
