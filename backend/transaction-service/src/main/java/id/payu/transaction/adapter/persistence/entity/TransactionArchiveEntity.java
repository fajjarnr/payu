package id.payu.transaction.adapter.persistence.entity;

import id.payu.transaction.domain.model.*;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_archives", indexes = {
        @Index(name = "idx_archive_account_id", columnList = "sender_account_id"),
        @Index(name = "idx_archive_batch_id", columnList = "archived_batch_id"),
        @Index(name = "idx_archive_created_at", columnList = "created_at")
})
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class TransactionArchiveEntity {
    public TransactionArchiveEntity() {
    }

    public TransactionArchiveEntity(UUID id, String referenceNumber, UUID senderAccountId, UUID recipientAccountId, TransactionArchiveType type, BigDecimal amount, String currency, String description, TransactionArchiveStatus status, String failureReason, String metadata, Instant createdAt, Instant updatedAt, Instant completedAt, Instant archivedAt, String archivalReason, Long archivedBatchId) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.senderAccountId = senderAccountId;
        this.recipientAccountId = recipientAccountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = status;
        this.failureReason = failureReason;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
        this.archivedAt = archivedAt;
        this.archivalReason = archivalReason;
        this.archivedBatchId = archivedBatchId;
    }

    public static TransactionArchiveBuilder builder() {
        return new TransactionArchiveBuilder();
    }

    public static class TransactionArchiveBuilder {
        private UUID id;
        private String referenceNumber;
        private UUID senderAccountId;
        private UUID recipientAccountId;
        private TransactionArchiveType type;
        private BigDecimal amount;
        private String currency;
        private String description;
        private TransactionArchiveStatus status;
        private String failureReason;
        private String metadata;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant completedAt;
        private Instant archivedAt;
        private String archivalReason;
        private Long archivedBatchId;

        public TransactionArchiveBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        public TransactionArchiveBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        public TransactionArchiveBuilder senderAccountId(UUID senderAccountId) {
            this.senderAccountId = senderAccountId;
            return this;
        }
        public TransactionArchiveBuilder recipientAccountId(UUID recipientAccountId) {
            this.recipientAccountId = recipientAccountId;
            return this;
        }
        public TransactionArchiveBuilder type(TransactionArchiveType type) {
            this.type = type;
            return this;
        }
        public TransactionArchiveBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public TransactionArchiveBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public TransactionArchiveBuilder description(String description) {
            this.description = description;
            return this;
        }
        public TransactionArchiveBuilder status(TransactionArchiveStatus status) {
            this.status = status;
            return this;
        }
        public TransactionArchiveBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }
        public TransactionArchiveBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }
        public TransactionArchiveBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public TransactionArchiveBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        public TransactionArchiveBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }
        public TransactionArchiveBuilder archivedAt(Instant archivedAt) {
            this.archivedAt = archivedAt;
            return this;
        }
        public TransactionArchiveBuilder archivalReason(String archivalReason) {
            this.archivalReason = archivalReason;
            return this;
        }
        public TransactionArchiveBuilder archivedBatchId(Long archivedBatchId) {
            this.archivedBatchId = archivedBatchId;
            return this;
        }

        public TransactionArchiveEntity build() {
            return new TransactionArchiveEntity(id, referenceNumber, senderAccountId, recipientAccountId, type, amount, currency, description, status, failureReason, metadata, createdAt, updatedAt, completedAt, archivedAt, archivalReason, archivedBatchId);
        }
    }

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

    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    public void setSenderAccountId(UUID senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public UUID getRecipientAccountId() {
        return recipientAccountId;
    }

    public void setRecipientAccountId(UUID recipientAccountId) {
        this.recipientAccountId = recipientAccountId;
    }

    public TransactionArchiveType getType() {
        return type;
    }

    public void setType(TransactionArchiveType type) {
        this.type = type;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionArchiveStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionArchiveStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public String getArchivalReason() {
        return archivalReason;
    }

    public void setArchivalReason(String archivalReason) {
        this.archivalReason = archivalReason;
    }

    public Long getArchivedBatchId() {
        return archivedBatchId;
    }

    public void setArchivedBatchId(Long archivedBatchId) {
        this.archivedBatchId = archivedBatchId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }



    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_number", nullable = false, length = 50)
    private String referenceNumber;

    @Column(name = "sender_account_id", nullable = false)
    private UUID senderAccountId;

    @Column(name = "recipient_account_id", nullable = false)
    private UUID recipientAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionArchiveType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionArchiveStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "archived_at", nullable = false)
    private Instant archivedAt;

    @Column(name = "archival_reason", length = 50)
    private String archivalReason;

    @Column(name = "archived_batch_id")
    private Long archivedBatchId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
}
