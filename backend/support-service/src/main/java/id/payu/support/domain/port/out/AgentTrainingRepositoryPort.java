package id.payu.support.domain.port.out;

import id.payu.support.domain.CompletionStatus;
import id.payu.support.domain.TrainingStatus;
import id.payu.support.domain.model.AgentTraining;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for AgentTraining persistence.
 */
public interface AgentTrainingRepositoryPort {

    List<AgentTraining> findAll();

    Optional<AgentTraining> findById(Long id);

    List<AgentTraining> findByAgentId(Long agentId);

    List<AgentTraining> findByTrainingModuleId(Long moduleId);

    Optional<AgentTraining> findByAgentIdAndTrainingModuleId(Long agentId, Long moduleId);

    AgentTraining save(AgentTraining training);

    long countByAgentIdAndTrainingModuleMandatoryTrueAndTrainingModuleStatusAndStatus(
            Long agentId, boolean mandatory, TrainingStatus moduleStatus, CompletionStatus completionStatus);
}
