package id.payu.simulator.dukcapil.dto;

import id.payu.simulator.dukcapil.entity.Citizen;
import java.time.LocalDate;

/**
 * Response DTO for citizen data retrieval.
 */
public record CitizenDataResponse(
    String requestId,
    String nik,
    String fullName,
    String birthPlace,
    LocalDate birthDate,
    String gender,
    String bloodType,
    String address,
    String rt,
    String rw,
    String village,
    String district,
    String city,
    String province,
    String religion,
    String maritalStatus,
    String occupation,
    String nationality,
    String status,
    String responseCode,
    String responseMessage
) {
    public static CitizenDataResponse fromEntity(String requestId, Citizen citizen) {
        return new CitizenDataResponse(
            requestId,
            citizen.nik,
            citizen.fullName,
            citizen.birthPlace,
            citizen.birthDate,
            citizen.gender != null ? citizen.gender.name() : null,
            citizen.bloodType,
            citizen.address,
            citizen.rt,
            citizen.rw,
            citizen.village,
            citizen.district,
            citizen.city,
            citizen.province,
            citizen.religion != null ? citizen.religion.name() : null,
            citizen.maritalStatus != null ? citizen.maritalStatus.name() : null,
            citizen.occupation,
            citizen.nationality,
            citizen.status.name(),
            "00",
            "Success"
        );
    }

    public static CitizenDataResponse notFound(String requestId, String nik) {
        return new CitizenDataResponse(
            requestId, nik, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            "NOT_FOUND", "14", "NIK not found in database"
        );
    }

    public static CitizenDataResponse blocked(String requestId, String nik) {
        return new CitizenDataResponse(
            requestId, nik, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            "BLOCKED", "62", "NIK is blocked - cannot retrieve data"
        );
    }

    public static CitizenDataResponse error(String requestId, String message) {
        return new CitizenDataResponse(
            requestId, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            "ERROR", "96", message
        );
    }
}
