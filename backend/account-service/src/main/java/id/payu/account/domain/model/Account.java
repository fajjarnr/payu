package id.payu.account.domain.model;

import id.payu.transaction.domain.model.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Account domain model with rich domain behavior.
 *
 * <p>This is the core business entity following DDD principles.
 * Business logic is encapsulated within the entity rather than
 * being scattered across service classes (anemic domain model anti-pattern).</p>
 *
 * <p>Domain behaviors:</p>
 * <ul>
 *   <li>Credit and debit operations with business rule enforcement</li>
 *   <li>Account lifecycle management (freeze, close, activate)</li>
 *   <li>Ownership verification for security</li>
 * </ul>
 *
 * @see AccountStatus
 * @see Money
 */
public class Account {

    private UUID id;
    private String externalId;
    private UUID userId;
    private String accountNumber;
    private String accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private String currency;

    /**
     * @deprecated Use Money instead. Kept for backward compatibility.
     */
    @Deprecated
    private transient Money moneyValue;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Minimum balance requirements by account type
    private static final BigDecimal MINIMUM_SAVINGS_BALANCE = new BigDecimal("10000");
    private static final BigDecimal MINIMUM_CHECKING_BALANCE = new BigDecimal("50000");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // Constructors
    public Account() {
    }

    public Account(UUID id, String externalId, UUID userId, String accountNumber,
                   String accountType, AccountStatus status, BigDecimal balance,
                   String currency, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.externalId = externalId;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.status = status;
        this.balance = balance;
        this.currency = currency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ==================== DOMAIN BEHAVIORS ====================

    /**
     * Credits (adds) funds to this account.
     *
     * Business rules:
     * - Account must be active
     * - Amount must be positive
     *
     * @param amount the amount to credit
     * @throws IllegalArgumentException if account is not active
     * @throws IllegalArgumentException if amount is negative
     */
    public void credit(BigDecimal amount) {
        assertAccountActive();
        assertPositiveAmount(amount);

        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Credits Money to this account.
     *
     * @param money the Money to credit
     * @throws IllegalArgumentException if account is not active
     * @throws IllegalArgumentException if currency doesn't match
     */
    public void credit(Money money) {
        assertAccountActive();
        assertCurrencyMatches(money);

        this.balance = this.balance.add(money.getAmount());
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Debits (subtracts) funds from this account.
     *
     * Business rules:
     * - Account must be active
     * - Sufficient funds must be available
     * - Minimum balance requirement must be maintained
     *
     * @param amount the amount to debit
     * @throws IllegalArgumentException if account is not active
     * @throws IllegalArgumentException if amount is negative
     * @throws InsufficientFundsException if insufficient balance
     */
    public void debit(BigDecimal amount) {
        assertAccountActive();
        assertPositiveAmount(amount);
        assertSufficientFunds(amount);
        assertMinimumBalanceAfterDebit(amount);

        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Debits Money from this account.
     *
     * @param money the Money to debit
     * @throws IllegalArgumentException if account is not active
     * @throws IllegalArgumentException if currency doesn't match
     * @throws InsufficientFundsException if insufficient balance
     */
    public void debit(Money money) {
        assertAccountActive();
        assertCurrencyMatches(money);
        assertSufficientFunds(money.getAmount());
        assertMinimumBalanceAfterDebit(money.getAmount());

        this.balance = this.balance.subtract(money.getAmount());
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Freezes this account, preventing all transactions.
     *
     * Business rules:
     * - Account must be active to be frozen
     *
     * @throws IllegalStateException if account is not active
     */
    public void freeze() {
        if (!isActive()) {
            throw new IllegalStateException("Cannot freeze non-active account. Current status: " + status);
        }
        this.status = AccountStatus.FROZEN;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Unfreezes this account, allowing transactions.
     *
     * Business rules:
     * - Account must be frozen to be unfrozen
     *
     * @throws IllegalStateException if account is not frozen
     */
    public void unfreeze() {
        if (!isFrozen()) {
            throw new IllegalStateException("Cannot unfreeze non-frozen account. Current status: " + status);
        }
        this.status = AccountStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Closes this account permanently.
     *
     * Business rules:
     * - Balance must be zero
     * - Account must not already be closed
     *
     * @throws IllegalStateException if balance is not zero
     * @throws IllegalStateException if account is already closed
     */
    public void close() {
        if (isClosed()) {
            throw new IllegalStateException("Account is already closed");
        }
        if (!this.balance.equals(ZERO)) {
            throw new IllegalStateException("Cannot close account with non-zero balance: " + this.balance);
        }
        this.status = AccountStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Activates this account after verification.
     *
     * Business rules:
     * - Account must be in pending verification status
     *
     * @throws IllegalStateException if account is not pending verification
     */
    public void activate() {
        if (status != AccountStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Only pending verification accounts can be activated. Current status: " + status);
        }
        this.status = AccountStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== QUERY METHODS ====================

    /**
     * Checks if this account is active and can process transactions.
     *
     * @return true if account is active
     */
    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Checks if this account is frozen.
     *
     * @return true if account is frozen
     */
    public boolean isFrozen() {
        return status == AccountStatus.FROZEN;
    }

    /**
     * Checks if this account is closed.
     *
     * @return true if account is closed
     */
    public boolean isClosed() {
        return status == AccountStatus.CLOSED;
    }

    /**
     * Checks if this account is pending verification.
     *
     * @return true if account is pending verification
     */
    public boolean isPendingVerification() {
        return status == AccountStatus.PENDING_VERIFICATION;
    }

    /**
     * Checks if this account is owned by the specified user.
     *
     * @param userId the user ID to check
     * @return true if the account belongs to the user
     */
    public boolean isOwnedBy(UUID userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    /**
     * Gets the current balance as Money.
     *
     * @return the balance as Money
     */
    public Money getBalanceAsMoney() {
        if (moneyValue == null) {
            moneyValue = Money.of(this.balance, this.currency);
        }
        return moneyValue;
    }

    /**
     * Checks if the account has sufficient funds for a debit operation.
     *
     * @param amount the amount to check
     * @return true if sufficient funds are available
     */
    public boolean hasSufficientFunds(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    /**
     * Checks if the account can maintain minimum balance after a debit.
     *
     * @param debitAmount the amount to debit
     * @return true if minimum balance can be maintained
     */
    public boolean canMaintainMinimumBalance(BigDecimal debitAmount) {
        BigDecimal postDebitBalance = this.balance.subtract(debitAmount);
        BigDecimal minimumRequired = getMinimumBalanceForType();
        return postDebitBalance.compareTo(minimumRequired) >= 0;
    }

    // ==================== PRIVATE HELPERS ====================

    private void assertAccountActive() {
        if (!isActive()) {
            throw new IllegalStateException("Account is not active. Current status: " + status);
        }
    }

    private void assertPositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }

    private void assertSufficientFunds(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                "Insufficient funds. Current balance: " + this.balance + ", required: " + amount);
        }
    }

    private void assertMinimumBalanceAfterDebit(BigDecimal amount) {
        BigDecimal postDebitBalance = this.balance.subtract(amount);
        BigDecimal minimumRequired = getMinimumBalanceForType();

        if (postDebitBalance.compareTo(minimumRequired) < 0) {
            throw new InsufficientFundsException(
                "Debit would violate minimum balance requirement. Minimum: " + minimumRequired +
                ", post-debit balance: " + postDebitBalance);
        }
    }

    private void assertCurrencyMatches(Money money) {
        if (!this.currency.equals(money.getCurrency().getCurrencyCode())) {
            throw new IllegalArgumentException(
                "Currency mismatch. Account currency: " + this.currency +
                ", payment currency: " + money.getCurrency().getCurrencyCode());
        }
    }

    private BigDecimal getMinimumBalanceForType() {
        if ("SAVINGS".equals(this.accountType)) {
            return MINIMUM_SAVINGS_BALANCE;
        } else if ("CHECKING".equals(this.accountType)) {
            return MINIMUM_CHECKING_BALANCE;
        }
        return ZERO; // Pocket accounts have no minimum
    }

    // ==================== EXCEPTIONS ====================

    /**
     * Exception thrown when account has insufficient funds.
     */
    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }

    // ==================== GETTERS AND SETTERS ====================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String externalId;
        private UUID userId;
        private String accountNumber;
        private String accountType;
        private AccountStatus status;
        private BigDecimal balance;
        private String currency;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder accountType(String accountType) {
            this.accountType = accountType;
            return this;
        }

        public Builder status(AccountStatus status) {
            this.status = status;
            return this;
        }

        public Builder balance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Account build() {
            return new Account(id, externalId, userId, accountNumber, accountType,
                    status, balance, currency, createdAt, updatedAt);
        }
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return id != null && id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", externalId='" + externalId + '\'' +
                ", userId=" + userId +
                ", accountNumber='" + accountNumber + '\'' +
                ", accountType='" + accountType + '\'' +
                ", status=" + status +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public enum AccountStatus {
        ACTIVE,
        FROZEN,
        CLOSED,
        PENDING_VERIFICATION
    }
}
