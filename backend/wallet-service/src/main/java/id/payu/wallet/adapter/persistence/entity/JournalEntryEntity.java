package id.payu.wallet.adapter.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity for JournalEntry — parent of double-entry ledger pairs.
 * Enforces sum(debit) == sum(credit) at the application layer.
 */
@Entity
@Table(name = "journal_entries", indexes = {
    @Index(name = "idx_journal_reference", columnList = "reference_type, reference_id"),
    @Index(name = "idx_journal_posted_at", columnList = "posted_at"),
    @Index(name = "idx_journal_status", columnList = "status")
})
public class JournalEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "journal_number", nullable = false, unique = true, length = 30)
    private String journalNumber;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JournalStatus status;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<LedgerEntryEntity> entries = new ArrayList<>();

    public JournalEntryEntity() {
    }

    public JournalEntryEntity(UUID id, String journalNumber, String description,
                              String referenceType, String referenceId,
                              JournalStatus status, LocalDateTime postedAt,
                              LocalDateTime createdAt, String createdBy) {
        this.id = id;
        this.journalNumber = journalNumber;
        this.description = description;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.status = status;
        this.postedAt = postedAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public static JournalEntryEntityBuilder builder() {
        return new JournalEntryEntityBuilder();
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
    public List<LedgerEntryEntity> getEntries() { return entries; }
    public void setEntries(List<LedgerEntryEntity> entries) { this.entries = entries; }

    public static class JournalEntryEntityBuilder {
        private UUID id;
        private String journalNumber;
        private String description;
        private String referenceType;
        private String referenceId;
        private JournalStatus status;
        private LocalDateTime postedAt;
        private LocalDateTime createdAt;
        private String createdBy;

        JournalEntryEntityBuilder() {}

        public JournalEntryEntityBuilder id(UUID id) { this.id = id; return this; }
        public JournalEntryEntityBuilder journalNumber(String journalNumber) { this.journalNumber = journalNumber; return this; }
        public JournalEntryEntityBuilder description(String description) { this.description = description; return this; }
        public JournalEntryEntityBuilder referenceType(String referenceType) { this.referenceType = referenceType; return this; }
        public JournalEntryEntityBuilder referenceId(String referenceId) { this.referenceId = referenceId; return this; }
        public JournalEntryEntityBuilder status(JournalStatus status) { this.status = status; return this; }
        public JournalEntryEntityBuilder postedAt(LocalDateTime postedAt) { this.postedAt = postedAt; return this; }
        public JournalEntryEntityBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public JournalEntryEntityBuilder createdBy(String createdBy) { this.createdBy = createdBy; return this; }

        public JournalEntryEntity build() {
            return new JournalEntryEntity(id, journalNumber, description, referenceType,
                    referenceId, status, postedAt, createdAt, createdBy);
        }
    }
}
