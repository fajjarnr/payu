package id.payu.lending.domain.port.in;

import id.payu.lending.domain.model.Loan;
import java.util.Optional;
import java.util.UUID;

public interface GetLoanUseCase {
    Optional<Loan> getLoanById(UUID loanId);
}
