package id.payu.fx.domain.port.out;

import id.payu.fx.domain.model.FxRate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Output port for FX rate persistence operations.
 * Manages foreign exchange rate data storage and retrieval.
 */
public interface FxRateRepositoryPort {

    FxRate save(FxRate fxRate);

    Optional<FxRate> findLatestRate(String fromCurrency, String toCurrency, LocalDateTime timestamp);

    List<FxRate> findRatesByCurrencyPair(String fromCurrency, String toCurrency);

    List<FxRate> findAll();

    void deleteExpiredRates(LocalDateTime before);
}
