package id.payu.account.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Monitoring configuration tests using minimal Spring Boot configuration.
 * Tests actuator endpoints without loading the full application context.
 */
@SpringBootTest(
    classes = MonitoringTestConfiguration.class,
    properties = {
        "management.endpoints.web.exposure.include=*",
        "management.endpoint.health.enabled=true",
        "management.endpoint.prometheus.enabled=true",
        "management.endpoint.metrics.enabled=true",
        "management.endpoint.info.enabled=true"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Monitoring Configuration Tests")
class MonitoringConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should expose Prometheus metrics endpoint")
    void shouldExposePrometheusMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should include JVM metrics in Prometheus output")
    void shouldIncludeJVMMetrics() throws Exception {
        String content = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content)
                .contains("jvm_memory_used_bytes")
                .contains("process_cpu_seconds_total");
    }

    @Test
    @DisplayName("Should expose health endpoint")
    void shouldExposeHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("Should expose metrics endpoint")
    void shouldExposeMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    @DisplayName("Should include HTTP request metrics")
    void shouldIncludeHTTPMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics/http.server.requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("http.server.requests"));
    }

    @Test
    @DisplayName("Should include JVM memory metrics")
    void shouldIncludeJVMMemoryMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm.memory.used"));
    }

    @Test
    @DisplayName("Should expose info endpoint")
    void shouldExposeInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return application name in metrics")
    void shouldReturnApplicationNameInMetrics() throws Exception {
        String content = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("application=\"account-service\"");
    }
}
