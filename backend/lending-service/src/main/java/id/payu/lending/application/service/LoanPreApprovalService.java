package id.payu.lending.application.service;

import id.payu.lending.domain.model.CreditScore;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.LoanPreApproval;
import id.payu.lending.domain.port.in.LoanPreApprovalUseCase;
import id.payu.lending.domain.port.out.CreditScorePersistencePort;
import id.payu.lending.domain.port.out.LoanPreApprovalPersistencePort;
import id.payu.lending.dto.LoanPreApprovalRequest;
import id.payu.lending.dto.LoanPreApprovalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanPreApprovalService implements LoanPreApprovalUseCase {

    private final CreditScorePersistencePort creditScorePersistencePort;
    private final LoanPreApprovalPersistencePort preApprovalPersistencePort;
    private final EnhancedCreditScoringService enhancedCreditScoringService;

    private static final BigDecimal MIN_CREDIT_SCORE_FOR_APPROVAL = new BigDecimal("650");
    private static final BigDecimal MIN_CREDIT_SCORE_FOR_CONDITIONAL = new BigDecimal("600");
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
        PreApprovalDecision decision = new PreApprovalDecision();

        if (creditScore.compareTo(MIN_CREDIT_SCORE_FOR_APPROVAL) >= 0) {
            decision.status = LoanPreApprovalResponse.PreApprovalStatus.APPROVED;
            decision.maxApprovedAmount = request.principalAmount();
            decision.minInterestRate = calculateInterestRate(creditScore);
            decision.maxTenureMonths = request.tenureMonths();
            decision.reason = null;
        } else if (creditScore.compareTo(MIN_CREDIT_SCORE_FOR_CONDITIONAL) >= 0) {
            decision.status = LoanPreApprovalResponse.PreApprovalStatus.CONDITIONALLY_APPROVED;
            decision.maxApprovedAmount = calculateConditionalAmount(creditScore, request.principalAmount());
            decision.minInterestRate = calculateInterestRate(creditScore);
            decision.maxTenureMonths = Math.min(request.tenureMonths(), 24);
            decision.reason = "Conditional approval: Higher interest rate and lower loan amount may apply";
        } else {
            decision.status = LoanPreApprovalResponse.PreApprovalStatus.REJECTED;
            decision.maxApprovedAmount = BigDecimal.ZERO;
            decision.minInterestRate = BigDecimal.ZERO;
            decision.maxTenureMonths = 0;
            decision.reason = "Credit score does not meet minimum requirements for loan approval";
        }

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
        preApproval.setStatus(convertStatus(decision.status));
        preApproval.setCreditScore(creditScore);
        preApproval.setRiskCategory(determineRiskCategory(creditScore));
        preApproval.setReason(decision.reason);
        preApproval.setValidUntil(LocalDate.now().plusDays(PRE_APPROVAL_VALIDITY_DAYS));
        preApproval.setCreatedAt(LocalDateTime.now());
        preApproval.setUpdatedAt(LocalDateTime.now());

        return preApproval;
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

    private BigDecimal calculateConditionalAmount(BigDecimal creditScore, BigDecimal requestedAmount) {
        if (creditScore.compareTo(new BigDecimal("640")) >= 0) {
            return requestedAmount.multiply(new BigDecimal("0.85"));
        } else {
            return requestedAmount.multiply(new BigDecimal("0.70"));
        }
    }

    private BigDecimal calculateMonthlyInstallment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (principal.compareTo(BigDecimal.ZERO) == 0 || months == 0) {
            return BigDecimal.ZERO;
        }

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

    private LoanPreApproval.PreApprovalStatus convertStatus(LoanPreApprovalResponse.PreApprovalStatus responseStatus) {
        return LoanPreApproval.PreApprovalStatus.valueOf(responseStatus.name());
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
                LoanPreApprovalResponse.PreApprovalStatus.valueOf(preApproval.getStatus().name()),
                preApproval.getCreditScore(),
                preApproval.getRiskCategory(),
                preApproval.getReason(),
                preApproval.getValidUntil().atStartOfDay(),
                preApproval.getCreatedAt()
        );
    }

    private static class PreApprovalDecision {
        LoanPreApprovalResponse.PreApprovalStatus status;
        BigDecimal maxApprovedAmount;
        BigDecimal minInterestRate;
        Integer maxTenureMonths;
        BigDecimal estimatedMonthlyPayment;
        String reason;
    }
}
