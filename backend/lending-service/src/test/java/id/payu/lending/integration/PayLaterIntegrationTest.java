package id.payu.lending.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.lending.adapter.external.TransactionClient;
import id.payu.lending.dto.PayLaterLimitRequest;
import id.payu.lending.interfaces.dto.PayLaterPaymentRequest;
import id.payu.lending.interfaces.dto.PayLaterPurchaseRequest;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the PayLater workflow under the current API contract:
 * activation, purchases, payments, credit-limit enforcement, and history.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Import(TestContainersConfig.class)
@DisplayName("PayLater Integration Tests")
class PayLaterIntegrationTest {

    private static final String BASE_PATH = "/api/v1/lending";
    private static final String TENANT = "test-tenant";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM paylater_transactions");
        jdbcTemplate.update("DELETE FROM paylater_accounts");
    }

    @MockitoBean
    private id.payu.lending.adapter.client.AccountGrpcClient accountClient;

    @MockitoBean
    private TransactionClient transactionClient;

    @MockitoBean
    private OutboxService outboxService;

    @MockitoBean
    private id.payu.lending.domain.port.out.WalletPaymentPort walletPaymentPort;

    // ─── activate pay-later ─────────────────────────────────────────

    @Test
    @DisplayName("Should activate PayLater and return 201 with correct credit limit")
    void activatePayLater_shouldReturn201WithCreditLimit() throws Exception {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        BigDecimal creditLimit = new BigDecimal("5000000.00");

        PayLaterLimitRequest request = new PayLaterLimitRequest(creditLimit, 15);

        mockMvc.perform(post(BASE_PATH + "/paylater/activate")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.creditLimit").value(creditLimit.doubleValue()))
                .andExpect(jsonPath("$.data.availableCredit").value(creditLimit.doubleValue()))
                .andExpect(jsonPath("$.data.usedCredit").value(0))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should return existing PayLater on duplicate activation")
    void activatePayLater_duplicateActivation_shouldReturnExisting() throws Exception {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        PayLaterLimitRequest request = new PayLaterLimitRequest(new BigDecimal("1000000.00"), 1);

        mockMvc.perform(post(BASE_PATH + "/paylater/activate")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE_PATH + "/paylater/activate")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(userId.toString()));
    }

    // ─── record purchase ────────────────────────────────────────────

    @Test
    @DisplayName("Should reduce available credit after recording a purchase")
    void recordPurchase_shouldReduceAvailableCredit() throws Exception {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        BigDecimal creditLimit = new BigDecimal("2000000.00");
        BigDecimal purchaseAmount = new BigDecimal("500000.00");

        activatePayLaterForUser(userId, creditLimit);

        PayLaterPurchaseRequest purchase =
                new PayLaterPurchaseRequest("Tokopedia", purchaseAmount, "Phone purchase");

        mockMvc.perform(post(BASE_PATH + "/paylater/" + userId + "/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "purch-" + UUID.randomUUID())
                        .content(MAPPER.writeValueAsString(purchase)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("PURCHASE"))
                .andExpect(jsonPath("$.data.amount").value(purchaseAmount.doubleValue()))
                .andExpect(jsonPath("$.data.merchantName").value("Tokopedia"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get(BASE_PATH + "/paylater/" + userId)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableCredit").value(
                        creditLimit.subtract(purchaseAmount).doubleValue()));
    }

    // ─── record payment ─────────────────────────────────────────────

    @Test
    @DisplayName("Should increase available credit after recording a payment")
    void recordPayment_shouldIncreaseAvailableCredit() throws Exception {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        BigDecimal creditLimit = new BigDecimal("3000000.00");
        BigDecimal purchaseAmount = new BigDecimal("1000000.00");
        BigDecimal paymentAmount = new BigDecimal("500000.00");

        activatePayLaterForUser(userId, creditLimit);

        mockMvc.perform(post(BASE_PATH + "/paylater/" + userId + "/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "purch-" + UUID.randomUUID())
                        .content(MAPPER.writeValueAsString(
                                new PayLaterPurchaseRequest("Shopee", purchaseAmount, "Shopee purchase"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE_PATH + "/paylater/" + userId + "/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "pay-" + UUID.randomUUID())
                        .content(MAPPER.writeValueAsString(new PayLaterPaymentRequest(paymentAmount))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("PAYMENT"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        BigDecimal expectedAvailable = creditLimit.subtract(purchaseAmount).add(paymentAmount);
        mockMvc.perform(get(BASE_PATH + "/paylater/" + userId)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableCredit").value(expectedAvailable.doubleValue()));
    }

    // ─── credit limit enforcement ───────────────────────────────────

    @Test
    @DisplayName("Should reject purchase that exceeds available credit limit")
    void recordPurchase_exceedingCreditLimit_shouldBeRejected() throws Exception {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        BigDecimal creditLimit = new BigDecimal("1000000.00");
        BigDecimal excessiveAmount = new BigDecimal("1500000.00");

        activatePayLaterForUser(userId, creditLimit);

        mockMvc.perform(post(BASE_PATH + "/paylater/" + userId + "/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "purch-" + UUID.randomUUID())
                        .content(MAPPER.writeValueAsString(
                                new PayLaterPurchaseRequest("LuxuryStore", excessiveAmount, "Luxury purchase"))))
                .andExpect(status().is4xxClientError());
    }

    // ─── transaction history ────────────────────────────────────────

    @Test
    @DisplayName("Should return correct transaction history after purchases and payments")
    void getTransactionHistory_shouldReturnAllTransactions() throws Exception {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        BigDecimal creditLimit = new BigDecimal("10000000.00");

        activatePayLaterForUser(userId, creditLimit);

        recordPurchaseForUser(userId, "MerchantA", new BigDecimal("100000.00"));
        recordPurchaseForUser(userId, "MerchantB", new BigDecimal("200000.00"));

        mockMvc.perform(post(BASE_PATH + "/paylater/" + userId + "/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "pay-" + UUID.randomUUID())
                        .content(MAPPER.writeValueAsString(new PayLaterPaymentRequest(new BigDecimal("50000.00")))))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(get(BASE_PATH + "/paylater/" + userId + "/transactions")
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn().getResponse().getContentAsString();

        JsonNode data = extractDataArray(body);
        assertThat(data).isNotNull();
        assertThat(data.size()).isGreaterThanOrEqualTo(3); // 2 purchases + 1 payment
    }

    // ─── helpers ────────────────────────────────────────────────────

    private void activatePayLaterForUser(UUID userId, BigDecimal creditLimit) throws Exception {
        PayLaterLimitRequest request = new PayLaterLimitRequest(creditLimit, 1);

        mockMvc.perform(post(BASE_PATH + "/paylater/activate")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void recordPurchaseForUser(UUID userId, String merchantName, BigDecimal amount) throws Exception {
        mockMvc.perform(post(BASE_PATH + "/paylater/" + userId + "/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "purch-" + UUID.randomUUID())
                        .content(MAPPER.writeValueAsString(new PayLaterPurchaseRequest(merchantName, amount, merchantName + " purchase"))))
                .andExpect(status().isCreated());
    }

    private JsonNode extractDataArray(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode data = MAPPER.readTree(body).path("data");
            return data.isArray() ? data : null;
        } catch (Exception e) {
            return null;
        }
    }
}
