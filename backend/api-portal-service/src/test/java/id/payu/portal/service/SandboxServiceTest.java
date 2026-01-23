package id.payu.portal.service;

import id.payu.portal.dto.SandboxPaymentRequest;
import id.payu.portal.dto.SandboxPaymentResponse;
import id.payu.portal.dto.SandboxPaymentStatusResponse;
import id.payu.portal.dto.SandboxRefundRequest;
import id.payu.portal.dto.SandboxRefundResponse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SandboxServiceTest {

    @Inject
    SandboxService sandboxService;

    @BeforeEach
    void clearData() {
        sandboxService.clearData().await().indefinitely();
    }

    @Test
    void testCreatePayment() {
        SandboxPaymentRequest request = new SandboxPaymentRequest(
            "TEST-REF-001",
            new SandboxPaymentRequest.Amount(new BigDecimal("100000.00"), "IDR"),
            "1234567890",
            "014",
            "9876543210",
            Map.of("description", "Test payment")
        );

        SandboxPaymentResponse response = sandboxService.createPayment(request).await().indefinitely();

        assertNotNull(response);
        assertEquals("TEST-REF-001", response.partnerReferenceNo());
        assertNotNull(response.paymentReferenceNo());
        assertEquals("COMPLETED", response.paymentStatus());
        assertEquals(new BigDecimal("100000.00"), response.amount().value());
        assertEquals("IDR", response.amount().currency());
        assertEquals("1234567890", response.beneficiaryAccountNo());
        assertEquals("014", response.beneficiaryBankCode());
        assertEquals("9876543210", response.sourceAccountNo());
    }

    @Test
    void testGetPaymentStatus() {
        SandboxPaymentRequest paymentRequest = new SandboxPaymentRequest(
            "TEST-REF-002",
            new SandboxPaymentRequest.Amount(new BigDecimal("50000.00"), "IDR"),
            "1234567890",
            "014",
            "9876543210",
            null
        );

        SandboxPaymentResponse payment = sandboxService.createPayment(paymentRequest).await().indefinitely();
        String paymentReferenceNo = payment.paymentReferenceNo();

        SandboxPaymentStatusResponse status = sandboxService.getPaymentStatus(paymentReferenceNo).await().indefinitely();

        assertNotNull(status);
        assertEquals("TEST-REF-002", status.partnerReferenceNo());
        assertEquals(paymentReferenceNo, status.paymentReferenceNo());
        assertEquals("COMPLETED", status.paymentStatus());
        assertEquals(new BigDecimal("50000.00"), status.amount().value());
        assertEquals("IDR", status.amount().currency());
    }

    @Test
    void testGetPaymentStatusNotFound() {
        SandboxPaymentStatusResponse status = sandboxService.getPaymentStatus("NON-EXISTENT-REF").await().indefinitely();
        assertNull(status);
    }

    @Test
    void testCreateRefund() {
        SandboxPaymentRequest paymentRequest = new SandboxPaymentRequest(
            "TEST-REF-003",
            new SandboxPaymentRequest.Amount(new BigDecimal("75000.00"), "IDR"),
            "1234567890",
            "014",
            "9876543210",
            null
        );

        SandboxPaymentResponse payment = sandboxService.createPayment(paymentRequest).await().indefinitely();
        String paymentReferenceNo = payment.paymentReferenceNo();

        SandboxRefundRequest refundRequest = new SandboxRefundRequest(
            "REFUND-REF-001",
            "Customer request"
        );

        SandboxRefundResponse refund = sandboxService.createRefund(paymentReferenceNo, refundRequest).await().indefinitely();

        assertNotNull(refund);
        assertEquals("REFUND-REF-001", refund.refundReferenceNo());
        assertEquals(paymentReferenceNo, refund.originalReferenceNo());
        assertEquals("COMPLETED", refund.refundStatus());
        assertEquals(new BigDecimal("75000.00"), refund.amount().value());
        assertEquals("IDR", refund.amount().currency());
    }

    @Test
    void testCreateRefundForNonExistentPayment() {
        SandboxRefundRequest refundRequest = new SandboxRefundRequest(
            "REFUND-REF-002",
            "Test refund"
        );

        SandboxRefundResponse refund = sandboxService.createRefund("NON-EXISTENT-PAYMENT", refundRequest).await().indefinitely();
        assertNull(refund);
    }

    @Test
    void testClearData() {
        SandboxPaymentRequest paymentRequest = new SandboxPaymentRequest(
            "TEST-REF-004",
            new SandboxPaymentRequest.Amount(new BigDecimal("25000.00"), "IDR"),
            "1234567890",
            "014",
            "9876543210",
            null
        );

        sandboxService.createPayment(paymentRequest).await().indefinitely();

        sandboxService.clearData().await().indefinitely();

        Map<String, Object> stats = sandboxService.getStats().await().indefinitely();
        assertEquals(0, stats.get("totalPayments"));
        assertEquals(0, stats.get("totalRefunds"));
    }

    @Test
    void testGetStats() {
        Map<String, Object> stats = sandboxService.getStats().await().indefinitely();

        assertNotNull(stats);
        assertTrue(stats.containsKey("totalPayments"));
        assertTrue(stats.containsKey("totalRefunds"));
        assertTrue(stats.containsKey("latencyEnabled"));
        assertTrue(stats.containsKey("latencyMinMs"));
        assertTrue(stats.containsKey("latencyMaxMs"));
    }
}
