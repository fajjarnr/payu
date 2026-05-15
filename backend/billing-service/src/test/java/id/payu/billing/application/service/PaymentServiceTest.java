package id.payu.billing.application.service;

import id.payu.billing.adapter.persistence.entity.BillPaymentEntity;
import id.payu.billing.domain.model.BillerType;
import id.payu.billing.domain.port.out.BillPaymentPersistencePort;
import id.payu.billing.domain.port.out.BillerPort;
import id.payu.billing.domain.port.out.PaymentEventPort;
import id.payu.billing.domain.port.out.WalletPort;
import id.payu.billing.dto.CreatePaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

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
        lenient().when(persistencePort.save(any(BillPaymentEntity.class)))
                .thenAnswer(invocation -> {
                    BillPaymentEntity p = invocation.getArgument(0);
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
            BillPaymentEntity payment = paymentService.createPayment(request);

            // Then
            assertNotNull(payment);
            assertEquals("account-123", payment.getAccountId());
            assertEquals(BillerType.PLN, payment.getBillerType());
            assertEquals("12345678901234", payment.getCustomerId());
            assertEquals(new BigDecimal("100000"), payment.getAmount());
            assertEquals(BillPaymentEntity.PaymentStatus.COMPLETED, payment.getStatus());
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
            BillPaymentEntity payment = paymentService.createPayment(request);

            // Then
            assertNotNull(payment);
            assertEquals(BillPaymentEntity.PaymentStatus.FAILED, payment.getStatus());
            assertEquals("Failed to reserve balance", payment.getFailureReason());
        }

        @Test
        @DisplayName("should fail payment when wallet service is unavailable")
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
            BillPaymentEntity payment = paymentService.createPayment(request);

            // Then
            assertNotNull(payment);
            assertEquals(BillPaymentEntity.PaymentStatus.FAILED, payment.getStatus());
            assertEquals("Payment processing failed: Connection refused", payment.getFailureReason());
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
            BillPaymentEntity payment = paymentService.createPayment(request);

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
            BillPaymentEntity payment = paymentService.createPayment(request);

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
            BillPaymentEntity payment = paymentService.createPayment(request);

            // Then
            assertEquals(new BigDecimal("2000"), payment.getAdminFee());
            assertEquals(new BigDecimal("77000"), payment.getTotalAmount());
        }
    }
}
