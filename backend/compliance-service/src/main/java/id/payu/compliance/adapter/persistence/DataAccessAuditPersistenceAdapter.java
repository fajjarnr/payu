package id.payu.compliance.adapter.persistence;

import id.payu.compliance.adapter.persistence.repository.DataAccessAuditRepository;
import id.payu.compliance.adapter.persistence.entity.DataAccessAuditEntity;
import id.payu.compliance.domain.model.DataAccessAudit;
import id.payu.compliance.domain.model.DataOperationType;
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
        return toDomain(repository.save(toEntity(audit)));
    }

    @Override
    public Page<DataAccessAudit> findByUserId(String userId, Pageable pageable) {
        return repository.findByUserIdOrderByAccessedAtDesc(userId, pageable).map(DataAccessAuditPersistenceAdapter::toDomain);
    }

    @Override
    public List<DataAccessAudit> findByUserIdAndDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        return map(repository.findByUserIdAndAccessedAtBetweenOrderByAccessedAtDesc(userId, startDate, endDate));
    }

    @Override
    public List<DataAccessAudit> findByAccessedByAndDateRange(String accessedBy, LocalDateTime startDate, LocalDateTime endDate) {
        return map(repository.findByAccessedByAndAccessedAtBetweenOrderByAccessedAtDesc(accessedBy, startDate, endDate));
    }

    @Override
    public Page<DataAccessAudit> findByOperationType(DataOperationType operationType, Pageable pageable) {
        return repository.findByOperationTypeOrderByAccessedAtDesc(operationType, pageable).map(DataAccessAuditPersistenceAdapter::toDomain);
    }

    @Override
    public List<DataAccessAudit> findByServiceNameAndDateRange(String serviceName, LocalDateTime startDate, LocalDateTime endDate) {
        return map(repository.findByServiceNameAndDateRange(serviceName, startDate, endDate));
    }

    @Override
    public long countByUserIdSinceDate(String userId, LocalDateTime since) {
        return repository.countByUserIdSinceDate(userId, since);
    }

    @Override
    public List<DataAccessAudit> findFailedAccessAttemptsSince(LocalDateTime since) {
        return map(repository.findFailedAccessAttemptsSince(since));
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
        return repository.findByFilters(userId, accessedBy, serviceName, operationType, startDate, endDate, pageable)
                .map(DataAccessAuditPersistenceAdapter::toDomain);
    }

    @Override
    public List<DataAccessAudit> findById(UUID id) {
        return repository.findById(id).map(DataAccessAuditPersistenceAdapter::toDomain).map(List::of).orElse(List.of());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private static List<DataAccessAudit> map(List<DataAccessAuditEntity> entities) {
        return entities.stream().map(DataAccessAuditPersistenceAdapter::toDomain).toList();
    }

    private static DataAccessAuditEntity toEntity(DataAccessAudit audit) {
        return DataAccessAuditEntity.builder()
                .id(audit.getId())
                .userId(audit.getUserId())
                .accessedBy(audit.getAccessedBy())
                .serviceName(audit.getServiceName())
                .resourceType(audit.getResourceType())
                .resourceId(audit.getResourceId())
                .operationType(audit.getOperationType())
                .purpose(audit.getPurpose())
                .ipAddress(audit.getIpAddress())
                .userAgent(audit.getUserAgent())
                .success(audit.isSuccess())
                .errorMessage(audit.getErrorMessage())
                .accessedAt(audit.getAccessedAt())
                .createdAt(audit.getCreatedAt())
                .version(audit.getVersion() != null ? audit.getVersion() : 0L)
                .build();
    }

    private static DataAccessAudit toDomain(DataAccessAuditEntity entity) {
        return DataAccessAudit.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .accessedBy(entity.getAccessedBy())
                .serviceName(entity.getServiceName())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .operationType(entity.getOperationType())
                .purpose(entity.getPurpose())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .success(Boolean.TRUE.equals(entity.getSuccess()))
                .errorMessage(entity.getErrorMessage())
                .accessedAt(entity.getAccessedAt())
                .createdAt(entity.getCreatedAt())
                .version(entity.getVersion())
                .build();
    }
}
