package id.payu.simulator.bifast.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a simulated BI-FAST transfer transaction.
 */
@Entity
@Table(name = "transfers")
public class Transfer extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 50)
    public String referenceNumber;

    @Column(name = "source_bank_code", nullable = false, length = 10)
    public String sourceBankCode;

    @Column(name = "source_account_number", nullable = false, length = 20)
    public String sourceAccountNumber;

    @Column(name = "source_account_name", length = 100)
    public String sourceAccountName;

    @Column(name = "destination_bank_code", nullable = false, length = 10)
    public String destinationBankCode;

    @Column(name = "destination_account_number", nullable = false, length = 20)
    public String destinationAccountNumber;

    @Column(name = "destination_account_name", length = 100)
    public String destinationAccountName;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(name = "currency", length = 3)
    public String currency = "IDR";

    @Column(name = "description", length = 255)
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public TransferStatus status = TransferStatus.PENDING;

    @Column(name = "failure_reason", length = 255)
    public String failureReason;

    @Column(name = "webhook_url", length = 500)
    public String webhookUrl;

    @Column(name = "webhook_sent")
    public boolean webhookSent = false;

    @Column(name = "webhook_sent_at")
    public LocalDateTime webhookSentAt;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @Column(name = "completed_at")
    public LocalDateTime completedAt;

    public enum TransferStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        TIMEOUT
    }

    // Finder methods
    public static Transfer findByReference(String referenceNumber) {
        return find("referenceNumber", referenceNumber).firstResult();
    }

    public static List<Transfer> findByStatus(TransferStatus status) {
        return list("status", status);
    }

    public static List<Transfer> findPendingWebhooks() {
        return list("webhookSent = false and status in (?1, ?2)", 
                    TransferStatus.COMPLETED, TransferStatus.FAILED);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method to complete transfer
    public void complete() {
        this.status = TransferStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    // Helper method to fail transfer
    public void fail(String reason) {
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
    }
}
