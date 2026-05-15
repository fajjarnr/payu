package id.payu.backoffice.domain.port.out;

import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.CustomerCaseStatus;

/**
 * Outbound port for Customer Case persistence.
 */
public interface CustomerCasePersistencePort {

    CustomerCaseEntity save(CustomerCaseEntity customerCase);

    Optional<CustomerCaseEntity> findById(UUID id);

    List<CustomerCaseEntity> findByUserId(String userId);

    List<CustomerCaseEntity> findByStatus(CustomerCaseStatus status, int page, int size);

    List<CustomerCaseEntity> findAll(int page, int size);

    List<CustomerCaseEntity> findByUserIdContainingIgnoreCase(String query);

    List<CustomerCaseEntity> findByAccountNumberContainingIgnoreCase(String query);

    List<CustomerCaseEntity> findByCaseNumberContainingIgnoreCase(String query);

    List<CustomerCaseEntity> findBySubjectContainingIgnoreCase(String query);

    void deleteById(UUID id);
}
