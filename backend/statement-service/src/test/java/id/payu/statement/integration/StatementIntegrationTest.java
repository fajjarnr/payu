package id.payu.statement.integration;

import id.payu.statement.application.service.dto.StatementGenerationRequest;
import id.payu.statement.domain.entity.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Statement generation and management APIs.
 * Verifies POST /api/v1/statements/generate, GET /api/v1/statements/{id},
 * GET /api/v1/statements, and GET /api/v1/statements/latest endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
@Import(TestContainersConfig.class)
@DisplayName("Statement Service Integration Tests")
class StatementIntegrationTest {

    private static final String BASE_PATH = "/api/v1/statements";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Should generate statement and return 202 ACCEPTED")
    void generateStatement_withValidRequest_shouldReturn202() {
        // Arrange
        when(jwtDecoder.decode(anyString())).thenReturn(
            TestContainersConfig.buildTestJwt(TestContainersConfig.TEST_CUSTOMER_ID)
        );

        StatementGenerationRequest request = new StatementGenerationRequest();
        request.setStatementPeriod(LocalDate.of(2026, 1, 1));

        // Act & Assert
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
    @DisplayName("Should reject statement generation without authentication")
    void generateStatement_withoutAuth_shouldReturn401() {
        StatementGenerationRequest request = new StatementGenerationRequest();
        request.setStatementPeriod(LocalDate.of(2026, 1, 1));

        webTestClient.post()
                .uri(BASE_PATH + "/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 400 for invalid statement period")
    void generateStatement_withInvalidPeriod_shouldReturn400() {
        when(jwtDecoder.decode(anyString())).thenReturn(
            TestContainersConfig.buildTestJwt(TestContainersConfig.TEST_CUSTOMER_ID)
        );

        // Missing statement period - should fail validation
        StatementGenerationRequest request = new StatementGenerationRequest();

        webTestClient.post()
                .uri(BASE_PATH + "/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", TestContainersConfig.bearerToken())
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should list statements with pagination")
    void listStatements_withAuth_shouldReturn200() {
        when(jwtDecoder.decode(anyString())).thenReturn(
            TestContainersConfig.buildTestJwt(TestContainersConfig.TEST_CUSTOMER_ID)
        );

        webTestClient.get()
                .uri(BASE_PATH + "?page=0&size=10")
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").exists()
                .jsonPath("$.meta").exists();
    }

    @Test
    @DisplayName("Should return 401 when listing statements without auth")
    void listStatements_withoutAuth_shouldReturn401() {
        webTestClient.get()
                .uri(BASE_PATH)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 404 for non-existent statement")
    void getStatement_withNonExistentId_shouldReturn404() {
        when(jwtDecoder.decode(anyString())).thenReturn(
            TestContainersConfig.buildTestJwt(TestContainersConfig.TEST_CUSTOMER_ID)
        );

        UUID nonExistentId = UUID.randomUUID();

        webTestClient.get()
                .uri(BASE_PATH + "/" + nonExistentId)
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should return 404 when latest statement not found")
    void getLatestStatement_whenNoStatements_shouldReturn404() {
        when(jwtDecoder.decode(anyString())).thenReturn(
            TestContainersConfig.buildTestJwt(TestContainersConfig.TEST_CUSTOMER_ID)
        );

        webTestClient.get()
                .uri(BASE_PATH + "/latest")
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should return 403 when non-admin tries to regenerate statement")
    void regenerateStatement_withNonAdminUser_shouldReturn403() {
        when(jwtDecoder.decode(anyString())).thenReturn(
            TestContainersConfig.buildTestJwt(TestContainersConfig.TEST_CUSTOMER_ID)
        );

        UUID statementId = UUID.randomUUID();

        webTestClient.post()
                .uri(BASE_PATH + "/" + statementId + "/regenerate")
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should accept regenerate request from admin user")
    void regenerateStatement_withAdminUser_shouldReturn202() {
        when(jwtDecoder.decode(anyString())).thenReturn(
            TestContainersConfig.buildAdminJwt()
        );

        UUID statementId = UUID.randomUUID();

        webTestClient.post()
                .uri(BASE_PATH + "/" + statementId + "/regenerate")
                .header("Authorization", TestContainersConfig.adminBearerToken())
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.status").isEqualTo("GENERATING");
    }

    @Test
    @DisplayName("Should return 404 when downloading non-existent statement")
    void downloadStatement_withNonExistentId_shouldReturn404() {
        when(jwtDecoder.decode(anyString())).thenReturn(
            TestContainersConfig.buildTestJwt(TestContainersConfig.TEST_CUSTOMER_ID)
        );

        UUID nonExistentId = UUID.randomUUID();

        webTestClient.get()
                .uri(BASE_PATH + "/" + nonExistentId + "/download")
                .header("Authorization", TestContainersConfig.bearerToken())
                .exchange()
                .expectStatus().isNotFound();
    }
}
