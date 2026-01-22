package id.payu.compliance.dto;

import id.payu.compliance.domain.model.DataAccessAudit.DataOperationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataAccessAuditRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Accessed by is required")
    private String accessedBy;

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @NotBlank(message = "Resource type is required")
    private String resourceType;

    private String resourceId;

    @NotNull(message = "Operation type is required")
    private DataOperationType operationType;

    private String purpose;
    private String ipAddress;
    private String userAgent;
    private Boolean success;
    private String errorMessage;

    private LocalDateTime accessedAt;
}