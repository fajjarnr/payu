package id.payu.wallet.application.service;

import id.payu.cache.service.CacheService;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.model.Pocket;
import id.payu.wallet.domain.model.PocketStatus;
import id.payu.wallet.domain.model.TransactionType;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.out.PocketPersistencePort;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * WALLET-LEDGER-001: pocket credit/debit move the account's main wallet the other
 * way and post one balanced double-entry journal per movement, idempotent by referenceId.
 */
@ExtendWith(MockitoExtension.class)
class PocketServiceTest {

    private static final String ACCOUNT_ID = "acc-1";
    private static final String REFERENCE_ID = "pkt-ref-1";
    private static final UUID POCKET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock private PocketPersistencePort pocketPersistencePort;
    @Mock private WalletPersistencePort walletPersistencePort;
    @Mock private JournalUseCase journalUseCase;
    @Mock private WalletEventPublisherPort walletEventPublisher;
    @Mock private CacheService cacheService;
    @Mock private id.payu.wallet.domain.port.out.FxRateProviderPort fxRateProviderPort;

    private PocketService pocketService;

    @BeforeEach
    void setUp() {
        pocketService = new PocketService(pocketPersistencePort, fxRateProviderPort,
                walletPersistencePort, journalUseCase, walletEventPublisher, cacheService);
    }

    @Test
    void creditPocket_debitsMainWalletCreditsPocketAndPostsBalancedJournal() {
        Pocket pocket = pocket("100.0000", PocketStatus.ACTIVE);
        Wallet wallet = wallet("1000.0000");
        stubLookup(pocket, wallet);
        stubSaves();

        pocketService.creditPocket(POCKET_ID, new BigDecimal("50.0000"), REFERENCE_ID);

        assertThat(pocket.getBalance()).isEqualByComparingTo("150.0000");
        assertThat(wallet.getBalance()).isEqualByComparingTo("950.0000");

        ArgumentCaptor<WalletTransaction> transaction = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletPersistencePort).saveTransaction(transaction.capture());
        assertThat(transaction.getValue().getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(transaction.getValue().getReferenceId()).isEqualTo(REFERENCE_ID);

        List<LedgerEntry> entries = captureJournalEntries();
        assertThat(entries).hasSize(2);
        LedgerEntry walletLeg = entries.stream()
                .filter(e -> ACCOUNT_ID.equals(e.getAccountId())).findFirst().orElseThrow();
        LedgerEntry pocketLeg = entries.stream()
                .filter(e -> ("POCKET:" + POCKET_ID).equals(e.getAccountId())).findFirst().orElseThrow();
        assertThat(walletLeg.getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(pocketLeg.getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(walletLeg.getTransactionId()).isEqualTo(pocketLeg.getTransactionId());
        assertThat(walletLeg.getAmount()).isEqualByComparingTo("50.0000");
        assertThat(pocketLeg.getAmount()).isEqualByComparingTo("50.0000");
        assertThat(walletLeg.getBalanceAfter()).isEqualByComparingTo("950.0000");
        assertThat(pocketLeg.getBalanceAfter()).isEqualByComparingTo("150.0000");
        assertThat(walletLeg.getReferenceType()).isEqualTo("POCKET_CREDIT");

        verify(walletEventPublisher).publishBalanceChanged(eq(ACCOUNT_ID), eq(wallet.getBalance()),
                eq(wallet.getAvailableBalance()), eq(new BigDecimal("50.0000")), eq("DEBIT"));
    }

    @Test
    void debitPocket_creditsMainWalletDebitsPocketAndPostsBalancedJournal() {
        Pocket pocket = pocket("100.0000", PocketStatus.ACTIVE);
        Wallet wallet = wallet("1000.0000");
        stubLookup(pocket, wallet);
        stubSaves();

        pocketService.debitPocket(POCKET_ID, new BigDecimal("50.0000"), REFERENCE_ID);

        assertThat(pocket.getBalance()).isEqualByComparingTo("50.0000");
        assertThat(wallet.getBalance()).isEqualByComparingTo("1050.0000");

        ArgumentCaptor<WalletTransaction> transaction = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletPersistencePort).saveTransaction(transaction.capture());
        assertThat(transaction.getValue().getType()).isEqualTo(TransactionType.CREDIT);

        List<LedgerEntry> entries = captureJournalEntries();
        assertThat(entries).hasSize(2);
        LedgerEntry walletLeg = entries.stream()
                .filter(e -> ACCOUNT_ID.equals(e.getAccountId())).findFirst().orElseThrow();
        LedgerEntry pocketLeg = entries.stream()
                .filter(e -> ("POCKET:" + POCKET_ID).equals(e.getAccountId())).findFirst().orElseThrow();
        assertThat(pocketLeg.getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(walletLeg.getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(pocketLeg.getTransactionId()).isEqualTo(walletLeg.getTransactionId());
        assertThat(pocketLeg.getBalanceAfter()).isEqualByComparingTo("50.0000");
        assertThat(walletLeg.getBalanceAfter()).isEqualByComparingTo("1050.0000");

        verify(walletEventPublisher).publishBalanceChanged(eq(ACCOUNT_ID), eq(wallet.getBalance()),
                eq(wallet.getAvailableBalance()), eq(new BigDecimal("50.0000")), eq("CREDIT"));
    }

    @Test
    void creditPocket_isIdempotentByReferenceId() {
        UUID transactionId = UUID.nameUUIDFromBytes(
                "POCKET_CREDIT:pkt-ref-1".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        LedgerEntry existing = LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(ACCOUNT_ID)
                .entryType(EntryType.DEBIT)
                .amount(new BigDecimal("50.0000"))
                .currency("IDR")
                .referenceType("POCKET_CREDIT")
                .referenceId(REFERENCE_ID)
                .build();
        when(walletPersistencePort.findByTransactionId(transactionId)).thenReturn(List.of(existing));

        pocketService.creditPocket(POCKET_ID, new BigDecimal("50.0000"), REFERENCE_ID);

        verify(pocketPersistencePort, never()).save(any());
        verify(walletPersistencePort, never()).save(any());
        verifyNoInteractions(journalUseCase, walletEventPublisher);
    }

    @Test
    void replayWithDifferentAmountIsRejected() {
        UUID transactionId = UUID.nameUUIDFromBytes(
                "POCKET_CREDIT:pkt-ref-1".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        LedgerEntry existing = LedgerEntry.builder()
                .transactionId(transactionId)
                .accountId(ACCOUNT_ID)
                .entryType(EntryType.DEBIT)
                .amount(new BigDecimal("50.0000"))
                .currency("IDR")
                .referenceType("POCKET_CREDIT")
                .referenceId(REFERENCE_ID)
                .build();
        when(walletPersistencePort.findByTransactionId(transactionId)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> pocketService.creditPocket(POCKET_ID, new BigDecimal("60.0000"), REFERENCE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different amount");
    }

    @Test
    void creditPocket_rejectsClosedPocket() {
        Pocket pocket = pocket("100.0000", PocketStatus.CLOSED);
        stubPocket(pocket);

        assertThatThrownBy(() -> pocketService.creditPocket(POCKET_ID, new BigDecimal("50.0000"), REFERENCE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOSED");

        verify(pocketPersistencePort, never()).save(any());
        verify(walletPersistencePort, never()).save(any());
        verifyNoInteractions(journalUseCase);
    }

    @Test
    void debitPocket_rejectsFrozenPocket() {
        Pocket pocket = pocket("100.0000", PocketStatus.FROZEN);
        stubPocket(pocket);

        assertThatThrownBy(() -> pocketService.debitPocket(POCKET_ID, new BigDecimal("50.0000"), REFERENCE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FROZEN");

        verify(pocketPersistencePort, never()).save(any());
        verify(walletPersistencePort, never()).save(any());
        verifyNoInteractions(journalUseCase);
    }

    @Test
    void creditPocket_rejectsInsufficientMainWalletBalance() {
        Pocket pocket = pocket("100.0000", PocketStatus.ACTIVE);
        Wallet wallet = wallet("10.0000");
        stubLookup(pocket, wallet);

        assertThatThrownBy(() -> pocketService.creditPocket(POCKET_ID, new BigDecimal("50.0000"), REFERENCE_ID))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("10.0000");
        assertThat(pocket.getBalance()).isEqualByComparingTo("100.0000");
        verifyNoInteractions(journalUseCase);
    }

    @Test
    void debitPocket_rejectsInsufficientPocketBalance() {
        Pocket pocket = pocket("10.0000", PocketStatus.ACTIVE);
        Wallet wallet = wallet("1000.0000");
        stubLookup(pocket, wallet);

        assertThatThrownBy(() -> pocketService.debitPocket(POCKET_ID, new BigDecimal("50.0000"), REFERENCE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient balance in pocket");

        assertThat(wallet.getBalance()).isEqualByComparingTo("1000.0000");
        verify(walletPersistencePort, never()).save(any());
        verifyNoInteractions(journalUseCase);
    }

    @Test
    void creditPocket_rejectsCurrencyMismatchBetweenPocketAndWallet() {
        Pocket pocket = pocket("100.0000", PocketStatus.ACTIVE, "USD");
        Wallet wallet = wallet("1000.0000", "IDR");
        stubLookup(pocket, wallet);

        assertThatThrownBy(() -> pocketService.creditPocket(POCKET_ID, new BigDecimal("50.0000"), REFERENCE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");

        verifyNoInteractions(journalUseCase);
    }

    @Test
    void rejectsInvalidAmountAndMissingReference() {
        assertThatThrownBy(() -> pocketService.creditPocket(POCKET_ID, BigDecimal.ZERO, REFERENCE_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pocketService.creditPocket(POCKET_ID, new BigDecimal("-1"), REFERENCE_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pocketService.creditPocket(POCKET_ID, new BigDecimal("1.00001"), REFERENCE_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pocketService.creditPocket(POCKET_ID, new BigDecimal("50.0000"), "  "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(pocketPersistencePort, never()).findById(any());
    }

    @Test
    void unknownPocketThrowsNotFound() {
        when(pocketPersistencePort.findById(POCKET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pocketService.creditPocket(POCKET_ID, new BigDecimal("50.0000"), REFERENCE_ID))
                .isInstanceOf(PocketNotFoundException.class);
    }

    private List<LedgerEntry> captureJournalEntries() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> entries = ArgumentCaptor.forClass(List.class);
        verify(journalUseCase).createAndPostJournal(
                any(String.class), any(String.class), eq(REFERENCE_ID), entries.capture(), any(String.class));
        return entries.getValue();
    }

    private void stubLookup(Pocket pocket, Wallet wallet) {
        when(pocketPersistencePort.findById(POCKET_ID)).thenReturn(Optional.of(pocket));
        when(walletPersistencePort.findByAccountIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(wallet));
        when(walletPersistencePort.findByTransactionId(any())).thenReturn(List.of());
    }

    private void stubSaves() {
        when(pocketPersistencePort.save(any(Pocket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletPersistencePort.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubPocket(Pocket pocket) {
        when(pocketPersistencePort.findById(POCKET_ID)).thenReturn(Optional.of(pocket));
        when(walletPersistencePort.findByTransactionId(any())).thenReturn(List.of());
    }

    private static Pocket pocket(String balance, PocketStatus status) {
        return pocket(balance, status, "IDR");
    }

    private static Pocket pocket(String balance, PocketStatus status, String currency) {
        return Pocket.builder()
                .id(POCKET_ID)
                .accountId(ACCOUNT_ID)
                .name("Tabungan")
                .currency(currency)
                .balance(new BigDecimal(balance))
                .status(status)
                .build();
    }

    private static Wallet wallet(String balance) {
        return wallet(balance, "IDR");
    }

    private static Wallet wallet(String balance, String currency) {
        return Wallet.builder()
                .id(UUID.randomUUID())
                .accountId(ACCOUNT_ID)
                .balance(new BigDecimal(balance))
                .reservedBalance(BigDecimal.ZERO)
                .currency(currency)
                .build();
    }
}
