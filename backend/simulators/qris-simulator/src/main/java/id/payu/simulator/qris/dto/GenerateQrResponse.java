package id.payu.simulator.qris.dto;

import id.payu.simulator.qris.entity.QrisPayment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for QR generation.
 */
public record GenerateQrResponse(
    String qrId,
    String referenceNumber,
    String merchantId,
    String merchantName,
    BigDecimal amount,
    BigDecimal tipAmount,
    String currency,
    String qrContent,
    String qrImageBase64,
    String status,
    LocalDateTime expiresAt,
    LocalDateTime createdAt,
    String responseCode,
    String responseMessage
) {
    public static GenerateQrResponse fromEntity(QrisPayment payment) {
        return new GenerateQrResponse(
            payment.qrId,
            payment.referenceNumber,
            payment.merchant != null ? payment.merchant.merchantId : null,
            payment.merchant != null ? payment.merchant.merchantName : null,
            payment.amount,
            payment.tipAmount,
            payment.currency,
            payment.qrContent,
            payment.qrImageBase64,
            payment.status.name(),
            payment.expiresAt,
            payment.createdAt,
            "00",
            "QR code generated successfully"
        );
    }

    public static GenerateQrResponse merchantNotFound(String merchantId) {
        return new GenerateQrResponse(
            null, null, merchantId, null, null, null, null, null, null,
            "ERROR", null, null, "14", "Merchant not found: " + merchantId
        );
    }

    public static GenerateQrResponse merchantBlocked(String merchantId) {
        return new GenerateQrResponse(
            null, null, merchantId, null, null, null, null, null, null,
            "BLOCKED", null, null, "62", "Merchant is blocked"
        );
    }

    public static GenerateQrResponse error(String message) {
        return new GenerateQrResponse(
            null, null, null, null, null, null, null, null, null,
            "ERROR", null, null, "96", message
        );
    }
}
