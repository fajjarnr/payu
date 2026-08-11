package id.payu.auth.config;

import id.payu.auth.adapter.persistence.RefreshTokenService;
import id.payu.auth.adapter.security.KeycloakService;
import id.payu.auth.adapter.web.AuthController;
import id.payu.auth.application.service.RiskEvaluationService;
import id.payu.auth.application.service.SessionValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security configuration tests for Auth Service.
 *
 * Uses a minimal Spring context to test only security filter chains
     * without requiring external infrastructure (DB, Data Grid, Keycloak).
 *
 * PCI-DSS Compliance:
 * - Requirement 1: Firewall configurations (actuator security)
 * - Requirement 7: Restrict access to cardholder data (endpoint security)
 */
@SpringBootTest(
        classes = SecurityConfigTest.TestConfig.class,
        properties = {
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/payu",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/realms/payu/protocol/openid-connect/certs",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    /**
     * Minimal test configuration that only imports SecurityConfig and the controller.
     * Also imports WebMvc and Security auto-configurations for proper filter chain setup.
     */
    @Configuration
    @Import({SecurityConfig.class, AuthController.class})
    @ImportAutoConfiguration({
            WebMvcAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class
    })
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    // Mock all service dependencies of AuthController
    @MockitoBean
    private KeycloakService keycloakService;

    @MockitoBean
    private RiskEvaluationService riskEvaluationService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private SessionValidationService sessionValidationService;

    // Public endpoint tests

    @Test
    @DisplayName("Should allow public access to login endpoint")
    void shouldAllowPublicAccessToCallbackEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/callback"))
                .andExpect(status().isMethodNotAllowed()); // 405 because we need POST
    }

    @Test
    @DisplayName("Should allow public access to register endpoint")
    void shouldAllowPublicAccessToRegisterEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/register"))
                .andExpect(status().isMethodNotAllowed()); // 405 because endpoint requires POST
    }

    @Test
    @DisplayName("Should allow public access to refresh endpoint")
    void shouldAllowPublicAccessToRefreshEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/refresh"))
                .andExpect(status().isMethodNotAllowed()); // 405 because we need POST
    }

    @Test
    @DisplayName("Should allow public access to forgot-password endpoint")
    void shouldAllowPublicAccessToForgotPasswordEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/forgot-password"))
                .andExpect(status().isNotFound()); // 404 because endpoint is not yet implemented
    }

    // Secured endpoint tests — without JWT token, all non-public endpoints should return 401

    @Test
    @DisplayName("Should require authentication for actuator metrics endpoint")
    void shouldRequireAuthenticationForActuatorMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for actuator prometheus endpoint")
    void shouldRequireAuthenticationForActuatorPrometheus() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for actuator env endpoint")
    void shouldRequireAuthenticationForActuatorEnv() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for actuator configprops endpoint")
    void shouldRequireAuthenticationForActuatorConfigProps() throws Exception {
        mockMvc.perform(get("/actuator/configprops"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for actuator beans endpoint")
    void shouldRequireAuthenticationForActuatorBeans() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for actuator mappings endpoint")
    void shouldRequireAuthenticationForActuatorMappings() throws Exception {
        mockMvc.perform(get("/actuator/mappings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for actuator loggers endpoint")
    void shouldRequireAuthenticationForActuatorLoggers() throws Exception {
        mockMvc.perform(get("/actuator/loggers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for actuator threaddump endpoint")
    void shouldRequireAuthenticationForActuatorThreadDump() throws Exception {
        mockMvc.perform(get("/actuator/threaddump"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for actuator heapdump endpoint")
    void shouldRequireAuthenticationForActuatorHeapDump() throws Exception {
        mockMvc.perform(get("/actuator/heapdump"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access to non-existent actuator endpoint")
    void shouldDenyAccessToNonExistentActuatorEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/nonexistent"))
                .andExpect(status().isUnauthorized());
    }
}
