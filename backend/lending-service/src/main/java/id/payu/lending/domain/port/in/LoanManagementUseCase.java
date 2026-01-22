package id.payu.lending.domain.port.in;

import id.payu.lending.domain.model.RepaymentSchedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanManagementUseCase {
    List<RepaymentSchedule> createRepaymentSchedule(UUID loanId);
    Optional<RepaymentSchedule> getRepaymentSchedule(UUID id);
    List<RepaymentSchedule> getRepaymentScheduleByLoanId(UUID loanId);
    RepaymentSchedule processRepayment(UUID repaymentScheduleId, java.math.BigDecimal amount);
}
