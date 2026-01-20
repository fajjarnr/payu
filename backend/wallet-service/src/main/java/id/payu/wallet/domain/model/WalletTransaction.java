package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WalletTransaction represents a ledger entry for wallet operations.
 * Each transaction is immutable - records CREDIT or DEBIT operations.
 */
public class WalletTransaction {

    private UUID id;
    private UUID walletId;
    private String referenceId;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt;

    public WalletTransaction() {
    }

    public WalletTransaction(UUID id, UUID walletId, String referenceId, TransactionType type, BigDecimal amount, BigDecimal balanceAfter, String description, LocalDateTime createdAt) {
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

    public static WalletTransactionBuilder builder() {
        return new WalletTransactionBuilder();
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

    public static class WalletTransactionBuilder {
        private UUID id;
        private UUID walletId;
        private String referenceId;
        private TransactionType type;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String description;
        private LocalDateTime createdAt;

        WalletTransactionBuilder() {}

        public WalletTransactionBuilder id(UUID id) { this.id = id; return this; }
        public WalletTransactionBuilder walletId(UUID walletId) { this.walletId = walletId; return this; }
        public WalletTransactionBuilder referenceId(String referenceId) { this.referenceId = referenceId; return this; }
        public WalletTransactionBuilder type(TransactionType type) { this.type = type; return this; }
        public WalletTransactionBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public WalletTransactionBuilder balanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; return this; }
        public WalletTransactionBuilder description(String description) { this.description = description; return this; }
        public WalletTransactionBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public WalletTransaction build() {
            return new WalletTransaction(id, walletId, referenceId, type, amount, balanceAfter, description, createdAt);
        }
    }
}
