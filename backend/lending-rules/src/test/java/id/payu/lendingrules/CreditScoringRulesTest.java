package id.payu.lendingrules;

import id.payu.lendingrules.domain.CreditScoringFact;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behavioral tests for the Drools credit scoring decision rules
 * (credit_scoring.drl).
 */
@SpringBootTest
class CreditScoringRulesTest {

    @Autowired
    KieContainer kieContainer;

    private BigDecimal evaluate(CreditScoringFact fact) {
        KieSession session = kieContainer.newKieSession();
        try {
            session.insert(fact);
            session.fireAllRules();
            return fact.getScore();
        } finally {
            session.dispose();
        }
    }

    @Test
    void excellentProfileScoresHigh() {
        CreditScoringFact fact = new CreditScoringFact();
        fact.setKycStatus("APPROVED");
        fact.setTenureMonths(36);
        fact.setSuccessRate(new BigDecimal("0.98"));
        fact.setTotalAmount(new BigDecimal("150000000"));
        fact.setTotalTransactions(120);

        BigDecimal score = evaluate(fact);

        // 50 (KYC) + 40 (tenure 36+) + 30 (rate 98+) + 20 (amount 100M+) + 10 (txn 100+)
        assertEquals(new BigDecimal("150"), score);
    }

    @Test
    void weakProfileScoresLow() {
        CreditScoringFact fact = new CreditScoringFact();
        fact.setKycStatus("PENDING");
        fact.setTenureMonths(6);
        fact.setSuccessRate(new BigDecimal("0.85"));
        fact.setTotalAmount(new BigDecimal("5000000"));
        fact.setTotalTransactions(30);

        BigDecimal score = evaluate(fact);

        // 25 (KYC pending) + 10 (tenure 6-11) - 20 (rate < 90%)
        assertEquals(new BigDecimal("15"), score);
    }

    @Test
    void tenureBandsAreMutuallyExclusive() {
        CreditScoringFact fact = new CreditScoringFact();
        fact.setTenureMonths(30);
        fact.setSuccessRate(new BigDecimal("0.96"));
        fact.setTotalAmount(new BigDecimal("60000000"));
        fact.setTotalTransactions(60);

        BigDecimal score = evaluate(fact);

        // 30 (tenure 24-35) + 20 (rate 95-97) + 15 (amount 50-100M) + 5 (txn 51-100)
        assertEquals(new BigDecimal("70"), score);
    }

    @Test
    void missingFieldsContributeZero() {
        CreditScoringFact fact = new CreditScoringFact();
        fact.setTenureMonths(12);

        BigDecimal score = evaluate(fact);

        // Only tenure 12-23 applies: 20
        assertEquals(new BigDecimal("20"), score);
    }

    @Test
    void successRateUnder90Penalizes() {
        CreditScoringFact fact = new CreditScoringFact();
        fact.setSuccessRate(new BigDecimal("0.50"));

        BigDecimal score = evaluate(fact);

        assertTrue(score.compareTo(BigDecimal.ZERO) < 0);
        assertEquals(new BigDecimal("-20"), score);
    }
}
