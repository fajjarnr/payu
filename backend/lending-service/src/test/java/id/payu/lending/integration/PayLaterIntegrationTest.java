package id.payu.lending.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.lending.adapter.external.TransactionClient;
import id.payu.lending.dto.PayLaterLimitRequest;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the PayLater workflow.
 * Verifies activation, purchases, payments, credit-limit enforcement,
 * and transaction history retrieval.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
@Import(TestContainersConfig.class)
@DisplayName("PayLater Integration Tests")
class PayLaterIntegrationTest {

    private static final String BASE_PATH = "/api/v1/lending";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private id.payu.lending.adapter.client.AccountGrpcClient accountClient;

    @MockitoBean
    private TransactionClient transactionClient;

    @MockitoBean
    private OutboxService outboxService;

    // ─── activate pay-later ─────────────────────────────────────────

    @Test
    @DisplayName("Should activate PayLater and return 201 with correct credit limit")
    void activatePayLater_shouldReturn201WithCreditLimit() {
        UUID userId = UUID.randomUUID();
        BigDecimal creditLimit = new BigDecimal("5000000.00");

        PayLaterLimitRequest request = new PayLaterLimitRequest(creditLimit, 15);

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/paylater/activate")
                        .queryParam("userId", userId.toString())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TestContainersConfig.bearerToken())
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.userId").isEqualTo(userId.toString())
                .jsonPath("$.data.creditLimit").isEqualTo(creditLimit.doubleValue())
                .jsonPath("$.data.availableCredit").isEqualTo(creditLimit.doubleValue())
                .jsonPath("$.data.usedCredit").isEqualTo(0)
                .jsonPath("$.data.status").isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should return existing PayLater on duplicate activation")
    void activatePayLater_duplicateActivation_shouldReturnExisting() {
        UUID userId = UUID.randomUUID();
        PayLaterLimitRequest request = new PayLaterLimitRequest(new BigDecimal("1000000.00"), 1);

        // First activation
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/paylater/activate")
                        .queryParam("userId", userId.toString())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TestContainersConfig.bearerToken())
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Second activation — should return existing account
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/paylater/activate")
                        .queryParam("userId", userId.toString())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TestContainersConfig.bearerToken())
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.userId").isEqualTo(userId.toString());
    }

    // ─── record purchase ────────────────────────────────────────────

    @Test
    @DisplayName("Should reduce available credit after recording a purchase")
    void recordPurchase_shouldReduceAvailableCredit() {
        UUID userId = UUID.randomUUID();
        BigDecimal creditLimit = new BigDecimal("2000000.00");
        BigDecimal purchaseAmount = new BigDecimal("500000.00");

        activatePayLaterForUser(userId, creditLimit);

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/paylater/" + userId + "/purchase")
                        .queryParam("merchantName", "Tokopedia")
                        .queryParam("amount", purchaseAmount.toPlainString())
                        .queryParam("description", "Phone purchase")
                        .build())
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.type").isEqualTo("PURCHASE")
                .jsonPath("$.data.amount").isEqualTo(purchaseAmount.doubleValue())
                .jsonPath("$.data.merchantName").isEqualTo("Tokopedia")
                .jsonPath("$.data.status").isEqualTo("COMPLETED");

        // Verify available credit decreased
        webTestClient.get()
                .uri(BASE_PATH + "/paylater/" + userId)
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.availableCredit").isEqualTo(
                        creditLimit.subtract(purchaseAmount).doubleValue());
    }

    // ─── record payment ─────────────────────────────────────────────

    @Test
    @DisplayName("Should increase available credit after recording a payment")
    void recordPayment_shouldIncreaseAvailableCredit() {
        UUID userId = UUID.randomUUID();
        BigDecimal creditLimit = new BigDecimal("3000000.00");
        BigDecimal purchaseAmount = new BigDecimal("1000000.00");
        BigDecimal paymentAmount = new BigDecimal("500000.00");

        activatePayLaterForUser(userId, creditLimit);

        // Make a purchase first
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/paylater/" + userId + "/purchase")
                        .queryParam("merchantName", "Shopee")
                        .queryParam("amount", purchaseAmount.toPlainString())
                        .build())
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isCreated();

        // Record payment
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/paylater/" + userId + "/payment")
                        .queryParam("amount", paymentAmount.toPlainString())
                        .build())
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.type").isEqualTo("PAYMENT")
                .jsonPath("$.data.status").isEqualTo("COMPLETED");

        // Verify available credit = limit - purchase + payment
        BigDecimal expectedAvailable = creditLimit.subtract(purchaseAmount).add(paymentAmount);
        webTestClient.get()
                .uri(BASE_PATH + "/paylater/" + userId)
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.availableCredit").isEqualTo(expectedAvailable.doubleValue());
    }

    // ─── credit limit enforcement ───────────────────────────────────

    @Test
    @DisplayName("Should reject purchase that exceeds available credit limit")
    void recordPurchase_exceedingCreditLimit_shouldBeRejected() {
        UUID userId = UUID.randomUUID();
        BigDecimal creditLimit = new BigDecimal("1000000.00");
        BigDecimal excessiveAmount = new BigDecimal("1500000.00");

        activatePayLaterForUser(userId, creditLimit);

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/paylater/" + userId + "/purchase")
                        .queryParam("merchantName", "LuxuryStore")
                        .queryParam("amount", excessiveAmount.toPlainString())
                        .build())
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                // Business rule: insufficient credit should be 4xx, not 5xx
                // TODO: Service should throw a proper BusinessException mapped to 400/422
                .expectStatus().is4xxClientError();
    }

    // ─── transaction history ────────────────────────────────────────

    @Test
    @DisplayName("Should return correct transaction history after purchases and payments")
    void getTransactionHistory_shouldReturnAllTransactions() {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        BigDecimal creditLimit = new BigDecimal("10000000.00");

        // Activate with the test user (matched by JWT decoder)
        activatePayLaterForUser(userId, creditLimit);

        // Record two purchases
        recordPurchaseForUser(userId, "MerchantA", new BigDecimal("100000.00"));
        recordPurchaseForUser(userId, "MerchantB", new BigDecimal("200000.00"));

        // Record one payment
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/paylater/" + userId + "/payment")
                        .queryParam("amount", "50000.00")
                        .build())
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isCreated();

        // Get transaction history
        byte[] body = webTestClient.get()
                .uri(BASE_PATH + "/paylater/" + userId + "/transactions")
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isArray()
                .returnResult()
                .getResponseBody();

        JsonNode data = extractDataArray(body);
        assertThat(data).isNotNull();
        assertThat(data.size()).isGreaterThanOrEqualTo(3); // 2 purchases + 1 payment
    }

    // ─── helpers ────────────────────────────────────────────────────

    private void activatePayLaterForUser(UUID userId, BigDecimal creditLimit) {
        PayLaterLimitRequest request = new PayLaterLimitRequest(creditLimit, 1);

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/paylater/activate")
                        .queryParam("userId", userId.toString())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TestContainersConfig.bearerToken())
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();
    }

    private void recordPurchaseForUser(UUID userId, String merchantName, BigDecimal amount) {
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/paylater/" + userId + "/purchase")
                        .queryParam("merchantName", merchantName)
                        .queryParam("amount", amount.toPlainString())
                        .build())
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isCreated();
    }

    private JsonNode extractDataArray(byte[] body) {
        if (body == null) return null;
        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode data = root.path("data");
            return data.isArray() ? data : null;
        } catch (Exception e) {
            return null;
        }
    }
}
