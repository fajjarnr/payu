package id.payu.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import id.payu.security.annotation.SensitivityLevel;

/**
 * DTO for user registration in Keycloak identity provider.
 * Called by account-service during the onboarding flow.
 */
public record RegisterRequest(
    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Sensitive(value = SensitivityLevel.CRITICAL)
    String password,

    @JsonProperty("fullName")
    String fullName
) {}
