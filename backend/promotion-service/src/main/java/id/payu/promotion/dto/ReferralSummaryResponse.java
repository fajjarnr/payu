package id.payu.promotion.dto;

import java.math.BigDecimal;

public record ReferralSummaryResponse(
    String referralCode,
    int totalReferrals,
    int completedReferrals,
    int pendingReferrals,
    BigDecimal totalEarnings
) {}
