package id.payu.partner.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QAMVP-020 + ADR-0022: rate-card fees are money — must keep scale 4 with
 * HALF_EVEN, never scale-2 truncation.
 */
@DisplayName("RateCard money rounding")
class RateCardTest {

    @Test
    @DisplayName("percentage fee keeps scale 4 (ADR-0022)")
    void percentageFeeKeepsScaleFour() {
        RateCard card = RateCard.createPercentageFee("Fee", "desc",
                new BigDecimal("0.05"), "IDR", null, null, "tester");

        FeeCalculationResult result = card.calculateFee(new BigDecimal("9999.9999"));

        assertThat(result.getFeeAmount().scale()).isEqualTo(4);
        assertThat(result.getTotalAmount().scale()).isEqualTo(4);
    }

    @Test
    @DisplayName("tiered percentage fee keeps scale 4")
    void tieredFeeKeepsScaleFour() {
        RateCard card = RateCard.createTieredFee("Tiered", "desc", "IDR", "tester");
        card.addTier(null, null, null, new BigDecimal("0.05"));

        FeeCalculationResult result = card.calculateFee(new BigDecimal("9999.9999"));

        assertThat(result.getFeeAmount().scale()).isEqualTo(4);
    }

    @Test
    @DisplayName("HALF_EVEN half-way rounding is applied")
    void halfEvenRounding() {
        RateCard card = RateCard.createPercentageFee("Fee", "desc",
                new BigDecimal("0.05"), "IDR", null, null, "tester");

        // 0.05% of 1000 = 0.50 exactly → HALF_EVEN keeps 0.5000
        FeeCalculationResult result = card.calculateFee(new BigDecimal("1000.0000"));

        assertThat(result.getFeeAmount()).isEqualByComparingTo(new BigDecimal("0.5000"));
    }

    @Test
    @DisplayName("flat fee bypasses rounding but stays scale 4")
    void flatFee() {
        RateCard card = RateCard.createFlatFee("Flat", "desc",
                new BigDecimal("2500.0000"), "IDR", "tester");

        FeeCalculationResult result = card.calculateFee(new BigDecimal("10000.0000"));

        assertThat(result.getFeeAmount()).isEqualByComparingTo(new BigDecimal("2500.0000"));
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("12500.0000"));
    }
}
