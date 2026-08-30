package id.payu.lending.application.service;

import id.payu.lending.domain.model.InstallmentCheckout;
import id.payu.lending.domain.model.CheckoutStatus;
import id.payu.lending.domain.model.InstallmentOption;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.LoanStatus;
import id.payu.lending.domain.model.LoanType;
import id.payu.lending.domain.model.PayLater;
import id.payu.lending.domain.model.PayLaterStatus;
import id.payu.lending.domain.model.RepaymentSchedule;
import id.payu.lending.domain.model.RepaymentStatus;
import id.payu.lending.domain.port.in.InstallmentUseCase;
import id.payu.lending.domain.port.out.InstallmentCheckoutPersistencePort;
import id.payu.lending.domain.port.out.LoanPersistencePort;
import id.payu.lending.domain.port.out.PayLaterPersistencePort;
import id.payu.lending.domain.port.out.RepaymentSchedulePersistencePort;
import id.payu.lending.exception.InstallmentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application service for installment checkout via PayLater (GAP-012).
 * <p>
 * Provides:
 * <ul>
 *   <li>Tenor options — simulated monthly payment for 3x/6x/12x</li>
 *   <li>Checkout — creates INSTALMENT_LOAN backed by PayLater credit + repayment schedule</li>
 *   <li>Checkout queries — by ID and by user</li>
 * </ul>
 */
@Service
public class InstallmentService implements InstallmentUseCase {

    private static final Logger log = LoggerFactory.getLogger(InstallmentService.class);

    private static final int[] DEFAULT_TENORS = {3, 6, 12};
    private static final BigDecimal DEFAULT_ANNUAL_RATE = new BigDecimal("0.1200"); // 12% p.a.

    private final PayLaterPersistencePort payLaterPersistencePort;
    private final LoanPersistencePort loanPersistencePort;
    private final RepaymentSchedulePersistencePort repaymentSchedulePersistencePort;
    private final InstallmentCheckoutPersistencePort checkoutPersistencePort;

    public InstallmentService(PayLaterPersistencePort payLaterPersistencePort,
                               LoanPersistencePort loanPersistencePort,
                               RepaymentSchedulePersistencePort repaymentSchedulePersistencePort,
                               InstallmentCheckoutPersistencePort checkoutPersistencePort) {
        this.payLaterPersistencePort = payLaterPersistencePort;
        this.loanPersistencePort = loanPersistencePort;
        this.repaymentSchedulePersistencePort = repaymentSchedulePersistencePort;
        this.checkoutPersistencePort = checkoutPersistencePort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentOption> getTenorOptions(UUID userId, BigDecimal amount) {
        log.info("Getting tenor options: userId={}", userId);

        // Validate PayLater eligibility
        PayLater payLater = getActivePayLater(userId);
        BigDecimal available = payLater.getAvailableCredit();

        if (available.compareTo(amount) < 0) {
            throw new InstallmentException("INST_001",
                    "Insufficient PayLater credit. Available: " + available + ", Requested: " + amount);
        }

        BigDecimal annualRate = payLater.getInterestRate() != null
                ? payLater.getInterestRate()
                : DEFAULT_ANNUAL_RATE;

        List<InstallmentOption> options = new ArrayList<>();
        for (int tenor : DEFAULT_TENORS) {
            if (amount.compareTo(BigDecimal.valueOf(tenor * 10000L)) >= 0) {
                // Only offer tenor if monthly payment would be >= 10k
                InstallmentOption option = calculateOption(amount, tenor, annualRate);
                options.add(option);
            }
        }

        log.info("Returning {} tenor options for userId={}", options.size(), userId);
        return options;
    }

    @Override
    @Transactional
    public InstallmentCheckout checkout(UUID userId, String partnerId, String externalOrderId,
                                         BigDecimal amount, int tenor) {
        log.info("Installment checkout: userId={}, partner={}, tenor={}x",
                userId, partnerId, tenor);

        // 1. Validate PayLater account is active with sufficient credit
        PayLater payLater = getActivePayLater(userId);
        BigDecimal available = payLater.getAvailableCredit();

        if (available.compareTo(amount) < 0) {
            throw new InstallmentException("INST_002",
                    "Insufficient PayLater credit for checkout");
        }

        // 2. Validate tenor
        if (tenor < 1 || tenor > 12) {
            throw new InstallmentException("INST_003",
                    "Invalid tenor: " + tenor + ". Must be between 1 and 12 months");
        }

        // 3. Check for duplicate external order
        if (externalOrderId != null) {
            checkoutPersistencePort.findByExternalOrderId(externalOrderId).ifPresent(existing -> {
                throw new InstallmentException("INST_004",
                        "Duplicate checkout for order: " + externalOrderId);
            });
        }

        // 4. Calculate installment
        BigDecimal annualRate = payLater.getInterestRate() != null
                ? payLater.getInterestRate()
                : DEFAULT_ANNUAL_RATE;
        InstallmentOption option = calculateOption(amount, tenor, annualRate);

        // 5. Create the INSTALMENT_LOAN
        Loan loan = new Loan();
        loan.setExternalId("INST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        loan.setUserId(userId);
        loan.setType(LoanType.INSTALMENT_LOAN);
        loan.setPrincipalAmount(amount);
        loan.setInterestRate(annualRate);
        loan.setTenureMonths(tenor);
        loan.setMonthlyInstallment(option.getMonthlyPayment());
        loan.setOutstandingBalance(option.getTotalPayment());
        loan.setStatus(LoanStatus.DISBURSED);
        loan.setPurpose("PayLater installment - " + partnerId);
        loan.setDisbursementDate(LocalDate.now());
        loan.setMaturityDate(LocalDate.now().plusMonths(tenor));
        loan.setCreatedAt(LocalDateTime.now());
        loan.setUpdatedAt(LocalDateTime.now());

        Loan savedLoan = loanPersistencePort.save(loan);
        log.info("Created INSTALMENT_LOAN: id={}, externalId={}", savedLoan.getId(), savedLoan.getExternalId());

        // 6. Generate repayment schedule
        generateRepaymentSchedule(savedLoan, option.getMonthlyPayment(), annualRate);

        // 7. Debit PayLater credit
        payLater.setUsedCredit(payLater.getUsedCredit().add(amount));
        payLater.setAvailableCredit(payLater.getAvailableCredit().subtract(amount));
        payLater.setUpdatedAt(LocalDateTime.now());
        payLaterPersistencePort.save(payLater);

        // 8. Create checkout record
        InstallmentCheckout checkout = new InstallmentCheckout();
        checkout.setUserId(userId);
        checkout.setPayLaterId(payLater.getId());
        checkout.setLoanId(savedLoan.getId());
        checkout.setPartnerId(partnerId);
        checkout.setExternalOrderId(externalOrderId);
        checkout.setPurchaseAmount(amount);
        checkout.setCurrency("IDR");
        checkout.setTenor(tenor);
        checkout.setMonthlyPayment(option.getMonthlyPayment());
        checkout.setInterestRate(annualRate);
        checkout.setStatus(CheckoutStatus.DISBURSED);
        checkout.setCreatedAt(LocalDateTime.now());
        checkout.setUpdatedAt(LocalDateTime.now());

        InstallmentCheckout saved = checkoutPersistencePort.save(checkout);
        log.info("Installment checkout completed: id={}, loanId={}", saved.getId(), savedLoan.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public InstallmentCheckout getCheckout(UUID checkoutId) {
        return checkoutPersistencePort.findById(checkoutId)
                .orElseThrow(() -> new InstallmentException("INST_005",
                        "Installment checkout not found: " + checkoutId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentCheckout> getCheckoutsByUser(UUID userId) {
        return checkoutPersistencePort.findByUserId(userId);
    }

    // ═══════════════════════════════════════════════════════
    //  Internal Helpers
    // ═══════════════════════════════════════════════════════

    private PayLater getActivePayLater(UUID userId) {
        PayLater payLater = payLaterPersistencePort.findByUserId(userId)
                .orElseThrow(() -> new InstallmentException("INST_006",
                        "No PayLater account found for user: " + userId));

        if (payLater.getStatus() != PayLaterStatus.ACTIVE) {
            throw new InstallmentException("INST_007",
                    "PayLater account is not active. Status: " + payLater.getStatus());
        }
        return payLater;
    }

    /**
     * Calculate installment option using flat interest method.
     * monthlyInterest = principal * annualRate / 12
     * monthlyPrincipal = principal / tenor
     * monthlyPayment = monthlyPrincipal + monthlyInterest
     */
    private InstallmentOption calculateOption(BigDecimal principal, int tenor, BigDecimal annualRate) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_EVEN);
        BigDecimal monthlyInterest = principal.multiply(monthlyRate).setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(tenor), 4, RoundingMode.HALF_EVEN);
        BigDecimal monthlyPayment = monthlyPrincipal.add(monthlyInterest);
        BigDecimal totalInterest = monthlyInterest.multiply(BigDecimal.valueOf(tenor)).setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal totalPayment = principal.add(totalInterest);

        return new InstallmentOption(tenor, monthlyPayment, totalPayment, totalInterest, annualRate);
    }

    private void generateRepaymentSchedule(Loan loan, BigDecimal monthlyPayment, BigDecimal annualRate) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_EVEN);
        BigDecimal outstanding = loan.getPrincipalAmount();
        int tenor = loan.getTenureMonths();
        // LEND-REPAY-001: principal must use the same money scale (4) as
        // calculateOption so option figures match the persisted schedule.
        BigDecimal monthlyPrincipal = loan.getPrincipalAmount()
                .divide(BigDecimal.valueOf(tenor), 4, RoundingMode.HALF_EVEN);

        for (int i = 1; i <= tenor; i++) {
            BigDecimal interest = loan.getPrincipalAmount().multiply(monthlyRate)
                    .setScale(4, RoundingMode.HALF_EVEN);

            // LEND-REPAY-001: the last installment absorbs the rounding
            // residual so sum(principal) == loan principal exactly (no
            // permanently unamortized balance). Clamp intermediate ones too.
            boolean isLast = (i == tenor);
            BigDecimal principalAmount = outstanding.min(monthlyPrincipal);
            if (isLast) {
                principalAmount = outstanding;
            }
            outstanding = outstanding.subtract(principalAmount);

            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setLoanId(loan.getId());
            schedule.setInstallmentNumber(i);
            schedule.setInstallmentAmount(monthlyPayment);
            schedule.setPrincipalAmount(principalAmount);
            schedule.setInterestAmount(interest);
            schedule.setOutstandingPrincipal(outstanding);
            schedule.setDueDate(LocalDate.now().plusMonths(i));
            schedule.setStatus(RepaymentStatus.PENDING);
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setUpdatedAt(LocalDateTime.now());

            repaymentSchedulePersistencePort.save(schedule);
        }
        log.info("Generated {} repayment schedules for loan {}", loan.getTenureMonths(), loan.getId());
    }
}
