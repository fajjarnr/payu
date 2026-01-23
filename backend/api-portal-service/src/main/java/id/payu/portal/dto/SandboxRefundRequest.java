package id.payu.portal.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SandboxRefundRequest(
    String refundReferenceNo,
    String reason
) {
}
