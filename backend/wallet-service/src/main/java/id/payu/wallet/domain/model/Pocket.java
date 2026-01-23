package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Pocket {

    private UUID id;
    private String accountId;
    private String name;
    private String description;
    private String currency;
    private BigDecimal balance;
    private PocketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum PocketStatus {
        ACTIVE, FROZEN, CLOSED
    }

    public Pocket() {
    }

    public Pocket(UUID id, String accountId, String name, String description, 
               String currency, BigDecimal balance, PocketStatus status, 
               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.accountId = accountId;
        this.name = name;
        this.description = description;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance in pocket");
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void freeze() {
        this.status = PocketStatus.FROZEN;
        this.updatedAt = LocalDateTime.now();
    }

    public void unfreeze() {
        this.status = PocketStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot close pocket with non-zero balance");
        }
        this.status = PocketStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    public static PocketBuilder builder() {
        return new PocketBuilder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public PocketStatus getStatus() { return status; }
    public void setStatus(PocketStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class PocketBuilder {
        private UUID id;
        private String accountId;
        private String name;
        private String description;
        private String currency;
        private BigDecimal balance;
        private PocketStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        PocketBuilder() {}

        public PocketBuilder id(UUID id) { this.id = id; return this; }
        public PocketBuilder accountId(String accountId) { this.accountId = accountId; return this; }
        public PocketBuilder name(String name) { this.name = name; return this; }
        public PocketBuilder description(String description) { this.description = description; return this; }
        public PocketBuilder currency(String currency) { this.currency = currency; return this; }
        public PocketBuilder balance(BigDecimal balance) { this.balance = balance; return this; }
        public PocketBuilder status(PocketStatus status) { this.status = status; return this; }
        public PocketBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public PocketBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Pocket build() {
            return new Pocket(id, accountId, name, description, currency, balance, status, createdAt, updatedAt);
        }
    }
}
