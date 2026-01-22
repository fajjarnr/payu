package id.payu.compliance.adapter.persistence;

import id.payu.compliance.adapter.persistence.repository.DataAccessAuditRepository;
import id.payu.compliance.domain.model.DataAccessAudit;
import id.payu.compliance.domain.model.DataAccessAudit.DataOperationType;
import id.payu.compliance.domain.port.out.DataAccessAuditPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataAccessAuditPersistenceAdapter implements DataAccessAuditPersistencePort {

    private final DataAccessAuditRepository repository;

    @Override
    public DataAccessAudit save(DataAccessAudit audit) {
        return repository.save(audit);
    }

    @Override
    public Page<DataAccessAudit> findByUserId(String userId, Pageable pageable) {
        return repository.findByUserIdOrderByAccessedAtDesc(userId, pageable);
    }

    @Override
    public List<DataAccessAudit> findByUserIdAndDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        return repository.findByUserIdAndAccessedAtBetweenOrderByAccessedAtDesc(userId, startDate, endDate);
    }

    @Override
    public List<DataAccessAudit> findByAccessedByAndDateRange(String accessedBy, LocalDateTime startDate, LocalDateTime endDate) {
        return repository.findByAccessedByAndAccessedAtBetweenOrderByAccessedAtDesc(accessedBy, startDate, endDate);
    }

    @Override
    public Page<DataAccessAudit> findByOperationType(DataOperationType operationType, Pageable pageable) {
        return repository.findByOperationTypeOrderByAccessedAtDesc(operationType, pageable);
    }

    @Override
    public List<DataAccessAudit> findByServiceNameAndDateRange(String serviceName, LocalDateTime startDate, LocalDateTime endDate) {
        return repository.findByServiceNameAndDateRange(serviceName, startDate, endDate);
    }

    @Override
    public long countByUserIdSinceDate(String userId, LocalDateTime since) {
        return repository.countByUserIdSinceDate(userId, since);
    }

    @Override
    public List<DataAccessAudit> findFailedAccessAttemptsSince(LocalDateTime since) {
        return repository.findFailedAccessAttemptsSince(since);
    }

    @Override
    public Page<DataAccessAudit> findByFilters(
            String userId,
            String accessedBy,
            String serviceName,
            DataOperationType operationType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        return repository.findByFilters(userId, accessedBy, serviceName, operationType, startDate, endDate, pageable);
    }

    @Override
    public List<DataAccessAudit> findById(UUID id) {
        return repository.findById(id).map(List::of).orElse(List.of());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}