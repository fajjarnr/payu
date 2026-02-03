package id.payu.transaction.domain.model;

import id.payu.transaction.domain.model.Transaction.TransactionType;
import jakarta.persistence.*;
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
@Entity
@Table(name = "scheduled_transfers")
public class ScheduledTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
