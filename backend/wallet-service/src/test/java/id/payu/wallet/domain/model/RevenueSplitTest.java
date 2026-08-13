package id.payu.wallet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QAMVP-020 + ADR-0022: revenue-split amounts are money — must keep scale 4
 * and round with HALF_EVEN (never scale 2 truncation).
 */
@DisplayName("RevenueSplit money rounding")
class RevenueSplitTest {

    private RevenueSplit revenueSplit() {
        RevenueSplit split = RevenueSplit.create("partner-1", "Revenue", "test split",
                id.payu.wallet.domain.model.SplitType.PERCENTAGE, "tester");
        split.addStakeholder("acc-a", "Stakeholder A", new BigDecimal("33.3333"), null, 1);
        split.addStakeholder("acc-b", "Stakeholder B", new BigDecimal("66.6667"), null, 2);
        return split;
    }

    @Test
    @DisplayName("percentage split amounts keep scale 4 (ADR-0022)")
    void percentageSplitKeepsScaleFour() {
        RevenueSplit split = revenueSplit();

        List<CalculatedSplit> results = split.calculateSplits(new BigDecimal("9999.9999"));

        assertThat(results).hasSize(2);
        for (CalculatedSplit result : results) {
            assertThat(result.getAmount().scale())
                    .as("money amounts must keep scale 4, not truncate to 2")
                    .isEqualTo(4);
        }
    }

    @Test
    @DisplayName("HALF_EVEN half-way rounding is applied on divide")
    void halfEvenRounding() {
        RevenueSplit split = RevenueSplit.create("partner-1", "Revenue", "half-even",
                id.payu.wallet.domain.model.SplitType.PERCENTAGE, "tester");
        // 0.05% of 1000 = 0.50 exactly half-way → HALF_EVEN keeps 0.5000
        split.addStakeholder("acc-a", "A", new BigDecimal("0.05"), null, 1);
        split.addStakeholder("acc-b", "B", new BigDecimal("99.95"), null, 2);

        List<CalculatedSplit> results = split.calculateSplits(new BigDecimal("1000.0000"));

        assertThat(results.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("0.5000"));
    }
}
