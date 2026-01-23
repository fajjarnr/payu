package id.payu.portal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record SandboxPaymentRequest(
    String partnerReferenceNo,
    Amount amount,
    String beneficiaryAccountNo,
    String beneficiaryBankCode,
    String sourceAccountNo,
    Map<String, Object> additionalInfo
) {
    public record Amount(
        BigDecimal value,
        String currency
    ) {
    }
}
