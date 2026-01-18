package id.payu.simulator.bifast.dto;

import id.payu.simulator.bifast.entity.BankAccount;

/**
 * Response DTO for account inquiry.
 */
public record InquiryResponse(
    String bankCode,
    String accountNumber,
    String accountName,
    String status,
    String responseCode,
    String responseMessage
) {
    public static InquiryResponse success(BankAccount account) {
        return new InquiryResponse(
            account.bankCode,
            account.accountNumber,
            account.accountName,
            account.status.name(),
            "00",
            "Success"
        );
    }

    public static InquiryResponse notFound(String bankCode, String accountNumber) {
        return new InquiryResponse(
            bankCode,
            accountNumber,
            null,
            "NOT_FOUND",
            "14",
            "Account not found"
        );
    }

    public static InquiryResponse blocked(BankAccount account) {
        return new InquiryResponse(
            account.bankCode,
            account.accountNumber,
            account.accountName,
            "BLOCKED",
            "62",
            "Account is blocked"
        );
    }

    public static InquiryResponse timeout() {
        return new InquiryResponse(
            null,
            null,
            null,
            "TIMEOUT",
            "68",
            "Request timeout"
        );
    }

    public static InquiryResponse error(String message) {
        return new InquiryResponse(
            null,
            null,
            null,
            "ERROR",
            "96",
            message
        );
    }
}
