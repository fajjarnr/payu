package id.payu.fx.application;

import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.in.FxConversionUseCase;
import id.payu.fx.domain.port.in.FxRateUseCase;
import id.payu.fx.domain.port.out.FxConversionRepositoryPort;
import id.payu.fx.domain.port.out.FxRateProviderPort;
import id.payu.fx.domain.port.out.FxRateRepositoryPort;
import id.payu.fx.application.service.FxConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FxConversionServiceTest {

    @Mock
    private FxConversionRepositoryPort conversionRepository;

    @Mock
    private FxRateUseCase fxRateUseCase;

    private FxConversionService fxConversionService;

    private FxRate testRate;

    @BeforeEach
    void setUp() {
        fxConversionService = new FxConversionService(conversionRepository, fxRateUseCase);
        
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
    void createConversion_shouldCalculateToAmount() {
        FxConversion conversion = FxConversion.builder()
                .id(UUID.randomUUID())
                .accountId("account-123")
                .fromCurrency("IDR")
                .toCurrency("USD")
                .fromAmount(new BigDecimal("1000000"))
                .build();

        when(fxRateUseCase.getCurrentRate("IDR", "USD")).thenReturn(testRate);
        when(conversionRepository.save(any(FxConversion.class))).thenAnswer(inv -> inv.getArgument(0));

        FxConversion result = fxConversionService.createConversion(conversion);

        assertThat(result.getToAmount()).isEqualByComparingTo("65.00");
        assertThat(result.getExchangeRate()).isEqualByComparingTo("0.000065");
        assertThat(result.getStatus()).isEqualTo(FxConversion.ConversionStatus.PENDING);
        verify(fxRateUseCase).getCurrentRate("IDR", "USD");
        verify(conversionRepository).save(any(FxConversion.class));
    }

    @Test
    void getConversion_shouldReturnConversion_whenExists() {
        UUID conversionId = UUID.randomUUID();
        FxConversion conversion = FxConversion.builder()
                .id(conversionId)
                .accountId("account-123")
                .fromCurrency("IDR")
                .toCurrency("USD")
                .fromAmount(new BigDecimal("1000000"))
                .toAmount(new BigDecimal("65.00"))
                .exchangeRate(new BigDecimal("0.000065"))
                .status(FxConversion.ConversionStatus.COMPLETED)
                .build();

        when(conversionRepository.findById(conversionId)).thenReturn(Optional.of(conversion));

        FxConversion result = fxConversionService.getConversion(conversionId);

        assertThat(result.getId()).isEqualTo(conversionId);
        verify(conversionRepository).findById(conversionId);
    }

    @Test
    void getConversion_shouldThrowException_whenNotFound() {
        UUID conversionId = UUID.randomUUID();
        when(conversionRepository.findById(conversionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fxConversionService.getConversion(conversionId))
                .isInstanceOf(id.payu.fx.application.service.FxConversionNotFoundException.class)
                .hasMessageContaining("Conversion not found");
    }

    @Test
    void getConversionsByAccount_shouldReturnConversions() {
        String accountId = "account-123";
        FxConversion conversion = FxConversion.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .fromCurrency("IDR")
                .toCurrency("USD")
                .fromAmount(new BigDecimal("1000000"))
                .build();

        when(conversionRepository.findByAccountId(accountId)).thenReturn(java.util.List.of(conversion));

        var results = fxConversionService.getConversionsByAccount(accountId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAccountId()).isEqualTo(accountId);
        verify(conversionRepository).findByAccountId(accountId);
    }

    @Test
    void reverseConversion_shouldReverseCompletedConversion() {
        UUID conversionId = UUID.randomUUID();
        FxConversion conversion = FxConversion.builder()
                .id(conversionId)
                .accountId("account-123")
                .fromCurrency("IDR")
                .toCurrency("USD")
                .status(FxConversion.ConversionStatus.COMPLETED)
                .build();

        when(conversionRepository.findById(conversionId)).thenReturn(Optional.of(conversion));
        when(conversionRepository.save(any(FxConversion.class))).thenAnswer(inv -> inv.getArgument(0));

        fxConversionService.reverseConversion(conversionId);

        assertThat(conversion.getStatus()).isEqualTo(FxConversion.ConversionStatus.REVERSED);
        verify(conversionRepository).save(conversion);
    }

    @Test
    void reverseConversion_shouldThrowException_whenNotCompleted() {
        UUID conversionId = UUID.randomUUID();
        FxConversion conversion = FxConversion.builder()
                .id(conversionId)
                .accountId("account-123")
                .status(FxConversion.ConversionStatus.PENDING)
                .build();

        when(conversionRepository.findById(conversionId)).thenReturn(Optional.of(conversion));

        assertThatThrownBy(() -> fxConversionService.reverseConversion(conversionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reverse conversion");
    }
}
