package id.payu.promotion.interfaces.dto;

import java.math.BigDecimal;

public record ReferralSummaryResponse(
    String referralCode,
    int totalReferrals,
    int completedReferrals,
    int pendingReferrals,
    BigDecimal totalEarnings
) {}
