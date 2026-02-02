package id.payu.auth.controller;

import id.payu.auth.dto.RefreshTokenRequest;
import id.payu.auth.dto.RefreshTokenResponse;
import id.payu.auth.dto.LoginResponse;
import id.payu.auth.service.KeycloakService;
import id.payu.auth.service.MFATokenService;
import id.payu.auth.service.RefreshTokenService;
import id.payu.auth.service.RiskEvaluationService;
import id.payu.auth.service.SessionValidationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for AuthController refresh token endpoint.
 * Tests POST /api/v1/auth/refresh with various scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - Refresh Token")
class AuthControllerRefreshTest {

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RiskEvaluationService riskEvaluationService;

    @Mock
    private MFATokenService mfaTokenService;

    @Mock
    private SessionValidationService sessionValidationService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        given(httpRequest.getHeader("X-Forwarded-For")).willReturn(null);
        given(httpRequest.getHeader("Proxy-Client-IP")).willReturn(null);
        given(httpRequest.getHeader("WL-Proxy-Client-IP")).willReturn(null);
        given(httpRequest.getRemoteAddr()).willReturn("127.0.0.1");
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenEndpoint {

        private static final String VALID_REFRESH_TOKEN = "v1.valid-token-id.abc123def456";
        private static final String NEW_REFRESH_TOKEN = "v1.new-token-id.xyz789";

        @Test
        @DisplayName("should return 200 OK with new tokens when refresh successful")
        void shouldReturnOkWithNewTokensWhenRefreshSuccessful() {
            // Given
            Instant expiresAt = Instant.now().plusSeconds(604800); // 7 days

            RefreshTokenService.RefreshTokenResponse refreshServiceResponse =
                    new RefreshTokenService.RefreshTokenResponse(NEW_REFRESH_TOKEN, expiresAt);

            LoginResponse keycloakResponse = new LoginResponse(
                    "new_access_token_value",
                    "keycloak_refresh_token",
                    3600L,
                    "Bearer"
            );

            given(refreshTokenService.rotateRefreshToken(anyString()))
                    .willReturn(refreshServiceResponse);

            given(keycloakService.refreshTokenBlocking(anyString()))
                    .willReturn(keycloakResponse);

            // When
            ResponseEntity result = authController.refreshToken(
                    new RefreshTokenRequest(VALID_REFRESH_TOKEN),
                    httpRequest
            );

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            // The response body should contain ApiResponse with data
            assertThat(result.getBody()).isNotNull();
        }

        @Test
        @DisplayName("should return 400 Bad Request when refresh token is invalid")
        void shouldReturnBadRequestWhenRefreshTokenInvalid() {
            // Given
            given(refreshTokenService.rotateRefreshToken(anyString()))
                    .willThrow(new org.springframework.security.authentication.BadCredentialsException(
                            "Invalid refresh token"));

            // When
            ResponseEntity result = authController.refreshToken(
                    new RefreshTokenRequest("invalid_token"),
                    httpRequest
            );

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should return 400 Bad Request when refresh token is expired")
        void shouldReturnBadRequestWhenRefreshTokenExpired() {
            // Given
            given(refreshTokenService.rotateRefreshToken(anyString()))
                    .willThrow(new org.springframework.security.authentication.BadCredentialsException(
                            "Refresh token has expired"));

            // When
            ResponseEntity result = authController.refreshToken(
                    new RefreshTokenRequest("expired_token"),
                    httpRequest
            );

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should return 500 Internal Server Error when Keycloak fails")
        void shouldReturnInternalServerErrorWhenKeycloakFails() {
            // Given
            Instant expiresAt = Instant.now().plusSeconds(604800);
            RefreshTokenService.RefreshTokenResponse refreshServiceResponse =
                    new RefreshTokenService.RefreshTokenResponse(NEW_REFRESH_TOKEN, expiresAt);

            given(refreshTokenService.rotateRefreshToken(anyString()))
                    .willReturn(refreshServiceResponse);

            given(keycloakService.refreshTokenBlocking(anyString()))
                    .willThrow(new RuntimeException("Keycloak unavailable"));

            // When
            ResponseEntity result = authController.refreshToken(
                    new RefreshTokenRequest(VALID_REFRESH_TOKEN),
                    httpRequest
            );

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
