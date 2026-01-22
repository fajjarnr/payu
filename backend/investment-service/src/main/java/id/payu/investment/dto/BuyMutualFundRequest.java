package id.payu.investment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BuyMutualFundRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotBlank(message = "Fund code is required")
    String fundCode,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000.00", message = "Minimum investment is Rp 10,000")
    BigDecimal amount
) {}
