package id.payu.transaction.domain.model;

import id.payu.transaction.domain.model.Transaction.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTransfer {
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

    public enum ScheduleType {
        ONE_TIME,
        RECURRING_DAILY,
        RECURRING_WEEKLY,
        RECURRING_MONTHLY,
        RECURRING_CUSTOM
    }

    public enum ScheduledStatus {
        ACTIVE,
        PAUSED,
        COMPLETED,
        CANCELLED,
        FAILED
    }

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
