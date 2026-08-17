package id.payu.simulator.va.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request to inquire about a Virtual Account.
 * Used by banks to validate VA before payment.
 */
public record VaInquiryRequest(
    @NotBlank(message = "VA number is required")
    @Pattern(regexp = "\\d{10,20}", message = "VA number must be 10-20 digits")
    String vaNumber,

    @NotBlank(message = "Bank code is required")
    String bankCode,

    String customerName,
    String paymentChannel
) {}
