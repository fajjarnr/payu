package id.payu.promotion.dto;

public record ReferralSummaryResponse(
    String referralCode,
    int totalReferrals,
    int completedReferrals,
    int pendingReferrals
) {}
