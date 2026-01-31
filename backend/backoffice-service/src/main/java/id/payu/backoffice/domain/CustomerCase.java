package id.payu.backoffice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_cases", indexes = {
        @Index(name = "idx_case_user", columnList = "userId"),
        @Index(name = "idx_case_status", columnList = "status"),
        @Index(name = "idx_case_type", columnList = "caseType"),
        @Index(name = "idx_case_priority", columnList = "priority")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(length = 100)
    private String accountNumber;

    @Column(length = 50)
    private String caseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseType caseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

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
