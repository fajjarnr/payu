package id.payu.lendingrules.adapter.web;

import id.payu.lendingrules.domain.CreditScoringFact;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rules")
public class CreditScoringController {

    private final KieContainer kieContainer;

    public CreditScoringController(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    @PostMapping("/credit-score")
    public Map<String, BigDecimal> evaluateCreditScore(@RequestBody CreditScoringFact fact) {
        KieSession session = kieContainer.newKieSession();
        try {
            session.insert(fact);
            session.fireAllRules();
            return Map.of("score", fact.getScore() != null ? fact.getScore() : BigDecimal.ZERO);
        } finally {
            session.dispose();
        }
    }
}
