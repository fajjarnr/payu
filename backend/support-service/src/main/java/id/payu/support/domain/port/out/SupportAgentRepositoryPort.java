package id.payu.support.domain.port.out;

import id.payu.support.domain.model.SupportAgent;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for SupportAgent persistence.
 */
public interface SupportAgentRepositoryPort {

    List<SupportAgent> findAll();

    Optional<SupportAgent> findById(Long id);

    Optional<SupportAgent> findByEmployeeId(String employeeId);

    SupportAgent save(SupportAgent agent);

    long countByActiveTrue();

    long countTrainedAgents();
}
