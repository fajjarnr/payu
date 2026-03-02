package id.payu.wallet.adapter.client;

import id.payu.wallet.adapter.messaging.fx.FxRateCache;
import id.payu.wallet.domain.model.FxRateInfo;
import id.payu.wallet.domain.port.out.FxRateProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * FX rate provider adapter that uses local cache populated from Kafka events.
 * This breaks the circular dependency between wallet-service and fx-service.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FxRateProviderAdapter implements FxRateProviderPort {

    private final FxRateCache fxRateCache;

    @Override
    public Optional<FxRateInfo> getCurrentRate(String fromCurrency, String toCurrency) {
        log.debug("Getting FX rate from cache for {}/{}", fromCurrency, toCurrency);

        BigDecimal rate = fxRateCache.getRate(fromCurrency, toCurrency);

        if (rate == null) {
            log.warn("FX rate not found in cache for {}/{}", fromCurrency, toCurrency);
            return Optional.empty();
        }

        // Calculate inverse rate
        BigDecimal inverseRate = BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_UP);

        FxRateInfo info = new FxRateInfo(
                UUID.randomUUID(),
                fromCurrency,
                toCurrency,
                rate,
                inverseRate,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15)
        );

        log.debug("Returning FX rate from cache: {}/{} = {}", fromCurrency, toCurrency, rate);
        return Optional.of(info);
    }
}
