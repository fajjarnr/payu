package id.payu.lending.application.service;

import id.payu.lending.domain.model.PreApprovalStatus;
import id.payu.lending.domain.model.RiskCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DMN-backed pricing and eligibility evaluation.
 * Decision tables are versioned in {@code src/main/resources/rules/dmn/pricing.dmn}
 * and {@code eligibility.dmn} (DMN 1.4, namespace http://payu.id/dmn).
 * This service mirrors those tables in Java for classpath-first execution
 * (no KIE DMN runtime required Phase 2); DMN files remain the auditable
 * source of truth and future KieScanner hot-reload target.
 * ADR-0048: DMN tables readable by risk/compliance, Java stays for math.
 */
@Service
public class LendingDmnService {

    private static final Logger log = LoggerFactory.getLogger(LendingDmnService.class);

    // pricing.dmn rows
    // ≥750→12%/EXCELLENT, 700-749→14%/GOOD, 650-699→16%/FAIR, 600-649→18%/POOR, <600→18%/VERY_POOR
    public PricingOutput evaluatePricing(BigDecimal creditScore) {
        if (creditScore == null) creditScore = BigDecimal.ZERO;
        BigDecimal rate;
        RiskCategory risk;
        if (creditScore.compareTo(new BigDecimal("750")) >= 0) {
            rate = new BigDecimal("0.12");
            risk = RiskCategory.EXCELLENT;
        } else if (creditScore.compareTo(new BigDecimal("700")) >= 0) {
            rate = new BigDecimal("0.14");
            risk = RiskCategory.GOOD;
        } else if (creditScore.compareTo(new BigDecimal("650")) >= 0) {
            rate = new BigDecimal("0.16");
            risk = RiskCategory.FAIR;
        } else if (creditScore.compareTo(new BigDecimal("600")) >= 0) {
            rate = new BigDecimal("0.18");
            risk = RiskCategory.POOR;
        } else {
            rate = new BigDecimal("0.18");
            risk = RiskCategory.VERY_POOR;
        }
        log.debug("DMN Pricing evaluated: score={} -> rate={}, risk={}", creditScore, rate, risk);
        return new PricingOutput(rate, risk);
    }

    // eligibility.dmn rows + invocation pricing(creditScore)
    // ≥650 APPROVED (requestedAmount, requestedTenure)
    // 600-649 CONDITIONALLY_APPROVED (640→0.85 else 0.70, min(tenure,24))
    // <600 REJECTED (0,0)
    public EligibilityOutput evaluateEligibility(BigDecimal creditScore, BigDecimal requestedAmount, int requestedTenure) {
        if (creditScore == null) creditScore = BigDecimal.ZERO;
        if (requestedAmount == null) requestedAmount = BigDecimal.ZERO;
        PricingOutput pricing = evaluatePricing(creditScore);
        PreApprovalStatus status;
        BigDecimal maxAmount;
        int maxTenure;
        String reason;
        if (creditScore.compareTo(new BigDecimal("650")) >= 0) {
            status = PreApprovalStatus.APPROVED;
            maxAmount = requestedAmount;
            maxTenure = requestedTenure;
            reason = null;
        } else if (creditScore.compareTo(new BigDecimal("600")) >= 0) {
            status = PreApprovalStatus.CONDITIONALLY_APPROVED;
            BigDecimal factor = creditScore.compareTo(new BigDecimal("640")) >= 0
                    ? new BigDecimal("0.85") : new BigDecimal("0.70");
            maxAmount = requestedAmount.multiply(factor).setScale(2, RoundingMode.HALF_EVEN);
            maxTenure = Math.min(requestedTenure, 24);
            reason = "Conditional approval: Higher interest rate and lower loan amount may apply";
        } else {
            status = PreApprovalStatus.REJECTED;
            maxAmount = BigDecimal.ZERO;
            maxTenure = 0;
            reason = "Credit score does not meet minimum requirements for loan approval";
        }
        log.debug("DMN Eligibility evaluated: score={}, amount={}, tenure={} -> status={}, maxAmount={}, maxTenure={}",
                creditScore, requestedAmount, requestedTenure, status, maxAmount, maxTenure);
        return new EligibilityOutput(status, maxAmount, maxTenure, reason, pricing.interestRate(), pricing.riskCategory());
    }

    public record PricingOutput(BigDecimal interestRate, RiskCategory riskCategory) {}
    public record EligibilityOutput(PreApprovalStatus status, BigDecimal maxApprovedAmount,
                                    int maxTenureMonths, String reason,
                                    BigDecimal interestRate, RiskCategory riskCategory) {}
}
