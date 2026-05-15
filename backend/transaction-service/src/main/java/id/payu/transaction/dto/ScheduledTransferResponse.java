package id.payu.transaction.dto;

import id.payu.transaction.adapter.persistence.entity.ScheduledTransferEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ScheduledTransferResponse {
    public ScheduledTransferResponse() {
    }

    public ScheduledTransferResponse(UUID id, String referenceNumber, UUID senderAccountId, String recipientAccountNumber, UUID recipientAccountId, String transferType, BigDecimal amount, String currency, String description, String scheduleType, Instant startDate, Instant endDate, Instant nextExecutionDate, Integer frequencyDays, Integer dayOfMonth, Integer occurrenceCount, Integer executedCount, String status, String failureReason, UUID lastTransactionId, Instant createdAt, Instant updatedAt) {
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

    public static ScheduledTransferResponseBuilder builder() {
        return new ScheduledTransferResponseBuilder();
    }

    public static class ScheduledTransferResponseBuilder {
        private UUID id;
        private String referenceNumber;
        private UUID senderAccountId;
        private String recipientAccountNumber;
        private UUID recipientAccountId;
        private String transferType;
        private BigDecimal amount;
        private String currency;
        private String description;
        private String scheduleType;
        private Instant startDate;
        private Instant endDate;
        private Instant nextExecutionDate;
        private Integer frequencyDays;
        private Integer dayOfMonth;
        private Integer occurrenceCount;
        private Integer executedCount;
        private String status;
        private String failureReason;
        private UUID lastTransactionId;
        private Instant createdAt;
        private Instant updatedAt;

        public ScheduledTransferResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        public ScheduledTransferResponseBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        public ScheduledTransferResponseBuilder senderAccountId(UUID senderAccountId) {
            this.senderAccountId = senderAccountId;
            return this;
        }
        public ScheduledTransferResponseBuilder recipientAccountNumber(String recipientAccountNumber) {
            this.recipientAccountNumber = recipientAccountNumber;
            return this;
        }
        public ScheduledTransferResponseBuilder recipientAccountId(UUID recipientAccountId) {
            this.recipientAccountId = recipientAccountId;
            return this;
        }
        public ScheduledTransferResponseBuilder transferType(String transferType) {
            this.transferType = transferType;
            return this;
        }
        public ScheduledTransferResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public ScheduledTransferResponseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public ScheduledTransferResponseBuilder description(String description) {
            this.description = description;
            return this;
        }
        public ScheduledTransferResponseBuilder scheduleType(String scheduleType) {
            this.scheduleType = scheduleType;
            return this;
        }
        public ScheduledTransferResponseBuilder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }
        public ScheduledTransferResponseBuilder endDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }
        public ScheduledTransferResponseBuilder nextExecutionDate(Instant nextExecutionDate) {
            this.nextExecutionDate = nextExecutionDate;
            return this;
        }
        public ScheduledTransferResponseBuilder frequencyDays(Integer frequencyDays) {
            this.frequencyDays = frequencyDays;
            return this;
        }
        public ScheduledTransferResponseBuilder dayOfMonth(Integer dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
            return this;
        }
        public ScheduledTransferResponseBuilder occurrenceCount(Integer occurrenceCount) {
            this.occurrenceCount = occurrenceCount;
            return this;
        }
        public ScheduledTransferResponseBuilder executedCount(Integer executedCount) {
            this.executedCount = executedCount;
            return this;
        }
        public ScheduledTransferResponseBuilder status(String status) {
            this.status = status;
            return this;
        }
        public ScheduledTransferResponseBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }
        public ScheduledTransferResponseBuilder lastTransactionId(UUID lastTransactionId) {
            this.lastTransactionId = lastTransactionId;
            return this;
        }
        public ScheduledTransferResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public ScheduledTransferResponseBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ScheduledTransferResponse build() {
            return new ScheduledTransferResponse(id, referenceNumber, senderAccountId, recipientAccountNumber, recipientAccountId, transferType, amount, currency, description, scheduleType, startDate, endDate, nextExecutionDate, frequencyDays, dayOfMonth, occurrenceCount, executedCount, status, failureReason, lastTransactionId, createdAt, updatedAt);
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

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
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

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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


    private UUID id;
    private String referenceNumber;
    private UUID senderAccountId;
    private String recipientAccountNumber;
    private UUID recipientAccountId;
    private String transferType;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String scheduleType;
    private Instant startDate;
    private Instant endDate;
    private Instant nextExecutionDate;
    private Integer frequencyDays;
    private Integer dayOfMonth;
    private Integer occurrenceCount;
    private Integer executedCount;
    private String status;
    private String failureReason;
    private UUID lastTransactionId;
    private Instant createdAt;
    private Instant updatedAt;
}
