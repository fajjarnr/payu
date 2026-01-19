package id.payu.billing.service;

import id.payu.billing.client.WalletClient;
import id.payu.billing.domain.BillPayment;
import id.payu.billing.domain.BillerType;
import id.payu.billing.dto.CreatePaymentRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
@DisplayName("Payment Service Unit Tests")
class PaymentServiceTest {

    @Inject
    PaymentService paymentService;

    @InjectMock
    WalletClient walletClient;

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

            when(walletClient.reserveBalance(eq("account-123"), any()))
                .thenReturn(new WalletClient.ReserveResponse("RESERVED", "ref-123"));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertNotNull(payment);
            assertEquals("account-123", payment.accountId);
            assertEquals(BillerType.PLN, payment.billerType);
            assertEquals("12345678901234", payment.customerId);
            assertEquals(new BigDecimal("100000"), payment.amount);
            assertEquals(BillPayment.PaymentStatus.COMPLETED, payment.status);
            assertNotNull(payment.referenceNumber);
            
            verify(walletClient).reserveBalance(eq("account-123"), any());
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

            when(walletClient.reserveBalance(eq("account-123"), any()))
                .thenReturn(new WalletClient.ReserveResponse("FAILED", null));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertNotNull(payment);
            assertEquals(BillPayment.PaymentStatus.FAILED, payment.status);
            assertEquals("Failed to reserve balance", payment.failureReason);
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

            when(walletClient.reserveBalance(any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertNotNull(payment);
            assertEquals(BillPayment.PaymentStatus.FAILED, payment.status);
            assertEquals("Wallet service unavailable", payment.failureReason);
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

            when(walletClient.reserveBalance(any(), any()))
                .thenReturn(new WalletClient.ReserveResponse("RESERVED", "ref-123"));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertEquals(new BigDecimal("2500"), payment.adminFee);
            assertEquals(new BigDecimal("102500"), payment.totalAmount);
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

            when(walletClient.reserveBalance(any(), any()))
                .thenReturn(new WalletClient.ReserveResponse("RESERVED", "ref-123"));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertEquals(BigDecimal.ZERO, payment.adminFee);
            assertEquals(new BigDecimal("50000"), payment.totalAmount);
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

            when(walletClient.reserveBalance(any(), any()))
                .thenReturn(new WalletClient.ReserveResponse("RESERVED", "ref-123"));

            // When
            BillPayment payment = paymentService.createPayment(request);

            // Then
            assertEquals(new BigDecimal("2000"), payment.adminFee);
            assertEquals(new BigDecimal("77000"), payment.totalAmount);
        }
    }
}
