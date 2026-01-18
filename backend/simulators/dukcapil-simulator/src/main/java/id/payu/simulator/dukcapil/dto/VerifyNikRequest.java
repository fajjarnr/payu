package id.payu.simulator.dukcapil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for NIK verification.
 */
public record VerifyNikRequest(
    @NotBlank(message = "NIK is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "NIK must be exactly 16 digits")
    String nik,
    
    @NotBlank(message = "Full name is required")
    String fullName,
    
    String birthPlace,
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Birth date must be in YYYY-MM-DD format")
    String birthDate
) {}
