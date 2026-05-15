package id.payu.support.domain.port.out;

import id.payu.support.domain.SupportAgent;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Support Agent persistence.
 */
public interface SupportAgentPersistencePort {

    SupportAgent save(SupportAgent agent);

    Optional<SupportAgent> findById(Long id);

    Optional<SupportAgent> findByEmployeeId(String employeeId);

    List<SupportAgent> findAll();

    long countByActiveTrue();

    long countTrainedAgents();
}
