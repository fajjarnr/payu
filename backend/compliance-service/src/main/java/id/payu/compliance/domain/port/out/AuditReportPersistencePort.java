package id.payu.compliance.domain.port.out;

import id.payu.compliance.domain.model.AuditReport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for audit report persistence operations.
 * AuditReport tracks compliance reports for regulatory purposes.
 */
public interface AuditReportPersistencePort {

    /**
     * Save an audit report (create or update).
     *
     * @param report the audit report to save
     * @return the saved report
     */
    AuditReport save(AuditReport report);

    /**
     * Find an audit report by its ID.
     *
     * @param id the report ID
     * @return Optional containing the report if found
     */
    Optional<AuditReport> findById(UUID id);

    /**
     * Find all audit reports for a specific transaction.
     *
     * @param transactionId the transaction ID
     * @return list of audit reports for the transaction
     */
    List<AuditReport> findByTransactionId(UUID transactionId);

    /**
     * Find all audit reports for a specific merchant.
     *
     * @param merchantId the merchant ID
     * @return list of audit reports for the merchant
     */
    List<AuditReport> findByMerchantId(String merchantId);
}
