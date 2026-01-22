package id.payu.compliance.dto;

import id.payu.compliance.domain.model.DataAccessAudit.DataOperationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataAccessAuditResponse {

    private UUID id;
    private String userId;
    private String accessedBy;
    private String serviceName;
    private String resourceType;
    private String resourceId;
    private DataOperationType operationType;
    private String purpose;
    private String ipAddress;
    private String userAgent;
    private Boolean success;
    private String errorMessage;
    private LocalDateTime accessedAt;
    private LocalDateTime createdAt;
}