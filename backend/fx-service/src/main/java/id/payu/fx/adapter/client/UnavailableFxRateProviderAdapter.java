package id.payu.fx.adapter.client;

import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.out.FxRateProviderPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Fail-closed provider used outside local/test until a real, audited provider is configured.
 */
@Component
@Profile("!local & !test")
@ConditionalOnMissingBean(name = "realFxRateProvider")
public class UnavailableFxRateProviderAdapter implements FxRateProviderPort {

    @Override
    public FxRate fetchCurrentRate(String fromCurrency, String toCurrency) {
        throw unavailable();
    }

    @Override
    public Map<String, BigDecimal> fetchAllRates(String baseCurrency) {
        throw unavailable();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    private IllegalStateException unavailable() {
        return new IllegalStateException("No production FX rate provider configured");
    }
}
