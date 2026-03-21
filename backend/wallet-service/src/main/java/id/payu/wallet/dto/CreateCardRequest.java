package id.payu.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateCardRequest(
        @NotBlank(message = "Account ID is required")
        String accountId,
        @NotBlank(message = "Card holder name is required")
        String cardHolderName,
        @NotNull(message = "Daily limit is required")
        @Positive(message = "Daily limit must be positive")
        BigDecimal dailyLimit) {
}
