package id.payu.compliance.domain.port.out;

import id.payu.compliance.domain.model.DataAccessAudit;
import id.payu.compliance.domain.model.DataOperationType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Output port for data access audit persistence operations.
 * Tracks all data access for GDPR compliance and security monitoring.
 */
public interface DataAccessAuditPersistencePort {

    /**
     * Save a data access audit entry.
     */
    DataAccessAudit save(DataAccessAudit audit);

    /**
     * Find audit entries by user ID with pagination.
     */
    Page<DataAccessAudit> findByUserId(String userId, Pageable pageable);

    /**
     * Find audit entries by user ID within a date range.
     */
    List<DataAccessAudit> findByUserIdAndDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find audit entries by the person who accessed data, within a date range.
     */
    List<DataAccessAudit> findByAccessedByAndDateRange(String accessedBy, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find audit entries by operation type with pagination.
     */
    Page<DataAccessAudit> findByOperationType(DataOperationType operationType, Pageable pageable);

    /**
     * Find audit entries by service name within a date range.
     */
    List<DataAccessAudit> findByServiceNameAndDateRange(String serviceName, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count audit entries for a user since a specific date.
     */
    long countByUserIdSinceDate(String userId, LocalDateTime since);

    /**
     * Find failed access attempts since a specific date.
     */
    List<DataAccessAudit> findFailedAccessAttemptsSince(LocalDateTime since);

    /**
     * Find audit entries by multiple filters with pagination.
     */
    Page<DataAccessAudit> findByFilters(
            String userId,
            String accessedBy,
            String serviceName,
            DataOperationType operationType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find audit entries by ID(s).
     */
    List<DataAccessAudit> findById(UUID id);

    /**
     * Delete an audit entry by ID.
     */
    void deleteById(UUID id);
}
