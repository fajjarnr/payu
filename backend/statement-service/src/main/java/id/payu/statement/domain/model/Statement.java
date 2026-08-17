package id.payu.statement.domain.model;

import id.payu.statement.domain.entity.StatementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing a monthly e-statement.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Statement {

    private UUID id;
    private String customerId;
    private String accountNumber;
    private LocalDate statementPeriod;
    private String storagePath;
    private Long fileSizeBytes;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private Integer transactionCount;
    private StatementStatus status;
    private LocalDateTime generatedAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void markCompleted(String storagePath, Long fileSizeBytes) {
        this.storagePath = storagePath;
        this.fileSizeBytes = fileSizeBytes;
        this.status = StatementStatus.COMPLETED;
        this.generatedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = StatementStatus.FAILED;
    }

    public void recordAccess() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
