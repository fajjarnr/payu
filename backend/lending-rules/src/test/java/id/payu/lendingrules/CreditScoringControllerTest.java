package id.payu.lendingrules;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract tests for the credit-scoring rules endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CreditScoringControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void evaluateCreditScoreAppliesRules() throws Exception {
        mockMvc.perform(post("/api/v1/rules/credit-score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kycStatus":"APPROVED","tenureMonths":36,
                                 "successRate":0.98,"totalAmount":150000000,
                                 "totalTransactions":120}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(150));
    }

    @Test
    void evaluateCreditScoreEmptyFactScoresZero() throws Exception {
        mockMvc.perform(post("/api/v1/rules/credit-score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0));
    }

    @Test
    void evaluateCreditScoreRejectsMalformedBody() throws Exception {
        mockMvc.perform(post("/api/v1/rules/credit-score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }
}
