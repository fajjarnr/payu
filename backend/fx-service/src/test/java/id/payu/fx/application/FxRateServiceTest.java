package id.payu.fx.application;

import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.out.FxRateProviderPort;
import id.payu.fx.domain.port.out.FxRateRepositoryPort;
import id.payu.fx.domain.port.in.FxRateUseCase;
import id.payu.fx.application.service.FxRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FxRateServiceTest {

    @Mock
    private FxRateRepositoryPort fxRateRepository;

    @Mock
    private FxRateProviderPort fxRateProvider;

    private FxRateService fxRateService;

    private FxRate testRate;

    @BeforeEach
    void setUp() {
        fxRateService = new id.payu.fx.application.service.FxRateService(fxRateRepository, fxRateProvider);

        testRate = FxRate.builder()
                .id(UUID.randomUUID())
                .fromCurrency("IDR")
                .toCurrency("USD")
                .rate(new BigDecimal("0.000065"))
                .inverseRate(new BigDecimal("15384.62"))
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    @Test
    void getCurrentRate_shouldReturnCachedRate_whenValid() {
        when(fxRateRepository.findLatestRate(eq("IDR"), eq("USD"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testRate));

        FxRate result = fxRateService.getCurrentRate("IDR", "USD");

        assertThat(result).isEqualTo(testRate);
        verify(fxRateRepository).findLatestRate(eq("IDR"), eq("USD"), any(LocalDateTime.class));
        verify(fxRateProvider, never()).fetchCurrentRate(anyString(), anyString());
    }

    @Test
    void getCurrentRate_shouldFetchNewRate_whenCacheExpired() {
        FxRate expiredRate = FxRate.builder()
                .id(UUID.randomUUID())
                .fromCurrency("IDR")
                .toCurrency("USD")
                .rate(new BigDecimal("0.000064"))
                .validFrom(LocalDateTime.now().minusHours(1))
                .validUntil(LocalDateTime.now().minusMinutes(30))
                .build();

        when(fxRateRepository.findLatestRate(eq("IDR"), eq("USD"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(expiredRate));
        when(fxRateProvider.fetchCurrentRate("IDR", "USD")).thenReturn(testRate);
        when(fxRateRepository.save(any(FxRate.class))).thenReturn(testRate);

        FxRate result = fxRateService.getCurrentRate("IDR", "USD");

        assertThat(result.getRate()).isEqualByComparingTo("0.000065");
        verify(fxRateProvider).fetchCurrentRate("IDR", "USD");
        verify(fxRateRepository).save(any(FxRate.class));
    }

    @Test
    void getCurrentRate_shouldThrowException_forUnsupportedCurrency() {
        assertThatThrownBy(() -> fxRateService.getCurrentRate("XYZ", "USD"))
                .isInstanceOf(id.payu.fx.application.service.FxRateNotFoundException.class)
                .hasMessageContaining("Unsupported from currency");
    }

    @Test
    void saveRate_shouldPersistRate() {
        when(fxRateRepository.save(testRate)).thenReturn(testRate);

        FxRate result = fxRateService.saveRate(testRate);

        assertThat(result).isEqualTo(testRate);
        verify(fxRateRepository).save(testRate);
    }

    @Test
    void convertCurrency_shouldCalculateCorrectAmount() {
        when(fxRateRepository.findLatestRate(eq("IDR"), eq("USD"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testRate));

        FxConversion result = fxRateService.convertCurrency("account-123", "IDR", "USD", new BigDecimal("1000000"));

        assertThat(result.getFromAmount()).isEqualByComparingTo("1000000");
        assertThat(result.getToAmount()).isEqualByComparingTo("65.00");
        assertThat(result.getExchangeRate()).isEqualByComparingTo("0.000065");
        assertThat(result.getStatus()).isEqualTo(FxConversion.ConversionStatus.COMPLETED);
    }

    @Test
    void getCurrentRate_shouldFetchNewRate_whenNoCachedRate() {
        when(fxRateRepository.findLatestRate(eq("IDR"), eq("USD"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(fxRateProvider.fetchCurrentRate("IDR", "USD")).thenReturn(testRate);
        when(fxRateRepository.save(any(FxRate.class))).thenReturn(testRate);

        FxRate result = fxRateService.getCurrentRate("IDR", "USD");

        assertThat(result).isEqualTo(testRate);
        verify(fxRateProvider).fetchCurrentRate("IDR", "USD");
        verify(fxRateRepository).save(any(FxRate.class));
    }

    @Test
    void getAllRates_shouldReturnAllRates() {
        when(fxRateRepository.findAll()).thenReturn(List.of(testRate));

        var result = fxRateService.getAllRates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testRate);
        verify(fxRateRepository).findAll();
    }

    @Test
    void updateRates_shouldUpdateAllSupportedCurrencies() {
        when(fxRateProvider.isAvailable()).thenReturn(true);
        when(fxRateProvider.fetchCurrentRate(anyString(), anyString())).thenReturn(testRate);
        when(fxRateRepository.save(any(FxRate.class))).thenReturn(testRate);

        fxRateService.updateRates();

        // Should update 7 currencies (all except IDR which is BASE_CURRENCY)
        verify(fxRateProvider, atLeastOnce()).fetchCurrentRate(anyString(), anyString());
        verify(fxRateRepository, atLeast(14)).save(any(FxRate.class)); // 7 pairs x 2 directions
    }

    @Test
    void updateRates_shouldDoNothing_whenProviderNotAvailable() {
        when(fxRateProvider.isAvailable()).thenReturn(false);

        fxRateService.updateRates();

        verify(fxRateProvider, never()).fetchCurrentRate(anyString(), anyString());
        verify(fxRateRepository, never()).save(any(FxRate.class));
    }

    @Test
    void getCurrentRate_shouldThrowException_forUnsupportedToCurrency() {
        assertThatThrownBy(() -> fxRateService.getCurrentRate("IDR", "XYZ"))
                .isInstanceOf(id.payu.fx.application.service.FxRateNotFoundException.class)
                .hasMessageContaining("Unsupported to currency");
    }
}
