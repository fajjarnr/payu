package id.payu.quarkus.commons.money;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

@JsonSerialize(using = MoneySerializer.class)
@JsonDeserialize(using = MoneyDeserializer.class)
public final class Money implements Serializable, Comparable<Money> {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_SCALE = 2;
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;
    public static final String DEFAULT_CURRENCY_CODE = "IDR";
    public static final Money ZERO = of(BigDecimal.ZERO, DEFAULT_CURRENCY_CODE);

    private final BigDecimal amount;
    private final String currencyCode;
    private transient Currency currency;

    private Money(BigDecimal amount, String currencyCode) {
        this.amount = normalizeAmount(amount);
        this.currencyCode = validateCurrencyCode(currencyCode);
    }

    @SuppressWarnings("unused")
    private Money() {
        this.amount = null;
        this.currencyCode = null;
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currencyCode, "Currency code must not be null");
        return new Money(amount, currencyCode);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        return new Money(amount, currency.getCurrencyCode());
    }

    public static Money of(BigDecimal amount) {
        return of(amount, DEFAULT_CURRENCY_CODE);
    }

    public static Money of(String amount, String currencyCode) {
        Objects.requireNonNull(amount, "Amount must not be null");
        try {
            return of(new BigDecimal(amount), currencyCode);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + amount, e);
        }
    }

    public BigDecimal getAmount() { return amount; }
    public String getCurrencyCode() { return currencyCode; }

    public Currency getCurrency() {
        if (currency == null) {
            currency = Currency.getInstance(currencyCode);
        }
        return currency;
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "Other money must not be null");
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Other money must not be null");
        assertSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient amount: cannot subtract " + other + " from " + this);
        }
        return new Money(result, this.currencyCode);
    }

    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "Multiplier must not be null");
        return new Money(
                this.amount.multiply(multiplier).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING),
                this.currencyCode);
    }

    public Money multiply(long multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "Divisor must not be null");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return new Money(
                this.amount.divide(divisor, DEFAULT_SCALE, DEFAULT_ROUNDING),
                this.currencyCode);
    }

    public Money divide(long divisor) {
        if (divisor == 0) throw new ArithmeticException("Cannot divide by zero");
        return divide(BigDecimal.valueOf(divisor));
    }

    public Money negate() { return new Money(this.amount.negate(), this.currencyCode); }
    public Money abs() { return new Money(this.amount.abs(), this.currencyCode); }
    public boolean isZero() { return this.amount.compareTo(BigDecimal.ZERO) == 0; }
    public boolean isPositive() { return this.amount.compareTo(BigDecimal.ZERO) > 0; }
    public boolean isNegative() { return this.amount.compareTo(BigDecimal.ZERO) < 0; }
    public boolean isPositiveOrZero() { return this.amount.compareTo(BigDecimal.ZERO) >= 0; }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "Other money must not be null");
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    public boolean isGreaterThan(Money other) { return compareTo(other) > 0; }
    public boolean isGreaterThanOrEqualTo(Money other) { return compareTo(other) >= 0; }
    public boolean isLessThan(Money other) { return compareTo(other) < 0; }
    public boolean isLessThanOrEqualTo(Money other) { return compareTo(other) <= 0; }
    public Money min(Money other) { return isLessThanOrEqualTo(other) ? this : other; }
    public Money max(Money other) { return isGreaterThanOrEqualTo(other) ? this : other; }

    public Money convertTo(String targetCurrencyCode, CurrencyConverter converter) {
        Objects.requireNonNull(targetCurrencyCode, "Target currency code must not be null");
        Objects.requireNonNull(converter, "Converter must not be null");
        if (this.currencyCode.equals(targetCurrencyCode)) return this;
        BigDecimal convertedAmount = converter.convert(this.amount, this.currencyCode, targetCurrencyCode);
        return new Money(convertedAmount, targetCurrencyCode);
    }

    public Money addPercentage(BigDecimal percentage) {
        Objects.requireNonNull(percentage, "Percentage must not be null");
        BigDecimal multiplier = BigDecimal.ONE.add(percentage.divide(BigDecimal.valueOf(100), DEFAULT_SCALE, DEFAULT_ROUNDING));
        return multiply(multiplier);
    }

    public Money subtractPercentage(BigDecimal percentage) {
        Objects.requireNonNull(percentage, "Percentage must not be null");
        BigDecimal multiplier = BigDecimal.ONE.subtract(percentage.divide(BigDecimal.valueOf(100), DEFAULT_SCALE, DEFAULT_ROUNDING));
        return multiply(multiplier);
    }

    public Money percentage(BigDecimal percentage) {
        Objects.requireNonNull(percentage, "Percentage must not be null");
        return multiply(percentage.divide(BigDecimal.valueOf(100), DEFAULT_SCALE, DEFAULT_ROUNDING));
    }

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    private static String validateCurrencyCode(String currencyCode) {
        String upperCode = currencyCode.toUpperCase();
        try {
            Currency.getInstance(upperCode);
            return upperCode;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code: " + currencyCode, e);
        }
    }

    private void assertSameCurrency(Money other) {
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException(
                    String.format("Currency mismatch: %s vs %s", this.currencyCode, other.currencyCode));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0 && currencyCode.equals(money.currencyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currencyCode);
    }

    @Override
    public String toString() {
        return currencyCode + " " + amount.toPlainString();
    }
}
