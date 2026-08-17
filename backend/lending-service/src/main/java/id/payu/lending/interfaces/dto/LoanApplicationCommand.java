package id.payu.lending.interfaces.dto;

import id.payu.lending.domain.model.Loan;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import id.payu.lending.domain.model.LoanType;

/**
 * Command object for loan application request body.
 * User ID is extracted from JWT token, not from request body.
 */
public record LoanApplicationCommand(
        @NotBlank(message = "External ID is required")
        String externalId,

        @NotNull(message = "Loan type is required")
        LoanType loanType,

        @NotNull(message = "Principal amount is required")
        @DecimalMin(value = "100000.00", message = "Principal amount must be at least 100,000")
        BigDecimal principalAmount,

        @NotNull(message = "Tenure months is required")
        @Positive(message = "Tenure months must be positive")
        Integer tenureMonths,

        String purpose
) {}
