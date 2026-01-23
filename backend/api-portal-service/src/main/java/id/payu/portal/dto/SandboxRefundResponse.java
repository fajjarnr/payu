package id.payu.portal.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SandboxRefundResponse(
    String refundReferenceNo,
    String originalReferenceNo,
    String refundDate,
    String refundStatus,
    Amount amount
) {
    public record Amount(
        BigDecimal value,
        String currency
    ) {
    }
}
