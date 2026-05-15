package id.payu.account.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import id.payu.security.annotation.SensitivityLevel;

public record RegisterUserRequest(
    @NotBlank(message = "External ID is required")
    String externalId,

    @NotBlank(message = "Username is required")
    @Sensitive
    String username,

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "Invalid email format")
    @Sensitive
    String email,

    @Sensitive
    String phoneNumber,

    @NotBlank(message = "Full Name is required")
    @Sensitive
    String fullName,

    @NotBlank(message = "NIK is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "NIK must be exactly 16 digits")
    @Sensitive
    String nik,

    @NotBlank(message = "Password is required")
    @Sensitive(value = SensitivityLevel.CRITICAL)
    String password
) {}
