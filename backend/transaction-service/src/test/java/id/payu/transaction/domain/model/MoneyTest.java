package id.payu.transaction.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Money Value Object.
 *
 * <p>P0 Critical Tests - These tests verify the core financial calculations
 * that must be precise for PCI-DSS compliance.</p>
 *
 * <p>Test Categories:</p>
 * <ul>
 *   <li>Factory Methods - idr(), usd(), of()</li>
 *   <li>Arithmetic Operations - add(), subtract(), multiply(), divide()</li>
 *   <li>Currency Validation - cross-currency operations must fail</li>
 *   <li>Precision & Rounding - proper decimal handling</li>
 *   <li>Comparison Operations - compareTo, equals, hashCode</li>
 *   <li>Edge Cases - zero, negative, overflow</li>
 * </ul>
 *
 * @see Money
 */
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("Money Value Object Tests")
class MoneyTest {

    // ==================== FACTORY METHOD TESTS ====================

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodsTests {

        @Test
        @DisplayName("Should create Money from BigDecimal and currency code")
        void shouldCreateMoneyFromBigDecimalAndCurrencyCode() {
            Money money = Money.of(new BigDecimal("100000"), "IDR");

            assertThat(money.getAmount()).isEqualTo(new BigDecimal("100000"));
            assertThat(money.getCurrency().getCurrencyCode()).isEqualTo("IDR");
        }

        @Test
        @DisplayName("Should create Money from string amount and currency code")
        void shouldCreateMoneyFromStringAndCurrencyCode() {
            Money money = Money.of("100000", "IDR");

            assertThat(money.getAmount()).isEqualTo(new BigDecimal("100000"));
            assertThat(money.getCurrency().getCurrencyCode()).isEqualTo("IDR");
        }

        @Test
        @DisplayName("Should create Money with idr() factory method")
        void shouldCreateMoneyWithIdrFactory() {
            Money money = Money.idr("100000");

            assertThat(money.getAmount()).isEqualTo(new BigDecimal("100000"));
            assertThat(money.getCurrency().getCurrencyCode()).isEqualTo("IDR");
        }

        @Test
        @DisplayName("Should create Money with idr() BigDecimal factory method")
        void shouldCreateMoneyWithIdrBigDecimalFactory() {
            BigDecimal amount = new BigDecimal("100000");
            Money money = Money.idr(amount);

            assertThat(money.getAmount()).isEqualTo(amount);
            assertThat(money.getCurrency().getCurrencyCode()).isEqualTo("IDR");
        }

        @Test
        @DisplayName("Should create Money with usd() factory method")
        void shouldCreateMoneyWithUsdFactory() {
            Money money = Money.usd(new BigDecimal("100"));

            assertThat(money.getAmount()).isEqualTo(new BigDecimal("100"));
            assertThat(money.getCurrency().getCurrencyCode()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThatThrownBy(() -> Money.of((BigDecimal) null, "IDR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when amount has more than 2 decimal places")
        void shouldThrowExceptionWhenAmountHasMoreThanTwoDecimalPlaces() {
            assertThatThrownBy(() -> Money.of("100.123", "IDR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot have more than 2 decimal places");
        }
    }

    // ==================== ARITHMETIC OPERATIONS TESTS ====================

    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticOperationsTests {

        @Test
        @DisplayName("Should add two Money instances of same currency")
        void shouldAddTwoMoneyInstancesOfSameCurrency() {
            Money money1 = Money.idr("100000");
            Money money2 = Money.idr("50000");

            Money result = money1.add(money2);

            assertThat(result.getAmount()).isEqualTo(new BigDecimal("150000"));
            assertThat(result.getCurrency().getCurrencyCode()).isEqualTo("IDR");
        }

        @Test
        @DisplayName("Should subtract two Money instances of same currency")
        void shouldSubtractTwoMoneyInstancesOfSameCurrency() {
            Money money1 = Money.idr("100000");
            Money money2 = Money.idr("50000");

            Money result = money1.subtract(money2);

            assertThat(result.getAmount()).isEqualTo(new BigDecimal("50000"));
            assertThat(result.getCurrency().getCurrencyCode()).isEqualTo("IDR");
        }

        @Test
        @DisplayName("Should multiply Money by positive BigDecimal")
        void shouldMultiplyMoneyByPositiveBigDecimal() {
            Money money = Money.idr("100000");

            Money result = money.multiply(new BigDecimal("2.5"));

            assertThat(result.getAmount()).isEqualTo(new BigDecimal("250000.00"));
        }

        @Test
        @DisplayName("Should multiply Money by positive integer")
        void shouldMultiplyMoneyByPositiveInteger() {
            Money money = Money.idr("100000");

            Money result = money.multiply(3);

            assertThat(result.getAmount()).isEqualTo(new BigDecimal("300000"));
        }

        @Test
        @DisplayName("Should divide Money by positive divisor")
        void shouldDivideMoneyByPositiveDivisor() {
            Money money = Money.idr("100000");

            Money result = money.divide(new BigDecimal("3"));

            assertThat(result.getAmount()).isEqualTo(new BigDecimal("33333.33"));
        }

        @Test
        @DisplayName("Should calculate percentage correctly")
        void shouldCalculatePercentageCorrectly() {
            Money money = Money.idr(new BigDecimal("100000"));

            Money result = money.percentage(10);

            assertThat(result.getAmount()).isEqualTo(new BigDecimal("10000.00"));
        }

        @Test
        @DisplayName("Should calculate percentage from integer")
        void shouldCalculatePercentageFromInteger() {
            Money money = Money.idr(new BigDecimal("100000"));

            Money result = money.percentage(25);

            assertThat(result.getAmount()).isEqualTo(new BigDecimal("25000.00"));
        }
    }

    // ==================== CURRENCY VALIDATION TESTS ====================

    @Nested
    @DisplayName("Currency Validation")
    class CurrencyValidationTests {

        @Test
        @DisplayName("Should throw exception when adding different currencies")
        void shouldThrowExceptionWhenAddingDifferentCurrencies() {
            Money idr = Money.idr("100000");
            Money usd = Money.usd("100");

            assertThatThrownBy(() -> idr.add(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        @DisplayName("Should throw exception when subtracting different currencies")
        void shouldThrowExceptionWhenSubtractingDifferentCurrencies() {
            Money idr = Money.idr("100000");
            Money usd = Money.usd("100");

            assertThatThrownBy(() -> idr.subtract(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        @DisplayName("Should throw exception when comparing different currencies")
        void shouldThrowExceptionWhenComparingDifferentCurrencies() {
            Money idr = Money.idr("100000");
            Money usd = Money.usd("100");

            assertThatThrownBy(() -> idr.compareTo(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }
    }

    // ==================== NEGATIVE VALUE TESTS ====================

    @Nested
    @DisplayName("Negative Value Protection")
    class NegativeValueProtectionTests {

        @Test
        @DisplayName("Should throw exception when subtract would result in negative")
        void shouldThrowExceptionWhenSubtractWouldResultInNegative() {
            Money money = Money.idr("100000");
            Money larger = Money.idr("150000");

            assertThatThrownBy(() -> money.subtract(larger))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Result cannot be negative");
        }

        @Test
        @DisplayName("Should throw exception when multiplying by negative")
        void shouldThrowExceptionWhenMultiplyingByNegative() {
            Money money = Money.idr("100000");

            assertThatThrownBy(() -> money.multiply(new BigDecimal("-1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Multiplier cannot be negative");
        }

        @Test
        @DisplayName("Should throw exception when multiplying by negative integer")
        void shouldThrowExceptionWhenMultiplyingByNegativeInteger() {
            Money money = Money.idr("100000");

            assertThatThrownBy(() -> money.multiply(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Multiplier cannot be negative");
        }

        @Test
        @DisplayName("Should throw exception when dividing by zero")
        void shouldThrowExceptionWhenDividingByZero() {
            Money money = Money.idr("100000");

            assertThatThrownBy(() -> money.divide(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Divisor cannot be null or zero");
        }

        @Test
        @DisplayName("Should throw exception when dividing by null")
        void shouldThrowExceptionWhenDividingByNull() {
            Money money = Money.idr("100000");

            assertThatThrownBy(() -> money.divide(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Divisor cannot be null or zero");
        }
    }

    // ==================== COMPARISON OPERATIONS TESTS ====================

    @Nested
    @DisplayName("Comparison Operations")
    class ComparisonOperationsTests {

        @Test
        @DisplayName("Should compare Money instances correctly")
        void shouldCompareMoneyInstancesCorrectly() {
            Money small = Money.idr("50000");
            Money medium = Money.idr("100000");
            Money large = Money.idr("150000");
            Money equal = Money.idr("100000");

            assertThat(medium.compareTo(small)).isPositive();
            assertThat(medium.compareTo(large)).isNegative();
            assertThat(medium.compareTo(equal)).isZero();
        }

        @Test
        @DisplayName("Should return true for isGreaterThan")
        void shouldReturnTrueForIsGreaterThan() {
            Money money1 = Money.idr("100000");
            Money money2 = Money.idr("50000");

            assertThat(money1.isGreaterThan(money2)).isTrue();
            assertThat(money2.isGreaterThan(money1)).isFalse();
        }

        @Test
        @DisplayName("Should return true for isLessThan")
        void shouldReturnTrueForIsLessThan() {
            Money money1 = Money.idr("50000");
            Money money2 = Money.idr("100000");

            assertThat(money1.isLessThan(money2)).isTrue();
            assertThat(money2.isLessThan(money1)).isFalse();
        }

        @Test
        @DisplayName("Should return true for isGreaterThanOrEqualTo")
        void shouldReturnTrueForIsGreaterThanOrEqualTo() {
            Money money1 = Money.idr("100000");
            Money money2 = Money.idr("100000");
            Money money3 = Money.idr("50000");

            assertThat(money1.isGreaterThanOrEqualTo(money2)).isTrue();
            assertThat(money1.isGreaterThanOrEqualTo(money3)).isTrue();
        }

        @Test
        @DisplayName("Should return true for isLessThanOrEqualTo")
        void shouldReturnTrueForIsLessThanOrEqualTo() {
            Money money1 = Money.idr("100000");
            Money money2 = Money.idr("100000");
            Money money3 = Money.idr("150000");

            assertThat(money1.isLessThanOrEqualTo(money2)).isTrue();
            assertThat(money1.isLessThanOrEqualTo(money3)).isTrue();
        }
    }

    // ==================== ROUNDING TESTS ====================

    @Nested
    @DisplayName("Rounding Operations")
    class RoundingOperationsTests {

        @Test
        @DisplayName("Should round to specified scale")
        void shouldRoundToSpecifiedScale() {
            Money money = Money.of(new BigDecimal("100.123"), "IDR");

            Money rounded = money.round(2);

            assertThat(rounded.getAmount()).isEqualTo(new BigDecimal("100.12"));
        }

        @Test
        @DisplayName("Should round to default scale (2 decimal places)")
        void shouldRoundToDefaultScale() {
            Money money = Money.of(new BigDecimal("100.456"), "IDR");

            Money rounded = money.round();

            assertThat(rounded.getAmount()).isEqualTo(new BigDecimal("100.46"));
        }

        @Test
        @DisplayName("Should use HALF_EVEN rounding mode")
        void shouldUseHalfEvenRoundingMode() {
            Money money1 = Money.of(new BigDecimal("2.5"), "IDR");
            Money money2 = Money.of(new BigDecimal("3.5"), "IDR");

            // HALF_EVEN rounds to nearest even number on .5
            assertThat(money1.round().getAmount()).isEqualTo(new BigDecimal("2.00"));
            assertThat(money2.round().getAmount()).isEqualTo(new BigDecimal("4.00"));
        }

        @Test
        @DisplayName("Should round after multiplication")
        void shouldRoundAfterMultiplication() {
            Money money = Money.idr(new BigDecimal("100000"));

            // 100000 * 0.333 = 33300, but with rounding to 2 decimal places
            Money result = money.multiply(new BigDecimal("0.333"));

            assertThat(result.getAmount()).isEqualTo(new BigDecimal("33300.00"));
        }
    }

    // ==================== EQUALITY AND HASHCODE TESTS ====================

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should consider equal amounts with same currency as equal")
        void shouldConsiderEqualAmountsWithSameCurrencyAsEqual() {
            Money money1 = Money.idr("100000");
            Money money2 = Money.idr("100000");

            assertThat(money1).isEqualTo(money2);
            assertThat(money1).hasSameHashCodeAs(money2);
        }

        @Test
        @DisplayName("Should not consider equal amounts with different currency as equal")
        void shouldNotConsiderEqualAmountsWithDifferentCurrencyAsEqual() {
            Money idr = Money.idr("100000");
            Money usd = Money.usd("100000");

            assertThat(idr).isNotEqualTo(usd);
        }

        @Test
        @DisplayName("Should not consider different amounts as equal")
        void shouldNotConsiderDifferentAmountsAsEqual() {
            Money money1 = Money.idr("100000");
            Money money2 = Money.idr("50000");

            assertThat(money1).isNotEqualTo(money2);
        }

        @Test
        @DisplayName("Should work correctly in HashSet")
        void shouldWorkCorrectlyInHashSet() {
            Set<Money> moneySet = new HashSet<>();
            moneySet.add(Money.idr("100000"));
            moneySet.add(Money.idr("100000")); // Duplicate
            moneySet.add(Money.idr("50000"));

            assertThat(moneySet).hasSize(2);
        }

        @Test
        @DisplayName("Should work correctly in TreeSet")
        void shouldWorkCorrectlyInTreeSet() {
            Set<Money> moneySet = new TreeSet<>();
            moneySet.add(Money.idr("150000"));
            moneySet.add(Money.idr("50000"));
            moneySet.add(Money.idr("100000"));

            assertThat(moneySet).containsExactly(
                    Money.idr("50000"),
                    Money.idr("100000"),
                    Money.idr("150000")
            );
        }
    }

    // ==================== ABSOLUTE VALUE TESTS ====================

    @Nested
    @DisplayName("Absolute Value Operations")
    class AbsoluteValueOperationsTests {

        @Test
        @DisplayName("Should return absolute value for positive Money")
        void shouldReturnAbsoluteValueForPositiveMoney() {
            Money money = Money.idr("100000");

            Money abs = money.abs();

            assertThat(abs.getAmount()).isEqualTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("Should return absolute value for negative Money")
        void shouldReturnAbsoluteValueForNegativeMoney() {
            Money money = Money.idr(new BigDecimal("100000")).negate();

            Money abs = money.abs();

            assertThat(abs.getAmount()).isEqualTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("Should negate Money correctly")
        void shouldNegateMoneyCorrectly() {
            Money money = Money.idr("100000");

            Money negated = money.negate();

            assertThat(negated.getAmount()).isEqualTo(new BigDecimal("-100000"));
        }
    }

    // ==================== FORMAT TESTS ====================

    @Nested
    @DisplayName("Formatting")
    class FormattingTests {

        @Test
        @DisplayName("Should format IDR correctly")
        void shouldFormatIdrCorrectly() {
            Money money = Money.idr("1000000");

            String formatted = money.format();

            assertThat(formatted).contains("Rp"); // IDR symbol
            assertThat(formatted).contains("1,000,000.00");
        }

        @Test
        @DisplayName("Should format USD correctly")
        void shouldFormatUsdCorrectly() {
            Money money = Money.usd("100");

            String formatted = money.format();

            assertThat(formatted).contains("$");
            assertThat(formatted).contains("100.00");
        }
    }

    // ==================== EDGE CASES TESTS ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle zero Money correctly")
        void shouldHandleZeroMoneyCorrectly() {
            Money zero = Money.idr("0");

            assertThat(zero.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(zero.isZero()).isTrue();
            assertThat(zero.isNegative()).isFalse();
        }

        @Test
        @DisplayName("Should handle very small amounts")
        void shouldHandleVerySmallAmounts() {
            Money money = Money.idr("0.01");

            assertThat(money.getAmount()).isEqualTo(new BigDecimal("0.01"));
        }

        @Test
        @DisplayName("Should handle large amounts")
        void shouldHandleLargeAmounts() {
            Money money = Money.idr("999999999999.99");

            assertThat(money.getAmount()).isEqualTo(new BigDecimal("999999999999.99"));
        }

        @Test
        @DisplayName("Should be immutable")
        void shouldBeImmutable() {
            Money money1 = Money.idr("100000");
            Money money2 = money1;

            // Operations should return new instances
            Money added = money1.add(Money.idr("50000"));

            assertThat(added).isNotEqualTo(money1);
            assertThat(money1).isEqualTo(money2); // Original unchanged
        }

        @Test
        @DisplayName("Should handle adding to zero correctly")
        void shouldHandleAddingToZeroCorrectly() {
            Money zero = Money.idr("0");
            Money amount = Money.idr("100000");

            Money result = zero.add(amount);

            assertThat(result).isEqualTo(amount);
        }

        @Test
        @DisplayName("Should handle subtracting zero correctly")
        void shouldHandleSubtractingZeroCorrectly() {
            Money money = Money.idr("100000");
            Money zero = Money.idr("0");

            Money result = money.subtract(zero);

            assertThat(result).isEqualTo(money);
        }

        @Test
        @DisplayName("Should handle multiplying by one correctly")
        void shouldHandleMultiplyingByOneCorrectly() {
            Money money = Money.idr("100000");

            Money result = money.multiply(BigDecimal.ONE);

            assertThat(result).isEqualTo(money);
        }

        @Test
        @DisplayName("Should handle dividing by one correctly")
        void shouldHandleDividingByOneCorrectly() {
            Money money = Money.idr("100000");

            Money result = money.divide(BigDecimal.ONE);

            assertThat(result).isEqualTo(money);
        }
    }

    // ==================== PRECISION & SCALE TESTS ====================

    @Nested
    @DisplayName("Precision and Scale")
    class PrecisionAndScaleTests {

        @Test
        @DisplayName("Should maintain 2 decimal places scale after creation")
        void shouldMaintainTwoDecimalPlacesScaleAfterCreation() {
            Money money = Money.of("100.1", "IDR"); // Only 1 decimal place

            assertThat(money.getAmount().scale()).isEqualTo(2); // Should be rounded to 2
        }

        @Test
        @DisplayName("Should maintain 2 decimal places scale after multiplication")
        void shouldMaintainTwoDecimalPlacesScaleAfterMultiplication() {
            Money money = Money.idr("100");

            Money result = money.multiply(new BigDecimal("1.5"));

            assertThat(result.getAmount().scale()).isEqualTo(2);
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("Should handle repeating decimals correctly")
        void shouldHandleRepeatingDecimalsCorrectly() {
            Money money = Money.idr("100");

            Money result = money.divide(new BigDecimal("3"));

            // 100 / 3 = 33.3333..., rounded to 2 decimal places with HALF_EVEN
            assertThat(result.getAmount()).isEqualTo(new BigDecimal("33.33"));
        }
    }

    // ==================== STRING REPRESENTATION TESTS ====================

    @Nested
    @DisplayName("String Representation")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should include amount and currency in toString")
        void shouldIncludeAmountAndCurrencyInToString() {
            Money money = Money.idr("100000");

            String str = money.toString();

            assertThat(str).contains("amount=100000");
            assertThat(str).contains("currency=IDR");
        }
    }
}
