package id.payu.promotion.dto;

import id.payu.promotion.domain.LoyaltyPoints;
import java.time.LocalDateTime;

public record CreateLoyaltyPointsRequest(
    String accountId,
    String transactionId,
    LoyaltyPoints.TransactionType transactionType,
    Integer points,
    LocalDateTime expiryDate
) {}
