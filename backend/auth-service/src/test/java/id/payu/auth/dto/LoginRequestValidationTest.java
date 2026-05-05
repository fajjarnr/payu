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
        LoginRequest request = new LoginRequest("john.doe", "SecureP@ss123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should validate login request with underscore and dots in username")
    void shouldValidateUsernameWithUnderscoreAndDots() {
        LoginRequest request = new LoginRequest("john.doe_2024", "SecureP@ss123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should reject null or empty username")
    void shouldRejectNullOrEmptyUsername(String username) {
        LoginRequest request = new LoginRequest(username, "SecureP@ss123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty().anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("Should reject username shorter than 3 characters")
    void shouldRejectShortUsername() {
        LoginRequest request = new LoginRequest("ab", "SecureP@ss123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty().anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("Should reject username longer than 80 characters")
    void shouldRejectLongUsername() {
        String longUsername = "a".repeat(81);
        LoginRequest request = new LoginRequest(longUsername, "SecureP@ss123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty().anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"user name", "user(name", "user;name", "user'name", "user\"name"})
    @DisplayName("Should reject username with invalid characters")
    void shouldRejectUsernameWithInvalidCharacters(String username) {
        LoginRequest request = new LoginRequest(username, "SecureP@ss123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty().anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("Should reject username with SQL injection pattern")
    void shouldRejectSQLInjectionPattern() {
        LoginRequest request = new LoginRequest("admin' OR '1'='1", "SecureP@ss123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty().anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should reject null or empty password")
    void shouldRejectNullOrEmptyPassword(String password) {
        LoginRequest request = new LoginRequest("john.doe", password);
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty().anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Should reject password longer than 128 characters")
    void shouldRejectLongPassword() {
        String longPassword = "a".repeat(129);
        LoginRequest request = new LoginRequest("john.doe", longPassword);
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty().anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Should return multiple violations for invalid username and password")
    void shouldReturnMultipleViolations() {
        LoginRequest request = new LoginRequest("ab", ""); // small username and empty password
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
    }
}
