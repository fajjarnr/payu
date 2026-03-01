package id.payu.fx.adapter.messaging;

import id.payu.fx.domain.model.FxRate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Event published when FX rates are updated.
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

    /**
     * Convert domain FxRate to DTO
     */
    public static FxRateDto fromDomain(FxRate fxRate) {
        return FxRateDto.builder()
                .fromCurrency(fxRate.getFromCurrency())
                .toCurrency(fxRate.getToCurrency())
                .rate(fxRate.getRate().toPlainString())
                .validFrom(fxRate.getValidFrom() != null ?
                        fxRate.getValidFrom().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .validUntil(fxRate.getValidUntil() != null ?
                        fxRate.getValidUntil().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
                .build();
    }
}
