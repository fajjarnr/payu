package id.payu.support.repository;

import id.payu.support.domain.AgentTraining;
import id.payu.support.domain.SupportAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SupportAgentRepository extends JpaRepository<SupportAgent, Long> {

    Optional<SupportAgent> findByEmployeeId(String employeeId);

    List<SupportAgent> findByActiveTrue();

    long countByActiveTrue();

    @Query("SELECT COUNT(DISTINCT at.agent) FROM AgentTraining at " +
           "WHERE at.agent.active = true AND at.status = 'PASSED'")
    long countTrainedAgents();
}
