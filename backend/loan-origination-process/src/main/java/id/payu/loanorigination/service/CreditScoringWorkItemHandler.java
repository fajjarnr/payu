package id.payu.loanorigination.service;

import id.payu.loanorigination.domain.CreditScoringFact;
import id.payu.shared.restclient.PayuRestClient;
import org.kie.api.runtime.process.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component("CreditScoring")
public class CreditScoringWorkItemHandler implements WorkItemHandler {

    private static final Logger log = LoggerFactory.getLogger(CreditScoringWorkItemHandler.class);

    private final PayuRestClient restClient;
    private final String lendingRulesUrl;

    public CreditScoringWorkItemHandler(PayuRestClient restClient,
                                        @Value("${payu.lending-rules.url}") String lendingRulesUrl) {
        this.restClient = restClient;
        this.lendingRulesUrl = lendingRulesUrl;
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        Object amountObj = workItem.getParameter("amount");
        Object tenureObj = workItem.getParameter("tenureMonths");

        BigDecimal amount = amountObj instanceof BigDecimal
                ? (BigDecimal) amountObj
                : new BigDecimal(String.valueOf(amountObj));
        int tenure = tenureObj instanceof Integer
                ? (Integer) tenureObj
                : Integer.parseInt(String.valueOf(tenureObj));

        log.info("CreditScoringWorkItemHandler: amount={}, tenureMonths={}", amount, tenure);

        CreditScoringFact fact = new CreditScoringFact();
        fact.setTotalAmount(amount);
        fact.setTenureMonths(tenure);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, BigDecimal>> response = restClient.post(
                    "lending-rules",
                    lendingRulesUrl + "/api/v1/rules/credit-score",
                    fact,
                    (Class<Map<String, BigDecimal>>) (Class<?>) Map.class);

            if (response.getBody() != null && response.getBody().containsKey("score")) {
                BigDecimal score = response.getBody().get("score");
                log.info("CreditScoring result: score={}", score);
                manager.completeWorkItem(workItem.getId(), Map.of("score", score));
                return;
            }
        } catch (Exception e) {
            log.error("CreditScoring call failed: {}", e.getMessage(), e);
        }

        manager.completeWorkItem(workItem.getId(), Map.of("score", BigDecimal.ZERO));
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.warn("CreditScoring aborted: workItemId={}", workItem.getId());
        manager.abortWorkItem(workItem.getId());
    }
}
