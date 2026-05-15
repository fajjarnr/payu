package id.payu.backoffice.domain.port.out;

import id.payu.backoffice.domain.FraudCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for Fraud Case persistence.
 */
public interface FraudCasePersistencePort {

    FraudCase save(FraudCase fraudCase);

    Optional<FraudCase> findById(UUID id);

    List<FraudCase> findByUserId(String userId);

    List<FraudCase> findByStatus(FraudCase.CaseStatus status, int page, int size);

    List<FraudCase> findAll(int page, int size);

    List<FraudCase> findByUserIdContainingIgnoreCase(String query);

    List<FraudCase> findByAccountNumberContainingIgnoreCase(String query);

    List<FraudCase> findByFraudTypeContainingIgnoreCase(String query);

    void deleteById(UUID id);
}
