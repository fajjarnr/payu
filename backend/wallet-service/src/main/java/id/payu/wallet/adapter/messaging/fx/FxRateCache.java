package id.payu.wallet.adapter.messaging.fx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local cache for FX rates consumed from Kafka.
 * Used to break circular dependency between wallet-service and fx-service.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Slf4j
@Component
public class FxRateCache {

    private final Map<String, FxRateEntry> rateCache = new ConcurrentHashMap<>();

    /**
     * Get cached FX rate for a currency pair.
     *
     * @param fromCurrency source currency
     * @param toCurrency   target currency
     * @return FX rate or null if not cached
     */
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        String key = buildKey(fromCurrency, toCurrency);
        FxRateEntry entry = rateCache.get(key);

        if (entry == null) {
            log.debug("FX rate not found in cache for {}/{}", fromCurrency, toCurrency);
            return null;
        }

        if (entry.isExpired()) {
            log.debug("FX rate expired for {}/{}", fromCurrency, toCurrency);
            rateCache.remove(key);
            return null;
        }

        log.debug("FX rate found in cache for {}/{}: {}", fromCurrency, toCurrency, entry.getRate());
        return entry.getRate();
    }

    /**
     * Update rates from event.
     *
     * @param event the FX rates updated event
     */
    public void updateRates(FxRatesUpdatedEvent event) {
        if (event.getRates() == null || event.getRates().isEmpty()) {
            log.debug("No rates to update in cache");
            return;
        }

        for (FxRatesUpdatedEvent.FxRateDto rateDto : event.getRates()) {
            try {
                String key = buildKey(rateDto.getFromCurrency(), rateDto.getToCurrency());
                BigDecimal rate = new BigDecimal(rateDto.getRate());
                Instant validUntil = rateDto.getValidUntil() != null ?
                        rateDto.getValidUntil() : Instant.now().plusSeconds(900); // 15 min default

                FxRateEntry entry = new FxRateEntry(rate, validUntil);
                rateCache.put(key, entry);

                log.debug("Updated FX rate in cache: {}/{} = {}",
                        rateDto.getFromCurrency(), rateDto.getToCurrency(), rate);

            } catch (NumberFormatException e) {
                log.warn("Invalid rate value: {}", rateDto.getRate());
            }
        }

        log.info("Updated {} FX rates in cache", event.getRates().size());
    }

    /**
     * Clear all cached rates.
     */
    public void clear() {
        rateCache.clear();
        log.info("FX rate cache cleared");
    }

    /**
     * Get cache size.
     */
    public int size() {
        // Clean expired entries first
        rateCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return rateCache.size();
    }

    private String buildKey(String fromCurrency, String toCurrency) {
        return fromCurrency + "/" + toCurrency;
    }

    /**
     * Entry in the FX rate cache.
     */
    private static class FxRateEntry {
        private final BigDecimal rate;
        private final Instant validUntil;

        FxRateEntry(BigDecimal rate, Instant validUntil) {
            this.rate = rate;
            this.validUntil = validUntil;
        }

        BigDecimal getRate() {
            return rate;
        }

        boolean isExpired() {
            return Instant.now().isAfter(validUntil);
        }
    }
}
