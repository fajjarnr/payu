package id.payu.auth.controller;

import id.payu.auth.dto.LoginRequest;
import id.payu.auth.dto.LoginResponse;
import id.payu.auth.service.KeycloakService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

/**
 * Integration tests for AuthController
 * Tests REST API endpoints with mocked service layer
 */
@WebFluxTest(AuthController.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private KeycloakService keycloakService;

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginEndpoint {

        @Test
        @WithMockUser
        @DisplayName("should return 200 OK with tokens when login successful")
        void shouldReturnOkWithTokensWhenLoginSuccessful() {
            // Given
            LoginRequest request = new LoginRequest("testuser", "SecurePass123!");
            LoginResponse response = new LoginResponse(
                    "access_token_value",
                    "refresh_token_value",
                    3600L,
                    "Bearer"
            );
            
            given(keycloakService.login(anyString(), anyString()))
                    .willReturn(Mono.just(response));

            // When/Then
            webTestClient
                    .mutateWith(csrf())
                    .post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.access_token").isEqualTo("access_token_value")
                    .jsonPath("$.refresh_token").isEqualTo("refresh_token_value")
                    .jsonPath("$.expires_in").isEqualTo(3600)
                    .jsonPath("$.token_type").isEqualTo("Bearer");
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 Bad Request when login fails")
        void shouldReturnBadRequestWhenLoginFails() {
            // Given
            LoginRequest request = new LoginRequest("testuser", "wrongpassword");
            
            given(keycloakService.login(anyString(), anyString()))
                    .willReturn(Mono.error(new IllegalArgumentException("Invalid credentials")));

            // When/Then
            webTestClient
                    .mutateWith(csrf())
                    .post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 Bad Request when account is locked")
        void shouldReturnBadRequestWhenAccountLocked() {
            // Given
            LoginRequest request = new LoginRequest("lockeduser", "password");
            
            given(keycloakService.login(anyString(), anyString()))
                    .willReturn(Mono.error(new IllegalArgumentException("Account temporarily locked")));

            // When/Then
            webTestClient
                    .mutateWith(csrf())
                    .post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }
}
