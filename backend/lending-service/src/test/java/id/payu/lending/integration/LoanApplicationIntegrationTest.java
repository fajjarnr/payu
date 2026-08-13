package id.payu.lending.integration;

import id.payu.lending.adapter.external.TransactionClient;
import id.payu.lending.domain.model.LoanType;
import id.payu.lending.dto.LoanApplicationRequest;
import id.payu.outbox.service.OutboxService;
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
 * Integration tests for loan application under the current API contract
 * (authenticated-user scoping, idempotency key, credit-score gating).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Import(TestContainersConfig.class)
@DisplayName("Loan Application Integration Tests")
class LoanApplicationIntegrationTest {

    private static final String BASE_PATH = "/api/v1/lending";
    private static final String TENANT = "test-tenant";

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

    // ─── POST /loans ────────────────────────────────────────────────

    @Test
    @DisplayName("Should create a loan application and return 201 with APPROVED status")
    void applyLoan_withValidRequest_shouldReturn201() throws Exception {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        String externalId = "EXT-" + UUID.randomUUID();

        LoanApplicationRequest request = new LoanApplicationRequest(
                userId, externalId, LoanType.PERSONAL_LOAN,
                new BigDecimal("5000000.00"), 12, "Home renovation");

        MvcResult applied = mockMvc.perform(post(BASE_PATH + "/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "apply-" + UUID.randomUUID())
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(applied))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.tenureMonths").value(12));
    }

    @Test
    @DisplayName("Should reject loan application with negative amount and return 400")
    void applyLoan_withNegativeAmount_shouldReturn400() throws Exception {
        LoanApplicationRequest request = new LoanApplicationRequest(
                TestContainersConfig.TEST_USER_ID, "EXT-" + UUID.randomUUID(),
                LoanType.PERSONAL_LOAN, new BigDecimal("-100000.00"), 12, "Invalid loan");

        mockMvc.perform(post(BASE_PATH + "/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Idempotency-Key", "apply-" + UUID.randomUUID())
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject loan application with zero tenure and return 400")
    void applyLoan_withZeroTenure_shouldReturn400() throws Exception {
        LoanApplicationRequest request = new LoanApplicationRequest(
                TestContainersConfig.TEST_USER_ID, "EXT-" + UUID.randomUUID(),
                LoanType.PERSONAL_LOAN, new BigDecimal("1000000.00"), 0, "Invalid tenure");

        mockMvc.perform(post(BASE_PATH + "/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Idempotency-Key", "apply-" + UUID.randomUUID())
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /loans/{loanId} ────────────────────────────────────────

    @Test
    @DisplayName("Should return loan by ID with 200")
    void getLoan_withExistingId_shouldReturn200() throws Exception {
        String loanId = createTestLoan();

        mockMvc.perform(get(BASE_PATH + "/loans/" + loanId)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(loanId))
                .andExpect(jsonPath("$.data.tenureMonths").value(12));
    }

    @Test
    @DisplayName("Should return 404 for non-existent loan")
    void getLoan_withNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/loans/" + UUID.randomUUID())
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject loan application without authorization (401 or 403)")
    void applyLoan_withoutAuth_shouldReturn401() throws Exception {
        LoanApplicationRequest request = new LoanApplicationRequest(
                TestContainersConfig.TEST_USER_ID, "EXT-" + UUID.randomUUID(),
                LoanType.PERSONAL_LOAN, new BigDecimal("1000000.00"), 6, "No auth");

        int status = mockMvc.perform(post(BASE_PATH + "/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", "apply-" + UUID.randomUUID())
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andReturn().getResponse().getStatus();

        // Security gate enforced: missing token is rejected as 401 (entrypoint)
        // or 403 (method security) depending on the security chain ordering.
        assertThat(status).isIn(401, 403);
    }

    // ─── helpers ────────────────────────────────────────────────────

    private String createTestLoan() throws Exception {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        LoanApplicationRequest request = new LoanApplicationRequest(
                userId, "EXT-GET-" + UUID.randomUUID(), LoanType.PERSONAL_LOAN,
                new BigDecimal("2000000.00"), 12, "Education");

        seedCreditScore(userId);

        MvcResult applied = mockMvc.perform(post(BASE_PATH + "/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "apply-" + UUID.randomUUID())
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mockMvc.perform(asyncDispatch(applied))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(body).path("data").path("id").asText(null);
    }

    private void seedCreditScore(UUID userId) throws Exception {
        id.payu.lending.dto.UserResponse user = new id.payu.lending.dto.UserResponse(
                userId, "EXT-USER", "user", "u@test.dev", "+62812", "Test User", "1234567890123456",
                "ACTIVE", "APPROVED", LocalDateTime.now().minusYears(2));
        UUID accountId = UUID.randomUUID();
        when(accountClient.getUserProfile(userId.toString())).thenReturn(user);
        when(accountClient.getAccountIdsByUserId(userId.toString())).thenReturn(List.of(accountId));

        id.payu.lending.dto.TransactionSummaryResponse summary =
                new id.payu.lending.dto.TransactionSummaryResponse(
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
}
