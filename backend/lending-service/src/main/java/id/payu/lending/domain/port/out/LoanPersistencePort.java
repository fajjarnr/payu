package id.payu.lending.domain.port.out;

import id.payu.lending.domain.model.Loan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for loan persistence operations.
 * Manages loan data including personal loans and other lending products.
 */
public interface LoanPersistencePort {

    /**
     * Save a loan.
     *
     * @param loan the loan to save
     * @return the saved loan
     */
    Loan save(Loan loan);

    /**
     * Find loan by ID.
     *
     * @param id the loan ID
     * @return optional containing the loan if found
     */
    Optional<Loan> findById(UUID id);

    /**
     * Find loan by external ID.
     *
     * @param externalId the external loan ID
     * @return optional containing the loan if found
     */
    Optional<Loan> findByExternalId(String externalId);

    /**
     * Find all loans for a user.
     *
     * @param userId the user ID
     * @return list of loans for the user
     */
    List<Loan> findByUserId(UUID userId);

    /**
     * Delete a loan.
     *
     * @param loan the loan to delete
     */
    void delete(Loan loan);
}
