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

    // Manual accessors for stability
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    public CaseType getCaseType() { return caseType; }
    public void setCaseType(CaseType caseType) { this.caseType = caseType; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }


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
