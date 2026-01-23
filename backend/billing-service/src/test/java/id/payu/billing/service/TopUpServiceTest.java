package id.payu.billing.service;

import id.payu.billing.client.WalletClient;
import id.payu.billing.domain.BillPayment;
import id.payu.billing.domain.BillerType;
import id.payu.billing.dto.TopUpRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
@DisplayName("Top-up Service Unit Tests")
class TopUpServiceTest {

    @Inject
    PaymentService paymentService;

    @InjectMock
    @RestClient
    WalletClient walletClient;

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

            when(walletClient.reserveBalance(eq("account-123"), any()))
                .thenReturn(new WalletClient.ReserveResponse("res-123", "account-123", "ref-123", "RESERVED"));

            BillPayment payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals("account-123", payment.accountId);
            assertEquals(BillerType.GOPAY, payment.billerType);
            assertEquals("08123456789", payment.customerId);
            assertEquals(new BigDecimal("100000"), payment.amount);
            assertEquals(BillPayment.PaymentStatus.COMPLETED, payment.status);
            assertNotNull(payment.referenceNumber);
            assertTrue(payment.referenceNumber.startsWith("BILL"));
            
            verify(walletClient).reserveBalance(eq("account-123"), any());
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

            when(walletClient.reserveBalance(eq("account-456"), any()))
                .thenReturn(new WalletClient.ReserveResponse("res-456", "account-456", "ref-456", "RESERVED"));

            BillPayment payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals(BillerType.OVO, payment.billerType);
            assertEquals(BillPayment.PaymentStatus.COMPLETED, payment.status);
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

            when(walletClient.reserveBalance(eq("account-789"), any()))
                .thenReturn(new WalletClient.ReserveResponse("res-789", "account-789", "ref-789", "RESERVED"));

            BillPayment payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals(BillerType.DANA, payment.billerType);
            assertEquals(BillPayment.PaymentStatus.COMPLETED, payment.status);
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

            when(walletClient.reserveBalance(eq("account-999"), any()))
                .thenReturn(new WalletClient.ReserveResponse("res-999", "account-999", "ref-999", "RESERVED"));

            BillPayment payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals(BillerType.LINKAJA, payment.billerType);
            assertEquals(BillPayment.PaymentStatus.COMPLETED, payment.status);
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

            when(walletClient.reserveBalance(eq("account-123"), any()))
                .thenReturn(new WalletClient.ReserveResponse(null, "account-123", null, "FAILED"));

            BillPayment payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals(BillPayment.PaymentStatus.FAILED, payment.status);
            assertEquals("Failed to reserve balance", payment.failureReason);
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

            when(walletClient.reserveBalance(any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

            BillPayment payment = paymentService.createTopUp(request);

            assertNotNull(payment);
            assertEquals(BillPayment.PaymentStatus.FAILED, payment.status);
            assertEquals("Wallet service unavailable", payment.failureReason);
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

            when(walletClient.reserveBalance(any(), any()))
                .thenReturn(new WalletClient.ReserveResponse("res-123", "account-123", "ref-123", "RESERVED"));

            BillPayment payment = paymentService.createTopUp(request);

            assertEquals(new BigDecimal("1000"), payment.adminFee);
            assertEquals(new BigDecimal("101000"), payment.totalAmount);
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

            when(walletClient.reserveBalance(any(), any()))
                .thenReturn(new WalletClient.ReserveResponse("res-123", "account-123", "ref-123", "RESERVED"));

            BillPayment payment = paymentService.createTopUp(request);

            assertEquals(new BigDecimal("1500"), payment.adminFee);
            assertEquals(new BigDecimal("301500"), payment.totalAmount);
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

            when(walletClient.reserveBalance(any(), any()))
                .thenReturn(new WalletClient.ReserveResponse("res-123", "account-123", "ref-123", "RESERVED"));

            BillPayment payment = paymentService.createTopUp(request);

            assertEquals(new BigDecimal("2000"), payment.adminFee);
            assertEquals(new BigDecimal("1002000"), payment.totalAmount);
        }
    }
}