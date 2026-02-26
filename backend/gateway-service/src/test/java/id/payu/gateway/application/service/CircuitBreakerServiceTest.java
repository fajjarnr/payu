package id.payu.gateway.application.service;

import id.payu.gateway.config.GatewayConfig;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("CircuitBreakerService Unit Tests")
class CircuitBreakerServiceTest {

    private CircuitBreakerService service;
    private GatewayConfig config;
    private GatewayConfig.CircuitBreakerConfig cbConfig;

    @BeforeEach
    void setUp() {
        config = Mockito.mock(GatewayConfig.class);
        cbConfig = Mockito.mock(GatewayConfig.CircuitBreakerConfig.class);
        when(config.circuitBreaker()).thenReturn(cbConfig);
        when(cbConfig.enabled()).thenReturn(true);
        when(cbConfig.failureRatio()).thenReturn(0.5);
        when(cbConfig.delay()).thenReturn(Duration.ofSeconds(30));
        when(cbConfig.successThreshold()).thenReturn(3);

        service = new CircuitBreakerService();
        // Inject config via reflection
        try {
            var field = CircuitBreakerService.class.getDeclaredField("config");
            field.setAccessible(true);
            field.set(service, config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker States")
    class CircuitBreakerStates {

        @Test
        @DisplayName("should start in CLOSED state")
        void shouldStartClosed() {
            CircuitBreakerService.CircuitBreakerInfo info = service.getCircuitState("test-service");
            assertEquals(CircuitBreakerService.State.CLOSED, info.state());
        }

        @Test
        @DisplayName("should allow requests when circuit is CLOSED")
        void shouldAllowRequestsWhenClosed() {
            Response response = service.execute("test-service", () ->
                    Uni.createFrom().item(Response.ok("success").build())
            ).await().atMost(Duration.ofSeconds(5));

            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("should record successes")
        void shouldRecordSuccesses() {
            for (int i = 0; i < 5; i++) {
                service.execute("test-service", () ->
                        Uni.createFrom().item(Response.ok("success").build())
                ).await().atMost(Duration.ofSeconds(5));
            }

            CircuitBreakerService.CircuitBreakerInfo info = service.getCircuitState("test-service");
            assertEquals(5, info.successCount());
            assertEquals(0, info.failureCount());
            assertEquals(CircuitBreakerService.State.CLOSED, info.state());
        }

        @Test
        @DisplayName("should record failures from 5xx responses")
        void shouldRecordFailuresFrom5xx() {
            service.execute("test-service", () ->
                    Uni.createFrom().item(Response.status(503).build())
            ).await().atMost(Duration.ofSeconds(5));

            CircuitBreakerService.CircuitBreakerInfo info = service.getCircuitState("test-service");
            assertEquals(1, info.failureCount());
        }

        @Test
        @DisplayName("should open circuit when failure ratio exceeded")
        void shouldOpenWhenFailureRatioExceeded() {
            // Need at least 10 requests (VOLUME_THRESHOLD) with >50% failures
            for (int i = 0; i < 12; i++) {
                service.execute("test-service", () ->
                        Uni.createFrom().item(Response.status(503).entity("error").build())
                ).await().atMost(Duration.ofSeconds(5));
            }

            CircuitBreakerService.CircuitBreakerInfo info = service.getCircuitState("test-service");
            assertEquals(CircuitBreakerService.State.OPEN, info.state());
        }

        @Test
        @DisplayName("should fail fast when circuit is OPEN")
        void shouldFailFastWhenOpen() {
            // Force circuit open by sending many failures
            for (int i = 0; i < 12; i++) {
                service.execute("test-service", () ->
                        Uni.createFrom().item(Response.status(503).entity("error").build())
                ).await().atMost(Duration.ofSeconds(5));
            }

            // Next request should fail fast with 503 without calling the action
            Response response = service.execute("test-service", () ->
                    Uni.createFrom().item(Response.ok("should not reach here").build())
            ).await().atMost(Duration.ofSeconds(5));

            assertEquals(503, response.getStatus());
            String body = response.readEntity(String.class);
            assertTrue(body.contains("CIRCUIT_OPEN"));
        }

        @Test
        @DisplayName("should handle exceptions and record as failures")
        void shouldHandleExceptionsAsFailures() {
            Response response = service.execute("test-service", () ->
                    Uni.createFrom().failure(new RuntimeException("Connection refused"))
            ).await().atMost(Duration.ofSeconds(5));

            assertEquals(503, response.getStatus());
            CircuitBreakerService.CircuitBreakerInfo info = service.getCircuitState("test-service");
            assertEquals(1, info.failureCount());
        }

        @Test
        @DisplayName("should not open circuit below volume threshold")
        void shouldNotOpenBelowVolumeThreshold() {
            // Send fewer than 10 requests (volume threshold), all failures
            for (int i = 0; i < 5; i++) {
                service.execute("test-service", () ->
                        Uni.createFrom().item(Response.status(500).entity("error").build())
                ).await().atMost(Duration.ofSeconds(5));
            }

            CircuitBreakerService.CircuitBreakerInfo info = service.getCircuitState("test-service");
            assertEquals(CircuitBreakerService.State.CLOSED, info.state(),
                    "Circuit should remain CLOSED below volume threshold");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Disabled")
    class CircuitBreakerDisabled {

        @Test
        @DisplayName("should pass through when disabled")
        void shouldPassThroughWhenDisabled() {
            when(cbConfig.enabled()).thenReturn(false);

            Response response = service.execute("test-service", () ->
                    Uni.createFrom().item(Response.ok("direct").build())
            ).await().atMost(Duration.ofSeconds(5));

            assertEquals(200, response.getStatus());
        }
    }

    @Nested
    @DisplayName("Circuit State Inspection")
    class CircuitStateInspection {

        @Test
        @DisplayName("should return empty map initially")
        void shouldReturnEmptyMapInitially() {
            assertTrue(service.getCircuitStates().isEmpty());
        }

        @Test
        @DisplayName("should track multiple services independently")
        void shouldTrackMultipleServicesIndependently() {
            service.execute("service-a", () ->
                    Uni.createFrom().item(Response.ok().build())
            ).await().atMost(Duration.ofSeconds(5));

            service.execute("service-b", () ->
                    Uni.createFrom().item(Response.status(500).build())
            ).await().atMost(Duration.ofSeconds(5));

            var states = service.getCircuitStates();
            assertEquals(2, states.size());
            assertEquals(1, states.get("service-a").successCount());
            assertEquals(1, states.get("service-b").failureCount());
        }

        @Test
        @DisplayName("should reset circuit breaker")
        void shouldResetCircuitBreaker() {
            // Generate some state
            service.execute("test-service", () ->
                    Uni.createFrom().item(Response.status(500).build())
            ).await().atMost(Duration.ofSeconds(5));

            service.reset("test-service");

            CircuitBreakerService.CircuitBreakerInfo info = service.getCircuitState("test-service");
            assertEquals(CircuitBreakerService.State.CLOSED, info.state());
            assertEquals(0, info.failureCount());
        }
    }
}
