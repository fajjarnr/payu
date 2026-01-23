package id.payu.lending.dto;

import id.payu.lending.domain.model.Loan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanPreApprovalRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Loan type is required")
        Loan.LoanType loanType,

        @NotNull(message = "Principal amount is required")
        @Positive(message = "Principal amount must be positive")
        BigDecimal principalAmount,

        @NotNull(message = "Tenure months is required")
        @Min(value = 1, message = "Tenure must be at least 1 month")
        @Max(value = 60, message = "Tenure cannot exceed 60 months")
        Integer tenureMonths,

        @Size(max = 200, message = "Purpose must not exceed 200 characters")
        String purpose
) {}
