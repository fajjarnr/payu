package id.payu.loanorigination.domain;

import java.math.BigDecimal;

public record DisbursementEvent(
        String userId,
        BigDecimal amount,
        String loanType,
        int tenureMonths,
        String reference
) {}
