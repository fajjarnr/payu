package id.payu.support.adapter.persistence.repository;

import id.payu.support.domain.AgentTraining;
import id.payu.support.domain.SupportAgent;
import id.payu.support.domain.TrainingModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgentTrainingRepository extends JpaRepository<AgentTraining, Long> {

    List<AgentTraining> findByAgent(SupportAgent agent);

    List<AgentTraining> findByTrainingModule(TrainingModule trainingModule);

    Optional<AgentTraining> findByAgentAndTrainingModule(SupportAgent agent, TrainingModule trainingModule);

    Optional<AgentTraining> findByAgentIdAndTrainingModuleId(Long agentId, Long moduleId);

    List<AgentTraining> findByAgentId(Long agentId);

    List<AgentTraining> findByTrainingModuleId(Long moduleId);

    @Query("SELECT COUNT(at) FROM AgentTraining at " +
           "WHERE at.agent.id = :agentId " +
           "AND at.trainingModule.mandatory = true " +
           "AND at.trainingModule.status = :moduleStatus " +
           "AND at.status = :completionStatus")
    long countByAgentIdAndTrainingModuleMandatoryTrueAndTrainingModuleStatusAndStatus(
            @Param("agentId") Long agentId,
            @Param("mandatory") boolean mandatory,
            @Param("moduleStatus") TrainingModule.TrainingStatus moduleStatus,
            @Param("completionStatus") AgentTraining.CompletionStatus completionStatus);
}
