package id.payu.promotion.dto;

import id.payu.promotion.adapter.persistence.entity.ReferralEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import id.payu.promotion.domain.ReferralRewardType;

public record CreateReferralRequest(
    String referrerAccountId,
    BigDecimal referrerReward,
    BigDecimal refereeReward,
    ReferralRewardType rewardType,
    LocalDateTime expiryDate
) {}
