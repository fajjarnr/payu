package id.payu.simulator.qris.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request DTO for generating QRIS code.
 */
public record GenerateQrRequest(
    @NotBlank(message = "Merchant ID is required")
    String merchantId,
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMin(value = "100", message = "Minimum amount is IDR 100")
    @DecimalMax(value = "10000000", message = "Maximum amount is IDR 10,000,000")
    BigDecimal amount,
    
    @PositiveOrZero(message = "Tip amount must be zero or positive")
    BigDecimal tipAmount,
    
    @Size(max = 100, message = "Description too long")
    String description,
    
    // QR expiry in seconds (default: 300 = 5 minutes)
    Integer expirySeconds,
    
    // Webhook URL for payment notification
    @Size(max = 500, message = "Webhook URL too long")
    String webhookUrl
) {
    public GenerateQrRequest {
        if (expirySeconds == null || expirySeconds <= 0) {
            expirySeconds = 300; // 5 minutes default
        }
        if (tipAmount == null) {
            tipAmount = BigDecimal.ZERO;
        }
    }
}
