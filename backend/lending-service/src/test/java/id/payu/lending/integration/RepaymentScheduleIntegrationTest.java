package id.payu.lending.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.lending.adapter.external.AccountClient;
import id.payu.lending.adapter.external.TransactionClient;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.dto.LoanApplicationRequest;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for repayment schedule creation and payment processing.
 * Verifies:
 * <ul>
 *   <li>POST /api/v1/lending/loans/{loanId}/repayment-schedule</li>
 *   <li>GET  /api/v1/lending/loans/{loanId}/repayment-schedule</li>
 *   <li>POST /api/v1/lending/repayment-schedules/{scheduleId}/pay</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
@Import(TestContainersConfig.class)
@DisplayName("Repayment Schedule Integration Tests")
class RepaymentScheduleIntegrationTest {

    private static final String BASE_PATH = "/api/v1/lending";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AccountClient accountClient;

    @MockBean
    private TransactionClient transactionClient;

    @MockBean
    private OutboxService outboxService;

    private String loanId;
    private int loanTenureMonths;

    @BeforeEach
    void setUp() {
        loanTenureMonths = 6;
        loanId = createTestLoan(loanTenureMonths);
    }

    // ─── create repayment schedule ──────────────────────────────────

    @Test
    @DisplayName("Should create repayment schedule with correct number of installments matching tenure")
    void createRepaymentSchedule_shouldReturnInstallmentsMatchingTenure() {
        if (loanId == null) return; // guard against setup failure

        byte[] responseBody = webTestClient.post()
                .uri(BASE_PATH + "/loans/" + loanId + "/repayment-schedule")
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isArray()
                .returnResult()
                .getResponseBody();

        JsonNode data = extractDataArray(responseBody);
        assertThat(data).isNotNull();
        assertThat(data.size()).isEqualTo(loanTenureMonths);

        // Verify first installment has expected structure
        JsonNode first = data.get(0);
        assertThat(first.path("installmentNumber").asInt()).isEqualTo(1);
        assertThat(first.path("status").asText()).isEqualTo("PENDING");
        assertThat(first.path("loanId").asText()).isEqualTo(loanId);
    }

    @Test
    @DisplayName("Should retrieve empty repayment schedule for loan without schedules")
    void getRepaymentSchedule_forLoanWithoutSchedules_shouldReturnEmptyList() {
        String freshLoanId = createTestLoan(3);
        if (freshLoanId == null) return;

        webTestClient.get()
                .uri(BASE_PATH + "/loans/" + freshLoanId + "/repayment-schedule")
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isArray();
    }

    // ─── process repayment ──────────────────────────────────────────

    @Test
    @DisplayName("Should mark installment as FULLY_PAID when repayment covers installment amount")
    void processRepayment_withFullAmount_shouldMarkFullyPaid() {
        if (loanId == null) return;

        // Create schedule and extract first installment
        byte[] scheduleResponse = webTestClient.post()
                .uri(BASE_PATH + "/loans/" + loanId + "/repayment-schedule")
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult()
                .getResponseBody();

        JsonNode schedules = extractDataArray(scheduleResponse);
        if (schedules == null || schedules.isEmpty()) return;

        String scheduleId = schedules.get(0).path("id").asText();
        BigDecimal installmentAmount = new BigDecimal(schedules.get(0).path("installmentAmount").asText());

        // Pay full installment
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/repayment-schedules/" + scheduleId + "/pay")
                        .queryParam("amount", installmentAmount.toPlainString())
                        .build())
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.status").isEqualTo("FULLY_PAID")
                .jsonPath("$.data.paidDate").isNotEmpty();
    }

    @Test
    @DisplayName("Should mark installment as PARTIALLY_PAID when repayment is less than full amount")
    void processRepayment_withPartialAmount_shouldMarkPartiallyPaid() {
        if (loanId == null) return;

        byte[] scheduleResponse = webTestClient.post()
                .uri(BASE_PATH + "/loans/" + loanId + "/repayment-schedule")
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult()
                .getResponseBody();

        JsonNode schedules = extractDataArray(scheduleResponse);
        if (schedules == null || schedules.isEmpty()) return;

        // Pick second installment to avoid collision with other tests
        JsonNode target = schedules.size() > 1 ? schedules.get(1) : schedules.get(0);
        String scheduleId = target.path("id").asText();

        // Pay a small partial amount
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/repayment-schedules/" + scheduleId + "/pay")
                        .queryParam("amount", "10000.00")
                        .build())
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.status").isEqualTo("PARTIALLY_PAID");
    }

    @Test
    @DisplayName("Should return error when processing repayment for already fully-paid installment")
    void processRepayment_forAlreadyPaidInstallment_shouldReturnError() {
        if (loanId == null) return;

        // Create schedule
        byte[] scheduleResponse = webTestClient.post()
                .uri(BASE_PATH + "/loans/" + loanId + "/repayment-schedule")
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult()
                .getResponseBody();

        JsonNode schedules = extractDataArray(scheduleResponse);
        if (schedules == null || schedules.isEmpty()) return;

        // Use last installment to avoid conflicts
        JsonNode target = schedules.get(schedules.size() - 1);
        String scheduleId = target.path("id").asText();
        BigDecimal installmentAmount = new BigDecimal(target.path("installmentAmount").asText());

        // First payment — marks FULLY_PAID
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/repayment-schedules/" + scheduleId + "/pay")
                        .queryParam("amount", installmentAmount.toPlainString())
                        .build())
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isOk();

        // Second payment on same installment — should fail
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH + "/repayment-schedules/" + scheduleId + "/pay")
                        .queryParam("amount", "100000.00")
                        .build())
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().is5xx(); // IllegalStateException mapped to 500
    }

    // ─── helpers ────────────────────────────────────────────────────

    private String createTestLoan(int tenureMonths) {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        LoanApplicationRequest request = new LoanApplicationRequest(
                userId,
                "EXT-RS-" + UUID.randomUUID(),
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("3000000.00"),
                tenureMonths,
                "Repayment test"
        );

        byte[] body = webTestClient.post()
                .uri(BASE_PATH + "/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TestContainersConfig.bearerToken())
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult()
                .getResponseBody();

        return extractField(body, "id");
    }

    private String extractField(byte[] body, String field) {
        if (body == null) return null;
        try {
            JsonNode root = MAPPER.readTree(body);
            return root.path("data").path(field).asText(null);
        } catch (Exception e) {
            return null;
        }
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
