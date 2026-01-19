package id.payu.billing.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request DTO for creating a bill payment.
 */
public record CreatePaymentRequest(
    @NotBlank(message = "Account ID is required")
    String accountId,

    @NotBlank(message = "Biller code is required")
    String billerCode,

    @NotBlank(message = "Customer ID is required")
    String customerId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount
) {}
