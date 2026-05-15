package id.payu.compliance.adapter.persistence;

import id.payu.compliance.adapter.persistence.entity.AuditReportEntity;
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
    public AuditReportEntity save(AuditReportEntity report) {
        return repository.save(report);
    }

    @Override
    public Optional<AuditReportEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<AuditReportEntity> findByTransactionId(UUID transactionId) {
        return repository.findByTransactionId(transactionId);
    }

    @Override
    public List<AuditReportEntity> findByMerchantId(String merchantId) {
        return repository.findByMerchantId(merchantId);
    }
}
