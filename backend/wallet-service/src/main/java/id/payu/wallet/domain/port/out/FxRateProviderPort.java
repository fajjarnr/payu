package id.payu.wallet.domain.port.out;

import id.payu.wallet.domain.model.FxRateInfo;

import java.util.Optional;

/**
 * Output port for foreign exchange (FX) rate provider operations.
 * Provides real-time and historical FX rates for currency conversion.
 */
public interface FxRateProviderPort {

    /**
     * Get the current FX rate between two currencies.
     *
     * @param fromCurrency the source currency code (e.g., "USD", "IDR")
     * @param toCurrency the target currency code (e.g., "USD", "IDR")
     * @return Optional containing the FX rate info if available, empty otherwise
     */
    Optional<FxRateInfo> getCurrentRate(String fromCurrency, String toCurrency);
}
