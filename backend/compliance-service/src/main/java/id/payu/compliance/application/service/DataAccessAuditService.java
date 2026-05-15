package id.payu.compliance.application.service;

import id.payu.compliance.domain.model.DataAccessAudit;
import id.payu.compliance.domain.model.DataAccessAudit.DataOperationType;
import id.payu.compliance.domain.port.in.DataAccessAuditUseCase;
import id.payu.compliance.domain.port.out.DataAccessAuditPersistencePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataAccessAuditService implements DataAccessAuditUseCase {

    private final DataAccessAuditPersistencePort persistencePort;

    @Override
    @CircuitBreaker(name = "compliance", fallbackMethod = "logDataAccessFallback")
    @Retry(name = "compliance")
    public DataAccessAudit logDataAccess(
            String userId,
            String accessedBy,
            String serviceName,
            String resourceType,
            String resourceId,
            DataOperationType operationType,
            String purpose
    ) {
        return logDataAccess(userId, accessedBy, serviceName, resourceType, resourceId, operationType, purpose, null, null, true, null);
    }

    @Override
    @CircuitBreaker(name = "compliance", fallbackMethod = "logDataAccessFullFallback")
    @Retry(name = "compliance")
    public DataAccessAudit logDataAccess(
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
    ) {
        log.info("Logging data access: userId={}, accessedBy={}, service={}, resource={}, operation={}",
                userId, accessedBy, serviceName, resourceType, operationType);

        DataAccessAudit audit = DataAccessAudit.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accessedBy(accessedBy)
                .serviceName(serviceName)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .operationType(operationType)
                .purpose(purpose)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .errorMessage(errorMessage)
                .accessedAt(LocalDateTime.now())
                .build();

        DataAccessAudit savedAudit = persistencePort.save(audit);
        log.debug("Data access audit logged successfully: {}", savedAudit.getId());
        return savedAudit;
    }

    @Override
    @CircuitBreaker(name = "compliance", fallbackMethod = "getDataAccessAuditFallback")
    @Retry(name = "compliance")
    public DataAccessAudit getDataAccessAudit(UUID auditId) {
        log.info("Retrieving data access audit: {}", auditId);
        return persistencePort.findById(auditId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Data access audit not found: " + auditId));
    }

    @Override
    @CircuitBreaker(name = "compliance", fallbackMethod = "getUserDataAccessHistoryFallback")
    @Retry(name = "compliance")
    public Page<DataAccessAudit> getUserDataAccessHistory(String userId, Pageable pageable) {
        log.info("Retrieving data access history for user: {}", userId);
        return persistencePort.findByUserId(userId, pageable);
    }

    @Override
    public List<DataAccessAudit> getUserDataAccessHistoryByDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Retrieving data access history for user {} between {} and {}", userId, startDate, endDate);
        return persistencePort.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    @Override
    public List<DataAccessAudit> getAccessedByUserHistory(String accessedBy, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Retrieving access history for {} between {} and {}", accessedBy, startDate, endDate);
        return persistencePort.findByAccessedByAndDateRange(accessedBy, startDate, endDate);
    }

    @Override
    public Page<DataAccessAudit> getDataAccessByOperationType(DataOperationType operationType, Pageable pageable) {
        log.info("Retrieving data access by operation type: {}", operationType);
        return persistencePort.findByOperationType(operationType, pageable);
    }

    @Override
    public List<DataAccessAudit> getServiceDataAccessHistory(String serviceName, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Retrieving data access history for service {} between {} and {}", serviceName, startDate, endDate);
        return persistencePort.findByServiceNameAndDateRange(serviceName, startDate, endDate);
    }

    @Override
    public long getUserDataAccessCount(String userId, LocalDateTime since) {
        log.info("Counting data access for user {} since {}", userId, since);
        return persistencePort.countByUserIdSinceDate(userId, since);
    }

    @Override
    public List<DataAccessAudit> getFailedAccessAttempts(LocalDateTime since) {
        log.info("Retrieving failed access attempts since {}", since);
        return persistencePort.findFailedAccessAttemptsSince(since);
    }

    @Override
    @CircuitBreaker(name = "compliance", fallbackMethod = "searchDataAccessAuditFallback")
    @Retry(name = "compliance")
    public Page<DataAccessAudit> searchDataAccessAudit(
            String userId,
            String accessedBy,
            String serviceName,
            DataOperationType operationType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        log.info("Searching data access audit with filters");
        return persistencePort.findByFilters(userId, accessedBy, serviceName, operationType, startDate, endDate, pageable);
    }

    // ═══════════════════════════════════════════════════════
    //  Resilience Fallback Methods
    // ═══════════════════════════════════════════════════════

    private DataAccessAudit logDataAccessFallback(String userId, String accessedBy, String serviceName,
                                                   String resourceType, String resourceId,
                                                   DataOperationType operationType, String purpose, Exception ex) {
        log.error("Fallback for logDataAccess: {}", ex.getMessage());
        throw new RuntimeException("Compliance service temporarily unavailable", ex);
    }

    private DataAccessAudit logDataAccessFullFallback(String userId, String accessedBy, String serviceName,
                                                      String resourceType, String resourceId,
                                                      DataOperationType operationType, String purpose,
                                                      String ipAddress, String userAgent, boolean success,
                                                      String errorMessage, Exception ex) {
        log.error("Fallback for logDataAccess: {}", ex.getMessage());
        throw new RuntimeException("Compliance service temporarily unavailable", ex);
    }

    private DataAccessAudit getDataAccessAuditFallback(UUID auditId, Exception ex) {
        log.error("Fallback for getDataAccessAudit: {}", ex.getMessage());
        throw new RuntimeException("Compliance service temporarily unavailable", ex);
    }

    private Page<DataAccessAudit> getUserDataAccessHistoryFallback(String userId, Pageable pageable, Exception ex) {
        log.error("Fallback for getUserDataAccessHistory: {}", ex.getMessage());
        throw new RuntimeException("Compliance service temporarily unavailable", ex);
    }

    private Page<DataAccessAudit> searchDataAccessAuditFallback(String userId, String accessedBy, String serviceName,
                                                                 DataOperationType operationType, LocalDateTime startDate,
                                                                 LocalDateTime endDate, Pageable pageable, Exception ex) {
        log.error("Fallback for searchDataAccessAudit: {}", ex.getMessage());
        throw new RuntimeException("Compliance service temporarily unavailable", ex);
    }

}
