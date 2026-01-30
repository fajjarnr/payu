package id.payu.api.common.money;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable Value Object representing monetary amount with currency.
 * <p>
 * Financial Grade implementation with the following characteristics:
 * <ul>
 *   <li>Uses {@link BigDecimal} for precise decimal arithmetic (never double/float)</li>
 *   <li>Scale of 2 with {@link RoundingMode#HALF_EVEN} (banker's rounding)</li>
 *   <li>Immutable - thread-safe for concurrent access</li>
 *   <li>Validates currency against ISO 4217 standard</li>
 *   <li>Supports arithmetic operations with currency consistency checks</li>
 * </ul>
 *
 * <p>Usage examples:
 * <pre>
 * Money amount = Money.of(new BigDecimal("100.50"), "IDR");
 * Money total = amount.add(Money.of(new BigDecimal("50.25"), "IDR"));
 * Money discounted = total.multiply(new BigDecimal("0.9"));
 * </pre>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode
@ToString
@Embeddable
@JsonSerialize(using = MoneySerializer.class)
@JsonDeserialize(using = MoneyDeserializer.class)
@Convert(converter = MoneyJpaConverter.class)
public final class Money implements Serializable, Comparable<Money> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Default scale for monetary calculations.
     */
    public static final int DEFAULT_SCALE = 2;

    /**
     * Default rounding mode for monetary calculations (Banker's rounding).
     */
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;

    /**
     * Default currency for PayU platform.
     */
    public static final String DEFAULT_CURRENCY_CODE = "IDR";

    /**
     * Zero amount in default currency.
     */
    public static final Money ZERO = of(BigDecimal.ZERO, DEFAULT_CURRENCY_CODE);

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private final BigDecimal amount;

    @Column(name = "currency_code", length = 3, nullable = false)
    private final String currencyCode;

    @Getter(AccessLevel.NONE)
    private transient Currency currency;

    /**
     * Private constructor - use factory methods {@link #of(BigDecimal, String)} or {@link #of(BigDecimal, Currency)}.
     */
    private Money(BigDecimal amount, String currencyCode) {
        this.amount = normalizeAmount(amount);
        this.currencyCode = validateCurrencyCode(currencyCode);
    }

    /**
     * Default constructor for JPA.
     * Do not use directly - always use factory methods.
     */
    @SuppressWarnings("unused")
    private Money() {
        this.amount = null;
        this.currencyCode = null;
    }

    /**
     * Creates a Money instance with the specified amount and currency code.
     *
     * @param amount       the monetary amount (must not be null)
     * @param currencyCode the ISO 4217 currency code (e.g., "IDR", "USD")
     * @return a new Money instance
     * @throws IllegalArgumentException if amount is null or currency code is invalid
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currencyCode, "Currency code must not be null");
        return new Money(amount, currencyCode);
    }

    /**
     * Creates a Money instance with the specified amount and currency.
     *
     * @param amount   the monetary amount (must not be null)
     * @param currency the currency (must not be null)
     * @return a new Money instance
     * @throws IllegalArgumentException if amount or currency is null
     */
    public static Money of(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        return new Money(amount, currency.getCurrencyCode());
    }

    /**
     * Creates a Money instance with the specified amount in the default currency (IDR).
     *
     * @param amount the monetary amount (must not be null)
     * @return a new Money instance in IDR
     * @throws IllegalArgumentException if amount is null
     */
    public static Money of(BigDecimal amount) {
        return of(amount, DEFAULT_CURRENCY_CODE);
    }

    /**
     * Creates a Money instance from a string representation of the amount.
     *
     * @param amount       the monetary amount as string (must not be null)
     * @param currencyCode the ISO 4217 currency code
     * @return a new Money instance
     * @throws IllegalArgumentException if amount is null, invalid, or currency code is invalid
     */
    public static Money of(String amount, String currencyCode) {
        Objects.requireNonNull(amount, "Amount must not be null");
        try {
            return of(new BigDecimal(amount), currencyCode);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + amount, e);
        }
    }

    /**
     * Returns the Currency object for this money's currency code.
     *
     * @return the Currency instance
     */
    public Currency getCurrency() {
        if (currency == null) {
            currency = Currency.getInstance(currencyCode);
        }
        return currency;
    }

    /**
     * Adds the specified Money to this Money.
     * Both monies must have the same currency.
     *
     * @param other the Money to add (must not be null and same currency)
     * @return a new Money instance with the sum
     * @throws IllegalArgumentException if currencies don't match
     * @throws NullPointerException     if other is null
     */
    public Money add(Money other) {
        Objects.requireNonNull(other, "Other money must not be null");
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }

    /**
     * Subtracts the specified Money from this Money.
     * Both monies must have the same currency.
     *
     * @param other the Money to subtract (must not be null and same currency)
     * @return a new Money instance with the difference
     * @throws IllegalArgumentException if currencies don't match or result would be negative
     * @throws InsufficientFundsException if the result would be negative
     * @throws NullPointerException     if other is null
     */
    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Other money must not be null");
        assertSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(
                    this,
                    other,
                    "Insufficient funds: cannot subtract " + other + " from " + this
            );
        }
        return new Money(result, this.currencyCode);
    }

    /**
     * Multiplies this Money by the specified multiplier.
     *
     * @param multiplier the multiplier (must not be null)
     * @return a new Money instance with the product
     * @throws NullPointerException if multiplier is null
     */
    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "Multiplier must not be null");
        return new Money(
                this.amount.multiply(multiplier).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING),
                this.currencyCode
        );
    }

    /**
     * Multiplies this Money by the specified multiplier.
     *
     * @param multiplier the multiplier
     * @return a new Money instance with the product
     */
    public Money multiply(long multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    /**
     * Multiplies this Money by the specified multiplier.
     *
     * @param multiplier the multiplier
     * @return a new Money instance with the product
     */
    public Money multiply(double multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    /**
     * Divides this Money by the specified divisor.
     *
     * @param divisor the divisor (must not be null or zero)
     * @return a new Money instance with the quotient
     * @throws ArithmeticException  if divisor is zero
     * @throws NullPointerException if divisor is null
     */
    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "Divisor must not be null");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return new Money(
                this.amount.divide(divisor, DEFAULT_SCALE, DEFAULT_ROUNDING),
                this.currencyCode
        );
    }

    /**
     * Divides this Money by the specified divisor.
     *
     * @param divisor the divisor (must not be zero)
     * @return a new Money instance with the quotient
     * @throws ArithmeticException if divisor is zero
     */
    public Money divide(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return divide(BigDecimal.valueOf(divisor));
    }

    /**
     * Divides this Money by the specified divisor.
     *
     * @param divisor the divisor (must not be zero)
     * @return a new Money instance with the quotient
     * @throws ArithmeticException if divisor is zero
     */
    public Money divide(double divisor) {
        return divide(BigDecimal.valueOf(divisor));
    }

    /**
     * Returns a new Money with the negated amount.
     *
     * @return a new Money instance with negated amount
     */
    public Money negate() {
        return new Money(this.amount.negate(), this.currencyCode);
    }

    /**
     * Returns the absolute value of this Money.
     *
     * @return a new Money instance with absolute amount
     */
    public Money abs() {
        return new Money(this.amount.abs(), this.currencyCode);
    }

    /**
     * Returns true if this Money has a zero amount.
     *
     * @return true if amount is zero
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Returns true if this Money has a positive amount.
     *
     * @return true if amount is greater than zero
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns true if this Money has a negative amount.
     *
     * @return true if amount is less than zero
     */
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Returns true if this Money has a positive or zero amount.
     *
     * @return true if amount is greater than or equal to zero
     */
    public boolean isPositiveOrZero() {
        return this.amount.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Compares this Money with another Money.
     * Both monies must have the same currency.
     *
     * @param other the Money to compare with
     * @return negative if less, zero if equal, positive if greater
     * @throws IllegalArgumentException if currencies don't match
     */
    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "Other money must not be null");
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    /**
     * Returns true if this Money is greater than the other Money.
     *
     * @param other the Money to compare with
     * @return true if this is greater than other
     */
    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    /**
     * Returns true if this Money is greater than or equal to the other Money.
     *
     * @param other the Money to compare with
     * @return true if this is greater than or equal to other
     */
    public boolean isGreaterThanOrEqualTo(Money other) {
        return compareTo(other) >= 0;
    }

    /**
     * Returns true if this Money is less than the other Money.
     *
     * @param other the Money to compare with
     * @return true if this is less than other
     */
    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    /**
     * Returns true if this Money is less than or equal to the other Money.
     *
     * @param other the Money to compare with
     * @return true if this is less than or equal to other
     */
    public boolean isLessThanOrEqualTo(Money other) {
        return compareTo(other) <= 0;
    }

    /**
     * Returns the minimum of this Money and the other Money.
     *
     * @param other the Money to compare with
     * @return the smaller Money
     */
    public Money min(Money other) {
        return isLessThanOrEqualTo(other) ? this : other;
    }

    /**
     * Returns the maximum of this Money and the other Money.
     *
     * @param other the Money to compare with
     * @return the larger Money
     */
    public Money max(Money other) {
        return isGreaterThanOrEqualTo(other) ? this : other;
    }

    /**
     * Converts this Money to a different currency using the provided converter.
     *
     * @param targetCurrencyCode the target currency code
     * @param converter          the currency converter to use
     * @return a new Money instance in the target currency
     */
    public Money convertTo(String targetCurrencyCode, CurrencyConverter converter) {
        Objects.requireNonNull(targetCurrencyCode, "Target currency code must not be null");
        Objects.requireNonNull(converter, "Converter must not be null");
        if (this.currencyCode.equals(targetCurrencyCode)) {
            return this;
        }
        BigDecimal convertedAmount = converter.convert(this.amount, this.currencyCode, targetCurrencyCode);
        return new Money(convertedAmount, targetCurrencyCode);
    }

    /**
     * Returns this Money with the specified percentage added.
     *
     * @param percentage the percentage to add (e.g., 10 for 10%)
     * @return a new Money instance with percentage added
     */
    public Money addPercentage(BigDecimal percentage) {
        Objects.requireNonNull(percentage, "Percentage must not be null");
        BigDecimal multiplier = BigDecimal.ONE.add(percentage.divide(BigDecimal.valueOf(100), DEFAULT_SCALE, DEFAULT_ROUNDING));
        return multiply(multiplier);
    }

    /**
     * Returns this Money with the specified percentage subtracted.
     *
     * @param percentage the percentage to subtract (e.g., 10 for 10%)
     * @return a new Money instance with percentage subtracted
     */
    public Money subtractPercentage(BigDecimal percentage) {
        Objects.requireNonNull(percentage, "Percentage must not be null");
        BigDecimal multiplier = BigDecimal.ONE.subtract(percentage.divide(BigDecimal.valueOf(100), DEFAULT_SCALE, DEFAULT_ROUNDING));
        return multiply(multiplier);
    }

    /**
     * Calculates the percentage of this Money.
     *
     * @param percentage the percentage to calculate (e.g., 10 for 10%)
     * @return a new Money instance representing the percentage
     */
    public Money percentage(BigDecimal percentage) {
        Objects.requireNonNull(percentage, "Percentage must not be null");
        return multiply(percentage.divide(BigDecimal.valueOf(100), DEFAULT_SCALE, DEFAULT_ROUNDING));
    }

    /**
     * Normalizes the amount to the default scale with proper rounding.
     *
     * @param amount the amount to normalize
     * @return normalized amount
     */
    private static BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Validates the currency code against ISO 4217.
     *
     * @param currencyCode the currency code to validate
     * @return the validated currency code
     * @throws IllegalArgumentException if currency code is invalid
     */
    private static String validateCurrencyCode(String currencyCode) {
        String upperCode = currencyCode.toUpperCase();
        try {
            Currency.getInstance(upperCode);
            return upperCode;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code: " + currencyCode, e);
        }
    }

    /**
     * Asserts that the other Money has the same currency.
     *
     * @param other the other Money to check
     * @throws IllegalArgumentException if currencies don't match
     */
    private void assertSameCurrency(Money other) {
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException(
                    String.format("Currency mismatch: %s vs %s", this.currencyCode, other.currencyCode)
            );
        }
    }
}
