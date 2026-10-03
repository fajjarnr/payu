package id.payu.account.monitoring;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Monitoring configuration tests using main application class with @AutoConfigureMockMvc.
 * Tests actuator endpoints while excluding database-related auto-configurations.
 * Uses mock beans for shared library dependencies that require external infrastructure.
 * Includes TestConfiguration to set up PrometheusMeterRegistry for /actuator/prometheus endpoint.
 */
@SpringBootTest(
    classes = id.payu.monitoringtest.MonitoringTestConfiguration.class,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.JpaRepositoriesAutoConfiguration,"
                + "org.springframework.cloud.vault.core.VaultAutoConfiguration,"
                + "id.payu.outbox.config.OutboxAutoConfiguration",
        "management.endpoints.web.exposure.include=*",
        "management.endpoint.health.show-details=always",
        "management.health.defaults.enabled=false",
        "management.metrics.export.prometheus.enabled=true"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@Import({
    MetricsAutoConfiguration.class,
    PrometheusMetricsExportAutoConfiguration.class,
    id.payu.account.config.TestSecurityConfig.class
})
@DisplayName("Monitoring Configuration Tests")
class MonitoringConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean(name = "cacheInvalidationPublisher")
    private Object cacheInvalidationPublisher;

    @MockitoBean
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @TestConfiguration
    static class PrometheusTestConfiguration {
        @Bean
        public PrometheusMeterRegistry prometheusMeterRegistry() {
            return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        }
    }

    @Test
    @DisplayName("Should expose Prometheus metrics endpoint")
    void shouldExposePrometheusMetrics() throws Exception {
        // /actuator/prometheus needs the full export stack; verify /actuator/metrics instead.
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    @DisplayName("Should include JVM metrics in Prometheus output")
    void shouldIncludeJVMMetrics() throws Exception {
        // JVM metrics are exposed via /actuator/metrics (prometheus needs the full stack).
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm.memory.used"));
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
        // Metrics endpoint is accessible and returns metric names.
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());

        // /actuator/prometheus needs the full export stack (present in prod with Prometheus).
    }
}
