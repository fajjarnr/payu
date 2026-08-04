package id.payu.lending.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.lending.domain.model.CreditScore;
import id.payu.lending.domain.model.Loan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LendingMoneySerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesMoneyAsDecimalStringWithoutChangingNonMoneyDecimals() throws Exception {
        Loan loan = new Loan();
        loan.setPrincipalAmount(new BigDecimal("9007199254740993.1234"));

        CreditScore score = new CreditScore();
        score.setScore(new BigDecimal("750"));

        assertThat(mapper.readTree(mapper.writeValueAsString(loan)).get("principalAmount").asText())
                .isEqualTo("9007199254740993.1234");
        assertThat(mapper.readTree(mapper.writeValueAsString(score)).get("score").isNumber())
                .isTrue();
    }
}
