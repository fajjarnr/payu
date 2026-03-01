package id.payu.wallet.adapter.messaging.fx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Event consumed when FX rates are updated.
 * Used to break circular dependency between fx-service and wallet-service.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxRatesUpdatedEvent {

    /**
     * Event ID for idempotency
     */
    private String eventId;

    /**
     * Timestamp when the event was published
     */
    private Instant timestamp;

    /**
     * List of updated FX rates
     */
    private List<FxRateDto> rates;

    /**
     * Base currency for the rates
     */
    private String baseCurrency;

    /**
     * DTO for FX rate data in events
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FxRateDto {
        private String fromCurrency;
        private String toCurrency;
        private String rate;
        private Instant validFrom;
        private Instant validUntil;
    }
}
