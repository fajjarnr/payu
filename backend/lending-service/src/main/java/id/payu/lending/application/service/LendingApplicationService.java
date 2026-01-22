package id.payu.lending.application.service;

import id.payu.lending.domain.model.CreditScore;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.port.in.ApplyLoanUseCase;
import id.payu.lending.domain.port.in.CreditScoreUseCase;
import id.payu.lending.domain.port.in.GetLoanUseCase;
import id.payu.lending.domain.port.in.PayLaterUseCase;
import id.payu.lending.domain.port.out.CreditScorePersistencePort;
import id.payu.lending.domain.port.out.LoanEventPublisherPort;
import id.payu.lending.domain.port.out.LoanPersistencePort;
import id.payu.lending.domain.port.out.PayLaterPersistencePort;
import id.payu.lending.dto.LoanApplicationRequest;
import id.payu.lending.dto.LoanApprovedEvent;
import id.payu.lending.dto.LoanRejectedEvent;
import id.payu.lending.dto.PayLaterLimitRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class LendingApplicationService implements ApplyLoanUseCase, GetLoanUseCase, PayLaterUseCase, CreditScoreUseCase {

    private final id.payu.lending.adapter.persistence.LoanPersistenceAdapter loanPersistenceAdapter;
    private final id.payu.lending.adapter.persistence.PayLaterPersistenceAdapter payLaterPersistenceAdapter;
    private final id.payu.lending.adapter.persistence.CreditScorePersistenceAdapter creditScorePersistenceAdapter;
    private final id.payu.lending.adapter.messaging.KafkaLoanEventPublisherAdapter loanEventPublisherPort;

    @Override
    @Transactional
    @CircuitBreaker(name = "creditScoring", fallbackMethod = "applyLoanFallback")
    @Retry(name = "creditScoring")
    @TimeLimiter(name = "creditScoring")
    public CompletableFuture<Loan> applyLoan(LoanApplicationRequest command) {
        log.info("Processing loan application for user: {}", command.userId());

        CreditScore creditScore = creditScorePersistenceAdapter.findByUserId(command.userId())
                .orElseGet(() -> calculateCreditScore(command.userId()));

        if (!isEligibleForLoan(creditScore.getScore(), command.principalAmount())) {
            Loan rejectedLoan = new Loan();
            rejectedLoan.setExternalId(command.externalId());
            rejectedLoan.setUserId(command.userId());
            rejectedLoan.setType(command.loanType());
            rejectedLoan.setPrincipalAmount(command.principalAmount());
            rejectedLoan.setTenureMonths(command.tenureMonths());
            rejectedLoan.setPurpose(command.purpose());
            rejectedLoan.setStatus(Loan.LoanStatus.REJECTED);
            rejectedLoan.setCreatedAt(LocalDateTime.now());
            rejectedLoan.setUpdatedAt(LocalDateTime.now());

            loanPersistenceAdapter.save(rejectedLoan);

            loanEventPublisherPort.publishLoanRejected(new LoanRejectedEvent(
                    rejectedLoan.getId(),
                    rejectedLoan.getUserId(),
                    rejectedLoan.getExternalId(),
                    rejectedLoan.getPrincipalAmount(),
                    "Credit score or loan amount does not meet eligibility criteria"
            ));

            log.info("Loan application rejected for user: {}", command.userId());
            return CompletableFuture.completedFuture(rejectedLoan);
        }

        BigDecimal interestRate = calculateInterestRate(creditScore.getScore());
        BigDecimal monthlyInstallment = calculateMonthlyInstallment(
                command.principalAmount(), interestRate, command.tenureMonths()
        );

        Loan loan = new Loan();
        loan.setExternalId(command.externalId());
        loan.setUserId(command.userId());
        loan.setType(command.loanType());
        loan.setPrincipalAmount(command.principalAmount());
        loan.setInterestRate(interestRate);
        loan.setTenureMonths(command.tenureMonths());
        loan.setMonthlyInstallment(monthlyInstallment);
        loan.setOutstandingBalance(command.principalAmount());
        loan.setStatus(Loan.LoanStatus.APPROVED);
        loan.setPurpose(command.purpose());
        loan.setDisbursementDate(LocalDate.now());
        loan.setMaturityDate(LocalDate.now().plusMonths(command.tenureMonths()));
        loan.setCreatedAt(LocalDateTime.now());
        loan.setUpdatedAt(LocalDateTime.now());

        Loan savedLoan = loanPersistenceAdapter.save(loan);

        loanEventPublisherPort.publishLoanApproved(new LoanApprovedEvent(
                savedLoan.getId(),
                savedLoan.getUserId(),
                savedLoan.getExternalId(),
                savedLoan.getPrincipalAmount(),
                savedLoan.getInterestRate(),
                savedLoan.getTenureMonths(),
                savedLoan.getMonthlyInstallment(),
                savedLoan.getDisbursementDate()
        ));

        log.info("Loan approved for user: {} with amount: {}", command.userId(), command.principalAmount());
        return CompletableFuture.completedFuture(savedLoan);
    }

    public CompletableFuture<Loan> applyLoanFallback(LoanApplicationRequest command, Throwable t) {
        log.error("Credit scoring service unavailable, falling back to conservative approval. Error: {}", t.getMessage());

        BigDecimal conservativeInterestRate = new BigDecimal("0.18");
        BigDecimal monthlyInstallment = calculateMonthlyInstallment(
                command.principalAmount(), conservativeInterestRate, command.tenureMonths()
        );

        if (command.principalAmount().compareTo(new BigDecimal("5000000")) > 0) {
            Loan rejectedLoan = new Loan();
            rejectedLoan.setExternalId(command.externalId());
            rejectedLoan.setUserId(command.userId());
            rejectedLoan.setType(command.loanType());
            rejectedLoan.setPrincipalAmount(command.principalAmount());
            rejectedLoan.setTenureMonths(command.tenureMonths());
            rejectedLoan.setPurpose(command.purpose());
            rejectedLoan.setStatus(Loan.LoanStatus.REJECTED);
            rejectedLoan.setCreatedAt(LocalDateTime.now());
            rejectedLoan.setUpdatedAt(LocalDateTime.now());

            return CompletableFuture.completedFuture(loanPersistenceAdapter.save(rejectedLoan));
        }

        Loan loan = new Loan();
        loan.setExternalId(command.externalId());
        loan.setUserId(command.userId());
        loan.setType(command.loanType());
        loan.setPrincipalAmount(command.principalAmount());
        loan.setInterestRate(conservativeInterestRate);
        loan.setTenureMonths(command.tenureMonths());
        loan.setMonthlyInstallment(monthlyInstallment);
        loan.setOutstandingBalance(command.principalAmount());
        loan.setStatus(Loan.LoanStatus.PENDING_APPROVAL);
        loan.setPurpose(command.purpose());
        loan.setCreatedAt(LocalDateTime.now());
        loan.setUpdatedAt(LocalDateTime.now());

        return CompletableFuture.completedFuture(loanPersistenceAdapter.save(loan));
    }

    @Override
    public Optional<Loan> getLoanById(UUID loanId) {
        return loanPersistenceAdapter.findById(loanId);
    }

    @Override
    @Transactional
    public id.payu.lending.domain.model.PayLater activatePayLater(UUID userId, PayLaterLimitRequest request) {
        log.info("Activating PayLater for user: {}", userId);

        Optional<id.payu.lending.domain.model.PayLater> existingPayLater = payLaterPersistenceAdapter.findByUserId(userId);
        if (existingPayLater.isPresent()) {
            log.info("PayLater already active for user: {}", userId);
            return existingPayLater.get();
        }

        id.payu.lending.domain.model.PayLater payLater = new id.payu.lending.domain.model.PayLater();
        payLater.setUserId(userId);
        payLater.setCreditLimit(request.creditLimit());
        payLater.setUsedCredit(BigDecimal.ZERO);
        payLater.setAvailableCredit(request.creditLimit());
        payLater.setStatus(id.payu.lending.domain.model.PayLater.PayLaterStatus.ACTIVE);
        payLater.setBillingCycleDay(request.billingCycleDay() != null ? request.billingCycleDay() : 1);
        payLater.setInterestRate(new BigDecimal("0.025"));
        payLater.setCreatedAt(LocalDateTime.now());
        payLater.setUpdatedAt(LocalDateTime.now());

        return payLaterPersistenceAdapter.save(payLater);
    }

    @Override
    public Optional<id.payu.lending.domain.model.PayLater> getPayLaterByUserId(UUID userId) {
        return payLaterPersistenceAdapter.findByUserId(userId);
    }

    @Override
    @Transactional
    public CreditScore calculateCreditScore(UUID userId) {
        log.info("Calculating credit score for user: {}", userId);

        BigDecimal score = calculateDefaultScore(userId);

        CreditScore creditScore = new CreditScore();
        creditScore.setUserId(userId);
        creditScore.setScore(score);
        creditScore.setRiskCategory(determineRiskCategory(score));
        creditScore.setLastCalculatedAt(LocalDateTime.now());
        creditScore.setCreatedAt(LocalDateTime.now());
        creditScore.setUpdatedAt(LocalDateTime.now());

        return creditScorePersistenceAdapter.save(creditScore);
    }

    @Override
    public Optional<CreditScore> getCreditScoreByUserId(UUID userId) {
        return creditScorePersistenceAdapter.findByUserId(userId);
    }

    private BigDecimal calculateDefaultScore(UUID userId) {
        return new BigDecimal("700");
    }

    private boolean isEligibleForLoan(BigDecimal creditScore, BigDecimal loanAmount) {
        BigDecimal minimumScore = new BigDecimal("600");
        BigDecimal maxLoanAmount = new BigDecimal("50000000");
        
        return creditScore.compareTo(minimumScore) >= 0 && 
               loanAmount.compareTo(maxLoanAmount) <= 0;
    }

    private BigDecimal calculateInterestRate(BigDecimal creditScore) {
        if (creditScore.compareTo(new BigDecimal("750")) >= 0) {
            return new BigDecimal("0.12");
        } else if (creditScore.compareTo(new BigDecimal("700")) >= 0) {
            return new BigDecimal("0.14");
        } else if (creditScore.compareTo(new BigDecimal("650")) >= 0) {
            return new BigDecimal("0.16");
        } else {
            return new BigDecimal("0.18");
        }
    }

    private BigDecimal calculateMonthlyInstallment(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 6, RoundingMode.HALF_UP);
        
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP);
        }
        
        BigDecimal numerator = monthlyRate.multiply(principal);
        BigDecimal denominator = BigDecimal.ONE.subtract(
                BigDecimal.ONE.add(monthlyRate).pow(-months, java.math.MathContext.DECIMAL128)
        );
        
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private CreditScore.RiskCategory determineRiskCategory(BigDecimal score) {
        if (score.compareTo(new BigDecimal("750")) >= 0) {
            return CreditScore.RiskCategory.EXCELLENT;
        } else if (score.compareTo(new BigDecimal("700")) >= 0) {
            return CreditScore.RiskCategory.GOOD;
        } else if (score.compareTo(new BigDecimal("650")) >= 0) {
            return CreditScore.RiskCategory.FAIR;
        } else if (score.compareTo(new BigDecimal("600")) >= 0) {
            return CreditScore.RiskCategory.POOR;
        } else {
            return CreditScore.RiskCategory.VERY_POOR;
        }
    }
}
