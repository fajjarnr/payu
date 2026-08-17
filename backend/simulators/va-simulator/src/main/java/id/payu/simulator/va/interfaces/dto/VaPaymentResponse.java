package id.payu.simulator.va.interfaces.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response to VA payment with confirmation details.
 */
public record VaPaymentResponse(
    String responseCode,
    String responseMessage,
    String vaNumber,
    String paymentReference,
    BigDecimal amount,
    String currency,
    String status,
    Instant paymentTime,
    String callbackStatus
) {

    public static VaPaymentResponse success(String vaNumber, String paymentReference,
                                            BigDecimal amount, String currency,
                                            String callbackStatus) {
        return new VaPaymentResponse(
            "00",
            "Payment successful",
            vaNumber,
            paymentReference,
            amount,
            currency,
            "PAID",
            Instant.now(),
            callbackStatus
        );
    }

    public static VaPaymentResponse notFound(String vaNumber) {
        return new VaPaymentResponse(
            "14",
            "Virtual Account not found",
            vaNumber,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public static VaPaymentResponse expired(String vaNumber) {
        return new VaPaymentResponse(
            "54",
            "Virtual Account expired",
            vaNumber,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public static VaPaymentResponse alreadyPaid(String vaNumber) {
        return new VaPaymentResponse(
            "94",
            "Virtual Account already paid",
            vaNumber,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public static VaPaymentResponse amountMismatch(String vaNumber, BigDecimal expected, BigDecimal actual) {
        return new VaPaymentResponse(
            "13",
            String.format("Amount mismatch. Expected: %s, Received: %s", expected, actual),
            vaNumber,
            null,
            actual,
            null,
            null,
            null,
            null
        );
    }

    public static VaPaymentResponse callbackFailed(String vaNumber, String paymentReference) {
        return new VaPaymentResponse(
            "68",
            "Payment recorded but callback failed",
            vaNumber,
            paymentReference,
            null,
            null,
            "PAID",
            Instant.now(),
            "FAILED"
        );
    }
}
