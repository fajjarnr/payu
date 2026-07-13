package id.payu.compliance.adapter.persistence;

import id.payu.compliance.adapter.persistence.entity.AuditReportEntity;
import id.payu.compliance.domain.model.AuditReport;
import id.payu.compliance.domain.port.out.AuditReportPersistencePort;
import id.payu.compliance.adapter.persistence.repository.AuditReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuditReportPersistenceAdapter implements AuditReportPersistencePort {

    private final AuditReportRepository repository;

    @Override
    public AuditReport save(AuditReport report) {
        return toDomain(repository.save(toEntity(report)));
    }

    @Override
    public Optional<AuditReport> findById(UUID id) {
        return repository.findById(id).map(AuditReportPersistenceAdapter::toDomain);
    }

    @Override
    public List<AuditReport> findByTransactionId(UUID transactionId) {
        return repository.findByTransactionId(transactionId).stream().map(AuditReportPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<AuditReport> findByMerchantId(String merchantId) {
        return repository.findByMerchantId(merchantId).stream().map(AuditReportPersistenceAdapter::toDomain).toList();
    }

    private static AuditReportEntity toEntity(AuditReport report) {
        return AuditReportEntity.builder()
                .id(report.getId())
                .transactionId(report.getTransactionId())
                .merchantId(report.getMerchantId())
                .standard(report.getStandard())
                .checks(report.getChecks())
                .overallStatus(report.getOverallStatus())
                .createdAt(report.getCreatedAt())
                .createdBy(report.getCreatedBy())
                .version(report.getVersion())
                .build();
    }

    private static AuditReport toDomain(AuditReportEntity entity) {
        return AuditReport.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .merchantId(entity.getMerchantId())
                .standard(entity.getStandard())
                .checks(entity.getChecks())
                .overallStatus(entity.getOverallStatus())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .version(entity.getVersion())
                .build();
    }
}
