package id.payu.lending.domain.port.out;

import id.payu.lending.domain.model.CreditScore;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for credit score persistence operations.
 * Manages credit score data for lending risk assessment.
 */
public interface CreditScorePersistencePort {

    /**
     * Save a credit score.
     *
     * @param creditScore the credit score to save
     * @return the saved credit score
     */
    CreditScore save(CreditScore creditScore);

    /**
     * Find credit score by user ID.
     *
     * @param userId the user ID
     * @return optional containing the credit score if found
     */
    Optional<CreditScore> findByUserId(UUID userId);
}
