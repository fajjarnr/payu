package id.payu.compliance.domain.port.out;

import id.payu.compliance.domain.model.DataAccessAudit;
import id.payu.compliance.domain.model.DataAccessAudit.DataOperationType;

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
     *
     * @param audit the audit entry to save
     * @return the saved audit entry
     */
    DataAccessAudit save(DataAccessAudit audit);

    /**
     * Find audit entries by user ID with pagination.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of audit entries for the user
     */
    Page<DataAccessAudit> findByUserId(String userId, Pageable pageable);

    /**
     * Find audit entries by user ID within a date range.
     *
     * @param userId the user ID
     * @param startDate the start date
     * @param endDate the end date
     * @return list of audit entries within the date range
     */
    List<DataAccessAudit> findByUserIdAndDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find audit entries by the person who accessed data, within a date range.
     *
     * @param accessedBy the ID of the person who accessed data
     * @param startDate the start date
     * @param endDate the end date
     * @return list of audit entries
     */
    List<DataAccessAudit> findByAccessedByAndDateRange(String accessedBy, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find audit entries by operation type with pagination.
     *
     * @param operationType the type of operation (READ, WRITE, DELETE, etc.)
     * @param pageable pagination parameters
     * @return page of audit entries with the specified operation type
     */
    Page<DataAccessAudit> findByOperationType(DataOperationType operationType, Pageable pageable);

    /**
     * Find audit entries by service name within a date range.
     *
     * @param serviceName the name of the service that accessed data
     * @param startDate the start date
     * @param endDate the end date
     * @return list of audit entries
     */
    List<DataAccessAudit> findByServiceNameAndDateRange(String serviceName, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count audit entries for a user since a specific date.
     *
     * @param userId the user ID
     * @param since the date to count from
     * @return the count of audit entries
     */
    long countByUserIdSinceDate(String userId, LocalDateTime since);

    /**
     * Find failed access attempts since a specific date.
     *
     * @param since the date to search from
     * @return list of failed access attempts
     */
    List<DataAccessAudit> findFailedAccessAttemptsSince(LocalDateTime since);

    /**
     * Find audit entries by multiple filters with pagination.
     *
     * @param userId optional user ID filter
     * @param accessedBy optional accessor ID filter
     * @param serviceName optional service name filter
     * @param operationType optional operation type filter
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @param pageable pagination parameters
     * @return page of filtered audit entries
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
     *
     * @param id the audit entry ID
     * @return list of matching audit entries
     */
    List<DataAccessAudit> findById(UUID id);

    /**
     * Delete an audit entry by ID.
     *
     * @param id the ID of the audit entry to delete
     */
    void deleteById(UUID id);
}
