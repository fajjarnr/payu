package id.payu.portal.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SandboxPaymentResponse(
    String partnerReferenceNo,
    String paymentReferenceNo,
    String originalReferenceNo,
    String transactionDate,
    String paymentStatus,
    Amount amount,
    String beneficiaryAccountNo,
    String beneficiaryBankCode,
    String sourceAccountNo
) {
    public record Amount(
        BigDecimal value,
        String currency
    ) {
    }
}
