package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wallet domain entity representing an account's balance.
 * Core domain model - no JPA annotations here (Clean Architecture).
 */
public class Wallet {
    
    private UUID id;
    private String accountId;
    private BigDecimal balance;
    private BigDecimal reservedBalance;
    private String currency;
    private WalletStatus status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Wallet() {
    }

    public Wallet(UUID id, String accountId, BigDecimal balance, BigDecimal reservedBalance, String currency, WalletStatus status, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.accountId = accountId;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.currency = currency;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Get available balance (total balance minus reserved).
     */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }

    /**
     * Check if wallet has sufficient available balance.
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return getAvailableBalance().compareTo(amount) >= 0;
    }

    /**
     * Reserve an amount from available balance.
     */
    public void reserve(BigDecimal amount) {
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient available balance");
        }
        this.reservedBalance = this.reservedBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Commit a reserved amount (deduct from balance).
     */
    public void commitReservation(BigDecimal amount) {
        if (reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Reserved amount not found");
        }
        this.balance = this.balance.subtract(amount);
        this.reservedBalance = this.reservedBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Release a reserved amount back to available.
     */
    public void releaseReservation(BigDecimal amount) {
        if (reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Reserved amount not found");
        }
        this.reservedBalance = this.reservedBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Credit amount to wallet (e.g., incoming transfer).
     */
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public enum WalletStatus {
        ACTIVE,
        FROZEN,
        CLOSED
    }

    public static WalletBuilder builder() {
        return new WalletBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getReservedBalance() { return reservedBalance; }
    public void setReservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public WalletStatus getStatus() { return status; }
    public void setStatus(WalletStatus status) { this.status = status; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class WalletBuilder {
        private UUID id;
        private String accountId;
        private BigDecimal balance;
        private BigDecimal reservedBalance;
        private String currency;
        private WalletStatus status;
        private Long version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        WalletBuilder() {}

        public WalletBuilder id(UUID id) { this.id = id; return this; }
        public WalletBuilder accountId(String accountId) { this.accountId = accountId; return this; }
        public WalletBuilder balance(BigDecimal balance) { this.balance = balance; return this; }
        public WalletBuilder reservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; return this; }
        public WalletBuilder currency(String currency) { this.currency = currency; return this; }
        public WalletBuilder status(WalletStatus status) { this.status = status; return this; }
        public WalletBuilder version(Long version) { this.version = version; return this; }
        public WalletBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public WalletBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Wallet build() {
            return new Wallet(id, accountId, balance, reservedBalance, currency, status, version, createdAt, updatedAt);
        }
    }
}
