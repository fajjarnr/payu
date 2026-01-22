package id.payu.lending.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LoanApprovedEvent(
        UUID loanId,
        UUID userId,
        String externalId,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        Integer tenureMonths,
        BigDecimal monthlyInstallment,
        LocalDate disbursementDate
) {}
