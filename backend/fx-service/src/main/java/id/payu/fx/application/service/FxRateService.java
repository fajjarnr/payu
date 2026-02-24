package id.payu.fx.application.service;

import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.in.FxRateUseCase;
import id.payu.fx.domain.port.out.FxRateProviderPort;
import id.payu.fx.domain.port.out.FxRateRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FxRateService implements FxRateUseCase {

    private static final Logger log = LoggerFactory.getLogger(FxRateService.class);

    private final FxRateRepositoryPort fxRateRepository;
    private final FxRateProviderPort fxRateProvider;

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("IDR", "USD", "EUR", "SGD", "JPY", "GBP", "AUD", "CNY");
    private static final String BASE_CURRENCY = "IDR";
    private static final long RATE_VALIDITY_MINUTES = 15;

    public FxRateService(FxRateRepositoryPort fxRateRepository, FxRateProviderPort fxRateProvider) {
        this.fxRateRepository = fxRateRepository;
        this.fxRateProvider = fxRateProvider;
    }

    @Override
    public FxRate getCurrentRate(String fromCurrency, String toCurrency) {
        validateCurrencyPair(fromCurrency, toCurrency);
        
        LocalDateTime now = LocalDateTime.now();
        Optional<FxRate> cachedRate = fxRateRepository.findLatestRate(fromCurrency, toCurrency, now);
        
        if (cachedRate.isPresent() && !cachedRate.get().isExpired()) {
            return cachedRate.get();
        }
        
        return fetchAndCacheRate(fromCurrency, toCurrency);
    }

    @Override
    public FxRate saveRate(FxRate fxRate) {
        return fxRateRepository.save(fxRate);
    }

    @Override
    public void updateRates() {
        if (!fxRateProvider.isAvailable()) {
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        for (String currency : SUPPORTED_CURRENCIES) {
            if (!currency.equals(BASE_CURRENCY)) {
                try {
                    FxRate baseToTarget = fetchAndCacheRate(BASE_CURRENCY, currency);
                    FxRate targetToBase = fetchAndCacheRate(currency, BASE_CURRENCY);
                    successCount++;
                } catch (Exception e) {
                    // BUG-BE-023 fix: Log error and continue instead of aborting all updates
                    log.warn("Failed to update rates for {}: {}. Continuing with other currencies.",
                            currency, e.getMessage());
                    failCount++;
                }
            }
        }
        if (failCount > 0) {
            log.warn("FX rate update completed with {} successes and {} failures", successCount, failCount);
        }
    }

    @Override
    public List<FxRate> getAllRates() {
        return fxRateRepository.findAll();
    }

    @Override
    public FxConversion convertCurrency(String accountId, String fromCurrency, String toCurrency, BigDecimal amount) {
        FxRate rate = getCurrentRate(fromCurrency, toCurrency);
        BigDecimal toAmount = amount.multiply(rate.getRate());
        
        FxConversion conversion = FxConversion.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .fromAmount(amount)
                .toAmount(toAmount)
                .exchangeRate(rate.getRate())
                .fee(BigDecimal.ZERO)
                .conversionDate(LocalDateTime.now())
                .status(FxConversion.ConversionStatus.COMPLETED)
                .build();
        
        conversion.markCompleted();
        return conversion;
    }

    private FxRate fetchAndCacheRate(String fromCurrency, String toCurrency) {
        FxRate rate = fxRateProvider.fetchCurrentRate(fromCurrency, toCurrency);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validUntil = now.plusMinutes(RATE_VALIDITY_MINUTES);
        
        rate.setValidFrom(now);
        rate.setValidUntil(validUntil);
        
        return fxRateRepository.save(rate);
    }

    private void validateCurrencyPair(String fromCurrency, String toCurrency) {
        if (!SUPPORTED_CURRENCIES.contains(fromCurrency)) {
            throw new FxRateNotFoundException("Unsupported from currency: " + fromCurrency);
        }
        if (!SUPPORTED_CURRENCIES.contains(toCurrency)) {
            throw new FxRateNotFoundException("Unsupported to currency: " + toCurrency);
        }
    }
}
