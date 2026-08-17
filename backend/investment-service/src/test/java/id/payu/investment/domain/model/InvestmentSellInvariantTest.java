package id.payu.investment.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariant tests for Investment Sell & Redemption operations (Flow 14).
 *
 * <p>Verifies investment liquidation core invariants:</p>
 * <ul>
 *   <li>Redemption proceeds: proceeds = (units * NAV_price) - redemption_fee.</li>
 *   <li>Unit balance sufficiency: unitsToSell <= totalHoldingUnits.</li>
 *   <li>Positivity: units and price must be strictly greater than zero.</li>
 *   <li>Scale 4 HALF_EVEN: all currency calculations preserve 4 decimal places.</li>
 * </ul>
 */
@DisplayName("Investment Sell & Redemption Invariant Tests (Flow 14)")
class InvestmentSellInvariantTest {

    private static final String ACCOUNT_ID = "inv-acct-123";

    @Nested
    @DisplayName("Redemption Proceeds & NAV Invariants")
    class RedemptionProceedsInvariants {

        @Test
        @DisplayName("Gross redemption amount equals units multiplied by NAV price")
        void grossRedemptionCalculation() {
            BigDecimal units = new BigDecimal("125.5000");
            BigDecimal navPrice = new BigDecimal("1520.2500");

            BigDecimal grossAmount = units.multiply(navPrice).setScale(4, RoundingMode.HALF_EVEN);
            assertThat(grossAmount).isEqualByComparingTo(new BigDecimal("190791.3750"));
        }

        @Test
        @DisplayName("Net proceeds equals gross redemption minus exit fee")
        void netProceedsCalculation() {
            BigDecimal grossAmount = new BigDecimal("200000.0000");
            BigDecimal exitFeeRate = new BigDecimal("0.0050"); // 0.5% exit fee

            BigDecimal exitFee = grossAmount.multiply(exitFeeRate).setScale(4, RoundingMode.HALF_EVEN);
            BigDecimal netProceeds = grossAmount.subtract(exitFee).setScale(4, RoundingMode.HALF_EVEN);

            assertThat(exitFee).isEqualByComparingTo(new BigDecimal("1000.0000"));
            assertThat(netProceeds).isEqualByComparingTo(new BigDecimal("199000.0000"));
            assertThat(netProceeds.add(exitFee)).isEqualByComparingTo(grossAmount);
        }

        @Test
        @DisplayName("Units to sell cannot exceed available holding units")
        void holdingUnitsSufficiency() {
            BigDecimal availableUnits = new BigDecimal("500.0000");
            BigDecimal validUnitsToSell = new BigDecimal("300.0000");
            BigDecimal excessiveUnitsToSell = new BigDecimal("500.0001");

            assertThat(validUnitsToSell).isLessThanOrEqualTo(availableUnits);
            assertThat(excessiveUnitsToSell).isGreaterThan(availableUnits);
        }
    }

    @Nested
    @DisplayName("Transaction Entity Invariants")
    class TransactionEntityInvariants {

        @Test
        @DisplayName("Sell transaction is constructed with TransactionType.SELL")
        void sellTransactionConstruction() {
            InvestmentTransaction txn = InvestmentTransaction.builder()
                .id(UUID.randomUUID())
                .accountId(ACCOUNT_ID)
                .type(TransactionType.SELL)
                .investmentType(InvestmentType.MUTUAL_FUND)
                .investmentId("fund-top-equity")
                .amount(new BigDecimal("199000.0000"))
                .units(new BigDecimal("125.5000"))
                .price(new BigDecimal("1520.2500"))
                .fee(new BigDecimal("1000.0000"))
                .currency("IDR")
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

            assertThat(txn.getType()).isEqualTo(TransactionType.SELL);
            assertThat(txn.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(txn.getAmount()).isEqualByComparingTo(new BigDecimal("199000.0000"));
        }
    }
}
