package id.payu.compliance.dto;

import id.payu.compliance.domain.model.ComplianceCheck;
import id.payu.compliance.domain.model.ComplianceCheckResult;
import id.payu.compliance.domain.model.ComplianceStandard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AuditReportRequest(
        @NotNull UUID transactionId,
        @NotBlank String merchantId,
        @NotNull ComplianceStandard standard,
        @NotEmpty @Valid List<ComplianceCheck> checks
) {}
