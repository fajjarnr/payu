package id.payu.simulator.dukcapil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for face matching (KTP photo vs Selfie).
 */
public record FaceMatchRequest(
    @NotBlank(message = "NIK is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "NIK must be exactly 16 digits")
    String nik,
    
    @NotBlank(message = "KTP photo is required")
    String ktpPhotoBase64,
    
    @NotBlank(message = "Selfie photo is required")
    String selfiePhotoBase64,
    
    // Optional: Enable liveness detection
    Boolean livenessCheck
) {
    public FaceMatchRequest {
        if (livenessCheck == null) {
            livenessCheck = true;
        }
    }
}
