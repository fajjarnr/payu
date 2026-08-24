package id.payu.lending.application.service;

import id.payu.lending.domain.model.CreditScore;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.LoanPreApproval;
import id.payu.lending.domain.port.in.LoanPreApprovalUseCase;
import id.payu.lending.domain.port.out.CreditScorePersistencePort;
import id.payu.lending.domain.port.out.LoanPreApprovalPersistencePort;
import id.payu.lending.interfaces.dto.LoanPreApprovalRequest;
import id.payu.lending.interfaces.dto.LoanPreApprovalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import id.payu.lending.domain.model.PreApprovalStatus;
import id.payu.lending.domain.model.RiskCategory;

@Service
public class LoanPreApprovalService implements LoanPreApprovalUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoanPreApprovalService.class);

    private final CreditScorePersistencePort creditScorePersistencePort;
    private final LoanPreApprovalPersistencePort preApprovalPersistencePort;
    private final EnhancedCreditScoringService enhancedCreditScoringService;
    private final LendingDmnService lendingDmnService;

    public LoanPreApprovalService(CreditScorePersistencePort creditScorePersistencePort, 
                                   LoanPreApprovalPersistencePort preApprovalPersistencePort, 
                                   EnhancedCreditScoringService enhancedCreditScoringService,
                                   LendingDmnService lendingDmnService) {
        this.creditScorePersistencePort = creditScorePersistencePort;
        this.preApprovalPersistencePort = preApprovalPersistencePort;
        this.enhancedCreditScoringService = enhancedCreditScoringService;
        this.lendingDmnService = lendingDmnService;
    }

    private static final int PRE_APPROVAL_VALIDITY_DAYS = 30;

    @Override
    public LoanPreApprovalResponse checkPreApproval(LoanPreApprovalRequest request) {
        log.info("Checking loan pre-approval for user: {}", request.userId());

        BigDecimal creditScore = getCreditScore(request.userId());

        PreApprovalDecision decision = evaluateEligibility(creditScore, request);

        LoanPreApproval preApproval = createPreApproval(request, creditScore, decision);

        LoanPreApproval savedPreApproval = preApprovalPersistencePort.save(preApproval);

        log.info("Loan pre-approval completed for user: {} with status: {}",
                request.userId(), decision.status);

        return mapToResponse(savedPreApproval);
    }

    @Override
    public Optional<LoanPreApproval> getPreApprovalById(UUID preApprovalId) {
        log.info("Fetching pre-approval by ID: {}", preApprovalId);
        return preApprovalPersistencePort.findById(preApprovalId);
    }

    @Override
    public Optional<LoanPreApproval> getActivePreApprovalByUserId(UUID userId) {
        log.info("Fetching active pre-approval for user: {}", userId);
        return preApprovalPersistencePort.findActiveByUserId(userId);
    }

    private BigDecimal getCreditScore(UUID userId) {
        Optional<CreditScore> existingScore = creditScorePersistencePort.findByUserId(userId);

        if (existingScore.isPresent()) {
            CreditScore score = existingScore.get();
            BigDecimal enhancedScore = enhancedCreditScoringService.calculateEnhancedCreditScore(
                    userId, score.getScore());
            score.setScore(enhancedScore);
            return enhancedScore;
        }

        BigDecimal baseScore = new BigDecimal("700");
        return enhancedCreditScoringService.calculateEnhancedCreditScore(userId, baseScore);
    }

    private PreApprovalDecision evaluateEligibility(BigDecimal creditScore, LoanPreApprovalRequest request) {
        LendingDmnService.EligibilityOutput out = lendingDmnService.evaluateEligibility(
                creditScore, request.principalAmount(), request.tenureMonths());
        PreApprovalDecision decision = new PreApprovalDecision();
        decision.status = out.status();
        decision.maxApprovedAmount = out.maxApprovedAmount();
        decision.maxTenureMonths = out.maxTenureMonths();
        decision.reason = out.reason();
        decision.riskCategory = out.riskCategory();
        // Pricing via DMN pricing table; REJECTED uses ZERO rate per previous contract
        decision.minInterestRate = out.status() == PreApprovalStatus.REJECTED ? BigDecimal.ZERO : out.interestRate();
        decision.estimatedMonthlyPayment = calculateMonthlyInstallment(
                decision.maxApprovedAmount,
                decision.minInterestRate,
                decision.maxTenureMonths
        );
        return decision;
    }
    private LoanPreApproval createPreApproval(LoanPreApprovalRequest request,
                                               BigDecimal creditScore,
                                               PreApprovalDecision decision) {
        LoanPreApproval preApproval = new LoanPreApproval();
        preApproval.setUserId(request.userId());
        preApproval.setLoanType(request.loanType());
        preApproval.setRequestedAmount(request.principalAmount());
        preApproval.setMaxApprovedAmount(decision.maxApprovedAmount);
        preApproval.setMinInterestRate(decision.minInterestRate);
        preApproval.setMaxTenureMonths(decision.maxTenureMonths);
        preApproval.setEstimatedMonthlyPayment(decision.estimatedMonthlyPayment);
        preApproval.setStatus(decision.status);
        preApproval.setCreditScore(creditScore);
        preApproval.setRiskCategory(decision.riskCategory);
        preApproval.setReason(decision.reason);
        preApproval.setValidUntil(LocalDate.now().plusDays(PRE_APPROVAL_VALIDITY_DAYS));
        preApproval.setCreatedAt(LocalDateTime.now());
        preApproval.setUpdatedAt(LocalDateTime.now());

        return preApproval;
    }

    private BigDecimal calculateMonthlyInstallment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (principal.compareTo(BigDecimal.ZERO) == 0 || months == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 6, RoundingMode.HALF_EVEN);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(months), 2, RoundingMode.HALF_EVEN);
        }

        BigDecimal numerator = monthlyRate.multiply(principal);
        BigDecimal denominator = BigDecimal.ONE.subtract(
                BigDecimal.ONE.add(monthlyRate).pow(-months, java.math.MathContext.DECIMAL128)
        );

        return numerator.divide(denominator, 2, RoundingMode.HALF_EVEN);
    }

    private LoanPreApprovalResponse mapToResponse(LoanPreApproval preApproval) {
        return new LoanPreApprovalResponse(
                preApproval.getId(),
                preApproval.getUserId(),
                preApproval.getLoanType(),
                preApproval.getRequestedAmount(),
                preApproval.getMaxApprovedAmount(),
                preApproval.getMinInterestRate(),
                preApproval.getMaxTenureMonths(),
                preApproval.getEstimatedMonthlyPayment(),
                preApproval.getStatus(),
                preApproval.getCreditScore(),
                preApproval.getRiskCategory(),
                preApproval.getReason(),
                preApproval.getValidUntil().atStartOfDay(),
                preApproval.getCreatedAt()
        );
    }

    private static class PreApprovalDecision {
        PreApprovalStatus status;
        BigDecimal maxApprovedAmount;
        BigDecimal minInterestRate;
        Integer maxTenureMonths;
        BigDecimal estimatedMonthlyPayment;
        String reason;
        RiskCategory riskCategory;
    }
}
