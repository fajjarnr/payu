package id.payu.investment.interfaces.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateInvestmentAccountRequest(
    @NotBlank(message = "User ID is required")
    String userId
) {}
