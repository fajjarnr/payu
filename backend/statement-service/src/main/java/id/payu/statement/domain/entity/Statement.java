package id.payu.statement.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Statement entity representing monthly e-statement metadata
 */
@Entity
@Table(name = "statements", indexes = {
    @Index(name = "idx_statements_user_id", columnList = "user_id"),
    @Index(name = "idx_statements_period", columnList = "statement_period"),
    @Index(name = "idx_statements_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "statement_period", nullable = false)
    private LocalDate statementPeriod; // First day of the month

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath; // S3 or local storage path

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal closingBalance;

    @Column(name = "total_credits", precision = 19, scale = 4)
    private BigDecimal totalCredits;

    @Column(name = "total_debits", precision = 19, scale = 4)
    private BigDecimal totalDebits;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatementStatus status = StatementStatus.GENERATING;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "access_count")
    @Builder.Default
    private Integer accessCount = 0;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum StatementStatus {
        GENERATING,   // PDF is being generated
        COMPLETED,    // PDF ready for download
        FAILED,       // Generation failed
        ARCHIVED      // Old statement, may need retrieval from archive
    }

    /**
     * Increment access count and update last accessed timestamp
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Mark statement as completed with file details
     */
    public void markCompleted(String storagePath, Long fileSize) {
        this.storagePath = storagePath;
        this.fileSizeBytes = fileSize;
        this.status = StatementStatus.COMPLETED;
        this.generatedAt = LocalDateTime.now();
    }

    /**
     * Mark statement as failed
     */
    public void markFailed() {
        this.status = StatementStatus.FAILED;
    }
}
