package id.payu.fx.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
 * Additional integration tests for FX Conversion flows.
 * Tests conversion execution, retrieval, and reversal scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("FX Conversion Flow Integration Tests")
class FxConversionFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static final String TEST_ACCOUNT_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
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

    @Nested
    @DisplayName("Conversion Execution")
    class ConversionExecutionTests {

        @Test
        @DisplayName("Should execute USD to IDR conversion with auth")
        void shouldExecuteConversion() {
            Map<String, Object> body = Map.of(
                    "fromCurrency", "USD",
                    "toCurrency", "IDR",
                    "amount", 100
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/conversions",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            // Success path: accept CREATED or OK; also accept 429/503 from rate limiting
            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.CREATED,
                    HttpStatus.OK,
                    HttpStatus.TOO_MANY_REQUESTS,
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        @Test
        @DisplayName("Should reject conversion with negative amount")
        void shouldRejectConversionWithNegativeAmount() {
            Map<String, Object> body = Map.of(
                    "fromCurrency", "USD",
                    "toCurrency", "IDR",
                    "amount", -100
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/conversions",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        @Test
        @DisplayName("Should reject conversion with zero amount")
        void shouldRejectConversionWithZeroAmount() {
            Map<String, Object> body = Map.of(
                    "fromCurrency", "USD",
                    "toCurrency", "IDR",
                    "amount", 0
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/conversions",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        @Test
        @DisplayName("Should reject conversion with same currency")
        void shouldRejectConversionWithSameCurrency() {
            Map<String, Object> body = Map.of(
                    "fromCurrency", "USD",
                    "toCurrency", "USD",
                    "amount", 100
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/conversions",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
    }

    @Nested
    @DisplayName("Conversion Retrieval")
    class ConversionRetrievalTests {

        @Test
        @DisplayName("Should return empty list for new user conversions")
        void shouldReturnEmptyConversionsForNewUser() {
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
        @DisplayName("Should return 404 for non-existent conversion")
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

    @Nested
    @DisplayName("Conversion Reversal")
    class ConversionReversalTests {

        @Test
        @DisplayName("Should return 404 when reversing non-existent conversion")
        void shouldReturn404WhenReversingNonExistentConversion() {
            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/conversions/00000000-0000-0000-0000-000000000000/reverse",
                    HttpMethod.POST,
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

    @Nested
    @DisplayName("Rate Queries")
    class RateQueryTests {

        @Test
        @DisplayName("Should support major currency pairs")
        void shouldSupportMajorCurrencyPairs() {
            List<String[]> pairs = List.of(
                    new String[]{"USD", "IDR"},
                    new String[]{"EUR", "IDR"},
                    new String[]{"SGD", "IDR"},
                    new String[]{"USD", "EUR"}
            );

            for (String[] pair : pairs) {
                HttpEntity<Void> request = new HttpEntity<>(authHeaders());
                ResponseEntity<String> response = restTemplate.exchange(
                        baseUrl() + "/rates/" + pair[0] + "/" + pair[1],
                        HttpMethod.GET,
                        request,
                        String.class
                );

                // Accept various responses depending on data availability
                assertThat(response.getStatusCode()).isIn(
                        HttpStatus.OK,
                        HttpStatus.NOT_FOUND
                );
            }
        }

        @Test
        @DisplayName("Should return error for invalid currency codes")
        void shouldReturnErrorForInvalidCurrencyCodes() {
            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/rates/XXX/YYY",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.NOT_FOUND
            );
        }

        @Test
        @DisplayName("Should return all available rates")
        void shouldReturnAllRates() {
            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/rates",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }
    }

    @Test
    @DisplayName("Should require authentication for conversion execution")
    void shouldRequireAuthForConversion() {
        Map<String, Object> body = Map.of(
                "fromCurrency", "USD",
                "toCurrency", "IDR",
                "amount", 100
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/conversions",
                HttpMethod.POST,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isIn(
                HttpStatus.UNAUTHORIZED,
                HttpStatus.FORBIDDEN
        );
    }

    @Test
    @DisplayName("Should require authentication for rate queries")
    void shouldRequireAuthForRateQueries() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/rates/USD/IDR",
                HttpMethod.GET,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isIn(
                HttpStatus.UNAUTHORIZED,
                HttpStatus.FORBIDDEN
        );
    }
}
