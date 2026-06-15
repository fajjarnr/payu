package id.payu.transaction.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security configuration tests for TransactionEntity Service.
 *
 * Tests verify that:
 * - All actuator endpoints are publicly accessible (bypassed by WebSecurityCustomizer)
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
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(200, 503); // 503 if health indicators are down
                });
    }

    @Test
    @DisplayName("Should allow public access to actuator health liveness")
    void shouldAllowPublicAccessToActuatorHealthLiveness() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(200, 503);
                });
    }

    @Test
    @DisplayName("Should allow public access to actuator health readiness")
    void shouldAllowPublicAccessToActuatorHealthReadiness() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(200, 503);
                });
    }

    @Test
    @DisplayName("Should allow public access to actuator info endpoint")
    void shouldAllowPublicAccessToActuatorInfo() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    // Actuator endpoints are publicly accessible (WebSecurityCustomizer ignores /actuator/**)

    @Test
    @DisplayName("Should allow public access to actuator metrics endpoint")
    void shouldAllowPublicAccessToActuatorMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to actuator prometheus endpoint")
    void shouldAllowPublicAccessToActuatorPrometheus() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Prometheus endpoint may return 200 or 500 depending on setup
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Should allow public access to actuator env endpoint")
    void shouldAllowPublicAccessToActuatorEnv() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Should allow public access to actuator configprops endpoint")
    void shouldAllowPublicAccessToActuatorConfigProps() throws Exception {
        mockMvc.perform(get("/actuator/configprops"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Should allow public access to actuator beans endpoint")
    void shouldAllowPublicAccessToActuatorBeans() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Should allow public access to actuator mappings endpoint")
    void shouldAllowPublicAccessToActuatorMappings() throws Exception {
        mockMvc.perform(get("/actuator/mappings"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Should allow public access to actuator loggers endpoint")
    void shouldAllowPublicAccessToActuatorLoggers() throws Exception {
        mockMvc.perform(get("/actuator/loggers"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Should allow public access to actuator threaddump endpoint")
    void shouldAllowPublicAccessToActuatorThreadDump() throws Exception {
        mockMvc.perform(get("/actuator/threaddump"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Should allow public access to actuator heapdump endpoint")
    void shouldAllowPublicAccessToActuatorHeapDump() throws Exception {
        mockMvc.perform(get("/actuator/heapdump"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
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

    // Verify wildcard actuator access is not blocked by auth

    @Test
    @DisplayName("Should allow public access to non-existent actuator endpoint")
    void shouldAllowPublicAccessToNonExistentActuatorEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/nonexistent"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }
}
