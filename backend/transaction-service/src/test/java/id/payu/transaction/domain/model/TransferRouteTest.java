package id.payu.transaction.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TransferRoute Value Object.
 *
 * <p>P0 Critical Tests - These tests verify route eligibility calculation
 * and fee computation that must be correct for smart routing decisions.</p>
 *
 * <p>Test Categories:</p>
 * <ul>
 *   <li>Factory Methods - for BI_FAST, RTGS, SKN</li>
 *   <li>Eligibility - isEligibleFor() with amount checks</li>
 *   <li>Fee Calculation - calculateFee()</li>
 *   <li>Comparison - compareTo() for sorting</li>
 *   <li>Edge Cases - boundary amounts, null checks</li>
 * </ul>
 *
 * @see TransferRoute
 */
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("TransferRoute Value Object Tests")
class TransferRouteTest {

    // ==================== FACTORY METHOD TESTS ====================

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodsTests {

        @Test
        @DisplayName("Should create BI-FAST route with correct defaults")
        void shouldCreateBiFastRouteWithCorrectDefaults() {
            TransferRoute route = TransferRoute.biFast();

            assertThat(route.getMethod()).isEqualTo(TransferMethod.BI_FAST);
            assertThat(route.getFee()).isEqualTo(Money.idr("2500"));
            assertThat(route.getEstimatedTime()).isEqualTo(Duration.ofSeconds(30));
            assertThat(route.getMinAmount()).isEqualTo(Money.idr("1"));
            assertThat(route.getMaxAmount()).isEqualTo(Money.idr("50000000"));
        }

        @Test
        @DisplayName("Should create RTGS route with correct defaults")
        void shouldCreateRtgsRouteWithCorrectDefaults() {
            TransferRoute route = TransferRoute.rtgs();

            assertThat(route.getMethod()).isEqualTo(TransferMethod.RTGS);
            assertThat(route.getFee()).isEqualTo(Money.idr("25000"));
            assertThat(route.getEstimatedTime()).isEqualTo(Duration.ofMinutes(5));
            assertThat(route.getMinAmount()).isEqualTo(Money.idr("100000000"));
            assertThat(route.getMaxAmount()).isEqualTo(Money.idr("10000000000"));
        }

        @Test
        @DisplayName("Should create SKN route with correct defaults")
        void shouldCreateSknRouteWithCorrectDefaults() {
            TransferRoute route = TransferRoute.skn();

            assertThat(route.getMethod()).isEqualTo(TransferMethod.SKN);
            assertThat(route.getFee()).isEqualTo(Money.idr("5000"));
            assertThat(route.getEstimatedTime()).isEqualTo(Duration.ofHours(4));
            assertThat(route.getMinAmount()).isEqualTo(Money.idr("1"));
            assertThat(route.getMaxAmount()).isEqualTo(Money.idr("1000000000"));
        }

        @Test
        @DisplayName("Should create custom route with builder")
        void shouldCreateCustomRouteWithBuilder() {
            TransferRoute route = TransferRoute.builder()
                    .method(TransferMethod.BI_FAST)
                    .fee(Money.idr("1000"))
                    .estimatedTime(Duration.ofMinutes(1))
                    .minAmount(Money.idr("10000"))
                    .maxAmount(Money.idr("1000000"))
                    .build();

            assertThat(route.getMethod()).isEqualTo(TransferMethod.BI_FAST);
            assertThat(route.getFee()).isEqualTo(Money.idr("1000"));
            assertThat(route.getEstimatedTime()).isEqualTo(Duration.ofMinutes(1));
            assertThat(route.getMinAmount()).isEqualTo(Money.idr("10000"));
            assertThat(route.getMaxAmount()).isEqualTo(Money.idr("1000000"));
        }
    }

    // ==================== ELIGIBILITY TESTS ====================

    @Nested
    @DisplayName("Eligibility Checks")
    class EligibilityTests {

        @Test
        @DisplayName("Should be eligible for amount within range")
        void shouldBeEligibleForAmountWithinRange() {
            TransferRoute route = TransferRoute.biFast();

            assertThat(route.isEligibleFor(Money.idr("100000"))).isTrue();
            assertThat(route.isEligibleFor(Money.idr("1"))).isTrue();
            assertThat(route.isEligibleFor(Money.idr("50000000"))).isTrue();
        }

        @Test
        @DisplayName("Should not be eligible for amount below minimum")
        void shouldNotBeEligibleForAmountBelowMinimum() {
            TransferRoute route = TransferRoute.builder()
                    .method(TransferMethod.BI_FAST)
                    .fee(Money.idr("2500"))
                    .estimatedTime(Duration.ofSeconds(30))
                    .minAmount(Money.idr("10000"))
                    .maxAmount(Money.idr("50000000"))
                    .build();

            assertThat(route.isEligibleFor(Money.idr("5000"))).isFalse();
            assertThat(route.isEligibleFor(Money.idr("1"))).isFalse();
        }

        @Test
        @DisplayName("Should not be eligible for amount above maximum")
        void shouldNotBeEligibleForAmountAboveMaximum() {
            TransferRoute route = TransferRoute.biFast();

            assertThat(route.isEligibleFor(Money.idr("50000001"))).isFalse();
            assertThat(route.isEligibleFor(Money.idr("100000000"))).isFalse();
        }

        @Test
        @DisplayName("Should throw exception when checking eligibility with null amount")
        void shouldThrowExceptionWhenCheckingEligibilityWithNullAmount() {
            TransferRoute route = TransferRoute.biFast();

            assertThatThrownBy(() -> route.isEligibleFor(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot be null");
        }

        @Test
        @DisplayName("Should not be eligible for different currency")
        void shouldNotBeEligibleForDifferentCurrency() {
            TransferRoute route = TransferRoute.biFast();
            Money usdAmount = Money.usd("100");

            assertThat(route.isEligibleFor(usdAmount)).isFalse();
        }
    }

    // ==================== FEE CALCULATION TESTS ====================

    @Nested
    @DisplayName("Fee Calculation")
    class FeeCalculationTests {

        @Test
        @DisplayName("Should return correct fee for route")
        void shouldReturnCorrectFeeForRoute() {
            TransferRoute biFast = TransferRoute.biFast();
            TransferRoute rtgs = TransferRoute.rtgs();
            TransferRoute skn = TransferRoute.skn();

            assertThat(biFast.getFee()).isEqualTo(Money.idr("2500"));
            assertThat(rtgs.getFee()).isEqualTo(Money.idr("25000"));
            assertThat(skn.getFee()).isEqualTo(Money.idr("5000"));
        }

        @Test
        @DisplayName("Should calculate total amount with fee")
        void shouldCalculateTotalAmountWithFee() {
            TransferRoute route = TransferRoute.biFast();
            Money amount = Money.idr("100000");

            Money total = route.calculateTotalAmount(amount);

            assertThat(total).isEqualTo(Money.idr("102500"));
        }

        @Test
        @DisplayName("Should throw exception when calculating total with null amount")
        void shouldThrowExceptionWhenCalculatingTotalWithNullAmount() {
            TransferRoute route = TransferRoute.biFast();

            assertThatThrownBy(() -> route.calculateTotalAmount(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when calculating total with different currency")
        void shouldThrowExceptionWhenCalculatingTotalWithDifferentCurrency() {
            TransferRoute route = TransferRoute.biFast();
            Money usdAmount = Money.usd("100");

            assertThatThrownBy(() -> route.calculateTotalAmount(usdAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }
    }

    // ==================== COMPARISON TESTS ====================

    @Nested
    @DisplayName("Comparison")
    class ComparisonTests {

        @Test
        @DisplayName("Should compare routes by fee - cheaper first")
        void shouldCompareRoutesByFeeCheaperFirst() {
            TransferRoute biFast = TransferRoute.biFast(); // 2500
            TransferRoute rtgs = TransferRoute.rtgs();     // 25000
            TransferRoute skn = TransferRoute.skn();       // 5000

            assertThat(biFast.compareTo(skn)).isNegative();
            assertThat(skn.compareTo(biFast)).isPositive();
            assertThat(biFast.compareTo(rtgs)).isNegative();
            assertThat(rtgs.compareTo(biFast)).isPositive();
        }

        @Test
        @DisplayName("Should return zero for equal fees")
        void shouldReturnZeroForEqualFees() {
            TransferRoute route1 = TransferRoute.builder()
                    .method(TransferMethod.BI_FAST)
                    .fee(Money.idr("5000"))
                    .estimatedTime(Duration.ofSeconds(30))
                    .minAmount(Money.idr("1"))
                    .maxAmount(Money.idr("50000000"))
                    .build();

            TransferRoute route2 = TransferRoute.builder()
                    .method(TransferMethod.SKN)
                    .fee(Money.idr("5000"))
                    .estimatedTime(Duration.ofHours(4))
                    .minAmount(Money.idr("1"))
                    .maxAmount(Money.idr("1000000000"))
                    .build();

            assertThat(route1.compareTo(route2)).isZero();
        }
    }

    // ==================== ESTIMATED TIME TESTS ====================

    @Nested
    @DisplayName("Estimated Time")
    class EstimatedTimeTests {

        @Test
        @DisplayName("Should return correct estimated time for each method")
        void shouldReturnCorrectEstimatedTimeForEachMethod() {
            TransferRoute biFast = TransferRoute.biFast();
            TransferRoute rtgs = TransferRoute.rtgs();
            TransferRoute skn = TransferRoute.skn();

            assertThat(biFast.getEstimatedTime()).isEqualTo(Duration.ofSeconds(30));
            assertThat(rtgs.getEstimatedTime()).isEqualTo(Duration.ofMinutes(5));
            assertThat(skn.getEstimatedTime()).isEqualTo(Duration.ofHours(4));
        }

        @Test
        @DisplayName("Should format estimated time correctly")
        void shouldFormatEstimatedTimeCorrectly() {
            TransferRoute biFast = TransferRoute.biFast();
            TransferRoute rtgs = TransferRoute.rtgs();
            TransferRoute skn = TransferRoute.skn();

            assertThat(biFast.getEstimatedTimeDisplay()).isEqualTo("30 seconds");
            assertThat(rtgs.getEstimatedTimeDisplay()).isEqualTo("5 minutes");
            assertThat(skn.getEstimatedTimeDisplay()).isEqualTo("4 hours");
        }
    }

    // ==================== EQUALITY TESTS ====================

    @Nested
    @DisplayName("Equality")
    class EqualityTests {

        @Test
        @DisplayName("Should consider routes with same method as equal")
        void shouldConsiderRoutesWithSameMethodAsEqual() {
            TransferRoute route1 = TransferRoute.biFast();
            TransferRoute route2 = TransferRoute.biFast();

            assertThat(route1).isEqualTo(route2);
            assertThat(route1.hashCode()).isEqualTo(route2.hashCode());
        }

        @Test
        @DisplayName("Should not consider routes with different methods as equal")
        void shouldNotConsiderRoutesWithDifferentMethodsAsEqual() {
            TransferRoute biFast = TransferRoute.biFast();
            TransferRoute rtgs = TransferRoute.rtgs();

            assertThat(biFast).isNotEqualTo(rtgs);
        }
    }

    // ==================== HELPER METHODS ====================

    private TransferRoute createSampleRoute() {
        return TransferRoute.biFast();
    }
}
