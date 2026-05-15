package id.payu.transaction.dto;

import id.payu.transaction.adapter.persistence.entity.ScheduledTransferEntity;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import id.payu.transaction.domain.model.ScheduleType;
import id.payu.transaction.domain.model.TransactionType;

public class CreateScheduledTransferRequest {
    public CreateScheduledTransferRequest() {
    }

    public CreateScheduledTransferRequest(UUID senderAccountId, String recipientAccountNumber, BigDecimal amount, String currency, String description, Instant startDate, Instant endDate, Integer frequencyDays, Integer dayOfMonth, Integer occurrenceCount) {
        this.senderAccountId = senderAccountId;
        this.recipientAccountNumber = recipientAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.frequencyDays = frequencyDays;
        this.dayOfMonth = dayOfMonth;
        this.occurrenceCount = occurrenceCount;
    }

    public static CreateScheduledTransferRequestBuilder builder() {
        return new CreateScheduledTransferRequestBuilder();
    }

    public static class CreateScheduledTransferRequestBuilder {
        private UUID senderAccountId;
        private String recipientAccountNumber;
        private BigDecimal amount;
        private String currency;
        private String description;
        private TransactionType transferType;
        private ScheduleType scheduleType;
        private Instant startDate;
        private Instant endDate;
        private Integer frequencyDays;
        private Integer dayOfMonth;
        private Integer occurrenceCount;

        public CreateScheduledTransferRequestBuilder senderAccountId(UUID senderAccountId) {
            this.senderAccountId = senderAccountId;
            return this;
        }
        public CreateScheduledTransferRequestBuilder recipientAccountNumber(String recipientAccountNumber) {
            this.recipientAccountNumber = recipientAccountNumber;
            return this;
        }
        public CreateScheduledTransferRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public CreateScheduledTransferRequestBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public CreateScheduledTransferRequestBuilder description(String description) {
            this.description = description;
            return this;
        }
        public CreateScheduledTransferRequestBuilder transferType(TransactionType transferType) {
            this.transferType = transferType;
            return this;
        }
        public CreateScheduledTransferRequestBuilder scheduleType(ScheduleType scheduleType) {
            this.scheduleType = scheduleType;
            return this;
        }
        public CreateScheduledTransferRequestBuilder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }
        public CreateScheduledTransferRequestBuilder endDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }
        public CreateScheduledTransferRequestBuilder frequencyDays(Integer frequencyDays) {
            this.frequencyDays = frequencyDays;
            return this;
        }
        public CreateScheduledTransferRequestBuilder dayOfMonth(Integer dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
            return this;
        }
        public CreateScheduledTransferRequestBuilder occurrenceCount(Integer occurrenceCount) {
            this.occurrenceCount = occurrenceCount;
            return this;
        }

        public CreateScheduledTransferRequest build() {
            CreateScheduledTransferRequest req = new CreateScheduledTransferRequest(senderAccountId, recipientAccountNumber, amount, currency, description, startDate, endDate, frequencyDays, dayOfMonth, occurrenceCount);
            req.setTransferType(transferType);
            req.setScheduleType(scheduleType);
            return req;
        }
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

    public TransactionType getTransferType() {
        return transferType;
    }

    public void setTransferType(TransactionType transferType) {
        this.transferType = transferType;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    @NotNull(message = "Sender account ID is required")
    private UUID senderAccountId;

    @NotBlank(message = "Recipient account number is required")
    private String recipientAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Transfer type is required")
    private TransactionType transferType;

    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType;

    @NotNull(message = "Start date is required")
    private Instant startDate;

    private Instant endDate;

    private Integer frequencyDays;

    private Integer dayOfMonth;

    private Integer occurrenceCount;
}
