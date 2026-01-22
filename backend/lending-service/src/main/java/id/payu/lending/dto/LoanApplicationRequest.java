package id.payu.lending.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanApplicationRequest(
        @NotBlank(message = "User ID is required")
        UUID userId,

        @NotBlank(message = "External ID is required")
        String externalId,

        @NotNull(message = "Loan type is required")
        id.payu.lending.domain.model.Loan.LoanType loanType,

        @NotNull(message = "Principal amount is required")
        @DecimalMin(value = "100000.00", message = "Principal amount must be at least 100,000")
        BigDecimal principalAmount,

        @NotNull(message = "Tenure months is required")
        @Positive(message = "Tenure months must be positive")
        Integer tenureMonths,

        String purpose
) {}
