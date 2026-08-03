package id.payu.lending.application.service;

import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.LoanStatus;
import id.payu.lending.domain.model.LoanType;
import id.payu.lending.domain.model.RepaymentPayment;
import id.payu.lending.domain.model.RepaymentPaymentStatus;
import id.payu.lending.domain.model.RepaymentSchedule;
import id.payu.lending.domain.model.RepaymentStatus;
import id.payu.lending.domain.port.out.LoanPersistencePort;
import id.payu.lending.domain.port.out.LoanEventPublisherPort;
import id.payu.lending.domain.port.out.RepaymentPaymentPersistencePort;
import id.payu.lending.domain.port.out.RepaymentSchedulePersistencePort;
import id.payu.lending.domain.port.out.WalletPaymentPort;
import id.payu.lending.exception.RepaymentProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanManagementService")
class LoanManagementServiceTest {

    @Mock
    private LoanPersistencePort loanPersistencePort;

    @Mock
    private RepaymentSchedulePersistencePort repaymentSchedulePersistencePort;

    @Mock
    private RepaymentPaymentPersistencePort repaymentPaymentPersistencePort;

    @Mock
    private WalletPaymentPort walletPaymentPort;

    @Mock
    private LoanEventPublisherPort loanEventPublisherPort;

    @InjectMocks
    private LoanManagementService loanManagementService;

    private Loan testLoan;
    private UUID loanId;

    @BeforeEach
    void setUp() {
        loanId = UUID.randomUUID();

        testLoan = new Loan();
        testLoan.setId(loanId);
        testLoan.setExternalId("EXT-001");
        testLoan.setUserId(UUID.randomUUID());
        testLoan.setType(LoanType.PERSONAL_LOAN);
        testLoan.setPrincipalAmount(new BigDecimal("12000000"));
        testLoan.setInterestRate(new BigDecimal("0.14"));
        testLoan.setTenureMonths(12);
        testLoan.setMonthlyInstallment(new BigDecimal("1078000"));
        testLoan.setOutstandingBalance(new BigDecimal("12000000"));
        testLoan.setStatus(LoanStatus.APPROVED);
        testLoan.setPurpose("Home renovation");
        testLoan.setDisbursementDate(LocalDate.now());
        testLoan.setMaturityDate(LocalDate.now().plusMonths(12));
        testLoan.setCreatedAt(LocalDateTime.now());
        testLoan.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createRepaymentSchedule")
    class CreateRepaymentSchedule {

        @Test
        @DisplayName("should create repayment schedule successfully")
        void shouldCreateRepaymentScheduleSuccessfully() {
            when(loanPersistencePort.findById(loanId)).thenReturn(Optional.of(testLoan));
            when(repaymentSchedulePersistencePort.save(any(RepaymentSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

            List<RepaymentSchedule> result = loanManagementService.createRepaymentSchedule(loanId);

            assertThat(result).hasSize(12);
            assertThat(result.get(0).getLoanId()).isEqualTo(loanId);
            assertThat(result.get(0).getInstallmentNumber()).isEqualTo(1);
            verify(repaymentSchedulePersistencePort, times(12)).save(any(RepaymentSchedule.class));
        }

        @Test
        @DisplayName("should throw exception when loan not found")
        void shouldThrowExceptionWhenLoanNotFound() {
            when(loanPersistencePort.findById(loanId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanManagementService.createRepaymentSchedule(loanId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan not found");

            verify(repaymentSchedulePersistencePort, never()).save(any(RepaymentSchedule.class));
        }

        @Test
        @DisplayName("should return existing schedule instead of creating duplicates")
        void shouldReturnExistingScheduleWhenAlreadyCreated() {
            RepaymentSchedule existing = new RepaymentSchedule();
            existing.setId(UUID.randomUUID());
            existing.setLoanId(loanId);
            existing.setInstallmentNumber(1);

            when(loanPersistencePort.findById(loanId)).thenReturn(Optional.of(testLoan));
            when(repaymentSchedulePersistencePort.findByLoanId(loanId)).thenReturn(List.of(existing));

            assertThat(loanManagementService.createRepaymentSchedule(loanId)).containsExactly(existing);
            verify(repaymentSchedulePersistencePort, never()).save(any(RepaymentSchedule.class));
        }
    }

    @Nested
    @DisplayName("getRepaymentSchedule")
    class GetRepaymentSchedule {

        @Test
        @DisplayName("should get repayment schedule by id")
        void shouldGetRepaymentScheduleById() {
            UUID scheduleId = UUID.randomUUID();
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setId(scheduleId);
            schedule.setLoanId(loanId);
            schedule.setInstallmentNumber(1);

            when(repaymentSchedulePersistencePort.findById(scheduleId)).thenReturn(Optional.of(schedule));

            Optional<RepaymentSchedule> result = loanManagementService.getRepaymentSchedule(scheduleId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(scheduleId);
        }

        @Test
        @DisplayName("should return empty when schedule not found")
        void shouldReturnEmptyWhenScheduleNotFound() {
            UUID scheduleId = UUID.randomUUID();

            when(repaymentSchedulePersistencePort.findById(scheduleId)).thenReturn(Optional.empty());

            Optional<RepaymentSchedule> result = loanManagementService.getRepaymentSchedule(scheduleId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRepaymentScheduleByLoanId")
    class GetRepaymentScheduleByLoanId {

        @Test
        @DisplayName("should get all schedules for loan")
        void shouldGetAllSchedulesForLoan() {
            RepaymentSchedule schedule1 = new RepaymentSchedule();
            schedule1.setId(UUID.randomUUID());
            schedule1.setLoanId(loanId);
            schedule1.setInstallmentNumber(1);

            RepaymentSchedule schedule2 = new RepaymentSchedule();
            schedule2.setId(UUID.randomUUID());
            schedule2.setLoanId(loanId);
            schedule2.setInstallmentNumber(2);

            when(repaymentSchedulePersistencePort.findByLoanId(loanId)).thenReturn(List.of(schedule1, schedule2));

            List<RepaymentSchedule> result = loanManagementService.getRepaymentScheduleByLoanId(loanId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getLoanId()).isEqualTo(loanId);
        }
    }

    @Nested
    @DisplayName("processRepayment")
    class ProcessRepayment {

        @Test
        @DisplayName("should process full repayment successfully")
        void shouldProcessFullRepaymentSuccessfully() {
            UUID scheduleId = UUID.randomUUID();
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setId(scheduleId);
            schedule.setLoanId(loanId);
            schedule.setInstallmentNumber(1);
            schedule.setInstallmentAmount(new BigDecimal("1078000"));
            schedule.setPrincipalAmount(new BigDecimal("1000000"));
            schedule.setInterestAmount(new BigDecimal("78000"));
            schedule.setOutstandingPrincipal(new BigDecimal("12000000"));
            schedule.setDueDate(LocalDate.now().plusMonths(1));
            schedule.setStatus(RepaymentStatus.PENDING);
            schedule.setPaidAmount(null);
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setUpdatedAt(LocalDateTime.now());

            when(repaymentSchedulePersistencePort.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
            when(repaymentSchedulePersistencePort.save(any(RepaymentSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

            when(loanPersistencePort.findById(loanId)).thenReturn(Optional.of(testLoan));
            when(repaymentPaymentPersistencePort.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(repaymentPaymentPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(walletPaymentPort.collectRepayment(any(), any(), any(), any(), any(), any())).thenReturn("wallet-tx-1");

            RepaymentSchedule result = loanManagementService.processRepayment(
                    scheduleId, new BigDecimal("1078000"), "repayment-key-1");

            assertThat(result.getStatus()).isEqualTo(RepaymentStatus.FULLY_PAID);
            assertThat(result.getPaidAmount()).isEqualByComparingTo(new BigDecimal("1078000"));
            assertThat(result.getPaidDate()).isNotNull();
            verify(repaymentSchedulePersistencePort).save(schedule);
        }

        @Test
        @DisplayName("should process partial repayment successfully")
        void shouldProcessPartialRepaymentSuccessfully() {
            UUID scheduleId = UUID.randomUUID();
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setId(scheduleId);
            schedule.setLoanId(loanId);
            schedule.setInstallmentNumber(1);
            schedule.setInstallmentAmount(new BigDecimal("1078000"));
            schedule.setPrincipalAmount(new BigDecimal("1000000"));
            schedule.setInterestAmount(new BigDecimal("78000"));
            schedule.setOutstandingPrincipal(new BigDecimal("12000000"));
            schedule.setDueDate(LocalDate.now().plusMonths(1));
            schedule.setStatus(RepaymentStatus.PENDING);
            schedule.setPaidAmount(BigDecimal.ZERO);
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setUpdatedAt(LocalDateTime.now());

            when(repaymentSchedulePersistencePort.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
            when(repaymentSchedulePersistencePort.save(any(RepaymentSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

            when(loanPersistencePort.findById(loanId)).thenReturn(Optional.of(testLoan));
            when(repaymentPaymentPersistencePort.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(repaymentPaymentPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(walletPaymentPort.collectRepayment(any(), any(), any(), any(), any(), any())).thenReturn("wallet-tx-2");

            RepaymentSchedule result = loanManagementService.processRepayment(
                    scheduleId, new BigDecimal("500000"), "repayment-key-2");

            assertThat(result.getStatus()).isEqualTo(RepaymentStatus.PARTIALLY_PAID);
            assertThat(result.getPaidAmount()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(result.getPaidDate()).isNull();
            verify(repaymentSchedulePersistencePort).save(schedule);
        }

        @Test
        @DisplayName("should throw exception when schedule not found")
        void shouldThrowExceptionWhenScheduleNotFound() {
            UUID scheduleId = UUID.randomUUID();

            when(repaymentSchedulePersistencePort.findByIdForUpdate(scheduleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanManagementService.processRepayment(
                    scheduleId, new BigDecimal("1000000"), "repayment-key-3"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Repayment schedule not found");

            verify(repaymentSchedulePersistencePort, never()).save(any(RepaymentSchedule.class));
        }

        @Test
        @DisplayName("should throw exception when repayment already fully paid")
        void shouldThrowExceptionWhenAlreadyFullyPaid() {
            UUID scheduleId = UUID.randomUUID();
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setId(scheduleId);
            schedule.setLoanId(loanId);
            schedule.setInstallmentAmount(new BigDecimal("1078000"));
            schedule.setStatus(RepaymentStatus.FULLY_PAID);
            schedule.setPaidAmount(new BigDecimal("1078000"));
            schedule.setPaidDate(LocalDate.now());

            when(repaymentSchedulePersistencePort.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));

            assertThatThrownBy(() -> loanManagementService.processRepayment(
                    scheduleId, new BigDecimal("100000"), "repayment-key-4"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Repayment already fully paid");

            verify(repaymentSchedulePersistencePort, never()).save(any(RepaymentSchedule.class));
        }

        @Test
        @DisplayName("should add to existing partial payment")
        void shouldAddToExistingPartialPayment() {
            UUID scheduleId = UUID.randomUUID();
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setId(scheduleId);
            schedule.setLoanId(loanId);
            schedule.setInstallmentNumber(1);
            schedule.setInstallmentAmount(new BigDecimal("1078000"));
            schedule.setStatus(RepaymentStatus.PARTIALLY_PAID);
            schedule.setPaidAmount(new BigDecimal("500000"));
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setUpdatedAt(LocalDateTime.now());

            when(repaymentSchedulePersistencePort.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
            when(repaymentSchedulePersistencePort.save(any(RepaymentSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

            when(loanPersistencePort.findById(loanId)).thenReturn(Optional.of(testLoan));
            when(repaymentPaymentPersistencePort.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(repaymentPaymentPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(walletPaymentPort.collectRepayment(any(), any(), any(), any(), any(), any())).thenReturn("wallet-tx-3");

            RepaymentSchedule result = loanManagementService.processRepayment(
                    scheduleId, new BigDecimal("578000"), "repayment-key-5");

            assertThat(result.getStatus()).isEqualTo(RepaymentStatus.FULLY_PAID);
            assertThat(result.getPaidAmount()).isEqualByComparingTo(new BigDecimal("1078000"));
            assertThat(result.getPaidDate()).isNotNull();
            verify(repaymentSchedulePersistencePort).save(schedule);
        }

        @Test
        @DisplayName("should reject repayment above remaining installment amount")
        void shouldRejectOverpayment() {
            UUID scheduleId = UUID.randomUUID();
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setId(scheduleId);
            schedule.setLoanId(loanId);
            schedule.setInstallmentAmount(new BigDecimal("1078000"));
            schedule.setStatus(RepaymentStatus.PENDING);
            schedule.setPaidAmount(new BigDecimal("500000"));

            when(repaymentSchedulePersistencePort.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));

            assertThatThrownBy(() -> loanManagementService.processRepayment(
                    scheduleId, new BigDecimal("578001"), "repayment-key-overpay"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds remaining");

            verifyNoInteractions(walletPaymentPort);
        }

        @Test
        @DisplayName("should replay completed repayment without charging wallet")
        void shouldReplayCompletedRepayment() {
            UUID scheduleId = UUID.randomUUID();
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setId(scheduleId);
            schedule.setLoanId(loanId);
            schedule.setInstallmentAmount(new BigDecimal("1078000"));
            schedule.setPaidAmount(new BigDecimal("500000"));
            schedule.setStatus(RepaymentStatus.PARTIALLY_PAID);

            RepaymentPayment payment = new RepaymentPayment();
            payment.setRepaymentScheduleId(scheduleId);
            payment.setAmount(new BigDecimal("578000"));
            payment.setIdempotencyKey("replay-key");
            payment.setStatus(RepaymentPaymentStatus.COMPLETED);

            when(repaymentPaymentPersistencePort.findByIdempotencyKey("replay-key"))
                    .thenReturn(Optional.of(payment));
            when(repaymentSchedulePersistencePort.findById(scheduleId)).thenReturn(Optional.of(schedule));

            assertThat(loanManagementService.processRepayment(
                    scheduleId, new BigDecimal("578000"), "replay-key")).isSameAs(schedule);
            verifyNoInteractions(walletPaymentPort);
            verify(repaymentSchedulePersistencePort, never()).findByIdForUpdate(scheduleId);
        }

        @Test
        @DisplayName("should mark wallet timeout for reconciliation and recover on retry")
        void shouldRecoverWalletFailure() {
            UUID scheduleId = UUID.randomUUID();
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setId(scheduleId);
            schedule.setLoanId(loanId);
            schedule.setInstallmentAmount(new BigDecimal("1078000"));
            schedule.setPrincipalAmount(new BigDecimal("1000000"));
            schedule.setInterestAmount(new BigDecimal("78000"));
            schedule.setPaidAmount(BigDecimal.ZERO);
            schedule.setStatus(RepaymentStatus.PENDING);

            when(repaymentPaymentPersistencePort.findByIdempotencyKey("recover-key"))
                    .thenReturn(Optional.empty(), Optional.of(reconciliationPayment(scheduleId)));
            when(repaymentSchedulePersistencePort.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
            when(repaymentSchedulePersistencePort.save(any(RepaymentSchedule.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(loanPersistencePort.findById(loanId)).thenReturn(Optional.of(testLoan));
            when(repaymentPaymentPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(repaymentPaymentPersistencePort.findByStatusIn(any()))
                    .thenReturn(List.of(reconciliationPayment(scheduleId)));
            when(walletPaymentPort.collectRepayment(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("wallet timeout"))
                    .thenReturn("wallet-recovered");

            assertThatThrownBy(() -> loanManagementService.processRepayment(
                    scheduleId, new BigDecimal("1078000"), "recover-key"))
                    .isInstanceOf(RepaymentProcessingException.class);
            verify(repaymentPaymentPersistencePort, atLeastOnce()).save(argThat(payment ->
                    payment.getStatus() == RepaymentPaymentStatus.RECONCILIATION_REQUIRED));

            loanManagementService.reconcileRepayments();

            assertThat(schedule.getStatus()).isEqualTo(RepaymentStatus.FULLY_PAID);
            verify(walletPaymentPort, times(2)).collectRepayment(any(), any(), any(), any(), any(), any());
        }

        private RepaymentPayment reconciliationPayment(UUID scheduleId) {
            RepaymentPayment payment = new RepaymentPayment();
            payment.setId(UUID.randomUUID());
            payment.setRepaymentScheduleId(scheduleId);
            payment.setLoanId(loanId);
            payment.setUserId(testLoan.getUserId());
            payment.setAmount(new BigDecimal("1078000"));
            payment.setIdempotencyKey("recover-key");
            payment.setStatus(RepaymentPaymentStatus.RECONCILIATION_REQUIRED);
            return payment;
        }
    }
}
