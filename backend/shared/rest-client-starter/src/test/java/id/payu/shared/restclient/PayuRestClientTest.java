package id.payu.shared.restclient;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("PayuRestClient")
class PayuRestClientTest {

    private RestClient.Builder restClientBuilder;
    private RestClient restClient;
    private MockRestServiceServer server;
    private PayuRestClient client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        restClient = restClientBuilder.build();

        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(2)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.of(1))
                .build());
        client = new PayuRestClient(restClient, cbRegistry, retryRegistry);
    }

    @Test
    @DisplayName("get returns the decoded body and entity")
    void getDecodesResponse() {
        server.expect(requestTo("/api/v1/things/1"))
                .andRespond(withSuccess("{\"id\":\"t1\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        ResponseEntity<Map> response = client.get("svc", "/api/v1/things/1", Map.class);

        assertThat(response.getBody()).containsEntry("id", "t1");
        server.verify();
    }

    @Test
    @DisplayName("retry retries 5xx then succeeds on second attempt")
    void retriesServerError() {
        server.expect(requestTo("/api/v1/things/2"))
                .andRespond(withServerError());
        server.expect(requestTo("/api/v1/things/2"))
                .andRespond(withSuccess("{\"id\":\"t2\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        ResponseEntity<Map> response = client.get("svc", "/api/v1/things/2", Map.class);

        assertThat(response.getBody()).containsEntry("id", "t2");
        server.verify();
    }

    @Test
    @DisplayName("circuit breaker opens after repeated failures")
    void circuitBreakerOpensAfterFailures() {
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build());
        RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(1)
                .build());
        client = new PayuRestClient(restClient, cbRegistry, retryRegistry);

        for (int i = 0; i < 4; i++) {
            server.expect(requestTo("/api/v1/things/3"))
                    .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_GATEWAY));
        }
        for (int i = 0; i < 4; i++) {
            try {
                client.get("svc", "/api/v1/things/3", Map.class);
            } catch (Exception ignored) {
                // expected failures to trip the breaker
            }
        }

        assertThat(cbRegistry.circuitBreaker("rest-svc").getState())
                .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
    }
}
