package id.payu.wallet.application.service;

import id.payu.cache.service.CacheService;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTransferTest {

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
        when(walletPersistencePort.findByTransactionId(any())).thenReturn(List.of());
    }

    @Test
    void transferDebitsAndCreditsBothWalletsInOneOperation() {
        Wallet sender = wallet("sender", "1000.00");
        Wallet recipient = wallet("recipient", "500.00");
        when(walletPersistencePort.findByAccountIdForUpdate("recipient")).thenReturn(Optional.of(recipient));
        when(walletPersistencePort.findByAccountIdForUpdate("sender")).thenReturn(Optional.of(sender));

        String transactionId = walletService.transfer(
                "sender", "recipient", new BigDecimal("100.00"), "IDR", "transfer-1", "test");

        assertThat(transactionId).isNotBlank();
        assertThat(sender.getBalance()).isEqualByComparingTo("900.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("600.00");
        ArgumentCaptor<id.payu.wallet.domain.model.LedgerEntry> entries =
                ArgumentCaptor.forClass(id.payu.wallet.domain.model.LedgerEntry.class);
        verify(walletPersistencePort, times(2)).saveLedgerEntry(entries.capture());
        assertThat(entries.getAllValues()).extracting(id.payu.wallet.domain.model.LedgerEntry::getEntryType)
                .containsExactlyInAnyOrder(EntryType.DEBIT, EntryType.CREDIT);
    }

    private Wallet wallet(String accountId, String balance) {
        return Wallet.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .balance(new BigDecimal(balance))
                .reservedBalance(BigDecimal.ZERO)
                .currency("IDR")
                .build();
    }
}
