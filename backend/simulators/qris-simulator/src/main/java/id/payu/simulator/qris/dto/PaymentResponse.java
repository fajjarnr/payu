package id.payu.simulator.qris.dto;

import id.payu.simulator.qris.entity.QrisPayment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment result.
 */
public record PaymentResponse(
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
    LocalDateTime paidAt,
    String responseCode,
    String responseMessage
) {
    public static PaymentResponse success(QrisPayment payment) {
        return new PaymentResponse(
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
            "PAID",
            payment.paidAt,
            "00",
            "Payment successful"
        );
    }

    public static PaymentResponse notFound(String qrId) {
        return new PaymentResponse(
            qrId, null, null, null, null, null, null, null, null, null,
            "NOT_FOUND", null, "14", "QR code not found"
        );
    }

    public static PaymentResponse expired(QrisPayment payment) {
        return new PaymentResponse(
            payment.qrId, payment.referenceNumber,
            payment.merchant != null ? payment.merchant.merchantId : null, null,
            payment.amount, null, payment.currency, null, null, null,
            "EXPIRED", null, "54", "QR code has expired"
        );
    }

    public static PaymentResponse alreadyPaid(QrisPayment payment) {
        return new PaymentResponse(
            payment.qrId, payment.referenceNumber,
            payment.merchant != null ? payment.merchant.merchantId : null,
            payment.merchant != null ? payment.merchant.merchantName : null,
            payment.amount, payment.tipAmount, payment.currency,
            payment.payerName, payment.payerAccount, payment.payerBank,
            "ALREADY_PAID", payment.paidAt, "55", "QR code already paid"
        );
    }

    public static PaymentResponse failed(QrisPayment payment, String reason) {
        return new PaymentResponse(
            payment.qrId, payment.referenceNumber,
            payment.merchant != null ? payment.merchant.merchantId : null, null,
            payment.amount, null, payment.currency, null, null, null,
            "FAILED", null, "51", reason
        );
    }

    public static PaymentResponse error(String message) {
        return new PaymentResponse(
            null, null, null, null, null, null, null, null, null, null,
            "ERROR", null, "96", message
        );
    }
}
