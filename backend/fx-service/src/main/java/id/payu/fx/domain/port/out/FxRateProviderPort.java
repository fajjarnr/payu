package id.payu.fx.domain.port.out;

import id.payu.fx.domain.model.FxRate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Output port for FX rate provider operations.
 * Fetches current FX rates from external providers.
 */
public interface FxRateProviderPort {

    /**
     * Fetch current FX rate for a currency pair.
     *
     * @param fromCurrency the source currency code (e.g., "USD")
     * @param toCurrency the target currency code (e.g., "IDR")
     * @return the current FX rate
     * @throws RuntimeException if rate is not available
     */
    FxRate fetchCurrentRate(String fromCurrency, String toCurrency);

    /**
     * Fetch all FX rates for a base currency.
     *
     * @param baseCurrency the base currency code
     * @return map of target currency to exchange rate
     */
    Map<String, BigDecimal> fetchAllRates(String baseCurrency);

    /**
     * Check if the FX rate provider is available.
     *
     * @return true if provider is available, false otherwise
     */
    boolean isAvailable();
}
