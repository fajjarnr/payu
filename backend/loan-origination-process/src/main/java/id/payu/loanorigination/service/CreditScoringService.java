package id.payu.loanorigination.service;

import id.payu.loanorigination.domain.CreditScoringFact;
import id.payu.shared.restclient.PayuRestClient;

import java.math.BigDecimal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CreditScoringService {

    private static final Logger log = LoggerFactory.getLogger(CreditScoringService.class);

    private final PayuRestClient restClient;
    private final String lendingRulesUrl;

    public CreditScoringService(PayuRestClient restClient,
                                @Value("${payu.lending-rules.url:http://lending-service:8080}") String lendingRulesUrl) {
        this.restClient = restClient;
        this.lendingRulesUrl = lendingRulesUrl;
    }

    public BigDecimal evaluate(BigDecimal amount, int tenureMonths) {
        log.info("Calling lending-service: amount={}, tenureMonths={}", amount, tenureMonths);
        var fact = new CreditScoringFact();
        fact.setTotalAmount(amount);
        fact.setTenureMonths(tenureMonths);
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, BigDecimal>> resp = restClient.post(
                    "lending-service",
                    lendingRulesUrl + "/api/v1/rules/credit-score",
                    fact,
                    (Class<Map<String, BigDecimal>>) (Class<?>) Map.class);
            if (resp != null && resp.getBody() != null) {
                Object scoreObj = resp.getBody().get("score");
                if (scoreObj instanceof BigDecimal bd) return bd;
                if (scoreObj instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
                if (scoreObj instanceof String s) return new BigDecimal(s);
            }
        } catch (Exception e) {
            log.warn("Credit scoring call failed: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }
}
