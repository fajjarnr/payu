package id.payu.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security configuration tests for Auth Service.
 *
 * Tests verify that:
 * - Public endpoints are accessible without authentication
 * - Actuator endpoints are properly secured (only health/info are public)
 * - All other endpoints require authentication
 *
 * PCI-DSS Compliance:
 * - Requirement 1: Firewall configurations (actuator security)
 * - Requirement 7: Restrict access to cardholder data (endpoint security)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // Public endpoint tests

    @Test
    @DisplayName("Should allow public access to login endpoint")
    void shouldAllowPublicAccessToLoginEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isMethodNotAllowed()); // 405 because we need POST
    }

    @Test
    @DisplayName("Should allow public access to register endpoint")
    void shouldAllowPublicAccessToRegisterEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/register"))
                .andExpect(status().isMethodNotAllowed()); // 405 because we need POST
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
                .andExpect(status().isMethodNotAllowed()); // 405 because we need POST
    }

    // Public actuator endpoint tests

    @Test
    @DisplayName("Should allow public access to actuator health endpoint")
    void shouldAllowPublicAccessToActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to actuator health liveness")
    void shouldAllowPublicAccessToActuatorHealthLiveness() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to actuator health readiness")
    void shouldAllowPublicAccessToActuatorHealthReadiness() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to actuator info endpoint")
    void shouldAllowPublicAccessToActuatorInfo() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    // Secured actuator endpoint tests

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

    // Verify wildcard actuator access is denied

    @Test
    @DisplayName("Should deny access to non-existent actuator endpoint")
    void shouldDenyAccessToNonExistentActuatorEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/nonexistent"))
                .andExpect(status().isUnauthorized());
    }
}
