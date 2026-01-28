package id.payu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Login request DTO with comprehensive input validation.
 *
 * Validation rules:
 * - Username: 3-50 characters, alphanumeric with dots and underscores
 * - Password: 8-128 characters, must include uppercase, lowercase, digit, and special char
 *
 * These validations help prevent:
 * - SQL injection via pattern restrictions
 * - Brute force attacks via size constraints
 * - Injection attacks via character whitelist
 */
public record LoginRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Username can only contain letters, numbers, dots, and underscores")
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    String password
) {}
