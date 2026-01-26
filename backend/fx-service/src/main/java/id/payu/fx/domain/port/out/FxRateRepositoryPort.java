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

    /**
     * Save an FX rate.
     *
     * @param fxRate the FX rate to save
     * @return the saved FX rate
     */
    FxRate save(FxRate fxRate);

    /**
     * Find the latest valid rate for a currency pair.
     *
     * @param fromCurrency the source currency
     * @param toCurrency the target currency
     * @param timestamp the timestamp to check validity
     * @return optional containing the FX rate if found
     */
    Optional<FxRate> findLatestRate(String fromCurrency, String toCurrency, LocalDateTime timestamp);

    /**
     * Find all rates for a specific currency pair.
     *
     * @param fromCurrency the source currency
     * @param toCurrency the target currency
     * @return list of FX rates for the currency pair
     */
    List<FxRate> findRatesByCurrencyPair(String fromCurrency, String toCurrency);

    /**
     * Find all FX rates.
     *
     * @return list of all FX rates
     */
    List<FxRate> findAll();

    /**
     * Delete expired FX rates.
     *
     * @param before delete rates valid before this timestamp
     */
    void deleteExpiredRates(LocalDateTime before);
}
