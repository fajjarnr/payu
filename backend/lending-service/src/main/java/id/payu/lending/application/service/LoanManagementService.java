package id.payu.lending.application.service;

import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.RepaymentSchedule;
import id.payu.lending.domain.model.RepaymentStatus;
import id.payu.lending.domain.port.in.LoanManagementUseCase;
import id.payu.lending.domain.port.out.LoanPersistencePort;
import id.payu.lending.domain.port.out.RepaymentSchedulePersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LoanManagementService implements LoanManagementUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoanManagementService.class);

    private final LoanPersistencePort loanPersistencePort;
    private final RepaymentSchedulePersistencePort repaymentSchedulePersistencePort;

    public LoanManagementService(LoanPersistencePort loanPersistencePort, 
                                 RepaymentSchedulePersistencePort repaymentSchedulePersistencePort) {
        this.loanPersistencePort = loanPersistencePort;
        this.repaymentSchedulePersistencePort = repaymentSchedulePersistencePort;
    }

    @Override
    @Transactional
    public List<RepaymentSchedule> createRepaymentSchedule(UUID loanId) {
        log.info("Creating repayment schedule for loan: {}", loanId);

        Loan loan = loanPersistencePort.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        List<RepaymentSchedule> schedules = generateRepaymentSchedule(loan);
        List<RepaymentSchedule> savedSchedules = schedules.stream()
                .map(repaymentSchedulePersistencePort::save)
                .collect(Collectors.toList());

        log.info("Created {} repayment schedules for loan: {}", savedSchedules.size(), loanId);
        return savedSchedules;
    }

    @Override
    public Optional<RepaymentSchedule> getRepaymentSchedule(UUID id) {
        return repaymentSchedulePersistencePort.findById(id);
    }

    @Override
    public List<RepaymentSchedule> getRepaymentScheduleByLoanId(UUID loanId) {
        return repaymentSchedulePersistencePort.findByLoanId(loanId);
    }

    @Override
    public RepaymentSchedule processRepayment(UUID repaymentScheduleId, BigDecimal amount) {
        log.info("Processing repayment for schedule: {} with amount: {}", repaymentScheduleId, amount);

        RepaymentSchedule schedule = repaymentSchedulePersistencePort.findById(repaymentScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Repayment schedule not found: " + repaymentScheduleId));

        if (schedule.getStatus() == RepaymentStatus.FULLY_PAID) {
            throw new IllegalStateException("Repayment already fully paid");
        }

        BigDecimal totalPaid = schedule.getPaidAmount() != null
                ? schedule.getPaidAmount()
                : BigDecimal.ZERO;

        BigDecimal newPaidAmount = totalPaid.add(amount);

        if (newPaidAmount.compareTo(schedule.getInstallmentAmount()) >= 0) {
            schedule.setStatus(RepaymentStatus.FULLY_PAID);
            schedule.setPaidAmount(schedule.getInstallmentAmount());
            schedule.setPaidDate(LocalDate.now());
        } else {
            schedule.setStatus(RepaymentStatus.PARTIALLY_PAID);
            schedule.setPaidAmount(newPaidAmount);
        }

        schedule.setUpdatedAt(LocalDateTime.now());

        RepaymentSchedule savedSchedule = repaymentSchedulePersistencePort.save(schedule);

        log.info("Processed repayment for schedule: {}, new status: {}", repaymentScheduleId, savedSchedule.getStatus());
        return savedSchedule;
    }

    private List<RepaymentSchedule> generateRepaymentSchedule(Loan loan) {
        List<RepaymentSchedule> schedules = new ArrayList<>();

        BigDecimal outstandingPrincipal = loan.getPrincipalAmount();
        BigDecimal monthlyRate = loan.getInterestRate().divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);

        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            BigDecimal interestAmount = outstandingPrincipal
                    .multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalAmount = loan.getMonthlyInstallment().subtract(interestAmount);

            if (i == loan.getTenureMonths()) {
                principalAmount = outstandingPrincipal;
            }

            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setLoanId(loan.getId());
            schedule.setInstallmentNumber(i);
            schedule.setInstallmentAmount(loan.getMonthlyInstallment());
            schedule.setPrincipalAmount(principalAmount);
            schedule.setInterestAmount(interestAmount);
            schedule.setOutstandingPrincipal(outstandingPrincipal);
            schedule.setDueDate(loan.getDisbursementDate().plusMonths(i));
            schedule.setStatus(RepaymentStatus.PENDING);
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setUpdatedAt(LocalDateTime.now());

            schedules.add(schedule);

            outstandingPrincipal = outstandingPrincipal.subtract(principalAmount);
        }

        return schedules;
    }
}
