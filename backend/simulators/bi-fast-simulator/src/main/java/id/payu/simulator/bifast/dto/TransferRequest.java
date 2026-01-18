package id.payu.simulator.bifast.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request DTO for fund transfer.
 */
public record TransferRequest(
    @NotBlank(message = "Source bank code is required")
    String sourceBankCode,
    
    @NotBlank(message = "Source account number is required")
    String sourceAccountNumber,
    
    @NotBlank(message = "Source account name is required")
    String sourceAccountName,
    
    @NotBlank(message = "Destination bank code is required")
    String destinationBankCode,
    
    @NotBlank(message = "Destination account number is required")
    String destinationAccountNumber,
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMin(value = "1.00", message = "Minimum transfer amount is IDR 1")
    @DecimalMax(value = "50000000.00", message = "Maximum transfer amount is IDR 50,000,000")
    BigDecimal amount,
    
    String currency,
    
    @Size(max = 255, message = "Description too long")
    String description,
    
    @Size(max = 500, message = "Webhook URL too long")
    String webhookUrl
) {
    public TransferRequest {
        if (currency == null || currency.isBlank()) {
            currency = "IDR";
        }
    }
}
