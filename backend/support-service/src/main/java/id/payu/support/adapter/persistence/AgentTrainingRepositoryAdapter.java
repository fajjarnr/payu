package id.payu.support.adapter.persistence;

import id.payu.support.adapter.persistence.entity.AgentTrainingEntity;
import id.payu.support.adapter.persistence.entity.SupportAgentEntity;
import id.payu.support.adapter.persistence.entity.TrainingModuleEntity;
import id.payu.support.adapter.persistence.repository.AgentTrainingRepository;
import id.payu.support.adapter.persistence.repository.SupportAgentRepository;
import id.payu.support.adapter.persistence.repository.TrainingModuleRepository;
import id.payu.support.domain.CompletionStatus;
import id.payu.support.domain.TrainingStatus;
import id.payu.support.domain.model.AgentTraining;
import id.payu.support.domain.port.out.AgentTrainingRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AgentTrainingRepositoryAdapter implements AgentTrainingRepositoryPort {

    private final AgentTrainingRepository repository;
    private final SupportAgentRepository agentRepository;
    private final TrainingModuleRepository moduleRepository;

    @Override
    public List<AgentTraining> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<AgentTraining> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<AgentTraining> findByAgentId(Long agentId) {
        return repository.findByAgentId(agentId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<AgentTraining> findByTrainingModuleId(Long moduleId) {
        return repository.findByTrainingModuleId(moduleId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<AgentTraining> findByAgentIdAndTrainingModuleId(Long agentId, Long moduleId) {
        return repository.findByAgentIdAndTrainingModuleId(agentId, moduleId).map(this::toDomain);
    }

    @Override
    public AgentTraining save(AgentTraining domain) {
        AgentTrainingEntity entity;
        if (domain.getId() != null) {
            entity = repository.findById(domain.getId()).orElseGet(() -> toEntity(domain));
            entity.setStatus(domain.getStatus());
            entity.setScore(domain.getScore());
            entity.setStartedAt(domain.getStartedAt());
            entity.setCompletedAt(domain.getCompletedAt());
            entity.setNotes(domain.getNotes());
        } else {
            entity = toEntity(domain);
        }
        AgentTrainingEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public long countByAgentIdAndTrainingModuleMandatoryTrueAndTrainingModuleStatusAndStatus(
            Long agentId, boolean mandatory, TrainingStatus moduleStatus, CompletionStatus completionStatus) {
        return repository.countByAgentIdAndTrainingModuleMandatoryTrueAndTrainingModuleStatusAndStatus(
                agentId, mandatory, moduleStatus, completionStatus);
    }

    private AgentTraining toDomain(AgentTrainingEntity entity) {
        if (entity == null) return null;
        return AgentTraining.builder()
                .id(entity.getId())
                .agentId(entity.getAgent() != null ? entity.getAgent().getId() : null)
                .agentName(entity.getAgent() != null ? entity.getAgent().getName() : null)
                .trainingModuleId(entity.getTrainingModule() != null ? entity.getTrainingModule().getId() : null)
                .moduleCode(entity.getTrainingModule() != null ? entity.getTrainingModule().getCode() : null)
                .moduleTitle(entity.getTrainingModule() != null ? entity.getTrainingModule().getTitle() : null)
                .status(entity.getStatus())
                .score(entity.getScore())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    private AgentTrainingEntity toEntity(AgentTraining domain) {
        if (domain == null) return null;
        SupportAgentEntity agent = domain.getAgentId() != null
                ? agentRepository.findById(domain.getAgentId()).orElse(null)
                : null;
        TrainingModuleEntity module = domain.getTrainingModuleId() != null
                ? moduleRepository.findById(domain.getTrainingModuleId()).orElse(null)
                : null;

        return AgentTrainingEntity.builder()
                .id(domain.getId())
                .agent(agent)
                .trainingModule(module)
                .status(domain.getStatus())
                .score(domain.getScore())
                .startedAt(domain.getStartedAt())
                .completedAt(domain.getCompletedAt())
                .notes(domain.getNotes())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }
}
