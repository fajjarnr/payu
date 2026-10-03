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

    SettlementFxRate getLockedRate(UUID rateId);

    Optional<SettlementFxRate> getLockedRateForSettlement(String settlementBatchId);

    boolean isRateValid(UUID rateId);

    BigDecimal convertWithLockedRate(UUID rateId, BigDecimal amount);

    void invalidateRate(UUID rateId);

    String getPartnerSettlementCurrency(String partnerId);

    void setPartnerSettlementCurrency(String partnerId, String currency);

    BigDecimal autoConvertForSettlement(String partnerId, BigDecimal amount,
                                         String sourceCurrency, String settlementBatchId);
}
