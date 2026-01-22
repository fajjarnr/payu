package id.payu.compliance.domain.port.in;

import id.payu.compliance.domain.model.DataAccessAudit;
import id.payu.compliance.domain.model.DataAccessAudit.DataOperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DataAccessAuditUseCase {

    void logDataAccess(
            String userId,
            String accessedBy,
            String serviceName,
            String resourceType,
            String resourceId,
            DataOperationType operationType,
            String purpose
    );

    void logDataAccess(
            String userId,
            String accessedBy,
            String serviceName,
            String resourceType,
            String resourceId,
            DataOperationType operationType,
            String purpose,
            String ipAddress,
            String userAgent,
            boolean success,
            String errorMessage
    );

    DataAccessAudit getDataAccessAudit(UUID auditId);

    Page<DataAccessAudit> getUserDataAccessHistory(String userId, Pageable pageable);

    List<DataAccessAudit> getUserDataAccessHistoryByDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate);

    List<DataAccessAudit> getAccessedByUserHistory(String accessedBy, LocalDateTime startDate, LocalDateTime endDate);

    Page<DataAccessAudit> getDataAccessByOperationType(DataOperationType operationType, Pageable pageable);

    List<DataAccessAudit> getServiceDataAccessHistory(String serviceName, LocalDateTime startDate, LocalDateTime endDate);

    long getUserDataAccessCount(String userId, LocalDateTime since);

    List<DataAccessAudit> getFailedAccessAttempts(LocalDateTime since);

    Page<DataAccessAudit> searchDataAccessAudit(
            String userId,
            String accessedBy,
            String serviceName,
            DataOperationType operationType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    void deleteDataAccessAudit(UUID auditId);
}