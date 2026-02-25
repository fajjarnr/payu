package id.payu.simulator.biller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Bill payment request — pay a bill for a customer.
 */
public record PaymentRequest(
        @NotBlank String billerCode,
        @NotBlank String customerNumber,
        @Positive BigDecimal amount,
        @NotBlank String referenceNumber
) {}
