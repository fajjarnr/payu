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
