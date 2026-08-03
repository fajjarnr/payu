package id.payu.lending.interfaces.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RepaymentRequest(
        @NotNull
        @DecimalMin(value = "0.0001", inclusive = true)
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount
) {}
