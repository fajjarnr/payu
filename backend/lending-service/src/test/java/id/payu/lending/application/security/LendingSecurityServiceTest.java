package id.payu.lending.application.security;

import id.payu.lending.application.service.InstallmentService;
import id.payu.lending.application.service.LendingApplicationService;
import id.payu.lending.application.service.LoanManagementService;
import id.payu.lending.application.service.LoanPreApprovalService;
import id.payu.lending.application.service.PayLaterTransactionService;
import id.payu.lending.domain.model.InstallmentCheckout;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.RepaymentSchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LendingSecurityService Unit Tests")
class LendingSecurityServiceTest {

    @Mock
    private LendingApplicationService lendingApplicationService;
    @Mock
    private PayLaterTransactionService payLaterTransactionService;
    @Mock
    private LoanManagementService loanManagementService;
    @Mock
    private LoanPreApprovalService preApprovalService;
    @Mock
    private InstallmentService installmentService;

    @InjectMocks
    private LendingSecurityService securityService;

    private final UUID owner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    @DisplayName("isLoanOwner returns true when loan belongs to the authenticated user")
    void isLoanOwner_whenOwner_shouldReturnTrue() {
        Loan loan = new Loan();
        loan.setUserId(owner);
        when(lendingApplicationService.getLoanById(any())).thenReturn(Optional.of(loan));

        assertThat(securityService.isLoanOwner(UUID.randomUUID(), owner)).isTrue();
    }

    @Test
    @DisplayName("isLoanOwner returns false when loan belongs to another user")
    void isLoanOwner_whenNotOwner_shouldReturnFalse() {
        Loan loan = new Loan();
        loan.setUserId(owner);
        when(lendingApplicationService.getLoanById(any())).thenReturn(Optional.of(loan));

        assertThat(securityService.isLoanOwner(UUID.randomUUID(), other)).isFalse();
    }

    @Test
    @DisplayName("isLoanOwner returns false for a non-existent loan")
    void isLoanOwner_whenMissingLoan_shouldReturnFalse() {
        when(lendingApplicationService.getLoanById(any())).thenReturn(Optional.empty());

        assertThat(securityService.isLoanOwner(UUID.randomUUID(), owner)).isFalse();
    }

    @Test
    @DisplayName("isPaylaterOwner matches authenticated user ID")
    void isPaylaterOwner_shouldMatchUserIds() {
        assertThat(securityService.isPaylaterOwner(owner, owner)).isTrue();
        assertThat(securityService.isPaylaterOwner(owner, other)).isFalse();
    }

    @Test
    @DisplayName("isCreditScoreOwner matches authenticated user ID")
    void isCreditScoreOwner_shouldMatchUserIds() {
        assertThat(securityService.isCreditScoreOwner(owner, owner)).isTrue();
        assertThat(securityService.isCreditScoreOwner(owner, other)).isFalse();
    }

    @Test
    @DisplayName("isRepaymentScheduleOwner delegates to the schedule's loan owner")
    void isRepaymentScheduleOwner_shouldCheckLoanOwnership() {
        UUID scheduleId = UUID.randomUUID();
        UUID loanId = UUID.randomUUID();
        Loan loan = new Loan();
        loan.setUserId(owner);

        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setLoanId(loanId);

        when(loanManagementService.getRepaymentSchedule(scheduleId)).thenReturn(Optional.of(schedule));
        when(lendingApplicationService.getLoanById(loanId)).thenReturn(Optional.of(loan));

        assertThat(securityService.isRepaymentScheduleOwner(scheduleId, owner)).isTrue();
        assertThat(securityService.isRepaymentScheduleOwner(scheduleId, other)).isFalse();
    }

    @Test
    @DisplayName("isPreApprovalOwnerById matches the pre-approval's user")
    void isPreApprovalOwnerById_shouldMatchUserIds() {
        UUID preApprovalId = UUID.randomUUID();
        id.payu.lending.domain.model.LoanPreApproval pre = new id.payu.lending.domain.model.LoanPreApproval();
        pre.setUserId(owner);
        when(preApprovalService.getPreApprovalById(preApprovalId)).thenReturn(Optional.of(pre));

        assertThat(securityService.isPreApprovalOwnerById(preApprovalId, owner)).isTrue();
        assertThat(securityService.isPreApprovalOwnerById(preApprovalId, other)).isFalse();
    }

    @Test
    @DisplayName("isInstallmentOwner matches the checkout's user and fails closed on errors")
    void isInstallmentOwner_shouldMatchAndFailClosed() {
        UUID checkoutId = UUID.randomUUID();
        InstallmentCheckout checkout = new InstallmentCheckout();
        checkout.setUserId(owner);
        when(installmentService.getCheckout(checkoutId)).thenReturn(checkout);

        assertThat(securityService.isInstallmentOwner(checkoutId, owner)).isTrue();
        assertThat(securityService.isInstallmentOwner(checkoutId, other)).isFalse();

        when(installmentService.getCheckout(checkoutId)).thenThrow(new RuntimeException("downstream"));
        assertThat(securityService.isInstallmentOwner(checkoutId, owner)).isFalse();
    }

    @Test
    @DisplayName("isPreApprovalOwner matches authenticated user ID")
    void isPreApprovalOwner_shouldMatchUserIds() {
        assertThat(securityService.isPreApprovalOwner(owner, owner)).isTrue();
        assertThat(securityService.isPreApprovalOwner(owner, other)).isFalse();
    }
}
