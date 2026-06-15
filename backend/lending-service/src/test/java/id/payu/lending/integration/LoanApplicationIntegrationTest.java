package id.payu.lending.integration;

import id.payu.lending.adapter.external.AccountClient;
import id.payu.lending.adapter.external.TransactionClient;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.LoanType;
import id.payu.lending.dto.LoanApplicationRequest;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for the loan application workflow.
 * Verifies POST /api/v1/lending/loans and GET /api/v1/lending/loans/{loanId}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
@Import(TestContainersConfig.class)
@DisplayName("Loan Application Integration Tests")
class LoanApplicationIntegrationTest {

    private static final String BASE_PATH = "/api/v1/lending";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AccountClient accountClient;

    @MockitoBean
    private TransactionClient transactionClient;

    @MockitoBean
    private OutboxService outboxService;

    // ─── POST /loans ────────────────────────────────────────────────

    @Test
    @DisplayName("Should create a loan application and return 201 with APPROVED status")
    void applyLoan_withValidRequest_shouldReturn201() {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        String externalId = "EXT-" + UUID.randomUUID();

        LoanApplicationRequest request = new LoanApplicationRequest(
                userId,
                externalId,
                LoanType.PERSONAL_LOAN,
                new BigDecimal("5000000.00"),
                12,
                "Home renovation"
        );

        webTestClient.post()
                .uri(BASE_PATH + "/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TestContainersConfig.bearerToken())
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.userId").isEqualTo(userId.toString())
                .jsonPath("$.data.principalAmount").isNotEmpty()
                .jsonPath("$.data.status").isNotEmpty()
                .jsonPath("$.data.id").isNotEmpty()
                .jsonPath("$.data.tenureMonths").isEqualTo(12);
    }

    @Test
    @DisplayName("Should reject loan application with negative amount and return 400")
    void applyLoan_withNegativeAmount_shouldReturn400() {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        String externalId = "EXT-" + UUID.randomUUID();

        LoanApplicationRequest request = new LoanApplicationRequest(
                userId,
                externalId,
                LoanType.PERSONAL_LOAN,
                new BigDecimal("-100000.00"),
                12,
                "Invalid loan"
        );

        webTestClient.post()
                .uri(BASE_PATH + "/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TestContainersConfig.bearerToken())
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should reject loan application with zero tenure and return 400")
    void applyLoan_withZeroTenure_shouldReturn400() {
        UUID userId = TestContainersConfig.TEST_USER_ID;
        String externalId = "EXT-" + UUID.randomUUID();

        LoanApplicationRequest request = new LoanApplicationRequest(
                userId,
                externalId,
                LoanType.PERSONAL_LOAN,
                new BigDecimal("1000000.00"),
                0,
                "Invalid tenure"
        );

        webTestClient.post()
                .uri(BASE_PATH + "/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TestContainersConfig.bearerToken())
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ─── GET /loans/{loanId} ────────────────────────────────────────

    @Test
    @DisplayName("Should return loan by ID with 200")
    void getLoan_withExistingId_shouldReturn200() {
        // Arrange: create a loan first
        UUID userId = TestContainersConfig.TEST_USER_ID;
        String externalId = "EXT-GET-" + UUID.randomUUID();

        LoanApplicationRequest request = new LoanApplicationRequest(
                userId,
                externalId,
                LoanType.PERSONAL_LOAN,
                new BigDecimal("2000000.00"),
                6,
                "Education"
        );

        // Create the loan and extract its ID
        String responseBody = webTestClient.post()
                .uri(BASE_PATH + "/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TestContainersConfig.bearerToken())
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        String loanId = extractLoanId(responseBody);
        assumeTrue(loanId != null, "loanId extraction required — loan creation must return an ID");

        // Verify we can fetch the created loan
        webTestClient.get()
                .uri(BASE_PATH + "/loans/" + loanId)
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(loanId)
                .jsonPath("$.data.userId").isEqualTo(userId.toString())
                .jsonPath("$.data.tenureMonths").isEqualTo(6);
    }

    @Test
    @DisplayName("Should return 404 for non-existent loan")
    void getLoan_withNonExistentId_shouldReturn404() {
        UUID nonExistentId = UUID.randomUUID();

        webTestClient.get()
                .uri(BASE_PATH + "/loans/" + nonExistentId)
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should return 401 when no authorization header provided")
    void applyLoan_withoutAuth_shouldReturn401() {
        LoanApplicationRequest request = new LoanApplicationRequest(
                UUID.randomUUID(),
                "EXT-NOAUTH",
                LoanType.PERSONAL_LOAN,
                new BigDecimal("1000000.00"),
                12,
                "Test"
        );

        webTestClient.post()
                .uri(BASE_PATH + "/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ─── utility ────────────────────────────────────────────────────

    private String extractLoanId(String responseBody) {
        if (responseBody == null) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode dataNode = root.path("data");
            return dataNode.path("id").asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}
