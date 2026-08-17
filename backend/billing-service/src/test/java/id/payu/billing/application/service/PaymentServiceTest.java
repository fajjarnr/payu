package id.payu.billing.application.service;

import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.model.BillerType;
import id.payu.billing.domain.model.PaymentStatus;
import id.payu.billing.domain.port.out.BillPaymentPersistencePort;
import id.payu.billing.domain.port.out.BillerPort;
import id.payu.billing.domain.port.out.PaymentEventPort;
import id.payu.billing.domain.port.out.WalletPort;
import id.payu.billing.interfaces.dto.CreatePaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service Unit Tests")
class PaymentServiceTest {

    @InjectMocks
    PaymentService paymentService;

    @Mock
    BillPaymentPersistencePort persistencePort;

    @Mock
    WalletPort walletPort;

    @Mock
    PaymentEventPort eventPort;

    @Mock
    BillerPort billerPort;

    @BeforeEach
    void setup() {
        lenient().when(persistencePort.save(any(BillPayment.class)))
                .thenAnswer(invocation -> {
                    BillPayment p = invocation.getArgument(0);
                    if (p.getId() == null) {
                        p.setId(java.util.UUID.randomUUID());
                    }
                    if (p.getReferenceNumber() == null) {
                        p.setReferenceNumber("BILL" + System.currentTimeMillis());
                    }
                    return p;
                });

        lenient().when(billerPort.pay(any(), any(), any(), any()))
                .thenReturn(new BillerPort.PaymentResult("00", "Success", "btx-123", "COMPLETED", java.time.Instant.now()));
    }

    @Nested
    @DisplayName("Create Payment Tests")
    class CreatePaymentTests {

        @Test
        @DisplayName("should resume the persisted payment with the same reference after a provider timeout")
        void shouldResumePersistedPaymentAfterProviderTimeout() {
            String idempotencyKey = UUID.randomUUID().toString();
            CreatePaymentRequest request = new CreatePaymentRequest(
                    "account-123", "PLN", "12345678901234", new BigDecimal("100000"));
            BillPayment checkpoint = new BillPayment();
            checkpoint.setId(UUID.randomUUID());
            checkpoint.setAccountId(request.accountId());
            checkpoint.setBillerType(BillerType.PLN);
            checkpoint.setCustomerId(request.customerId());
            checkpoint.setAmount(request.amount());
            checkpoint.setAdminFee(new BigDecimal("2500"));
            checkpoint.setTotalAmount(new BigDecimal("102500"));
            checkpoint.setStatus(PaymentStatus.PROCESSING);
            checkpoint.setReferenceNumber("BILL-STABLE-001");
            checkpoint.setIdempotencyKey(idempotencyKey);
            checkpoint.setWalletReservationId("res-123");

            when(persistencePort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(checkpoint));
            when(billerPort.pay(eq("PLN"), eq(request.customerId()), eq(request.amount()), eq("BILL-STABLE-001")))
                    .thenThrow(new RuntimeException("provider timeout"));

            BillPayment payment = paymentService.createPayment(request, idempotencyKey);

            assertSame(checkpoint, payment);
            assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
            verify(billerPort).pay("PLN", request.customerId(), request.amount(), "BILL-STABLE-001");
        }

        @Test
        @DisplayName("should retry the durable event without calling the provider again")
        void shouldRetryDurableEventWithoutRepeatingProvider() {
            String idempotencyKey = UUID.randomUUID().toString();
            CreatePaymentRequest request = new CreatePaymentRequest(
                    "account-123", "PLN", "12345678901234", new BigDecimal("100000"));
            BillPayment checkpoint = new BillPayment();
            checkpoint.setId(UUID.randomUUID());
            checkpoint.setAccountId(request.accountId());
            checkpoint.setBillerType(BillerType.PLN);
            checkpoint.setCustomerId(request.customerId());
            checkpoint.setAmount(request.amount());
            checkpoint.setStatus(PaymentStatus.COMPLETED);
            checkpoint.setReferenceNumber("BILL-STABLE-002");
            checkpoint.setIdempotencyKey(idempotencyKey);

            when(persistencePort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(checkpoint));

            BillPayment payment = paymentService.createPayment(request, idempotencyKey);

            assertSame(checkpoint, payment);
            assertTrue(payment.isEventPublished());
            verify(eventPort).publishPaymentEvent(checkpoint);
            verifyNoInteractions(walletPort, billerPort);
        }

        @Test
        @DisplayName("should retry a failed payment event without calling providers")
        void shouldRetryFailedPaymentEventWithoutCallingProviders() {
            String idempotencyKey = UUID.randomUUID().toString();
            CreatePaymentRequest request = new CreatePaymentRequest(
                    "account-123", "PLN", "12345678901234", new BigDecimal("100000"));
            BillPayment checkpoint = new BillPayment();
            checkpoint.setId(UUID.randomUUID());
            checkpoint.setAccountId(request.accountId());
            checkpoint.setBillerType(BillerType.PLN);
            checkpoint.setCustomerId(request.customerId());
            checkpoint.setAmount(request.amount());
            checkpoint.setStatus(PaymentStatus.FAILED);
            checkpoint.setFailureReason("Biller rejected");
            checkpoint.setReferenceNumber("BILL-FAILED-001");
            checkpoint.setIdempotencyKey(idempotencyKey);

            when(persistencePort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(checkpoint));

            BillPayment payment = paymentService.createPayment(request, idempotencyKey);

            assertSame(checkpoint, payment);
            assertTrue(payment.isEventPublished());
            verify(eventPort).publishPaymentEvent(checkpoint);
            verifyNoInteractions(walletPort, billerPort);
        }

        @Test
        @DisplayName("should create payment successfully when wallet reserves balance")
        void shouldCreatePaymentSuccessfully() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(
                "account-123",
                "PLN",
                "12345678901234",
                new BigDecimal("100000")
            );

            when(walletPort.reserveBalance(eq("account-123"), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertNotNull(payment);
            assertEquals("account-123", payment.getAccountId());
            assertEquals(BillerType.PLN, payment.getBillerType());
            assertEquals("12345678901234", payment.getCustomerId());
            assertEquals(new BigDecimal("100000"), payment.getAmount());
            assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
            assertNotNull(payment.getReferenceNumber());
            
            verify(walletPort).reserveBalance(eq("account-123"), any(BigDecimal.class), any(String.class));
        }

        @Test
        @DisplayName("should fail payment when wallet fails to reserve balance")
        void shouldFailPaymentWhenWalletFailsToReserve() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(
                "account-123",
                "PLN",
                "12345678901234",
                new BigDecimal("100000")
            );

            when(walletPort.reserveBalance(eq("account-123"), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult(null, "FAILED"));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertNotNull(payment);
            assertEquals(PaymentStatus.FAILED, payment.getStatus());
            assertEquals("Failed to reserve balance", payment.getFailureReason());
        }

        @Test
        @DisplayName("should retain payment checkpoint when wallet service is unavailable")
        void shouldFailPaymentWhenWalletServiceUnavailable() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(
                "account-123",
                "PLN",
                "12345678901234",
                new BigDecimal("100000")
            );

            when(walletPort.reserveBalance(any(), any(BigDecimal.class), any(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertNotNull(payment);
            assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
            assertTrue(payment.getFailureReason().startsWith("Reconciliation required:"));
        }

        @Test
        @DisplayName("should throw exception for unknown biller code")
        void shouldThrowExceptionForUnknownBiller() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(
                "account-123",
                "UNKNOWN_BILLER",
                "12345678901234",
                new BigDecimal("100000")
            );

            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPayment(request)
            );
            
            assertTrue(exception.getMessage().contains("Unknown biller"));
        }
    }

    @Nested
    @DisplayName("Admin Fee Calculation Tests")
    class AdminFeeCalculationTests {

        @Test
        @DisplayName("should calculate correct admin fee for electricity (PLN)")
        void shouldCalculateAdminFeeForElectricity() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(
                "account-123",
                "PLN",
                "12345678901234",
                new BigDecimal("100000")
            );

            when(walletPort.reserveBalance(any(), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertEquals(new BigDecimal("2500"), payment.getAdminFee());
            assertEquals(new BigDecimal("102500"), payment.getTotalAmount());
        }

        @Test
        @DisplayName("should have zero admin fee for mobile top-up")
        void shouldHaveZeroAdminFeeForMobileTopUp() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(
                "account-123",
                "TELKOMSEL",
                "08123456789",
                new BigDecimal("50000")
            );

            when(walletPort.reserveBalance(any(), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertEquals(BigDecimal.ZERO, payment.getAdminFee());
            assertEquals(new BigDecimal("50000"), payment.getTotalAmount());
        }

        @Test
        @DisplayName("should calculate correct admin fee for water (PDAM)")
        void shouldCalculateAdminFeeForWater() {
            // Given
            CreatePaymentRequest request = new CreatePaymentRequest(
                "account-123",
                "PDAM",
                "123456789",
                new BigDecimal("75000")
            );

            when(walletPort.reserveBalance(any(), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertEquals(new BigDecimal("2000"), payment.getAdminFee());
            assertEquals(new BigDecimal("77000"), payment.getTotalAmount());
        }
    }

    @Nested
    @DisplayName("BILL-RECON-001: reconcilePayments")
    class ReconcilePaymentsTests {

        @Test
        @DisplayName("scans only reconcilable payments (not every historical completed row)")
        void shouldQueryOnlyReconcilablePayments() {
            when(persistencePort.findReconcilableIn(java.util.List.of(
                    PaymentStatus.PENDING, PaymentStatus.PROCESSING))).thenReturn(java.util.List.of());
            when(persistencePort.findReconcilableIn(java.util.List.of(
                    PaymentStatus.COMPLETED, PaymentStatus.FAILED))).thenReturn(java.util.List.of());

            paymentService.reconcilePayments();

            verify(persistencePort).findReconcilableIn(java.util.List.of(
                    PaymentStatus.PENDING, PaymentStatus.PROCESSING));
            verify(persistencePort).findReconcilableIn(java.util.List.of(
                    PaymentStatus.COMPLETED, PaymentStatus.FAILED));
            // Must NOT scan all four statuses in one unbounded query
            verify(persistencePort, never()).findByStatusIn(any());
        }

        @Test
        @DisplayName("reconciles in-flight and unpublished-terminal payments")
        void shouldReconcileInflightAndUnpublishedTerminal() {
            BillPayment inflight = new BillPayment();
            inflight.setId(UUID.randomUUID());
            inflight.setAccountId("account-1");
            inflight.setBillerType(BillerType.PLN);
            inflight.setCustomerId("cust-1");
            inflight.setAmount(new BigDecimal("100000"));
            inflight.setAdminFee(new BigDecimal("2500"));
            inflight.setTotalAmount(new BigDecimal("102500"));
            inflight.setStatus(PaymentStatus.PROCESSING);
            inflight.setReferenceNumber("BILL-REC-001");

            BillPayment unpublishedCompleted = new BillPayment();
            unpublishedCompleted.setId(UUID.randomUUID());
            unpublishedCompleted.setAccountId("account-2");
            unpublishedCompleted.setBillerType(BillerType.PLN);
            unpublishedCompleted.setCustomerId("cust-2");
            unpublishedCompleted.setAmount(new BigDecimal("50000"));
            unpublishedCompleted.setAdminFee(new BigDecimal("2500"));
            unpublishedCompleted.setTotalAmount(new BigDecimal("52500"));
            unpublishedCompleted.setStatus(PaymentStatus.COMPLETED);
            unpublishedCompleted.setReferenceNumber("BILL-REC-002");

            when(persistencePort.findReconcilableIn(java.util.List.of(
                    PaymentStatus.PENDING, PaymentStatus.PROCESSING)))
                    .thenReturn(java.util.List.of(inflight));
            when(persistencePort.findReconcilableIn(java.util.List.of(
                    PaymentStatus.COMPLETED, PaymentStatus.FAILED)))
                    .thenReturn(java.util.List.of(unpublishedCompleted));

            paymentService.reconcilePayments();

            // COMPLETED + unpublished event -> publishPaymentEvent called
            verify(eventPort).publishPaymentEvent(unpublishedCompleted);
        }
    }
}
