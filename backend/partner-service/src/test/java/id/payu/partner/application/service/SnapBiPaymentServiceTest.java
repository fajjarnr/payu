package id.payu.partner.application.service;

import id.payu.partner.dto.snap.PaymentRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SnapBiPaymentServiceTest {

    @InjectMocks
    private SnapBiPaymentService paymentService;

    @Test
    public void testCreatePayment() {
        String partnerId = "123";
        PaymentRequest request = new PaymentRequest();
        request.partnerReferenceNo = "REF-TEST-001";
        request.amount = new PaymentRequest.Amount();
        request.amount.value = new BigDecimal("10000.00");
        request.amount.currency = "IDR";
        request.beneficiaryAccountNo = "1234567890";
        request.beneficiaryBankCode = "014";
        request.sourceAccountNo = "0987654321";

        var response = paymentService.createPayment(partnerId, request);

        assertNotNull(response);
        assertEquals("2002500", response.responseCode);
        assertEquals("Successful", response.responseMessage);
        assertNotNull(response.referenceNo);
        assertTrue(response.referenceNo.startsWith("PAYU-"));
    }

    @Test
    public void testGetPaymentStatus() {
        String partnerId = "123";
        PaymentRequest request = new PaymentRequest();
        request.partnerReferenceNo = "REF-TEST-002";
        request.amount = new PaymentRequest.Amount();
        request.amount.value = new BigDecimal("15000.00");
        request.amount.currency = "IDR";
        request.beneficiaryAccountNo = "1234567890";
        request.beneficiaryBankCode = "014";
        request.sourceAccountNo = "0987654321";

        var createResponse = paymentService.createPayment(partnerId, request);
        var statusResponse = paymentService.getPaymentStatus(partnerId, createResponse.referenceNo);

        assertNotNull(statusResponse);
        assertEquals("2002500", statusResponse.responseCode);
        assertEquals("Successful", statusResponse.responseMessage);
        assertEquals(createResponse.referenceNo, statusResponse.referenceNo);
        assertEquals("PENDING", statusResponse.status);
    }

    @Test
    public void testGetPaymentStatusByPartnerRef() {
        String partnerId = "123";
        PaymentRequest request = new PaymentRequest();
        request.partnerReferenceNo = "REF-TEST-003";
        request.amount = new PaymentRequest.Amount();
        request.amount.value = new BigDecimal("20000.00");
        request.amount.currency = "IDR";
        request.beneficiaryAccountNo = "1234567890";
        request.beneficiaryBankCode = "014";
        request.sourceAccountNo = "0987654321";

        paymentService.createPayment(partnerId, request);
        var statusResponse = paymentService.getPaymentStatus(partnerId, "REF-TEST-003");

        assertNotNull(statusResponse);
        assertEquals("2002500", statusResponse.responseCode);
    }

    @Test
    public void testGetPaymentStatusNotFound() {
        String partnerId = "123";
        var statusResponse = paymentService.getPaymentStatus(partnerId, "NON-EXISTENT-REF");

        assertNotNull(statusResponse);
        assertEquals("4042500", statusResponse.responseCode);
        assertEquals("Payment not found", statusResponse.responseMessage);
    }

    @Test
    public void testUpdatePaymentStatus() {
        String partnerId = "123";
        PaymentRequest request = new PaymentRequest();
        request.partnerReferenceNo = "REF-TEST-004";
        request.amount = new PaymentRequest.Amount();
        request.amount.value = new BigDecimal("25000.00");
        request.amount.currency = "IDR";
        request.beneficiaryAccountNo = "1234567890";
        request.beneficiaryBankCode = "014";
        request.sourceAccountNo = "0987654321";

        var createResponse = paymentService.createPayment(partnerId, request);

        paymentService.updatePaymentStatus(createResponse.referenceNo, "COMPLETED");

        var statusResponse = paymentService.getPaymentStatus(partnerId, createResponse.referenceNo);
        assertEquals("COMPLETED", statusResponse.status);
    }
}
