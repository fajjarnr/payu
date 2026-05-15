package id.payu.backoffice.domain.port.out;

import id.payu.backoffice.domain.CustomerCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for Customer Case persistence.
 */
public interface CustomerCasePersistencePort {

    CustomerCase save(CustomerCase customerCase);

    Optional<CustomerCase> findById(UUID id);

    List<CustomerCase> findByUserId(String userId);

    List<CustomerCase> findByStatus(CustomerCase.CaseStatus status, int page, int size);

    List<CustomerCase> findAll(int page, int size);

    List<CustomerCase> findByUserIdContainingIgnoreCase(String query);

    List<CustomerCase> findByAccountNumberContainingIgnoreCase(String query);

    List<CustomerCase> findByCaseNumberContainingIgnoreCase(String query);

    List<CustomerCase> findBySubjectContainingIgnoreCase(String query);

    void deleteById(UUID id);
}
