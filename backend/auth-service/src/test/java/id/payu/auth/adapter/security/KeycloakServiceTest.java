package id.payu.auth.adapter.security;

import id.payu.auth.application.service.RiskEvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.auth.config.KeycloakConfig;
import id.payu.auth.domain.model.LoginContext;
import id.payu.auth.dto.LoginResponse;
import id.payu.cache.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KeycloakService
 * Tests authentication, password validation, account lockout logic, and MFA integration
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

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private ClientResponse clientResponse;

    @Mock
    private RiskEvaluationService riskEvaluationService;

    @Mock
    private CacheService cacheService;

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
            when(cacheService.get("auth:failedAttempts:" + username, KeycloakService.FailedAttempt.class)).thenReturn(null);

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
            long futureLockTime = System.currentTimeMillis() + 15 * 60 * 1000; // 15 minutes from now
            KeycloakService.FailedAttempt lockedAttempt = new KeycloakService.FailedAttempt(5, futureLockTime);

            when(cacheService.get("auth:failedAttempts:" + username, KeycloakService.FailedAttempt.class)).thenReturn(lockedAttempt);

            // When
            boolean locked = invokeIsAccountLocked(username);

            // Then
            assertThat(locked).isTrue();
        }

        @Test
        @DisplayName("should not be locked when count is below max")
        void shouldNotBeLockedWhenCountBelowMax() throws Exception {
            // Given
            String username = "testuser";
            KeycloakService.FailedAttempt attempt = new KeycloakService.FailedAttempt(3, 0L);

            when(cacheService.get("auth:failedAttempts:" + username, KeycloakService.FailedAttempt.class)).thenReturn(attempt);

            // When
            boolean locked = invokeIsAccountLocked(username);

            // Then
            assertThat(locked).isFalse();
        }

        @Test
        @DisplayName("should not be locked when lock has expired")
        void shouldNotBeLockedWhenLockExpired() throws Exception {
            // Given
            String username = "testuser";
            long pastLockTime = System.currentTimeMillis() - 1000; // 1 second ago
            KeycloakService.FailedAttempt expiredAttempt = new KeycloakService.FailedAttempt(5, pastLockTime);

            when(cacheService.get("auth:failedAttempts:" + username, KeycloakService.FailedAttempt.class)).thenReturn(expiredAttempt);

            // When
            boolean locked = invokeIsAccountLocked(username);

            // Then
            assertThat(locked).isFalse();
        }

        @Test
        @DisplayName("should clear failed attempts on success")
        void shouldClearFailedAttemptsOnSuccess() throws Exception {
            // Given
            String username = "testuser";

            // When - clear attempts
            invokeClearFailedAttempts(username);

            // Then - verify cache invalidation was called
            verify(cacheService).invalidate("auth:failedAttempts:" + username);
            verify(riskEvaluationService).clearFailedAttempts(username);
        }

        @Test
        @DisplayName("should record failed attempt via cache")
        void shouldRecordFailedAttemptViaCache() throws Exception {
            // Given
            String username = "testuser";
            KeycloakService.FailedAttempt existingAttempt = new KeycloakService.FailedAttempt(2, 0L);

            when(cacheService.get(eq("auth:failedAttempts:" + username), eq(KeycloakService.FailedAttempt.class), any()))
                    .thenReturn(existingAttempt);

            // When
            invokeRecordFailedAttempt(username);

            // Then
            verify(cacheService).put(eq("auth:failedAttempts:" + username), any(KeycloakService.FailedAttempt.class), eq(Duration.ofMinutes(15)));
            verify(riskEvaluationService).recordFailedAttempt(username);
        }

        private boolean invokeIsAccountLocked(String username) throws Exception {
            java.lang.reflect.Method method = KeycloakService.class.getDeclaredMethod("isAccountLocked", String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(keycloakService, username);
        }

        private void invokeRecordFailedAttempt(String username) throws Exception {
            java.lang.reflect.Method method = KeycloakService.class.getDeclaredMethod("recordFailedAttemptInternal", String.class);
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

    @DisplayName("Credential Validation")
    class CredentialValidation {

        @Test
        @DisplayName("should return false for locked account")
        void shouldReturnFalseForLockedAccount() throws Exception {
            // Given
            String username = "testuser";
            String password = "password";
            long futureLockTime = System.currentTimeMillis() + 15 * 60 * 1000;
            KeycloakService.FailedAttempt lockedAttempt = new KeycloakService.FailedAttempt(5, futureLockTime);

            when(cacheService.get("auth:failedAttempts:" + username, KeycloakService.FailedAttempt.class)).thenReturn(lockedAttempt);

            // When
            Mono<Boolean> result = keycloakService.validateCredentials(username, password);

            // Then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }
    }
}
