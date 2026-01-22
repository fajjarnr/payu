package id.payu.compliance.adapter.persistence.repository;

import id.payu.compliance.domain.model.AuditReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditReportRepository extends JpaRepository<AuditReport, UUID> {
    List<AuditReport> findByTransactionId(UUID transactionId);
    List<AuditReport> findByMerchantId(String merchantId);
}
