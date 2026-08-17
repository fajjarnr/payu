package id.payu.support.adapter.persistence;

import id.payu.support.adapter.persistence.entity.TrainingModuleEntity;
import id.payu.support.adapter.persistence.repository.TrainingModuleRepository;
import id.payu.support.domain.TrainingStatus;
import id.payu.support.domain.model.TrainingModule;
import id.payu.support.domain.port.out.TrainingModuleRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TrainingModuleRepositoryAdapter implements TrainingModuleRepositoryPort {

    private final TrainingModuleRepository repository;

    @Override
    public List<TrainingModule> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<TrainingModule> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<TrainingModule> findByCode(String code) {
        return repository.findByCode(code).map(this::toDomain);
    }

    @Override
    public TrainingModule save(TrainingModule module) {
        TrainingModuleEntity entity = toEntity(module);
        TrainingModuleEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<TrainingModule> findByStatus(TrainingStatus status) {
        return repository.findByStatus(status).stream().map(this::toDomain).toList();
    }

    @Override
    public List<TrainingModule> findByMandatoryTrue() {
        return repository.findByMandatoryTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public List<TrainingModule> findByStatusAndMandatoryTrue(TrainingStatus status) {
        return repository.findByStatusAndMandatoryTrue(status).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByMandatoryTrueAndStatus(TrainingStatus status) {
        return repository.countByMandatoryTrueAndStatus(status);
    }

    private TrainingModule toDomain(TrainingModuleEntity entity) {
        if (entity == null) return null;
        return TrainingModule.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .durationMinutes(entity.getDurationMinutes())
                .status(entity.getStatus())
                .mandatory(entity.isMandatory())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    private TrainingModuleEntity toEntity(TrainingModule domain) {
        if (domain == null) return null;
        return TrainingModuleEntity.builder()
                .id(domain.getId())
                .code(domain.getCode())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .category(domain.getCategory())
                .durationMinutes(domain.getDurationMinutes())
                .status(domain.getStatus())
                .mandatory(domain.isMandatory())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }
}
