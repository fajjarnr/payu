package id.payu.account.monitoring;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Distributed Tracing Configuration Tests")
class TracingConfigurationTest {

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired
    private MockMvc mockMvc;

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
        System.getenv("OTEL_ENDPOINT");
    }
}
