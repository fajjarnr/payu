package id.payu.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterUserRequest(
    @NotBlank(message = "External ID is required")
    String externalId,

    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "Invalid email format")
    String email,

    String phoneNumber,
    
    @NotBlank(message = "Full Name is required")
    String fullName,

    @NotBlank(message = "NIK is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "NIK must be exactly 16 digits")
    String nik
) {}
