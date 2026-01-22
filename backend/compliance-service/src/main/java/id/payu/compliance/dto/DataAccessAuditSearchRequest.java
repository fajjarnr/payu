package id.payu.compliance.dto;

import id.payu.compliance.domain.model.DataAccessAudit.DataOperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataAccessAuditSearchRequest {

    private String userId;
    private String accessedBy;
    private String serviceName;
    private DataOperationType operationType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;
}