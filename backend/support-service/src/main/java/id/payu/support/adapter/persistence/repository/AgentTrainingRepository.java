package id.payu.support.adapter.persistence.repository;

import id.payu.support.adapter.persistence.entity.AgentTrainingEntity;
import id.payu.support.adapter.persistence.entity.SupportAgentEntity;
import id.payu.support.adapter.persistence.entity.TrainingModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import id.payu.support.domain.CompletionStatus;
import id.payu.support.domain.TrainingStatus;

public interface AgentTrainingRepository extends JpaRepository<AgentTrainingEntity, Long> {

    List<AgentTrainingEntity> findByAgent(SupportAgentEntity agent);

    List<AgentTrainingEntity> findByTrainingModule(TrainingModuleEntity trainingModule);

    Optional<AgentTrainingEntity> findByAgentAndTrainingModule(SupportAgentEntity agent, TrainingModuleEntity trainingModule);

    Optional<AgentTrainingEntity> findByAgentIdAndTrainingModuleId(Long agentId, Long moduleId);

    List<AgentTrainingEntity> findByAgentId(Long agentId);

    List<AgentTrainingEntity> findByTrainingModuleId(Long moduleId);

    @Query("SELECT COUNT(at) FROM AgentTrainingEntity at " +
           "WHERE at.agent.id = :agentId " +
           "AND at.trainingModule.mandatory = true " +
           "AND at.trainingModule.status = :moduleStatus " +
           "AND at.status = :completionStatus")
    long countByAgentIdAndTrainingModuleMandatoryTrueAndTrainingModuleStatusAndStatus(
            @Param("agentId") Long agentId,
            @Param("mandatory") boolean mandatory,
            @Param("moduleStatus") TrainingStatus moduleStatus,
            @Param("completionStatus") CompletionStatus completionStatus);
}
