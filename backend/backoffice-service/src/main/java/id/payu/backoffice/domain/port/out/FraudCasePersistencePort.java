package id.payu.backoffice.domain.port.out;

import id.payu.backoffice.adapter.persistence.entity.FraudCaseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.FraudCaseStatus;

/**
 * Outbound port for Fraud Case persistence.
 */
public interface FraudCasePersistencePort {

    FraudCaseEntity save(FraudCaseEntity fraudCase);

    Optional<FraudCaseEntity> findById(UUID id);

    List<FraudCaseEntity> findByUserId(String userId);

    List<FraudCaseEntity> findByStatus(FraudCaseStatus status, int page, int size);

    List<FraudCaseEntity> findAll(int page, int size);

    List<FraudCaseEntity> findByUserIdContainingIgnoreCase(String query);

    List<FraudCaseEntity> findByAccountNumberContainingIgnoreCase(String query);

    List<FraudCaseEntity> findByFraudTypeContainingIgnoreCase(String query);

    void deleteById(UUID id);
}
