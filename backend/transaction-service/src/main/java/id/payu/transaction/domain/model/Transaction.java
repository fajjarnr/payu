package id.payu.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private UUID id;
    private String referenceNumber;
    private UUID senderAccountId;
    private UUID recipientAccountId;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private String description;
    private TransactionStatus status;
    private String failureReason;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private String idempotencyKey;

    public enum TransactionType {
        INTERNAL_TRANSFER,
        BIFAST_TRANSFER,
        SKN_TRANSFER,
        RTGS_TRANSFER,
        QRIS_PAYMENT,
        BILL_PAYMENT,
        TOP_UP
    }

    public enum TransactionStatus {
        PENDING,
        VALIDATING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
