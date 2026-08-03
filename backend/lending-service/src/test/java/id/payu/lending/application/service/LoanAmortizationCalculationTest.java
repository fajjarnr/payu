package id.payu.lending.application.service;

import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.LoanStatus;
import id.payu.lending.domain.model.LoanType;
import id.payu.lending.domain.model.RepaymentSchedule;
import id.payu.lending.domain.model.RepaymentStatus;
import id.payu.lending.domain.port.out.LoanPersistencePort;
import id.payu.lending.domain.port.out.LoanEventPublisherPort;
import id.payu.lending.domain.port.out.RepaymentPaymentPersistencePort;
import id.payu.lending.domain.port.out.RepaymentSchedulePersistencePort;
import id.payu.lending.domain.port.out.WalletPaymentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Financial calculation accuracy tests for LoanManagementService.
 * Verifies amortization schedule generation produces correct interest/principal splits,
 * proper rounding, and last-installment residual handling.
 *
 * Complements LoanManagementServiceTest which covers CRUD/persistence behavior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanManagementService — Amortization Calculations")
class LoanAmortizationCalculationTest {

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
    private LoanManagementService service;

    private UUID loanId;

    @BeforeEach
    void setUp() {
        loanId = UUID.randomUUID();
    }

    private Loan buildLoan(BigDecimal principal, BigDecimal interestRate, int tenureMonths,
                           BigDecimal monthlyInstallment) {
        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setExternalId("CALC-TEST");
        loan.setUserId(UUID.randomUUID());
        loan.setType(LoanType.PERSONAL_LOAN);
        loan.setPrincipalAmount(principal);
        loan.setInterestRate(interestRate);
        loan.setTenureMonths(tenureMonths);
        loan.setMonthlyInstallment(monthlyInstallment);
        loan.setOutstandingBalance(principal);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setPurpose("Test");
        loan.setDisbursementDate(LocalDate.of(2025, 1, 15));
        loan.setMaturityDate(LocalDate.of(2025, 1, 15).plusMonths(tenureMonths));
        loan.setCreatedAt(LocalDateTime.now());
        loan.setUpdatedAt(LocalDateTime.now());
        return loan;
    }

    private List<RepaymentSchedule> generateSchedule(Loan loan) {
        when(loanPersistencePort.findById(loanId)).thenReturn(Optional.of(loan));
        when(repaymentSchedulePersistencePort.save(any(RepaymentSchedule.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        return service.createRepaymentSchedule(loanId);
    }

    // ========================================================================
    // Exact amortization verification: 3-month, 12% annual, principal 3,000,000
    // ========================================================================
    @Nested
    @DisplayName("3-month 12% loan — exact figures")
    class ThreeMonthTwelvePercent {

        // monthlyRate = 0.12 / 12 = 0.0100000000
        // Month 1: interest = 3,000,000 × 0.01 = 30,000.00; principal = 1,020,100 - 30,000 = 990,100.00
        // Month 2: out = 2,009,900; interest = 20,099.00; principal = 1,000,001.00
        // Month 3: out = 1,009,899; interest = 10,098.99; principal = 1,009,899 (last = outstanding)

        private List<RepaymentSchedule> schedules;

        @BeforeEach
        void generate() {
            Loan loan = buildLoan(
                    new BigDecimal("3000000"),
                    new BigDecimal("0.12"),
                    3,
                    new BigDecimal("1020100")
            );
            schedules = generateSchedule(loan);
        }

        @Test
        @DisplayName("generates exactly 3 installments")
        void correctSize() {
            assertThat(schedules).hasSize(3);
        }

        @Test
        @DisplayName("installment 1: interest=30000.00, principal=990100.00, outstanding=3000000")
        void installmentOne() {
            RepaymentSchedule s = schedules.get(0);
            assertThat(s.getInstallmentNumber()).isEqualTo(1);
            assertThat(s.getOutstandingPrincipal()).isEqualByComparingTo(new BigDecimal("3000000"));
            assertThat(s.getInterestAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));
            assertThat(s.getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("990100.00"));
        }

        @Test
        @DisplayName("installment 2: interest=20099.00, principal=1000001.00, outstanding=2009900")
        void installmentTwo() {
            RepaymentSchedule s = schedules.get(1);
            assertThat(s.getInstallmentNumber()).isEqualTo(2);
            assertThat(s.getOutstandingPrincipal()).isEqualByComparingTo(new BigDecimal("2009900"));
            assertThat(s.getInterestAmount()).isEqualByComparingTo(new BigDecimal("20099.00"));
            assertThat(s.getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("1000001.00"));
        }

        @Test
        @DisplayName("installment 3 (last): principal forced to outstanding balance 1009899.00")
        void installmentThree() {
            RepaymentSchedule s = schedules.get(2);
            assertThat(s.getInstallmentNumber()).isEqualTo(3);
            assertThat(s.getOutstandingPrincipal()).isEqualByComparingTo(new BigDecimal("1009899"));
            assertThat(s.getInterestAmount()).isEqualByComparingTo(new BigDecimal("10098.99"));
            // Last installment: principal = outstanding (not installment - interest)
            assertThat(s.getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("1009899.00"));
        }

        @Test
        @DisplayName("sum of all principal amounts equals loan principal")
        void principalSumEqualsLoanPrincipal() {
            BigDecimal totalPrincipal = schedules.stream()
                    .map(RepaymentSchedule::getPrincipalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalPrincipal).isEqualByComparingTo(new BigDecimal("3000000"));
        }

        @Test
        @DisplayName("due dates are monthly from disbursement")
        void dueDatesMonthly() {
            LocalDate startDate = LocalDate.of(2025, 1, 15);
            for (int i = 0; i < schedules.size(); i++) {
                assertThat(schedules.get(i).getDueDate()).isEqualTo(startDate.plusMonths(i + 1));
            }
        }

        @Test
        @DisplayName("all installments have PENDING status")
        void allPending() {
            assertThat(schedules).allSatisfy(s ->
                    assertThat(s.getStatus()).isEqualTo(RepaymentStatus.PENDING));
        }
    }

    // ========================================================================
    // Zero interest-rate edge case
    // ========================================================================
    @Nested
    @DisplayName("Zero interest rate")
    class ZeroInterest {

        private List<RepaymentSchedule> schedules;

        @BeforeEach
        void generate() {
            Loan loan = buildLoan(
                    new BigDecimal("3000000"),
                    new BigDecimal("0.00"),
                    3,
                    new BigDecimal("1000000")
            );
            schedules = generateSchedule(loan);
        }

        @Test
        @DisplayName("all interest amounts are zero")
        void allInterestZero() {
            assertThat(schedules).allSatisfy(s ->
                    assertThat(s.getInterestAmount()).isEqualByComparingTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("first two installment principal = monthly installment, last = outstanding")
        void principalSplit() {
            assertThat(schedules.get(0).getPrincipalAmount())
                    .isEqualByComparingTo(new BigDecimal("1000000"));
            assertThat(schedules.get(1).getPrincipalAmount())
                    .isEqualByComparingTo(new BigDecimal("1000000"));
            // Last: outstanding = 3M - 1M - 1M = 1M (forced)
            assertThat(schedules.get(2).getPrincipalAmount())
                    .isEqualByComparingTo(new BigDecimal("1000000"));
        }

        @Test
        @DisplayName("sum of principal equals loan amount")
        void principalSum() {
            BigDecimal total = schedules.stream()
                    .map(RepaymentSchedule::getPrincipalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(total).isEqualByComparingTo(new BigDecimal("3000000"));
        }
    }

    // ========================================================================
    // Single-month tenure
    // ========================================================================
    @Nested
    @DisplayName("1-month tenure")
    class SingleMonth {

        private List<RepaymentSchedule> schedules;

        @BeforeEach
        void generate() {
            Loan loan = buildLoan(
                    new BigDecimal("1000000"),
                    new BigDecimal("0.12"),
                    1,
                    new BigDecimal("1010000")
            );
            schedules = generateSchedule(loan);
        }

        @Test
        @DisplayName("generates exactly 1 installment")
        void singleInstallment() {
            assertThat(schedules).hasSize(1);
        }

        @Test
        @DisplayName("principal equals full outstanding balance (last-installment rule)")
        void principalEqualsOutstanding() {
            RepaymentSchedule s = schedules.get(0);
            assertThat(s.getPrincipalAmount()).isEqualByComparingTo(new BigDecimal("1000000"));
        }

        @Test
        @DisplayName("interest = 1,000,000 × 0.01 = 10,000.00")
        void correctInterest() {
            assertThat(schedules.get(0).getInterestAmount())
                    .isEqualByComparingTo(new BigDecimal("10000.00"));
        }
    }

    // ========================================================================
    // 12-month 14% loan invariants (the default test loan from other tests)
    // ========================================================================
    @Nested
    @DisplayName("12-month 14% loan — invariant checks")
    class TwelveMonthFourteenPercent {

        private List<RepaymentSchedule> schedules;
        private static final BigDecimal PRINCIPAL = new BigDecimal("12000000");
        private static final BigDecimal INTEREST_RATE = new BigDecimal("0.14");
        private static final BigDecimal MONTHLY_INSTALLMENT = new BigDecimal("1078000");
        private static final int TENURE = 12;

        @BeforeEach
        void generate() {
            Loan loan = buildLoan(PRINCIPAL, INTEREST_RATE, TENURE, MONTHLY_INSTALLMENT);
            schedules = generateSchedule(loan);
        }

        @Test
        @DisplayName("generates 12 installments with sequential numbers")
        void correctSizeAndNumbers() {
            assertThat(schedules).hasSize(12);
            for (int i = 0; i < 12; i++) {
                assertThat(schedules.get(i).getInstallmentNumber()).isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("sum of principal amounts equals loan principal exactly")
        void totalPrincipalEqualsLoanAmount() {
            BigDecimal totalPrincipal = schedules.stream()
                    .map(RepaymentSchedule::getPrincipalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalPrincipal).isEqualByComparingTo(PRINCIPAL);
        }

        @Test
        @DisplayName("interest decreases over time (amortizing behaviour)")
        void interestDecreases() {
            for (int i = 1; i < schedules.size(); i++) {
                BigDecimal prevInterest = schedules.get(i - 1).getInterestAmount();
                BigDecimal currInterest = schedules.get(i).getInterestAmount();
                assertThat(currInterest)
                        .as("installment %d interest <= installment %d", i + 1, i)
                        .isLessThanOrEqualTo(prevInterest);
            }
        }

        @Test
        @DisplayName("outstanding principal decreases each month")
        void outstandingDecreases() {
            for (int i = 1; i < schedules.size(); i++) {
                assertThat(schedules.get(i).getOutstandingPrincipal())
                        .as("outstanding at installment %d < %d", i + 1, i)
                        .isLessThan(schedules.get(i - 1).getOutstandingPrincipal());
            }
        }

        @Test
        @DisplayName("each interest = outstanding × monthlyRate rounded to 2dp")
        void interestCalculationAccuracy() {
            BigDecimal monthlyRate = INTEREST_RATE.divide(new BigDecimal("12"), 10, RoundingMode.HALF_EVEN);
            for (RepaymentSchedule s : schedules) {
                BigDecimal expectedInterest = s.getOutstandingPrincipal()
                        .multiply(monthlyRate)
                        .setScale(2, RoundingMode.HALF_EVEN);
                assertThat(s.getInterestAmount())
                        .as("installment %d interest", s.getInstallmentNumber())
                        .isEqualByComparingTo(expectedInterest);
            }
        }

        @Test
        @DisplayName("non-last installments: principal = installment - interest")
        void principalEqualsInstallmentMinusInterest() {
            for (int i = 0; i < schedules.size() - 1; i++) {
                RepaymentSchedule s = schedules.get(i);
                BigDecimal expectedPrincipal = MONTHLY_INSTALLMENT.subtract(s.getInterestAmount());
                assertThat(s.getPrincipalAmount())
                        .as("installment %d principal", s.getInstallmentNumber())
                        .isEqualByComparingTo(expectedPrincipal);
            }
        }

        @Test
        @DisplayName("last installment: principal = remaining outstanding balance")
        void lastInstallmentPrincipalEqualsOutstanding() {
            RepaymentSchedule last = schedules.get(schedules.size() - 1);
            assertThat(last.getPrincipalAmount())
                    .isEqualByComparingTo(last.getOutstandingPrincipal());
        }

        @Test
        @DisplayName("first installment has interest 140,000.00 (12M × 14%/12)")
        void firstInstallmentInterest() {
            assertThat(schedules.get(0).getInterestAmount())
                    .isEqualByComparingTo(new BigDecimal("140000.00"));
        }

        @Test
        @DisplayName("first outstanding principal = loan principal")
        void firstOutstandingEqualsPrincipal() {
            assertThat(schedules.get(0).getOutstandingPrincipal())
                    .isEqualByComparingTo(PRINCIPAL);
        }

        @Test
        @DisplayName("non-last installment amounts equal monthly installment, last = principal + interest (BUG-BE-009)")
        void allInstallmentAmountsMatch() {
            // Non-last installments should equal the standard monthly installment
            for (int i = 0; i < schedules.size() - 1; i++) {
                assertThat(schedules.get(i).getInstallmentAmount())
                        .as("installment %d amount", i + 1)
                        .isEqualByComparingTo(MONTHLY_INSTALLMENT);
            }
            // Last installment = remaining outstanding + interest (may differ from monthly)
            RepaymentSchedule last = schedules.get(schedules.size() - 1);
            BigDecimal expectedLast = last.getPrincipalAmount().add(last.getInterestAmount());
            assertThat(last.getInstallmentAmount())
                    .as("last installment amount = principal + interest")
                    .isEqualByComparingTo(expectedLast);
        }

        @Test
        @DisplayName("all loan IDs match")
        void allLoanIdsMatch() {
            assertThat(schedules).allSatisfy(s ->
                    assertThat(s.getLoanId()).isEqualTo(loanId));
        }
    }

    // ========================================================================
    // Repayment overpayment edge case (not covered by existing tests)
    // ========================================================================
    @Nested
    @DisplayName("processRepayment — rejects overpayment")
    class OverpaymentCapping {

        @Test
        @DisplayName("overpayment is rejected")
        void overpaymentRejected() {
            UUID scheduleId = UUID.randomUUID();
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setId(scheduleId);
            schedule.setLoanId(loanId);
            schedule.setInstallmentNumber(1);
            schedule.setInstallmentAmount(new BigDecimal("1000000"));
            schedule.setStatus(RepaymentStatus.PENDING);
            schedule.setPaidAmount(BigDecimal.ZERO);
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setUpdatedAt(LocalDateTime.now());

            when(repaymentSchedulePersistencePort.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
            // Pay 1,500,000 on a 1,000,000 installment
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.processRepayment(
                    scheduleId, new BigDecimal("1500000"), "overpay-key"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds remaining");
        }

        @Test
        @DisplayName("repayment with null paidAmount initialises correctly")
        void nullPaidAmountInitialises() {
            UUID scheduleId = UUID.randomUUID();
            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setId(scheduleId);
            schedule.setLoanId(loanId);
            schedule.setInstallmentNumber(1);
            schedule.setInstallmentAmount(new BigDecimal("1000000"));
            schedule.setStatus(RepaymentStatus.PENDING);
            schedule.setPaidAmount(null); // null, not zero
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setUpdatedAt(LocalDateTime.now());

            when(repaymentSchedulePersistencePort.findByIdForUpdate(scheduleId)).thenReturn(Optional.of(schedule));
            when(repaymentSchedulePersistencePort.save(any(RepaymentSchedule.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            when(loanPersistencePort.findById(loanId)).thenReturn(Optional.of(buildLoan(
                    new BigDecimal("12000000"), new BigDecimal("0.14"), 12, new BigDecimal("1078000"))));
            when(repaymentPaymentPersistencePort.findByIdempotencyKey(any())).thenReturn(Optional.empty());
            when(repaymentPaymentPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(walletPaymentPort.collectRepayment(any(), any(), any(), any(), any(), any())).thenReturn("wallet-tx");

            RepaymentSchedule result = service.processRepayment(
                    scheduleId, new BigDecimal("500000"), "partial-key");

            assertThat(result.getStatus()).isEqualTo(RepaymentStatus.PARTIALLY_PAID);
            assertThat(result.getPaidAmount()).isEqualByComparingTo(new BigDecimal("500000"));
        }
    }
}
