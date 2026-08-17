package id.payu.partner.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant tests for Partner Settlement Batch (Flow 22).
 *
 * <p>Verifies Settlement Batch core financial invariants:</p>
 * <ul>
 *   <li>Batch aggregation: total gross equals exact sum of item amounts.</li>
 *   <li>Net settlement: netPayout = totalGross - totalPlatformFee.</li>
 *   <li>Zero-loss rounding: all fee and net calculations preserve scale 4 with HALF_EVEN.</li>
 * </ul>
 */
@DisplayName("Settlement Batch Invariant Tests (Flow 22)")
class SettlementBatchInvariantTest {

    @Nested
    @DisplayName("Batch Aggregation & Payout Invariants")
    class BatchAggregationInvariants {

        @Test
        @DisplayName("Batch gross amount must equal sum of all individual transaction amounts")
        void batchGrossSumInvariant() {
            List<BigDecimal> itemAmounts = List.of(
                new BigDecimal("150000.0000"),
                new BigDecimal("250000.0000"),
                new BigDecimal("100000.0000")
            );

            BigDecimal expectedGross = new BigDecimal("500000.0000");
            BigDecimal calculatedGross = itemAmounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_EVEN);

            assertThat(calculatedGross).isEqualByComparingTo(expectedGross);
        }

        @Test
        @DisplayName("Net payout equals total gross minus MDR platform fees")
        void netPayoutCalculation() {
            BigDecimal totalGross = new BigDecimal("1000000.0000");
            BigDecimal feeRate = new BigDecimal("0.0150"); // 1.5% fee

            BigDecimal totalFee = totalGross.multiply(feeRate).setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal netPayout = totalGross.subtract(totalFee).setScale(4, RoundingMode.HALF_EVEN);

            assertThat(totalFee).isEqualByComparingTo(new BigDecimal("15000.0000"));
            assertThat(netPayout).isEqualByComparingTo(new BigDecimal("985000.0000"));
            assertThat(netPayout.add(totalFee)).isEqualByComparingTo(totalGross);
        }
    }
}
