package id.payu.lending.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.lending.adapter.external.TransactionClient;
import id.payu.lending.domain.model.LoanType;
import id.payu.lending.interfaces.dto.LoanApplicationRequest;
import id.payu.lending.interfaces.dto.RepaymentRequest;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for repayment schedule creation and payment processing
 * under the current API contract (body-based amounts + idempotency keys).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Import(TestContainersConfig.class)
@DisplayName("Repayment Schedule Integration Tests")
class RepaymentScheduleIntegrationTest {

    private static final String BASE_PATH = "/api/v1/lending";
    private static final String TENANT = "test-tenant";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private id.payu.lending.adapter.client.AccountGrpcClient accountClient;

    @MockitoBean
    private TransactionClient transactionClient;

    @MockitoBean
    private OutboxService outboxService;

    @MockitoBean
    private id.payu.lending.domain.port.out.WalletPaymentPort walletPaymentPort;

    private String loanId;
    private int loanTenureMonths;

    @BeforeEach
    void setUp() throws Exception {
        loanTenureMonths = 6;
        loanId = createTestLoan(loanTenureMonths);
    }

    // ─── create repayment schedule ──────────────────────────────────

    @Test
    @DisplayName("Should create repayment schedule with correct number of installments matching tenure")
    void createRepaymentSchedule_shouldReturnInstallmentsMatchingTenure() throws Exception {
        String responseBody = mockMvc.perform(post(BASE_PATH + "/loans/" + loanId + "/repayment-schedule")
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn().getResponse().getContentAsString();

        JsonNode data = extractDataArray(responseBody);
        assertThat(data).isNotNull();
        assertThat(data.size()).isEqualTo(loanTenureMonths);

        JsonNode first = data.get(0);
        assertThat(first.path("installmentNumber").asInt()).isEqualTo(1);
        assertThat(first.path("status").asText()).isEqualTo("PENDING");
        assertThat(first.path("loanId").asText()).isEqualTo(loanId);
    }

    @Test
    @DisplayName("Should retrieve empty repayment schedule for loan without schedules")
    void getRepaymentSchedule_forLoanWithoutSchedules_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/loans/" + loanId + "/repayment-schedule")
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ─── process repayment ──────────────────────────────────────────

    @Test
    @DisplayName("Should mark installment as FULLY_PAID when repayment covers installment amount")
    void processRepayment_withFullAmount_shouldMarkFullyPaid() throws Exception {
        JsonNode schedules = createSchedule();
        String scheduleId = schedules.get(0).path("id").asText();
        String installmentAmount = schedules.get(0).path("installmentAmount").asText();

        mockMvc.perform(post(BASE_PATH + "/repayment-schedules/" + scheduleId + "/pay")
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "pay-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(new RepaymentRequest(new BigDecimal(installmentAmount)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FULLY_PAID"))
                .andExpect(jsonPath("$.data.paidDate").isNotEmpty());
    }

    @Test
    @DisplayName("Should mark installment as PARTIALLY_PAID when repayment is less than full amount")
    void processRepayment_withPartialAmount_shouldMarkPartiallyPaid() throws Exception {
        JsonNode schedules = createSchedule();
        JsonNode target = schedules.size() > 1 ? schedules.get(1) : schedules.get(0);
        String scheduleId = target.path("id").asText();

        mockMvc.perform(post(BASE_PATH + "/repayment-schedules/" + scheduleId + "/pay")
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "pay-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(new RepaymentRequest(new BigDecimal("10000.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_PAID"));
    }

    @Test
    @DisplayName("Should return error when processing repayment for already fully-paid installment")
    void processRepayment_forAlreadyPaidInstallment_shouldReturnError() throws Exception {
        JsonNode schedules = createSchedule();
        JsonNode target = schedules.get(schedules.size() - 1);
        String scheduleId = target.path("id").asText();
        String installmentAmount = target.path("installmentAmount").asText();

        mockMvc.perform(post(BASE_PATH + "/repayment-schedules/" + scheduleId + "/pay")
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "pay-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(new RepaymentRequest(new BigDecimal(installmentAmount)))))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE_PATH + "/repayment-schedules/" + scheduleId + "/pay")
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "pay-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(new RepaymentRequest(new BigDecimal("100000.00")))))
                .andExpect(status().is4xxClientError());
    }

    // ─── helpers ────────────────────────────────────────────────────

    private JsonNode createSchedule() throws Exception {
        String body = mockMvc.perform(post(BASE_PATH + "/loans/" + loanId + "/repayment-schedule")
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode schedules = extractDataArray(body);
        assertThat(schedules).isNotNull();
        assertThat(schedules).isNotEmpty();
        return schedules;
    }

    private String createTestLoan(int tenureMonths) throws Exception {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        LoanApplicationRequest request = new LoanApplicationRequest(
                userId,
                "EXT-RS-" + UUID.randomUUID(),
                LoanType.PERSONAL_LOAN,
                new BigDecimal("3000000.00"),
                tenureMonths,
                "Repayment test"
        );

        seedCreditScore(userId);

        MvcResult applied = mockMvc.perform(post(BASE_PATH + "/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "apply-" + UUID.randomUUID())
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        return extractField(mockMvc.perform(asyncDispatch(applied))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "id");
    }

    private void seedCreditScore(UUID userId) throws Exception {
        id.payu.lending.interfaces.dto.UserResponse user = new id.payu.lending.interfaces.dto.UserResponse(
                userId, "EXT-USER", "user", "u@test.dev", "+62812", "Test User", "1234567890123456",
                "ACTIVE", "APPROVED", LocalDateTime.now().minusYears(2));
        UUID accountId = UUID.randomUUID();
        when(accountClient.getUserProfile(userId.toString())).thenReturn(user);
        when(accountClient.getAccountIdsByUserId(userId.toString())).thenReturn(List.of(accountId));

        id.payu.lending.interfaces.dto.TransactionSummaryResponse summary =
                new id.payu.lending.interfaces.dto.TransactionSummaryResponse(
                        accountId, 200L, new BigDecimal("20000000.00"),
                        new BigDecimal("15000000.00"), new BigDecimal("5000000.00"),
                        198L, 2L, null, null);
        when(transactionClient.getTransactionSummary(accountId)).thenReturn(
                id.payu.api.common.response.ApiResponse.success(summary));
        when(walletPaymentPort.collectRepayment(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                anyString(), anyString(), anyString()))
                .thenReturn("wallet-tx-" + UUID.randomUUID());

        mockMvc.perform(post(BASE_PATH + "/credit-score/calculate")
                        .param("userId", userId.toString())
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String extractField(String body, String field) {
        if (body == null || body.isBlank()) return null;
        try {
            return MAPPER.readTree(body).path("data").path(field).asText(null);
        } catch (Exception e) {
            return null;
        }
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
