package id.payu.transaction.adapter.persistence.entity;

import id.payu.transaction.domain.model.*;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import id.payu.transaction.domain.model.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_transfers")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class ScheduledTransferEntity {
    public ScheduledTransferEntity() {
    }

    public ScheduledTransferEntity(UUID id, String referenceNumber, UUID senderAccountId, String recipientAccountNumber, UUID recipientAccountId, TransactionType transferType, BigDecimal amount, String currency, String description, ScheduleType scheduleType, Instant startDate, Instant endDate, Instant nextExecutionDate, Integer frequencyDays, Integer dayOfMonth, Integer occurrenceCount, Integer executedCount, ScheduledStatus status, String failureReason, UUID lastTransactionId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.senderAccountId = senderAccountId;
        this.recipientAccountNumber = recipientAccountNumber;
        this.recipientAccountId = recipientAccountId;
        this.transferType = transferType;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.scheduleType = scheduleType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.nextExecutionDate = nextExecutionDate;
        this.frequencyDays = frequencyDays;
        this.dayOfMonth = dayOfMonth;
        this.occurrenceCount = occurrenceCount;
        this.executedCount = executedCount;
        this.status = status;
        this.failureReason = failureReason;
        this.lastTransactionId = lastTransactionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ScheduledTransferBuilder builder() {
        return new ScheduledTransferBuilder();
    }

    public static class ScheduledTransferBuilder {
        private UUID id;
        private String referenceNumber;
        private UUID senderAccountId;
        private String recipientAccountNumber;
        private UUID recipientAccountId;
        private TransactionType transferType;
        private BigDecimal amount;
        private String currency;
        private String description;
        private ScheduleType scheduleType;
        private Instant startDate;
        private Instant endDate;
        private Instant nextExecutionDate;
        private Integer frequencyDays;
        private Integer dayOfMonth;
        private Integer occurrenceCount;
        private Integer executedCount;
        private ScheduledStatus status;
        private String failureReason;
        private UUID lastTransactionId;
        private Instant createdAt;
        private Instant updatedAt;

        public ScheduledTransferBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        public ScheduledTransferBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        public ScheduledTransferBuilder senderAccountId(UUID senderAccountId) {
            this.senderAccountId = senderAccountId;
            return this;
        }
        public ScheduledTransferBuilder recipientAccountNumber(String recipientAccountNumber) {
            this.recipientAccountNumber = recipientAccountNumber;
            return this;
        }
        public ScheduledTransferBuilder recipientAccountId(UUID recipientAccountId) {
            this.recipientAccountId = recipientAccountId;
            return this;
        }
        public ScheduledTransferBuilder transferType(TransactionType transferType) {
            this.transferType = transferType;
            return this;
        }
        public ScheduledTransferBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public ScheduledTransferBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public ScheduledTransferBuilder description(String description) {
            this.description = description;
            return this;
        }
        public ScheduledTransferBuilder scheduleType(ScheduleType scheduleType) {
            this.scheduleType = scheduleType;
            return this;
        }
        public ScheduledTransferBuilder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }
        public ScheduledTransferBuilder endDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }
        public ScheduledTransferBuilder nextExecutionDate(Instant nextExecutionDate) {
            this.nextExecutionDate = nextExecutionDate;
            return this;
        }
        public ScheduledTransferBuilder frequencyDays(Integer frequencyDays) {
            this.frequencyDays = frequencyDays;
            return this;
        }
        public ScheduledTransferBuilder dayOfMonth(Integer dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
            return this;
        }
        public ScheduledTransferBuilder occurrenceCount(Integer occurrenceCount) {
            this.occurrenceCount = occurrenceCount;
            return this;
        }
        public ScheduledTransferBuilder executedCount(Integer executedCount) {
            this.executedCount = executedCount;
            return this;
        }
        public ScheduledTransferBuilder status(ScheduledStatus status) {
            this.status = status;
            return this;
        }
        public ScheduledTransferBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }
        public ScheduledTransferBuilder lastTransactionId(UUID lastTransactionId) {
            this.lastTransactionId = lastTransactionId;
            return this;
        }
        public ScheduledTransferBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public ScheduledTransferBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ScheduledTransferEntity build() {
            return new ScheduledTransferEntity(id, referenceNumber, senderAccountId, recipientAccountNumber, recipientAccountId, transferType, amount, currency, description, scheduleType, startDate, endDate, nextExecutionDate, frequencyDays, dayOfMonth, occurrenceCount, executedCount, status, failureReason, lastTransactionId, createdAt, updatedAt);
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

    public String getRecipientAccountNumber() {
        return recipientAccountNumber;
    }

    public void setRecipientAccountNumber(String recipientAccountNumber) {
        this.recipientAccountNumber = recipientAccountNumber;
    }

    public UUID getRecipientAccountId() {
        return recipientAccountId;
    }

    public void setRecipientAccountId(UUID recipientAccountId) {
        this.recipientAccountId = recipientAccountId;
    }

    public TransactionType getTransferType() {
        return transferType;
    }

    public void setTransferType(TransactionType transferType) {
        this.transferType = transferType;
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

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public Instant getNextExecutionDate() {
        return nextExecutionDate;
    }

    public void setNextExecutionDate(Instant nextExecutionDate) {
        this.nextExecutionDate = nextExecutionDate;
    }

    public Integer getFrequencyDays() {
        return frequencyDays;
    }

    public void setFrequencyDays(Integer frequencyDays) {
        this.frequencyDays = frequencyDays;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public Integer getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(Integer occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public Integer getExecutedCount() {
        return executedCount;
    }

    public void setExecutedCount(Integer executedCount) {
        this.executedCount = executedCount;
    }

    public ScheduledStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduledStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public UUID getLastTransactionId() {
        return lastTransactionId;
    }

    public void setLastTransactionId(UUID lastTransactionId) {
        this.lastTransactionId = lastTransactionId;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }


    // Application-assigned UUID (no @GeneratedValue). Per the READY-063 fix
    // pattern: with manual id + @GeneratedValue, Spring Data JPA's isNew()
    // detection treats the entity as "previously persisted" and calls merge()
    // instead of persist(), which fails with StaleObject for new rows.
    @Id
    private UUID id;

    @Column(name = "reference_number", nullable = false, unique = true)
    private String referenceNumber;

    @Column(name = "sender_account_id", nullable = false)
    private UUID senderAccountId;

    @Column(name = "recipient_account_number", nullable = false)
    private String recipientAccountNumber;

    @Column(name = "recipient_account_id")
    private UUID recipientAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false)
    private TransactionType transferType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "next_execution_date")
    private Instant nextExecutionDate;

    @Column(name = "frequency_days")
    private Integer frequencyDays;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    @Column(name = "executed_count")
    private Integer executedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduledStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "last_transaction_id")
    private UUID lastTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    public boolean isDueForExecution() {
        return status == ScheduledStatus.ACTIVE
                && nextExecutionDate != null
                && !nextExecutionDate.isAfter(Instant.now());
    }

    public boolean isCompleted() {
        if (status != ScheduledStatus.ACTIVE) {
            return false;
        }
        if (occurrenceCount != null && executedCount >= occurrenceCount) {
            return true;
        }
        if (endDate != null && nextExecutionDate.isAfter(endDate)) {
            return true;
        }
        return false;
    }
}
