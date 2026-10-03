package id.payu.lending.domain.port.out;

import id.payu.lending.domain.model.CreditScore;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for credit score persistence operations.
 * Manages credit score data for lending risk assessment.
 */
public interface CreditScorePersistencePort {

    CreditScore save(CreditScore creditScore);

    Optional<CreditScore> findByUserId(UUID userId);
}
