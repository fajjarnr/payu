package id.payu.lending.domain.port.out;

import id.payu.lending.domain.model.RepaymentSchedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepaymentSchedulePersistencePort {
    RepaymentSchedule save(RepaymentSchedule repaymentSchedule);
    Optional<RepaymentSchedule> findById(UUID id);
    List<RepaymentSchedule> findByLoanId(UUID loanId);
    void deleteByLoanId(UUID loanId);
}
