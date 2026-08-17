package id.payu.promotion.interfaces.dto;

public record CompleteReferralRequest(
    String referralCode,
    String refereeAccountId
) {}
