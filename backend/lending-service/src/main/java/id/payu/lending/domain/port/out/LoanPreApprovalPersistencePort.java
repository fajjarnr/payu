package id.payu.lending.domain.port.out;

import id.payu.lending.domain.model.LoanPreApproval;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for loan pre-approval persistence operations.
 * Manages pre-approval data for faster loan processing.
 */
public interface LoanPreApprovalPersistencePort {

    /**
     * Save a loan pre-approval.
     *
     * @param preApproval the pre-approval to save
     * @return the saved pre-approval
     */
    LoanPreApproval save(LoanPreApproval preApproval);

    /**
     * Find pre-approval by ID.
     *
     * @param id the pre-approval ID
     * @return optional containing the pre-approval if found
     */
    Optional<LoanPreApproval> findById(UUID id);

    /**
     * Find active pre-approval for a user.
     * A pre-approval is active if validUntil is after the current date.
     *
     * @param userId the user ID
     * @return optional containing the active pre-approval if found
     */
    Optional<LoanPreApproval> findActiveByUserId(UUID userId);

    /**
     * Delete pre-approval by ID.
     *
     * @param id the pre-approval ID
     */
    void deleteById(UUID id);
}
