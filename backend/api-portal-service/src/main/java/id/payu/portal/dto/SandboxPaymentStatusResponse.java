package id.payu.portal.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SandboxPaymentStatusResponse(
    String partnerReferenceNo,
    String paymentReferenceNo,
    String originalReferenceNo,
    String transactionDate,
    String paymentStatus,
    Amount amount
) {
    public record Amount(
        BigDecimal value,
        String currency
    ) {
    }
}
