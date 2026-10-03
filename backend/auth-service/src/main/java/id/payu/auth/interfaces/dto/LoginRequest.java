package id.payu.auth.interfaces.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import id.payu.security.annotation.SensitivityLevel;

/**
 * Login request DTO. Validation is intentionally lenient (presence and size only):
 * password complexity is enforced at registration; Keycloak verifies credentials.
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
