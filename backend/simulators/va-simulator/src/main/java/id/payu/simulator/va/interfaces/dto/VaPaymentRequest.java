package id.payu.simulator.va.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request to make a payment to a Virtual Account.
 * Simulates bank transfer to VA number.
 */
public record VaPaymentRequest(
    @NotBlank(message = "VA number is required")
    String vaNumber,

    @NotBlank(message = "Bank code is required")
    String bankCode,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    String currency,

    @NotBlank(message = "Customer account number is required")
    String customerAccountNumber,

    String customerAccountName,

    String paymentChannel,

    String referenceNumber
) {}
