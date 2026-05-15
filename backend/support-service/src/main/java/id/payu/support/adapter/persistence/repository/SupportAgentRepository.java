package id.payu.support.adapter.persistence.repository;

import id.payu.support.adapter.persistence.entity.AgentTrainingEntity;
import id.payu.support.adapter.persistence.entity.SupportAgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SupportAgentRepository extends JpaRepository<SupportAgentEntity, Long> {

    Optional<SupportAgentEntity> findByEmployeeId(String employeeId);

    List<SupportAgentEntity> findByActiveTrue();

    long countByActiveTrue();

    @Query("SELECT COUNT(DISTINCT at.agent) FROM AgentTrainingEntity at " +
           "WHERE at.agent.active = true AND at.status = 'PASSED'")
    long countTrainedAgents();
}
