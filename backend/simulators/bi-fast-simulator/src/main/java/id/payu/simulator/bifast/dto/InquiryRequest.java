package id.payu.simulator.bifast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Request DTO for account inquiry.
 */
public record InquiryRequest(
    @NotBlank(message = "Bank code is required")
    @Pattern(regexp = "^[A-Z]{3,10}$", message = "Invalid bank code format")
    String bankCode,
    
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{10,20}$", message = "Invalid account number format")
    String accountNumber
) {}
