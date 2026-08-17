package id.payu.investment.interfaces.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SellInvestmentRequest(
    @NotBlank(message = "Account ID is required")
    String accountId,

    @NotNull(message = "Transaction ID is required")
    UUID transactionId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount
) {}
