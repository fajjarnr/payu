package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerEntry {
    private UUID id;
    private UUID transactionId;
    private UUID journalEntryId;
    private String accountId;
    private String coaCode;
    private EntryType entryType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceAfter;
    private String referenceType;
    private String referenceId;
    private LocalDateTime createdAt;

    public LedgerEntry() {
    }

    public LedgerEntry(UUID id, UUID transactionId, UUID journalEntryId, String accountId,
                       String coaCode, EntryType entryType, BigDecimal amount, String currency,
                       BigDecimal balanceAfter, String referenceType, String referenceId,
                       LocalDateTime createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.journalEntryId = journalEntryId;
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

    public static LedgerEntryBuilder builder() {
        return new LedgerEntryBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getCoaCode() { return coaCode; }
    public void setCoaCode(String coaCode) { this.coaCode = coaCode; }
    public EntryType getEntryType() { return entryType; }
    public void setEntryType(EntryType entryType) { this.entryType = entryType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class LedgerEntryBuilder {
        private UUID id;
        private UUID transactionId;
        private UUID journalEntryId;
        private String accountId;
        private String coaCode;
        private EntryType entryType;
        private BigDecimal amount;
        private String currency;
        private BigDecimal balanceAfter;
        private String referenceType;
        private String referenceId;
        private LocalDateTime createdAt;

        LedgerEntryBuilder() {}

        public LedgerEntryBuilder id(UUID id) { this.id = id; return this; }
        public LedgerEntryBuilder transactionId(UUID transactionId) { this.transactionId = transactionId; return this; }
        public LedgerEntryBuilder journalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; return this; }
        public LedgerEntryBuilder accountId(String accountId) { this.accountId = accountId; return this; }
        public LedgerEntryBuilder coaCode(String coaCode) { this.coaCode = coaCode; return this; }
        public LedgerEntryBuilder entryType(EntryType entryType) { this.entryType = entryType; return this; }
        public LedgerEntryBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public LedgerEntryBuilder currency(String currency) { this.currency = currency; return this; }
        public LedgerEntryBuilder balanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; return this; }
        public LedgerEntryBuilder referenceType(String referenceType) { this.referenceType = referenceType; return this; }
        public LedgerEntryBuilder referenceId(String referenceId) { this.referenceId = referenceId; return this; }
        public LedgerEntryBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public LedgerEntry build() {
            return new LedgerEntry(id, transactionId, journalEntryId, accountId, coaCode,
                    entryType, amount, currency, balanceAfter, referenceType, referenceId, createdAt);
        }
    }
}
