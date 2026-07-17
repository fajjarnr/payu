package id.payu.backoffice.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FraudCaseTest {
    @Test void amountUsesFourDecimalHalfEvenInvariant() {
        FraudCase value = FraudCase.create("user", "ACC", UUID.randomUUID(), "TRANSFER",
                new BigDecimal("1.23455"), "TYPE", null, "description", "{}");
        assertEquals(new BigDecimal("1.2346"), value.getAmount());
        assertEquals(RiskLevel.MEDIUM, value.getRiskLevel());
        assertEquals(FraudCaseStatus.OPEN, value.getStatus());
    }

    @Test void negativeAmountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FraudCase.create("user", "ACC", null,
                "TRANSFER", new BigDecimal("-0.01"), "TYPE", RiskLevel.HIGH, null, null));
    }

    @Test void assignAndResolveAreDomainTransitions() {
        FraudCase value = FraudCase.create("user", "ACC", null, "TRANSFER",
                BigDecimal.ONE, "TYPE", RiskLevel.HIGH, null, null);
        value.assignTo("agent");
        value.resolve(FraudCaseStatus.RESOLVED, "confirmed", "resolver");
        assertEquals(FraudCaseStatus.RESOLVED, value.getStatus());
        assertEquals("resolver", value.getResolvedBy());
        assertNotNull(value.getResolvedAt());
    }
}
