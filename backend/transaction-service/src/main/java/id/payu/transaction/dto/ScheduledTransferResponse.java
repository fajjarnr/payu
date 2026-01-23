package id.payu.transaction.dto;

import id.payu.transaction.domain.model.ScheduledTransfer;
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
public class ScheduledTransferResponse {
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
