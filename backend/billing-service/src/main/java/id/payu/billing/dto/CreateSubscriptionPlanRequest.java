package id.payu.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateSubscriptionPlanRequest(
        @NotBlank(message = "Partner ID is required")
        String partnerId,

        @NotBlank(message = "Plan name is required")
        String planName,

        String description,

        @NotNull(message = "Billing interval is required")
        String billingInterval,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        BigDecimal price,

        String currency,

        @Min(value = 0, message = "Trial days cannot be negative")
        int trialDays,

        @Min(value = 0, message = "Grace period days cannot be negative")
        int gracePeriodDays
) {
}
