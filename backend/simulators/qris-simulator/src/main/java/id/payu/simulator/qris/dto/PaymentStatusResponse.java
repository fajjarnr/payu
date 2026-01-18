package id.payu.simulator.qris.dto;

import id.payu.simulator.qris.entity.QrisPayment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment status.
 */
public record PaymentStatusResponse(
    String qrId,
    String referenceNumber,
    String merchantId,
    String merchantName,
    BigDecimal amount,
    BigDecimal tipAmount,
    String currency,
    String payerName,
    String payerAccount,
    String payerBank,
    String status,
    LocalDateTime expiresAt,
    LocalDateTime createdAt,
    LocalDateTime paidAt,
    String failureReason,
    String responseCode,
    String responseMessage
) {
    public static PaymentStatusResponse fromEntity(QrisPayment payment) {
        String responseCode = switch (payment.status) {
            case PENDING -> "09";
            case PAID -> "00";
            case EXPIRED -> "54";
            case FAILED -> "51";
            case CANCELLED -> "56";
            case REFUNDED -> "57";
        };
        
        String responseMessage = switch (payment.status) {
            case PENDING -> "Waiting for payment";
            case PAID -> "Payment completed";
            case EXPIRED -> "QR code has expired";
            case FAILED -> payment.failureReason != null ? payment.failureReason : "Payment failed";
            case CANCELLED -> "Payment cancelled";
            case REFUNDED -> "Payment refunded";
        };

        return new PaymentStatusResponse(
            payment.qrId,
            payment.referenceNumber,
            payment.merchant != null ? payment.merchant.merchantId : null,
            payment.merchant != null ? payment.merchant.merchantName : null,
            payment.amount,
            payment.tipAmount,
            payment.currency,
            payment.payerName,
            payment.payerAccount,
            payment.payerBank,
            payment.status.name(),
            payment.expiresAt,
            payment.createdAt,
            payment.paidAt,
            payment.failureReason,
            responseCode,
            responseMessage
        );
    }

    public static PaymentStatusResponse notFound(String qrId) {
        return new PaymentStatusResponse(
            qrId, null, null, null, null, null, null, null, null, null,
            "NOT_FOUND", null, null, null, null, "14", "QR code not found"
        );
    }

    public static PaymentStatusResponse error(String message) {
        return new PaymentStatusResponse(
            null, null, null, null, null, null, null, null, null, null,
            "ERROR", null, null, null, null, "96", message
        );
    }
}
