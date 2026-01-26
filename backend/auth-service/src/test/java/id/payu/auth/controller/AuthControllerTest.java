package id.payu.auth.controller;

import id.payu.auth.dto.LoginRequest;
import id.payu.auth.dto.LoginResponse;
import id.payu.auth.service.KeycloakService;
import id.payu.auth.service.MFATokenService;
import id.payu.auth.service.RiskEvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for AuthController
 * Tests REST API endpoints with mocked service layer
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakService keycloakService;

    @MockBean
    private RiskEvaluationService riskEvaluationService;

    @MockBean
    private MFATokenService mfaTokenService;

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginEndpoint {

        @Test
        @WithMockUser
        @DisplayName("should return 200 OK with tokens when login successful")
        void shouldReturnOkWithTokensWhenLoginSuccessful() throws Exception {
            // Given
            LoginRequest request = new LoginRequest("testuser", "SecurePass123!");
            LoginResponse response = new LoginResponse(
                    "access_token_value",
                    "refresh_token_value",
                    3600L,
                    "Bearer"
            );

            // Mock validateCredentialsBlocking to return true
            given(keycloakService.validateCredentialsBlocking(anyString(), anyString()))
                    .willReturn(true);

            // Mock risk evaluation to return low risk (no MFA required)
            given(riskEvaluationService.evaluateRisk(any()))
                    .willReturn(new RiskEvaluationService.RiskEvaluationResult(
                            10,  // low risk score
                            false,  // MFA not required
                            Collections.emptyList(),  // no risk factors
                            "Low risk"  // message
                    ));

            given(keycloakService.loginBlocking(anyString(), anyString()))
                    .willReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "username": "testuser",
                                        "password": "SecurePass123!"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").value("access_token_value"))
                    .andExpect(jsonPath("$.refresh_token").value("refresh_token_value"))
                    .andExpect(jsonPath("$.expires_in").value(3600))
                    .andExpect(jsonPath("$.token_type").value("Bearer"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 Bad Request when login fails")
        void shouldReturnBadRequestWhenLoginFails() throws Exception {
            // Given
            LoginRequest request = new LoginRequest("testuser", "wrongpassword");

            // Mock validateCredentialsBlocking to return false
            given(keycloakService.validateCredentialsBlocking(anyString(), anyString()))
                    .willReturn(false);

            // When/Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "username": "testuser",
                                        "password": "wrongpassword"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 Bad Request when account is locked")
        void shouldReturnBadRequestWhenAccountLocked() throws Exception {
            // Given
            LoginRequest request = new LoginRequest("lockeduser", "password");

            // Mock validateCredentialsBlocking to return false (simulating locked account)
            given(keycloakService.validateCredentialsBlocking(anyString(), anyString()))
                    .willReturn(false);

            // When/Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "username": "lockeduser",
                                        "password": "password"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }
}
