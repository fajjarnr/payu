package id.payu.promotion.dto;

public record LoyaltyBalanceResponse(
    Integer currentBalance,
    Integer totalEarned,
    Integer totalRedeemed,
    Integer expiredPoints
) {}
