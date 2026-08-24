package id.payu.lending.application.service;

import id.payu.lending.domain.model.PreApprovalStatus;
import id.payu.lending.domain.model.RiskCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD boundary tests for DMN decision tables (ADR-0048).
 * Verifies pricing (interest rate tiers) and eligibility (thresholds 600/640/650/700/750)
 * plus BigDecimal HALF_EVEN for conditional amounts.
 */
class LendingDmnServiceTest {

    private LendingDmnService dmn;

    @BeforeEach
    void setUp() {
        dmn = new LendingDmnService();
    }

    // pricing.dmn: ≥750→12%/EXCELLENT, 700-749→14%/GOOD, 650-699→16%/FAIR, 600-649→18%/POOR, <600→18%/VERY_POOR
    @ParameterizedTest
    @CsvSource({
            "800,0.12,EXCELLENT",
            "750,0.12,EXCELLENT",
            "749,0.14,GOOD",
            "700,0.14,GOOD",
            "699,0.16,FAIR",
            "650,0.16,FAIR",
            "649,0.18,POOR",
            "600,0.18,POOR",
            "599,0.18,VERY_POOR",
            "0,0.18,VERY_POOR"
    })
    void pricingBoundary(int score, String expectedRate, String expectedRisk) {
        var out = dmn.evaluatePricing(new BigDecimal(score));
        assertEquals(new BigDecimal(expectedRate), out.interestRate());
        assertEquals(RiskCategory.valueOf(expectedRisk), out.riskCategory());
    }

    // eligibility.dmn: ≥650 APPROVED, 600-649 CONDITIONAL (640→0.85 else 0.70, min tenure 24), <600 REJECTED
    @Test
    void eligibility_650_shouldBeApproved() {
        var out = dmn.evaluateEligibility(new BigDecimal("650"), new BigDecimal("10000000"), 36);
        assertEquals(PreApprovalStatus.APPROVED, out.status());
        assertEquals(new BigDecimal("10000000"), out.maxApprovedAmount());
        assertEquals(36, out.maxTenureMonths());
        assertNull(out.reason());
        assertEquals(new BigDecimal("0.16"), out.interestRate());
        assertEquals(RiskCategory.FAIR, out.riskCategory());
    }

    @Test
    void eligibility_640_shouldBeConditional_085() {
        var out = dmn.evaluateEligibility(new BigDecimal("640"), new BigDecimal("10000000"), 36);
        assertEquals(PreApprovalStatus.CONDITIONALLY_APPROVED, out.status());
        // 10M * 0.85 = 8500000.00 with HALF_EVEN
        assertEquals(new BigDecimal("8500000.00"), out.maxApprovedAmount());
        assertEquals(24, out.maxTenureMonths());
        assertNotNull(out.reason());
        assertEquals(new BigDecimal("0.18"), out.interestRate());
    }

    @Test
    void eligibility_639_shouldBeConditional_070() {
        var out = dmn.evaluateEligibility(new BigDecimal("639"), new BigDecimal("10000000"), 36);
        assertEquals(PreApprovalStatus.CONDITIONALLY_APPROVED, out.status());
        assertEquals(new BigDecimal("7000000.00"), out.maxApprovedAmount());
        assertEquals(24, out.maxTenureMonths());
    }

    @Test
    void eligibility_600_shouldBeConditional_070() {
        var out = dmn.evaluateEligibility(new BigDecimal("600"), new BigDecimal("5000000"), 12);
        assertEquals(PreApprovalStatus.CONDITIONALLY_APPROVED, out.status());
        assertEquals(new BigDecimal("3500000.00"), out.maxApprovedAmount());
        assertEquals(12, out.maxTenureMonths()); // min(12,24)=12
        assertEquals(RiskCategory.POOR, out.riskCategory());
    }

    @Test
    void eligibility_599_shouldBeRejected() {
        var out = dmn.evaluateEligibility(new BigDecimal("599"), new BigDecimal("10000000"), 24);
        assertEquals(PreApprovalStatus.REJECTED, out.status());
        assertEquals(BigDecimal.ZERO, out.maxApprovedAmount());
        assertEquals(0, out.maxTenureMonths());
        assertNotNull(out.reason());
        assertEquals(RiskCategory.VERY_POOR, out.riskCategory());
    }

    @Test
    void eligibility_conditionalTenureCappedAt24() {
        var out = dmn.evaluateEligibility(new BigDecimal("620"), new BigDecimal("20000000"), 60);
        assertEquals(24, out.maxTenureMonths());
        var out2 = dmn.evaluateEligibility(new BigDecimal("620"), new BigDecimal("20000000"), 24);
        assertEquals(24, out2.maxTenureMonths());
        var out3 = dmn.evaluateEligibility(new BigDecimal("620"), new BigDecimal("20000000"), 12);
        assertEquals(12, out3.maxTenureMonths());
    }

    @Test
    void pricingAndEligibility_750_boundary() {
        var pricing = dmn.evaluatePricing(new BigDecimal("750"));
        assertEquals(new BigDecimal("0.12"), pricing.interestRate());
        var elig = dmn.evaluateEligibility(new BigDecimal("750"), new BigDecimal("50000000"), 60);
        assertEquals(PreApprovalStatus.APPROVED, elig.status());
        assertEquals(new BigDecimal("0.12"), elig.interestRate());
        assertEquals(RiskCategory.EXCELLENT, elig.riskCategory());
    }

    @Test
    void conditionalAmount_HALF_EVEN_rounding() {
        // 3333333 * 0.85 = 2833333.05, *0.70 = 2333333.10
        var out = dmn.evaluateEligibility(new BigDecimal("640"), new BigDecimal("3333333"), 24);
        assertEquals(new BigDecimal("2833333.05"), out.maxApprovedAmount());
        var out2 = dmn.evaluateEligibility(new BigDecimal("600"), new BigDecimal("3333333"), 24);
        assertEquals(new BigDecimal("2333333.10"), out2.maxApprovedAmount());
    }
}
