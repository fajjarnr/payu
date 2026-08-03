package id.payu.wallet.application.service;

import id.payu.cache.service.CacheService;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.model.TransactionType;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceIdempotencyTest {

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
    void reserveReplayReturnsOriginalReservation() {
        UUID reservationId = UUID.randomUUID();
        LedgerEntry existing = LedgerEntry.builder()
                .transactionId(reservationId)
                .accountId("user-1")
                .entryType(EntryType.DEBIT)
                .amount(new BigDecimal("100.00"))
                .currency("IDR")
                .referenceType("RESERVATION")
                .referenceId("INVESTMENT_DEBIT:1")
                .build();
        when(walletPersistencePort.findReservationByReference("INVESTMENT_DEBIT:1"))
                .thenReturn(Optional.of(existing));

        assertThat(walletService.reserveBalance("user-1", new BigDecimal("100.00"), "INVESTMENT_DEBIT:1"))
                .isEqualTo(reservationId.toString());
        verify(walletPersistencePort, never()).findByAccountIdForUpdate(anyString());
        verify(walletPersistencePort, never()).saveLedgerEntry(any());
    }

    @Test
    void creditReplayReturnsOriginalTransaction() {
        UUID walletId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Wallet wallet = Wallet.builder().id(walletId).accountId("user-1")
                .balance(new BigDecimal("500.00")).reservedBalance(BigDecimal.ZERO).currency("IDR").build();
        WalletTransaction existing = WalletTransaction.builder()
                .id(transactionId)
                .walletId(walletId)
                .referenceId("INVESTMENT_COMPENSATION:1")
                .type(TransactionType.CREDIT)
                .amount(new BigDecimal("100.00"))
                .build();
        when(walletPersistencePort.findTransactionByReference("INVESTMENT_COMPENSATION:1"))
                .thenReturn(Optional.of(existing));
        when(walletPersistencePort.findByAccountId("user-1")).thenReturn(Optional.of(wallet));

        assertThat(walletService.credit("user-1", new BigDecimal("100.00"),
                "INVESTMENT_COMPENSATION:1", "investment compensation"))
                .isEqualTo(transactionId.toString());
        verify(walletPersistencePort, never()).findByAccountIdForUpdate(anyString());
        verify(walletPersistencePort, never()).saveTransaction(any());
    }
}
