package id.payu.wallet.adapter.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for WalletTransaction (ledger entry) - Infrastructure layer.
 */
@Entity
@Table(name = "wallet_transactions", indexes = {
    @Index(name = "idx_txn_wallet_id", columnList = "walletId"),
    @Index(name = "idx_txn_reference_id", columnList = "referenceId")
})
public class WalletTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID walletId;

    @Column(nullable = false)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public WalletTransactionEntity() {
    }

    public WalletTransactionEntity(UUID id, UUID walletId, String referenceId, TransactionType type, BigDecimal amount, BigDecimal balanceAfter, String description, LocalDateTime createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.referenceId = referenceId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = createdAt;
    }

    public enum TransactionType {
        CREDIT,
        DEBIT
    }

    public static WalletTransactionEntityBuilder builder() {
        return new WalletTransactionEntityBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class WalletTransactionEntityBuilder {
        private UUID id;
        private UUID walletId;
        private String referenceId;
        private TransactionType type;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String description;
        private LocalDateTime createdAt;

        WalletTransactionEntityBuilder() {}

        public WalletTransactionEntityBuilder id(UUID id) { this.id = id; return this; }
        public WalletTransactionEntityBuilder walletId(UUID walletId) { this.walletId = walletId; return this; }
        public WalletTransactionEntityBuilder referenceId(String referenceId) { this.referenceId = referenceId; return this; }
        public WalletTransactionEntityBuilder type(TransactionType type) { this.type = type; return this; }
        public WalletTransactionEntityBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public WalletTransactionEntityBuilder balanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; return this; }
        public WalletTransactionEntityBuilder description(String description) { this.description = description; return this; }
        public WalletTransactionEntityBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public WalletTransactionEntity build() {
            return new WalletTransactionEntity(id, walletId, referenceId, type, amount, balanceAfter, description, createdAt);
        }
    }
}
