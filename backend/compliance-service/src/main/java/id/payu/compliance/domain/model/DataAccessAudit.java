package id.payu.compliance.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "data_access_audits", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_accessed_by", columnList = "accessedBy"),
    @Index(name = "idx_accessed_at", columnList = "accessedAt"),
    @Index(name = "idx_service_name", columnList = "serviceName")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataAccessAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "accessed_by", nullable = false)
    private String accessedBy;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "operation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DataOperationType operationType;

    @Column(name = "purpose", length = 500)
    private String purpose;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (accessedAt == null) {
            accessedAt = LocalDateTime.now();
        }
    }

    public enum DataOperationType {
        READ,
        UPDATE,
        DELETE,
        EXPORT,
        SEARCH
    }
}