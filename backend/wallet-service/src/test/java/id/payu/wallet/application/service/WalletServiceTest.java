package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Unit Tests")
class WalletServiceTest {

    @Mock
    private WalletPersistencePort walletPersistencePort;

    @Mock
    private WalletEventPublisherPort walletEventPublisher;

    @InjectMocks
    private WalletService walletService;

    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .accountId("ACC-001")
                .balance(new BigDecimal("1000000.00"))
                .reservedBalance(BigDecimal.ZERO)
                .currency("IDR")
                .status(Wallet.WalletStatus.ACTIVE)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should get wallet by account ID")
    void shouldGetWalletByAccountId() {
        // Given
        when(walletPersistencePort.findByAccountId("ACC-001")).thenReturn(Optional.of(testWallet));

        // When
        Optional<Wallet> result = walletService.getWalletByAccountId("ACC-001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("ACC-001");
        verify(walletPersistencePort).findByAccountId("ACC-001");
    }

    @Test
    @DisplayName("Should return balance for account")
    void shouldReturnBalanceForAccount() {
        // Given
        when(walletPersistencePort.findByAccountId("ACC-001")).thenReturn(Optional.of(testWallet));

        // When
        BigDecimal balance = walletService.getBalance("ACC-001");

        // Then
        assertThat(balance).isEqualByComparingTo(new BigDecimal("1000000.00"));
    }

    @Test
    @DisplayName("Should throw exception when wallet not found")
    void shouldThrowExceptionWhenWalletNotFound() {
        // Given
        when(walletPersistencePort.findByAccountId("UNKNOWN")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> walletService.getBalance("UNKNOWN"))
                .isInstanceOf(WalletService.WalletNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("Should reserve balance successfully")
    void shouldReserveBalanceSuccessfully() {
        // Given
        BigDecimal amount = new BigDecimal("100000.00");
        when(walletPersistencePort.findByAccountId("ACC-001")).thenReturn(Optional.of(testWallet));
        when(walletPersistencePort.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        String reservationId = walletService.reserveBalance("ACC-001", amount, "TXN-001");

        // Then
        assertThat(reservationId).isNotNull();

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletPersistencePort).save(walletCaptor.capture());

        Wallet savedWallet = walletCaptor.getValue();
        assertThat(savedWallet.getReservedBalance()).isEqualByComparingTo(amount);

        verify(walletEventPublisher).publishBalanceReserved(eq("ACC-001"), eq(reservationId), eq(amount));
    }

    @Test
    @DisplayName("Should throw exception when insufficient balance")
    void shouldThrowExceptionWhenInsufficientBalance() {
        // Given
        BigDecimal tooMuch = new BigDecimal("5000000.00");
        when(walletPersistencePort.findByAccountId("ACC-001")).thenReturn(Optional.of(testWallet));

        // When/Then
        assertThatThrownBy(() -> walletService.reserveBalance("ACC-001", tooMuch, "TXN-001"))
                .isInstanceOf(WalletService.InsufficientBalanceException.class);
    }

    @Test
    @DisplayName("Should credit amount to wallet")
    void shouldCreditAmountToWallet() {
        // Given
        BigDecimal amount = new BigDecimal("50000.00");
        when(walletPersistencePort.findByAccountId("ACC-001")).thenReturn(Optional.of(testWallet));
        when(walletPersistencePort.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletPersistencePort.saveTransaction(any(WalletTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        walletService.credit("ACC-001", amount, "TXN-002", "Incoming transfer");

        // Then
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletPersistencePort).save(walletCaptor.capture());

        Wallet savedWallet = walletCaptor.getValue();
        assertThat(savedWallet.getBalance()).isEqualByComparingTo(new BigDecimal("1050000.00"));

        verify(walletPersistencePort).saveTransaction(any(WalletTransaction.class));
        verify(walletEventPublisher).publishBalanceChanged(eq("ACC-001"), any(), any());
    }

    @Test
    @DisplayName("Should return available balance correctly")
    void shouldReturnAvailableBalanceCorrectly() {
        // Given
        testWallet.setReservedBalance(new BigDecimal("200000.00"));
        when(walletPersistencePort.findByAccountId("ACC-001")).thenReturn(Optional.of(testWallet));

        // When
        BigDecimal available = walletService.getAvailableBalance("ACC-001");

        // Then
        assertThat(available).isEqualByComparingTo(new BigDecimal("800000.00"));
    }

    @Test
    @DisplayName("Should create wallet successfully")
    void shouldCreateWallet() {
        // Given
        when(walletPersistencePort.findByAccountId("ACC-NEW")).thenReturn(Optional.empty());

        // When
        Wallet createdWallet = walletService.createWallet("ACC-NEW");

        // Then
        assertThat(createdWallet).isNotNull();
        assertThat(createdWallet.getAccountId()).isEqualTo("ACC-NEW");
        assertThat(createdWallet.getBalance()).isZero();

        verify(walletPersistencePort).save(any(Wallet.class));
        verify(walletEventPublisher).publishWalletCreated(eq("ACC-NEW"), anyString());
    }

    @Test
    @DisplayName("Should return existing wallet if already exists during creation")
    void shouldReturnExistingWalletIfAlreadyExists() {
        // Given
        when(walletPersistencePort.findByAccountId("ACC-001")).thenReturn(Optional.of(testWallet));

        // When
        Wallet result = walletService.createWallet("ACC-001");

        // Then
        assertThat(result).isEqualTo(testWallet);
        verify(walletPersistencePort, never()).save(any(Wallet.class));
    }
}
