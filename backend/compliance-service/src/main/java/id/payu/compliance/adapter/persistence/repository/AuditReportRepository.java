package id.payu.compliance.adapter.persistence.repository;

import id.payu.compliance.adapter.persistence.entity.AuditReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditReportRepository extends JpaRepository<AuditReportEntity, UUID> {
    List<AuditReportEntity> findByTransactionId(UUID transactionId);
    List<AuditReportEntity> findByMerchantId(String merchantId);
}
