package id.payu.billing.adapter.web;

import id.payu.billing.dto.CreatePaymentRequest;
import id.payu.security.annotation.Audited;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentControllerContractTest {

    @Test
    void createPaymentKeepsLegacyAuditEntityType() throws NoSuchMethodException {
        Audited audited = PaymentController.class
                .getMethod("createPayment", CreatePaymentRequest.class)
                .getAnnotation(Audited.class);

        assertEquals("BillPaymentEntity", audited.entityType());
    }
}
