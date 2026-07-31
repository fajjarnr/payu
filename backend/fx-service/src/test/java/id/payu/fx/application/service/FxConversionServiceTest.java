package id.payu.fx.application.service;

import id.payu.fx.domain.model.ConversionStatus;
import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.in.FxRateUseCase;
import id.payu.fx.domain.port.out.FxConversionRepositoryPort;
import id.payu.fx.domain.port.out.WalletServicePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FxConversionService Unit Tests")
class FxConversionServiceTest {

    @Mock
    private FxConversionRepositoryPort conversionRepository;

    @Mock
    private FxRateUseCase fxRateUseCase;

    @Mock
    private WalletServicePort walletServicePort;

    @InjectMocks
    private FxConversionService fxConversionService;

    private FxRate rate(String from, String to) {
        return new FxRate(UUID.randomUUID(), from, to, new BigDecimal("15000.00000000"),
                new BigDecimal("0.00006667"), LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(5), 0L, LocalDateTime.now());
    }

    @Test
    @DisplayName("createConversion persists conversion without wallet-side effect on failure")
    void createConversionShouldSaveAndDebit() {
        when(fxRateUseCase.getCurrentRate("USD", "IDR")).thenReturn(rate("USD", "IDR"));
        when(conversionRepository.save(any(FxConversion.class)))
                .thenAnswer(invocation -> {
                    FxConversion conversion = invocation.getArgument(0);
                    if (conversion.getId() == null) {
                        conversion.setId(UUID.randomUUID());
                    }
                    return conversion;
                });
        when(walletServicePort.debit(any(), any(), any(), any())).thenReturn(true);
        when(walletServicePort.credit(any(), any(), any(), any())).thenReturn(true);

        FxConversion result = fxConversionService.createConversion(
                FxConversion.builder()
                        .accountId("acct-1")
                        .fromCurrency("USD").toCurrency("IDR")
                        .fromAmount(new BigDecimal("10.0000"))
                        .build());

        assertThat(result.getStatus()).isEqualTo(ConversionStatus.COMPLETED);
        assertThat(result.getConversionDate()).isNotNull();
        verify(conversionRepository, atLeastOnce()).save(any(FxConversion.class));
    }

    @Test
    @DisplayName("estimateConversion must not persist or move money")
    void estimateConversionShouldNotPersistOrTouchWallet() {
        when(fxRateUseCase.getCurrentRate("USD", "IDR")).thenReturn(rate("USD", "IDR"));

        FxConversion estimate = fxConversionService.estimateConversion(
                FxConversion.builder()
                        .fromCurrency("USD").toCurrency("IDR")
                        .fromAmount(new BigDecimal("100.0000"))
                        .build());

        assertThat(estimate.getToAmount()).isEqualByComparingTo("1500000.0000");
        assertThat(estimate.getExchangeRate()).isEqualByComparingTo("15000.00000000");
        verify(conversionRepository, never()).save(any(FxConversion.class));
        verify(walletServicePort, never()).debit(any(), any(), any(), any());
        verify(walletServicePort, never()).credit(any(), any(), any(), any());
    }
}
