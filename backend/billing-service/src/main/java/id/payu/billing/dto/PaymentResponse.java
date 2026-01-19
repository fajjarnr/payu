package id.payu.billing.dto;

import id.payu.billing.domain.BillPayment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for bill payment.
 */
public record PaymentResponse(
    UUID id,
    String referenceNumber,
    String accountId,
    String billerCode,
    String billerName,
    String customerId,
    BigDecimal amount,
    BigDecimal adminFee,
    BigDecimal totalAmount,
    String status,
    String failureReason,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {
    public static PaymentResponse from(BillPayment payment) {
        return new PaymentResponse(
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
