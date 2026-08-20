package id.payu.fx.adapter.client;

import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.out.FxRateProviderPort;
import id.payu.fx.application.service.FxRateNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// ponytail: local-only stub; production uses the configured provider or fails closed.
@Component
@Profile("local")
@ConditionalOnMissingBean(name = "realFxRateProvider")
public class StubFxRateProviderAdapter implements FxRateProviderPort {

    private static final Map<String, BigDecimal> RATES = Map.of(
        "USD", new BigDecimal("16000.0000"),
        "EUR", new BigDecimal("17500.0000"),
        "SGD", new BigDecimal("12000.0000"),
        "JPY", new BigDecimal("110.0000"),
        "GBP", new BigDecimal("20500.0000")
    );

    @Override
    public FxRate fetchCurrentRate(String fromCurrency, String toCurrency) {
        String from = fromCurrency.toUpperCase(Locale.ROOT);
        String to = toCurrency.toUpperCase(Locale.ROOT);
        BigDecimal rate = resolveRate(from, to);
        LocalDateTime observedAt = LocalDateTime.now();
        return FxRate.builder()
            .fromCurrency(from)
            .toCurrency(to)
            .rate(rate.setScale(4, RoundingMode.HALF_EVEN))
            .inverseRate(BigDecimal.ONE.divide(rate, 4, RoundingMode.HALF_EVEN))
            .validFrom(LocalDateTime.now())
            .validUntil(observedAt.plusHours(1))
            .source("local-stub")
            .observedAt(observedAt)
            .build();
    }

    @Override
    public Map<String, BigDecimal> fetchAllRates(String baseCurrency) {
        if (!"IDR".equalsIgnoreCase(baseCurrency)) {
            throw new FxRateNotFoundException(
                    "No stub rates available for base currency " + baseCurrency);
        }
        Map<String, BigDecimal> rates = new HashMap<>();
        RATES.forEach((currency, idrRate) ->
                rates.put(currency, BigDecimal.ONE.divide(idrRate, 4, RoundingMode.HALF_EVEN)));
        return rates;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private BigDecimal resolveRate(String fromCurrency, String toCurrency) {
        if ("IDR".equals(toCurrency)) {
            BigDecimal idrRate = RATES.get(fromCurrency);
            if (idrRate != null) {
                return idrRate;
            }
        }
        if ("IDR".equals(fromCurrency)) {
            BigDecimal idrRate = RATES.get(toCurrency);
            if (idrRate != null) {
                return BigDecimal.ONE.divide(idrRate, 4, RoundingMode.HALF_EVEN);
            }
        }
        return throwUnknownPair(fromCurrency, toCurrency);
    }

    private BigDecimal throwUnknownPair(String fromCurrency, String toCurrency) {
        throw new FxRateNotFoundException(
                "No stub rate available for " + fromCurrency + "-" + toCurrency);
    }
}
