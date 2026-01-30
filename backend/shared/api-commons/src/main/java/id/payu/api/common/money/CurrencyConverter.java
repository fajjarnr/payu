package id.payu.api.common.money;

import java.math.BigDecimal;

/**
 * Interface for currency conversion operations.
 * <p>
 * Implementations of this interface provide the logic for converting monetary amounts
 * between different currencies. This is typically implemented by services that
 * interact with external exchange rate providers or internal rate repositories.
 *
 * <p>Example implementation:
 * <pre>
 * public class FxServiceCurrencyConverter implements CurrencyConverter {
 *     private final FxRateRepository fxRateRepository;
 *
 *     public FxServiceCurrencyConverter(FxRateRepository fxRateRepository) {
 *         this.fxRateRepository = fxRateRepository;
 *     }
 *
 *     @Override
 *     public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
 *         FxRate rate = fxRateRepository.getLatestRate(fromCurrency, toCurrency);
 *         return amount.multiply(rate.getMidRate());
 *     }
 * }
 * </pre>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 * @see Money#convertTo(String, CurrencyConverter)
 */
@FunctionalInterface
public interface CurrencyConverter {

    /**
     * Converts an amount from one currency to another.
     *
     * @param amount       the amount to convert (in fromCurrency)
     * @param fromCurrency the source currency code (ISO 4217)
     * @param toCurrency   the target currency code (ISO 4217)
     * @return the converted amount in the target currency
     * @throws IllegalArgumentException if currency codes are invalid or conversion not supported
     * @throws IllegalStateException    if exchange rate is not available
     */
    BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency);

    /**
     * Converts an amount from one currency to another using a specific rate.
     *
     * @param amount the amount to convert
     * @param rate   the exchange rate (amount in target currency = amount * rate)
     * @return the converted amount
     * @throws IllegalArgumentException if rate is null or negative
     */
    default BigDecimal convertWithRate(BigDecimal amount, BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Exchange rate must not be null");
        }
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
        return amount.multiply(rate);
    }

    /**
     * Gets the exchange rate between two currencies.
     *
     * @param fromCurrency the source currency code (ISO 4217)
     * @param toCurrency   the target currency code (ISO 4217)
     * @return the exchange rate (amount in target currency = amount * rate)
     * @throws IllegalArgumentException if currency codes are invalid
     * @throws IllegalStateException    if exchange rate is not available
     */
    default BigDecimal getRate(String fromCurrency, String toCurrency) {
        // Default implementation converts 1 unit
        return convert(BigDecimal.ONE, fromCurrency, toCurrency);
    }
}
