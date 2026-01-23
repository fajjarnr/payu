package id.payu.wallet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    @Test
    @DisplayName("Should create wallet with builder")
    void shouldCreateWalletWithBuilder() {
        UUID id = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(id)
                .accountId("ACC-001")
                .balance(new BigDecimal("10000000"))
                .reservedBalance(new BigDecimal("5000000"))
                .currency("IDR")
                .status(Wallet.WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertThat(wallet).isNotNull();
        assertThat(wallet.getId()).isEqualTo(id);
        assertThat(wallet.getAccountId()).isEqualTo("ACC-001");
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("10000000"));
    }

    @Test
    @DisplayName("Should calculate available balance")
    void shouldCalculateAvailableBalance() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("10000000"))
                .reservedBalance(new BigDecimal("3000000"))
                .build();

        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("7000000"));
    }

    @Test
    @DisplayName("Should return true when has sufficient balance")
    void shouldReturnTrueWhenHasSufficientBalance() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("10000000"))
                .reservedBalance(new BigDecimal("2000000"))
                .build();

        assertThat(wallet.hasSufficientBalance(new BigDecimal("5000000"))).isTrue();
    }

    @Test
    @DisplayName("Should return false when has insufficient balance")
    void shouldReturnFalseWhenHasInsufficientBalance() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("10000000"))
                .reservedBalance(new BigDecimal("2000000"))
                .build();

        assertThat(wallet.hasSufficientBalance(new BigDecimal("10000000"))).isFalse();
    }

    @Test
    @DisplayName("Should reserve balance")
    void shouldReserveBalance() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("10000000"))
                .reservedBalance(new BigDecimal("0"))
                .updatedAt(LocalDateTime.now())
                .build();

        wallet.reserve(new BigDecimal("5000000"));

        assertThat(wallet.getReservedBalance()).isEqualByComparingTo(new BigDecimal("5000000"));
    }

    @Test
    @DisplayName("Should commit reservation")
    void shouldCommitReservation() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("10000000"))
                .reservedBalance(new BigDecimal("5000000"))
                .updatedAt(LocalDateTime.now())
                .build();

        wallet.commitReservation(new BigDecimal("5000000"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should release reservation")
    void shouldReleaseReservation() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("10000000"))
                .reservedBalance(new BigDecimal("5000000"))
                .updatedAt(LocalDateTime.now())
                .build();

        wallet.releaseReservation(new BigDecimal("5000000"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("10000000"));
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should credit amount to wallet")
    void shouldCreditAmountToWallet() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("10000000"))
                .updatedAt(LocalDateTime.now())
                .build();

        wallet.credit(new BigDecimal("5000000"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("15000000"));
    }


}
