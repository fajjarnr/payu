package id.payu.compliance.domain.port.in;

import id.payu.compliance.adapter.persistence.entity.DataAccessAuditEntity;
import id.payu.compliance.domain.model.DataOperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DataAccessAuditUseCase {

    DataAccessAuditEntity logDataAccess(
            String userId,
            String accessedBy,
            String serviceName,
            String resourceType,
            String resourceId,
            DataOperationType operationType,
            String purpose
    );

    DataAccessAuditEntity logDataAccess(
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

    DataAccessAuditEntity getDataAccessAudit(UUID auditId);

    Page<DataAccessAuditEntity> getUserDataAccessHistory(String userId, Pageable pageable);

    List<DataAccessAuditEntity> getUserDataAccessHistoryByDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate);

    List<DataAccessAuditEntity> getAccessedByUserHistory(String accessedBy, LocalDateTime startDate, LocalDateTime endDate);

    Page<DataAccessAuditEntity> getDataAccessByOperationType(DataOperationType operationType, Pageable pageable);

    List<DataAccessAuditEntity> getServiceDataAccessHistory(String serviceName, LocalDateTime startDate, LocalDateTime endDate);

    long getUserDataAccessCount(String userId, LocalDateTime since);

    List<DataAccessAuditEntity> getFailedAccessAttempts(LocalDateTime since);

    Page<DataAccessAuditEntity> searchDataAccessAudit(
            String userId,
            String accessedBy,
            String serviceName,
            DataOperationType operationType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

}
