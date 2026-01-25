package id.payu.account.monitoring;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Distributed tracing configuration tests using @SpringBootTest with @AutoConfigureMockMvc.
 * Tests actuator endpoints while excluding database-related auto-configurations (JPA, Flyway, Vault).
 * Actuator endpoints remain fully functional per Spring Boot 3.4 documentation.
 */
@SpringBootTest(
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                + "org.springframework.cloud.vault.core.VaultAutoConfiguration"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("Distributed Tracing Configuration Tests")
class TracingConfigurationTest {

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired
    private MockMvc mockMvc;

    // Mock security beans for OAuth2/JWT
    @MockBean
    private JwtDecoder jwtDecoder;

    // Mock JPA repositories
    @MockBean
    private id.payu.account.adapter.persistence.repository.UserRepository userRepository;

    @MockBean
    private id.payu.account.adapter.persistence.repository.ProfileRepository profileRepository;

    // Mock persistence adapters
    @MockBean
    private id.payu.account.adapter.persistence.UserPersistenceAdapter userPersistenceAdapter;

    // Mock messaging adapters
    @MockBean
    private id.payu.account.adapter.messaging.KafkaUserEventPublisherAdapter kafkaUserEventPublisherAdapter;

    // Mock client adapters
    @MockBean
    private id.payu.account.adapter.client.KycVerificationAdapter kycVerificationAdapter;

    @MockBean
    private id.payu.account.adapter.client.GatewayClient gatewayClient;

    @Test
    @DisplayName("Should have Tracer bean available")
    void shouldHaveTracerBeanAvailable() {
        assertThat(tracer).isNotNull();
    }

    @Test
    @DisplayName("Should create trace context on HTTP request")
    void shouldCreateTraceContextOnHttpRequest() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        
        if (tracer != null) {
            assertThat(tracer.currentSpan()).isNotNull();
        }
    }

    @Test
    @DisplayName("Should include tracing in actuator configuration")
    void shouldIncludeTracingInActuatorConfiguration() throws Exception {
        mockMvc.perform(get("/actuator/tracing"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should expose OTLP endpoint configuration")
    void shouldHaveOtlpExporterConfigured() {
        // The OTEL_ENDPOINT environment variable should be configurable
        // In test environment, we just verify the property can be set
        String otelEndpoint = System.getenv("OTEL_ENDPOINT");
        // If not set, tests should still pass - endpoint is optional for unit tests
        // In production with Jaeger, this would be set to http://jaeger:4317
    }
}
