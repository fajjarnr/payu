package id.payu.simulator.biller.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Bill payment response — result of processing a payment.
 */
public record PaymentResponse(
        String responseCode,
        String responseMessage,
        String billerTransactionId,
        String billerCode,
        String customerNumber,
        BigDecimal amount,
        String status,
        Instant completedAt
) {
    public static PaymentResponse success(String billerTxId, String billerCode,
                                           String customerNumber, BigDecimal amount) {
        return new PaymentResponse("00", "PAYMENT_SUCCESS", billerTxId, billerCode,
                customerNumber, amount, "COMPLETED", Instant.now());
    }

    public static PaymentResponse duplicate(String billerTxId) {
        return new PaymentResponse("94", "DUPLICATE_REFERENCE", billerTxId, null,
                null, null, "DUPLICATE", null);
    }

    public static PaymentResponse customerNotFound(String billerCode, String customerNumber) {
        return new PaymentResponse("14", "CUSTOMER_NOT_FOUND", null, billerCode,
                customerNumber, null, "FAILED", null);
    }

    public static PaymentResponse insufficientBill(String billerCode, String customerNumber) {
        return new PaymentResponse("51", "AMOUNT_EXCEEDS_BILL", null, billerCode,
                customerNumber, null, "FAILED", null);
    }

    public static PaymentResponse error(String message) {
        return new PaymentResponse("96", message, null, null, null, null, "FAILED", null);
    }
}
