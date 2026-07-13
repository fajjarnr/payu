package id.payu.compliance.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class DataAccessAudit {
    UUID id;
    String userId;
    String accessedBy;
    String serviceName;
    String resourceType;
    String resourceId;
    DataOperationType operationType;
    String purpose;
    String ipAddress;
    String userAgent;
    boolean success;
    String errorMessage;
    LocalDateTime accessedAt;
    LocalDateTime createdAt;
    Long version;
}
