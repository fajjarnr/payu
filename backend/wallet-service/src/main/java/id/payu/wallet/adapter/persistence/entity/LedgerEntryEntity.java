package id.payu.wallet.adapter.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_account_id", columnList = "account_id"),
    @Index(name = "idx_ledger_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_ledger_created_at", columnList = "account_id, created_at")
})
@NamedEntityGraph
public class LedgerEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private String entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "reference_type", nullable = true, length = 50)
    private String referenceType;

    @Column(name = "reference_id", nullable = true, length = 100)
    private String referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LedgerEntryEntity() {
    }

    public LedgerEntryEntity(UUID id, UUID transactionId, UUID accountId, String entryType, BigDecimal amount, String currency, BigDecimal balanceAfter, String referenceType, String referenceId, LocalDateTime createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.entryType = entryType;
        this.amount = amount;
        this.currency = currency;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (referenceId == null && referenceType == null) {
            referenceId = "INTERNAL";
            referenceType = "RESERVATION";
        }
    }

    public static LedgerEntryEntityBuilder builder() {
        return new LedgerEntryEntityBuilder();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class LedgerEntryEntityBuilder {
        private UUID id;
        private UUID transactionId;
        private UUID accountId;
        private String entryType;
        private BigDecimal amount;
        private String currency;
        private BigDecimal balanceAfter;
        private String referenceType;
        private String referenceId;
        private LocalDateTime createdAt;

        LedgerEntryEntityBuilder() {
        }

        public LedgerEntryEntityBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public LedgerEntryEntityBuilder transactionId(UUID transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public LedgerEntryEntityBuilder accountId(UUID accountId) {
            this.accountId = accountId;
            return this;
        }

        public LedgerEntryEntityBuilder entryType(String entryType) {
            this.entryType = entryType;
            return this;
        }

        public LedgerEntryEntityBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public LedgerEntryEntityBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public LedgerEntryEntityBuilder balanceAfter(BigDecimal balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }

        public LedgerEntryEntityBuilder referenceType(String referenceType) {
            this.referenceType = referenceType;
            return this;
        }

        public LedgerEntryEntityBuilder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public LedgerEntryEntityBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public LedgerEntryEntity build() {
            return new LedgerEntryEntity(id, transactionId, accountId, entryType, amount, currency, balanceAfter, referenceType, referenceId, createdAt);
        }

        public String toString() {
            return "LedgerEntryEntity.LedgerEntryEntityBuilder(id=" + this.id + ", transactionId=" + this.transactionId + ", accountId=" + this.accountId + ", entryType=" + this.entryType + ", amount=" + this.amount + ", currency=" + this.currency + ", balanceAfter=" + this.balanceAfter + ", referenceType=" + this.referenceType + ", referenceId=" + this.referenceId + ", createdAt=" + this.createdAt + ")";
        }
    }
}
