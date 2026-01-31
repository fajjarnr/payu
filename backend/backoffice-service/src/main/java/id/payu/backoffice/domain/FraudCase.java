package id.payu.backoffice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_cases", indexes = {
        @Index(name = "idx_fraud_user", columnList = "userId"),
        @Index(name = "idx_fraud_status", columnList = "status"),
        @Index(name = "idx_fraud_risk", columnList = "riskLevel"),
        @Index(name = "idx_fraud_transaction", columnList = "transactionId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(length = 100)
    private String accountNumber;

    private UUID transactionId;

    @Column(length = 50)
    private String transactionType;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String fraudType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String evidence;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String assignedTo;

    private String resolvedBy;

    private LocalDateTime resolvedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = CaseStatus.OPEN;
        }
        if (riskLevel == null) {
            riskLevel = RiskLevel.MEDIUM;
        }
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum CaseStatus {
        OPEN,
        UNDER_INVESTIGATION,
        RESOLVED,
        CLOSED,
        ESCALATED
    }
}
