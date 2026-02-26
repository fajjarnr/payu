package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JournalEntry is the parent entity for double-entry bookkeeping.
 * Each journal must have paired DEBIT + CREDIT LedgerEntry rows
 * where sum(debit) == sum(credit).
 *
 * This enforces accounting integrity at the domain level.
 */
public class JournalEntry {

    private UUID id;
    private String journalNumber;
    private String description;
    private String referenceType;
    private String referenceId;
    private JournalStatus status;
    private LocalDateTime postedAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<LedgerEntry> entries;

    public JournalEntry() {
        this.entries = new ArrayList<>();
    }

    public JournalEntry(UUID id, String journalNumber, String description,
                        String referenceType, String referenceId,
                        JournalStatus status, LocalDateTime postedAt,
                        LocalDateTime createdAt, String createdBy,
                        List<LedgerEntry> entries) {
        this.id = id;
        this.journalNumber = journalNumber;
        this.description = description;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.status = status;
        this.postedAt = postedAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.entries = entries != null ? entries : new ArrayList<>();
    }

    public enum JournalStatus {
        PENDING,
        POSTED,
        REVERSED
    }

    /**
     * Validates that the journal is balanced: sum(debit) == sum(credit).
     */
    public boolean isBalanced() {
        BigDecimal totalDebit = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntry.EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntry.EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalDebit.compareTo(totalCredit) == 0;
    }

    /**
     * Gets the total debit amount of this journal.
     */
    public BigDecimal getTotalDebit() {
        return entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntry.EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Gets the total credit amount of this journal.
     */
    public BigDecimal getTotalCredit() {
        return entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntry.EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Validates that the journal has at least one DEBIT and one CREDIT entry.
     */
    public boolean hasMatchingPairs() {
        boolean hasDebit = entries.stream()
                .anyMatch(e -> e.getEntryType() == LedgerEntry.EntryType.DEBIT);
        boolean hasCredit = entries.stream()
                .anyMatch(e -> e.getEntryType() == LedgerEntry.EntryType.CREDIT);
        return hasDebit && hasCredit;
    }

    /**
     * Posts this journal entry, validating balance constraint.
     *
     * @throws IllegalStateException if journal is not balanced or has no matching pairs
     */
    public void post() {
        if (!hasMatchingPairs()) {
            throw new IllegalStateException("Journal must have at least one DEBIT and one CREDIT entry");
        }
        if (!isBalanced()) {
            throw new IllegalStateException(
                    "Journal is not balanced: total debit=" + getTotalDebit()
                            + ", total credit=" + getTotalCredit());
        }
        this.status = JournalStatus.POSTED;
        this.postedAt = LocalDateTime.now();
    }

    /**
     * Adds a ledger entry to this journal.
     */
    public void addEntry(LedgerEntry entry) {
        entry.setJournalEntryId(this.id);
        this.entries.add(entry);
    }

    public static JournalEntryBuilder builder() {
        return new JournalEntryBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getJournalNumber() { return journalNumber; }
    public void setJournalNumber(String journalNumber) { this.journalNumber = journalNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public JournalStatus getStatus() { return status; }
    public void setStatus(JournalStatus status) { this.status = status; }
    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public List<LedgerEntry> getEntries() { return entries; }
    public void setEntries(List<LedgerEntry> entries) { this.entries = entries; }

    public static class JournalEntryBuilder {
        private UUID id;
        private String journalNumber;
        private String description;
        private String referenceType;
        private String referenceId;
        private JournalStatus status;
        private LocalDateTime postedAt;
        private LocalDateTime createdAt;
        private String createdBy;
        private List<LedgerEntry> entries;

        JournalEntryBuilder() {}

        public JournalEntryBuilder id(UUID id) { this.id = id; return this; }
        public JournalEntryBuilder journalNumber(String journalNumber) { this.journalNumber = journalNumber; return this; }
        public JournalEntryBuilder description(String description) { this.description = description; return this; }
        public JournalEntryBuilder referenceType(String referenceType) { this.referenceType = referenceType; return this; }
        public JournalEntryBuilder referenceId(String referenceId) { this.referenceId = referenceId; return this; }
        public JournalEntryBuilder status(JournalStatus status) { this.status = status; return this; }
        public JournalEntryBuilder postedAt(LocalDateTime postedAt) { this.postedAt = postedAt; return this; }
        public JournalEntryBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public JournalEntryBuilder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public JournalEntryBuilder entries(List<LedgerEntry> entries) { this.entries = entries; return this; }

        public JournalEntry build() {
            return new JournalEntry(id, journalNumber, description, referenceType,
                    referenceId, status, postedAt, createdAt, createdBy, entries);
        }
    }
}
