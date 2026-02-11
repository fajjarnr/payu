package id.payu.auth.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Login request DTO with input validation.
 *
 * Validation is intentionally lenient for login: only checks presence and size.
 * The actual password complexity rules are enforced during registration (RegisterRequest),
 * not at login time. Keycloak performs the actual credential verification.
 *
 * Username validation:
 * - 3-50 characters, alphanumeric with dots and underscores
 *
 * Password validation:
 * - Present and 1-128 characters (any format accepted — Keycloak validates)
 */
public record LoginRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Username can only contain letters, numbers, dots, and underscores")
    @Sensitive
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 128, message = "Password must not exceed 128 characters")
    @Sensitive(value = Sensitive.SensitivityLevel.CRITICAL)
    String password
) {}
