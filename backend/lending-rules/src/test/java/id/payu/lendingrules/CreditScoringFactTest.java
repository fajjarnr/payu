package id.payu.lendingrules;

import id.payu.lendingrules.domain.CreditScoringFact;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreditScoringFactTest {

    @Test
    void scoreDefaultsToZero() {
        CreditScoringFact fact = new CreditScoringFact();
        assertEquals(BigDecimal.ZERO, fact.getScore());
    }

    @Test
    void gettersAndSettersRoundTrip() {
        CreditScoringFact fact = new CreditScoringFact();
        fact.setKycStatus("APPROVED");
        fact.setTenureMonths(24);
        fact.setSuccessRate(new BigDecimal("0.95"));
        fact.setTotalAmount(new BigDecimal("100000000"));
        fact.setTotalTransactions(50);
        fact.setScore(new BigDecimal("60"));

        assertEquals("APPROVED", fact.getKycStatus());
        assertEquals(24, fact.getTenureMonths());
        assertEquals(new BigDecimal("0.95"), fact.getSuccessRate());
        assertEquals(new BigDecimal("100000000"), fact.getTotalAmount());
        assertEquals(50, fact.getTotalTransactions());
        assertEquals(new BigDecimal("60"), fact.getScore());
    }

    @Test
    void unsetSuccessRateIsNull() {
        CreditScoringFact fact = new CreditScoringFact();
        assertNull(fact.getSuccessRate());
    }

    @Test
    void addScoreAccumulates() {
        CreditScoringFact fact = new CreditScoringFact();
        fact.addScore(new BigDecimal("50"));
        fact.addScore(new BigDecimal("40"));
        assertEquals(new BigDecimal("90"), fact.getScore());
    }

    @Test
    void subtractScoreDecreases() {
        CreditScoringFact fact = new CreditScoringFact();
        fact.setScore(new BigDecimal("80"));
        fact.subtractScore(new BigDecimal("20"));
        assertEquals(new BigDecimal("60"), fact.getScore());
    }
}
