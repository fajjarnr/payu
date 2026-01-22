package id.payu.backoffice.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_cases", indexes = {
        @Index(name = "idx_case_user", columnList = "userId"),
        @Index(name = "idx_case_status", columnList = "status"),
        @Index(name = "idx_case_type", columnList = "caseType"),
        @Index(name = "idx_case_priority", columnList = "priority")
})
public class CustomerCase extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    public String userId;

    @Column(length = 100)
    public String accountNumber;

    @Column(length = 50)
    public String caseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CaseType caseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Priority priority;

    @Column(nullable = false, length = 200)
    public String subject;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CaseStatus status;

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
        if (priority == null) {
            priority = Priority.MEDIUM;
        }
        if (caseNumber == null || caseNumber.isEmpty()) {
            caseNumber = "CASE-" + System.currentTimeMillis();
        }
    }

    public enum CaseType {
        GENERAL_INQUIRY,
        TRANSACTION_DISPUTE,
        ACCOUNT_ISSUE,
        TECHNICAL_ISSUE,
        BILLING_ISSUE,
        OTHER
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    public enum CaseStatus {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        CLOSED,
        ESCALATED
    }
}
