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

    Loan save(Loan loan);

    Optional<Loan> findById(UUID id);

    Optional<Loan> findByExternalId(String externalId);

    List<Loan> findByUserId(UUID userId);

    void delete(Loan loan);
}
