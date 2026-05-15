package id.payu.support.domain.port.out;

import id.payu.support.adapter.persistence.entity.AgentTrainingEntity;
import id.payu.support.adapter.persistence.entity.SupportAgentEntity;
import id.payu.support.adapter.persistence.entity.TrainingModuleEntity;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Agent Training persistence.
 */
public interface AgentTrainingPersistencePort {

    AgentTrainingEntity save(AgentTrainingEntity agentTraining);

    List<AgentTrainingEntity> findAll();

    List<AgentTrainingEntity> findByAgentId(Long agentId);

    List<AgentTrainingEntity> findByTrainingModuleId(Long moduleId);

    Optional<AgentTrainingEntity> findByAgentIdAndTrainingModuleId(Long agentId, Long moduleId);

    Optional<AgentTrainingEntity> findByAgentAndTrainingModule(SupportAgentEntity agent, TrainingModuleEntity module);

    List<AgentTrainingEntity> findByAgent(SupportAgentEntity agent);
}
