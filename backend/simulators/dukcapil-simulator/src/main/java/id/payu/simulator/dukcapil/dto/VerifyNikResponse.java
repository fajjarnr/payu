package id.payu.simulator.dukcapil.dto;

import id.payu.simulator.dukcapil.entity.Citizen;
import java.time.LocalDate;

/**
 * Response DTO for NIK verification.
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
    public static VerifyNikResponse success(String requestId, Citizen citizen, boolean nameMatch) {
        return new VerifyNikResponse(
            requestId,
            citizen.nik,
            nameMatch,
            citizen.fullName,
            citizen.birthPlace,
            citizen.birthDate,
            citizen.gender != null ? citizen.gender.name() : null,
            citizen.address,
            citizen.status.name(),
            "00",
            nameMatch ? "Verification successful - Data matched" : "Verification failed - Name mismatch"
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
            "NIK not found in database"
        );
    }

    public static VerifyNikResponse blocked(String requestId, Citizen citizen) {
        return new VerifyNikResponse(
            requestId,
            citizen.nik,
            false,
            citizen.fullName,
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
            "Invalid NIK format or checksum"
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
}
