package id.payu.auth.application.service;

import id.payu.auth.interfaces.dto.SessionValidationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for SessionValidationService.
 * Tests session validation logic with various JWT token scenarios.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SessionValidationService")
class SessionValidationServiceTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private RiskEvaluationService riskEvaluationService;

    private SessionValidationService sessionValidationService;

    @BeforeEach
    void setUp() {
        sessionValidationService = new SessionValidationService(jwtDecoder, riskEvaluationService);

        // Configure default stubbing for common scenarios
        given(riskEvaluationService.isAccountActive(anyString())).willReturn(true);
    }

    @Nested
    @DisplayName("validateSession(Authentication)")
    class ValidateSession {

        @Test
        @DisplayName("should return invalid when authentication is null")
        void shouldReturnInvalidWhenAuthenticationIsNull() {
            // When
            SessionValidationResponse response = sessionValidationService.validateSession(null);

            // Then
            assertThat(response.valid()).isFalse();
            assertThat(response.sessionActive()).isFalse();
        }

        @Test
        @DisplayName("should return invalid when authentication is not authenticated")
        void shouldReturnInvalidWhenNotAuthenticated() {
            // Given
            Authentication authentication = new TestingAuthenticationToken(null, null);
            authentication.setAuthenticated(false);

            // When
            SessionValidationResponse response = sessionValidationService.validateSession(authentication);

            // Then
            assertThat(response.valid()).isFalse();
            assertThat(response.sessionActive()).isFalse();
        }

        @Test
        @DisplayName("should return valid with user data when JWT is valid")
        void shouldReturnValidWhenJwtIsValid() {
            // Given
            Jwt jwt = createTestJwt("user-123", "testuser",
                    Instant.now().plusSeconds(3600), List.of("USER", "CUSTOMER"));

            Authentication authentication = new TestingAuthenticationToken(jwt, null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            // When
            SessionValidationResponse response = sessionValidationService.validateSession(authentication);

            // Then
            assertThat(response.valid()).isTrue();
            assertThat(response.sessionActive()).isTrue();
            assertThat(response.userId()).isEqualTo("user-123");
            assertThat(response.username()).isEqualTo("testuser");
            assertThat(response.expiresIn()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should return invalid when JWT is expired")
        void shouldReturnInvalidWhenJwtIsExpired() {
            // Given
            Instant issuedAt = Instant.now().minusSeconds(7200); // Issued 2 hours ago
            Instant expiresAt = Instant.now().minusSeconds(3600); // Expired 1 hour ago
            Jwt jwt = createTestJwtWithIssuedAt("user-123", "testuser", issuedAt, expiresAt, List.of("USER"));

            Authentication authentication = new TestingAuthenticationToken(jwt, null);

            // When
            SessionValidationResponse response = sessionValidationService.validateSession(authentication);

            // Then
            assertThat(response.valid()).isFalse();
            assertThat(response.sessionActive()).isFalse();
        }

        @Test
        @DisplayName("should return invalid when account is not active")
        void shouldReturnInvalidWhenAccountNotActive() {
            // Given
            Jwt jwt = createTestJwt("user-123", "testuser",
                    Instant.now().plusSeconds(3600), List.of("USER"));

            Authentication authentication = new TestingAuthenticationToken(jwt, null);

            given(riskEvaluationService.isAccountActive("user-123")).willReturn(false);

            // When
            SessionValidationResponse response = sessionValidationService.validateSession(authentication);

            // Then
            assertThat(response.valid()).isFalse();
            assertThat(response.sessionActive()).isFalse();
        }

        @Test
        @DisplayName("should return invalid when authentication principal is not JWT")
        void shouldReturnInvalidWhenPrincipalIsNotJwt() {
            // Given
            Authentication authentication = new TestingAuthenticationToken("not-a-jwt", null);
            authentication.setAuthenticated(true);

            // When
            SessionValidationResponse response = sessionValidationService.validateSession(authentication);

            // Then
            assertThat(response.valid()).isFalse();
            assertThat(response.sessionActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("validateToken(String)")
    class ValidateToken {

        @Test
        @DisplayName("should return valid when token string is valid")
        void shouldReturnValidWhenTokenStringIsValid() {
            // Given
            String tokenString = "valid-jwt-token";
            Jwt jwt = createTestJwt("user-456", "anotheruser",
                    Instant.now().plusSeconds(7200), List.of("ADMIN"));

            given(jwtDecoder.decode(tokenString)).willReturn(jwt);

            // When
            SessionValidationResponse response = sessionValidationService.validateToken(tokenString);

            // Then
            assertThat(response.valid()).isTrue();
            assertThat(response.sessionActive()).isTrue();
            assertThat(response.userId()).isEqualTo("user-456");
            assertThat(response.username()).isEqualTo("anotheruser");
        }

        @Test
        @DisplayName("should return invalid when token string is invalid")
        void shouldReturnInvalidWhenTokenStringIsInvalid() {
            // Given
            String tokenString = "invalid-jwt-token";
            given(jwtDecoder.decode(tokenString)).willThrow(new JwtException("Invalid token"));

            // When
            SessionValidationResponse response = sessionValidationService.validateToken(tokenString);

            // Then
            assertThat(response.valid()).isFalse();
            assertThat(response.sessionActive()).isFalse();
        }

        @Test
        @DisplayName("should return invalid when token is expired")
        void shouldReturnInvalidWhenTokenIsExpired() {
            // Given
            String tokenString = "expired-jwt-token";
            Instant issuedAt = Instant.now().minusSeconds(7200);
            Instant expiresAt = Instant.now().minusSeconds(100);
            Jwt jwt = createTestJwtWithIssuedAt("user-789", "expireduser", issuedAt, expiresAt, List.of("USER"));

            given(jwtDecoder.decode(tokenString)).willReturn(jwt);

            // When
            SessionValidationResponse response = sessionValidationService.validateToken(tokenString);

            // Then
            assertThat(response.valid()).isFalse();
            assertThat(response.sessionActive()).isFalse();
        }
    }

    /**
     * Helper method to create a test JWT token with issuedAt = now.
     */
    private Jwt createTestJwt(String subject, String username, Instant expiresAt, List<String> roles) {
        return createTestJwtWithIssuedAt(subject, username, Instant.now(), expiresAt, roles);
    }

    /**
     * Helper method to create a test JWT token with custom issuedAt.
     */
    private Jwt createTestJwtWithIssuedAt(String subject, String username, Instant issuedAt, Instant expiresAt, List<String> roles) {
        return new Jwt(
                "test-token",
                issuedAt,
                expiresAt,
                Map.of("hdr", "value"),
                Map.of(
                        "sub", subject,
                        "preferred_username", username,
                        "realm_access", Map.of("roles", roles),
                        "exp", expiresAt.getEpochSecond(),
                        "iat", issuedAt.getEpochSecond()
                )
        );
    }
}
