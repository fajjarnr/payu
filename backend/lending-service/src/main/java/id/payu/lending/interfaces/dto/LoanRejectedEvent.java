package id.payu.lending.interfaces.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanRejectedEvent(
        UUID loanId,
        UUID userId,
        String externalId,
        BigDecimal principalAmount,
        String reason
) {}
