package id.payu.investment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BuyDepositRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000000.00", message = "Minimum deposit amount is Rp 1,000,000")
    BigDecimal amount,
    
    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Minimum tenure is 1 month")
    @Max(value = 12, message = "Maximum tenure is 12 months")
    Integer tenure
) {}
