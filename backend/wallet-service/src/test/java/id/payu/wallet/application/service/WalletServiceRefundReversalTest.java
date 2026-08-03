package id.payu.wallet.application.service;

import id.payu.cache.service.CacheService;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceRefundReversalTest {

    private static final UUID REFUND_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Mock
    private WalletPersistencePort walletPersistencePort;
    @Mock
    private WalletEventPublisherPort walletEventPublisher;
    @Mock
    private CacheService cacheService;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletPersistencePort, walletEventPublisher, cacheService);
    }

    @Test
    void reverseTransfer_shouldDebitRecipientAndCreditSenderAtomically() {
        Wallet sender = wallet("sender", "1000.00");
        Wallet recipient = wallet("recipient", "500.00");
        when(walletPersistencePort.findByAccountIdForUpdate("recipient")).thenReturn(java.util.Optional.of(recipient));
        when(walletPersistencePort.findByAccountIdForUpdate("sender")).thenReturn(java.util.Optional.of(sender));
        when(walletPersistencePort.findByTransactionId(REFUND_ID)).thenReturn(java.util.List.of());

        walletService.reverseTransfer("sender", "recipient", new BigDecimal("100.00"), "IDR", REFUND_ID, "refund");

        assertThat(sender.getBalance()).isEqualByComparingTo("1100.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("400.00");
        ArgumentCaptor<id.payu.wallet.domain.model.LedgerEntry> entries = ArgumentCaptor.forClass(id.payu.wallet.domain.model.LedgerEntry.class);
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
