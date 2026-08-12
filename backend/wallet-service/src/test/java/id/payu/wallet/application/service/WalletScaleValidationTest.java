package id.payu.wallet.application.service;

import id.payu.cache.service.CacheService;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * GRPC-020: reserveBalance and credit must enforce the same scale-4 money
 * rule as transfer — an amount with more than 4 decimals must be rejected
 * before any state change or persistence.
 */
@ExtendWith(MockitoExtension.class)
class WalletScaleValidationTest {

    @Mock
    private WalletPersistencePort walletPersistencePort;
    @Mock
    private WalletEventPublisherPort walletEventPublisher;
    @Mock
    private CacheService cacheService;
    @Mock
    private JournalUseCase journalUseCase;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletPersistencePort, walletEventPublisher, cacheService, journalUseCase);
    }

    @Test
    void reserveBalanceRejectsAmountWithMoreThanFourDecimals() {
        assertThatThrownBy(() -> walletService.reserveBalance("ACC-1", new BigDecimal("0.00001"), "REF-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 decimals");

        verify(walletPersistencePort, never()).save(any());
        verify(walletPersistencePort, never()).saveLedgerEntry(any());
        verify(walletEventPublisher, never()).publishBalanceReserved(anyString(), anyString(), any());
    }

    @Test
    void creditRejectsAmountWithMoreThanFourDecimals() {
        assertThatThrownBy(() -> walletService.credit("ACC-1", new BigDecimal("0.00001"), "REF-1", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 decimals");

        verify(walletPersistencePort, never()).save(any());
        verify(walletPersistencePort, never()).saveLedgerEntry(any());
        verify(walletEventPublisher, never()).publishBalanceChanged(anyString(), any(), any());
    }

    @Test
    void reserveBalanceAcceptsExactlyFourDecimals() {
        assertThatThrownBy(() -> walletService.reserveBalance("ACC-1", new BigDecimal("0.0001"), "REF-1"))
                .isNotInstanceOf(IllegalArgumentException.class)
                .isInstanceOf(id.payu.wallet.application.exception.WalletNotFoundException.class);
    }
}
