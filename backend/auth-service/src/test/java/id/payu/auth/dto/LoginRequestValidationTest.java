package id.payu.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation tests for {@link LoginRequest} DTO.
 *
 * Tests verify that the enhanced validation constraints prevent:
 * - SQL injection via username pattern restrictions
 * - Brute force attacks via size constraints
 * - Weak passwords via complexity requirements
 *
 * PCI-DSS Compliance:
 * - Requirement 8.2.3: Secure authentication (password complexity)
 * - Requirement 6.5.1: Injection flaws (input validation)
 */
class LoginRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should validate valid login request")
    void shouldValidateValidLoginRequest() {
        // Given
        LoginRequest request = new LoginRequest(
                "john.doe",
                "SecureP@ss123"
        );

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should validate login request with underscore and dots in username")
    void shouldValidateUsernameWithUnderscoreAndDots() {
        // Given
        LoginRequest request = new LoginRequest(
                "john.doe_2024",
                "SecureP@ss123"
        );

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    // Username validation tests

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should reject null or empty username")
    void shouldRejectNullOrEmptyUsername(String username) {
        // Given
        LoginRequest request = new LoginRequest(
                username,
                "SecureP@ss123"
        );

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("Should reject username shorter than 3 characters")
    void shouldRejectShortUsername() {
        // Given
        LoginRequest request = new LoginRequest("ab", "SecureP@ss123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("between 3 and 50"));
    }

    @Test
    @DisplayName("Should reject username longer than 50 characters")
    void shouldRejectLongUsername() {
        // Given
        String longUsername = "a".repeat(51);
        LoginRequest request = new LoginRequest(longUsername, "SecureP@ss123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("between 3 and 50"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"user name", "user@name", "user-name", "user(name", "user;name", "user'name", "user\"name"})
    @DisplayName("Should reject username with invalid characters")
    void shouldRejectUsernameWithInvalidCharacters(String username) {
        // Given
        LoginRequest request = new LoginRequest(username, "SecureP@ss123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("contain only letters, numbers, dots, and underscores"));
    }

    @Test
    @DisplayName("Should reject username with SQL injection pattern")
    void shouldRejectSQLInjectionPattern() {
        // Given
        LoginRequest request = new LoginRequest(
                "admin' OR '1'='1",
                "SecureP@ss123"
        );

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    // Password validation tests

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should reject null or empty password")
    void shouldRejectNullOrEmptyPassword(String password) {
        // Given
        LoginRequest request = new LoginRequest("john.doe", password);

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Should reject password shorter than 8 characters")
    void shouldRejectShortPassword() {
        // Given
        LoginRequest request = new LoginRequest("john.doe", "Pass1@");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("between 8 and 128"));
    }

    @Test
    @DisplayName("Should reject password longer than 128 characters")
    void shouldRejectLongPassword() {
        // Given
        String longPassword = "Password1@" + "a".repeat(128); // pragma: allowlist secret
        LoginRequest request = new LoginRequest("john.doe", longPassword);

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("between 8 and 128"));
    }

    @Test
    @DisplayName("Should reject password without uppercase letter")
    void shouldRejectPasswordWithoutUppercase() {
        // Given
        LoginRequest request = new LoginRequest("john.doe", "password1@");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("uppercase"));
    }

    @Test
    @DisplayName("Should reject password without lowercase letter")
    void shouldRejectPasswordWithoutLowercase() {
        // Given
        LoginRequest request = new LoginRequest("john.doe", "PASSWORD1@");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("lowercase"));
    }

    @Test
    @DisplayName("Should reject password without digit")
    void shouldRejectPasswordWithoutDigit() {
        // Given
        LoginRequest request = new LoginRequest("john.doe", "Password@");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("digit"));
    }

    @Test
    @DisplayName("Should reject password without special character")
    void shouldRejectPasswordWithoutSpecialCharacter() {
        // Given
        LoginRequest request = new LoginRequest("john.doe", "Password123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("special character"));
    }

    @Test
    @DisplayName("Should reject password with spaces")
    void shouldRejectPasswordWithSpaces() {
        // Given
        LoginRequest request = new LoginRequest("john.doe", "Pass word1@");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Should accept password with all required complexity")
    void shouldAcceptPasswordWithAllComplexity() {
        // Given
        LoginRequest request = new LoginRequest("john.doe", "SecureP@ss123");

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should accept password with different special characters")
    void shouldAcceptPasswordWithDifferentSpecialCharacters() {
        // Given - test all allowed special characters
        String[] testCases = {
                "Password1!",
                "Password1$",
                "Password1*",
                "Password1?",
                "Password1&"
        };

        for (String password : testCases) {
            LoginRequest request = new LoginRequest("john.doe", password);

            // When
            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations)
                    .as("Password '%s' should be valid", password)
                    .isEmpty();
        }
    }

    // Combined validation tests

    @Test
    @DisplayName("Should return multiple violations for invalid username and password")
    void shouldReturnMultipleViolations() {
        // Given
        LoginRequest request = new LoginRequest(
                "ab", // Too short
                "pass" // Too short and missing complexity
        );

        // When
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .hasSizeGreaterThanOrEqualTo(2)
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"))
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }
}
