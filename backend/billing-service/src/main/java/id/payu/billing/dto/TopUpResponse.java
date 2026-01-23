package id.payu.billing.dto;

import id.payu.billing.domain.BillPayment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TopUpResponse(
    UUID id,
    String referenceNumber,
    String accountId,
    String provider,
    String providerName,
    String walletNumber,
    BigDecimal amount,
    BigDecimal adminFee,
    BigDecimal totalAmount,
    String status,
    String failureReason,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {
    public static TopUpResponse from(BillPayment payment) {
        return new TopUpResponse(
            payment.id,
            payment.referenceNumber,
            payment.accountId,
            payment.billerType.getCode(),
            payment.billerType.getDisplayName(),
            payment.customerId,
            payment.amount,
            payment.adminFee,
            payment.totalAmount,
            payment.status.name(),
            payment.failureReason,
            payment.createdAt,
            payment.completedAt
        );
    }
}