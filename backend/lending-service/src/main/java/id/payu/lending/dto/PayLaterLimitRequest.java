package id.payu.lending.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayLaterLimitRequest(
        @NotNull(message = "Credit limit is required")
        @DecimalMin(value = "500000.00", message = "Credit limit must be at least 500,000")
        BigDecimal creditLimit,

        Integer billingCycleDay
) {}
