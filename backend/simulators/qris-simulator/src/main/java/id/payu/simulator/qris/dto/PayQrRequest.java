package id.payu.simulator.qris.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Request DTO for simulating QRIS payment.
 */
public record PayQrRequest(
    @NotBlank(message = "QR ID is required")
    String qrId,
    
    @NotBlank(message = "Payer name is required")
    String payerName,
    
    @NotBlank(message = "Payer account is required")
    String payerAccount,
    
    @NotBlank(message = "Payer bank is required")
    String payerBank,
    
    // Optional: Override amount for static QR
    @Positive(message = "Amount must be positive")
    BigDecimal amount,
    
    // Optional: Add tip
    BigDecimal tipAmount,
    
    // Simulate failure (for testing)
    Boolean simulateFailure
) {
    public PayQrRequest {
        if (simulateFailure == null) {
            simulateFailure = false;
        }
    }
}
