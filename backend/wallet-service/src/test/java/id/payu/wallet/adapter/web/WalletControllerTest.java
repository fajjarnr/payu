package id.payu.wallet.adapter.web;

import id.payu.wallet.application.exception.WalletNotFoundException;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletUseCase walletUseCase;

    @InjectMocks
    private WalletController walletController;

    private Wallet testWallet;
    private LedgerEntry testLedgerEntry;
    private WalletTransaction testTransaction;

    @BeforeEach
    void setUp() {
        UUID walletId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        testWallet = Wallet.builder()
                .id(walletId)
                .accountId(accountId.toString())
                .balance(new BigDecimal("10000000"))
                .reservedBalance(new BigDecimal("5000000"))
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

        testTransaction = WalletTransaction.builder()
                .id(UUID.randomUUID())
                .walletId(testWallet.getId())
                .referenceId("REF-001")
                .type(WalletTransaction.TransactionType.CREDIT)
                .amount(new BigDecimal("5000000"))
                .balanceAfter(new BigDecimal("15000000"))
                .description("Test transaction")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should get balance")
    void shouldGetBalance() {
        when(walletUseCase.getWalletByAccountId(testWallet.getAccountId())).thenReturn(Optional.of(testWallet));

        ResponseEntity<BalanceResponse> response = walletController.getBalance(testWallet.getAccountId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccountId()).isEqualTo(testWallet.getAccountId());
        assertThat(response.getBody().getBalance()).isEqualByComparingTo(new BigDecimal("10000000"));
        assertThat(response.getBody().getAvailableBalance()).isEqualByComparingTo(new BigDecimal("5000000"));
    }

    @Test
    @DisplayName("Should throw exception when getting balance for non-existent wallet")
    void shouldThrowExceptionWhenGettingBalanceForNonExistentWallet() {
        String accountId = UUID.randomUUID().toString();
        when(walletUseCase.getWalletByAccountId(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletController.getBalance(accountId))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    @DisplayName("Should reserve balance")
    void shouldReserveBalance() {
        ReserveBalanceRequest request = new ReserveBalanceRequest(new BigDecimal("5000000"), "REF-001");

        when(walletUseCase.reserveBalance(testWallet.getAccountId(), new BigDecimal("5000000"), "REF-001"))
                .thenReturn(UUID.randomUUID().toString());

        ResponseEntity<ReserveBalanceResponse> response = walletController.reserveBalance(testWallet.getAccountId(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("RESERVED");
        assertThat(response.getBody().getReferenceId()).isEqualTo("REF-001");
    }

    @Test
    @DisplayName("Should commit reservation")
    void shouldCommitReservation() {
        String reservationId = UUID.randomUUID().toString();

        ResponseEntity<Map<String, String>> response = walletController.commitReservation(reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("COMMITTED");
        assertThat(response.getBody().get("reservationId")).isEqualTo(reservationId);
        verify(walletUseCase).commitReservation(reservationId);
    }

    @Test
    @DisplayName("Should release reservation")
    void shouldReleaseReservation() {
        String reservationId = UUID.randomUUID().toString();

        ResponseEntity<Map<String, String>> response = walletController.releaseReservation(reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("RELEASED");
        assertThat(response.getBody().get("reservationId")).isEqualTo(reservationId);
        verify(walletUseCase).releaseReservation(reservationId);
    }

    @Test
    @DisplayName("Should credit amount to wallet")
    void shouldCreditAmountToWallet() {
        CreditRequest request = new CreditRequest(new BigDecimal("5000000"), "REF-001", "Test credit");

        ResponseEntity<Map<String, String>> response = walletController.credit(testWallet.getAccountId(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("CREDITED");
        assertThat(response.getBody().get("accountId")).isEqualTo(testWallet.getAccountId());
        verify(walletUseCase).credit(testWallet.getAccountId(), new BigDecimal("5000000"), "REF-001", "Test credit");
    }

    @Test
    @DisplayName("Should get transaction history")
    void shouldGetTransactionHistory() {
        when(walletUseCase.getTransactionHistory(testWallet.getAccountId(), 0, 20)).thenReturn(List.of(testTransaction));

        ResponseEntity<List<WalletTransaction>> response = walletController.getTransactionHistory(testWallet.getAccountId(), 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getType()).isEqualTo(WalletTransaction.TransactionType.CREDIT);
    }

    @Test
    @DisplayName("Should get ledger entries by transaction ID")
    void shouldGetLedgerEntriesByTransactionId() {
        UUID transactionId = testLedgerEntry.getTransactionId();
        when(walletUseCase.getLedgerEntriesByTransactionId(transactionId))
                .thenReturn(List.of(testLedgerEntry));

        ResponseEntity<List<LedgerEntry>> response = walletController.getLedgerEntriesByTransaction(
                testWallet.getAccountId(), transactionId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
