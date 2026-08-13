package id.payu.lending.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.lending.adapter.external.TransactionClient;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for credit score calculation and retrieval
 * under the current API contract (authenticated-user scoping).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Import(TestContainersConfig.class)
@DisplayName("Credit Score Integration Tests")
class CreditScoreIntegrationTest {

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

    @Test
    @DisplayName("Should calculate credit score and return score with risk category")
    void calculateCreditScore_shouldReturnScoreWithRiskCategory() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/credit-score/calculate")
                        .param("userId", TestContainersConfig.TEST_USER_ID.toString())
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").isNotEmpty())
                .andExpect(jsonPath("$.data.riskCategory").isNotEmpty());
    }

    @Test
    @DisplayName("Should return same score on recalculation for same user (idempotent persistence)")
    void calculateCreditScore_recalculation_shouldKeepSameScore() throws Exception {
        String first = mockMvc.perform(post(BASE_PATH + "/credit-score/calculate")
                        .param("userId", TestContainersConfig.TEST_USER_ID.toString())
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post(BASE_PATH + "/credit-score/calculate")
                        .param("userId", TestContainersConfig.TEST_USER_ID.toString())
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String firstScore = extractField(first, "score");
        String secondScore = extractField(second, "score");
        assertThat(firstScore).isNotBlank();
        assertThat(secondScore).isNotBlank();
        assertThat(secondScore).isEqualTo(firstScore);
    }

    @Test
    @DisplayName("Should return 200 with credit score for user after calculation")
    void getCreditScore_afterCalculation_shouldReturn200() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/credit-score/calculate")
                        .param("userId", TestContainersConfig.TEST_USER_ID.toString())
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE_PATH + "/credit-score/" + TestContainersConfig.TEST_USER_ID)
                        .header("Authorization", TestContainersConfig.bearerToken())
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(TestContainersConfig.TEST_USER_ID.toString()));
    }

    private String extractField(String body, String field) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode node = MAPPER.readTree(body).path("data").path(field);
            return node.asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}
