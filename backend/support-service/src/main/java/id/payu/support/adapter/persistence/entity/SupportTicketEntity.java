package id.payu.support.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
public class SupportTicketEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId = "payu";

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "priority", nullable = false)
    private String priority = "MEDIUM";

    @Column(name = "status", nullable = false)
    private String status = "OPEN";

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        createdAt = now; updatedAt = now;
    }
    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public SupportTicketEntity() {}

    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public String getTenantId() { return tenantId; } public void setTenantId(String t) { this.tenantId = t; }
    public String getUserId() { return userId; } public void setUserId(String u) { this.userId = u; }
    public String getSubject() { return subject; } public void setSubject(String s) { this.subject = s; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
    public String getCategory() { return category; } public void setCategory(String c) { this.category = c; }
    public String getPriority() { return priority; } public void setPriority(String p) { this.priority = p; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public String getAssignedTo() { return assignedTo; } public void setAssignedTo(String a) { this.assignedTo = a; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; } public void setResolvedAt(Instant r) { this.resolvedAt = r; }
}
