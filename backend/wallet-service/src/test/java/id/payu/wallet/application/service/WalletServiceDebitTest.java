package id.payu.wallet.application.service;

import id.payu.cache.service.CacheService;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletStatus;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GRPC-012: the gRPC Debit RPC must perform a real debit (available balance
 * decreases), not a reserve. The FX conversion flow depends on it.
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceDebitTest {

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

    private Wallet wallet(String accountId, String balance) {
        return Wallet.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .balance(new BigDecimal(balance))
                .reservedBalance(BigDecimal.ZERO)
                .currency("IDR")
                .status(WalletStatus.ACTIVE)
                .build();
    }

    @Test
    void debitDecreasesAvailableBalanceAndRecordsLedger() {
        Wallet wallet = wallet("ACC-1", "1000.0000");
        when(walletPersistencePort.findByAccountIdForUpdate("ACC-1")).thenReturn(Optional.of(wallet));
        when(walletPersistencePort.findTransactionByReference("REF-1")).thenReturn(Optional.empty());
        when(walletPersistencePort.save(wallet)).thenReturn(wallet);

        String txId = walletService.debit("ACC-1", new BigDecimal("250.0000"), "REF-1", "FX debit");

        assertThat(txId).isNotBlank();
        assertThat(wallet.getBalance()).isEqualByComparingTo("750.0000");
        verify(walletPersistencePort).saveLedgerEntry(any());
        verify(walletPersistencePort).saveTransaction(any());
    }

    @Test
    void debitRejectsInsufficientBalanceWithoutStateChange() {
        Wallet wallet = wallet("ACC-1", "100.0000");
        when(walletPersistencePort.findByAccountIdForUpdate("ACC-1")).thenReturn(Optional.of(wallet));
        when(walletPersistencePort.findTransactionByReference("REF-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.debit("ACC-1", new BigDecimal("250.0000"), "REF-1", "FX debit"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.0000");
        verify(walletPersistencePort, never()).saveLedgerEntry(any());
    }

    @Test
    void debitRejectsAmountWithMoreThanFourDecimals() {
        assertThatThrownBy(() -> walletService.debit("ACC-1", new BigDecimal("0.00001"), "REF-1", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 decimals");

        verify(walletPersistencePort, never()).findByAccountIdForUpdate(anyString());
    }

    @Test
    void debitReplayReturnsExistingTransactionWithoutDoubleDebit() {
        Wallet wallet = wallet("ACC-1", "750.0000");
        when(walletPersistencePort.findByAccountIdForUpdate("ACC-1")).thenReturn(Optional.of(wallet));
        when(walletPersistencePort.findByAccountId("ACC-1")).thenReturn(Optional.of(wallet));
        when(walletPersistencePort.findTransactionByReference("REF-1"))
                .thenReturn(Optional.empty())   // debit 1: pre-lock check
                .thenReturn(Optional.empty())   // debit 1: post-lock check
                .thenReturn(Optional.of(id.payu.wallet.domain.model.WalletTransaction.builder()
                        .id(UUID.randomUUID())
                        .walletId(wallet.getId())
                        .referenceId("REF-1")
                        .type(id.payu.wallet.domain.model.TransactionType.DEBIT)
                        .amount(new BigDecimal("250.0000"))
                        .balanceAfter(new BigDecimal("750.0000"))
                        .createdAt(java.time.LocalDateTime.now())
                        .build()));
        when(walletPersistencePort.save(wallet)).thenReturn(wallet);

        walletService.debit("ACC-1", new BigDecimal("250.0000"), "REF-1", "FX debit");

        assertThat(wallet.getBalance()).isEqualByComparingTo("500.0000");
        String replayTxId = walletService.debit("ACC-1", new BigDecimal("250.0000"), "REF-1", "FX debit");
        assertThat(replayTxId).isNotBlank();
        assertThat(wallet.getBalance()).isEqualByComparingTo("500.0000");
        verify(walletPersistencePort).saveLedgerEntry(any());
    }
}
