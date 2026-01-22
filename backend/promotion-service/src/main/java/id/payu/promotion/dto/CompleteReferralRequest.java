package id.payu.promotion.dto;

public record CompleteReferralRequest(
    String referralCode,
    String refereeAccountId
) {}
