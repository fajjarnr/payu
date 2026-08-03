package id.payu.transaction.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DisbursementControllerIdempotencyTest {

    @Test
    void callbackRequiresIdempotencyKey() {
        Method method = Arrays.stream(DisbursementController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("handleCallback"))
                .findFirst()
                .orElseThrow();
        Idempotent idempotent = method.getAnnotation(Idempotent.class);
        assertTrue(idempotent != null && idempotent.required(),
                "handleCallback must require X-Idempotency-Key");
    }
}
