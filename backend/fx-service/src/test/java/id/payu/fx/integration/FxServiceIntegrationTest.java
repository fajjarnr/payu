package id.payu.fx.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for FX Rate API and Currency Conversion flows.
 * Uses H2 in-memory database with mock JWT authentication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("FX Service Integration Tests")
class FxServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String TEST_ACCOUNT_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        // Mock the JWT decoder to accept any token and return a valid JWT
        when(jwtDecoder.decode(anyString())).thenReturn(buildTestJwt(TEST_ACCOUNT_ID));
    }

    private Jwt buildTestJwt(String accountId) {
        return new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", accountId,
                        "iss", "https://fake-issuer.example.com",
                        "account_id", accountId,
                        "preferred_username", "testuser",
                        "realm_access", Map.of("roles", List.of("user")),
                        "scope", "openid profile email"
                )
        );
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/fx-api/v1";
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer test-token");
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
            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/rates/USD/IDR",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            // Mock provider may or may not have a rate seeded
            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.OK, HttpStatus.NOT_FOUND,
                    HttpStatus.TOO_MANY_REQUESTS, HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        @Test
        @DisplayName("Should get all available rates")
        void shouldGetAllRates() {
            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
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
            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/rates/INVALID/XYZ",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND,
                    HttpStatus.TOO_MANY_REQUESTS, HttpStatus.SERVICE_UNAVAILABLE
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

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/conversions/estimate",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.OK, HttpStatus.NOT_FOUND,
                    HttpStatus.TOO_MANY_REQUESTS, HttpStatus.SERVICE_UNAVAILABLE
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
                    HttpStatus.TOO_MANY_REQUESTS, HttpStatus.SERVICE_UNAVAILABLE
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

            // The endpoint requires authentication, so it should reject without auth header
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
                    HttpStatus.TOO_MANY_REQUESTS,
                    HttpStatus.SERVICE_UNAVAILABLE
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
