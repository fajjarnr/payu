package id.payu.billing.domain.port.out;

import java.math.BigDecimal;
import java.time.Instant;

public interface BillerPort {
    InquiryResult inquiry(String billerCode, String customerNumber);
    PaymentResult pay(String billerCode, String customerNumber, BigDecimal amount, String referenceNumber);

    record InquiryResult(
        String responseCode,
        String responseMessage,
        String customerName,
        BigDecimal outstandingAmount
    ) {}

    record PaymentResult(
        String responseCode,
        String responseMessage,
        String billerTransactionId,
        String status,
        Instant completedAt
    ) {
        public boolean isSuccess() {
            return "00".equals(responseCode);
        }

        public boolean isDuplicate() {
            return "94".equals(responseCode);
        }
    }
}
