package id.payu.auth.interfaces.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import id.payu.security.annotation.SensitivityLevel;

/**
 * Login request DTO with input validation.
 *
 * Validation is intentionally lenient for login: only checks presence and size.
 * The actual password complexity rules are enforced during registration (RegisterRequest),
 * not at login time. Keycloak performs the actual credential verification.
 *
 * Username validation:
 * - 3-80 characters, alphanumeric with dots, underscores, @, and hyphens (supports email)
 *
 * Password validation:
 * - Present and 1-128 characters (any format accepted — Keycloak validates)
 */
public record LoginRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 80, message = "Username must be between 3 and 80 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._@\\-]+$", message = "Username can only contain letters, numbers, dots, underscores, hyphens, and @")
    @Sensitive
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 128, message = "Password must not exceed 128 characters")
    @Sensitive(value = SensitivityLevel.CRITICAL)
    String password
) {
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=****]";
    }
}
