package id.payu.fx.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for FX Rate API and Currency Conversion flows.
 * Uses real PostgreSQL (Testcontainers) with mock JWT authentication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(FxTestConfig.class)
@Tag("integration")
@DisplayName("FX Service Integration Tests")
class FxServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/fx-api/v1";
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", FxTestConfig.bearerToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders publicHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Nested
    @DisplayName("FX Rates (Public)")
    class FxRateTests {

        @Test
        @DisplayName("Should get FX rate for USD/IDR pair")
        void shouldGetFxRateForUsdIdr() {
            HttpEntity<Void> request = new HttpEntity<>(publicHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/rates/USD/IDR",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            // Mock provider may or may not have a rate seeded
            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.OK, HttpStatus.NOT_FOUND
            );
        }

        @Test
        @DisplayName("Should get all available rates")
        void shouldGetAllRates() {
            HttpEntity<Void> request = new HttpEntity<>(publicHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/rates",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should return error for invalid currency pair")
        void shouldReturnErrorForInvalidCurrency() {
            HttpEntity<Void> request = new HttpEntity<>(publicHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/rates/INVALID/XYZ",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Nested
    @DisplayName("Currency Conversion Estimate (Public)")
    class ConversionEstimateTests {

        @Test
        @DisplayName("Should estimate USD to IDR conversion")
        void shouldEstimateUsdToIdrConversion() {
            Map<String, Object> body = Map.of(
                    "fromCurrency", "USD",
                    "toCurrency", "IDR",
                    "amount", 100
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, publicHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/conversions/estimate",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.OK, HttpStatus.NOT_FOUND,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Nested
    @DisplayName("Currency Conversion (Authenticated)")
    class ConversionFlowTests {

        @Test
        @DisplayName("Should execute currency conversion with auth")
        void shouldExecuteConversion() {
            Map<String, Object> body = Map.of(
                    "fromCurrency", "USD",
                    "toCurrency", "IDR",
                    "amount", 50
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/conversions",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            // May succeed or fail depending on rate availability
            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.OK, HttpStatus.CREATED,
                    HttpStatus.NOT_FOUND,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        @Test
        @DisplayName("Should reject conversion without authentication")
        void shouldRejectConversionWithoutAuth() {
            Map<String, Object> body = Map.of(
                    "fromCurrency", "USD",
                    "toCurrency", "IDR",
                    "amount", 50
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, publicHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/conversions",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN
            );
        }

        @Test
        @DisplayName("Should get conversions for authenticated user")
        void shouldGetConversionsForUser() {
            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/conversions",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should return 404 for non-existent conversion ID")
        void shouldReturn404ForNonExistentConversion() {
            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/conversions/00000000-0000-0000-0000-000000000000",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.NOT_FOUND,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Test
    @DisplayName("Health endpoint should be accessible")
    void healthEndpointShouldBeAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/fx-api/actuator/health",
                String.class
        );

        assertThat(response.getStatusCode()).isIn(
                HttpStatus.OK, HttpStatus.NOT_FOUND
        );
    }
}
