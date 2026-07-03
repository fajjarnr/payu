package id.payu.fx.adapter.client;

import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.out.FxRateProviderPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// ponytail: stub provider. Replace with real API (XE/Reuters) when integration needed.
@Component
@ConditionalOnMissingBean(name = "realFxRateProvider")
public class StubFxRateProviderAdapter implements FxRateProviderPort {

    private static final Map<String, BigDecimal> RATES = Map.of(
        "USD", new BigDecimal("16000.00"),
        "EUR", new BigDecimal("17500.00"),
        "SGD", new BigDecimal("12000.00"),
        "JPY", new BigDecimal("110.00"),
        "GBP", new BigDecimal("20500.00")
    );

    @Override
    public FxRate fetchCurrentRate(String fromCurrency, String toCurrency) {
        BigDecimal rate = RATES.getOrDefault(fromCurrency.toUpperCase(), BigDecimal.ONE);
        return FxRate.builder()
            .fromCurrency(fromCurrency)
            .toCurrency(toCurrency)
            .rate(rate)
            .inverseRate(BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_EVEN))
            .validFrom(LocalDateTime.now())
            .validUntil(LocalDateTime.now().plusHours(1))
            .build();
    }

    @Override
    public Map<String, BigDecimal> fetchAllRates(String baseCurrency) {
        return new HashMap<>(RATES);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
