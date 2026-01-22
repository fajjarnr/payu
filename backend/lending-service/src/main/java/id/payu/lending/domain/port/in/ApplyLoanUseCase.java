package id.payu.lending.domain.port.in;

import id.payu.lending.domain.model.Loan;
import id.payu.lending.dto.LoanApplicationRequest;
import java.util.concurrent.CompletableFuture;

public interface ApplyLoanUseCase {
    CompletableFuture<Loan> applyLoan(LoanApplicationRequest command);
}
