package id.payu.fx.domain.port.in;

import id.payu.fx.domain.model.SettlementFxRate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for settlement FX operations (GAP-010).
 * Defines use cases for multi-currency settlement with rate locking.
 */
public interface SettlementFxUseCase {

    /**
     * Lock FX rate for settlement (15-minute window).
     */
    SettlementFxRate lockRateForSettlement(String partnerId, String fromCurrency,
                                            String toCurrency, BigDecimal rate,
                                            String settlementBatchId);

    /**
     * Get locked rate by ID.
     */
    SettlementFxRate getLockedRate(UUID rateId);

    /**
     * Get locked rate for settlement batch.
     */
    Optional<SettlementFxRate> getLockedRateForSettlement(String settlementBatchId);

    /**
     * Validate if locked rate is still valid.
     */
    boolean isRateValid(UUID rateId);

    /**
     * Convert amount using locked rate.
     */
    BigDecimal convertWithLockedRate(UUID rateId, BigDecimal amount);

    /**
     * Invalidate/expired rate lock.
     */
    void invalidateRate(UUID rateId);

    /**
     * Get partner's preferred settlement currency.
     */
    String getPartnerSettlementCurrency(String partnerId);

    /**
     * Set partner's preferred settlement currency.
     */
    void setPartnerSettlementCurrency(String partnerId, String currency);

    /**
     * Auto-convert settlement amount to partner's preferred currency.
     */
    BigDecimal autoConvertForSettlement(String partnerId, BigDecimal amount,
                                         String sourceCurrency, String settlementBatchId);
}
