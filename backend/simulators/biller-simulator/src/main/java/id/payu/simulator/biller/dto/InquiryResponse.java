package id.payu.simulator.biller.dto;

import java.math.BigDecimal;

/**
 * Bill inquiry response — returns customer info and outstanding balance.
 */
public record InquiryResponse(
        String responseCode,
        String responseMessage,
        String billerCode,
        String customerNumber,
        String customerName,
        BigDecimal outstandingAmount
) {
    public static InquiryResponse success(String billerCode, String customerNumber,
                                           String customerName, BigDecimal outstanding) {
        return new InquiryResponse("00", "SUCCESS", billerCode, customerNumber,
                customerName, outstanding);
    }

    public static InquiryResponse notFound(String billerCode, String customerNumber) {
        return new InquiryResponse("14", "CUSTOMER_NOT_FOUND", billerCode, customerNumber,
                null, null);
    }

    public static InquiryResponse blocked(String billerCode, String customerNumber) {
        return new InquiryResponse("62", "ACCOUNT_BLOCKED", billerCode, customerNumber,
                null, null);
    }

    public static InquiryResponse error(String message) {
        return new InquiryResponse("96", message, null, null, null, null);
    }
}
