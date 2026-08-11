package id.payu.auth.adapter.security;

import id.payu.auth.application.service.RiskEvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
    private ObjectMapper objectMapper;

    @Mock
    private RiskEvaluationService riskEvaluationService;

    @InjectMocks
    private KeycloakService keycloakService;

    @BeforeEach
    void setUp() {
        // Set default values via reflection
        ReflectionTestUtils.setField(keycloakService, "passwordMinLength", 8);
        ReflectionTestUtils.setField(keycloakService, "requireUppercase", true);
        ReflectionTestUtils.setField(keycloakService, "requireLowercase", true);
        ReflectionTestUtils.setField(keycloakService, "requireDigit", true);
        ReflectionTestUtils.setField(keycloakService, "requireSpecialChar", true);
    }

    @Nested
    @DisplayName("revokeSession")
    class RevokeSession {

        private org.springframework.test.web.client.MockRestServiceServer mockServer;
        private org.springframework.web.client.RestTemplate mockRestTemplate;

        @BeforeEach
        void setUpRestTemplate() {
            mockRestTemplate = new org.springframework.web.client.RestTemplate();
            mockServer = org.springframework.test.web.client.MockRestServiceServer.bindTo(mockRestTemplate).build();
            ReflectionTestUtils.setField(keycloakService, "restTemplate", mockRestTemplate);
            ReflectionTestUtils.setField(keycloakService, "objectMapper", new ObjectMapper());
        }

        private void stubKeycloakConfig() {
            given(keycloakConfig.getServerUrl()).willReturn("http://keycloak:8080");
            given(keycloakConfig.getRealm()).willReturn("payu");
            given(keycloakConfig.getWebClientId()).willReturn("payu-web-app");
            given(keycloakConfig.getWebClientSecret()).willReturn("web-secret");
        }

        @Test
        @DisplayName("calls the Keycloak end_session endpoint with the web client and refresh_token")
        void revokesSessionAtKeycloak() {
            stubKeycloakConfig();
            mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                            "http://keycloak:8080/realms/payu/protocol/openid-connect/logout"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content()
                            .string(org.hamcrest.Matchers.allOf(
                                    org.hamcrest.Matchers.containsString("client_id=payu-web-app"),
                                    org.hamcrest.Matchers.containsString("client_secret=web-secret"),
                                    org.hamcrest.Matchers.containsString("refresh_token=rt-123"))))
                    .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                            .withSuccess("", org.springframework.http.MediaType.TEXT_PLAIN));

            keycloakService.revokeSession("rt-123");

            mockServer.verify();
        }

        @Test
        @DisplayName("rejects blank refresh tokens without calling Keycloak")
        void rejectsBlankToken() {
            assertThatThrownBy(() -> keycloakService.revokeSession("  "))
                    .isInstanceOf(IllegalArgumentException.class);
            mockServer.verify();
        }

        @Test
        @DisplayName("propagates Keycloak rejection as invalid token")
        void propagatesKeycloakRejection() {
            stubKeycloakConfig();
            mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                            "http://keycloak:8080/realms/payu/protocol/openid-connect/logout"))
                    .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                            .withStatus(org.springframework.http.HttpStatus.BAD_REQUEST));

            assertThatThrownBy(() -> keycloakService.revokeSession("rt-expired"))
                    .isInstanceOf(IllegalArgumentException.class);
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("exchangeAuthorizationCode")
    class ExchangeAuthorizationCode {

        private org.springframework.test.web.client.MockRestServiceServer mockServer;
        private org.springframework.web.client.RestTemplate mockRestTemplate;

        @BeforeEach
        void setUpRestTemplate() {
            mockRestTemplate = new org.springframework.web.client.RestTemplate();
            mockServer = org.springframework.test.web.client.MockRestServiceServer.bindTo(mockRestTemplate).build();
            ReflectionTestUtils.setField(keycloakService, "restTemplate", mockRestTemplate);
            ReflectionTestUtils.setField(keycloakService, "objectMapper", new ObjectMapper());
        }

        private void stubKeycloakConfig() {
            given(keycloakConfig.getServerUrl()).willReturn("http://keycloak:8080");
            given(keycloakConfig.getRealm()).willReturn("payu");
            given(keycloakConfig.getWebClientId()).willReturn("payu-web-app");
            given(keycloakConfig.getWebClientSecret()).willReturn("web-secret");
        }

        @Test
        @DisplayName("exchanges the authorization code with PKCE verifier using the web client")
        void exchangesCodeWithPkceVerifier() {
            stubKeycloakConfig();
            String tokenJson = """
                    {"access_token":"at-123","refresh_token":"rt-456","expires_in":900,"token_type":"Bearer"}""";
            mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                            "http://keycloak:8080/realms/payu/protocol/openid-connect/token"))
                    .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content()
                            .string(org.hamcrest.Matchers.allOf(
                                    org.hamcrest.Matchers.containsString("grant_type=authorization_code"),
                                    org.hamcrest.Matchers.containsString("client_id=payu-web-app"),
                                    org.hamcrest.Matchers.containsString("client_secret=web-secret"),
                                    org.hamcrest.Matchers.containsString("code=auth-code-1"),
                                    org.hamcrest.Matchers.containsString("code_verifier=verifier-123"),
                                    org.hamcrest.Matchers.containsString("redirect_uri=http%3A%2F%2Flocalhost%3A3001%2Fapi%2Fauth%2Fcallback"))))
                    .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                            .withSuccess(tokenJson, org.springframework.http.MediaType.APPLICATION_JSON));

            LoginResponse response = keycloakService.exchangeAuthorizationCode(
                    "auth-code-1", "verifier-123", "http://localhost:3001/api/auth/callback");

            assertThat(response.accessToken()).isEqualTo("at-123");
            assertThat(response.refreshToken()).isEqualTo("rt-456");
            assertThat(response.expiresIn()).isEqualTo(900L);
            mockServer.verify();
        }

        @Test
        @DisplayName("propagates Keycloak invalid_grant rejection")
        void propagatesInvalidGrantRejection() {
            stubKeycloakConfig();
            mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
                            "http://keycloak:8080/realms/payu/protocol/openid-connect/token"))
                    .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                            .withStatus(org.springframework.http.HttpStatus.BAD_REQUEST));

            assertThatThrownBy(() -> keycloakService.exchangeAuthorizationCode("expired", "verifier", "http://localhost:3001/api/auth/callback"))
                    .isInstanceOf(IllegalArgumentException.class);
            mockServer.verify();
        }
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

}
