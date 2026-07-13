package id.payu.compliance.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class AuditReport {
    UUID id;
    UUID transactionId;
    String merchantId;
    ComplianceStandard standard;
    @Builder.Default
    List<ComplianceCheck> checks = List.of();
    ComplianceCheckResult overallStatus;
    LocalDateTime createdAt;
    String createdBy;
    Long version;
}
