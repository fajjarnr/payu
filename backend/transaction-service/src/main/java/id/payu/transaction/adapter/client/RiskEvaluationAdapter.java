package id.payu.transaction.adapter.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.transaction.domain.port.out.RiskEvaluationPort;
import id.payu.transaction.exception.TransactionDomainException.RiskEvaluationUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ADR-0030: adapter calling analytics-service fraud engine
 * ({@code POST /api/v1/analytics/fraud/score}) and mapping
 * {@code data.fraud_score.risk_score} (0.0–100.0) to an int score.
 *
 * <p>Latency budget &lt; 25ms; circuit breaker (no retry — a stale score is worse
 * than no score). Any outage/malformed payload surfaces as
 * {@link RiskEvaluationUnavailableException}; the caller applies the ADR-0030
 * fail-closed policy.
 */
@Slf4j
@Component
public class RiskEvaluationAdapter implements RiskEvaluationPort {

    @Value("${services.analytics.url:http://localhost:8082}")
    private String analyticsServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RiskEvaluationAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @CircuitBreaker(name = "analyticsService", fallbackMethod = "scoreFallback")
    public int score(String userId, BigDecimal amount, String currency) {
        try {
            String url = analyticsServiceUrl + "/api/v1/analytics/fraud/score";

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("transaction_id", UUID.randomUUID().toString());
            request.put("user_id", userId);
            request.put("amount", amount);
            request.put("currency", currency != null ? currency : "IDR");
            request.put("transaction_type", "TRANSFER");

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return extractRiskScore(response.getBody());
        } catch (RiskEvaluationUnavailableException e) {
            throw e;
        } catch (Exception e) {
            // ponytail: RestTemplate throws HttpServerErrorException on 500;
            // CircuitBreaker fallback only works via AOP proxy (not in unit test),
            // so wrap here as well to guarantee fail-closed policy.
            log.warn("Analytics call failed for user {}: {}", userId, e.getMessage());
            throw new RiskEvaluationUnavailableException(e);
        }
    }

    private int extractRiskScore(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode riskScore = root.path("data").path("fraud_score").path("risk_score");
            if (riskScore.isMissingNode() || !riskScore.isNumber()) {
                throw new RiskEvaluationUnavailableException(
                        new IllegalStateException("Malformed fraud score response"));
            }
            return Math.round(riskScore.floatValue());
        } catch (RiskEvaluationUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new RiskEvaluationUnavailableException(e);
        }
    }

    /**
     * Circuit-breaker fallback: rethrow as the port's unavailable exception so the
     * caller enforces the fail-closed policy (ADR-0030: fail-safe to STEP_UP).
     */
    private int scoreFallback(String userId, BigDecimal amount, String currency, Exception e) {
        log.warn("Analytics fraud scoring failed for user {}: {}", userId, e.getMessage());
        throw e instanceof RiskEvaluationUnavailableException ree
                ? ree
                : new RiskEvaluationUnavailableException(e);
    }
}
