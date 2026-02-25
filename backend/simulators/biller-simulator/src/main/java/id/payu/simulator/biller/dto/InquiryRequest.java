package id.payu.simulator.biller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Bill inquiry request — check outstanding balance for a customer.
 */
public record InquiryRequest(
        @NotBlank String billerCode,
        @NotBlank String customerNumber
) {}
