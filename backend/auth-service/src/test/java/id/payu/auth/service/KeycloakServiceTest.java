package id.payu.auth.service;

import id.payu.auth.config.KeycloakConfig;
import id.payu.auth.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KeycloakService
 * Tests authentication, password validation, and account lockout logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KeycloakService")
class KeycloakServiceTest {

    @Mock
    private Keycloak keycloakAdmin;

    @Mock
    private KeycloakConfig keycloakConfig;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private KeycloakService keycloakService;

    @BeforeEach
    void setUp() {
        // Set default values via reflection
        ReflectionTestUtils.setField(keycloakService, "maxLoginAttempts", 5);
        ReflectionTestUtils.setField(keycloakService, "lockoutDurationMinutes", 15);
        ReflectionTestUtils.setField(keycloakService, "passwordMinLength", 8);
        ReflectionTestUtils.setField(keycloakService, "requireUppercase", true);
        ReflectionTestUtils.setField(keycloakService, "requireLowercase", true);
        ReflectionTestUtils.setField(keycloakService, "requireDigit", true);
        ReflectionTestUtils.setField(keycloakService, "requireSpecialChar", true);
    }

    @Nested
    @DisplayName("validatePassword")
    class ValidatePassword {

        @Test
        @DisplayName("should accept valid password with all requirements")
        void shouldAcceptValidPassword() {
            // Given
            String validPassword = "SecurePass123!";

            // When/Then - no exception thrown
            // We need to call createUser as validatePassword is private
            // Instead, test through reflection
            java.lang.reflect.Method method;
            try {
                method = KeycloakService.class.getDeclaredMethod("validatePassword", String.class);
                method.setAccessible(true);
                method.invoke(keycloakService, validPassword);
                // If no exception, test passes
            } catch (Exception e) {
                if (e.getCause() instanceof IllegalArgumentException) {
                    throw new AssertionError("Valid password should not throw exception", e);
                }
            }
        }

        @Test
        @DisplayName("should reject password shorter than minimum length")
        void shouldRejectShortPassword() {
            String shortPassword = "Ab1!";
            
            assertThatThrownBy(() -> invokeValidatePassword(shortPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 8 characters");
        }

        @Test
        @DisplayName("should reject password without uppercase")
        void shouldRejectPasswordWithoutUppercase() {
            String noUppercase = "password123!";
            
            assertThatThrownBy(() -> invokeValidatePassword(noUppercase))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("uppercase");
        }

        @Test
        @DisplayName("should reject password without lowercase")
        void shouldRejectPasswordWithoutLowercase() {
            String noLowercase = "PASSWORD123!";
            
            assertThatThrownBy(() -> invokeValidatePassword(noLowercase))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lowercase");
        }

        @Test
        @DisplayName("should reject password without digit")
        void shouldRejectPasswordWithoutDigit() {
            String noDigit = "Password!!!!";
            
            assertThatThrownBy(() -> invokeValidatePassword(noDigit))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("digit");
        }

        @Test
        @DisplayName("should reject password without special character")
        void shouldRejectPasswordWithoutSpecialChar() {
            String noSpecial = "Password123";
            
            assertThatThrownBy(() -> invokeValidatePassword(noSpecial))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("special character");
        }

        private void invokeValidatePassword(String password) throws Exception {
            java.lang.reflect.Method method = KeycloakService.class.getDeclaredMethod("validatePassword", String.class);
            method.setAccessible(true);
            try {
                method.invoke(keycloakService, password);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw (Exception) e.getCause();
            }
        }
    }

    @Nested
    @DisplayName("Account Lockout")
    class AccountLockout {

        @Test
        @DisplayName("should not be locked initially")
        void shouldNotBeLockedInitially() throws Exception {
            // Given
            String username = "testuser";
            
            // When
            boolean locked = invokeIsAccountLocked(username);
            
            // Then
            assertThat(locked).isFalse();
        }

        @Test
        @DisplayName("should lock account after max failed attempts")
        void shouldLockAccountAfterMaxFailedAttempts() throws Exception {
            // Given
            String username = "testuser";
            
            // Simulate 5 failed attempts
            for (int i = 0; i < 5; i++) {
                invokeRecordFailedAttempt(username);
            }
            
            // When
            boolean locked = invokeIsAccountLocked(username);
            
            // Then
            assertThat(locked).isTrue();
        }

        @Test
        @DisplayName("should clear failed attempts on success")
        void shouldClearFailedAttemptsOnSuccess() throws Exception {
            // Given
            String username = "testuser";
            
            // Record some failed attempts
            for (int i = 0; i < 3; i++) {
                invokeRecordFailedAttempt(username);
            }
            
            // When - clear attempts
            invokeClearFailedAttempts(username);
            
            // Record 3 more (should not reach 5 total)
            for (int i = 0; i < 3; i++) {
                invokeRecordFailedAttempt(username);
            }
            
            // Then - should not be locked
            boolean locked = invokeIsAccountLocked(username);
            assertThat(locked).isFalse();
        }

        private boolean invokeIsAccountLocked(String username) throws Exception {
            java.lang.reflect.Method method = KeycloakService.class.getDeclaredMethod("isAccountLocked", String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(keycloakService, username);
        }

        private void invokeRecordFailedAttempt(String username) throws Exception {
            java.lang.reflect.Method method = KeycloakService.class.getDeclaredMethod("recordFailedAttempt", String.class);
            method.setAccessible(true);
            method.invoke(keycloakService, username);
        }

        private void invokeClearFailedAttempts(String username) throws Exception {
            java.lang.reflect.Method method = KeycloakService.class.getDeclaredMethod("clearFailedAttempts", String.class);
            method.setAccessible(true);
            method.invoke(keycloakService, username);
        }
    }

    @Nested
    @DisplayName("Rate Limit Fallback")
    class RateLimitFallback {

        @Test
        @DisplayName("should return error mono when rate limited")
        void shouldReturnErrorWhenRateLimited() {
            // When
            Mono<LoginResponse> result = keycloakService.rateLimitFallback("user", "pass", new RuntimeException("Rate limited"));

            // Then
            StepVerifier.create(result)
                    .expectErrorMatches(error -> 
                        error instanceof IllegalArgumentException &&
                        error.getMessage().contains("Too many login attempts"))
                    .verify();
        }
    }
}
