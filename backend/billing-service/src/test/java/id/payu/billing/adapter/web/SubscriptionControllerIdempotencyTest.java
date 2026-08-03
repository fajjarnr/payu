package id.payu.billing.adapter.web;

import id.payu.commons.idempotency.Idempotent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionControllerIdempotencyTest {

    @Test
    void cancelRequiresIdempotencyKey() throws NoSuchMethodException {
        Method method = SubscriptionController.class.getDeclaredMethod(
                "cancelSubscription", java.util.UUID.class, String.class);
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        assertNotNull(idempotent);
        assertTrue(idempotent.required());
        assertEquals("X-Idempotency-Key", idempotent.headerName());
    }
}
