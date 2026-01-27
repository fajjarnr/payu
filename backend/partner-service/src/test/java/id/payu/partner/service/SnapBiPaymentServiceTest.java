package id.payu.partner.service;

import id.payu.partner.dto.snap.PaymentRequest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

@QuarkusTest
@Disabled("Service tests require Docker/Testcontainers - disabled when Docker not available")
public class SnapBiPaymentServiceTest {

    @jakarta.inject.Inject
    SnapBiPaymentService paymentService;

    @Test
    public void testCreatePayment() throws Exception {
        String partnerId = "123";
        PaymentRequest request = new PaymentRequest();
        request.partnerReferenceNo = "REF-TEST-001";
        request.amount = new PaymentRequest.Amount();
        request.amount.value = new BigDecimal("10000.00");
        request.amount.currency = "IDR";
        request.beneficiaryAccountNo = "1234567890";
        request.beneficiaryBankCode = "014";
        request.sourceAccountNo = "0987654321";

        var response = paymentService.createPayment(partnerId, request).await().indefinitely();

        assertNotNull(response);
        assertEquals("2002500", response.responseCode);
        assertEquals("Successful", response.responseMessage);
        assertNotNull(response.referenceNo);
        assertTrue(response.referenceNo.startsWith("PAYU-"));
    }

    @Test
    public void testGetPaymentStatus() throws Exception {
        String partnerId = "123";
        PaymentRequest request = new PaymentRequest();
        request.partnerReferenceNo = "REF-TEST-002";
        request.amount = new PaymentRequest.Amount();
        request.amount.value = new BigDecimal("15000.00");
        request.amount.currency = "IDR";
        request.beneficiaryAccountNo = "1234567890";
        request.beneficiaryBankCode = "014";
        request.sourceAccountNo = "0987654321";

        var createResponse = paymentService.createPayment(partnerId, request).await().indefinitely();
        var statusResponse = paymentService.getPaymentStatus(partnerId, createResponse.referenceNo).await().indefinitely();

        assertNotNull(statusResponse);
        assertEquals("2002500", statusResponse.responseCode);
        assertEquals("Successful", statusResponse.responseMessage);
        assertEquals(createResponse.referenceNo, statusResponse.referenceNo);
        assertEquals("PENDING", statusResponse.status);
    }

    @Test
    public void testGetPaymentStatusByPartnerRef() throws Exception {
        String partnerId = "123";
        PaymentRequest request = new PaymentRequest();
        request.partnerReferenceNo = "REF-TEST-003";
        request.amount = new PaymentRequest.Amount();
        request.amount.value = new BigDecimal("20000.00");
        request.amount.currency = "IDR";
        request.beneficiaryAccountNo = "1234567890";
        request.beneficiaryBankCode = "014";
        request.sourceAccountNo = "0987654321";

        paymentService.createPayment(partnerId, request).await().indefinitely();
        var statusResponse = paymentService.getPaymentStatus(partnerId, "REF-TEST-003").await().indefinitely();

        assertNotNull(statusResponse);
        assertEquals("2002500", statusResponse.responseCode);
    }

    @Test
    public void testGetPaymentStatusNotFound() {
        String partnerId = "123";
        var statusResponse = paymentService.getPaymentStatus(partnerId, "NON-EXISTENT-REF").await().indefinitely();

        assertNotNull(statusResponse);
        assertEquals("4042500", statusResponse.responseCode);
        assertEquals("Payment not found", statusResponse.responseMessage);
    }

    @Test
    public void testUpdatePaymentStatus() throws Exception {
        String partnerId = "123";
        PaymentRequest request = new PaymentRequest();
        request.partnerReferenceNo = "REF-TEST-004";
        request.amount = new PaymentRequest.Amount();
        request.amount.value = new BigDecimal("25000.00");
        request.amount.currency = "IDR";
        request.beneficiaryAccountNo = "1234567890";
        request.beneficiaryBankCode = "014";
        request.sourceAccountNo = "0987654321";

        var createResponse = paymentService.createPayment(partnerId, request).await().indefinitely();

        paymentService.updatePaymentStatus(createResponse.referenceNo, "COMPLETED");

        var statusResponse = paymentService.getPaymentStatus(partnerId, createResponse.referenceNo).await().indefinitely();
        assertEquals("COMPLETED", statusResponse.status);
    }
}
