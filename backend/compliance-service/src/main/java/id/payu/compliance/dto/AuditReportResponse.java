package id.payu.compliance.dto;

import id.payu.compliance.domain.model.ComplianceCheck;
import id.payu.compliance.domain.model.ComplianceCheckResult;
import id.payu.compliance.domain.model.ComplianceStandard;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AuditReportResponse(
        UUID id,
        UUID transactionId,
        String merchantId,
        ComplianceStandard standard,
        List<ComplianceCheck> checks,
        ComplianceCheckResult overallStatus,
        LocalDateTime createdAt,
        String createdBy
) {}
