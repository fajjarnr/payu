package id.payu.compliance.domain.port.in;

import id.payu.compliance.adapter.persistence.entity.AuditReportEntity;
import id.payu.compliance.domain.model.ComplianceCheck;
import id.payu.compliance.domain.model.ComplianceStandard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditReportUseCase {
    AuditReportEntity createAuditReport(UUID transactionId, String merchantId, ComplianceStandard standard, List<ComplianceCheck> checks);
    AuditReportEntity getAuditReport(UUID reportId);
    Optional<AuditReportEntity> findAuditReport(UUID reportId);
    List<AuditReportEntity> getReportsByTransaction(UUID transactionId);
    List<AuditReportEntity> getReportsByMerchant(String merchantId);
}
