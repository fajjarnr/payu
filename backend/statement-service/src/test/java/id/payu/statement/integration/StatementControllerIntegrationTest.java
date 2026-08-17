package id.payu.statement.integration;

import id.payu.statement.interfaces.dto.StatementGenerationRequest;
import id.payu.statement.interfaces.dto.StatementResponse;
import id.payu.statement.adapter.persistence.entity.StatementEntity;
import id.payu.statement.adapter.persistence.repository.StatementRepository;
import id.payu.statement.domain.entity.StatementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for StatementEntity Controller.
 * Verifies POST /api/v1/statements, GET /api/v1/statements/{id},
 * GET /api/v1/statements, and GET /api/v1/statements/{id}/download endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
@Import(TestContainersConfig.class)
@DisplayName("StatementEntity Controller Integration Tests")
class StatementControllerIntegrationTest {

    private static final String BASE_PATH = "/api/v1/statements";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private StatementRepository statementRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        // Clean up repository before each test
        statementRepository.deleteAll();

        // Mock JWT decoder
        when(jwtDecoder.decode(anyString()))
            .thenReturn(TestContainersConfig.buildTestJwt(TestContainersConfig.TEST_CUSTOMER_ID));
    }

    // ─── POST /statements/generate ───────────────────────────────────

    @Nested
    @DisplayName("Generate StatementEntity")
    class GenerateStatementTests {

        @Test
        @DisplayName("Should accept statement generation request and return 202")
        void generateStatement_withValidRequest_shouldReturn202() {
            StatementGenerationRequest request = StatementGenerationRequest.builder()
                    .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                    .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .year(2026)
                    .month(2)
                    .build();

            webTestClient.post()
                    .uri(BASE_PATH + "/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isAccepted()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.customerId").isEqualTo(TestContainersConfig.TEST_CUSTOMER_ID)
                    .jsonPath("$.data.status").isEqualTo("GENERATING");
        }

        @Test
        @DisplayName("Should reject request without authentication")
        void generateStatement_withoutAuth_shouldReturn401() {
            StatementGenerationRequest request = StatementGenerationRequest.builder()
                    .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                    .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .year(2026)
                    .month(2)
                    .build();

            webTestClient.post()
                    .uri(BASE_PATH + "/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Should reject request with invalid year")
        void generateStatement_withInvalidYear_shouldReturn400() {
            StatementGenerationRequest request = StatementGenerationRequest.builder()
                    .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                    .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .year(2019) // Before 2020
                    .month(2)
                    .build();

            webTestClient.post()
                    .uri(BASE_PATH + "/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("Should reject request with invalid month")
        void generateStatement_withInvalidMonth_shouldReturn400() {
            StatementGenerationRequest request = StatementGenerationRequest.builder()
                    .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                    .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .year(2026)
                    .month(13) // Invalid month
                    .build();

            webTestClient.post()
                    .uri(BASE_PATH + "/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("Should reject request with missing customerId")
        void generateStatement_withMissingCustomerId_shouldReturn400() {
            StatementGenerationRequest request = StatementGenerationRequest.builder()
                    .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .year(2026)
                    .month(2)
                    .build();

            webTestClient.post()
                    .uri(BASE_PATH + "/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    // ─── GET /statements/{id} ────────────────────────────────────────

    @Nested
    @DisplayName("Get StatementEntity by ID")
    class GetStatementTests {

        @Test
        @DisplayName("Should return statement by ID")
        void getStatement_withExistingId_shouldReturn200() {
            // Create a statement first
            StatementEntity statement = StatementEntity.builder()
                    .id(UUID.randomUUID())
                    .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                    .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .statementPeriod(LocalDate.of(2026, 2, 1))
                    .storagePath("s3://bucket/statement-1.pdf")
                    .fileSizeBytes(1024L)
                    .openingBalance(new BigDecimal("1000000.00"))
                    .closingBalance(new BigDecimal("1200000.00"))
                    .totalCredits(new BigDecimal("500000.00"))
                    .totalDebits(new BigDecimal("300000.00"))
                    .transactionCount(25)
                    .status(StatementStatus.COMPLETED)
                    .build();

            statement = statementRepository.save(statement);

            webTestClient.get()
                    .uri(BASE_PATH + "/" + statement.getId())
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.id").isEqualTo(statement.getId().toString())
                    .jsonPath("$.data.customerId").isEqualTo(TestContainersConfig.TEST_CUSTOMER_ID)
                    .jsonPath("$.data.accountNumber").isEqualTo(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .jsonPath("$.data.status").isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("Should return 404 for non-existent statement")
        void getStatement_withNonExistentId_shouldReturn404() {
            UUID nonExistentId = UUID.randomUUID();

            webTestClient.get()
                    .uri(BASE_PATH + "/" + nonExistentId)
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void getStatement_withoutAuth_shouldReturn401() {
            webTestClient.get()
                    .uri(BASE_PATH + "/" + UUID.randomUUID())
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    // ─── GET /statements ─────────────────────────────────────────────

    @Nested
    @DisplayName("List Statements")
    class ListStatementsTests {

        @Test
        @DisplayName("Should return list of statements")
        void listStatements_shouldReturn200() {
            // Create some statements
            for (int i = 0; i < 3; i++) {
                StatementEntity statement = StatementEntity.builder()
                        .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                        .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                        .statementPeriod(LocalDate.of(2026, i + 1, 1))
                        .storagePath("s3://bucket/statement-" + i + ".pdf")
                        .openingBalance(new BigDecimal("1000000.00"))
                        .closingBalance(new BigDecimal("1100000.00"))
                        .status(StatementStatus.COMPLETED)
                        .build();
                statementRepository.save(statement);
            }

            webTestClient.get()
                    .uri(BASE_PATH)
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.content").isArray()
                    .jsonPath("$.data.totalElements").isEqualTo(3);
        }

        @Test
        @DisplayName("Should return empty list when no statements")
        void listStatements_whenEmpty_shouldReturn200() {
            webTestClient.get()
                    .uri(BASE_PATH)
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.content").isArray()
                    .jsonPath("$.data.totalElements").isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void listStatements_withoutAuth_shouldReturn401() {
            webTestClient.get()
                    .uri(BASE_PATH)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Should support pagination")
        void listStatements_withPagination_shouldReturnPagedResult() {
            // Create 5 statements
            for (int i = 0; i < 5; i++) {
                StatementEntity statement = StatementEntity.builder()
                        .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                        .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                        .statementPeriod(LocalDate.of(2026, i + 1, 1))
                        .storagePath("s3://bucket/statement-" + i + ".pdf")
                        .openingBalance(new BigDecimal("1000000.00"))
                        .closingBalance(new BigDecimal("1100000.00"))
                        .status(StatementStatus.COMPLETED)
                        .build();
                statementRepository.save(statement);
            }

            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(BASE_PATH)
                            .queryParam("page", 0)
                            .queryParam("size", 2)
                            .build())
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.content").isArray()
                    .jsonPath("$.data.content.length()").isEqualTo(2)
                    .jsonPath("$.data.totalElements").isEqualTo(5)
                    .jsonPath("$.data.totalPages").isEqualTo(3);
        }
    }

    // ─── GET /statements/latest ──────────────────────────────────────

    @Nested
    @DisplayName("Get Latest StatementEntity")
    class GetLatestStatementTests {

        @Test
        @DisplayName("Should return latest statement")
        void getLatestStatement_shouldReturn200() {
            // Create statements with different periods
            StatementEntity oldStatement = StatementEntity.builder()
                    .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                    .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .statementPeriod(LocalDate.of(2026, 1, 1))
                    .storagePath("s3://bucket/statement-jan.pdf")
                    .openingBalance(new BigDecimal("1000000.00"))
                    .closingBalance(new BigDecimal("1100000.00"))
                    .status(StatementStatus.COMPLETED)
                    .build();
            statementRepository.save(oldStatement);

            StatementEntity latestStatement = StatementEntity.builder()
                    .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                    .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .statementPeriod(LocalDate.of(2026, 2, 1))
                    .storagePath("s3://bucket/statement-feb.pdf")
                    .openingBalance(new BigDecimal("1100000.00"))
                    .closingBalance(new BigDecimal("1200000.00"))
                    .status(StatementStatus.COMPLETED)
                    .build();
            statementRepository.save(latestStatement);

            webTestClient.get()
                    .uri(BASE_PATH + "/latest")
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.id").isEqualTo(latestStatement.getId().toString());
        }

        @Test
        @DisplayName("Should return 404 when no statements exist")
        void getLatestStatement_whenEmpty_shouldReturn404() {
            webTestClient.get()
                    .uri(BASE_PATH + "/latest")
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    // ─── GET /statements/{id}/download ───────────────────────────────

    @Nested
    @DisplayName("Download StatementEntity")
    class DownloadStatementTests {

        @Test
        @DisplayName("Should return PDF for completed statement")
        void downloadStatement_withCompletedStatus_shouldReturnPdf() {
            // Create a completed statement
            StatementEntity statement = StatementEntity.builder()
                    .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                    .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .statementPeriod(LocalDate.of(2026, 2, 1))
                    .storagePath("classpath:test-statement.pdf")
                    .fileSizeBytes(1024L)
                    .openingBalance(new BigDecimal("1000000.00"))
                    .closingBalance(new BigDecimal("1200000.00"))
                    .status(StatementStatus.COMPLETED)
                    .build();
            statement = statementRepository.save(statement);

            // Note: Download may fail if PDF doesn't exist, but we verify the endpoint
            webTestClient.get()
                    .uri(BASE_PATH + "/" + statement.getId() + "/download")
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .exchange()
                    .expectStatus().is2xxSuccessful()
                    .expectHeader().contentType(MediaType.APPLICATION_PDF);
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void downloadStatement_withoutAuth_shouldReturn401() {
            webTestClient.get()
                    .uri(BASE_PATH + "/" + UUID.randomUUID() + "/download")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    // ─── POST /statements/{id}/regenerate ────────────────────────────

    @Nested
    @DisplayName("Regenerate StatementEntity (Admin Only)")
    class RegenerateStatementTests {

        @Test
        @DisplayName("Should accept regenerate request with admin role")
        void regenerateStatement_withAdminRole_shouldReturn202() {
            // Mock admin JWT
            when(jwtDecoder.decode("admin-token"))
                .thenReturn(TestContainersConfig.buildAdminJwt());

            // Create a statement
            StatementEntity statement = StatementEntity.builder()
                    .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                    .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .statementPeriod(LocalDate.of(2026, 2, 1))
                    .storagePath("s3://bucket/statement.pdf")
                    .openingBalance(new BigDecimal("1000000.00"))
                    .closingBalance(new BigDecimal("1200000.00"))
                    .status(StatementStatus.COMPLETED)
                    .build();
            statement = statementRepository.save(statement);

            webTestClient.post()
                    .uri(BASE_PATH + "/" + statement.getId() + "/regenerate")
                    .header("Authorization", TestContainersConfig.adminBearerToken())
                    .exchange()
                    .expectStatus().isAccepted()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.status").isEqualTo("GENERATING");
        }

        @Test
        @DisplayName("Should return 403 for non-admin user")
        void regenerateStatement_withUserRole_shouldReturn403() {
            StatementEntity statement = StatementEntity.builder()
                    .customerId(TestContainersConfig.TEST_CUSTOMER_ID)
                    .accountNumber(TestContainersConfig.TEST_ACCOUNT_NUMBER)
                    .statementPeriod(LocalDate.of(2026, 2, 1))
                    .storagePath("s3://bucket/statement.pdf")
                    .openingBalance(new BigDecimal("1000000.00"))
                    .closingBalance(new BigDecimal("1200000.00"))
                    .status(StatementStatus.COMPLETED)
                    .build();
            statement = statementRepository.save(statement);

            webTestClient.post()
                    .uri(BASE_PATH + "/" + statement.getId() + "/regenerate")
                    .header("Authorization", TestContainersConfig.bearerToken())
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }
}
