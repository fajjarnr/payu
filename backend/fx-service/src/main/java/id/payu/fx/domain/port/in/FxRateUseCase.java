package id.payu.fx.domain.port.in;

import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.model.FxRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface FxRateUseCase {

    FxRate getCurrentRate(String fromCurrency, String toCurrency);

    FxRate saveRate(FxRate fxRate);

    void updateRates();

    List<FxRate> getAllRates();

    FxConversion convertCurrency(String accountId, String fromCurrency, String toCurrency, BigDecimal amount);
}
