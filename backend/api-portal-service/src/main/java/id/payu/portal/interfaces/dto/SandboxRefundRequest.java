package id.payu.portal.interfaces.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SandboxRefundRequest(
    String refundReferenceNo,
    String reason
) {
}
