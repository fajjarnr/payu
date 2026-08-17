package id.payu.lending.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.lending.domain.model.LoanType;
import id.payu.lending.interfaces.dto.LoanApplicationRequest;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
 * Financial E2E test for the full loan money lifecycle (CB-009).
 * <p>
 * Exercises the loan lifecycle through the public API: apply → repayment
 * schedule → installment payment → idempotent re-play, asserting the
 * financial invariants of the amortization schedule (money arithmetic uses
 * {@link BigDecimal} with {@link RoundingMode#HALF_EVEN} only, never float).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Tag("financial")
@Import(TestContainersConfig.class)
@DisplayName("Lending Financial E2E Tests")
class LendingFinancialE2ETest {

    private static final String BASE_PATH = "/api/v1/lending";
    private static final String TENANT = "test-tenant";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private id.payu.lending.adapter.client.AccountGrpcClient accountClient;

    @MockitoBean
    private id.payu.lending.adapter.external.TransactionClient transactionClient;

    @MockitoBean
    private id.payu.outbox.service.OutboxService outboxService;

    @MockitoBean
    private id.payu.lending.domain.port.out.WalletPaymentPort walletPaymentPort;

    // ─── full lifecycle ─────────────────────────────────────────────

    @Test
    @DisplayName("Full lifecycle: apply → schedule → pay installment → idempotent re-play")
    void fullLifecycle_applySchedulePay_shouldPreserveFinancialInvariants() throws Exception {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        BigDecimal principal = new BigDecimal("12000000.00");
        int tenure = 12;

        // 1. Seed an eligible credit score (KYC APPROVED + healthy transaction history)
        seedEligibleCreditScore(userId);

        // 2. Apply for a loan (async handler → await the dispatched response)
        LoanApplicationRequest request = new LoanApplicationRequest(
                userId, "EXT-FIN-" + UUID.randomUUID(), LoanType.PERSONAL_LOAN,
                principal, tenure, "Financial E2E");

        MvcResult applied = mockMvc.perform(post(BASE_PATH + "/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", "apply-" + UUID.randomUUID())
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        JsonNode loan = extractData(mockMvc.perform(asyncDispatch(applied))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        String loanId = loan.path("id").asText();
        BigDecimal monthlyInstallment = new BigDecimal(loan.path("monthlyInstallment").asText());
        BigDecimal interestRate = new BigDecimal(loan.path("interestRate").asText());
        assertThat(loanId).isNotBlank();
        assertThat(monthlyInstallment).isGreaterThan(BigDecimal.ZERO);
        assertThat(interestRate).isGreaterThan(BigDecimal.ZERO);

        // 2. Generate the repayment schedule
        JsonNode schedules = extractData(mockMvc.perform(post(BASE_PATH + "/loans/" + loanId + "/repayment-schedule")
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        assertThat(schedules).isNotNull();
        assertThat(schedules.size()).isEqualTo(tenure);

        // 3. Financial invariants — amortization must reconcile to the principal
        BigDecimal sumPrincipal = BigDecimal.ZERO;
        BigDecimal previousOutstanding = principal;
        for (JsonNode s : schedules) {
            BigDecimal installmentAmount = new BigDecimal(s.path("installmentAmount").asText());
            BigDecimal principalPart = new BigDecimal(s.path("principalAmount").asText());
            BigDecimal interestPart = new BigDecimal(s.path("interestAmount").asText());
            BigDecimal outstanding = new BigDecimal(s.path("outstandingPrincipal").asText());

            // principal + interest must equal the installment amount (HALF_EVEN)
            assertThat(principalPart.add(interestPart).setScale(4, RoundingMode.HALF_EVEN))
                    .isEqualByComparingTo(installmentAmount.setScale(4, RoundingMode.HALF_EVEN));
            // outstanding principal non-increasing across periods (pre-payment balance)
            assertThat(outstanding).isLessThanOrEqualTo(previousOutstanding);
            previousOutstanding = outstanding;

            sumPrincipal = sumPrincipal.add(principalPart);
        }
        // amortized principal sums back to the exact loan principal (no float drift)
        assertThat(sumPrincipal.setScale(4, RoundingMode.HALF_EVEN))
                .isEqualByComparingTo(principal);
        // final period fully amortizes: its outstanding equals its remaining principal
        JsonNode last = schedules.get(schedules.size() - 1);
        assertThat(new BigDecimal(last.path("outstandingPrincipal").asText()).setScale(4, RoundingMode.HALF_EVEN))
                .isEqualByComparingTo(new BigDecimal(last.path("principalAmount").asText()).setScale(4, RoundingMode.HALF_EVEN));

        // 4. Pay the first installment (full amount) — idempotency key required
        String firstScheduleId = schedules.get(0).path("id").asText();
        String idemKey = "idem-" + UUID.randomUUID();
        String firstInstallment = schedules.get(0).path("installmentAmount").asText();

        String payBody = mockMvc.perform(post(BASE_PATH + "/repayment-schedules/" + firstScheduleId + "/pay")
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(Map.of("amount", firstInstallment))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode paid = extractData(payBody);
        assertThat(paid.path("status").asText()).isEqualTo("FULLY_PAID");

        // loan outstanding balance must have dropped by the paid principal
        JsonNode loanAfter = extractData(mockMvc.perform(get(BASE_PATH + "/loans/" + loanId)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        BigDecimal outstandingAfter = new BigDecimal(loanAfter.path("outstandingBalance").asText());
        BigDecimal firstPrincipal = new BigDecimal(schedules.get(0).path("principalAmount").asText());
        assertThat(outstandingAfter.setScale(4, RoundingMode.HALF_EVEN))
                .isEqualByComparingTo(principal.subtract(firstPrincipal).setScale(4, RoundingMode.HALF_EVEN));

        // 5. Re-play with the same idempotency key — must not double-pay
        String replayBody = mockMvc.perform(post(BASE_PATH + "/repayment-schedules/" + firstScheduleId + "/pay")
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(Map.of("amount", firstInstallment))))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        JsonNode replayed = extractData(replayBody);
        assertThat(replayed.path("paidAmount").asText())
                .isEqualTo(paid.path("paidAmount").asText());
    }

    // ─── helpers ────────────────────────────────────────────────────

    private void seedEligibleCreditScore(UUID userId) throws Exception {
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
                .andExpect(jsonPath("$.data.score").value(org.hamcrest.Matchers.greaterThanOrEqualTo(600)))
                .andReturn();
    }

    private JsonNode extractData(String body) {
        if (body == null || body.isBlank()) return MAPPER.createObjectNode();
        try {
            return MAPPER.readTree(body).path("data");
        } catch (Exception e) {
            return MAPPER.createObjectNode();
        }
    }
}
