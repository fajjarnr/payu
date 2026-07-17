package id.payu.backoffice.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class CustomerCase {
    private final UUID id;
    private final String userId;
    private final String accountNumber;
    private final String caseNumber;
    private final CaseType caseType;
    private final Priority priority;
    private final String subject;
    private final String description;
    private CustomerCaseStatus status;
    private String notes;
    private String assignedTo;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private final LocalDateTime createdAt;
    private final Long version;

    private CustomerCase(UUID id, String userId, String accountNumber, String caseNumber,
            CaseType caseType, Priority priority, String subject, String description,
            CustomerCaseStatus status, String notes, String assignedTo, String resolvedBy,
            LocalDateTime resolvedAt, LocalDateTime createdAt, Long version) {
        this.id = id;
        this.userId = requireText(userId, "userId");
        this.accountNumber = accountNumber;
        this.caseNumber = requireText(caseNumber, "caseNumber");
        this.caseType = Objects.requireNonNull(caseType, "caseType");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.subject = requireText(subject, "subject");
        this.description = description;
        this.status = Objects.requireNonNull(status, "status");
        this.notes = notes;
        this.assignedTo = assignedTo;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.version = version;
    }

    public static CustomerCase create(String userId, String accountNumber, CaseType caseType,
            Priority priority, String subject, String description, String notes) {
        return new CustomerCase(null, userId, accountNumber, "CASE-" + UUID.randomUUID(),
                caseType, priority == null ? Priority.MEDIUM : priority, subject, description,
                CustomerCaseStatus.OPEN, notes, null, null, null, LocalDateTime.now(), null);
    }

    public static CustomerCase reconstitute(UUID id, String userId, String accountNumber,
            String caseNumber, CaseType caseType, Priority priority, String subject,
            String description, CustomerCaseStatus status, String notes, String assignedTo,
            String resolvedBy, LocalDateTime resolvedAt, LocalDateTime createdAt, Long version) {
        return new CustomerCase(id, userId, accountNumber, caseNumber, caseType, priority,
                subject, description, status, notes, assignedTo, resolvedBy, resolvedAt,
                createdAt, version);
    }

    public void assignTo(String agent) {
        assignedTo = requireText(agent, "assignedTo");
        if (status == CustomerCaseStatus.OPEN) status = CustomerCaseStatus.IN_PROGRESS;
    }

    public void update(CustomerCaseStatus newStatus, String newNotes, String actor) {
        status = Objects.requireNonNull(newStatus, "status");
        notes = newNotes;
        if (newStatus == CustomerCaseStatus.RESOLVED || newStatus == CustomerCaseStatus.CLOSED) {
            resolvedBy = requireText(actor, "resolvedBy");
            resolvedAt = LocalDateTime.now();
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getAccountNumber() { return accountNumber; }
    public String getCaseNumber() { return caseNumber; }
    public CaseType getCaseType() { return caseType; }
    public Priority getPriority() { return priority; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public CustomerCaseStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getAssignedTo() { return assignedTo; }
    public String getResolvedBy() { return resolvedBy; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
}
