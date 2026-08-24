package id.payu.transaction.adapter.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.transaction.exception.TransactionDomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * ADR-0030: adapter maps analytics-service POST /api/v1/analytics/fraud/score
 * response ({data.fraud_score.risk_score} 0.0-100.0) to an int score.
 */
class RiskEvaluationAdapterTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private RiskEvaluationAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        adapter = new RiskEvaluationAdapter(restTemplate);
        setUrl(adapter, "http://analytics-service:8082");
    }

    @Test
    void postsFraudScoreRequestAndMapsRiskScoreToInt() {
        server.expect(requestTo("http://analytics-service:8082/api/v1/analytics/fraud/score"))
                .andExpect(jsonPath("$.user_id").value("user-1"))
                .andExpect(jsonPath("$.amount").value(250000))
                .andExpect(jsonPath("$.currency").value("IDR"))
                .andRespond(withSuccess("""
                        {"success": true,
                         "data": {"fraud_score": {"risk_score": 86.4, "risk_level": "CRITICAL"},
                                  "is_blocked": true, "requires_review": false}}
                        """, MediaType.APPLICATION_JSON));

        int score = adapter.score("user-1", new BigDecimal("250000"), "IDR");

        assertThat(score).isEqualTo(86);
        server.verify();
    }

    @Test
    void wrapsAnalyticsOutageAsRiskEvaluationUnavailable() {
        server.expect(requestTo("http://analytics-service:8082/api/v1/analytics/fraud/score"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> adapter.score("user-1", new BigDecimal("100000"), "IDR"))
                .isInstanceOf(TransactionDomainException.RiskEvaluationUnavailableException.class);
    }

    @Test
    void wrapsMalformedResponseAsRiskEvaluationUnavailable() {
        server.expect(requestTo("http://analytics-service:8082/api/v1/analytics/fraud/score"))
                .andRespond(withSuccess("{\"success\": false}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.score("user-1", new BigDecimal("100000"), "IDR"))
                .isInstanceOf(TransactionDomainException.RiskEvaluationUnavailableException.class);
    }

    @Test
    void defaultsNullCurrencyToIdr() {
        server.expect(requestTo("http://analytics-service:8082/api/v1/analytics/fraud/score"))
                .andExpect(jsonPath("$.currency").value("IDR"))
                .andRespond(withSuccess(
                        "{\"data\": {\"fraud_score\": {\"risk_score\": 15.0}}}",
                        MediaType.APPLICATION_JSON));

        assertThat(adapter.score("user-1", new BigDecimal("50000"), null)).isEqualTo(15);
    }

    private static void setUrl(RiskEvaluationAdapter target, String url) throws Exception {
        Field field = RiskEvaluationAdapter.class.getDeclaredField("analyticsServiceUrl");
        field.setAccessible(true);
        field.set(target, url);
    }
}
