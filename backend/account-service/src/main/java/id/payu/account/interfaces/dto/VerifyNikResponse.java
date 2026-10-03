package id.payu.account.interfaces.dto;

import java.time.LocalDate;

/**
 * Response DTO for NIK verification.
 * Contains only essential information for security and privacy.
 */
public record VerifyNikResponse(
    String requestId,
    String nik,
    boolean verified,
    String fullName,
    String birthPlace,
    LocalDate birthDate,
    String gender,
    String address,
    String status,
    String responseCode,
    String responseMessage
) {
    public static VerifyNikResponse success(
        String requestId,
        String nik,
        boolean verified,
        String fullName,
        String birthPlace,
        LocalDate birthDate,
        String gender,
        String address,
        String status
    ) {
        return new VerifyNikResponse(
            requestId,
            nik,
            verified,
            fullName,
            birthPlace,
            birthDate,
            gender,
            address,
            status,
            "00",
            verified ? "Verification successful" : "Name mismatch detected"
        );
    }

    public static VerifyNikResponse notFound(String requestId, String nik) {
        return new VerifyNikResponse(
            requestId,
            nik,
            false,
            null,
            null,
            null,
            null,
            null,
            "NOT_FOUND",
            "14",
            "NIK not found in Dukcapil database"
        );
    }

    public static VerifyNikResponse blocked(String requestId, String nik) {
        return new VerifyNikResponse(
            requestId,
            nik,
            false,
            null,
            null,
            null,
            null,
            null,
            "BLOCKED",
            "62",
            "NIK is blocked or flagged"
        );
    }

    public static VerifyNikResponse invalid(String requestId, String nik) {
        return new VerifyNikResponse(
            requestId,
            nik,
            false,
            null,
            null,
            null,
            null,
            null,
            "INVALID",
            "30",
            "Invalid NIK format"
        );
    }

    public static VerifyNikResponse error(String requestId, String message) {
        return new VerifyNikResponse(
            requestId,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            "ERROR",
            "96",
            message
        );
    }

    public static VerifyNikResponse serviceUnavailable(String requestId) {
        return new VerifyNikResponse(
            requestId,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            "UNAVAILABLE",
            "503",
            "Dukcapil service is temporarily unavailable"
        );
    }
}
