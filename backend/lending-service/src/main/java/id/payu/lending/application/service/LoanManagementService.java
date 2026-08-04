package id.payu.lending.application.service;

import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.RepaymentPayment;
import id.payu.lending.domain.model.RepaymentPaymentStatus;
import id.payu.lending.domain.model.RepaymentSchedule;
import id.payu.lending.domain.model.RepaymentStatus;
import id.payu.lending.domain.port.in.LoanManagementUseCase;
import id.payu.lending.domain.port.out.LoanEventPublisherPort;
import id.payu.lending.domain.port.out.LoanPersistencePort;
import id.payu.lending.domain.port.out.RepaymentPaymentPersistencePort;
import id.payu.lending.domain.port.out.RepaymentSchedulePersistencePort;
import id.payu.lending.domain.port.out.WalletPaymentPort;
import id.payu.lending.dto.LoanRepaymentProcessedEvent;
import id.payu.lending.exception.RepaymentProcessingException;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.time.Instant;

@Service
public class LoanManagementService implements LoanManagementUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoanManagementService.class);
    private static final String DEFAULT_CURRENCY = "IDR";

    private final LoanPersistencePort loanPersistencePort;
    private final RepaymentSchedulePersistencePort repaymentSchedulePersistencePort;
    private final RepaymentPaymentPersistencePort repaymentPaymentPersistencePort;
    private final WalletPaymentPort walletPaymentPort;
    private final LoanEventPublisherPort loanEventPublisherPort;

    public LoanManagementService(LoanPersistencePort loanPersistencePort, 
                                 RepaymentSchedulePersistencePort repaymentSchedulePersistencePort,
                                 RepaymentPaymentPersistencePort repaymentPaymentPersistencePort,
                                 WalletPaymentPort walletPaymentPort,
                                 LoanEventPublisherPort loanEventPublisherPort) {
        this.loanPersistencePort = loanPersistencePort;
        this.repaymentSchedulePersistencePort = repaymentSchedulePersistencePort;
        this.repaymentPaymentPersistencePort = repaymentPaymentPersistencePort;
        this.walletPaymentPort = walletPaymentPort;
        this.loanEventPublisherPort = loanEventPublisherPort;
    }

    @Override
    @Transactional
    public List<RepaymentSchedule> createRepaymentSchedule(UUID loanId) {
        log.info("Creating repayment schedule for loan: {}", loanId);

        Loan loan = loanPersistencePort.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        List<RepaymentSchedule> existingSchedules = repaymentSchedulePersistencePort.findByLoanId(loanId);
        if (!existingSchedules.isEmpty()) {
            log.info("Repayment schedule already exists for loan: {}", loanId);
            return existingSchedules;
        }

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
    @Transactional(noRollbackFor = RepaymentProcessingException.class)
    public RepaymentSchedule processRepayment(UUID repaymentScheduleId, BigDecimal amount, String idempotencyKey) {
        validateRepaymentInput(repaymentScheduleId, amount, idempotencyKey);
        log.info("Processing repayment for schedule: {} with amount: {}", repaymentScheduleId, amount);

        RepaymentPayment payment = repaymentPaymentPersistencePort.findByIdempotencyKey(idempotencyKey)
                .orElse(null);
        if (payment != null) {
            validateIdempotencyReplay(payment, repaymentScheduleId, amount);
            if (payment.getStatus() == RepaymentPaymentStatus.COMPLETED) {
                return repaymentSchedulePersistencePort.findById(repaymentScheduleId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Repayment schedule not found: " + repaymentScheduleId));
            }
        }

        RepaymentSchedule schedule = repaymentSchedulePersistencePort.findByIdForUpdate(repaymentScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Repayment schedule not found: " + repaymentScheduleId));
        if (schedule.getStatus() == RepaymentStatus.FULLY_PAID) {
            throw new IllegalStateException("Repayment already fully paid");
        }

        BigDecimal totalPaid = schedule.getPaidAmount() != null ? schedule.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = schedule.getInstallmentAmount().subtract(totalPaid);
        if (remaining.signum() <= 0 || amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Repayment amount exceeds remaining installment amount");
        }

        Loan loan = loanPersistencePort.findById(schedule.getLoanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + schedule.getLoanId()));
        if (loan.getUserId() == null) {
            throw new IllegalArgumentException("Loan has no repayment account: " + loan.getId());
        }

        BigDecimal newPaidAmount = totalPaid.add(amount);
        BigDecimal principalApplied = principalApplied(schedule, totalPaid, newPaidAmount);
        BigDecimal interestApplied = interestApplied(schedule, totalPaid, newPaidAmount);
        validateOutstandingBalance(loan, principalApplied);

        if (payment == null) {
            payment = new RepaymentPayment();
            payment.setRepaymentScheduleId(repaymentScheduleId);
            payment.setLoanId(loan.getId());
            payment.setUserId(loan.getUserId());
            payment.setAmount(amount);
            payment.setCurrency(DEFAULT_CURRENCY);
            payment.setIdempotencyKey(idempotencyKey);
            payment.setCreatedAt(LocalDateTime.now());
        }
        payment.setStatus(RepaymentPaymentStatus.PROCESSING);
        payment.setFailureReason(null);
        payment.setUpdatedAt(LocalDateTime.now());
        payment = repaymentPaymentPersistencePort.save(payment);

        final String walletTransactionId;
        try {
            walletTransactionId = walletPaymentPort.collectRepayment(
                    loan.getId(), loan.getUserId(), amount, DEFAULT_CURRENCY, idempotencyKey,
                    "Loan repayment: " + loan.getId());
        } catch (RuntimeException ex) {
            payment.setStatus(RepaymentPaymentStatus.RECONCILIATION_REQUIRED);
            payment.setFailureReason(safeFailureReason(ex));
            payment.setUpdatedAt(LocalDateTime.now());
            repaymentPaymentPersistencePort.save(payment);
            throw new RepaymentProcessingException(
                    "Wallet collection failed for repayment " + repaymentScheduleId, ex);
        }

        if (walletTransactionId == null || walletTransactionId.isBlank()) {
            payment.setStatus(RepaymentPaymentStatus.RECONCILIATION_REQUIRED);
            payment.setFailureReason("Wallet returned no transaction ID");
            payment.setUpdatedAt(LocalDateTime.now());
            repaymentPaymentPersistencePort.save(payment);
            throw new RepaymentProcessingException(
                    "Wallet returned no transaction ID for repayment " + repaymentScheduleId,
                    new IllegalStateException("Wallet returned no transaction ID"));
        }

        schedule.setPaidAmount(newPaidAmount);
        schedule.setStatus(newPaidAmount.compareTo(schedule.getInstallmentAmount()) == 0
                ? RepaymentStatus.FULLY_PAID : RepaymentStatus.PARTIALLY_PAID);
        schedule.setPaidDate(schedule.getStatus() == RepaymentStatus.FULLY_PAID ? LocalDate.now() : null);
        schedule.setUpdatedAt(LocalDateTime.now());

        if (principalApplied.signum() > 0) {
            loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(principalApplied));
            loan.setUpdatedAt(LocalDateTime.now());
            loanPersistencePort.save(loan);
        }

        RepaymentSchedule savedSchedule = repaymentSchedulePersistencePort.save(schedule);
        payment.setWalletTransactionId(walletTransactionId);
        payment.setStatus(RepaymentPaymentStatus.COMPLETED);
        payment.setUpdatedAt(LocalDateTime.now());
        repaymentPaymentPersistencePort.save(payment);
        loanEventPublisherPort.publishRepaymentProcessed(new LoanRepaymentProcessedEvent(
                payment.getId(), repaymentScheduleId, loan.getId(), loan.getUserId(), amount,
                principalApplied, interestApplied, DEFAULT_CURRENCY, idempotencyKey,
                walletTransactionId, Instant.now()));

        log.info("Processed repayment: schedule={}, status={}, walletTransactionId={}",
                repaymentScheduleId, savedSchedule.getStatus(), walletTransactionId);
        return savedSchedule;
    }

    @Scheduled(fixedDelayString = "${lending.repayment.reconciliation-interval-ms:60000}")
    @SchedulerLock(name = "LoanManagementService_reconcileRepayments", lockAtLeastFor = "PT1S", lockAtMostFor = "PT5M")
    @Transactional
    public void reconcileRepayments() {
        List<RepaymentPayment> pending = repaymentPaymentPersistencePort.findByStatusIn(
                List.of(RepaymentPaymentStatus.RECONCILIATION_REQUIRED));
        for (RepaymentPayment payment : pending) {
            try {
                processRepayment(payment.getRepaymentScheduleId(), payment.getAmount(), payment.getIdempotencyKey());
            } catch (Exception ex) {
                log.warn("Repayment reconciliation still pending: repaymentId={}", payment.getId(), ex);
            }
        }
    }

    private void validateRepaymentInput(UUID scheduleId, BigDecimal amount, String idempotencyKey) {
        if (scheduleId == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Repayment schedule and idempotency key are required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Repayment amount must be greater than zero");
        }
        if (amount.scale() > 4) {
            throw new IllegalArgumentException("Repayment amount supports at most 4 decimal places");
        }
    }

    private void validateIdempotencyReplay(RepaymentPayment payment, UUID scheduleId, BigDecimal amount) {
        if (!scheduleId.equals(payment.getRepaymentScheduleId())
                || payment.getAmount() == null
                || payment.getAmount().compareTo(amount) != 0) {
            throw new IllegalArgumentException("Idempotency key was already used for a different repayment");
        }
    }

    private BigDecimal principalApplied(RepaymentSchedule schedule, BigDecimal previousPaid, BigDecimal newPaid) {
        BigDecimal interest = schedule.getInterestAmount() != null ? schedule.getInterestAmount() : BigDecimal.ZERO;
        BigDecimal installment = schedule.getInstallmentAmount();
        BigDecimal previousPrincipalPaid = previousPaid.min(installment).subtract(interest).max(BigDecimal.ZERO);
        BigDecimal newPrincipalPaid = newPaid.min(installment).subtract(interest).max(BigDecimal.ZERO);
        return newPrincipalPaid.subtract(previousPrincipalPaid);
    }

    private BigDecimal interestApplied(RepaymentSchedule schedule, BigDecimal previousPaid, BigDecimal newPaid) {
        BigDecimal interest = schedule.getInterestAmount() != null ? schedule.getInterestAmount() : BigDecimal.ZERO;
        return newPaid.min(interest).subtract(previousPaid.min(interest));
    }

    private void validateOutstandingBalance(Loan loan, BigDecimal principalApplied) {
        if (principalApplied.signum() > 0
                && (loan.getOutstandingBalance() == null
                || loan.getOutstandingBalance().compareTo(principalApplied) < 0)) {
            throw new IllegalStateException("Repayment exceeds loan outstanding principal");
        }
    }

    private String safeFailureReason(RuntimeException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private List<RepaymentSchedule> generateRepaymentSchedule(Loan loan) {
        List<RepaymentSchedule> schedules = new ArrayList<>();

        BigDecimal outstandingPrincipal = loan.getPrincipalAmount();
        BigDecimal monthlyRate = loan.getInterestRate().divide(new BigDecimal("12"), 10, RoundingMode.HALF_EVEN);

        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            BigDecimal interestAmount = outstandingPrincipal
                    .multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_EVEN);

            BigDecimal principalAmount = loan.getMonthlyInstallment().subtract(interestAmount);

            if (i == loan.getTenureMonths()) {
                // BUG-BE-009 fix: Last installment uses remaining principal, not calculated monthly
                principalAmount = outstandingPrincipal;
            }

            // BUG-BE-009 fix: installmentAmount for last month = actual remaining principal + interest
            BigDecimal installmentAmount = (i == loan.getTenureMonths())
                    ? outstandingPrincipal.add(interestAmount)
                    : loan.getMonthlyInstallment();

            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setLoanId(loan.getId());
            schedule.setInstallmentNumber(i);
            schedule.setInstallmentAmount(installmentAmount);
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
