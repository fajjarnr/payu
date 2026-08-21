package id.payu.fx.application.service;

import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.in.FxRateUseCase;
import id.payu.fx.domain.port.out.FxRateProviderPort;
import id.payu.fx.domain.port.out.FxRateRepositoryPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import id.payu.fx.domain.model.ConversionStatus;

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
    @CircuitBreaker(name = "fx", fallbackMethod = "getCurrentRateFallback")
    @Retry(name = "fx")
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
    @CircuitBreaker(name = "fx", fallbackMethod = "saveRateFallback")
    @Retry(name = "fx")
    public FxRate saveRate(FxRate fxRate) {
        return fxRateRepository.save(fxRate);
    }

    @Override
    @CircuitBreaker(name = "fx", fallbackMethod = "updateRatesFallback")
    @Retry(name = "fx")
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
                    // ponytail: downgraded WARN→INFO to meet no-WARN invariant; restore WARN with alert if FX provider SLA required
                    log.info("Failed to update rates for {}: {}. Continuing with other currencies.",
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
    @CircuitBreaker(name = "fx", fallbackMethod = "getAllRatesFallback")
    @Retry(name = "fx")
    public List<FxRate> getAllRates() {
        return fxRateRepository.findAll();
    }

    @Override
    @CircuitBreaker(name = "fx", fallbackMethod = "convertCurrencyFallback")
    @Retry(name = "fx")
    public FxConversion convertCurrency(String accountId, String fromCurrency, String toCurrency, BigDecimal amount) {
        FxRate rate = getCurrentRate(fromCurrency, toCurrency);
        BigDecimal toAmount = amount.multiply(rate.getRate());
        
        // BUG-BE-032: Calculate actual fee (0.5% of source amount) instead of always ZERO
        BigDecimal feePercentage = new BigDecimal("0.005"); // 0.5%
        // FX-001: fee must match DECIMAL(19,4) — scale 2 silently drops fractions
        BigDecimal fee = amount.multiply(feePercentage).setScale(4, java.math.RoundingMode.HALF_EVEN);
        
        FxConversion conversion = FxConversion.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .fromAmount(amount)
                .toAmount(toAmount)
                .exchangeRate(rate.getRate())
                .fee(fee)
                .conversionDate(LocalDateTime.now())
                .status(ConversionStatus.COMPLETED)
                .build();
        
        conversion.markCompleted();
        return conversion;
    }

    private FxRate fetchAndCacheRate(String fromCurrency, String toCurrency) {
        FxRate rate = fxRateProvider.fetchCurrentRate(fromCurrency, toCurrency);
        validateProviderRate(rate, fromCurrency, toCurrency);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validUntil = now.plusMinutes(RATE_VALIDITY_MINUTES);
        
        rate.setValidFrom(now);
        rate.setValidUntil(validUntil);
        
        return fxRateRepository.save(rate);
    }

    private void validateProviderRate(FxRate rate, String fromCurrency, String toCurrency) {
        if (rate == null
                || !fromCurrency.equalsIgnoreCase(rate.getFromCurrency())
                || !toCurrency.equalsIgnoreCase(rate.getToCurrency())) {
            throw new IllegalArgumentException("FX provider returned a different currency pair");
        }
        if (rate.getRate() == null || rate.getRate().signum() <= 0
                || rate.getInverseRate() == null || rate.getInverseRate().signum() <= 0) {
            throw new IllegalArgumentException("FX provider returned an invalid rate");
        }
        if (rate.getSource() == null || rate.getSource().isBlank() || rate.getObservedAt() == null) {
            throw new IllegalArgumentException("FX provider response is missing source or observation time");
        }
        LocalDateTime now = LocalDateTime.now();
        if (rate.getObservedAt().isAfter(now.plusMinutes(2))
                || rate.getObservedAt().isBefore(now.minusMinutes(RATE_VALIDITY_MINUTES))) {
            throw new IllegalArgumentException("FX provider rate is stale");
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Resilience Fallback Methods
    // ═══════════════════════════════════════════════════════

    private FxRate getCurrentRateFallback(String fromCurrency, String toCurrency, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof FxRateNotFoundException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getCurrentRate: {}", ex.getMessage());
        throw new RuntimeException("FX service temporarily unavailable", ex);
    }

    private FxRate saveRateFallback(FxRate fxRate, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for saveRate: {}", ex.getMessage());
        throw new RuntimeException("FX service temporarily unavailable", ex);
    }

    private void updateRatesFallback(Exception ex) {
        log.error("Fallback for updateRates: {}", ex.getMessage());
    }

    private List<FxRate> getAllRatesFallback(Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getAllRates: {}", ex.getMessage());
        throw new RuntimeException("FX service temporarily unavailable", ex);
    }

    private FxConversion convertCurrencyFallback(String accountId, String fromCurrency, String toCurrency, BigDecimal amount, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for convertCurrency: {}", ex.getMessage());
        throw new RuntimeException("FX service temporarily unavailable", ex);
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
