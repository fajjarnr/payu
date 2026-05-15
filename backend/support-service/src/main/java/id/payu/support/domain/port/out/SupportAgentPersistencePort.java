package id.payu.support.domain.port.out;

import id.payu.support.adapter.persistence.entity.SupportAgentEntity;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Support Agent persistence.
 */
public interface SupportAgentPersistencePort {

    SupportAgentEntity save(SupportAgentEntity agent);

    Optional<SupportAgentEntity> findById(Long id);

    Optional<SupportAgentEntity> findByEmployeeId(String employeeId);

    List<SupportAgentEntity> findAll();

    long countByActiveTrue();

    long countTrainedAgents();
}
