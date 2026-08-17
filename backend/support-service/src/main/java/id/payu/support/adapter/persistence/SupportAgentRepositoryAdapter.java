package id.payu.support.adapter.persistence;

import id.payu.support.adapter.persistence.entity.SupportAgentEntity;
import id.payu.support.adapter.persistence.repository.SupportAgentRepository;
import id.payu.support.domain.model.SupportAgent;
import id.payu.support.domain.port.out.SupportAgentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SupportAgentRepositoryAdapter implements SupportAgentRepositoryPort {

    private final SupportAgentRepository repository;

    @Override
    public List<SupportAgent> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<SupportAgent> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<SupportAgent> findByEmployeeId(String employeeId) {
        return repository.findByEmployeeId(employeeId).map(this::toDomain);
    }

    @Override
    public SupportAgent save(SupportAgent agent) {
        SupportAgentEntity entity = toEntity(agent);
        SupportAgentEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public long countByActiveTrue() {
        return repository.countByActiveTrue();
    }

    @Override
    public long countTrainedAgents() {
        return repository.countTrainedAgents();
    }

    private SupportAgent toDomain(SupportAgentEntity entity) {
        if (entity == null) return null;
        return SupportAgent.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .name(entity.getName())
                .email(entity.getEmail())
                .department(entity.getDepartment())
                .level(entity.getLevel())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    private SupportAgentEntity toEntity(SupportAgent domain) {
        if (domain == null) return null;
        return SupportAgentEntity.builder()
                .id(domain.getId())
                .employeeId(domain.getEmployeeId())
                .name(domain.getName())
                .email(domain.getEmail())
                .department(domain.getDepartment())
                .level(domain.getLevel())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }
}
