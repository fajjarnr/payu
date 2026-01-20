package id.payu.wallet.adapter.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for Wallet - Infrastructure layer.
 */
@Entity
@Table(name = "wallets", indexes = {
    @Index(name = "idx_wallet_account_id", columnList = "accountId", unique = true)
})
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedBalance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public WalletEntity() {
    }

    public WalletEntity(UUID id, String accountId, BigDecimal balance, BigDecimal reservedBalance, String currency, WalletStatus status, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
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

    public enum WalletStatus {
        ACTIVE,
        FROZEN,
        CLOSED
    }

    public static WalletEntityBuilder builder() {
        return new WalletEntityBuilder();
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

    public static class WalletEntityBuilder {
        private UUID id;
        private String accountId;
        private BigDecimal balance;
        private BigDecimal reservedBalance;
        private String currency;
        private WalletStatus status;
        private Long version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        WalletEntityBuilder() {}

        public WalletEntityBuilder id(UUID id) { this.id = id; return this; }
        public WalletEntityBuilder accountId(String accountId) { this.accountId = accountId; return this; }
        public WalletEntityBuilder balance(BigDecimal balance) { this.balance = balance; return this; }
        public WalletEntityBuilder reservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; return this; }
        public WalletEntityBuilder currency(String currency) { this.currency = currency; return this; }
        public WalletEntityBuilder status(WalletStatus status) { this.status = status; return this; }
        public WalletEntityBuilder version(Long version) { this.version = version; return this; }
        public WalletEntityBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public WalletEntityBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public WalletEntity build() {
            return new WalletEntity(id, accountId, balance, reservedBalance, currency, status, version, createdAt, updatedAt);
        }
    }
}
