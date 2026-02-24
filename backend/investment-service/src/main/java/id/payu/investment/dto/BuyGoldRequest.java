package id.payu.investment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BuyGoldRequest(
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum gold purchase is Rp 1,000")
    BigDecimal amount
) {}
