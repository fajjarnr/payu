package id.payu.lending.application.service;

import id.payu.lending.domain.model.CreditScore;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.PayLater;
import id.payu.lending.domain.port.in.ApplyLoanUseCase;
import id.payu.lending.adapter.persistence.LoanPersistenceAdapter;
import id.payu.lending.adapter.persistence.PayLaterPersistenceAdapter;
import id.payu.lending.adapter.persistence.CreditScorePersistenceAdapter;
import id.payu.lending.adapter.messaging.KafkaLoanEventPublisherAdapter;
import id.payu.lending.dto.LoanApplicationRequest;
import id.payu.lending.dto.PayLaterLimitRequest;
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
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LendingApplicationServiceTest {

    @Mock
    private LoanPersistenceAdapter loanPersistenceAdapter;

    @Mock
    private PayLaterPersistenceAdapter payLaterPersistenceAdapter;

    @Mock
    private CreditScorePersistenceAdapter creditScorePersistenceAdapter;

    @Mock
    private KafkaLoanEventPublisherAdapter loanEventPublisherPort;

    private LendingApplicationService lendingApplicationService;

    @BeforeEach
    void setUp() {
        lendingApplicationService = new LendingApplicationService(
                loanPersistenceAdapter,
                payLaterPersistenceAdapter,
                creditScorePersistenceAdapter,
                loanEventPublisherPort
        );
    }

    @Test
    void testApplyLoan_WithGoodCreditScore_ShouldApprove() {
        UUID userId = UUID.randomUUID();
        CreditScore creditScore = new CreditScore();
        creditScore.setScore(new BigDecimal("750"));

        LoanApplicationRequest request = new LoanApplicationRequest(
                userId,
                "EXT-001",
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("10000000"),
                12,
                "Emergency"
        );

        when(creditScorePersistenceAdapter.findByUserId(userId)).thenReturn(Optional.of(creditScore));
        when(loanPersistenceAdapter.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(UUID.randomUUID());
            return loan;
        });

        CompletableFuture<Loan> result = lendingApplicationService.applyLoan(request);

        assertNotNull(result.join());
        assertEquals(Loan.LoanStatus.APPROVED, result.join().getStatus());
        verify(loanPersistenceAdapter, times(1)).save(any(Loan.class));
        verify(loanEventPublisherPort, times(1)).publishLoanApproved(any());
    }

    @Test
    void testApplyLoan_WithPoorCreditScore_ShouldReject() {
        UUID userId = UUID.randomUUID();
        CreditScore creditScore = new CreditScore();
        creditScore.setScore(new BigDecimal("500"));

        LoanApplicationRequest request = new LoanApplicationRequest(
                userId,
                "EXT-002",
                Loan.LoanType.PERSONAL_LOAN,
                new BigDecimal("10000000"),
                12,
                "Emergency"
        );

        when(creditScorePersistenceAdapter.findByUserId(userId)).thenReturn(Optional.of(creditScore));
        when(loanPersistenceAdapter.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(UUID.randomUUID());
            return loan;
        });

        CompletableFuture<Loan> result = lendingApplicationService.applyLoan(request);

        assertNotNull(result.join());
        assertEquals(Loan.LoanStatus.REJECTED, result.join().getStatus());
        verify(loanPersistenceAdapter, times(1)).save(any(Loan.class));
        verify(loanEventPublisherPort, times(1)).publishLoanRejected(any());
    }

    @Test
    void testGetLoanById_ShouldReturnLoan() {
        UUID loanId = UUID.randomUUID();
        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setExternalId("EXT-001");
        loan.setStatus(Loan.LoanStatus.APPROVED);

        when(loanPersistenceAdapter.findById(loanId)).thenReturn(Optional.of(loan));

        Optional<Loan> result = lendingApplicationService.getLoanById(loanId);

        assertTrue(result.isPresent());
        assertEquals(loanId, result.get().getId());
    }

    @Test
    void testActivatePayLater_ShouldCreatePayLaterAccount() {
        UUID userId = UUID.randomUUID();
        PayLaterLimitRequest request = new PayLaterLimitRequest(
                new BigDecimal("5000000"),
                1
        );

        when(payLaterPersistenceAdapter.findByUserId(userId)).thenReturn(Optional.empty());
        when(payLaterPersistenceAdapter.save(any(PayLater.class))).thenAnswer(invocation -> {
            PayLater payLater = invocation.getArgument(0);
            payLater.setId(UUID.randomUUID());
            return payLater;
        });

        PayLater result = lendingApplicationService.activatePayLater(userId, request);

        assertNotNull(result.getId());
        assertEquals(PayLater.PayLaterStatus.ACTIVE, result.getStatus());
        verify(payLaterPersistenceAdapter, times(1)).save(any(PayLater.class));
    }

    @Test
    void testCalculateCreditScore_ShouldCreateCreditScore() {
        UUID userId = UUID.randomUUID();

        when(creditScorePersistenceAdapter.save(any(CreditScore.class))).thenAnswer(invocation -> {
            CreditScore creditScore = invocation.getArgument(0);
            creditScore.setId(UUID.randomUUID());
            return creditScore;
        });

        CreditScore result = lendingApplicationService.calculateCreditScore(userId);

        assertNotNull(result.getId());
        assertNotNull(result.getScore());
        verify(creditScorePersistenceAdapter, times(1)).save(any(CreditScore.class));
    }

    @Test
    void testGetCreditScoreByUserId_ShouldReturnCreditScore() {
        UUID userId = UUID.randomUUID();
        CreditScore creditScore = new CreditScore();
        creditScore.setId(UUID.randomUUID());
        creditScore.setUserId(userId);
        creditScore.setScore(new BigDecimal("700"));

        when(creditScorePersistenceAdapter.findByUserId(userId)).thenReturn(Optional.of(creditScore));

        Optional<CreditScore> result = lendingApplicationService.getCreditScoreByUserId(userId);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
    }
}
