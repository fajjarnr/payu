package id.payu.support.domain.port.out;

import id.payu.support.domain.AgentTraining;
import id.payu.support.domain.SupportAgent;
import id.payu.support.domain.TrainingModule;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Agent Training persistence.
 */
public interface AgentTrainingPersistencePort {

    AgentTraining save(AgentTraining agentTraining);

    List<AgentTraining> findAll();

    List<AgentTraining> findByAgentId(Long agentId);

    List<AgentTraining> findByTrainingModuleId(Long moduleId);

    Optional<AgentTraining> findByAgentIdAndTrainingModuleId(Long agentId, Long moduleId);

    Optional<AgentTraining> findByAgentAndTrainingModule(SupportAgent agent, TrainingModule module);

    List<AgentTraining> findByAgent(SupportAgent agent);
}
