package id.payu.account.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P0 Critical Financial Integrity Tests for Account domain model.
 *
 * <p>These tests verify the core financial operations that MUST work correctly
 * for PCI-DSS compliance and financial integrity.</p>
 *
 * <p>Test Categories:</p>
 * <ul>
 *   <li>BigDecimal Precision - No floating-point errors</li>
 *   <li>No Negative Balance - Unless overdraft enabled</li>
 *   <li>Concurrent Operations - Optimistic locking behavior</li>
 *   <li>Balance Overflow Protection - Maximum balance limits</li>
 *   <li>Minimum Balance Requirements - Per account type</li>
 * </ul>
 *
 * <p>Priority: P0 - Must Pass Before Release</p>
 */
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("P0: Account Financial Integrity Tests")
class AccountFinancialIntegrityTest {

    private UUID accountId;
    private UUID userId;
    private String accountNumber;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        userId = UUID.randomUUID();
        accountNumber = "1234567890";
    }

    // ==================== BIGDECIMAL PRECISION TESTS ====================

    @Nested
    @DisplayName("BigDecimal Precision - No Floating Point Errors")
    class BigDecimalPrecisionTests {

        @Test
        @DisplayName("Should handle decimal precision correctly without floating point errors")
        void shouldHandleDecimalPrecisionCorrectly() {
            Account account = createSavingsAccount(new BigDecimal("100000.50"));

            account.credit(new BigDecimal("0.50"));

            // Verify exact decimal arithmetic - no floating point errors
            assertThat(account.getBalance()).isEqualTo(new BigDecimal("100001.00"));
        }

        @Test
        @DisplayName("Should handle large decimal amounts precisely")
        void shouldHandleLargeDecimalAmountsPrecisely() {
            Account account = createSavingsAccount(new BigDecimal("999999999.99"));

            account.credit(new BigDecimal("0.01"));

            assertThat(account.getBalance()).isEqualTo(new BigDecimal("1000000000.00"));
        }

        @Test
        @DisplayName("Should subtract precise decimal amounts")
        void shouldSubtractPreciseDecimalAmounts() {
            Account account = createSavingsAccount(new BigDecimal("100000.99"));

            account.debit(new BigDecimal("0.99"));

            assertThat(account.getBalance()).isEqualTo(new BigDecimal("100000.00"));
        }

        @Test
        @DisplayName("Should handle scale(2) values correctly")
        void shouldHandleScale2ValuesCorrectly() {
            Account account = createSavingsAccount(new BigDecimal("100.00"));

            account.credit(new BigDecimal("50.50"));
            account.debit(new BigDecimal("25.25"));

            assertThat(account.getBalance()).isEqualTo(new BigDecimal("125.25"));
        }

        @Test
        @DisplayName("Should not lose precision on multiple operations")
        void shouldNotLosePrecisionOnMultipleOperations() {
            Account account = createSavingsAccount(new BigDecimal("1000.00"));

            account.credit(new BigDecimal("333.33"));
            account.credit(new BigDecimal("333.33"));
            account.credit(new BigDecimal("333.34"));

            assertThat(account.getBalance()).isEqualTo(new BigDecimal("2000.00"));
        }
    }

    // ==================== NO NEGATIVE BALANCE TESTS ====================

    @Nested
    @DisplayName("No Negative Balance Protection")
    class NoNegativeBalanceTests {

        @Test
        @DisplayName("Should prevent debit when insufficient funds")
        void shouldPreventDebitWhenInsufficientFunds() {
            Account account = createSavingsAccount(new BigDecimal("100000"));

            assertThatThrownBy(() -> account.debit(new BigDecimal("150000")))
                    .isInstanceOf(Account.InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient funds");
        }

        @Test
        @DisplayName("Should prevent debit when exact balance but minimum required")
        void shouldPreventDebitWhenExactBalanceButMinimumRequired() {
            Account account = createSavingsAccount(new BigDecimal("10000")); // Exactly minimum

            assertThatThrownBy(() -> account.debit(new BigDecimal("1")))
                    .isInstanceOf(Account.InsufficientFundsException.class)
                    .hasMessageContaining("minimum balance requirement");
        }

        @Test
        @DisplayName("Should allow debit to minimum balance")
        void shouldAllowDebitToMinimumBalance() {
            Account account = createSavingsAccount(new BigDecimal("20000"));

            account.debit(new BigDecimal("10000")); // Bring to minimum

            assertThat(account.getBalance()).isEqualTo(new BigDecimal("10000")); // Minimum
        }

        @Test
        @DisplayName("Should not change balance on failed debit")
        void shouldNotChangeBalanceOnFailedDebit() {
            Account account = createSavingsAccount(new BigDecimal("100000"));
            BigDecimal originalBalance = account.getBalance();

            try {
                account.debit(new BigDecimal("150000"));
            } catch (Exception e) {
                // Expected
            }

            // Balance should remain unchanged
            assertThat(account.getBalance()).isEqualTo(originalBalance);
        }

        @Test
        @DisplayName("Should prevent zero or negative debit amounts")
        void shouldPreventZeroOrNegativeDebitAmounts() {
            Account account = createSavingsAccount(new BigDecimal("100000"));

            assertThatThrownBy(() -> account.debit(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> account.debit(new BigDecimal("-1000")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== MAXIMUM BALANCE TESTS ====================

    @Nested
    @DisplayName("Maximum Balance Protection")
    class MaximumBalanceTests {

        @Test
        @DisplayName("Should prevent credit exceeding maximum balance")
        void shouldPreventCreditExceedingMaximumBalance() {
            Account account = createSavingsAccount(new BigDecimal("999999999999.00"));

            assertThatThrownBy(() -> account.credit(new BigDecimal("1.00")))
                    .isInstanceOf(Account.InsufficientFundsException.class)
                    .hasMessageContaining("maximum balance limit");
        }

        @Test
        @DisplayName("Should allow credit up to maximum balance")
        void shouldAllowCreditUpToMaximumBalance() {
            Account account = createSavingsAccount(new BigDecimal("999999999998.00"));

            account.credit(new BigDecimal("1.00"));

            assertThat(account.getBalance()).isEqualTo(new BigDecimal("999999999999.00"));
        }

        @Test
        @DisplayName("Should not change balance on failed credit")
        void shouldNotChangeBalanceOnFailedCredit() {
            Account account = createSavingsAccount(new BigDecimal("999999999999.00"));
            BigDecimal originalBalance = account.getBalance();

            try {
                account.credit(new BigDecimal("1.00"));
            } catch (Exception e) {
                // Expected
            }

            assertThat(account.getBalance()).isEqualTo(originalBalance);
        }

        @Test
        @DisplayName("Should prevent zero or negative credit amounts")
        void shouldPreventZeroOrNegativeCreditAmounts() {
            Account account = createSavingsAccount(new BigDecimal("100000"));

            assertThatThrownBy(() -> account.credit(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> account.credit(new BigDecimal("-1000")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== CONCURRENT OPERATIONS TESTS ====================

    @Nested
    @DisplayName("Concurrent Operations (P0 - Data Integrity)")
    class ConcurrentOperationsTests {

        @Test
        @DisplayName("Should handle concurrent debits correctly")
        void shouldHandleConcurrentDebitsCorrectly() throws Exception {
            Account account = createSavingsAccount(new BigDecimal("100000"));
            account.setVersion(1L); // Enable optimistic locking

            ExecutorService executor = Executors.newFixedThreadPool(2);

            // Two concurrent debits of 60000 each (total 120000, but only 100000 available)
            CompletableFuture<Void> debit1 = CompletableFuture.runAsync(
                    () -> {
                        try {
                            account.debit(new BigDecimal("60000"));
                        } catch (Exception e) {
                            // One should fail
                            assertThat(e).isInstanceOf(Account.InsufficientFundsException.class);
                        }
                    }, executor);

            CompletableFuture<Void> debit2 = CompletableFuture.runAsync(
                    () -> {
                        try {
                            account.debit(new BigDecimal("60000"));
                        } catch (Exception e) {
                            // One should fail
                            assertThat(e).isInstanceOf(Account.InsufficientFundsException.class);
                        }
                    }, executor);

            CompletableFuture.allOf(debit1, debit2).join();

            // At least one debit should have failed, balance should be >= minimum
            assertThat(account.getBalance()).isGreaterThanOrEqualTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("Should handle concurrent credits correctly")
        void shouldHandleConcurrentCreditsCorrectly() throws Exception {
            Account account = createSavingsAccount(new BigDecimal("100000"));

            ExecutorService executor = Executors.newFixedThreadPool(3);

            // Three concurrent credits
            CompletableFuture<Void> credit1 = CompletableFuture.runAsync(
                    () -> account.credit(new BigDecimal("50000")), executor);

            CompletableFuture<Void> credit2 = CompletableFuture.runAsync(
                    () -> account.credit(new BigDecimal("30000")), executor);

            CompletableFuture<Void> credit3 = CompletableFuture.runAsync(
                    () -> account.credit(new BigDecimal("20000")), executor);

            CompletableFuture.allOf(credit1, credit2, credit3).join();

            // All credits should succeed
            assertThat(account.getBalance()).isEqualTo(new BigDecimal("200000"));
        }

        @Test
        @DisplayName("Should handle mixed concurrent operations")
        void shouldHandleMixedConcurrentOperations() throws Exception {
            Account account = createSavingsAccount(new BigDecimal("100000"));

            ExecutorService executor = Executors.newFixedThreadPool(4);

            // Mixed operations
            CompletableFuture<Void> credit1 = CompletableFuture.runAsync(
                    () -> account.credit(new BigDecimal("50000")), executor);

            CompletableFuture<Void> debit1 = CompletableFuture.runAsync(
                    () -> {
                        try {
                            account.debit(new BigDecimal("30000"));
                        } catch (Exception e) {
                            // May fail due to timing
                        }
                    }, executor);

            CompletableFuture<Void> credit2 = CompletableFuture.runAsync(
                    () -> account.credit(new BigDecimal("20000")), executor);

            CompletableFuture<Void> debit2 = CompletableFuture.runAsync(
                    () -> {
                        try {
                            account.debit(new BigDecimal("40000"));
                        } catch (Exception e) {
                            // May fail due to timing
                        }
                    }, executor);

            CompletableFuture.allOf(credit1, debit1, credit2, debit2).join();

            // Final balance should be consistent
            assertThat(account.getBalance()).isGreaterThanOrEqualTo(new BigDecimal("10000"));
        }
    }

    // ==================== MINIMUM BALANCE BY TYPE TESTS ====================

    @Nested
    @DisplayName("Minimum Balance Requirements by Account Type")
    class MinimumBalanceByTypeTests {

        @Test
        @DisplayName("Should enforce 10000 minimum for savings accounts")
        void shouldEnforce10000MinimumForSavingsAccounts() {
            Account account = createSavingsAccount(new BigDecimal("15000"));

            account.debit(new BigDecimal("5000")); // Bring to minimum

            assertThat(account.getBalance()).isEqualTo(new BigDecimal("10000"));

            assertThatThrownBy(() -> account.debit(new BigDecimal("1")))
                    .isInstanceOf(Account.InsufficientFundsException.class)
                    .hasMessageContaining("minimum balance requirement");
        }

        @Test
        @DisplayName("Should enforce 50000 minimum for checking accounts")
        void shouldEnforce50000MinimumForCheckingAccounts() {
            Account account = createCheckingAccount(new BigDecimal("75000"));

            account.debit(new BigDecimal("25000")); // Bring to minimum

            assertThat(account.getBalance()).isEqualTo(new BigDecimal("50000"));

            assertThatThrownBy(() -> account.debit(new BigDecimal("1")))
                    .isInstanceOf(Account.InsufficientFundsException.class)
                    .hasMessageContaining("minimum balance requirement");
        }

        @Test
        @DisplayName("Should allow zero minimum for pocket accounts")
        void shouldAllowZeroMinimumForPocketAccounts() {
            Account account = createPocketAccount(new BigDecimal("100"));

            account.debit(new BigDecimal("100")); // Bring to zero

            assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

            // Should allow full debit
            assertThatThrownBy(() -> account.debit(new BigDecimal("1")))
                    .isInstanceOf(Account.InsufficientFundsException.class);
        }
    }

    // ==================== ACCOUNT STATUS TESTS ====================

    @Nested
    @DisplayName("Account Status Validation")
    class AccountStatusTests {

        @Test
        @DisplayName("Should not allow debit on frozen account")
        void shouldNotAllowDebitOnFrozenAccount() {
            Account account = createSavingsAccount(new BigDecimal("100000"));
            account.freeze();

            assertThatThrownBy(() -> account.debit(new BigDecimal("50000")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Account is not active");
        }

        @Test
        @DisplayName("Should not allow credit on frozen account")
        void shouldNotAllowCreditOnFrozenAccount() {
            Account account = createSavingsAccount(new BigDecimal("100000"));
            account.freeze();

            assertThatThrownBy(() -> account.credit(new BigDecimal("50000")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Account is not active");
        }

        @Test
        @DisplayName("Should not allow debit on closed account")
        void shouldNotAllowDebitOnClosedAccount() {
            Account account = createSavingsAccount(new BigDecimal("0"));
            account.close();

            assertThatThrownBy(() -> account.debit(new BigDecimal("50000")))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should not allow operations on pending verification account")
        void shouldNotAllowOperationsOnPendingVerificationAccount() {
            Account account = Account.builder()
                    .id(accountId)
                    .userId(userId)
                    .accountNumber(accountNumber)
                    .accountType("SAVINGS")
                    .status(Account.AccountStatus.PENDING_VERIFICATION)
                    .balance(new BigDecimal("100000"))
                    .currency("IDR")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            assertThatThrownBy(() -> account.debit(new BigDecimal("50000")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ==================== OWNERSHIP VERIFICATION TESTS ====================

    @Nested
    @DisplayName("Resource Ownership Verification")
    class OwnershipVerificationTests {

        @Test
        @DisplayName("Should verify account ownership correctly")
        void shouldVerifyAccountOwnershipCorrectly() {
            UUID ownerUserId = UUID.randomUUID();
            Account account = createAccountWithUserId(ownerUserId);

            assertThat(account.isOwnedBy(ownerUserId)).isTrue();
            assertThat(account.isOwnedBy(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Should return false for null userId")
        void shouldReturnFalseForNullUserId() {
            Account account = createSavingsAccount(new BigDecimal("100000"));

            assertThat(account.isOwnedBy((UUID) null)).isFalse();
        }

        @Test
        @DisplayName("Should return false when account userId is null")
        void shouldReturnFalseWhenAccountUserIdIsNull() {
            Account account = Account.builder()
                    .id(accountId)
                    .userId(null)
                    .accountNumber(accountNumber)
                    .accountType("SAVINGS")
                    .status(Account.AccountStatus.ACTIVE)
                    .balance(new BigDecimal("100000"))
                    .currency("IDR")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            assertThat(account.isOwnedBy(UUID.randomUUID())).isFalse();
        }
    }

    // ==================== QUERY METHOD TESTS ====================

    @Nested
    @DisplayName("Query Methods")
    class QueryMethodTests {

        @Test
        @DisplayName("Should check sufficient funds correctly")
        void shouldCheckSufficientFundsCorrectly() {
            Account account = createSavingsAccount(new BigDecimal("100000"));

            assertThat(account.hasSufficientFunds(new BigDecimal("50000"))).isTrue();
            assertThat(account.hasSufficientFunds(new BigDecimal("100000"))).isTrue();
            assertThat(account.hasSufficientFunds(new BigDecimal("100001"))).isFalse();
        }

        @Test
        @DisplayName("Should check can maintain minimum balance correctly")
        void shouldCheckCanMaintainMinimumBalanceCorrectly() {
            Account account = createSavingsAccount(new BigDecimal("20000"));

            assertThat(account.canMaintainMinimumBalance(new BigDecimal("5000"))).isTrue();
            assertThat(account.canMaintainMinimumBalance(new BigDecimal("10000"))).isTrue();
            assertThat(account.canMaintainMinimumBalance(new BigDecimal("10001"))).isFalse();
        }

        @Test
        @DisplayName("Should return correct status for active account")
        void shouldReturnCorrectStatusForActiveAccount() {
            Account account = createSavingsAccount(new BigDecimal("100000"));

            assertThat(account.isActive()).isTrue();
            assertThat(account.isFrozen()).isFalse();
            assertThat(account.isClosed()).isFalse();
        }

        @Test
        @DisplayName("Should return correct status for frozen account")
        void shouldReturnCorrectStatusForFrozenAccount() {
            Account account = createSavingsAccount(new BigDecimal("100000"));
            account.freeze();

            assertThat(account.isActive()).isFalse();
            assertThat(account.isFrozen()).isTrue();
        }

        @Test
        @DisplayName("Should return correct status for closed account")
        void shouldReturnCorrectStatusForClosedAccount() {
            Account account = createSavingsAccount(new BigDecimal("0"));
            account.close();

            assertThat(account.isActive()).isFalse();
            assertThat(account.isClosed()).isTrue();
        }
    }

    // ==================== ACCOUNT LIFECYCLE TESTS ====================

    @Nested
    @DisplayName("Account Lifecycle Operations")
    class AccountLifecycleTests {

        @Test
        @DisplayName("Should freeze active account successfully")
        void shouldFreezeActiveAccountSuccessfully() {
            Account account = createSavingsAccount(new BigDecimal("100000"));

            account.freeze();

            assertThat(account.getStatus()).isEqualTo(Account.AccountStatus.FROZEN);
        }

        @Test
        @DisplayName("Should not freeze non-active account")
        void shouldNotFreezeNonActiveAccount() {
            Account account = createSavingsAccount(new BigDecimal("100000"));
            account.close();

            assertThatThrownBy(() -> account.freeze())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should unfreeze frozen account successfully")
        void shouldUnfreezeFrozenAccountSuccessfully() {
            Account account = createSavingsAccount(new BigDecimal("100000"));
            account.freeze();

            account.unfreeze();

            assertThat(account.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should not unfreeze non-frozen account")
        void shouldNotUnfreezeNonFrozenAccount() {
            Account account = createSavingsAccount(new BigDecimal("100000"));

            assertThatThrownBy(() -> account.unfreeze())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should close account with zero balance")
        void shouldCloseAccountWithZeroBalance() {
            Account account = createSavingsAccount(new BigDecimal("0"));

            account.close();

            assertThat(account.getStatus()).isEqualTo(Account.AccountStatus.CLOSED);
        }

        @Test
        @DisplayName("Should not close account with non-zero balance")
        void shouldNotCloseAccountWithNonZeroBalance() {
            Account account = createSavingsAccount(new BigDecimal("100"));

            assertThatThrownBy(() -> account.close())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot close account with non-zero balance");
        }

        @Test
        @DisplayName("Should activate pending verification account")
        void shouldActivatePendingVerificationAccount() {
            Account account = Account.builder()
                    .id(accountId)
                    .userId(userId)
                    .accountNumber(accountNumber)
                    .accountType("SAVINGS")
                    .status(Account.AccountStatus.PENDING_VERIFICATION)
                    .balance(new BigDecimal("0"))
                    .currency("IDR")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            account.activate();

            assertThat(account.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
        }
    }

    // ==================== HELPER METHODS ====================

    private Account createSavingsAccount(BigDecimal balance) {
        return Account.builder()
                .id(accountId)
                .userId(userId)
                .accountNumber(accountNumber)
                .accountType("SAVINGS")
                .status(Account.AccountStatus.ACTIVE)
                .balance(balance)
                .currency("IDR")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Account createCheckingAccount(BigDecimal balance) {
        return Account.builder()
                .id(accountId)
                .userId(userId)
                .accountNumber(accountNumber)
                .accountType("CHECKING")
                .status(Account.AccountStatus.ACTIVE)
                .balance(balance)
                .currency("IDR")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Account createPocketAccount(BigDecimal balance) {
        return Account.builder()
                .id(accountId)
                .userId(userId)
                .accountNumber(accountNumber)
                .accountType("POCKET")
                .status(Account.AccountStatus.ACTIVE)
                .balance(balance)
                .currency("IDR")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Account createAccountWithUserId(UUID userId) {
        return Account.builder()
                .id(accountId)
                .userId(userId)
                .accountNumber(accountNumber)
                .accountType("SAVINGS")
                .status(Account.AccountStatus.ACTIVE)
                .balance(new BigDecimal("100000"))
                .currency("IDR")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
