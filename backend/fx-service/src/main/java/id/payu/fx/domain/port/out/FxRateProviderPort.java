package id.payu.fx.domain.port.out;

import id.payu.fx.domain.model.FxRate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Output port for FX rate provider operations.
 * Fetches current FX rates from external providers.
 */
public interface FxRateProviderPort {

    FxRate fetchCurrentRate(String fromCurrency, String toCurrency);

    Map<String, BigDecimal> fetchAllRates(String baseCurrency);

    boolean isAvailable();
}
