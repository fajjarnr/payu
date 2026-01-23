package id.payu.lending.application.service;

import id.payu.lending.domain.model.CreditScore;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.LoanPreApproval;
import id.payu.lending.domain.port.out.CreditScorePersistencePort;
import id.payu.lending.domain.port.out.LoanPreApprovalPersistencePort;
import id.payu.lending.dto.LoanPreApprovalRequest;
import id.payu.lending.dto.LoanPreApprovalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanPreApprovalServiceTest {

    @Mock
    private CreditScorePersistencePort creditScorePersistencePort;

    @Mock
    private LoanPreApprovalPersistencePort preApprovalPersistencePort;

    @Mock
    private EnhancedCreditScoringService enhancedCreditScoringService;

    private LoanPreApprovalService loanPreApprovalService;

    @BeforeEach
    void setUp() {
        loanPreApprovalService = new LoanPreApprovalService(
                creditScorePersistencePort,
                preApprovalPersistencePort,
                enhancedCreditScoringService
        );
    }

    @Test
    void testCheckPreApproval_WithExcellentCreditScore_ShouldApprove() {
        UUID userId = UUID.randomUUID();
        CreditScore creditScore = new CreditScore();
        creditScore.setId(UUID.randomUUID());
        creditScore.setUserId(userId);
        creditScore.setScore(new BigDecimal("800"));
        creditScore.setRiskCategory(CreditScore.RiskCategory.EXCELLENT);
        creditScore.setLastCalculatedAt(LocalDateTime.now());

        LoanPreApprovalRequest request = new LoanPreApprovalRequest(
                userId,
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("20000000"),
                24,
                "Home renovation"
        );

        when(creditScorePersistencePort.findByUserId(userId)).thenReturn(Optional.of(creditScore));
        when(enhancedCreditScoringService.calculateEnhancedCreditScore(eq(userId), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("800"));
        when(preApprovalPersistencePort.save(any(LoanPreApproval.class))).thenAnswer(invocation -> {
            LoanPreApproval preApproval = invocation.getArgument(0);
            preApproval.setId(UUID.randomUUID());
            return preApproval;
        });

        LoanPreApprovalResponse result = loanPreApprovalService.checkPreApproval(request);

        assertNotNull(result);
        assertEquals(LoanPreApprovalResponse.PreApprovalStatus.APPROVED, result.status());
        assertEquals(userId, result.userId());
        assertEquals(Loan.LoanType.PERSONAL_LOAN, result.loanType());
        assertTrue(result.maxApprovedAmount().compareTo(request.principalAmount()) >= 0);
        assertEquals(new BigDecimal("0.12"), result.minInterestRate());
        assertEquals(CreditScore.RiskCategory.EXCELLENT, result.riskCategory());
        assertNotNull(result.validUntil());
        verify(preApprovalPersistencePort, times(1)).save(any(LoanPreApproval.class));
    }

    @Test
    void testCheckPreApproval_WithGoodCreditScore_ShouldApprove() {
        UUID userId = UUID.randomUUID();
        CreditScore creditScore = new CreditScore();
        creditScore.setId(UUID.randomUUID());
        creditScore.setUserId(userId);
        creditScore.setScore(new BigDecimal("700"));
        creditScore.setRiskCategory(CreditScore.RiskCategory.GOOD);
        creditScore.setLastCalculatedAt(LocalDateTime.now());

        LoanPreApprovalRequest request = new LoanPreApprovalRequest(
                userId,
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("50000000"),
                36,
                "Education"
        );

        when(creditScorePersistencePort.findByUserId(userId)).thenReturn(Optional.of(creditScore));
        when(enhancedCreditScoringService.calculateEnhancedCreditScore(eq(userId), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("700"));
        when(preApprovalPersistencePort.save(any(LoanPreApproval.class))).thenAnswer(invocation -> {
            LoanPreApproval preApproval = invocation.getArgument(0);
            preApproval.setId(UUID.randomUUID());
            return preApproval;
        });

        LoanPreApprovalResponse result = loanPreApprovalService.checkPreApproval(request);

        assertNotNull(result);
        assertEquals(LoanPreApprovalResponse.PreApprovalStatus.APPROVED, result.status());
        assertEquals(userId, result.userId());
        assertEquals(CreditScore.RiskCategory.GOOD, result.riskCategory());
        verify(preApprovalPersistencePort, times(1)).save(any(LoanPreApproval.class));
    }

    @Test
    void testCheckPreApproval_WithFairCreditScore_ShouldConditionallyApprove() {
        UUID userId = UUID.randomUUID();
        CreditScore creditScore = new CreditScore();
        creditScore.setId(UUID.randomUUID());
        creditScore.setUserId(userId);
        creditScore.setScore(new BigDecimal("650"));
        creditScore.setRiskCategory(CreditScore.RiskCategory.FAIR);
        creditScore.setLastCalculatedAt(LocalDateTime.now());

        LoanPreApprovalRequest request = new LoanPreApprovalRequest(
                userId,
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("40000000"),
                24,
                "Car repair"
        );

        when(creditScorePersistencePort.findByUserId(userId)).thenReturn(Optional.of(creditScore));
        when(enhancedCreditScoringService.calculateEnhancedCreditScore(eq(userId), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("650"));
        when(preApprovalPersistencePort.save(any(LoanPreApproval.class))).thenAnswer(invocation -> {
            LoanPreApproval preApproval = invocation.getArgument(0);
            preApproval.setId(UUID.randomUUID());
            return preApproval;
        });

        LoanPreApprovalResponse result = loanPreApprovalService.checkPreApproval(request);

        assertNotNull(result);
        assertEquals(LoanPreApprovalResponse.PreApprovalStatus.APPROVED, result.status());
        assertEquals(userId, result.userId());
        assertEquals(CreditScore.RiskCategory.FAIR, result.riskCategory());
        verify(preApprovalPersistencePort, times(1)).save(any(LoanPreApproval.class));
    }

    @Test
    void testCheckPreApproval_WithPoorCreditScore_ShouldReject() {
        UUID userId = UUID.randomUUID();
        CreditScore creditScore = new CreditScore();
        creditScore.setId(UUID.randomUUID());
        creditScore.setUserId(userId);
        creditScore.setScore(new BigDecimal("550"));
        creditScore.setRiskCategory(CreditScore.RiskCategory.POOR);
        creditScore.setLastCalculatedAt(LocalDateTime.now());

        LoanPreApprovalRequest request = new LoanPreApprovalRequest(
                userId,
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("10000000"),
                12,
                "Emergency"
        );

        when(creditScorePersistencePort.findByUserId(userId)).thenReturn(Optional.of(creditScore));
        when(enhancedCreditScoringService.calculateEnhancedCreditScore(eq(userId), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("550"));
        when(preApprovalPersistencePort.save(any(LoanPreApproval.class))).thenAnswer(invocation -> {
            LoanPreApproval preApproval = invocation.getArgument(0);
            preApproval.setId(UUID.randomUUID());
            return preApproval;
        });

        LoanPreApprovalResponse result = loanPreApprovalService.checkPreApproval(request);

        assertNotNull(result);
        assertEquals(LoanPreApprovalResponse.PreApprovalStatus.REJECTED, result.status());
        assertEquals(userId, result.userId());
        assertNotNull(result.reason());
        verify(preApprovalPersistencePort, times(1)).save(any(LoanPreApproval.class));
    }

    @Test
    void testCheckPreApproval_WithVeryPoorCreditScore_ShouldReject() {
        UUID userId = UUID.randomUUID();
        CreditScore creditScore = new CreditScore();
        creditScore.setId(UUID.randomUUID());
        creditScore.setUserId(userId);
        creditScore.setScore(new BigDecimal("450"));
        creditScore.setRiskCategory(CreditScore.RiskCategory.VERY_POOR);
        creditScore.setLastCalculatedAt(LocalDateTime.now());

        LoanPreApprovalRequest request = new LoanPreApprovalRequest(
                userId,
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("5000000"),
                6,
                "Emergency"
        );

        when(creditScorePersistencePort.findByUserId(userId)).thenReturn(Optional.of(creditScore));
        when(enhancedCreditScoringService.calculateEnhancedCreditScore(eq(userId), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("450"));
        when(preApprovalPersistencePort.save(any(LoanPreApproval.class))).thenAnswer(invocation -> {
            LoanPreApproval preApproval = invocation.getArgument(0);
            preApproval.setId(UUID.randomUUID());
            return preApproval;
        });

        LoanPreApprovalResponse result = loanPreApprovalService.checkPreApproval(request);

        assertNotNull(result);
        assertEquals(LoanPreApprovalResponse.PreApprovalStatus.REJECTED, result.status());
        verify(preApprovalPersistencePort, times(1)).save(any(LoanPreApproval.class));
    }

    @Test
    void testCheckPreApproval_WithNoExistingCreditScore_ShouldCalculateNewScore() {
        UUID userId = UUID.randomUUID();
        LoanPreApprovalRequest request = new LoanPreApprovalRequest(
                userId,
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("15000000"),
                18,
                "Car purchase"
        );

        when(creditScorePersistencePort.findByUserId(userId)).thenReturn(Optional.empty());
        when(enhancedCreditScoringService.calculateEnhancedCreditScore(eq(userId), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("720"));
        when(preApprovalPersistencePort.save(any(LoanPreApproval.class))).thenAnswer(invocation -> {
            LoanPreApproval preApproval = invocation.getArgument(0);
            preApproval.setId(UUID.randomUUID());
            return preApproval;
        });

        LoanPreApprovalResponse result = loanPreApprovalService.checkPreApproval(request);

        assertNotNull(result);
        assertNotNull(result.creditScore());
        assertEquals(new BigDecimal("720"), result.creditScore());
        verify(preApprovalPersistencePort, times(1)).save(any(LoanPreApproval.class));
    }

    @Test
    void testCheckPreApproval_ValidUntilDate_ShouldBe30Days() {
        UUID userId = UUID.randomUUID();
        CreditScore creditScore = new CreditScore();
        creditScore.setId(UUID.randomUUID());
        creditScore.setUserId(userId);
        creditScore.setScore(new BigDecimal("750"));
        creditScore.setRiskCategory(CreditScore.RiskCategory.GOOD);
        creditScore.setLastCalculatedAt(LocalDateTime.now());

        LoanPreApprovalRequest request = new LoanPreApprovalRequest(
                userId,
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("10000000"),
                12,
                "Vacation"
        );

        when(creditScorePersistencePort.findByUserId(userId)).thenReturn(Optional.of(creditScore));
        when(enhancedCreditScoringService.calculateEnhancedCreditScore(eq(userId), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("750"));
        when(preApprovalPersistencePort.save(any(LoanPreApproval.class))).thenAnswer(invocation -> {
            LoanPreApproval preApproval = invocation.getArgument(0);
            preApproval.setId(UUID.randomUUID());
            return preApproval;
        });

        LoanPreApprovalResponse result = loanPreApprovalService.checkPreApproval(request);

        assertNotNull(result);
        assertNotNull(result.validUntil());
        LocalDateTime expectedValidUntil = LocalDateTime.now().plusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
        assertEquals(expectedValidUntil.toLocalDate(), result.validUntil().toLocalDate());
    }

    @Test
    void testGetPreApprovalById_ShouldReturnPreApproval() {
        UUID preApprovalId = UUID.randomUUID();
        LoanPreApproval preApproval = new LoanPreApproval();
        preApproval.setId(preApprovalId);
        preApproval.setUserId(UUID.randomUUID());
        preApproval.setStatus(LoanPreApproval.PreApprovalStatus.APPROVED);

        when(preApprovalPersistencePort.findById(preApprovalId)).thenReturn(Optional.of(preApproval));

        var result = loanPreApprovalService.getPreApprovalById(preApprovalId);

        assertTrue(result.isPresent());
        assertEquals(preApprovalId, result.get().getId());
    }

    @Test
    void testGetActivePreApprovalByUserId_ShouldReturnActivePreApproval() {
        UUID userId = UUID.randomUUID();
        LoanPreApproval preApproval = new LoanPreApproval();
        preApproval.setId(UUID.randomUUID());
        preApproval.setUserId(userId);
        preApproval.setStatus(LoanPreApproval.PreApprovalStatus.APPROVED);
        preApproval.setValidUntil(LocalDate.now().plusDays(15));

        when(preApprovalPersistencePort.findActiveByUserId(userId)).thenReturn(Optional.of(preApproval));

        var result = loanPreApprovalService.getActivePreApprovalByUserId(userId);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
    }

    @Test
    void testCheckPreApproval_EstimatedMonthlyPayment_ShouldBeCalculatedCorrectly() {
        UUID userId = UUID.randomUUID();
        CreditScore creditScore = new CreditScore();
        creditScore.setId(UUID.randomUUID());
        creditScore.setUserId(userId);
        creditScore.setScore(new BigDecimal("780"));
        creditScore.setRiskCategory(CreditScore.RiskCategory.EXCELLENT);
        creditScore.setLastCalculatedAt(LocalDateTime.now());

        BigDecimal principalAmount = new BigDecimal("12000000");
        int tenureMonths = 12;

        LoanPreApprovalRequest request = new LoanPreApprovalRequest(
                userId,
                Loan.LoanType.PERSONAL_LOAN,
                principalAmount,
                tenureMonths,
                "Business"
        );

        when(creditScorePersistencePort.findByUserId(userId)).thenReturn(Optional.of(creditScore));
        when(enhancedCreditScoringService.calculateEnhancedCreditScore(eq(userId), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("780"));
        when(preApprovalPersistencePort.save(any(LoanPreApproval.class))).thenAnswer(invocation -> {
            LoanPreApproval preApproval = invocation.getArgument(0);
            preApproval.setId(UUID.randomUUID());
            return preApproval;
        });

        LoanPreApprovalResponse result = loanPreApprovalService.checkPreApproval(request);

        assertNotNull(result);
        assertNotNull(result.estimatedMonthlyPayment());
        assertTrue(result.estimatedMonthlyPayment().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCheckPreApproval_MaxTenureMonths_WithExcellentCredit() {
        UUID userId = UUID.randomUUID();
        CreditScore creditScore = new CreditScore();
        creditScore.setId(UUID.randomUUID());
        creditScore.setUserId(userId);
        creditScore.setScore(new BigDecimal("800"));
        creditScore.setRiskCategory(CreditScore.RiskCategory.EXCELLENT);
        creditScore.setLastCalculatedAt(LocalDateTime.now());

        LoanPreApprovalRequest request = new LoanPreApprovalRequest(
                userId,
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("30000000"),
                36,
                "Home improvement"
        );

        when(creditScorePersistencePort.findByUserId(userId)).thenReturn(Optional.of(creditScore));
        when(enhancedCreditScoringService.calculateEnhancedCreditScore(eq(userId), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("800"));
        when(preApprovalPersistencePort.save(any(LoanPreApproval.class))).thenAnswer(invocation -> {
            LoanPreApproval preApproval = invocation.getArgument(0);
            preApproval.setId(UUID.randomUUID());
            return preApproval;
        });

        LoanPreApprovalResponse result = loanPreApprovalService.checkPreApproval(request);

        assertNotNull(result);
        assertNotNull(result.maxTenureMonths());
        assertTrue(result.maxTenureMonths() >= request.tenureMonths());
    }
}
