package id.payu.promotion.dto;

public record RedeemLoyaltyPointsRequest(
    String accountId,
    Integer points,
    String transactionId
) {}
