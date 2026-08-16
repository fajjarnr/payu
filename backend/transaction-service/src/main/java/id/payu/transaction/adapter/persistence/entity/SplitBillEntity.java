package id.payu.transaction.adapter.persistence.entity;

import id.payu.transaction.domain.model.*;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "split_bills")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class SplitBillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_number", nullable = false, unique = true)
    private String referenceNumber;

    @Column(name = "creator_account_id", nullable = false)
    private UUID creatorAccountId;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false)
    private SplitType splitType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplitStatus status;

    @Column(name = "due_date")
    private Instant dueDate;

    // QAMVP-008: participants are managed explicitly via the persistence port
    // (saveParticipant/findParticipantsBySplitBillId). A managed unidirectional
    // @OneToMany + @JoinColumn here triggers a one-shot FK-null sync on save
    // ("delete all, re-insert") which violates the NOT NULL split_bill_id and
    // crashes every update (activate/addParticipant/makePayment).
    @Transient
    private List<SplitBillParticipantEntity> participants;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // BUG-TXN-SPLITBILL-001: @Version makes Spring Data's isNew() check this
    // field instead of @Id. With @GeneratedValue(UUID) + no @Version, the
    // JPA spec's "is the entity new" check was unreliable across
    // Spring Data 3.5 + Hibernate 6, causing StaleObjectStateException on
    // first save. Hibernate manages the value automatically.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // Default constructor
    public SplitBillEntity() {
    }

    // All-args constructor
    public SplitBillEntity(UUID id, String referenceNumber, UUID creatorAccountId, BigDecimal totalAmount,
                     String currency, String title, String description, SplitType splitType,
                     SplitStatus status, Instant dueDate, List<SplitBillParticipantEntity> participants,
                     Instant createdAt, Instant updatedAt, Instant completedAt, Long version) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.creatorAccountId = creatorAccountId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.title = title;
        this.description = description;
        this.splitType = splitType;
        this.status = status;
        this.dueDate = dueDate;
        this.participants = participants;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
        this.version = version;
    }

    // Builder
    public static SplitBillBuilder builder() {
        return new SplitBillBuilder();
    }

    public static class SplitBillBuilder {
        private UUID id;
        private String referenceNumber;
        private UUID creatorAccountId;
        private BigDecimal totalAmount;
        private String currency;
        private String title;
        private String description;
        private SplitType splitType;
        private SplitStatus status;
        private Instant dueDate;
        private List<SplitBillParticipantEntity> participants;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant completedAt;
        private Long version;

        public SplitBillBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public SplitBillBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }

        public SplitBillBuilder creatorAccountId(UUID creatorAccountId) {
            this.creatorAccountId = creatorAccountId;
            return this;
        }

        public SplitBillBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public SplitBillBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public SplitBillBuilder title(String title) {
            this.title = title;
            return this;
        }

        public SplitBillBuilder description(String description) {
            this.description = description;
            return this;
        }

        public SplitBillBuilder splitType(SplitType splitType) {
            this.splitType = splitType;
            return this;
        }

        public SplitBillBuilder status(SplitStatus status) {
            this.status = status;
            return this;
        }

        public SplitBillBuilder dueDate(Instant dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public SplitBillBuilder participants(List<SplitBillParticipantEntity> participants) {
            this.participants = participants;
            return this;
        }

        public SplitBillBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SplitBillBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public SplitBillBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public SplitBillBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public SplitBillEntity build() {
            return new SplitBillEntity(id, referenceNumber, creatorAccountId, totalAmount, currency,
                    title, description, splitType, status, dueDate, participants,
                    createdAt, updatedAt, completedAt, version);
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public UUID getCreatorAccountId() {
        return creatorAccountId;
    }

    public void setCreatorAccountId(UUID creatorAccountId) {
        this.creatorAccountId = creatorAccountId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public SplitStatus getStatus() {
        return status;
    }

    public void setStatus(SplitStatus status) {
        this.status = status;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    public List<SplitBillParticipantEntity> getParticipants() {
        return participants;
    }

    public void setParticipants(List<SplitBillParticipantEntity> participants) {
        this.participants = participants;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    // Business methods
    public boolean isFullyPaid() {
        if (participants == null || participants.isEmpty()) {
            return false;
        }
        // TXN-SPLIT-001: every participant paid their share AND the total
        // collected covers the bill total. Before this guard, a CUSTOM split
        // whose shares summed below totalAmount could be settled with money
        // still missing (all participants "fully paid" their own share).
        return participants.stream()
                .allMatch(p -> p.getAmountPaid().compareTo(p.getAmountOwed()) >= 0)
                && getTotalPaid().compareTo(totalAmount) >= 0;
    }

    public BigDecimal getTotalPaid() {
        if (participants == null || participants.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return participants.stream()
                .map(SplitBillParticipantEntity::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(getTotalPaid());
    }

    public boolean canBeCancelled() {
        return status == SplitStatus.DRAFT || status == SplitStatus.ACTIVE;
    }

    /**
     * BUG-BE-117: Separate modifiability check from cancellability.
     * Updates and adding participants are allowed in DRAFT/ACTIVE,
     * but cancellation may have stricter rules in the future.
     */
    public boolean canBeModified() {
        return status == SplitStatus.DRAFT || status == SplitStatus.ACTIVE;
    }

    public boolean canAddPayment() {
        return status == SplitStatus.ACTIVE || status == SplitStatus.IN_PROGRESS;
    }
}
