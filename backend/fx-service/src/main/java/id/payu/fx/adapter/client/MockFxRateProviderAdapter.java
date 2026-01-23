package id.payu.fx.adapter.client;

import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.out.FxRateProviderPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockFxRateProviderAdapter implements FxRateProviderPort {

    private static final Map<String, BigDecimal> MOCK_RATES = new ConcurrentHashMap<>();

    static {
        MOCK_RATES.put("IDR-USD", new BigDecimal("0.000065"));
        MOCK_RATES.put("IDR-EUR", new BigDecimal("0.000059"));
        MOCK_RATES.put("IDR-SGD", new BigDecimal("0.000087"));
        MOCK_RATES.put("IDR-JPY", new BigDecimal("0.0095"));
        MOCK_RATES.put("IDR-GBP", new BigDecimal("0.000051"));
        MOCK_RATES.put("IDR-AUD", new BigDecimal("0.000099"));
        MOCK_RATES.put("IDR-CNY", new BigDecimal("0.00046"));

        MOCK_RATES.put("USD-IDR", new BigDecimal("15384.62"));
        MOCK_RATES.put("EUR-IDR", new BigDecimal("16949.15"));
        MOCK_RATES.put("SGD-IDR", new BigDecimal("11494.25"));
        MOCK_RATES.put("JPY-IDR", new BigDecimal("105.26"));
        MOCK_RATES.put("GBP-IDR", new BigDecimal("19607.84"));
        MOCK_RATES.put("AUD-IDR", new BigDecimal("10101.01"));
        MOCK_RATES.put("CNY-IDR", new BigDecimal("2173.91"));

        MOCK_RATES.put("USD-EUR", new BigDecimal("0.91"));
        MOCK_RATES.put("EUR-USD", new BigDecimal("1.10"));
        MOCK_RATES.put("USD-GBP", new BigDecimal("0.79"));
        MOCK_RATES.put("GBP-USD", new BigDecimal("1.27"));
        MOCK_RATES.put("USD-JPY", new BigDecimal("146.00"));
        MOCK_RATES.put("JPY-USD", new BigDecimal("0.00685"));
    }

    @Override
    public FxRate fetchCurrentRate(String fromCurrency, String toCurrency) {
        String key = fromCurrency + "-" + toCurrency;
        
        if (!MOCK_RATES.containsKey(key)) {
            throw new RuntimeException("No rate available for " + key);
        }

        BigDecimal rate = MOCK_RATES.get(key);
        BigDecimal inverseRate = BigDecimal.ONE.divide(rate, 8, java.math.RoundingMode.HALF_UP);

        return FxRate.builder()
                .id(UUID.randomUUID())
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(rate)
                .inverseRate(inverseRate)
                .build();
    }

    @Override
    public Map<String, BigDecimal> fetchAllRates(String baseCurrency) {
        return MOCK_RATES.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(baseCurrency))
                .collect(ConcurrentHashMap::new, 
                        (map, entry) -> map.put(entry.getKey().split("-")[1], entry.getValue()),
                        ConcurrentHashMap::putAll);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
