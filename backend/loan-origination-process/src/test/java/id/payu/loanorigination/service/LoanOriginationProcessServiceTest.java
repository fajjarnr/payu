package id.payu.loanorigination.service;

import id.payu.loanorigination.adapter.persistence.LoanOriginationProcessEntity;
import id.payu.loanorigination.adapter.persistence.LoanOriginationProcessRepository;
import id.payu.loanorigination.domain.LoanOriginationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanOriginationProcessServiceTest {

    @Mock
    private CreditScoringService creditScoring;

    @Mock
    private DisbursementService disbursement;

    @Mock
    private LoanOriginationProcessRepository repository;

    @Test
    void shouldPersistAuthenticatedUserInsteadOfRequestUser() {
        when(creditScoring.evaluate(any(), anyInt())).thenReturn(new BigDecimal("700"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new LoanOriginationProcessService(creditScoring, disbursement, repository);

        var result = service.startProcess(
                new LoanOriginationRequest("attacker", new BigDecimal("100000"), 12, "home", "PERSONAL_LOAN"),
                "jwt-user");

        assertThat(result.getUserId()).isEqualTo("jwt-user");
        assertThat(result.getStatus()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void shouldNotDisburseAgainWhenApprovalReplayArrives() {
        var process = pendingProcess();
        process.setStatus("APPROVED");
        process.setApproved(true);
        when(repository.findByIdForUpdate(process.getId())).thenReturn(java.util.Optional.of(process));
        var service = new LoanOriginationProcessService(creditScoring, disbursement, repository);

        var result = service.approve(process.getId(), true, "replay", "loan-officer");

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        verifyNoInteractions(disbursement);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectBlankAuthenticatedUser() {
        var service = new LoanOriginationProcessService(creditScoring, disbursement, repository);

        assertThatThrownBy(() -> service.startProcess(
                new LoanOriginationRequest("u", new BigDecimal("100000"), 12, "home", "PERSONAL_LOAN"),
                " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user is required");
    }

    @Test
    void shouldRejectNonPositivePrincipal() {
        var service = new LoanOriginationProcessService(creditScoring, disbursement, repository);

        assertThatThrownBy(() -> service.startProcess(
                new LoanOriginationRequest("u", new BigDecimal("0"), 12, "home", "PERSONAL_LOAN"),
                "jwt-user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Principal amount must be positive");
    }

    @Test
    void shouldRejectLowScore() {
        when(creditScoring.evaluate(any(), anyInt())).thenReturn(new BigDecimal("500"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new LoanOriginationProcessService(creditScoring, disbursement, repository);

        var result = service.startProcess(
                new LoanOriginationRequest("u", new BigDecimal("100000"), 12, "home", "PERSONAL_LOAN"),
                "jwt-user");

        assertThat(result.getStatus()).isEqualTo("REJECTED_LOW_SCORE");
        assertThat(result.getApproved()).isFalse();
    }

    @Test
    void shouldDefaultLoanTypeToPersonalLoan() {
        when(creditScoring.evaluate(any(), anyInt())).thenReturn(new BigDecimal("700"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new LoanOriginationProcessService(creditScoring, disbursement, repository);

        var result = service.startProcess(
                new LoanOriginationRequest("u", new BigDecimal("100000"), 12, "home", null),
                "jwt-user");

        assertThat(result.getLoanType()).isEqualTo("PERSONAL_LOAN");
    }

    @Test
    void shouldGetProcessAndListIds() {
        var process = pendingProcess();
        when(repository.findById(process.getId())).thenReturn(java.util.Optional.of(process));
        when(repository.findAll()).thenReturn(java.util.List.of(process));
        var service = new LoanOriginationProcessService(creditScoring, disbursement, repository);
        assertThat(service.getProcess(process.getId())).isPresent();
        assertThat(service.getProcess(process.getId()).get().getId()).isEqualTo(process.getId());
        assertThat(service.listProcessIds()).containsExactly(process.getId());
    }

    @Test
    void shouldFailWhenApprovingMissingProcess() {
        var missing = java.util.UUID.randomUUID();
        when(repository.findByIdForUpdate(missing)).thenReturn(java.util.Optional.empty());
        var service = new LoanOriginationProcessService(creditScoring, disbursement, repository);

        assertThatThrownBy(() -> service.approve(missing, true, "c", "officer"))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void shouldRejectApprovalOfCompletedProcessWithDifferentOutcome() {
        var process = pendingProcess();
        process.setStatus("REJECTED");
        process.setApproved(false);
        when(repository.findByIdForUpdate(process.getId())).thenReturn(java.util.Optional.of(process));
        var service = new LoanOriginationProcessService(creditScoring, disbursement, repository);

        assertThatThrownBy(() -> service.approve(process.getId(), true, "late", "officer"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void shouldRejectProcessWithoutDisbursement() {
        var process = pendingProcess();
        when(repository.findByIdForUpdate(process.getId())).thenReturn(java.util.Optional.of(process));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new LoanOriginationProcessService(creditScoring, disbursement, repository);

        var result = service.approve(process.getId(), false, "no", "officer");

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(result.getApproved()).isFalse();
        verifyNoInteractions(disbursement);
    }

    @Test
    void shouldApproveAndDisburse() {
        var process = pendingProcess();
        when(repository.findByIdForUpdate(process.getId())).thenReturn(java.util.Optional.of(process));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new LoanOriginationProcessService(creditScoring, disbursement, repository);

        var result = service.approve(process.getId(), true, "ok", "officer");

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getDisbursementReference()).isEqualTo("LOAN-" + process.getId());
        verify(disbursement).execute("jwt-user", new BigDecimal("100000"), "PERSONAL_LOAN", 12, "LOAN-" + process.getId());
    }

    private LoanOriginationProcessEntity pendingProcess() {
        var process = new LoanOriginationProcessEntity();
        process.setId(UUID.randomUUID());
        process.setUserId("jwt-user");
        process.setPrincipalAmount(new BigDecimal("100000"));
        process.setLoanType("PERSONAL_LOAN");
        process.setTenureMonths(12);
        process.setStatus("PENDING_APPROVAL");
        return process;
    }
}
