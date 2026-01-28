package id.payu.transaction.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Value Object representing monetary amounts.
 *
 * <p>This class encapsulates both the amount and currency, ensuring that
 * arithmetic operations are only performed on amounts with the same currency.
 * It uses BigDecimal internally to avoid floating-point precision issues.</p>
 *
 * <p>PCI-DSS Compliance:</p>
 * <ul>
 *   <li>All financial calculations use precise decimal arithmetic</li>
 *   <li>No floating-point operations that could lead to rounding errors</li>
 *   <li>Immutable to prevent accidental modifications</li>
 * </ul>
 *
 * @see java.math.BigDecimal
 * @see java.util.Currency
 */
@EqualsAndHashCode
@Getter
public class Money {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(SCALE, ROUNDING_MODE);
        this.currency = currency;
    }

    /**
     * Creates a Money instance with the specified amount and currency code.
     *
     * @param amount the monetary amount
     * @param currencyCode the ISO 4217 currency code (e.g., "USD", "IDR")
     * @return a new Money instance
     * @throws IllegalArgumentException if amount is null or currency code is invalid
     * @throws IllegalArgumentException if amount has more than 2 decimal places
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                "Amount cannot have more than 2 decimal places. Scale was: " + amount.scale());
        }

        Currency currency = Currency.getInstance(currencyCode);
        return new Money(amount, currency);
    }

    /**
     * Creates a Money instance from a string amount and currency code.
     *
     * @param amount the monetary amount as a string
     * @param currencyCode the ISO 4217 currency code
     * @return a new Money instance
     */
    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    /**
     * Creates a Money instance with Indonesian Rupiah (IDR) currency.
     *
     * @param amount the monetary amount
     * @return a new Money instance in IDR
     */
    public static Money idr(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("IDR"));
    }

    /**
     * Creates a Money instance with Indonesian Rupiah (IDR) currency from string.
     *
     * @param amount the monetary amount as a string
     * @return a new Money instance in IDR
     */
    public static Money idr(String amount) {
        return new Money(new BigDecimal(amount), Currency.getInstance("IDR"));
    }

    /**
     * Creates a Money instance with USD currency.
     *
     * @param amount the monetary amount
     * @return a new Money instance in USD
     */
    public static Money usd(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("USD"));
    }

    /**
     * Adds two Money instances of the same currency.
     *
     * @param other the Money instance to add
     * @return a new Money instance representing the sum
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another Money instance from this one.
     *
     * @param other the Money instance to subtract
     * @return a new Money instance representing the difference
     * @throws IllegalArgumentException if currencies don't match
     * @throws IllegalArgumentException if result would be negative
     */
    public Money subtract(Money other) {
        assertSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Result cannot be negative");
        }
        return new Money(result, this.currency);
    }

    /**
     * Multiplies this Money instance by a scalar.
     *
     * @param multiplier the multiplication factor
     * @return a new Money instance representing the product
     * @throws IllegalArgumentException if multiplier is negative
     */
    public Money multiply(BigDecimal multiplier) {
        if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Multiplier cannot be negative");
        }
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    /**
     * Multiplies this Money instance by an integer scalar.
     *
     * @param multiplier the multiplication factor
     * @return a new Money instance representing the product
     */
    public Money multiply(int multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("Multiplier cannot be negative");
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    /**
     * Divides this Money instance by a scalar, using banker's rounding.
     *
     * @param divisor the division factor
     * @return a new Money instance representing the quotient
     * @throws IllegalArgumentException if divisor is null or zero
     */
    public Money divide(BigDecimal divisor) {
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Divisor cannot be null or zero");
        }
        return new Money(this.amount.divide(divisor, SCALE, ROUNDING_MODE), this.currency);
    }

    /**
     * Checks if this Money instance is greater than another.
     *
     * @param other the Money instance to compare with
     * @return true if this amount is greater than the other
     * @throws IllegalArgumentException if currencies don't match
     */
    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Checks if this Money instance is greater than or equal to another.
     *
     * @param other the Money instance to compare with
     * @return true if this amount is greater than or equal to the other
     * @throws IllegalArgumentException if currencies don't match
     */
    public boolean isGreaterThanOrEqualTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    /**
     * Checks if this Money instance is less than another.
     *
     * @param other the Money instance to compare with
     * @return true if this amount is less than the other
     * @throws IllegalArgumentException if currencies don't match
     */
    public boolean isLessThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Checks if this Money instance is less than or equal to another.
     *
     * @param other the Money instance to compare with
     * @return true if this amount is less than or equal to the other
     * @throws IllegalArgumentException if currencies don't match
     */
    public boolean isLessThanOrEqualTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) <= 0;
    }

    /**
     * Checks if this Money instance is zero.
     *
     * @return true if the amount is zero
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if this Money instance is positive (greater than zero).
     *
     * @return true if the amount is positive
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this Money instance is negative (less than zero).
     *
     * @return true if the amount is negative
     */
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Returns the absolute value of this Money instance.
     *
     * @return a new Money instance with the absolute value
     */
    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    /**
     * Negates this Money instance (multiplies by -1).
     *
     * @return a new Money instance with the negated value
     */
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    /**
     * Returns a percentage of this Money instance.
     *
     * @param percentage the percentage (e.g., 10 for 10%)
     * @return a new Money instance representing the percentage
     */
    public Money percentage(BigDecimal percentage) {
        return multiply(percentage).divide(BigDecimal.valueOf(100));
    }

    /**
     * Returns a percentage of this Money instance.
     *
     * @param percentage the percentage (e.g., 10 for 10%)
     * @return a new Money instance representing the percentage
     */
    public Money percentage(int percentage) {
        return multiply(percentage).divide(BigDecimal.valueOf(100));
    }

    /**
     * Formats this Money instance as a string for display.
     *
     * @return a formatted string (e.g., "IDR 10,000.00" or "$100.00")
     */
    public String format() {
        String symbol = currency.getSymbol();
        String formattedAmount = String.format("%,.2f", amount);
        return symbol + " " + formattedAmount;
    }

    /**
     * Asserts that two Money instances have the same currency.
     *
     * @param other the other Money instance
     * @throws IllegalArgumentException if currencies don't match
     */
    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Currency mismatch: cannot operate on " + this.currency.getCurrencyCode() +
                " and " + other.currency.getCurrencyCode()
            );
        }
    }

    @Override
    public String toString() {
        return "Money{" +
                "amount=" + amount +
                ", currency=" + currency.getCurrencyCode() +
                '}';
    }
}
