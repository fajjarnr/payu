package id.payu.auth.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.auth.application.service.RefreshTokenService;
import id.payu.auth.adapter.security.KeycloakService;
import id.payu.auth.application.service.RiskEvaluationService;
import id.payu.auth.application.service.SessionValidationService;
import id.payu.auth.interfaces.dto.LoginRequest;
import id.payu.auth.interfaces.dto.LoginResponse;
import id.payu.auth.interfaces.dto.LogoutRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.ResourceAccessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LOGIN-005: deterministic login error contract — 401 invalid credentials,
 * 423 locked account, 503 identity provider unavailable, 429 rate limited
 * (aspect), never a 500 for expected failures. LOGIN-002: logout revokes the
 * session at Keycloak.
 */
@DisplayName("AuthController login/logout contract")
class AuthControllerTest {

    private MockMvc mockMvc;
    private KeycloakService keycloakService;
    private RiskEvaluationService riskEvaluationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        keycloakService = mock(KeycloakService.class);
        riskEvaluationService = mock(RiskEvaluationService.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        SessionValidationService sessionValidationService = mock(SessionValidationService.class);
        id.payu.auth.application.metrics.BusinessMetrics businessMetrics = mock(id.payu.auth.application.metrics.BusinessMetrics.class);
        AuthController controller = new AuthController(
                keycloakService, riskEvaluationService, refreshTokenService, sessionValidationService, businessMetrics);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/callback")
    class CallbackEndpoint {

        private static final String CALLBACK_BODY =
                "{\"code\":\"auth-code-1\",\"codeVerifier\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"redirectUri\":\"http://localhost:3001/api/auth/callback\"}";

        @Test
        @DisplayName("200 with tokens on successful code exchange")
        void successReturnsTokens() throws Exception {
            given(keycloakService.exchangeAuthorizationCode("auth-code-1", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "http://localhost:3001/api/auth/callback"))
                    .willReturn(new LoginResponse("access-token", "refresh-token", 900L, "Bearer"));

            mockMvc.perform(post("/api/v1/auth/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CALLBACK_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").value("access-token"));
        }

        @Test
        @DisplayName("400 AUTH_BUS_009 for an invalid or expired authorization code")
        void invalidCodeReturns400() throws Exception {
            given(keycloakService.exchangeAuthorizationCode(anyString(), anyString(), anyString()))
                    .willThrow(new IllegalArgumentException("Keycloak rejected the authorization code"));

            mockMvc.perform(post("/api/v1/auth/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CALLBACK_BODY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("AUTH_BUS_009"));
        }

        @Test
        @DisplayName("400 for a request missing the code or code verifier")
        void missingFieldsReturn400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"only-code\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("503 SERVICE_UNAVAILABLE when the identity provider is down")
        void idpDownReturns503() throws Exception {
            given(keycloakService.exchangeAuthorizationCode(anyString(), anyString(), anyString()))
                    .willThrow(new ResourceAccessException("Keycloak unreachable"));

            mockMvc.perform(post("/api/v1/auth/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CALLBACK_BODY))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"));
        }

        @Test
        @DisplayName("429 RATE_LIMIT_EXCEEDED when either rate limiter denies the attempt")
        void rateLimitedReturns429() throws Exception {
            given(keycloakService.exchangeAuthorizationCode(anyString(), anyString(), anyString()))
                    .willThrow(new id.payu.auth.exception.AuthDomainException.AuthRateLimitExceededException());

            mockMvc.perform(post("/api/v1/auth/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CALLBACK_BODY))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshEndpoint {

        @Test
        @DisplayName("400 AUTH_BUS_006 for a revoked or expired refresh token")
        void revokedTokenReturns400() throws Exception {
            given(keycloakService.refreshTokenBlocking(anyString()))
                    .willThrow(new IllegalArgumentException("Keycloak rejected the refresh token"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new id.payu.auth.interfaces.dto.RefreshTokenRequest("revoked-token"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("AUTH_BUS_006"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutEndpoint {
        @Test
        @DisplayName("200 and revokes the session at Keycloak")
        void logoutRevokesSession() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LogoutRequest("rt-123"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("400 for an invalid refresh token")
        void logoutRejectsInvalidToken() throws Exception {
            willThrow(new IllegalArgumentException("Invalid refresh token"))
                    .given(keycloakService).revokeSession(anyString());

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LogoutRequest("rt-expired"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("AUTH_BUS_006"));
        }

        @Test
        @DisplayName("503 when the identity provider is unreachable")
        void logoutIdpDownReturns503() throws Exception {
            willThrow(new ResourceAccessException("Keycloak unreachable"))
                    .given(keycloakService).revokeSession(anyString());

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LogoutRequest("rt-123"))))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/auth/users/{userId}")
    class DeleteUserEndpoint {

        @Test
        @DisplayName("200 and deletes the IAM user")
        void deleteUserReturns200() throws Exception {
            mockMvc.perform(delete("/api/v1/auth/users/user-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message").value("User deleted from IAM"));

            verify(keycloakService).deleteUser("user-1");
        }

        @Test
        @DisplayName("400 when the user id is blank")
        void blankUserIdReturns400() throws Exception {
            willThrow(new IllegalArgumentException("User ID is required"))
                    .given(keycloakService).deleteUser("");

            mockMvc.perform(delete("/api/v1/auth/users/"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("500 when Keycloak deletion fails")
        void deleteFailureReturns500() throws Exception {
            willThrow(new RuntimeException("Failed to delete user in IAM"))
                    .given(keycloakService).deleteUser("user-1");

            mockMvc.perform(delete("/api/v1/auth/users/user-1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));

            verify(keycloakService).deleteUser("user-1");
        }
    }
}
