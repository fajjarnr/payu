package id.payu.compliance.domain.port.in;

import id.payu.compliance.domain.model.AuditReport;
import id.payu.compliance.domain.model.ComplianceCheck;
import id.payu.compliance.domain.model.ComplianceStandard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditReportUseCase {
    AuditReport createAuditReport(UUID transactionId, String merchantId, ComplianceStandard standard, List<ComplianceCheck> checks);
    AuditReport getAuditReport(UUID reportId);
    Optional<AuditReport> findAuditReport(UUID reportId);
    List<AuditReport> getReportsByTransaction(UUID transactionId);
    List<AuditReport> getReportsByMerchant(String merchantId);
}
