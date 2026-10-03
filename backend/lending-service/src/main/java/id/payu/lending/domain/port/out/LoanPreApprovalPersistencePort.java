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

    LoanPreApproval save(LoanPreApproval preApproval);

    Optional<LoanPreApproval> findById(UUID id);

    Optional<LoanPreApproval> findActiveByUserId(UUID userId);

    void deleteById(UUID id);
}
