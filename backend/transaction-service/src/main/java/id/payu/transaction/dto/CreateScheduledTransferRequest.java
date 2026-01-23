package id.payu.transaction.dto;

import id.payu.transaction.domain.model.ScheduledTransfer;
import id.payu.transaction.domain.model.Transaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateScheduledTransferRequest {
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
    private Transaction.TransactionType transferType;

    @NotNull(message = "Schedule type is required")
    private ScheduledTransfer.ScheduleType scheduleType;

    @NotNull(message = "Start date is required")
    private Instant startDate;

    private Instant endDate;

    private Integer frequencyDays;

    private Integer dayOfMonth;

    private Integer occurrenceCount;
}
