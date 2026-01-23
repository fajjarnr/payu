package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import id.payu.wallet.application.exception.WalletNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletPersistencePort walletPersistencePort;

    @Mock
    private WalletEventPublisherPort walletEventPublisher;

    @InjectMocks
    private WalletService walletService;

    private Wallet testWallet;
    private LedgerEntry testLedgerEntry;

    @BeforeEach
    void setUp() {
        UUID accountId = UUID.randomUUID();
        testWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .accountId(accountId.toString())
                .balance(new BigDecimal("10000000"))
                .reservedBalance(new BigDecimal("0"))
                .currency("IDR")
                .status(Wallet.WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testLedgerEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .accountId(accountId)
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amount(new BigDecimal("5000000"))
                .currency("IDR")
                .balanceAfter(new BigDecimal("5000000"))
                .referenceType("RESERVATION")
                .referenceId("REF-001")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should get wallet by account ID")
    void shouldGetWalletByAccountId() {
        when(walletPersistencePort.findByAccountId(testWallet.getAccountId())).thenReturn(Optional.of(testWallet));

        Optional<Wallet> result = walletService.getWalletByAccountId(testWallet.getAccountId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(testWallet.getId());
        verify(walletPersistencePort).findByAccountId(testWallet.getAccountId());
    }

    @Test
    @DisplayName("Should return empty when wallet not found by account ID")
    void shouldReturnEmptyWhenWalletNotFoundByAccountId() {
        String accountId = UUID.randomUUID().toString();
        when(walletPersistencePort.findByAccountId(accountId)).thenReturn(Optional.empty());

        Optional<Wallet> result = walletService.getWalletByAccountId(accountId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get wallet by ID")
    void shouldGetWalletById() {
        when(walletPersistencePort.findById(testWallet.getId())).thenReturn(Optional.of(testWallet));

        Wallet result = walletService.getWallet(testWallet.getId());

        assertThat(result.getId()).isEqualTo(testWallet.getId());
        verify(walletPersistencePort).findById(testWallet.getId());
    }

    @Test
    @DisplayName("Should throw exception when wallet not found by ID")
    void shouldThrowExceptionWhenWalletNotFoundById() {
        UUID walletId = UUID.randomUUID();
        when(walletPersistencePort.findById(walletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet(walletId))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining(walletId.toString());
    }

    @Test
    @DisplayName("Should create new wallet")
    void shouldCreateNewWallet() {
        when(walletPersistencePort.findByAccountId("ACC-NEW")).thenReturn(Optional.empty());
        when(walletPersistencePort.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.createWallet("ACC-NEW");

        assertThat(result.getAccountId()).isEqualTo("ACC-NEW");
        assertThat(result.getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getStatus()).isEqualTo(Wallet.WalletStatus.ACTIVE);
        verify(walletPersistencePort).save(any(Wallet.class));
        verify(walletEventPublisher).publishWalletCreated(eq("ACC-NEW"), anyString());
    }

    @Test
    @DisplayName("Should return existing wallet when creating duplicate")
    void shouldReturnExistingWalletWhenCreatingDuplicate() {
        when(walletPersistencePort.findByAccountId("ACC-001")).thenReturn(Optional.of(testWallet));

        Wallet result = walletService.createWallet("ACC-001");

        assertThat(result.getId()).isEqualTo(testWallet.getId());
        verify(walletPersistencePort, never()).save(any(Wallet.class));
        verify(walletEventPublisher, never()).publishWalletCreated(anyString(), anyString());
    }

    @Test
    @DisplayName("Should get balance")
    void shouldGetBalance() {
        when(walletPersistencePort.findByAccountId(testWallet.getAccountId())).thenReturn(Optional.of(testWallet));

        BigDecimal result = walletService.getBalance(testWallet.getAccountId());

        assertThat(result).isEqualTo(testWallet.getBalance());
    }

    @Test
    @DisplayName("Should throw exception when getting balance for non-existent wallet")
    void shouldThrowExceptionWhenGettingBalanceForNonExistentWallet() {
        String accountId = UUID.randomUUID().toString();
        when(walletPersistencePort.findByAccountId(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getBalance(accountId))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    @DisplayName("Should get available balance")
    void shouldGetAvailableBalance() {
        when(walletPersistencePort.findByAccountId(testWallet.getAccountId())).thenReturn(Optional.of(testWallet));

        BigDecimal result = walletService.getAvailableBalance(testWallet.getAccountId());

        assertThat(result).isEqualTo(testWallet.getAvailableBalance());
    }

    @Test
    @DisplayName("Should reserve balance")
    void shouldReserveBalance() {
        when(walletPersistencePort.findByAccountId(testWallet.getAccountId())).thenReturn(Optional.of(testWallet));
        when(walletPersistencePort.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        String reservationId = walletService.reserveBalance(testWallet.getAccountId(), new BigDecimal("5000000"), "REF-001");

        assertThat(reservationId).isNotNull();
        verify(walletPersistencePort).save(testWallet);
        verify(walletPersistencePort).saveLedgerEntry(any(LedgerEntry.class));
        verify(walletEventPublisher).publishBalanceReserved(eq(testWallet.getAccountId()), anyString(), eq(new BigDecimal("5000000")));
    }

    @Test
    @DisplayName("Should throw exception when reserving with insufficient balance")
    void shouldThrowExceptionWhenReservingWithInsufficientBalance() {
        when(walletPersistencePort.findByAccountId(testWallet.getAccountId())).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.reserveBalance(testWallet.getAccountId(), new BigDecimal("20000000"), "REF-001"))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletPersistencePort, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should commit reservation")
    void shouldCommitReservation() {
        UUID reservationId = testLedgerEntry.getTransactionId();
        testWallet.setReservedBalance(new BigDecimal("5000000"));
        when(walletPersistencePort.findByTransactionId(reservationId)).thenReturn(List.of(testLedgerEntry));
        when(walletPersistencePort.findByAccountId(testWallet.getAccountId())).thenReturn(Optional.of(testWallet));
        when(walletPersistencePort.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.commitReservation(reservationId.toString());

        assertThat(testWallet.getBalance()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(testWallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(walletPersistencePort).save(testWallet);
        verify(walletPersistencePort, times(1)).saveLedgerEntry(any(LedgerEntry.class));
        verify(walletEventPublisher).publishReservationCommitted(eq(testWallet.getAccountId()), eq(reservationId.toString()), any(BigDecimal.class));
        verify(walletEventPublisher).publishBalanceChanged(eq(testWallet.getAccountId()), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should throw exception when committing non-existent reservation")
    void shouldThrowExceptionWhenCommittingNonExistentReservation() {
        UUID reservationId = UUID.randomUUID();
        when(walletPersistencePort.findByTransactionId(reservationId)).thenReturn(List.of());

        assertThatThrownBy(() -> walletService.commitReservation(reservationId.toString()))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    @DisplayName("Should release reservation")
    void shouldReleaseReservation() {
        UUID reservationId = testLedgerEntry.getTransactionId();
        testWallet.setReservedBalance(new BigDecimal("5000000"));
        when(walletPersistencePort.findByTransactionId(reservationId)).thenReturn(List.of(testLedgerEntry));
        when(walletPersistencePort.findByAccountId(testWallet.getAccountId())).thenReturn(Optional.of(testWallet));
        when(walletPersistencePort.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.releaseReservation(reservationId.toString());

        assertThat(testWallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(testWallet.getBalance()).isEqualByComparingTo(new BigDecimal("10000000"));
        verify(walletPersistencePort).save(testWallet);
        verify(walletPersistencePort).saveLedgerEntry(any(LedgerEntry.class));
        verify(walletEventPublisher).publishReservationReleased(eq(testWallet.getAccountId()), eq(reservationId.toString()), any(BigDecimal.class));
        verify(walletEventPublisher).publishBalanceChanged(eq(testWallet.getAccountId()), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should credit amount to wallet")
    void shouldCreditAmountToWallet() {
        when(walletPersistencePort.findByAccountId(testWallet.getAccountId())).thenReturn(Optional.of(testWallet));
        when(walletPersistencePort.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.credit(testWallet.getAccountId(), new BigDecimal("5000000"), "REF-001", "Test credit");

        assertThat(testWallet.getBalance()).isEqualByComparingTo(new BigDecimal("15000000"));
        verify(walletPersistencePort).save(testWallet);
        verify(walletPersistencePort).saveLedgerEntry(any(LedgerEntry.class));
        verify(walletPersistencePort).saveTransaction(any(WalletTransaction.class));
        verify(walletEventPublisher).publishBalanceChanged(eq(testWallet.getAccountId()), any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should get transaction history")
    void shouldGetTransactionHistory() {
        when(walletPersistencePort.findByAccountId(testWallet.getAccountId())).thenReturn(Optional.of(testWallet));
        List<WalletTransaction> transactions = List.of(
                WalletTransaction.builder()
                        .id(UUID.randomUUID())
                        .walletId(testWallet.getId())
                        .referenceId("REF-001")
                        .type(WalletTransaction.TransactionType.CREDIT)
                        .amount(new BigDecimal("5000000"))
                        .balanceAfter(new BigDecimal("15000000"))
                        .description("Test")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        when(walletPersistencePort.findTransactionsByWalletId(testWallet.getId(), 0, 20)).thenReturn(transactions);

        List<WalletTransaction> result = walletService.getTransactionHistory(testWallet.getAccountId(), 0, 20);

        assertThat(result).hasSize(1);
        verify(walletPersistencePort).findTransactionsByWalletId(testWallet.getId(), 0, 20);
    }

    @Test
    @DisplayName("Should get ledger entries by account ID")
    void shouldGetLedgerEntriesByAccountId() {
        UUID accountId = UUID.fromString(testWallet.getAccountId());
        when(walletPersistencePort.findByAccountIdOrderByCreatedAtDesc(accountId)).thenReturn(List.of(testLedgerEntry));

        List<LedgerEntry> result = walletService.getLedgerEntriesByAccountId(accountId);

        assertThat(result).hasSize(1);
        verify(walletPersistencePort).findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Test
    @DisplayName("Should get ledger entries by transaction ID")
    void shouldGetLedgerEntriesByTransactionId() {
        UUID transactionId = testLedgerEntry.getTransactionId();
        when(walletPersistencePort.findByTransactionId(transactionId)).thenReturn(List.of(testLedgerEntry));

        List<LedgerEntry> result = walletService.getLedgerEntriesByTransactionId(transactionId);

        assertThat(result).hasSize(1);
        verify(walletPersistencePort).findByTransactionId(transactionId);
    }
}
