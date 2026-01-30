package id.payu.api.common.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link Money} Value Object.
 * <p>
 * Tests cover:
 * <ul>
 *   <li>Construction and factory methods</li>
 *   <li>Immutability</li>
 *   <li>Arithmetic operations</li>
 *   <li>Currency validation</li>
 *   <li>Comparison operations</li>
 *   <li>Edge cases and boundary conditions</li>
 * </ul>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@DisplayName("Money Value Object Tests")
class MoneyTest {

    //region Factory Methods Tests

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodsTest {

        @Test
        @DisplayName("Should create Money with BigDecimal and currency code")
        void shouldCreateMoneyWithBigDecimalAndCurrencyCode() {
            BigDecimal amount = new BigDecimal("100.50");
            Money money = Money.of(amount, "IDR");

            assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
            assertThat(money.getCurrencyCode()).isEqualTo("IDR");
        }

        @Test
        @DisplayName("Should create Money with BigDecimal and Currency object")
        void shouldCreateMoneyWithBigDecimalAndCurrency() {
            BigDecimal amount = new BigDecimal("100.50");
            Currency currency = Currency.getInstance("USD");
            Money money = Money.of(amount, currency);

            assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
            assertThat(money.getCurrencyCode()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should create Money with default currency (IDR)")
        void shouldCreateMoneyWithDefaultCurrency() {
            BigDecimal amount = new BigDecimal("100.50");
            Money money = Money.of(amount);

            assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
            assertThat(money.getCurrencyCode()).isEqualTo("IDR");
        }

        @Test
        @DisplayName("Should create Money from string amount")
        void shouldCreateMoneyFromStringAmount() {
            Money money = Money.of("100.50", "IDR");

            assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
            assertThat(money.getCurrencyCode()).isEqualTo("IDR");
        }

        @Test
        @DisplayName("Should normalize amount to scale 2")
        void shouldNormalizeAmountToScale2() {
            Money money = Money.of(new BigDecimal("100.555"), "IDR");

            assertThat(money.getAmount().scale()).isEqualTo(2);
            assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.56"));
        }

        @Test
        @DisplayName("Should use banker's rounding (HALF_EVEN)")
        void shouldUseBankersRounding() {
            Money money1 = Money.of(new BigDecimal("100.545"), "IDR");
            Money money2 = Money.of(new BigDecimal("100.535"), "IDR");

            assertThat(money1.getAmount()).isEqualByComparingTo(new BigDecimal("100.54"));
            assertThat(money2.getAmount()).isEqualByComparingTo(new BigDecimal("100.54"));
        }

        @Test
        @DisplayName("Should convert currency code to uppercase")
        void shouldConvertCurrencyCodeToUppercase() {
            Money money = Money.of(new BigDecimal("100"), "idr");

            assertThat(money.getCurrencyCode()).isEqualTo("IDR");
        }

        @Test
        @DisplayName("Should throw NullPointerException when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThatThrownBy(() -> Money.of((BigDecimal) null, "IDR"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Amount must not be null");
        }

        @Test
        @DisplayName("Should throw NullPointerException when currency code is null")
        void shouldThrowExceptionWhenCurrencyCodeIsNull() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("100"), (String) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Currency code must not be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid currency code")
        void shouldThrowExceptionForInvalidCurrencyCode() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("100"), "INVALID"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid currency code");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid amount format")
        void shouldThrowExceptionForInvalidAmountFormat() {
            assertThatThrownBy(() -> Money.of("not-a-number", "IDR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid amount format");
        }

        @Test
        @DisplayName("Should return ZERO constant")
        void shouldReturnZeroConstant() {
            assertThat(Money.ZERO.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(Money.ZERO.getCurrencyCode()).isEqualTo("IDR");
        }
    }

    //endregion

    //region Immutability Tests

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTest {

        @Test
        @DisplayName("Should be immutable - external modification of BigDecimal should not affect Money")
        void shouldBeImmutable() {
            BigDecimal mutableAmount = new BigDecimal("100.00");
            Money money = Money.of(mutableAmount, "IDR");

            // Modify the original BigDecimal
            mutableAmount = mutableAmount.add(new BigDecimal("50.00"));

            // Money should remain unchanged
            assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should create new instance on arithmetic operations")
        void shouldCreateNewInstanceOnArithmeticOperations() {
            Money original = Money.of(new BigDecimal("100.00"), "IDR");
            Money result = original.add(Money.of(new BigDecimal("50.00"), "IDR"));

            assertThat(result).isNotSameAs(original);
            assertThat(original.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }

    //endregion

    //region Arithmetic Operations Tests

    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticOperationsTest {

        @Nested
        @DisplayName("Addition")
        class AdditionTest {

            @Test
            @DisplayName("Should add two monies with same currency")
            void shouldAddTwoMoniesWithSameCurrency() {
                Money money1 = Money.of(new BigDecimal("100.00"), "IDR");
                Money money2 = Money.of(new BigDecimal("50.00"), "IDR");

                Money result = money1.add(money2);

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
                assertThat(result.getCurrencyCode()).isEqualTo("IDR");
            }

            @Test
            @DisplayName("Should throw exception when adding different currencies")
            void shouldThrowExceptionWhenAddingDifferentCurrencies() {
                Money idr = Money.of(new BigDecimal("100.00"), "IDR");
                Money usd = Money.of(new BigDecimal("50.00"), "USD");

                assertThatThrownBy(() -> idr.add(usd))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Currency mismatch");
            }

            @Test
            @DisplayName("Should throw NullPointerException when adding null")
            void shouldThrowExceptionWhenAddingNull() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                assertThatThrownBy(() -> money.add(null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("Other money must not be null");
            }
        }

        @Nested
        @DisplayName("Subtraction")
        class SubtractionTest {

            @Test
            @DisplayName("Should subtract two monies with same currency")
            void shouldSubtractTwoMoniesWithSameCurrency() {
                Money money1 = Money.of(new BigDecimal("100.00"), "IDR");
                Money money2 = Money.of(new BigDecimal("30.00"), "IDR");

                Money result = money1.subtract(money2);

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("70.00"));
                assertThat(result.getCurrencyCode()).isEqualTo("IDR");
            }

            @Test
            @DisplayName("Should throw InsufficientFundsException when result is negative")
            void shouldThrowExceptionWhenResultIsNegative() {
                Money money1 = Money.of(new BigDecimal("30.00"), "IDR");
                Money money2 = Money.of(new BigDecimal("100.00"), "IDR");

                assertThatThrownBy(() -> money1.subtract(money2))
                        .isInstanceOf(InsufficientFundsException.class)
                        .hasMessageContaining("Insufficient funds");
            }

            @Test
            @DisplayName("Should throw exception when subtracting different currencies")
            void shouldThrowExceptionWhenSubtractingDifferentCurrencies() {
                Money idr = Money.of(new BigDecimal("100.00"), "IDR");
                Money usd = Money.of(new BigDecimal("50.00"), "USD");

                assertThatThrownBy(() -> idr.subtract(usd))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Currency mismatch");
            }

            @Test
            @DisplayName("Should throw NullPointerException when subtracting null")
            void shouldThrowExceptionWhenSubtractingNull() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                assertThatThrownBy(() -> money.subtract(null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("Other money must not be null");
            }
        }

        @Nested
        @DisplayName("Multiplication")
        class MultiplicationTest {

            @Test
            @DisplayName("Should multiply by BigDecimal")
            void shouldMultiplyByBigDecimal() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");
                BigDecimal multiplier = new BigDecimal("1.5");

                Money result = money.multiply(multiplier);

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            }

            @Test
            @DisplayName("Should multiply by long")
            void shouldMultiplyByLong() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                Money result = money.multiply(3L);

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
            }

            @Test
            @DisplayName("Should multiply by double")
            void shouldMultiplyByDouble() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                Money result = money.multiply(1.5);

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
            }

            @Test
            @DisplayName("Should throw NullPointerException when multiplying by null")
            void shouldThrowExceptionWhenMultiplyingByNull() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                assertThatThrownBy(() -> money.multiply((BigDecimal) null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("Multiplier must not be null");
            }
        }

        @Nested
        @DisplayName("Division")
        class DivisionTest {

            @Test
            @DisplayName("Should divide by BigDecimal")
            void shouldDivideByBigDecimal() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");
                BigDecimal divisor = new BigDecimal("4");

                Money result = money.divide(divisor);

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
            }

            @Test
            @DisplayName("Should divide by long")
            void shouldDivideByLong() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                Money result = money.divide(4L);

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
            }

            @Test
            @DisplayName("Should divide by double")
            void shouldDivideByDouble() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                Money result = money.divide(4.0);

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
            }

            @Test
            @DisplayName("Should throw ArithmeticException when dividing by zero BigDecimal")
            void shouldThrowExceptionWhenDividingByZeroBigDecimal() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                assertThatThrownBy(() -> money.divide(BigDecimal.ZERO))
                        .isInstanceOf(ArithmeticException.class)
                        .hasMessageContaining("Cannot divide by zero");
            }

            @Test
            @DisplayName("Should throw ArithmeticException when dividing by zero long")
            void shouldThrowExceptionWhenDividingByZeroLong() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                assertThatThrownBy(() -> money.divide(0L))
                        .isInstanceOf(ArithmeticException.class)
                        .hasMessageContaining("Cannot divide by zero");
            }

            @Test
            @DisplayName("Should throw NullPointerException when dividing by null")
            void shouldThrowExceptionWhenDividingByNull() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                assertThatThrownBy(() -> money.divide((BigDecimal) null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("Divisor must not be null");
            }
        }

        @Nested
        @DisplayName("Negation and Absolute")
        class NegationAndAbsoluteTest {

            @Test
            @DisplayName("Should negate positive amount")
            void shouldNegatePositiveAmount() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                Money result = money.negate();

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("-100.00"));
                assertThat(result.getCurrencyCode()).isEqualTo("IDR");
            }

            @Test
            @DisplayName("Should negate negative amount")
            void shouldNegateNegativeAmount() {
                Money money = Money.of(new BigDecimal("-100.00"), "IDR");

                Money result = money.negate();

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            }

            @Test
            @DisplayName("Should return absolute value of negative amount")
            void shouldReturnAbsoluteValueOfNegativeAmount() {
                Money money = Money.of(new BigDecimal("-100.00"), "IDR");

                Money result = money.abs();

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            }

            @Test
            @DisplayName("Should return absolute value of positive amount unchanged")
            void shouldReturnAbsoluteValueOfPositiveAmountUnchanged() {
                Money money = Money.of(new BigDecimal("100.00"), "IDR");

                Money result = money.abs();

                assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            }
        }
    }

    //endregion

    //region Comparison Tests

    @Nested
    @DisplayName("Comparison Operations")
    class ComparisonTest {

        @Test
        @DisplayName("Should compare two monies correctly")
        void shouldCompareTwoMonies() {
            Money money1 = Money.of(new BigDecimal("100.00"), "IDR");
            Money money2 = Money.of(new BigDecimal("50.00"), "IDR");
            Money money3 = Money.of(new BigDecimal("100.00"), "IDR");

            assertThat(money1.compareTo(money2)).isPositive();
            assertThat(money2.compareTo(money1)).isNegative();
            assertThat(money1.compareTo(money3)).isZero();
        }

        @Test
        @DisplayName("Should throw exception when comparing different currencies")
        void shouldThrowExceptionWhenComparingDifferentCurrencies() {
            Money idr = Money.of(new BigDecimal("100.00"), "IDR");
            Money usd = Money.of(new BigDecimal("50.00"), "USD");

            assertThatThrownBy(() -> idr.compareTo(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        @DisplayName("Should check isGreaterThan correctly")
        void shouldCheckIsGreaterThan() {
            Money money1 = Money.of(new BigDecimal("100.00"), "IDR");
            Money money2 = Money.of(new BigDecimal("50.00"), "IDR");

            assertThat(money1.isGreaterThan(money2)).isTrue();
            assertThat(money2.isGreaterThan(money1)).isFalse();
        }

        @Test
        @DisplayName("Should check isGreaterThanOrEqualTo correctly")
        void shouldCheckIsGreaterThanOrEqualTo() {
            Money money1 = Money.of(new BigDecimal("100.00"), "IDR");
            Money money2 = Money.of(new BigDecimal("100.00"), "IDR");
            Money money3 = Money.of(new BigDecimal("50.00"), "IDR");

            assertThat(money1.isGreaterThanOrEqualTo(money2)).isTrue();
            assertThat(money1.isGreaterThanOrEqualTo(money3)).isTrue();
            assertThat(money3.isGreaterThanOrEqualTo(money1)).isFalse();
        }

        @Test
        @DisplayName("Should check isLessThan correctly")
        void shouldCheckIsLessThan() {
            Money money1 = Money.of(new BigDecimal("50.00"), "IDR");
            Money money2 = Money.of(new BigDecimal("100.00"), "IDR");

            assertThat(money1.isLessThan(money2)).isTrue();
            assertThat(money2.isLessThan(money1)).isFalse();
        }

        @Test
        @DisplayName("Should check isLessThanOrEqualTo correctly")
        void shouldCheckIsLessThanOrEqualTo() {
            Money money1 = Money.of(new BigDecimal("50.00"), "IDR");
            Money money2 = Money.of(new BigDecimal("50.00"), "IDR");
            Money money3 = Money.of(new BigDecimal("100.00"), "IDR");

            assertThat(money1.isLessThanOrEqualTo(money2)).isTrue();
            assertThat(money1.isLessThanOrEqualTo(money3)).isTrue();
            assertThat(money3.isLessThanOrEqualTo(money1)).isFalse();
        }

        @Test
        @DisplayName("Should return min correctly")
        void shouldReturnMin() {
            Money money1 = Money.of(new BigDecimal("50.00"), "IDR");
            Money money2 = Money.of(new BigDecimal("100.00"), "IDR");

            assertThat(money1.min(money2)).isEqualTo(money1);
            assertThat(money2.min(money1)).isEqualTo(money1);
        }

        @Test
        @DisplayName("Should return max correctly")
        void shouldReturnMax() {
            Money money1 = Money.of(new BigDecimal("50.00"), "IDR");
            Money money2 = Money.of(new BigDecimal("100.00"), "IDR");

            assertThat(money1.max(money2)).isEqualTo(money2);
            assertThat(money2.max(money1)).isEqualTo(money2);
        }
    }

    //endregion

    //region State Checks Tests

    @Nested
    @DisplayName("State Checks")
    class StateChecksTest {

        @Test
        @DisplayName("Should return true for isZero when amount is zero")
        void shouldReturnTrueForIsZero() {
            Money money = Money.of(BigDecimal.ZERO, "IDR");

            assertThat(money.isZero()).isTrue();
        }

        @Test
        @DisplayName("Should return false for isZero when amount is not zero")
        void shouldReturnFalseForIsZero() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");

            assertThat(money.isZero()).isFalse();
        }

        @Test
        @DisplayName("Should return true for isPositive when amount is positive")
        void shouldReturnTrueForIsPositive() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");

            assertThat(money.isPositive()).isTrue();
        }

        @Test
        @DisplayName("Should return false for isPositive when amount is zero or negative")
        void shouldReturnFalseForIsPositive() {
            Money zero = Money.of(BigDecimal.ZERO, "IDR");
            Money negative = Money.of(new BigDecimal("-100.00"), "IDR");

            assertThat(zero.isPositive()).isFalse();
            assertThat(negative.isPositive()).isFalse();
        }

        @Test
        @DisplayName("Should return true for isNegative when amount is negative")
        void shouldReturnTrueForIsNegative() {
            Money money = Money.of(new BigDecimal("-100.00"), "IDR");

            assertThat(money.isNegative()).isTrue();
        }

        @Test
        @DisplayName("Should return false for isNegative when amount is zero or positive")
        void shouldReturnFalseForIsNegative() {
            Money zero = Money.of(BigDecimal.ZERO, "IDR");
            Money positive = Money.of(new BigDecimal("100.00"), "IDR");

            assertThat(zero.isNegative()).isFalse();
            assertThat(positive.isNegative()).isFalse();
        }

        @Test
        @DisplayName("Should return true for isPositiveOrZero when amount is positive or zero")
        void shouldReturnTrueForIsPositiveOrZero() {
            Money positive = Money.of(new BigDecimal("100.00"), "IDR");
            Money zero = Money.of(BigDecimal.ZERO, "IDR");

            assertThat(positive.isPositiveOrZero()).isTrue();
            assertThat(zero.isPositiveOrZero()).isTrue();
        }

        @Test
        @DisplayName("Should return false for isPositiveOrZero when amount is negative")
        void shouldReturnFalseForIsPositiveOrZero() {
            Money negative = Money.of(new BigDecimal("-100.00"), "IDR");

            assertThat(negative.isPositiveOrZero()).isFalse();
        }
    }

    //endregion

    //region Percentage Tests

    @Nested
    @DisplayName("Percentage Operations")
    class PercentageTest {

        @Test
        @DisplayName("Should add percentage correctly")
        void shouldAddPercentage() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");
            BigDecimal percentage = new BigDecimal("10");

            Money result = money.addPercentage(percentage);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("110.00"));
        }

        @Test
        @DisplayName("Should subtract percentage correctly")
        void shouldSubtractPercentage() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");
            BigDecimal percentage = new BigDecimal("10");

            Money result = money.subtractPercentage(percentage);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("90.00"));
        }

        @Test
        @DisplayName("Should calculate percentage correctly")
        void shouldCalculatePercentage() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");
            BigDecimal percentage = new BigDecimal("10");

            Money result = money.percentage(percentage);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("Should throw NullPointerException when percentage is null")
        void shouldThrowExceptionWhenPercentageIsNull() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");

            assertThatThrownBy(() -> money.addPercentage(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Percentage must not be null");
        }
    }

    //endregion

    //region Currency Conversion Tests

    @Nested
    @DisplayName("Currency Conversion")
    class CurrencyConversionTest {

        @Test
        @DisplayName("Should convert to different currency")
        void shouldConvertToDifferentCurrency() {
            Money idr = Money.of(new BigDecimal("10000.00"), "IDR");
            CurrencyConverter converter = (amount, from, to) -> {
                // Mock conversion: IDR to USD at rate 0.000065
                return amount.multiply(new BigDecimal("0.000065"));
            };

            Money usd = idr.convertTo("USD", converter);

            assertThat(usd.getCurrencyCode()).isEqualTo("USD");
            assertThat(usd.getAmount()).isEqualByComparingTo(new BigDecimal("0.65"));
        }

        @Test
        @DisplayName("Should return same instance when converting to same currency")
        void shouldReturnSameInstanceWhenConvertingToSameCurrency() {
            Money idr = Money.of(new BigDecimal("10000.00"), "IDR");
            CurrencyConverter converter = (amount, from, to) -> amount;

            Money result = idr.convertTo("IDR", converter);

            assertThat(result).isSameAs(idr);
        }

        @Test
        @DisplayName("Should throw NullPointerException when target currency is null")
        void shouldThrowExceptionWhenTargetCurrencyIsNull() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");
            CurrencyConverter converter = (amount, from, to) -> amount;

            assertThatThrownBy(() -> money.convertTo(null, converter))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Target currency code must not be null");
        }

        @Test
        @DisplayName("Should throw NullPointerException when converter is null")
        void shouldThrowExceptionWhenConverterIsNull() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");

            assertThatThrownBy(() -> money.convertTo("USD", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Converter must not be null");
        }
    }

    //endregion

    //region Equals and HashCode Tests

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCodeTest {

        @Test
        @DisplayName("Should be equal when amount and currency are the same")
        void shouldBeEqualWhenAmountAndCurrencyAreSame() {
            Money money1 = Money.of(new BigDecimal("100.00"), "IDR");
            Money money2 = Money.of(new BigDecimal("100.00"), "IDR");

            assertThat(money1).isEqualTo(money2);
            assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when amounts differ")
        void shouldNotBeEqualWhenAmountsDiffer() {
            Money money1 = Money.of(new BigDecimal("100.00"), "IDR");
            Money money2 = Money.of(new BigDecimal("200.00"), "IDR");

            assertThat(money1).isNotEqualTo(money2);
        }

        @Test
        @DisplayName("Should not be equal when currencies differ")
        void shouldNotBeEqualWhenCurrenciesDiffer() {
            Money money1 = Money.of(new BigDecimal("100.00"), "IDR");
            Money money2 = Money.of(new BigDecimal("100.00"), "USD");

            assertThat(money1).isNotEqualTo(money2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");

            assertThat(money).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");

            assertThat(money).isNotEqualTo("100.00 IDR");
        }
    }

    //endregion

    //region ToString Tests

    @Nested
    @DisplayName("ToString")
    class ToStringTest {

        @Test
        @DisplayName("Should return meaningful string representation")
        void shouldReturnMeaningfulStringRepresentation() {
            Money money = Money.of(new BigDecimal("100.50"), "IDR");

            String result = money.toString();

            assertThat(result).contains("IDR");
            assertThat(result).contains("100.50");
        }
    }

    //endregion

    //region Constants Tests

    @Nested
    @DisplayName("Constants")
    class ConstantsTest {

        @Test
        @DisplayName("Should have correct default scale")
        void shouldHaveCorrectDefaultScale() {
            assertThat(Money.DEFAULT_SCALE).isEqualTo(2);
        }

        @Test
        @DisplayName("Should have correct default rounding mode")
        void shouldHaveCorrectDefaultRoundingMode() {
            assertThat(Money.DEFAULT_ROUNDING).isEqualTo(RoundingMode.HALF_EVEN);
        }

        @Test
        @DisplayName("Should have correct default currency code")
        void shouldHaveCorrectDefaultCurrencyCode() {
            assertThat(Money.DEFAULT_CURRENCY_CODE).isEqualTo("IDR");
        }
    }

    //endregion

    //region GetCurrency Tests

    @Nested
    @DisplayName("GetCurrency")
    class GetCurrencyTest {

        @Test
        @DisplayName("Should return Currency object")
        void shouldReturnCurrencyObject() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");

            Currency currency = money.getCurrency();

            assertThat(currency).isEqualTo(Currency.getInstance("IDR"));
        }

        @Test
        @DisplayName("Should cache Currency object")
        void shouldCacheCurrencyObject() {
            Money money = Money.of(new BigDecimal("100.00"), "IDR");

            Currency currency1 = money.getCurrency();
            Currency currency2 = money.getCurrency();

            assertThat(currency1).isSameAs(currency2);
        }
    }

    //endregion

    //region Edge Cases Tests

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @ParameterizedTest
        @ValueSource(strings = {"0.00", "0.000", "0", "0.0"})
        @DisplayName("Should handle various zero representations")
        void shouldHandleVariousZeroRepresentations(String zeroValue) {
            Money money = Money.of(new BigDecimal(zeroValue), "IDR");

            assertThat(money.isZero()).isTrue();
        }

        @Test
        @DisplayName("Should handle very large amounts")
        void shouldHandleVeryLargeAmounts() {
            BigDecimal largeAmount = new BigDecimal("999999999999999999.99");
            Money money = Money.of(largeAmount, "IDR");

            assertThat(money.getAmount()).isEqualByComparingTo(largeAmount);
        }

        @Test
        @DisplayName("Should handle very small amounts")
        void shouldHandleVerySmallAmounts() {
            BigDecimal smallAmount = new BigDecimal("0.01");
            Money money = Money.of(smallAmount, "IDR");

            assertThat(money.getAmount()).isEqualByComparingTo(smallAmount);
        }

        @ParameterizedTest
        @CsvSource({
            "100.005, 100.00",
            "100.015, 100.02",
            "100.025, 100.02",
            "100.035, 100.04"
        })
        @DisplayName("Should apply banker's rounding correctly")
        void shouldApplyBankersRoundingCorrectly(String input, String expected) {
            Money money = Money.of(new BigDecimal(input), "IDR");

            assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal(expected));
        }
    }

    //endregion
}
