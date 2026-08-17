package id.payu.promotion.interfaces.dto;

public record RedeemLoyaltyPointsRequest(
    String accountId,
    Integer points,
    String transactionId
) {}
