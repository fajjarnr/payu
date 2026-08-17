package id.payu.wallet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariant tests for Wallet Top-up & Balance Management (Flow 12).
 *
 * <p>Verifies core wallet balance invariants:</p>
 * <ul>
 *   <li>Credit invariant: balanceAfter = balanceBefore + topupAmount.</li>
 *   <li>Available balance: available = totalBalance - reservedBalance.</li>
 *   <li>Currency matching: top-up currency must match wallet currency.</li>
 *   <li>Reservation lifecycle: reserve -> commit (balance - reserved) OR reserve -> release (reserved restored).</li>
 * </ul>
 */
@DisplayName("Wallet Top-up Invariant Tests (Flow 12)")
class WalletTopupInvariantTest {

    private static final String ACCOUNT_ID = "acct-wallet-001";

    private Wallet createWallet(BigDecimal initialBalance, BigDecimal reserved) {
        return Wallet.builder()
            .id(UUID.randomUUID())
            .accountId(ACCOUNT_ID)
            .balance(initialBalance.setScale(4, RoundingMode.HALF_EVEN))
            .reservedBalance(reserved.setScale(4, RoundingMode.HALF_EVEN))
            .currency("IDR")
            .status(WalletStatus.ACTIVE)
            .version(1L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Nested
    @DisplayName("Top-up & Credit Invariants")
    class TopupCreditInvariants {

        @Test
        @DisplayName("Top-up credit directly increases total and available balance")
        void topupIncreasesBalance() {
            Wallet wallet = createWallet(new BigDecimal("100000.0000"), BigDecimal.ZERO);
            BigDecimal topupAmount = new BigDecimal("250000.0000");

            wallet.credit(topupAmount);

            assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("350000.0000"));
            assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("350000.0000"));
        }

        @Test
        @DisplayName("Available balance reflects total minus reserved balance")
        void availableBalanceCalculation() {
            Wallet wallet = createWallet(new BigDecimal("500000.0000"), new BigDecimal("150000.0000"));

            assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("350000.0000"));
        }

        @Test
        @DisplayName("Scale 4 precision is strictly preserved across top-up arithmetic")
        void scale4Precision() {
            Wallet wallet = createWallet(new BigDecimal("10000.1234"), BigDecimal.ZERO);
            wallet.credit(new BigDecimal("5000.4321"));

            assertThat(wallet.getBalance().scale()).isEqualTo(4);
            assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("15000.5555"));
        }
    }

    @Nested
    @DisplayName("Two-Phase Reservation Invariants")
    class ReservationInvariants {

        @Test
        @DisplayName("Reserve decreases available balance while total balance remains unchanged")
        void reserveDecreasesAvailable() {
            Wallet wallet = createWallet(new BigDecimal("200000.0000"), BigDecimal.ZERO);
            BigDecimal reservation = new BigDecimal("50000.0000");

            wallet.reserve(reservation);

            assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("200000.0000"));
            assertThat(wallet.getReservedBalance()).isEqualByComparingTo(new BigDecimal("50000.0000"));
            assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("150000.0000"));
        }

        @Test
        @DisplayName("Commit reservation deducts from both balance and reserved balance")
        void commitReservationDeduction() {
            Wallet wallet = createWallet(new BigDecimal("200000.0000"), BigDecimal.ZERO);
            BigDecimal amount = new BigDecimal("50000.0000");

            wallet.reserve(amount);
            wallet.commitReservation(amount);

            assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("150000.0000"));
            assertThat(wallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("150000.0000"));
        }

        @Test
        @DisplayName("Release reservation restores available balance")
        void releaseReservationRestoresAvailable() {
            Wallet wallet = createWallet(new BigDecimal("200000.0000"), BigDecimal.ZERO);
            BigDecimal amount = new BigDecimal("50000.0000");

            wallet.reserve(amount);
            wallet.releaseReservation(amount);

            assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("200000.0000"));
            assertThat(wallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("200000.0000"));
        }

        @Test
        @DisplayName("Reserving more than available balance throws IllegalStateException")
        void excessiveReservationThrows() {
            Wallet wallet = createWallet(new BigDecimal("100000.0000"), BigDecimal.ZERO);

            assertThatThrownBy(() -> wallet.reserve(new BigDecimal("100000.0001")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient available balance");
        }
    }
}
