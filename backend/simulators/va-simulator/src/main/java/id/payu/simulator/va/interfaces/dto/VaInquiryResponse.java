package id.payu.simulator.va.interfaces.dto;

import java.math.BigDecimal;

/**
 * Response to VA inquiry with customer and amount information.
 */
public record VaInquiryResponse(
    String responseCode,
    String responseMessage,
    String vaNumber,
    String bankCode,
    String customerName,
    BigDecimal amount,
    String currency,
    String description,
    String status,
    String expiryTime
) {

    public static VaInquiryResponse success(String vaNumber, String bankCode,
                                            String customerName, BigDecimal amount,
                                            String currency, String description,
                                            String expiryTime) {
        return new VaInquiryResponse(
            "00",
            "Success",
            vaNumber,
            bankCode,
            customerName,
            amount,
            currency,
            description,
            "ACTIVE",
            expiryTime
        );
    }

    public static VaInquiryResponse notFound(String vaNumber) {
        return new VaInquiryResponse(
            "14",
            "Virtual Account not found",
            vaNumber,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public static VaInquiryResponse expired(String vaNumber) {
        return new VaInquiryResponse(
            "54",
            "Virtual Account expired",
            vaNumber,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public static VaInquiryResponse alreadyPaid(String vaNumber) {
        return new VaInquiryResponse(
            "94",
            "Virtual Account already paid",
            vaNumber,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
