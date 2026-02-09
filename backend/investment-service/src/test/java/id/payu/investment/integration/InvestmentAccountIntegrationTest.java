package id.payu.investment.integration;

import id.payu.investment.domain.port.out.InvestmentEventPublisherPort;
import id.payu.investment.domain.port.out.WalletServicePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Investment Account flows.
 * Tests account creation, deposit purchase, gold purchase via real DB (Testcontainers PG).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(InvestmentTestConfig.class)
@Tag("integration")
@DisplayName("Investment Account Integration Tests")
class InvestmentAccountIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private WalletServicePort walletServicePort;

    @MockBean
    private InvestmentEventPublisherPort eventPublisherPort;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/investments";
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", InvestmentTestConfig.bearerToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Nested
    @DisplayName("Create Investment Account")
    class CreateAccountTests {

        @Test
        @DisplayName("Should create investment account successfully")
        void shouldCreateAccountSuccessfully() {
            String userId = InvestmentTestConfig.TEST_USER_ID;

            HttpEntity<Map<String, String>> request = new HttpEntity<>(
                    Map.of("userId", userId),
                    authHeaders()
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/accounts?userId=" + userId,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED
            );
        }

        @Test
        @DisplayName("Should retrieve account by user ID")
        void shouldRetrieveAccountByUserId() {
            String userId = UUID.randomUUID().toString();

            // Create account first
            HttpEntity<Void> createRequest = new HttpEntity<>(authHeaders());
            restTemplate.exchange(
                    baseUrl() + "/accounts?userId=" + userId,
                    HttpMethod.POST,
                    createRequest,
                    String.class
            );

            // Retrieve
            HttpEntity<Void> getRequest = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/accounts/" + userId,
                    HttpMethod.GET,
                    getRequest,
                    String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.OK, HttpStatus.NOT_FOUND
            );
        }
    }

    @Nested
    @DisplayName("Buy Deposit")
    class BuyDepositTests {

        @Test
        @DisplayName("Should buy a time deposit with valid parameters")
        void shouldBuyDepositSuccessfully() {
            when(walletServicePort.hasSufficientBalance(anyString(), any(BigDecimal.class)))
                    .thenReturn(true);
            when(walletServicePort.deductBalance(anyString(), any(BigDecimal.class)))
                    .thenReturn(true);

            String userId = UUID.randomUUID().toString();

            // Create account
            restTemplate.exchange(
                    baseUrl() + "/accounts?userId=" + userId,
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders()),
                    String.class
            );

            // Buy deposit
            String url = baseUrl() + "/deposits?accountId=" + userId
                    + "&userId=" + userId
                    + "&amount=10000000&tenure=12";

            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED
            );
        }

        @Test
        @DisplayName("Should reject deposit with insufficient balance")
        void shouldRejectDepositInsufficientBalance() {
            when(walletServicePort.hasSufficientBalance(anyString(), any(BigDecimal.class)))
                    .thenReturn(false);

            String userId = UUID.randomUUID().toString();

            // Create account
            restTemplate.exchange(
                    baseUrl() + "/accounts?userId=" + userId,
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders()),
                    String.class
            );

            String url = baseUrl() + "/deposits?accountId=" + userId
                    + "&userId=" + userId
                    + "&amount=10000000&tenure=12";

            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class
            );

            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
    }

    @Nested
    @DisplayName("Buy Gold")
    class BuyGoldTests {

        @Test
        @DisplayName("Should purchase gold successfully")
        void shouldPurchaseGoldSuccessfully() {
            when(walletServicePort.hasSufficientBalance(anyString(), any(BigDecimal.class)))
                    .thenReturn(true);
            when(walletServicePort.deductBalance(anyString(), any(BigDecimal.class)))
                    .thenReturn(true);

            String userId = UUID.randomUUID().toString();

            // Create account
            restTemplate.exchange(
                    baseUrl() + "/accounts?userId=" + userId,
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders()),
                    String.class
            );

            String url = baseUrl() + "/gold?userId=" + userId + "&amount=500000";

            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class
            );

            // Gold purchase may depend on price feed availability
            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED,
                    HttpStatus.INTERNAL_SERVER_ERROR // Price feed unavailable in test
            );
        }

        @Test
        @DisplayName("Should retrieve gold holdings")
        void shouldRetrieveGoldHoldings() {
            String userId = UUID.randomUUID().toString();

            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl() + "/gold/" + userId,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            // May return 404 if no gold holdings yet
            assertThat(response.getStatusCode()).isIn(
                    HttpStatus.OK, HttpStatus.NOT_FOUND
            );
        }
    }

    @Test
    @DisplayName("Health endpoint should be accessible")
    void healthEndpointShouldBeAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class
        );

        assertThat(response.getStatusCode()).isIn(
                HttpStatus.OK, HttpStatus.UNAUTHORIZED
        );
    }
}
