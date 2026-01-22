package id.payu.backoffice.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "fraud_cases", indexes = {
        @Index(name = "idx_fraud_user", columnList = "userId"),
        @Index(name = "idx_fraud_status", columnList = "status"),
        @Index(name = "idx_fraud_risk", columnList = "riskLevel"),
        @Index(name = "idx_fraud_transaction", columnList = "transactionId")
})
public class FraudCase extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    public String userId;

    @Column(length = 100)
    public String accountNumber;

    public UUID transactionId;

    @Column(length = 50)
    public String transactionType;

    @Column(precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(length = 100)
    public String fraudType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CaseStatus status;

    @Column(columnDefinition = "TEXT")
    public String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    public String evidence;

    @Column(columnDefinition = "TEXT")
    public String notes;

    public String assignedTo;

    public String resolvedBy;

    public LocalDateTime resolvedAt;

    @Column(updatable = false)
    public LocalDateTime createdAt;

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
