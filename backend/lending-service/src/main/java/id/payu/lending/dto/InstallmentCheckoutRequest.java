package id.payu.lending.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InstallmentCheckoutRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotBlank(message = "Partner ID is required")
        String partnerId,

        String externalOrderId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "10000", message = "Minimum amount is 10000")
        BigDecimal amount,

        @Min(value = 1, message = "Tenor must be at least 1 month")
        int tenor
) {
}
