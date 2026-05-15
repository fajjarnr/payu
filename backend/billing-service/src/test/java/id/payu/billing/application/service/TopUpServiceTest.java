package id.payu.billing.application.service;

import id.payu.billing.adapter.persistence.entity.BillPaymentEntity;
import id.payu.billing.domain.model.BillerType;
import id.payu.billing.domain.port.out.BillPaymentPersistencePort;
import id.payu.billing.domain.port.out.BillerPort;
import id.payu.billing.domain.port.out.PaymentEventPort;
import id.payu.billing.domain.port.out.WalletPort;
import id.payu.billing.dto.TopUpRequest;
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
@DisplayName("Top-up Service Unit Tests")
class TopUpServiceTest {

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
    @DisplayName("Create Top-up Tests")
    class CreateTopUpTests {

        @Test
        @DisplayName("should create top-up successfully for GoPay")
        void shouldCreateGoPayTopUpSuccessfully() {
            TopUpRequest request = new TopUpRequest(
                "account-123",
                "GOPAY",
                "08123456789",
                new BigDecimal("100000")
            );

            when(walletPort.reserveBalance(eq("account-123"), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));

            BillPaymentEntity payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals("account-123", payment.getAccountId());
            assertEquals(BillerType.GOPAY, payment.getBillerType());
            assertEquals("08123456789", payment.getCustomerId());
            assertEquals(new BigDecimal("100000"), payment.getAmount());
            assertEquals(BillPaymentEntity.PaymentStatus.COMPLETED, payment.getStatus());
            assertNotNull(payment.getReferenceNumber());
            assertTrue(payment.getReferenceNumber().startsWith("BILL"));
            
            verify(walletPort).reserveBalance(eq("account-123"), any(BigDecimal.class), any(String.class));
        }

        @Test
        @DisplayName("should create top-up successfully for OVO")
        void shouldCreateOVOTopUpSuccessfully() {
            TopUpRequest request = new TopUpRequest(
                "account-456",
                "OVO",
                "08987654321",
                new BigDecimal("50000")
            );

            when(walletPort.reserveBalance(eq("account-456"), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult("res-456", "RESERVED"));

            BillPaymentEntity payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals(BillerType.OVO, payment.getBillerType());
            assertEquals(BillPaymentEntity.PaymentStatus.COMPLETED, payment.getStatus());
        }

        @Test
        @DisplayName("should create top-up successfully for DANA")
        void shouldCreateDNATopUpSuccessfully() {
            TopUpRequest request = new TopUpRequest(
                "account-789",
                "DANA",
                "08555555555",
                new BigDecimal("200000")
            );

            when(walletPort.reserveBalance(eq("account-789"), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult("res-789", "RESERVED"));

            BillPaymentEntity payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals(BillerType.DANA, payment.getBillerType());
            assertEquals(BillPaymentEntity.PaymentStatus.COMPLETED, payment.getStatus());
        }

        @Test
        @DisplayName("should create top-up successfully for LinkAja")
        void shouldCreateLinkAjaTopUpSuccessfully() {
            TopUpRequest request = new TopUpRequest(
                "account-999",
                "LINKAJA",
                "08777777777",
                new BigDecimal("75000")
            );

            when(walletPort.reserveBalance(eq("account-999"), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult("res-999", "RESERVED"));

            BillPaymentEntity payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals(BillerType.LINKAJA, payment.getBillerType());
            assertEquals(BillPaymentEntity.PaymentStatus.COMPLETED, payment.getStatus());
        }

        @Test
        @DisplayName("should fail top-up when wallet fails to reserve balance")
        void shouldFailTopUpWhenWalletFailsToReserve() {
            TopUpRequest request = new TopUpRequest(
                "account-123",
                "GOPAY",
                "08123456789",
                new BigDecimal("100000")
            );

            when(walletPort.reserveBalance(eq("account-123"), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult(null, "FAILED"));

            BillPaymentEntity payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals(BillPaymentEntity.PaymentStatus.FAILED, payment.getStatus());
            assertEquals("Failed to reserve balance", payment.getFailureReason());
        }

        @Test
        @DisplayName("should fail top-up when wallet service is unavailable")
        void shouldFailTopUpWhenWalletServiceUnavailable() {
            TopUpRequest request = new TopUpRequest(
                "account-123",
                "OVO",
                "08123456789",
                new BigDecimal("50000")
            );

            when(walletPort.reserveBalance(any(), any(BigDecimal.class), any(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

            BillPaymentEntity payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals(BillPaymentEntity.PaymentStatus.FAILED, payment.getStatus());
            assertEquals("Top-up processing failed: Connection refused", payment.getFailureReason());
        }

        @Test
        @DisplayName("should throw exception for unknown provider")
        void shouldThrowExceptionForUnknownProvider() {
            TopUpRequest request = new TopUpRequest(
                "account-123",
                "UNKNOWN",
                "08123456789",
                new BigDecimal("100000")
            );

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createTopUp(request)
            );
            
            assertTrue(exception.getMessage().contains("Unknown e-wallet provider"));
        }
    }

    @Nested
    @DisplayName("Top-up Admin Fee Calculation Tests")
    class TopUpFeeCalculationTests {

        @Test
        @DisplayName("should charge Rp 1.000 admin fee for top-up <= Rp 100.000")
        void shouldCharge1000AdminFeeForSmallTopUp() {
            TopUpRequest request = new TopUpRequest(
                "account-123",
                "GOPAY",
                "08123456789",
                new BigDecimal("100000")
            );

            when(walletPort.reserveBalance(any(), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));

            BillPaymentEntity payment = paymentService.createTopUp(request);

            assertEquals(new BigDecimal("1000"), payment.getAdminFee());
            assertEquals(new BigDecimal("101000"), payment.getTotalAmount());
        }

        @Test
        @DisplayName("should charge Rp 1.500 admin fee for top-up Rp 100.001 - Rp 500.000")
        void shouldCharge1500AdminFeeForMediumTopUp() {
            TopUpRequest request = new TopUpRequest(
                "account-123",
                "OVO",
                "08123456789",
                new BigDecimal("300000")
            );

            when(walletPort.reserveBalance(any(), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));

            BillPaymentEntity payment = paymentService.createTopUp(request);

            assertEquals(new BigDecimal("1500"), payment.getAdminFee());
            assertEquals(new BigDecimal("301500"), payment.getTotalAmount());
        }

        @Test
        @DisplayName("should charge Rp 2.000 admin fee for top-up > Rp 500.000")
        void shouldCharge2000AdminFeeForLargeTopUp() {
            TopUpRequest request = new TopUpRequest(
                "account-123",
                "DANA",
                "08123456789",
                new BigDecimal("1000000")
            );

            when(walletPort.reserveBalance(any(), any(BigDecimal.class), any(String.class)))
                .thenReturn(new WalletPort.ReserveResult("res-123", "RESERVED"));

            BillPaymentEntity payment = paymentService.createTopUp(request);

            assertEquals(new BigDecimal("2000"), payment.getAdminFee());
            assertEquals(new BigDecimal("1002000"), payment.getTotalAmount());
        }
    }
}