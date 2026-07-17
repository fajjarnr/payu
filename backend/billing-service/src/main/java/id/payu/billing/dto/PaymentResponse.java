package id.payu.billing.dto;

import id.payu.billing.domain.model.BillPayment;
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
            payment.getId(),
            payment.getReferenceNumber(),
            payment.getAccountId(),
            payment.getBillerType().getCode(),
            payment.getBillerType().getDisplayName(),
            payment.getCustomerId(),
            payment.getAmount(),
            payment.getAdminFee(),
            payment.getTotalAmount(),
            payment.getStatus().name(),
            payment.getFailureReason(),
            payment.getCreatedAt(),
            payment.getCompletedAt()
        );
    }
}
