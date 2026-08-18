package id.payu.wallet.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateCardRequest(
        @NotNull(message = "Daily limit is required")
        @Positive(message = "Daily limit must be positive")
        BigDecimal dailyLimit) {
}
