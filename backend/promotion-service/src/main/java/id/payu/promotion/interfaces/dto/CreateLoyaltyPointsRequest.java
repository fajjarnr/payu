package id.payu.promotion.interfaces.dto;

import java.time.LocalDateTime;
import id.payu.promotion.domain.TransactionType;

public record CreateLoyaltyPointsRequest(
    String accountId,
    String transactionId,
    TransactionType transactionType,
    Integer points,
    LocalDateTime expiryDate
) {}
