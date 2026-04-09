package id.payu.wallet.adapter.persistence.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_account_id", columnList = "account_id"),
    @Index(name = "idx_ledger_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_ledger_created_at", columnList = "account_id, created_at"),
    @Index(name = "idx_ledger_journal_id", columnList = "journal_entry_id"),
    @Index(name = "idx_ledger_coa_code", columnList = "coa_code")
})
public class LedgerEntryEntity implements Persistable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryEntity journalEntry;

    @Column(name = "account_id", nullable = false, updatable = false)
    private String accountId;

    @Column(name = "coa_code", length = 20)
    private String coaCode;

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

    public LedgerEntryEntity(UUID id, UUID transactionId, String accountId, String coaCode,
                             String entryType, BigDecimal amount, String currency,
                             BigDecimal balanceAfter, String referenceType, String referenceId,
                             LocalDateTime createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.coaCode = coaCode;
        this.entryType = entryType;
        this.amount = amount;
        this.currency = currency;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
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

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCoaCode() {
        return coaCode;
    }

    public void setCoaCode(String coaCode) {
        this.coaCode = coaCode;
    }

    public JournalEntryEntity getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntryEntity journalEntry) {
        this.journalEntry = journalEntry;
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
        private JournalEntryEntity journalEntry;
        private String accountId;
        private String coaCode;
        private String entryType;
        private BigDecimal amount;
        private String currency;
        private BigDecimal balanceAfter;
        private String referenceType;
        private String referenceId;
        private LocalDateTime createdAt;

        LedgerEntryEntityBuilder() {
        }

        public LedgerEntryEntityBuilder journalEntry(JournalEntryEntity journalEntry) {
            this.journalEntry = journalEntry;
            return this;
        }

        public LedgerEntryEntityBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public LedgerEntryEntityBuilder transactionId(UUID transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public LedgerEntryEntityBuilder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public LedgerEntryEntityBuilder coaCode(String coaCode) {
            this.coaCode = coaCode;
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
            LedgerEntryEntity entity = new LedgerEntryEntity(id, transactionId, accountId, coaCode, entryType, amount, currency, balanceAfter, referenceType, referenceId, createdAt);
            entity.setJournalEntry(journalEntry);
            return entity;
        }

        public String toString() {
            return "LedgerEntryEntity.LedgerEntryEntityBuilder(id=" + this.id + ", transactionId=" + this.transactionId + ", journalEntry=" + this.journalEntry + ", accountId=" + this.accountId + ", coaCode=" + this.coaCode + ", entryType=" + this.entryType + ", amount=" + this.amount + ", currency=" + this.currency + ", balanceAfter=" + this.balanceAfter + ", referenceType=" + this.referenceType + ", referenceId=" + this.referenceId + ", createdAt=" + this.createdAt + ")";
        }
    }
}
