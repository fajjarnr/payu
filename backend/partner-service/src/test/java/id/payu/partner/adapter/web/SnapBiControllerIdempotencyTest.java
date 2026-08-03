package id.payu.partner.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapBiControllerIdempotencyTest {

    @Test
    void paymentAndRefundRequireIdempotencyKey() {
        assertRequired("createPayment");
        assertRequired("createRefund");
    }

    private void assertRequired(String methodName) {
        Method method = Arrays.stream(SnapBiController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        Idempotent idempotent = method.getAnnotation(Idempotent.class);
        assertTrue(idempotent != null && idempotent.required(),
                methodName + " must require X-Idempotency-Key");
    }
}
