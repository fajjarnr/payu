package id.payu.loanorigination.domain;

import java.math.BigDecimal;

public record LoanOriginationRequest(
        String userId,
        BigDecimal principalAmount,
        int tenureMonths,
        String purpose,
        String loanType
) {}
