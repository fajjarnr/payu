package id.payu.transaction.config;

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
 * Security configuration tests for Transaction Service.
 *
 * Tests verify that:
 * - Public actuator endpoints (health/info) are accessible
 * - All other actuator endpoints require SCOPE_actuator authority
 * - All API endpoints require authentication
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

    // Basic instantiation test

    @Test
    @DisplayName("SecurityConfig should be instantiable")
    void securityConfigShouldBeInstantiable() {
        SecurityConfig securityConfig = new SecurityConfig();
        assertThat(securityConfig).isNotNull();
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
    @DisplayName("Should require SCOPE_actuator for actuator metrics endpoint")
    void shouldRequireScopeActuatorForActuatorMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require SCOPE_actuator for actuator prometheus endpoint")
    void shouldRequireScopeActuatorForActuatorPrometheus() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require SCOPE_actuator for actuator env endpoint")
    void shouldRequireScopeActuatorForActuatorEnv() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require SCOPE_actuator for actuator configprops endpoint")
    void shouldRequireScopeActuatorForActuatorConfigProps() throws Exception {
        mockMvc.perform(get("/actuator/configprops"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require SCOPE_actuator for actuator beans endpoint")
    void shouldRequireScopeActuatorForActuatorBeans() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require SCOPE_actuator for actuator mappings endpoint")
    void shouldRequireScopeActuatorForActuatorMappings() throws Exception {
        mockMvc.perform(get("/actuator/mappings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require SCOPE_actuator for actuator loggers endpoint")
    void shouldRequireScopeActuatorForActuatorLoggers() throws Exception {
        mockMvc.perform(get("/actuator/loggers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require SCOPE_actuator for actuator threaddump endpoint")
    void shouldRequireScopeActuatorForActuatorThreadDump() throws Exception {
        mockMvc.perform(get("/actuator/threaddump"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require SCOPE_actuator for actuator heapdump endpoint")
    void shouldRequireScopeActuatorForActuatorHeapDump() throws Exception {
        mockMvc.perform(get("/actuator/heapdump"))
                .andExpect(status().isUnauthorized());
    }

    // API endpoint tests

    @Test
    @DisplayName("Should require authentication for transaction transfer endpoint")
    void shouldRequireAuthenticationForTransferEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/transfer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for transaction detail endpoint")
    void shouldRequireAuthenticationForTransactionDetail() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for account transactions endpoint")
    void shouldRequireAuthenticationForAccountTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/accounts/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require authentication for QRIS payment endpoint")
    void shouldRequireAuthenticationForQrisPayment() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/qris/pay"))
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
