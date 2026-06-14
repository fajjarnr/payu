package id.payu.lending.application.service;

import id.payu.lending.domain.model.InstallmentCheckout;
import id.payu.lending.domain.model.CheckoutStatus;
import id.payu.lending.domain.model.InstallmentOption;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.PayLater;
import id.payu.lending.domain.model.PayLaterStatus;
import id.payu.lending.domain.port.out.InstallmentCheckoutPersistencePort;
import id.payu.lending.domain.port.out.LoanPersistencePort;
import id.payu.lending.domain.port.out.PayLaterPersistencePort;
import id.payu.lending.domain.port.out.RepaymentSchedulePersistencePort;
import id.payu.lending.exception.InstallmentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Installment Service Unit Tests")
class InstallmentServiceTest {

    private InstallmentService installmentService;

    @Mock
    PayLaterPersistencePort payLaterPersistencePort;

    @Mock
    LoanPersistencePort loanPersistencePort;

    @Mock
    RepaymentSchedulePersistencePort repaymentSchedulePersistencePort;

    @Mock
    InstallmentCheckoutPersistencePort checkoutPersistencePort;

    private UUID userId;
    private PayLater activePayLater;

    @BeforeEach
    void setup() {
        installmentService = new InstallmentService(
                payLaterPersistencePort,
                loanPersistencePort,
                repaymentSchedulePersistencePort,
                checkoutPersistencePort
        );

        userId = UUID.randomUUID();

        activePayLater = new PayLater();
        activePayLater.setId(UUID.randomUUID());
        activePayLater.setUserId(userId);
        activePayLater.setCreditLimit(new BigDecimal("5000000"));
        activePayLater.setUsedCredit(BigDecimal.ZERO);
        activePayLater.setAvailableCredit(new BigDecimal("5000000"));
        activePayLater.setStatus(PayLaterStatus.ACTIVE);
        activePayLater.setInterestRate(new BigDecimal("0.1200"));
        activePayLater.setBillingCycleDay(25);
        activePayLater.setCreatedAt(LocalDateTime.now());
        activePayLater.setUpdatedAt(LocalDateTime.now());

        lenient().when(loanPersistencePort.save(any(Loan.class)))
                .thenAnswer(inv -> {
                    Loan loan = inv.getArgument(0);
                    if (loan.getId() == null) loan.setId(UUID.randomUUID());
                    return loan;
                });

        lenient().when(payLaterPersistencePort.save(any(PayLater.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        lenient().when(checkoutPersistencePort.save(any(InstallmentCheckout.class)))
                .thenAnswer(inv -> {
                    InstallmentCheckout c = inv.getArgument(0);
                    if (c.getId() == null) c.setId(UUID.randomUUID());
                    return c;
                });

        lenient().when(repaymentSchedulePersistencePort.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ═══════════════════════════════════════════════════════
    //  Tenor Options Tests
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Tenor Options")
    class TenorOptionsTests {

        @Test
        @DisplayName("should return tenor options for eligible user")
        void shouldReturnTenorOptions() {
            when(payLaterPersistencePort.findByUserId(userId))
                    .thenReturn(Optional.of(activePayLater));

            List<InstallmentOption> options = installmentService
                    .getTenorOptions(userId, new BigDecimal("1200000"));

            assertFalse(options.isEmpty());
            assertTrue(options.size() <= 3);
            for (InstallmentOption opt : options) {
                assertTrue(opt.getTenor() > 0);
                assertTrue(opt.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0);
                assertTrue(opt.getTotalPayment().compareTo(new BigDecimal("1200000")) >= 0);
                assertTrue(opt.getTotalInterest().compareTo(BigDecimal.ZERO) >= 0);
            }
        }

        @Test
        @DisplayName("should throw when insufficient credit for tenor options")
        void shouldThrowWhenInsufficientCredit() {
            when(payLaterPersistencePort.findByUserId(userId))
                    .thenReturn(Optional.of(activePayLater));

            assertThrows(InstallmentException.class,
                    () -> installmentService.getTenorOptions(userId, new BigDecimal("10000000")));
        }

        @Test
        @DisplayName("should throw when no PayLater account")
        void shouldThrowWhenNoPayLater() {
            when(payLaterPersistencePort.findByUserId(userId))
                    .thenReturn(Optional.empty());

            assertThrows(InstallmentException.class,
                    () -> installmentService.getTenorOptions(userId, new BigDecimal("100000")));
        }

        @Test
        @DisplayName("should throw when PayLater is suspended")
        void shouldThrowWhenPayLaterSuspended() {
            activePayLater.setStatus(PayLaterStatus.SUSPENDED);
            when(payLaterPersistencePort.findByUserId(userId))
                    .thenReturn(Optional.of(activePayLater));

            assertThrows(InstallmentException.class,
                    () -> installmentService.getTenorOptions(userId, new BigDecimal("100000")));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Checkout Tests
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Checkout")
    class CheckoutTests {

        @Test
        @DisplayName("should create installment checkout successfully")
        void shouldCreateCheckoutSuccessfully() {
            when(payLaterPersistencePort.findByUserId(userId))
                    .thenReturn(Optional.of(activePayLater));
            when(checkoutPersistencePort.findByExternalOrderId("ORD-001"))
                    .thenReturn(Optional.empty());

            InstallmentCheckout result = installmentService.checkout(
                    userId, "partner-dolan", "ORD-001",
                    new BigDecimal("1200000"), 6);

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals(userId, result.getUserId());
            assertEquals("partner-dolan", result.getPartnerId());
            assertEquals(new BigDecimal("1200000"), result.getPurchaseAmount());
            assertEquals(6, result.getTenor());
            assertEquals(CheckoutStatus.DISBURSED, result.getStatus());
            assertNotNull(result.getLoanId());
            assertTrue(result.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0);

            // Verify loan was created
            verify(loanPersistencePort).save(any(Loan.class));
            // Verify repayment schedule was created (6 installments)
            verify(repaymentSchedulePersistencePort, times(6)).save(any());
            // Verify PayLater credit was debited
            verify(payLaterPersistencePort).save(any(PayLater.class));
        }

        @Test
        @DisplayName("should reject checkout with insufficient credit")
        void shouldRejectInsufficientCredit() {
            when(payLaterPersistencePort.findByUserId(userId))
                    .thenReturn(Optional.of(activePayLater));

            assertThrows(InstallmentException.class,
                    () -> installmentService.checkout(
                            userId, "partner-1", null,
                            new BigDecimal("10000000"), 3));
        }

        @Test
        @DisplayName("should reject invalid tenor")
        void shouldRejectInvalidTenor() {
            when(payLaterPersistencePort.findByUserId(userId))
                    .thenReturn(Optional.of(activePayLater));

            assertThrows(InstallmentException.class,
                    () -> installmentService.checkout(
                            userId, "partner-1", null,
                            new BigDecimal("500000"), 24));
        }

        @Test
        @DisplayName("should reject duplicate external order")
        void shouldRejectDuplicateOrder() {
            when(payLaterPersistencePort.findByUserId(userId))
                    .thenReturn(Optional.of(activePayLater));
            when(checkoutPersistencePort.findByExternalOrderId("ORD-DUP"))
                    .thenReturn(Optional.of(new InstallmentCheckout()));

            assertThrows(InstallmentException.class,
                    () -> installmentService.checkout(
                            userId, "partner-1", "ORD-DUP",
                            new BigDecimal("500000"), 3));
        }

        @Test
        @DisplayName("should debit PayLater available credit after checkout")
        void shouldDebitPayLaterCredit() {
            when(payLaterPersistencePort.findByUserId(userId))
                    .thenReturn(Optional.of(activePayLater));
            when(checkoutPersistencePort.findByExternalOrderId(any()))
                    .thenReturn(Optional.empty());

            BigDecimal amount = new BigDecimal("1000000");
            installmentService.checkout(userId, "partner-1", "ORD-002", amount, 3);

            assertEquals(new BigDecimal("1000000"), activePayLater.getUsedCredit());
            assertEquals(new BigDecimal("4000000"), activePayLater.getAvailableCredit());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Query Tests
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Query Checkouts")
    class QueryTests {

        @Test
        @DisplayName("should get checkout by ID")
        void shouldGetCheckoutById() {
            UUID checkoutId = UUID.randomUUID();
            InstallmentCheckout checkout = new InstallmentCheckout();
            checkout.setId(checkoutId);
            checkout.setUserId(userId);
            checkout.setStatus(CheckoutStatus.DISBURSED);

            when(checkoutPersistencePort.findById(checkoutId))
                    .thenReturn(Optional.of(checkout));

            InstallmentCheckout result = installmentService.getCheckout(checkoutId);
            assertEquals(checkoutId, result.getId());
        }

        @Test
        @DisplayName("should throw when checkout not found")
        void shouldThrowWhenCheckoutNotFound() {
            UUID checkoutId = UUID.randomUUID();
            when(checkoutPersistencePort.findById(checkoutId))
                    .thenReturn(Optional.empty());

            assertThrows(InstallmentException.class,
                    () -> installmentService.getCheckout(checkoutId));
        }

        @Test
        @DisplayName("should get checkouts by user")
        void shouldGetCheckoutsByUser() {
            InstallmentCheckout checkout = new InstallmentCheckout();
            checkout.setId(UUID.randomUUID());
            checkout.setUserId(userId);

            when(checkoutPersistencePort.findByUserId(userId))
                    .thenReturn(List.of(checkout));

            List<InstallmentCheckout> result = installmentService.getCheckoutsByUser(userId);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("should return empty list when no checkouts")
        void shouldReturnEmptyWhenNoCheckouts() {
            when(checkoutPersistencePort.findByUserId(userId))
                    .thenReturn(List.of());

            List<InstallmentCheckout> result = installmentService.getCheckoutsByUser(userId);
            assertTrue(result.isEmpty());
        }
    }
}
