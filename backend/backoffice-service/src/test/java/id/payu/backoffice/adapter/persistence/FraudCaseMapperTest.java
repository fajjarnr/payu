package id.payu.backoffice.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import id.payu.backoffice.domain.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FraudCaseMapperTest {
    @Test void mapperRoundTripPreservesMoney() {
        FraudCaseMapper mapper = new FraudCaseMapper();
        FraudCase original = FraudCase.create("user", "ACC", null, "TRANSFER",
                new BigDecimal("10.1250"), "TYPE", RiskLevel.HIGH, "desc", "{}");
        FraudCase restored = mapper.toDomain(mapper.toEntity(original));
        assertEquals(new BigDecimal("10.1250"), restored.getAmount());
        assertEquals(FraudCaseStatus.OPEN, restored.getStatus());
    }
}
