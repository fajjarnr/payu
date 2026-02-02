package id.payu.auth.controller;

import id.payu.auth.dto.SessionValidationResponse;
import id.payu.auth.service.KeycloakService;
import id.payu.auth.service.MFATokenService;
import id.payu.auth.service.RefreshTokenService;
import id.payu.auth.service.RiskEvaluationService;
import id.payu.auth.service.SessionValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AuthController session validation endpoint.
 * Tests the GET /api/v1/auth/validate endpoint with various scenarios.
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController - Session Validation")
class AuthControllerValidateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakService keycloakService;

    @MockBean
    private RiskEvaluationService riskEvaluationService;

    @MockBean
    private MFATokenService mfaTokenService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private SessionValidationService sessionValidationService;

    @Nested
    @DisplayName("GET /api/v1/auth/validate")
    class ValidateEndpoint {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should return 200 OK with valid session data")
        void shouldReturnOkWithValidSessionData() throws Exception {
            // Given
            SessionValidationResponse response = SessionValidationResponse.valid(
                    "user-123",
                    "testuser",
                    3600L,
                    Set.of("USER", "CUSTOMER")
            );

            given(sessionValidationService.validateSession(any()))
                    .willReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/auth/validate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.valid").value(true))
                    .andExpect(jsonPath("$.data.user_id").value("user-123"))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.expires_in").value(3600))
                    .andExpect(jsonPath("$.data.roles").isArray())
                    .andExpect(jsonPath("$.data.session_active").value(true));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should return 401 Unauthorized when session is invalid")
        void shouldReturnUnauthorizedWhenSessionInvalid() throws Exception {
            // Given
            SessionValidationResponse response = SessionValidationResponse.invalid();

            given(sessionValidationService.validateSession(any()))
                    .willReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/auth/validate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 401 Unauthorized when not authenticated")
        void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
            // When/Then - no @WithMockUser annotation
            mockMvc.perform(get("/api/v1/auth/validate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("should return session data with admin roles")
        void shouldReturnSessionDataWithAdminRoles() throws Exception {
            // Given
            SessionValidationResponse response = SessionValidationResponse.valid(
                    "admin-456",
                    "admin",
                    7200L,
                    Set.of("ADMIN", "STAFF")
            );

            given(sessionValidationService.validateSession(any()))
                    .willReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/auth/validate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.valid").value(true))
                    .andExpect(jsonPath("$.data.user_id").value("admin-456"))
                    .andExpect(jsonPath("$.data.username").value("admin"))
                    .andExpect(jsonPath("$.data.expires_in").value(7200))
                    .andExpect(jsonPath("$.data.roles").isArray());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should return 500 Internal Server Error when service throws exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsException() throws Exception {
            // Given
            given(sessionValidationService.validateSession(any()))
                    .willThrow(new RuntimeException("Unexpected error"));

            // When/Then
            mockMvc.perform(get("/api/v1/auth/validate")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());
        }
    }
}
