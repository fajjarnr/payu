package id.payu.transaction.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_archives", indexes = {
        @Index(name = "idx_archive_account_id", columnList = "sender_account_id"),
        @Index(name = "idx_archive_batch_id", columnList = "archived_batch_id"),
        @Index(name = "idx_archive_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionArchive {

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
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

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

    public enum TransactionType {
        TRANSFER,
        INTERNAL_TRANSFER,
        BI_FAST,
        SKN,
        RTGS,
        QRIS,
        SPLIT_BILL,
        BILL_PAYMENT
    }

    public enum TransactionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        TIMEOUT
    }
}
