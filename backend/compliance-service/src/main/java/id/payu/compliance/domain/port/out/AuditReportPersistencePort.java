package id.payu.compliance.domain.port.out;

import id.payu.compliance.domain.model.AuditReport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for audit report persistence operations.
 * AuditReportEntity tracks compliance reports for regulatory purposes.
 */
public interface AuditReportPersistencePort {

    /**
     * Save an audit report (create or update).
     */
    AuditReport save(AuditReport report);

    /**
     * Find an audit report by its ID.
     */
    Optional<AuditReport> findById(UUID id);

    /**
     * Find all audit reports for a specific transaction.
     */
    List<AuditReport> findByTransactionId(UUID transactionId);

    /**
     * Find all audit reports for a specific merchant.
     */
    List<AuditReport> findByMerchantId(String merchantId);
}
